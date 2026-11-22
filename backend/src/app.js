'use strict';
const http = require('node:http');
const crypto = require('node:crypto');
const { hashPassword, verifyPassword, sign, verify } = require('./auth');
const { createLimiter } = require('./ratelimit');
const { normalizePkMobile, parseCallerId, validName, validEmail } = require('./validate');
const { ProviderError } = require('./providers/errors');
const { mask } = require('./logger');

class HttpError extends Error { constructor(status, message) { super(message); this.status = status; } }

function send(res, status, body) {
  const payload = body === undefined ? '' : JSON.stringify(body);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff',
  });
  res.end(payload);
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let size = 0; const chunks = [];
    req.on('data', c => {
      size += c.length;
      if (size > 10000) { reject(new HttpError(413, 'Request too large.')); req.destroy(); return; }
      chunks.push(c);
    });
    req.on('end', () => {
      if (!chunks.length) return resolve({});
      try { resolve(JSON.parse(Buffer.concat(chunks).toString())); }
      catch { reject(new HttpError(400, 'Invalid JSON.')); }
    });
    req.on('error', reject);
  });
}

function createApp({ provider, store, secret, log = () => {}, limits = {} }) {
  const general = createLimiter({ windowMs: 60000, max: limits.general ?? 120 });
  const authLimit = createLimiter({ windowMs: 60000, max: limits.auth ?? 10 });
  const callLimit = createLimiter({ windowMs: 60000, max: limits.calls ?? 10 });
  const dummyHash = hashPassword('dummy-password-for-timing');
  const d = store.data;

  const publicNumber = (n, userId) => {
    const nm = d.names[userId];
    return {
      id: n.id, e164: n.e164, status: n.status, isDefault: n.isDefault,
      callerName: n.status === 'verified' && nm && nm.status === 'approved' ? nm.name : null,
    };
  };
  const ownedNumber = (userId, id) => {
    const n = d.numbers.find(x => x.id === id && x.userId === userId);
    if (!n) throw new HttpError(404, 'Number not found.');
    return n;
  };
  const callView = (c, testMode) => ({ callId: c.callId, status: c.status, durationSec: c.durationSec, testMode });
  const testMode = () => provider.mode === 'mock';

  function authenticate(req, ip) {
    const h = req.headers.authorization || '';
    const p = h.startsWith('Bearer ') ? verify(h.slice(7), secret) : null;
    if (!p) { log('invalid_token', { ip }); throw new HttpError(401, 'Session expired. Sign in again.'); }
    const user = d.users.find(u => u.id === p.sub);
    if (!user) throw new HttpError(401, 'Session expired. Sign in again.');
    return user;
  }

  async function route(req, res) {
    const ip = req.socket.remoteAddress || 'unknown';
    if (!general(ip)) throw new HttpError(429, 'Too many requests. Try again in a minute.');
    const url = new URL(req.url, 'http://localhost');
    const seg = url.pathname.split('/').filter(Boolean);
    const m = req.method;

    if (m === 'GET' && seg.length === 1 && seg[0] === 'health') return send(res, 200, { ok: true });

    if (seg[0] === 'auth' && seg.length === 2 && m === 'POST') {
      if (!authLimit(ip)) { log('auth_rate_limited', { ip }); throw new HttpError(429, 'Too many attempts. Try again in a minute.'); }
      const { email, password } = await readBody(req);
      if (seg[1] === 'register') {
        if (!validEmail(email)) throw new HttpError(400, 'Enter a valid email address.');
        if (typeof password !== 'string' || password.length < 8 || password.length > 100) throw new HttpError(400, 'Password must be 8 to 100 characters.');
        const e = email.trim().toLowerCase();
        if (d.users.some(u => u.email === e)) throw new HttpError(409, 'An account with this email already exists.');
        const user = { id: crypto.randomUUID(), email: e, passHash: hashPassword(password), createdAt: Date.now() };
        d.users.push(user); store.save();
        log('user_registered', { user: user.id });
        return send(res, 201, { token: sign({ sub: user.id }, secret) });
      }
      if (seg[1] === 'login') {
        const e = typeof email === 'string' ? email.trim().toLowerCase() : '';
        const user = d.users.find(u => u.email === e);
        const ok = typeof password === 'string' && verifyPassword(password, user ? user.passHash : dummyHash) && !!user;
        if (!ok) { log('login_failed', { ip }); throw new HttpError(401, 'Wrong email or password.'); }
        return send(res, 200, { token: sign({ sub: user.id }, secret) });
      }
      throw new HttpError(404, 'Not found.');
    }

    const user = authenticate(req, ip);

    if (m === 'GET' && seg[0] === 'me' && seg.length === 1) return send(res, 200, { email: user.email });
    if (m === 'GET' && seg[0] === 'mode' && seg.length === 1)
      return send(res, 200, { providerConfigured: provider.configured, mode: provider.mode });

    // ---- Verified numbers ----
    if (seg[0] === 'numbers') {
      if (seg.length === 1 && m === 'GET')
        return send(res, 200, d.numbers.filter(n => n.userId === user.id).map(n => publicNumber(n, user.id)));
      if (seg.length === 1 && m === 'POST') {
        const { e164: raw } = await readBody(req);
        const e164 = parseCallerId(raw);
        if (!e164) throw new HttpError(400, 'Enter a valid phone number.');
        if (d.numbers.filter(n => n.userId === user.id).length >= 10) throw new HttpError(400, 'You can add up to 10 numbers.');
        if (d.numbers.some(n => n.userId === user.id && n.e164 === e164)) throw new HttpError(409, 'You already added this number.');
        if (d.numbers.some(n => n.userId !== user.id && n.e164 === e164 && n.status === 'verified')) {
          log('number_claim_conflict', { user: user.id, number: mask(e164) });
          throw new HttpError(409, 'This number is already registered to another account.');
        }
        await provider.requestCallerIdVerification(user.id, e164);
        const n = { id: crypto.randomUUID(), userId: user.id, e164, status: 'pending', isDefault: false, attempts: 0, createdAt: Date.now() };
        d.numbers.push(n); store.save();
        log('caller_id_verification_requested', { user: user.id, number: mask(e164) });
        return send(res, 201, publicNumber(n, user.id));
      }
      if (seg.length === 3 && seg[2] === 'verify' && m === 'POST') {
        const n = ownedNumber(user.id, seg[1]);
        if (n.status === 'verified') return send(res, 200, publicNumber(n, user.id));
        if (n.attempts >= 5) throw new HttpError(429, 'Too many wrong codes. Remove this number and add it again.');
        const { code } = await readBody(req);
        n.attempts += 1;
        const r = await provider.verifyCallerId(user.id, n.e164, String(code || ''));
        if (!r.verified) { store.save(); log('verification_failed', { user: user.id, number: mask(n.e164) }); throw new HttpError(400, 'That code is not correct.'); }
        n.status = 'verified';
        if (!d.numbers.some(x => x.userId === user.id && x.isDefault)) n.isDefault = true;
        store.save();
        log('caller_id_verified', { user: user.id, number: mask(n.e164) });
        return send(res, 200, publicNumber(n, user.id));
      }
      if (seg.length === 3 && seg[2] === 'default' && m === 'POST') {
        const n = ownedNumber(user.id, seg[1]);
        if (n.status !== 'verified') {
          log('unverified_default_attempt', { user: user.id, number: mask(n.e164) });
          throw new HttpError(403, 'Only verified numbers can be used as caller ID.');
        }
        d.numbers.filter(x => x.userId === user.id).forEach(x => { x.isDefault = x.id === n.id; });
        store.save();
        return send(res, 200, publicNumber(n, user.id));
      }
      if (seg.length === 2 && m === 'DELETE') {
        const n = ownedNumber(user.id, seg[1]);
        d.numbers = d.numbers.filter(x => x.id !== n.id);
        if (n.isDefault) {
          const next = d.numbers.find(x => x.userId === user.id && x.status === 'verified');
          if (next) next.isDefault = true;
        }
        store.save();
        log('caller_id_removed', { user: user.id, number: mask(n.e164) });
        return send(res, 204);
      }
    }

    // ---- Caller name ----
    if (seg[0] === 'caller-name' && seg.length === 1) {
      if (m === 'GET') {
        const nm = d.names[user.id];
        if (!nm) return send(res, 200, { name: null, status: 'none' });
        if (nm.status === 'pending') {
          const s = await provider.getCallerNameStatus(user.id);
          if (s.status !== 'none') { nm.status = s.status; store.save(); }
        }
        return send(res, 200, { name: nm.name, status: nm.status });
      }
      if (m === 'POST') {
        const { name } = await readBody(req);
        if (!validName(name)) throw new HttpError(400, 'Use 2 to 30 letters, numbers or basic punctuation.');
        const r = await provider.setApprovedCallerName(user.id, name.trim());
        d.names[user.id] = { name: name.trim(), status: r.status }; store.save();
        log('caller_name_submitted', { user: user.id });
        return send(res, 200, { name: name.trim(), status: r.status });
      }
    }

    // ---- Calls ----
    if (seg[0] === 'calls') {
      if (seg.length === 1 && m === 'POST') {
        if (!callLimit(user.id)) { log('call_rate_limited', { user: user.id }); throw new HttpError(429, 'Too many calls. Wait a minute and try again.'); }
        const { to: rawTo, callerIdId } = await readBody(req);
        const to = normalizePkMobile(rawTo);
        if (!to) throw new HttpError(400, 'Enter a valid Pakistani mobile number.');
        const from = d.numbers.find(n => n.id === callerIdId && n.userId === user.id);
        if (!from || from.status !== 'verified') {
          log('unauthorized_caller_id_attempt', { user: user.id });
          throw new HttpError(403, 'This caller ID is not verified for your account.');
        }
        const nm = d.names[user.id];
        const r = await provider.makeCall({ userId: user.id, to, from: from.e164, callerName: nm && nm.status === 'approved' ? nm.name : undefined });
        d.calls.push({ id: r.callId, userId: user.id, createdAt: Date.now(), toMasked: mask(to) }); store.save();
        log('call_requested', { user: user.id, to: mask(to) });
        return send(res, 201, callView(r, testMode()));
      }
      if (seg.length >= 2) {
        const c = d.calls.find(x => x.id === seg[1] && x.userId === user.id);
        if (!c) throw new HttpError(404, 'Call not found.');
        if (seg.length === 2 && m === 'GET') return send(res, 200, callView(await provider.getCallStatus(c.id), testMode()));
        if (seg.length === 3 && seg[2] === 'end' && m === 'POST') return send(res, 200, callView(await provider.endCall(c.id), testMode()));
      }
    }

    if (m === 'GET' && seg[0] === 'balance' && seg.length === 1) {
      const b = await provider.getAccountBalance();
      return send(res, 200, { balance: b.balance ?? null, currency: b.currency ?? null });
    }
    throw new HttpError(404, 'Not found.');
  }

  return http.createServer(async (req, res) => {
    try { await route(req, res); }
    catch (e) {
      if (e instanceof HttpError || e instanceof ProviderError) return send(res, e.status, { error: e.message });
      log('server_error', { message: String(e && e.message) });
      send(res, 500, { error: 'Something went wrong. Try again.' });
    }
  });
}
module.exports = { createApp };

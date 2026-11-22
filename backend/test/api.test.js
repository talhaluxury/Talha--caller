process.env.NODE_ENV = 'test';
const test = require('node:test');
const assert = require('node:assert');
const { createApp } = require('../src/app');
const { Store } = require('../src/store');
const { MockProvider } = require('../src/providers/mock');
const { NotConfiguredProvider } = require('../src/providers/notConfigured');

const SECRET = 'x'.repeat(40);
async function start(provider, limits = { general: 1000, auth: 1000, calls: 1000 }) {
  const server = createApp({ provider, store: new Store(null), secret: SECRET, limits });
  await new Promise(r => server.listen(0, r));
  const base = `http://127.0.0.1:${server.address().port}`;
  const call = async (method, path, body, token) => {
    const res = await fetch(base + path, {
      method,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
      body: body ? JSON.stringify(body) : undefined,
    });
    const text = await res.text();
    return { status: res.status, body: text ? JSON.parse(text) : null };
  };
  const register = async email => (await call('POST', '/auth/register', { email, password: 'password123' })).body.token;
  return { server, call, register };
}

test('auth: register, login, wrong password, missing token', async () => {
  const { server, call } = await start(new MockProvider());
  const r = await call('POST', '/auth/register', { email: 'a@b.com', password: 'password123' });
  assert.strictEqual(r.status, 201);
  assert.strictEqual((await call('POST', '/auth/login', { email: 'a@b.com', password: 'password123' })).status, 200);
  assert.strictEqual((await call('POST', '/auth/login', { email: 'a@b.com', password: 'nope-nope' })).status, 401);
  assert.strictEqual((await call('GET', '/numbers')).status, 401);
  assert.strictEqual((await call('GET', '/numbers', null, 'garbage.token')).status, 401);
  server.close();
});

test('caller ID: unverified numbers cannot call, verified can, others cannot borrow', async () => {
  let now = 1_000_000;
  const { server, call, register } = await start(new MockProvider(() => now));
  const a = await register('a@b.com');
  const b = await register('b@b.com');
  const added = await call('POST', '/numbers', { e164: '+442071838750' }, a);
  assert.strictEqual(added.status, 201);
  assert.strictEqual(added.body.status, 'pending');
  const id = added.body.id;

  let r = await call('POST', '/calls', { to: '03001234567', callerIdId: id }, a);
  assert.strictEqual(r.status, 403, 'unverified caller ID must be refused');
  r = await call('POST', `/numbers/${id}/default`, null, a);
  assert.strictEqual(r.status, 403);
  r = await call('POST', `/numbers/${id}/verify`, { code: '000000' }, a);
  assert.strictEqual(r.status, 400);
  r = await call('POST', `/numbers/${id}/verify`, { code: '123456' }, a);
  assert.strictEqual(r.body.status, 'verified');
  assert.strictEqual(r.body.isDefault, true);

  r = await call('POST', '/calls', { to: '03001234567', callerIdId: id }, b);
  assert.strictEqual(r.status, 403, "another user's caller ID must be refused");
  r = await call('POST', '/calls', { to: '12345', callerIdId: id }, a);
  assert.strictEqual(r.status, 400);

  r = await call('POST', '/calls', { to: '03001234567', callerIdId: id }, a);
  assert.strictEqual(r.status, 201);
  assert.strictEqual(r.body.testMode, true);
  assert.strictEqual(r.body.status, 'calling');
  const callId = r.body.callId;
  now += 3000; assert.strictEqual((await call('GET', `/calls/${callId}`, null, a)).body.status, 'ringing');
  now += 4000; r = await call('GET', `/calls/${callId}`, null, a);
  assert.strictEqual(r.body.status, 'connected');
  assert.strictEqual(r.body.durationSec, 2);
  assert.strictEqual((await call('GET', `/calls/${callId}`, null, b)).status, 404, 'call records are private');
  r = await call('POST', `/calls/${callId}/end`, null, a);
  assert.strictEqual(r.body.status, 'ended');
  server.close();
});

test('failed call simulation and number removal', async () => {
  let now = 0;
  const { server, call, register } = await start(new MockProvider(() => now));
  const a = await register('a@b.com');
  const n = (await call('POST', '/numbers', { e164: '+442071838750' }, a)).body;
  await call('POST', `/numbers/${n.id}/verify`, { code: '123456' }, a);
  const c = (await call('POST', '/calls', { to: '03000000000', callerIdId: n.id }, a)).body;
  now += 2500;
  assert.strictEqual((await call('GET', `/calls/${c.callId}`, null, a)).body.status, 'failed');
  assert.strictEqual((await call('DELETE', `/numbers/${n.id}`, null, a)).status, 204);
  assert.deepStrictEqual((await call('GET', '/numbers', null, a)).body, []);
  server.close();
});

test('caller name goes pending then approved or rejected', async () => {
  let now = 0;
  const { server, call, register } = await start(new MockProvider(() => now));
  const a = await register('a@b.com');
  assert.strictEqual((await call('GET', '/caller-name', null, a)).body.status, 'none');
  assert.strictEqual((await call('POST', '/caller-name', { name: '<b>' }, a)).status, 400);
  assert.strictEqual((await call('POST', '/caller-name', { name: 'TALHA STUDIO' }, a)).body.status, 'pending');
  now += 4000;
  assert.strictEqual((await call('GET', '/caller-name', null, a)).body.status, 'approved');
  await call('POST', '/caller-name', { name: 'Reject me' }, a);
  now += 4000;
  assert.strictEqual((await call('GET', '/caller-name', null, a)).body.status, 'rejected');
  server.close();
});

test('no provider configured: clear message, no fake success', async () => {
  const { server, call, register } = await start(new NotConfiguredProvider());
  const a = await register('a@b.com');
  assert.deepStrictEqual((await call('GET', '/mode', null, a)).body, { providerConfigured: false, mode: 'none' });
  const r = await call('POST', '/numbers', { e164: '+442071838750' }, a);
  assert.strictEqual(r.status, 503);
  assert.strictEqual(r.body.error, 'Calling service is not configured yet.');
  server.close();
});

test('rate limiting on auth endpoints', async () => {
  const { server, call } = await start(new MockProvider(), { general: 1000, auth: 3, calls: 10 });
  const codes = [];
  for (let i = 0; i < 5; i++) codes.push((await call('POST', '/auth/login', { email: 'x@y.com', password: 'wrongwrong' })).status);
  assert.deepStrictEqual(codes, [401, 401, 401, 429, 429]);
  server.close();
});

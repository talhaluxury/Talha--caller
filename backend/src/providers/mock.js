'use strict';
const crypto = require('node:crypto');
const { ProviderError } = require('./errors');
// TEST MODE ONLY. Simulates a provider so the app flow can be exercised.
// Nothing here places a real call. Verification code is always 123456.
// Numbers ending in 0000 simulate a failed call. Names containing "reject" are rejected.
class MockProvider {
  constructor(clock = Date.now) {
    this.name = 'mock'; this.mode = 'mock'; this.configured = true;
    this.clock = clock; this.calls = new Map(); this.names = new Map();
  }
  async requestCallerIdVerification() { return { status: 'pending' }; }
  async verifyCallerId(_userId, _e164, code) { return { verified: code === '123456' }; }
  async setApprovedCallerName(userId, name) {
    this.names.set(userId, { name, at: this.clock() });
    return { status: 'pending' };
  }
  async getCallerNameStatus(userId) {
    const n = this.names.get(userId);
    if (!n) return { status: 'none' };
    if (this.clock() - n.at < 3000) return { status: 'pending' };
    return { status: /reject/i.test(n.name) ? 'rejected' : 'approved' };
  }
  async makeCall({ to }) {
    const id = 'mock_' + crypto.randomUUID();
    this.calls.set(id, { start: this.clock(), fail: to.endsWith('0000'), endedAt: null });
    return this._view(id);
  }
  async getCallStatus(id) { return this._view(id); }
  async endCall(id) {
    const c = this.calls.get(id);
    if (!c) throw new ProviderError(404, 'Call not found.');
    if (!c.endedAt && !c.fail) c.endedAt = this.clock();
    return this._view(id);
  }
  async getAccountBalance() { return { balance: 10, currency: 'USD' }; }
  _view(id) {
    const c = this.calls.get(id);
    if (!c) throw new ProviderError(404, 'Call not found.');
    const el = this.clock() - c.start;
    let status, durationSec = 0;
    if (c.fail) status = el < 2000 ? 'calling' : 'failed';
    else if (c.endedAt) { status = 'ended'; durationSec = Math.max(0, Math.floor((c.endedAt - c.start - 5000) / 1000)); }
    else if (el < 2000) status = 'calling';
    else if (el < 5000) status = 'ringing';
    else { status = 'connected'; durationSec = Math.floor((el - 5000) / 1000); }
    return { callId: id, status, durationSec };
  }
}
module.exports = { MockProvider };

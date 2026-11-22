'use strict';
const { ProviderError } = require('./errors');
const MSG = 'Calling service is not configured yet.';
const fail = async () => { throw new ProviderError(503, MSG); };
// Default provider. Every operation fails clearly until a real adapter is supplied.
class NotConfiguredProvider {
  constructor() { this.name = 'none'; this.mode = 'none'; this.configured = false; }
  requestCallerIdVerification = fail;
  verifyCallerId = fail;
  setApprovedCallerName = fail;
  getCallerNameStatus = fail;
  makeCall = fail;
  getCallStatus = fail;
  endCall = fail;
  getAccountBalance = fail;
}
module.exports = { NotConfiguredProvider };

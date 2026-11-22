'use strict';
class ProviderError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}
module.exports = { ProviderError };

'use strict';
const { MockProvider } = require('./mock');
const { NotConfiguredProvider } = require('./notConfigured');
// PROVIDER=mock enables test mode. Anything else means "not configured".
// A real adapter must implement the same methods as MockProvider and be added here
// only after you have the provider's API documentation, credentials and approval.
function createProvider(env = process.env) {
  if (env.PROVIDER === 'mock') return new MockProvider();
  return new NotConfiguredProvider();
}
module.exports = { createProvider };

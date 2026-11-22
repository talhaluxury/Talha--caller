'use strict';
const path = require('node:path');
const { createApp } = require('./app');
const { Store } = require('./store');
const { createProvider } = require('./providers');
const { createSecurityLogger } = require('./logger');

const secret = process.env.JWT_SECRET;
if (!secret || secret.length < 32) {
  console.error('Set JWT_SECRET to a random string of at least 32 characters. Example: openssl rand -hex 32');
  process.exit(1);
}
const dataDir = process.env.DATA_DIR || path.join(__dirname, '..', 'data');
const provider = createProvider(process.env);
const app = createApp({
  provider,
  secret,
  store: new Store(path.join(dataDir, 'db.json')),
  log: createSecurityLogger(path.join(dataDir, 'security.log')),
});
const port = Number(process.env.PORT) || 8080;
app.listen(port, () => console.log(`TALHA CALLER ID backend on :${port} (provider: ${provider.mode})`));

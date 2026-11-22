'use strict';
const fs = require('node:fs');
const path = require('node:path');
// Security events as JSON lines. Never log passwords, tokens, or full phone numbers.
function createSecurityLogger(file) {
  if (file) fs.mkdirSync(path.dirname(file), { recursive: true });
  return function log(event, fields = {}) {
    const line = JSON.stringify({ t: new Date().toISOString(), event, ...fields });
    if (file) fs.appendFileSync(file, line + '\n');
    else if (process.env.NODE_ENV !== 'test') console.log(line);
  };
}
function mask(n) { return typeof n === 'string' && n.length > 4 ? '***' + n.slice(-4) : '***'; }
module.exports = { createSecurityLogger, mask };

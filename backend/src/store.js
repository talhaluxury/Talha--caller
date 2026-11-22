'use strict';
const fs = require('node:fs');
const path = require('node:path');
// Minimal JSON-file store so the project runs with zero dependencies.
// For production, swap for PostgreSQL or similar behind the same shape.
class Store {
  constructor(file) {
    this.file = file || null;
    this.data = { users: [], numbers: [], calls: [], names: {} };
    if (this.file && fs.existsSync(this.file)) this.data = JSON.parse(fs.readFileSync(this.file, 'utf8'));
  }
  save() {
    if (!this.file) return;
    fs.mkdirSync(path.dirname(this.file), { recursive: true });
    const tmp = this.file + '.tmp';
    fs.writeFileSync(tmp, JSON.stringify(this.data));
    fs.renameSync(tmp, this.file);
  }
}
module.exports = { Store };

'use strict';
function createLimiter({ windowMs, max }) {
  const hits = new Map();
  return function allow(key) {
    const now = Date.now();
    if (hits.size > 10000) for (const [k, v] of hits) if (!v.some(t => now - t < windowMs)) hits.delete(k);
    const arr = (hits.get(key) || []).filter(t => now - t < windowMs);
    if (arr.length >= max) { hits.set(key, arr); return false; }
    arr.push(now); hits.set(key, arr);
    return true;
  };
}
module.exports = { createLimiter };

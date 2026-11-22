'use strict';
// Normalizes 03XXXXXXXXX, +923XXXXXXXXX, 923XXXXXXXXX (and bare 3XXXXXXXXX) to +923XXXXXXXXX.
function normalizePkMobile(raw) {
  if (typeof raw !== 'string') return null;
  const c = raw.trim().replace(/[\s\-()]/g, '');
  let n;
  if (c.startsWith('+92')) n = c.slice(3);
  else if (c.startsWith('0092')) n = c.slice(4);
  else if (c.startsWith('92') && c.length === 12) n = c.slice(2);
  else if (c.startsWith('0')) n = c.slice(1);
  else n = c;
  return /^3\d{9}$/.test(n) ? '+92' + n : null;
}
function isE164(s) { return typeof s === 'string' && /^\+[1-9]\d{7,14}$/.test(s); }
// Caller IDs may be a Pakistani number or any provider-issued international number.
function parseCallerId(raw) {
  const pk = normalizePkMobile(raw);
  if (pk) return pk;
  const c = typeof raw === 'string' ? raw.trim().replace(/[\s\-()]/g, '') : '';
  return isE164(c) ? c : null;
}
function validName(n) { return typeof n === 'string' && /^[A-Za-z0-9 &.'-]{2,30}$/.test(n.trim()); }
function validEmail(e) { return typeof e === 'string' && e.length <= 120 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e); }
module.exports = { normalizePkMobile, isE164, parseCallerId, validName, validEmail };

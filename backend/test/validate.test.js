const test = require('node:test');
const assert = require('node:assert');
const { normalizePkMobile, parseCallerId, validName } = require('../src/validate');

test('normalizes the three supported Pakistani formats', () => {
  assert.strictEqual(normalizePkMobile('03001234567'), '+923001234567');
  assert.strictEqual(normalizePkMobile('+923001234567'), '+923001234567');
  assert.strictEqual(normalizePkMobile('923001234567'), '+923001234567');
  assert.strictEqual(normalizePkMobile('0300 123-4567'), '+923001234567');
});
test('rejects invalid Pakistani numbers', () => {
  for (const bad of ['', '0300123456', '030012345678', '02101234567', '+14155550123', 'abc', null, '+92300123456'])
    assert.strictEqual(normalizePkMobile(bad), null, String(bad));
});
test('caller ID accepts Pakistani or international E.164 only', () => {
  assert.strictEqual(parseCallerId('03451234567'), '+923451234567');
  assert.strictEqual(parseCallerId('+442071838750'), '+442071838750');
  assert.strictEqual(parseCallerId('12345'), null);
});
test('caller name rules', () => {
  assert.ok(validName('TALHA STUDIO'));
  assert.ok(!validName('<script>'));
  assert.ok(!validName('A'));
});

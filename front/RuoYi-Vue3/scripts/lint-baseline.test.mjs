import test from 'node:test'
import assert from 'node:assert/strict'
import { collectIssues, compareIssues } from './lint-baseline.mjs'

const result = (filePath, source, messages) => ({ filePath, source, messages })
const message = (line = 1) => ({ ruleId: 'no-undef', severity: 2, message: "'missing' is not defined.", line, column: 1, nodeType: 'Identifier' })
const issues = (rows) => collectIssues(rows, '/repo')
const original = issues([result('/repo/src/a.js', 'missing()\n', [message()])])

test('identical debt is allowed; inserting clean lines does not create debt', () => {
  const moved = issues([result('/repo/src/a.js', '// clean\nmissing()\n', [message(2)])])
  assert.deepEqual(compareIssues(original, moved).added, [])
})
test('removing old debt cannot offset a new violation in another file', () => {
  const changed = issues([result('/repo/src/b.js', 'missing()\n', [message()])])
  assert.equal(compareIssues(original, changed).added.length, 1)
})
test('same-rule error on a different source line is new debt', () => {
  const changed = issues([result('/repo/src/a.js', 'missing(1)\n', [message()])])
  assert.equal(compareIssues(original, changed).added.length, 1)
})
test('duplicate identical violations cannot hide behind a set comparison', () => {
  const changed = issues([result('/repo/src/a.js', 'missing()\nmissing()\n', [message(), message(2)])])
  assert.equal(compareIssues(original, changed).added[0].count, 1)
})
test('warnings and fatal parse errors are checked too; removed debt is reported', () => {
  const changed = issues([result('/repo/src/a.js', 'broken\n', [{ ...message(), ruleId: null, fatal: true }, { ...message(), ruleId: 'no-unused-vars', severity: 1 }])])
  assert.equal(compareIssues(original, changed).added.length, 2)
  assert.equal(compareIssues(original, []).removed.length, 1)
})

import test from 'node:test'
import assert from 'node:assert/strict'
import { reactive, computed, ref, effect, stop, isReactive } from 'vue'
import { amountToken, noteBody, createNoteKey, reviewCommand, batchCommand, approvalBlock, createResource, createSubmitGuard, batchReceipt, pageParams, snapshot, batchOutcome, createReasonDrafts, sendReasonDraft, createNoteDrafts, auditActionLabel } from '../src/utils/reviewUi.js'
import { businessError } from '../src/utils/businessError.js'
const note = { adjustmentAmount: '-0.10', reason: '  原因  ', handledAt: '2026-11-07', noteText: '  备注  ' }
test('signed money serializes exact decimal token without JS rounding', () => {
  assert.equal(amountToken('-9999999999.99'), '-9999999999.99')
  assert.equal(amountToken('0.1'), '0.10')
  assert.equal(amountToken('-0'), '-0.00')
  for (const value of ['1.005', '1e2', '01', '+1', 'Infinity', '10000000000', ' 1', '.5']) assert.throws(() => amountToken(value))
  const body = noteBody(note)
  assert.match(body, /"adjustmentAmount":-0\.10,/)
  assert.equal(JSON.parse(body).reason, '  原因  ')
  assert.equal(JSON.parse(body).noteText, '  备注  ')
})
test('note validates raw lengths, real calendar and controlled attachments', () => {
  assert.throws(() => noteBody({ ...note, reason: ` ${'字'.repeat(499)} ` }))
  assert.throws(() => noteBody({ ...note, handledAt: '2026-02-30' }))
  assert.throws(() => noteBody({ ...note, reason: '  ' }))
  for (const attachmentRef of ['file:///tmp/x', '/profile/upload/../x', '/profile/upload/a//x', '/profile/upload/a/./x', '/profile/upload/x/']) assert.throws(() => noteBody({ ...note, attachmentRef }))
  assert.equal(JSON.parse(noteBody({ ...note, attachmentRef: '/profile/upload/2026/凭证 (1).pdf' })).attachmentRef, '/profile/upload/2026/凭证 (1).pdf')
})
test('same-content retry reuses key, changed body and changed summary replace key', () => {
  let count = 0
  const key = createNoteKey(() => `key-${++count}`)
  const body = noteBody(note)
  assert.equal(key(`117:${body}`), key(`117:${body}`))
  const first = key(`117:${body}`)
  assert.notEqual(first, key(`118:${body}`))
  assert.notEqual(key(`117:${body}`), key(`117:${noteBody({ ...note, reason: '更正' })}`))
})
test('commands accept only version/reason and enforce independent action requirements', () => {
  const row = { id: 1, version: 0, reviewerName: 'forged', result: 'PENDING' }
  assert.deepEqual(reviewCommand(row, 'PASS', ''), { version: 0, reason: '' })
  for (const action of ['REJECT', 'WAIVE']) assert.throws(() => reviewCommand(row, action, '  '))
  assert.throws(() => reviewCommand({ version: '0' }, 'PASS', ''))
  assert.throws(() => batchCommand([row, row], 'PASS', ''))
  assert.throws(() => batchCommand(Array.from({ length: 101 }, (_, i) => ({ id: i + 1, version: 0 })), 'PASS', ''))
  assert.deepEqual(batchCommand([row], 'REJECT', ' 原文 '), { items: [{ id: 1, version: 0, reason: ' 原文 ' }] })
})
const valid = { reviewEvidenceStatus: 'VERIFIED', currentBatch: { current: true }, reviewCounts: { PENDING: '0', REJECT: '0', PASS: '2', WAIVE: '0' } }
test('approval blocks missing history, stale evidence, empty members and rejected/pending members', () => {
  assert.equal(approvalBlock(valid), '')
  assert.match(approvalBlock({ ...valid, reviewCounts: {} }), /为空/)
  assert.match(approvalBlock({ ...valid, currentBatch: { current: false } }), /失效/)
  assert.match(approvalBlock({ ...valid, reviewCounts: { PENDING: '3', REJECT: '2' } }), /未处理 3 条、已驳回 2 条/)
  assert.match(approvalBlock(null), /缺失/)
})
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }
test('latest page wins and an old rejection cannot replace current state', async () => {
  const resource = createResource(), old = deferred(), fresh = deferred()
  const a = resource.load(() => old.promise), b = resource.load(() => fresh.promise)
  fresh.resolve({ rows: ['page2'], total: 11 }); await b
  old.reject(new Error('old failed')); await a
  assert.deepEqual(resource.state.data.rows, ['page2'])
  assert.equal(resource.state.error, '')
  assert.equal(resource.state.loading, false)
})
test('explicit failure clears stale rows, retry works, close invalidates inflight detail', async () => {
  const resource = createResource()
  await resource.load(async () => ({ rows: [1] }))
  await resource.load(async () => { throw new Error('network down') })
  assert.equal(resource.state.error, 'network down'); assert.equal(resource.state.data, null)
  await resource.load(async () => ({ rows: [] })); assert.equal(resource.state.error, '')
  const d = deferred(), running = resource.load(() => d.promise)
  resource.invalidate(); d.resolve({ rows: [2] }); await running
  assert.equal(resource.state.data, null)
})
test('double-click guard executes once and releases after failure for retry', async () => {
  const guard = createSubmitGuard(), d = deferred(); let writes = 0
  const first = guard(async () => { writes++; await d.promise })
  assert.equal(await guard(async () => writes++), false)
  d.reject(new Error('network')); await assert.rejects(first)
  await guard(async () => writes++)
  assert.equal(writes, 2)
})
test('independent paging is bounded and batch partial failures remain visible', () => {
  assert.deepEqual(pageParams(), { pageNum: 1, pageSize: 10 })
  const reviews = pageParams(2, 100), notes = pageParams()
  assert.equal(reviews.pageNum, 2); assert.equal(notes.pageNum, 1)
  for (const p of [[0, 10], [1, 101], [1.1, 10], ['1', 10]]) assert.throws(() => pageParams(...p))
  assert.equal(batchReceipt({ successCount: 1, failCount: 1, failDetails: [{ id: 12, reason: '批次过期' }] }), '成功 1 条，失败 1 条；记录 12：批次过期')
})
test('canonical numeric strings stay exact and business conflict retains machine code', () => {
  const saved = snapshot({ snapshotJson: '{"values":{"calculatedWorkload":"123.4500","isOverLimit":"1"}}' })
  assert.equal(saved.values.calculatedWorkload, '123.4500')
  const e = businessError(409, '冲突'); assert.equal(e.code, 409); assert.equal(e.message, '冲突')
})

test('HTTP200 partial failure preserves raw reason and only failed rows, blocking stale resubmit', () => {
  const rows = [{ id: 1, version: 0 }, { id: 2, version: 0 }]
  const result = batchOutcome({ successCount: 1, failCount: 1, failDetails: [{ id: 2, reason: '审核版本过期' }] }, rows, '  原始原因  ')
  assert.equal(result.open, true)
  assert.equal(result.reason, '  原始原因  ')
  assert.deepEqual(result.targets, [rows[1]])
  assert.equal(result.requiresReselect, true)
  assert.match(result.error, /记录 2：审核版本过期/)
})
test('HTTP200 full failure retains every failed item and reason; full success clears input', () => {
  const rows = [{ id: 1, version: 0 }, { id: 2, version: 0 }]
  const failed = batchOutcome({ successCount: 0, failCount: 2, failDetails: rows.map(row => ({ id: row.id, reason: '批次过期' })) }, rows, '拒绝原因')
  assert.deepEqual(failed.targets, rows)
  assert.equal(failed.reason, '拒绝原因')
  assert.equal(failed.requiresReselect, true)
  const success = batchOutcome({ successCount: 2, failCount: 0, failDetails: [] }, rows, '拒绝原因')
  assert.deepEqual(success, { open: false, reason: '', targets: [], requiresReselect: false, error: '' })
})

test('summary rejection failure retains per-summary raw draft; only success clears it', async () => {
  const drafts = createReasonDrafts()
  drafts.set(118, '  驳回原文  '); drafts.set(119, '另一汇总')
  const sent = []
  await assert.rejects(sendReasonDraft(drafts, 118, async (id, reason) => { sent.push({ id, reason }); throw businessError(409, '已退回') }))
  assert.deepEqual(sent, [{ id: 118, reason: '  驳回原文  ' }])
  assert.equal(drafts.get(118), '  驳回原文  ')
  assert.equal(drafts.get(119), '另一汇总')
  await sendReasonDraft(drafts, 118, async (id, reason) => { sent.push({ id, reason }) })
  assert.equal(sent.length, 2)
  assert.equal(drafts.get(118), '')
  assert.equal(drafts.get(119), '另一汇总')
})

test('lost response then close/reopen reuses original pending note body/key', async () => {
  let sequence = 0
  const drafts = createNoteDrafts({}, () => `note-${++sequence}`)
  const scope = 'admin:117'
  Object.assign(drafts.get(scope).form, note)
  const first = drafts.request(scope)
  const server = new Map()
  const send = async request => { if (!server.has(request.key)) server.set(request.key, request.body); throw new Error('response lost after commit') }
  await assert.rejects(send(first))
  drafts.close(scope)
  assert.equal(drafts.get(scope).form.reason, note.reason)
  Object.assign(drafts.get(scope).form, note)
  const retry = drafts.request(scope)
  assert.deepEqual(retry, first)
  await assert.rejects(send(retry))
  assert.equal(server.size, 1)
  assert.equal(drafts.get(scope).form.reason, note.reason)
  drafts.get(scope).form.reason = '改变内容'
  assert.notEqual(drafts.request(scope).key, first.key)
  drafts.get(scope).form.reason = note.reason
  assert.deepEqual(drafts.request(scope), first)
})
test('pending note drafts isolate summary/login scope; only acknowledged success clears', () => {
  let sequence = 0
  const drafts = createNoteDrafts({}, () => `note-${++sequence}`)
  for (const scope of ['admin:117', 'admin:118', 'another:117']) Object.assign(drafts.get(scope).form, note)
  const first = drafts.request('admin:117'), second = drafts.request('admin:118'), another = drafts.request('another:117')
  assert.notEqual(first.key, second.key); assert.notEqual(first.key, another.key)
  drafts.confirm('admin:117')
  assert.equal(drafts.get('admin:117').form.reason, '')
  assert.deepEqual(drafts.request('admin:118'), second)
  assert.deepEqual(drafts.request('another:117'), another)
  Object.assign(drafts.get('admin:117').form, note)
  assert.notEqual(drafts.request('admin:117').key, first.key)
})

test('reactive drawer form switches scope without mixing drafts and resets only confirmed scope', () => {
  const drafts = createNoteDrafts(reactive({}), () => 'stable-key')
  const scope = ref('user1:117'), form = computed(() => drafts.get(scope.value).form)
  Object.assign(form.value, note)
  const original = drafts.request(scope.value)
  scope.value = 'user1:118'
  assert.equal(form.value.reason, '')
  form.value.reason = '另一汇总未提交原因'
  scope.value = 'user1:117'
  assert.equal(form.value.reason, note.reason)
  assert.deepEqual(drafts.request(scope.value), original)
  drafts.confirm(scope.value)
  assert.equal(form.value.reason, '')
  scope.value = 'user1:118'
  assert.equal(form.value.reason, '另一汇总未提交原因')
})

test('first pending note form and post-success replacement are reactive during edits', () => {
  const drafts = createNoteDrafts(reactive({}))
  const scope = 'admin:117'
  const form = computed(() => drafts.get(scope).form)
  assert.equal(isReactive(form.value), true)
  const seen = []
  const watcher = effect(() => seen.push(form.value.reason))
  form.value.reason = '首次输入'
  assert.equal(seen.at(-1), '首次输入')
  drafts.confirm(scope)
  assert.equal(isReactive(form.value), true)
  form.value.reason = '成功后新输入'
  assert.equal(seen.at(-1), '成功后新输入')
  stop(watcher)
})

test('timeline labels map actual server actions and preserve unknown audit codes', () => {
  assert.equal(auditActionLabel('approve'), '终审通过')
  assert.equal(auditActionLabel('teacherConfirm'), '教师确认')
  assert.equal(auditActionLabel('offlineNote'), '追加线下补差说明')
  assert.equal(auditActionLabel('review_pass'), '逐条审核通过')
  assert.equal(auditActionLabel('review_reject'), '逐条审核驳回')
  assert.equal(auditActionLabel('review_waive'), '逐条审核豁免')
  assert.equal(auditActionLabel('sign'), '院领导签字（历史）')
  assert.equal(auditActionLabel('unlock'), '解锁（历史）')
  assert.equal(auditActionLabel('future_action'), 'future_action')
  assert.equal(auditActionLabel('constructor'), 'constructor')
  assert.equal(auditActionLabel(null), '—')
})

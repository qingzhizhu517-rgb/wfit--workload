// Transport values stay strings until a bounded numeric command is needed.
export const reviewTypes = { TEACHING_TASK: '教学任务', MANUAL_DECLARATION: '手工申报', G11_ROLE: '岗位减免', OVER_LIMIT: '超限项目' }
export const reviewResults = { PENDING: '待处理', PASS: '通过', REJECT: '驳回', WAIVE: '豁免' }
export function snapshot(row) {
  try { return typeof row?.snapshotJson === 'string' ? JSON.parse(row.snapshotJson) : row?.snapshotJson || {} } catch { return {} }
}
export function amountToken(value) {
  const raw = String(value)
  if (!/^-?(?:0|[1-9]\d{0,9})(?:\.\d{1,2})?$/.test(raw)) throw new Error('金额须为正负十进制数，最多两位小数、十位整数，禁止舍入')
  const [whole, fraction = ''] = raw.split('.')
  return `${whole}.${fraction.padEnd(2, '0')}`
}
export function noteBody(form) {
  if (!form.reason?.trim() || form.reason.length > 500) throw new Error('原因必填，原文最多500字')
  if (!/^\d{4}-\d{2}-\d{2}$/.test(form.handledAt || '') || new Date(form.handledAt).toISOString().slice(0, 10) !== form.handledAt) throw new Error('请填写有效处理日期')
  if ((form.referenceNo || '').length > 100 || (form.noteText || '').length > 10000) throw new Error('凭证号最多100字，备注最多10000字')
  const attachment = (form.attachmentRef || '').trim()
  if (attachment && (!/^\/profile\/upload\/[\p{L}\p{M}\p{N}_./()（） -]+$/u.test(attachment) || /\.\.|\/\/|\/\.\/|\/\.$|\/$/.test(attachment) || attachment.length > 500)) throw new Error('附件须为受控上传引用 /profile/upload/…')
  const fields = { reason: form.reason, handledAt: form.handledAt, referenceNo: form.referenceNo || null, noteText: form.noteText || null, attachmentRef: attachment || null }
  return `{"adjustmentAmount":${amountToken(form.adjustmentAmount)},${JSON.stringify(fields).slice(1)}`
}
export function createNoteKey(makeKey = () => globalThis.crypto.randomUUID()) {
  let previous, key
  return body => {
    if (body !== previous) { previous = body; key = makeKey() }
    return key
  }
}
export function reviewCommand(row, action, reason) {
  if (!Object.hasOwn(reviewResults, action) || action === 'PENDING') throw new Error('审核动作无效')
  if (!Number.isSafeInteger(row.version) || row.version < 0) throw new Error('审核版本无效，请刷新')
  if (reason.length > 500 || (action !== 'PASS' && !reason.trim())) throw new Error('原因必填，原文最多500字')
  return { version: row.version, reason }
}
export function batchCommand(rows, action, reason) {
  if (!rows.length || rows.length > 100 || new Set(rows.map(r => String(r.id))).size !== rows.length) throw new Error('每批须为1至100条不同记录')
  return { items: rows.map(row => {
    if (!Number.isSafeInteger(row.id) || row.id <= 0) throw new Error('审核ID无效')
    return { id: row.id, ...reviewCommand(row, action, reason) }
  }) }
}
export function approvalBlock(detail) {
  const counts = detail?.reviewCounts || {}
  const pending = Number(counts.PENDING || 0), rejected = Number(counts.REJECT || 0)
  if (!detail?.currentBatch?.current || detail.reviewEvidenceStatus !== 'VERIFIED') return '当前审核证据缺失或失效；请查看详情，必要时驳回重算重提'
  if (!Object.values(counts).reduce((sum, v) => sum + Number(v), 0)) return '审核成员为空，不能终审'
  if (pending || rejected) return `未处理 ${pending} 条、已驳回 ${rejected} 条，须全部通过或豁免后终审`
  return ''
}
export function createResource(state = { loading: false, error: '', data: null }) {
  let epoch = 0
  return {
    state,
    invalidate() { epoch++; state.loading = false; state.data = null; state.error = '' },
    async load(fetcher) {
      const current = ++epoch
      state.loading = true; state.error = ''; state.data = null
      try { const data = await fetcher(); if (current === epoch) state.data = data; return current === epoch }
      catch (error) { if (current === epoch) state.error = error?.message || '请求失败，请重试'; return false }
      finally { if (current === epoch) state.loading = false }
    }
  }
}
export function createSubmitGuard() {
  let busy = false
  return async task => {
    if (busy) return false
    busy = true
    try { await task(); return true } finally { busy = false }
  }
}
export function batchReceipt(data) {
  return `成功 ${data?.successCount ?? 0} 条，失败 ${data?.failCount ?? 0} 条${(data?.failDetails || []).map(v => `；记录 ${v.id}：${v.reason}`).join('')}`
}

export function pageParams(pageNum = 1, pageSize = 10) {
  if (!Number.isSafeInteger(pageNum) || pageNum < 1 || pageNum > 2147483647 || !Number.isSafeInteger(pageSize) || pageSize < 1 || pageSize > 100) throw new Error('分页须为正整数，每页最多100条')
  return { pageNum, pageSize }
}

export function calculationEvidence(row) {
  const raw = snapshot(row).values?.calculationSnapshotJson
  try { return typeof raw === 'string' ? JSON.parse(raw) : raw || {} } catch { return {} }
}

export function batchOutcome(data, targets, reason) {
  if (!data?.failCount) return { open: false, reason: '', targets: [], requiresReselect: false, error: '' }
  const failed = new Set((data.failDetails || []).map(row => String(row.id)))
  return {
    open: true,
    reason,
    targets: targets.filter(row => failed.has(String(row.id))),
    requiresReselect: true,
    error: `${batchReceipt(data)}。原因已保留，成功项已移除。列表已刷新；请返回列表重新选择失败项，禁止按旧版本直接重发。`
  }
}
export function createReasonDrafts(state = {}) {
  return {
    get(id) { return state[id] || '' },
    set(id, reason) { state[id] = reason },
    clear(id) { delete state[id] }
  }
}

export async function sendReasonDraft(drafts, id, send) {
  const reason = drafts.get(id)
  if (!reason.trim() || reason.length > 500) throw new Error('原因必填，原文最多500字')
  await send(id, reason)
  drafts.clear(id)
}

// Component-local memory only: closing is not an acknowledgment of an uncertain POST.
export function createNoteDrafts(state = {}, makeKey = () => globalThis.crypto.randomUUID()) {
  const get = scope => {
    if (!state[scope]) state[scope] = { form: { adjustmentAmount: '', handledAt: '', reason: '', referenceNo: '', attachmentRef: '', noteText: '' }, requests: new Map() }
    return state[scope]
  }
  return {
    get,
    request(scope) {
      const draft = get(scope), body = noteBody(draft.form)
      if (!draft.requests.has(body)) draft.requests.set(body, makeKey())
      return { body, key: draft.requests.get(body) }
    },
    close(scope) { return state[scope] },
    confirm(scope) { delete state[scope] }
  }
}

const auditActionLabels = {
  submit: '提交审核', approve: '终审通过', reject: '驳回汇总',
  sign: '院领导签字（历史）', unlock: '解锁（历史）', teacherConfirm: '教师确认',
  conflict: '操作冲突', review_pass: '逐条审核通过', review_reject: '逐条审核驳回',
  review_waive: '逐条审核豁免', offlineNote: '追加线下补差说明'
}
export function auditActionLabel(action) {
  return Object.hasOwn(auditActionLabels, action) ? auditActionLabels[action] : action ?? '—'
}

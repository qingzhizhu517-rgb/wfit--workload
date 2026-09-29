/**
 * 跨页选择集合逻辑（与页面/Element Plus 解耦，便于用 node:test 单测）
 *
 * 学期汇总列表分页展示，批量导出需要「翻页后保留已选、当前页取消即时生效」。
 * Element Plus 的 selection-change 每次只给出当前页的勾选结果，因此以汇总主键
 * id 为键维护一个全局 Map：合并时先剔除当前页所有行（无论是否勾选），再塞入当前页
 * 选中的行，从而只替换当前页、保留其他页。
 *
 * 键必须用「每行唯一」的汇总主键 id，而非 userId：清空学期筛选后列表可能跨多学期，
 * 同一教师会出现多行、userId 相同——若以 userId 为键会被折叠，导致计数错误。以 id
 * 为键各行互不干扰；导出/核算按教师维度调用时再用 distinctUserIds 折算回教师并去重。
 *
 * 筛选范围（学期/教师/状态）变化时应由调用方 clear()，防止把不同学期/筛选范围混进
 * 同一批次；批量导出/核算本身是「单学期」操作，调用方需保证仅传当前学期的行。
 */

/**
 * 合并当前页选择到全局已选集合。
 * @param {Map<number, object>} existing 现有全局已选（键为汇总主键 id）
 * @param {Array<{id:number}>} pageRows 当前页全部行
 * @param {Array<{id:number}>} selectedRows 当前页被勾选的行
 * @returns {Map<number, object>} 新的全局已选集合（不修改入参）
 */
export function mergePageSelection(existing, pageRows, selectedRows) {
  const next = new Map(existing)
  pageRows.forEach(row => next.delete(row.id))
  selectedRows.forEach(row => row.id && next.set(row.id, row))
  return next
}

/**
 * 取出全局已选的所有汇总主键 id（用于计数与视觉勾选对齐）。
 * @param {Map<number, object>} selected
 * @returns {number[]}
 */
export const selectedIds = selected => [...selected.keys()]

/**
 * 折算到教师维度：取已选行的 userId，去重并过滤空值（用于批量导出/核算传参）。
 * @param {Map<number, {userId:number}>} selected
 * @returns {number[]}
 */
export const distinctUserIds = selected =>
  [...new Set([...selected.values()].map(row => row.userId).filter(Boolean))]

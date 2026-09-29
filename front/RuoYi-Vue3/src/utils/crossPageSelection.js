/**
 * 跨页选择集合逻辑（与页面/Element Plus 解耦，便于用 node:test 单测）
 *
 * 学期汇总列表分页展示，批量导出需要「翻页后保留已选、当前页取消即时生效」。
 * Element Plus 的 selection-change 每次只给出当前页的勾选结果，因此以 userId
 * 为键维护一个全局 Map：合并时先剔除当前页所有行（无论是否勾选），再塞入当前页
 * 选中的行，从而只替换当前页、保留其他页。筛选范围（学期/教师/状态）变化时应由
 * 调用方 clear()，防止把不同学期/筛选范围混进同一批次。
 */

/**
 * 合并当前页选择到全局已选集合。
 * @param {Map<number, object>} existing 现有全局已选（键为 userId）
 * @param {Array<{userId:number}>} pageRows 当前页全部行
 * @param {Array<{userId:number}>} selectedRows 当前页被勾选的行
 * @returns {Map<number, object>} 新的全局已选集合（不修改入参）
 */
export function mergePageSelection(existing, pageRows, selectedRows) {
  const next = new Map(existing)
  pageRows.forEach(row => next.delete(row.userId))
  selectedRows.forEach(row => row.userId && next.set(row.userId, row))
  return next
}

/**
 * 取出全局已选的所有 userId。
 * @param {Map<number, object>} selected
 * @returns {number[]}
 */
export const selectedIds = selected => [...selected.keys()]

import test from 'node:test'
import assert from 'node:assert/strict'
import { mergePageSelection, selectedIds, distinctUserIds } from '../src/utils/crossPageSelection.js'

test('换页只替换当前页选择并保留其他页（按汇总主键 id 跟踪）', () => {
  let selected = new Map([[11, { id: 11, userId: 1 }], [33, { id: 33, userId: 3 }]])
  // 当前页为 id 11、22，仅勾选 22：应保留其他页的 33，并并入当前页的 22
  selected = mergePageSelection(
    selected,
    [{ id: 11, userId: 1 }, { id: 22, userId: 2 }],
    [{ id: 22, userId: 2 }]
  )
  assert.deepEqual(selectedIds(selected), [33, 22])
})

test('同一教师跨学期两行不再折叠（id 唯一）', () => {
  // 同 userId=1 的两条不同学期汇总（id 101/102），选择应各自保留、不被 userId 折叠
  const selected = mergePageSelection(
    new Map(),
    [
      { id: 101, userId: 1, semester: '2025-2026-1' },
      { id: 102, userId: 1, semester: '2025-2026-2' }
    ],
    [
      { id: 101, userId: 1, semester: '2025-2026-1' },
      { id: 102, userId: 1, semester: '2025-2026-2' }
    ]
  )
  assert.deepEqual(selectedIds(selected), [101, 102])
  // 去重到教师维度仅一位
  assert.deepEqual(distinctUserIds(selected), [1])
})

test('distinctUserIds 去重并过滤空值', () => {
  const selected = new Map([
    [1, { id: 1, userId: 5 }],
    [2, { id: 2, userId: 5 }],
    [3, { id: 3, userId: 7 }],
    [4, { id: 4, userId: null }]
  ])
  assert.deepEqual(distinctUserIds(selected), [5, 7])
})

test('筛选范围变化时由调用方清空', () => {
  const selected = mergePageSelection(new Map(), [{ id: 1, userId: 1 }], [{ id: 1, userId: 1 }])
  selected.clear()
  assert.deepEqual(selectedIds(selected), [])
})

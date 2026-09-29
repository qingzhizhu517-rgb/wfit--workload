import test from 'node:test'
import assert from 'node:assert/strict'
import { mergePageSelection, selectedIds } from '../src/utils/crossPageSelection.js'

test('换页只替换当前页选择并保留其他页', () => {
  let selected = new Map([[1, { userId: 1 }], [3, { userId: 3 }]])
  selected = mergePageSelection(selected, [{ userId: 1 }, { userId: 2 }], [{ userId: 2 }])
  assert.deepEqual(selectedIds(selected), [3, 2])
})

test('筛选范围变化时由调用方清空', () => {
  const selected = mergePageSelection(new Map(), [{ userId: 1 }], [{ userId: 1 }])
  selected.clear()
  assert.deepEqual(selectedIds(selected), [])
})

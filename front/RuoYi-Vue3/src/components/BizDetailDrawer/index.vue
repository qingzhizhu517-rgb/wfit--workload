<template>
  <el-drawer
    v-model="visible"
    :title="title"
    size="460px"
    append-to-body
  >
    <el-descriptions
      :column="1"
      border
    >
      <el-descriptions-item
        v-for="f in fields"
        :key="f.prop"
        :label="f.label"
      >
        <biz-tag
          v-if="f.map"
          :value="row?.[f.prop]"
          :map="f.map"
        />
        <span v-else>{{ formatValue(f, row?.[f.prop]) }}</span>
      </el-descriptions-item>
      <el-descriptions-item
        v-if="showRemark"
        label="备注"
      >
        {{ row?.remark || '-' }}
      </el-descriptions-item>
    </el-descriptions>
  </el-drawer>
</template>

<script setup name="BizDetailDrawer">
import { computed } from 'vue'
import { formatNumber, formatAmount } from '@/utils/bizDict'
import { parseTime } from '@/utils/ruoyi'

/**
 * 通用「查看详情」抽屉：以 el-descriptions 展示一行记录的字段，末尾统一附「备注」。
 * 用于把列表页中大量占位却常为空的备注/系数列收敛进详情，页面更清爽。
 *
 * fields: [{ label, prop, type?, map?, suffix? }]
 *   type: 'number'(默认两位) | 'int'(整数) | 'amount'(金额千分位) | 'time'(日期时间) | 'text'(原样)
 *   map:  传入则用 biz-tag 渲染（枚举/字典）
 *   suffix: 数值后缀，如 '学时' '元'
 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '查看详情' },
  row: { type: Object, default: () => ({}) },
  fields: { type: Array, default: () => [] },
  showRemark: { type: Boolean, default: true }
})
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

function formatValue(f, v) {
  if (v === null || v === undefined || v === '') return '-'
  let out
  switch (f.type) {
  case 'int': out = formatNumber(v, 0); break
  case 'amount': out = formatAmount(v); break
  case 'time': out = parseTime(v) || '-'; break
  case 'text': out = v; break
  default: out = formatNumber(v)
  }
  return f.suffix ? `${out} ${f.suffix}` : out
}
</script>

<template>
  <el-tooltip
    :content="tooltipText"
    placement="top"
    :disabled="!tooltipText"
  >
    <span
      class="factor-chip"
      :class="[`is-${tone}`, { 'has-notice': hasNotice }]"
    >
      <span class="factor-value">{{ formatNumber(factor.value) }}</span>
      <span class="factor-name">{{ factor.key || '-' }}</span>
      <span class="factor-source">{{ sourceLabel }}</span>
    </span>
  </el-tooltip>
</template>

<script setup name="FactorChip">
import { computed } from 'vue'
import { formatNumber } from '@/utils/bizDict'

const props = defineProps({
  factor: { type: Object, required: true }
})

const sourceLabels = {
  // 归一后的来源枚举
  IMPORT_VALUE: '导入值',
  RULE_DEFAULT: '默认规则',
  APPROVED_OVERRIDE: '审批调整',
  FORMULA_CONSTANT: '制度常量',
  DERIVED: '派生值',
  DISPLAY_ONLY: '仅展示',
  // 旧枚举兜底（存量数据/兼容）
  IMPORT: '任务导入',
  AUTO: '系统自动',
  MANUAL: '手工录入',
  SELF: '教师申报',
  CURRENT_RULE_DERIVED: '派生值',
  UNKNOWN: '来源未说明'
}

const sourceLabel = computed(() => sourceLabels[props.factor.source] || props.factor.source || '来源未说明')
const hasNotice = computed(() => ['WARNING', 'CAPPED', 'APPROVAL_REQUIRED', 'PENDING', 'ERROR'].includes(props.factor.status))
const tone = computed(() => {
  if (['PENDING', 'ERROR'].includes(props.factor.status)) return 'danger'
  if (['WARNING', 'CAPPED', 'APPROVAL_REQUIRED'].includes(props.factor.status)) return 'warning'
  if (props.factor.source === 'APPROVED_OVERRIDE') return 'primary'
  if (['IMPORT', 'IMPORT_VALUE'].includes(props.factor.source)) return 'primary'
  if (props.factor.source === 'SELF') return 'warning'
  return 'neutral'
})
const tooltipText = computed(() => {
  const parts = [sourceLabel.value]
  if (props.factor.description) parts.push(props.factor.description)
  if (props.factor.status === 'WARNING') parts.push('该因子超过制度阈值，仅提示核查，不截断工作量')
  if (props.factor.status === 'CAPPED') parts.push('该因子已按制度上限参与核算')
  if (props.factor.status === 'APPROVAL_REQUIRED') parts.push('该因子超出申报阈值，需履行报批手续')
  return parts.join('：')
})
</script>

<style scoped>
.factor-chip {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  min-width: 72px;
  padding: var(--wfit-space-xs) var(--wfit-space-sm);
  border: 1px solid var(--el-border-color-lighter);
  border-left: 3px solid var(--el-text-color-placeholder);
  border-radius: var(--wfit-radius-sm);
  background: var(--el-bg-color);
  cursor: help;
}
.factor-chip.is-primary { border-left-color: var(--el-color-primary); }
.factor-chip.is-warning { border-left-color: var(--el-color-warning); }
.factor-chip.is-danger { border-left-color: var(--el-color-danger); }
.factor-chip.has-notice { background: var(--el-color-warning-light-9); }
.factor-value { font-size: var(--wfit-font-md); font-weight: 600; color: var(--el-text-color-primary); }
.factor-name { font-size: var(--wfit-font-xs); color: var(--el-text-color-regular); }
.factor-source { font-size: 10px; color: var(--el-text-color-secondary); }
</style>

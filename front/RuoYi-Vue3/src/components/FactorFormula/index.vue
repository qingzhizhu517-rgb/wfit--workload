<template>
  <section
    v-if="formula"
    class="factor-formula"
    aria-label="工作量计算口径"
  >
    <div class="ff-head">
      <div>
        <div class="ff-kicker">
          {{ formula.reproducible === false ? '当前规则参考' : '计算口径' }}
        </div>
        <div class="ff-expression">
          {{ formula.expression || formula.formulaType || itemType }}
        </div>
      </div>
      <div class="ff-result">
        {{ formatNumber(formula.result) }} <span class="ff-unit">学时</span>
      </div>
    </div>

    <template v-if="formula.factors?.length">
      <div
        v-if="calculationFactors.length"
        class="ff-chain"
      >
        <template
          v-for="(factor, index) in calculationFactors"
          :key="`${factor.key}-${index}`"
        >
          <span
            v-if="index > 0"
            class="ff-op"
            aria-hidden="true"
          >×</span>
          <factor-chip :factor="factor" />
        </template>
      </div>
      <div
        v-if="displayOnlyFactors.length"
        class="ff-reference"
      >
        <span class="ff-reference-label">{{ referenceLabel }}</span>
        <factor-chip
          v-for="(factor, index) in displayOnlyFactors"
          :key="`${factor.key}-${index}`"
          :factor="factor"
        />
      </div>
    </template>
    <el-empty
      v-else
      description="该计算口径暂未返回因子明细"
      :image-size="48"
    />

    <div
      v-if="formula.description"
      class="ff-note"
    >
      {{ formula.description }}
    </div>
  </section>

  <div
    v-else-if="loading"
    class="factor-formula ff-state"
  >
    <el-skeleton
      :rows="2"
      animated
    />
  </div>

  <el-alert
    v-else-if="error"
    type="error"
    :title="error"
    :closable="false"
    show-icon
    class="factor-formula"
  />

  <div
    v-else
    class="factor-formula ff-state"
  >
    {{ emptyText }}
  </div>
</template>

<script setup name="FactorFormula">
import { computed } from 'vue'
import FactorChip from '@/components/FactorChip/index.vue'
import { formatNumber } from '@/utils/bizDict'

const props = defineProps({
  itemType: { type: String, default: '' },
  formula: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' }
})

const calculationFactors = computed(() => {
  if (props.formula?.reproducible === false) return []
  return props.formula?.factors?.filter(factor => factor.status !== 'DISPLAY_ONLY') || []
})
const displayOnlyFactors = computed(() => {
  const factors = props.formula?.factors || []
  if (props.formula?.reproducible === false) return factors
  return factors.filter(factor => factor.status === 'DISPLAY_ONLY')
})
const referenceLabel = computed(() => props.formula?.reproducible === false
  ? '参考因子（未保存历史规则快照，不作为结果复算式）'
  : '仅展示，不参与计算')

const emptyText = computed(() => {
  const text = {
    G7: 'G7 为 G1 至 G6 教学明细的汇总，不对应单条因子算式。',
    G8: 'G8 为第二课堂工作量申报项，以核定学时和申报说明为准。',
    G9: 'G9 为其他工作量申报项，以核定学时和申报说明为准。',
    G11: 'G11 直接同步教务确认的本学期岗位减免工作量，学期累计在汇总时按 180 封顶。'
  }
  return text[props.itemType] || '当前明细没有可展示的计算因子。'
})
</script>

<style scoped>
.factor-formula {
  padding: var(--wfit-space-md);
  margin-bottom: var(--wfit-space-md);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--wfit-radius-md);
  background: var(--el-fill-color-lighter);
}
.ff-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--wfit-space-md);
  margin-bottom: var(--wfit-space-sm);
}
.ff-kicker { font-size: var(--wfit-font-xs); color: var(--el-text-color-secondary); }
.ff-expression {
  margin-top: 2px;
  color: var(--el-text-color-primary);
  font-family: Consolas, Monaco, monospace;
  font-size: var(--wfit-font-md);
  overflow-wrap: anywhere;
}
.ff-result { flex-shrink: 0; font-size: 18px; font-weight: 700; color: var(--el-color-primary); }
.ff-unit { font-size: var(--wfit-font-xs); font-weight: 400; color: var(--el-text-color-secondary); }
.ff-chain { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; }
.ff-op { color: var(--el-text-color-secondary); font-weight: 600; }
.ff-reference {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: var(--wfit-space-sm);
}
.ff-reference-label {
  color: var(--el-text-color-secondary);
  font-size: var(--wfit-font-xs);
}
.ff-note {
  margin-top: var(--wfit-space-sm);
  padding-top: var(--wfit-space-sm);
  border-top: 1px dashed var(--el-border-color);
  color: var(--el-text-color-secondary);
  font-size: var(--wfit-font-xs);
  line-height: 1.6;
}
.ff-state { color: var(--el-text-color-secondary); font-size: 13px; }
@media (max-width: 600px) {
  .ff-head { flex-direction: column; }
  .ff-result { align-self: flex-end; }
}
</style>

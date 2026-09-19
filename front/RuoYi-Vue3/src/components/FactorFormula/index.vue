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

    <!-- 课程信息：解释因子取值依据的原始课程属性 -->
    <div
      v-if="sourceTask"
      class="ff-course"
    >
      <div class="ff-course-title">
        课程信息
      </div>
      <div class="ff-course-grid">
        <span>课程级别：{{ sourceTask.courseLevel || '-' }}</span>
        <span>课程角色：{{ sourceTask.courseRole || '-' }}</span>
        <span>课程性质：{{ sourceTask.courseNature || '-' }}</span>
        <span>班级：{{ sourceTask.className || '-' }}</span>
        <span>重复次序：{{ sourceTask.repeatOrder != null ? sourceTask.repeatOrder : '-' }}</span>
      </div>
      <div
        v-if="q2Reason"
        class="ff-course-reason"
      >
        {{ q2Reason }}
      </div>
    </div>

    <!-- 系数申请入口：G1/G2 的可申请因子 -->
    <div
      v-if="adjustableFactors.length"
      class="ff-adjust"
    >
      <span class="ff-adjust-label">可申请调整：</span>
      <el-button
        v-for="factor in adjustableFactors"
        :key="factor.key"
        size="small"
        type="primary"
        plain
        @click="emitAdjust(factor)"
      >
        {{ factor.key }} 申请调整
      </el-button>
    </div>

    <!-- 数据溯源：内部 ID 与快照信息，默认折叠 -->
    <el-collapse
      v-if="hasTrace"
      class="ff-trace"
    >
      <el-collapse-item
        title="数据溯源"
        name="trace"
      >
        <div class="ff-trace-grid">
          <span>明细ID：{{ item?.id || '-' }}</span>
          <span>教学任务ID：{{ item?.taskId || '-' }}</span>
          <span>岗位任职ID：{{ item?.assignmentId || '-' }}</span>
          <span v-if="formula.snapshotVersion != null">快照版本：v{{ formula.snapshotVersion }}</span>
          <span v-if="formula.calculatedAt">固化时间：{{ formula.calculatedAt }}</span>
          <span
            v-if="formula.snapshotHash"
            class="ff-trace-hash"
          >快照哈希：{{ formula.snapshotHash }}</span>
        </div>
        <div
          v-if="formula.legacy"
          class="ff-trace-legacy"
        >
          该明细无计算快照，展示口径读回当前子表，可信但无法逐因子复现历史核算。
        </div>
      </el-collapse-item>
    </el-collapse>
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
  error: { type: String, default: '' },
  // 明细主记录：提供 id/taskId/assignmentId 供「数据溯源」区展示
  item: { type: Object, default: null }
})

const emit = defineEmits(['adjust'])

// G1/G2 允许教师申请调整的因子（与后端 CoefficientAdjustment 白名单一致）
const ADJUSTABLE = {
  G1: ['C1', 'K1', 'Q1', 'Q2', 'N'],
  G2: ['K', 'C2', 'Q1', 'Q2']
}

const sourceTask = computed(() => props.formula?.sourceTask || null)

const q2Reason = computed(() => {
  const level = sourceTask.value?.courseLevel
  if (!level) return ''
  return `Q2（课程质量系数）依据课程级别「${level}」取值`
})

const adjustableFactors = computed(() => {
  const allowed = ADJUSTABLE[props.itemType]
  if (!allowed) return []
  return (props.formula?.factors || []).filter(
    factor => factor.status !== 'DISPLAY_ONLY' && allowed.includes(factor.key)
  )
})

const hasSnapshotVersion = computed(() => {
  const v = props.formula?.snapshotVersion
  return v !== null && v !== undefined
})
const hasTrace = computed(() => Boolean(
  props.item?.id || props.item?.taskId || props.item?.assignmentId ||
  hasSnapshotVersion.value || props.formula?.snapshotHash || props.formula?.legacy
))

function emitAdjust(factor) {
  emit('adjust', {
    category: props.itemType,
    factorCode: factor.key,
    oldValue: factor.value,
    factorLabel: factor.description || factor.key
  })
}

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
.ff-course {
  margin-top: var(--wfit-space-sm);
  padding-top: var(--wfit-space-sm);
  border-top: 1px dashed var(--el-border-color);
}
.ff-course-title { font-size: var(--wfit-font-xs); color: var(--el-text-color-secondary); margin-bottom: 4px; }
.ff-course-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  font-size: var(--wfit-font-xs);
  color: var(--el-text-color-regular);
}
.ff-course-reason { margin-top: 4px; font-size: var(--wfit-font-xs); color: var(--el-text-color-secondary); }
.ff-adjust {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: var(--wfit-space-sm);
}
.ff-adjust-label { font-size: var(--wfit-font-xs); color: var(--el-text-color-secondary); }
.ff-trace { margin-top: var(--wfit-space-sm); }
.ff-trace-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  font-size: var(--wfit-font-xs);
  color: var(--el-text-color-secondary);
}
.ff-trace-hash { overflow-wrap: anywhere; }
.ff-trace-legacy { margin-top: 6px; font-size: var(--wfit-font-xs); color: var(--el-color-warning); }
@media (max-width: 600px) {
  .ff-head { flex-direction: column; }
  .ff-result { align-self: flex-end; }
}
</style>

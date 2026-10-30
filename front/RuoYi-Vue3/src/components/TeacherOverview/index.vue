<template>
  <div
    v-if="row"
    class="teacher-overview"
  >
    <div class="to-left">
      <svg
        class="to-gauge"
        viewBox="0 0 180 180"
        role="img"
        :aria-label="gaugeAriaLabel"
      >
        <title>{{ gaugeAriaLabel }}</title>
        <!-- 背景环 -->
        <circle
          cx="90"
          cy="90"
          r="70"
          fill="none"
          stroke="var(--el-fill-color)"
          stroke-width="14"
        />
        <!-- 已知历史封顶值时绘制进度；缺失时不伪造比例 -->
        <circle
          cx="90"
          cy="90"
          r="70"
          fill="none"
          :stroke="ringColor"
          stroke-width="14"
          stroke-linecap="round"
          :stroke-dasharray="dasharray"
          transform="rotate(-90 90 90)"
        />
        <!-- 额定刻度（180/封顶 占比处） -->
        <line
          v-if="cap !== null && cap > 0"
          v-bind="ratedTick"
          stroke="var(--el-text-color-regular)"
          stroke-width="2.5"
        />
        <text
          v-if="cap !== null && cap > 0"
          :x="ratedText.x"
          :y="ratedText.y"
          class="to-ticktext"
        >额定 {{ rated }}</text>
        <!-- 中心读数 -->
        <text
          x="90"
          y="86"
          text-anchor="middle"
          class="to-num"
        >{{ formatNumber(row.totalWorkload) }}</text>
        <text
          x="90"
          y="104"
          text-anchor="middle"
          class="to-numlabel"
        >总工作量(学时)</text>
        <text
          x="90"
          y="122"
          text-anchor="middle"
          class="to-sub"
        >超额 {{ formatNumber(row.excessWorkload) }} ｜ {{ cap === null ? '历史阈值缺失' : '封顶 ' + formatNumber(cap) }}</text>
      </svg>
      <div class="to-scale-note">
        <span>额定 {{ formatNumber(rated) }}</span>
        <span>{{ cap === null ? '历史阈值缺失' : '绩效封顶 ' + formatNumber(cap) }}</span>
      </div>
    </div>

    <div class="to-right">
      <div class="to-row to-seg">
        <span class="to-seglabel">构成</span>
        <div class="to-bar">
          <div
            v-for="seg in segments"
            :key="seg.label"
            class="to-segfill"
            :style="{ width: seg.pct + '%', background: seg.color }"
            :title="`${seg.label} ${formatNumber(seg.value)} 学时`"
          />
        </div>
        <span class="to-segval">{{ formatNumber(row.totalWorkload) }}</span>
      </div>
      <div class="to-legend">
        <span
          v-for="seg in segments"
          :key="'lg' + seg.label"
          class="ff-item"
        ><i
          class="to-dot"
          :style="{ background: seg.color }"
        />{{ seg.label }} {{ formatNumber(seg.value) }}</span>
      </div>

      <div class="to-row to-metrics">
        <div class="to-metric">
          <div class="to-mval">
            {{ formatAmount(row.performancePay) }}
          </div>
          <div class="to-mlabel">
            绩效酬金(元)
            <el-tooltip
              v-if="row.isCapped === 1"
              :content="cap === null ? '历史记录已标记触顶，但未保存当时的封顶阈值' : `总工作量已触 ${cap} 学时绩效上限，酬金按封顶值计发`"
              placement="top"
            >
              <el-tag
                type="danger"
                size="small"
                disable-transitions
              >
                {{ cap === null ? '触顶记录' : '封顶' }}
              </el-tag>
            </el-tooltip>
          </div>
        </div>
        <div class="to-metric">
          <div class="to-mval">
            {{ formatNumber(row.basicTeachingStandard) }}
          </div>
          <div class="to-mlabel">
            基本教学量标准(学时)
          </div>
        </div>
        <div class="to-metric">
          <div class="to-mval">
            <el-tag
              :type="row.basicTeachingMet === 1 ? 'success' : 'warning'"
              disable-transitions
            >
              {{ row.basicTeachingMet === 1 ? '已达标' : '未达标' }}
            </el-tag>
          </div>
          <div class="to-mlabel">
            基本教学量（三门理论课视同达标）
          </div>
        </div>
      </div>

      <div
        v-if="remarkText"
        class="to-remark"
      >
        <span class="to-remark-label">备注 / 审核说明</span>
        <span>{{ remarkText }}</span>
      </div>
    </div>
  </div>
</template>

<script setup name="TeacherOverview">
import { computed } from 'vue'
import { formatNumber, formatAmount } from '@/utils/bizDict'

/**
 * 教师总览卡：额定环形进度 + 工作量构成堆叠条 + 绩效与达标信息。
 *
 * 数据直接取汇总行（biz_workload_summary 落库值，与表格/导出同源）。
 * 绩效封顶只取保存的结算依据，缺失时明确提示，不参与前端核算。
 */
const props = defineProps({
  row: { type: Object, default: null }
})

const cap = computed(() => {
  const raw = props.row?.workloadCap
  if (raw === null || raw === undefined || raw === '') return null
  const value = Number(raw)
  return Number.isFinite(value) ? value : null
})
const rated = computed(() => {
  const value = Number(props.row?.ratedWorkload)
  return Number.isFinite(value) && value >= 0 ? value : 180
})
const remarkText = computed(() => {
  const value = props.row?.remark
  return value && value !== '-' ? String(value).trim() : ''
})
const gaugeAriaLabel = computed(() => {
  const total = formatNumber(props.row?.totalWorkload)
  const capText = cap.value === null ? '历史封顶阈值缺失' : `绩效封顶 ${formatNumber(cap.value)} 学时`
  return `学期总工作量 ${total} 学时，额定 ${formatNumber(rated.value)} 学时，${capText}`
})

// 环形几何：只在封顶依据完整时绘制有意义的比例
const CIRC = 2 * Math.PI * 70
const dasharray = computed(() => {
  const total = Number(props.row?.totalWorkload) || 0
  const pct = cap.value !== null && cap.value > 0 ? Math.max(0, Math.min(total / cap.value, 1)) : 0
  return `${(pct * CIRC).toFixed(1)} ${CIRC.toFixed(1)}`
})
const ringColor = computed(() => {
  const total = Number(props.row?.totalWorkload) || 0
  if (cap.value !== null && total >= cap.value) return 'var(--el-color-danger)'
  if (total >= rated.value) return 'var(--el-color-primary)'
  return 'var(--el-color-info)'
})

// 额定刻度线位置（顶部为 0，顺时针）
const ratedTick = computed(() => {
  const angle = (Math.min(rated.value / (cap.value > 0 ? cap.value : 1), 1)) * 360
  const rad = (angle - 90) * Math.PI / 180
  const r1 = 62, r2 = 78
  return {
    x1: 90 + r1 * Math.cos(rad), y1: 90 + r1 * Math.sin(rad),
    x2: 90 + r2 * Math.cos(rad), y2: 90 + r2 * Math.sin(rad)
  }
})
const ratedText = computed(() => {
  const angle = Math.min(rated.value / (cap.value > 0 ? cap.value : 1), 1) * 360
  const rad = (angle - 90) * Math.PI / 180
  const r = 88
  const x = 90 + r * Math.cos(rad)
  const y = 90 + r * Math.sin(rad) + 3
  return { x: x > 90 ? x - 4 : (x < 90 ? x + 4 : x - 14), y }
})

// 构成堆叠条：G7/G8/G9/G11（G1~G6 子项不落库，以 G7 整段呈现）
const SEG_COLORS = { G7: 'var(--el-color-primary)', G8: 'var(--el-color-success)', G9: 'var(--el-color-warning)', G11: 'var(--el-color-info)' }
const segments = computed(() => {
  const row = props.row || {}
  const defs = [
    { key: 'G7', label: '第一课堂 G7' },
    { key: 'G8', label: '第二课堂 G8' },
    { key: 'G9', label: '其他 G9' },
    { key: 'G11', label: '管理服务 G11' }
  ]
  const total = Number(row.totalWorkload) || 0
  return defs.map(d => {
    const v = Number(row[d.key]) || 0
    return { label: d.label, value: v, color: SEG_COLORS[d.key], pct: total > 0 ? (v / total * 100) : 0 }
  }).filter(s => s.value > 0)
})
</script>

<style scoped>
.teacher-overview {
  display: flex;
  gap: var(--wfit-space-lg);
  align-items: center;
  padding: 14px 18px;
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--wfit-radius-md);
  background: var(--el-bg-color);
}
.to-left { flex-shrink: 0; text-align: center; }
.to-gauge { width: min(180px, 100%); height: auto; aspect-ratio: 1; }
.to-scale-note {
  display: flex;
  justify-content: space-between;
  gap: var(--wfit-space-sm);
  margin-top: -8px;
  font-size: var(--wfit-font-xs);
  color: var(--el-text-color-secondary);
}
.to-num { font-size: 30px; font-weight: 700; fill: var(--el-text-color-primary); }
.to-numlabel { font-size: 10px; fill: var(--el-text-color-secondary); }
.to-sub { font-size: 9px; fill: var(--el-text-color-secondary); }
.to-ticktext { font-size: 9px; fill: var(--el-text-color-regular); }
.to-right { flex: 1; min-width: 0; }
.to-row { display: flex; align-items: center; gap: 10px; }
.to-seglabel { font-size: 12px; color: var(--el-text-color-secondary); flex-shrink: 0; }
.to-segval { font-size: 12px; font-weight: 600; color: var(--el-text-color-primary); flex-shrink: 0; }
.to-bar {
  flex: 1;
  display: flex;
  height: 18px;
  border-radius: 4px;
  overflow: hidden;
  background: var(--el-fill-color);
}
.to-segfill { height: 100%; min-width: 2px; }
.to-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin: 6px 0 2px 44px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.ff-item { display: inline-flex; align-items: center; gap: 4px; }
.to-dot { display: inline-block; width: 8px; height: 8px; border-radius: 2px; }
.to-metrics {
  display: flex;
  gap: 28px;
  margin-top: 10px;
  margin-left: 44px;
}
.to-metric { display: flex; flex-direction: column; gap: 2px; }
.to-mval { font-size: 18px; font-weight: 700; color: var(--el-text-color-primary); }
.to-mlabel {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.to-remark {
  display: flex;
  gap: var(--wfit-space-sm);
  margin: var(--wfit-space-md) 0 0 44px;
  padding: var(--wfit-space-sm) var(--wfit-space-md);
  border-radius: var(--wfit-radius-sm);
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  font-size: var(--wfit-font-xs);
  line-height: 1.6;
}
.to-remark-label { flex-shrink: 0; font-weight: 600; }

@media (max-width: 768px) {
  .teacher-overview { flex-direction: column; align-items: stretch; }
  .to-left { width: min(220px, 100%); margin: 0 auto; }
  .to-legend, .to-metrics, .to-remark { margin-left: 0; }
  .to-metrics { flex-wrap: wrap; gap: var(--wfit-space-md); }
  .to-metric { min-width: 130px; flex: 1; }
}
</style>

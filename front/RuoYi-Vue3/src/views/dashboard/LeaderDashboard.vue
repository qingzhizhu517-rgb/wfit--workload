<template>
  <div class="dashboard-editor-container">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="24">
        <h2 class="dashboard-title">院领导工作台</h2>
        <p class="dashboard-subtitle">
          当前学期：{{ stats.semester || '2025-2026-1' }}
          <span class="last-updated" v-if="stats.lastUpdated">（数据更新于 {{ stats.lastUpdated }}）</span>
        </p>
      </el-col>
    </el-row>

    <!-- 4 统计卡片 -->
    <el-row :gutter="20" class="panel-group">
      <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
        <div class="card-panel" @click="router.push('/system/workloadSummary')">
          <div class="card-panel-icon-wrapper icon-pending">
            <el-icon :size="36"><Clock /></el-icon>
          </div>
          <div class="card-panel-description">
            <div class="card-panel-text">待签字汇总</div>
            <div class="card-panel-num" :class="{ 'has-alert': pendingSignCount > 0 }">
              {{ pendingSignCount }}
            </div>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
        <div class="card-panel" @click="router.push('/system/workloadSummary')">
          <div class="card-panel-icon-wrapper icon-done">
            <el-icon :size="36"><CircleCheck /></el-icon>
          </div>
          <div class="card-panel-description">
            <div class="card-panel-text">已完结汇总</div>
            <div class="card-panel-num">{{ completedCount }}</div>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
        <div class="card-panel" @click="router.push('/system/workloadSummary')">
          <div class="card-panel-icon-wrapper icon-total">
            <el-icon :size="36"><DataLine /></el-icon>
          </div>
          <div class="card-panel-description">
            <div class="card-panel-text">汇总总数</div>
            <div class="card-panel-num">{{ stats.summaryCount ?? '--' }}</div>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
        <div class="card-panel" @click="router.push('/system/payRecord')">
          <div class="card-panel-icon-wrapper icon-pay">
            <el-icon :size="36"><Money /></el-icon>
          </div>
          <div class="card-panel-description">
            <div class="card-panel-text">绩效酬金总额</div>
            <div class="card-panel-num">¥{{ formatMoney(stats.totalPay) }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 快捷操作 -->
      <el-col :xs="24" :sm="24" :lg="8">
        <el-card class="box-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          <div class="quick-links">
            <el-button type="primary" plain icon="Checked" @click="router.push('/system/workloadSummary')">
              审批学期汇总
            </el-button>
            <el-button type="success" plain icon="Download" @click="handleExportPaySummary">
              导出绩效酬金表
            </el-button>
          </div>
        </el-card>

        <!-- 待签字列表 -->
        <el-card class="box-card todo-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>待签字汇总</span>
              <el-badge :value="pendingSignCount" :hidden="pendingSignCount === 0" />
            </div>
          </template>
          <div class="todo-list" v-loading="loading">
            <div v-for="item in pendingList" :key="item.id" class="todo-item todo-warning"
              @click="router.push('/system/workloadSummary')">
              <el-icon><WarningFilled /></el-icon>
              <span>{{ item.userName }} — {{ item.semester }}</span>
              <el-icon class="arrow-right"><ArrowRight /></el-icon>
            </div>
            <div v-if="pendingSignCount === 0 && !loading" class="todo-item todo-success">
              <el-icon><CircleCheck /></el-icon>
              <span>暂无待签字汇总</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 审批进度统计 -->
      <el-col :xs="24" :sm="24" :lg="16">
        <el-card class="box-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>本学期审批进度</span>
              <el-radio-group v-model="chartView" size="small" @change="renderChart">
                <el-radio-button value="bar">柱状图</el-radio-button>
                <el-radio-button value="pie">饼图</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="chartRef" style="height: 380px" v-loading="chartLoading" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="LeaderDashboard">
import { ref, reactive, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  Clock, CircleCheck, DataLine, Money, Download,
  WarningFilled, ArrowRight
} from '@element-plus/icons-vue'
import { getAdminStats } from '@/api/system/dashboard'
import { listWorkloadSummary } from '@/api/system/workloadSummary'
import { exportPaySummary } from '@/api/system/export'

const router = useRouter()
const { proxy } = getCurrentInstance()

const loading = ref(false)
const chartLoading = ref(false)
const chartRef = ref(null)
const chartView = ref('bar')
let chartInstance = null

const pendingSignCount = ref(0)
const completedCount = ref(0)
const pendingList = ref([])
const auditCounts = reactive({
  draft: 0,
  pending: 0,
  signed: 0,
  completed: 0
})

const stats = reactive({
  taskCount: 0,
  teacherCount: 0,
  summaryCount: 0,
  totalPay: 0,
  semester: '',
  lastUpdated: ''
})

function formatMoney(val) {
  if (val == null) return '--'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function fetchStats() {
  loading.value = true
  try {
    const res = await getAdminStats()
    Object.assign(stats, res.data)
  } catch (e) {
    proxy.$modal.msgError('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

async function fetchPendingList() {
  try {
    // 待签字 = auditStatus=2 (院领导待签)
    const res = await listWorkloadSummary({ auditStatus: 2, pageNum: 1, pageSize: 5 })
    pendingList.value = res.rows || []
    pendingSignCount.value = res.total || 0
  } catch (e) {
    // ignore
  }
}

async function fetchAuditCounts() {
  try {
    const statuses = [
      { key: 'draft', status: 0 },
      { key: 'pending', status: 1 },
      { key: 'signed', status: 2 },
      { key: 'completed', status: 3 }
    ]
    for (const s of statuses) {
      const res = await listWorkloadSummary({ auditStatus: s.status, pageNum: 1, pageSize: 1 })
      auditCounts[s.key] = res.total || 0
    }
    completedCount.value = auditCounts.completed
  } catch (e) {
    // ignore
  }
}

function handleExportPaySummary() {
  proxy.$prompt('请输入学年学期（如 2025-2026-1）', '导出绩效酬金统计表', {
    confirmButtonText: '导出',
    cancelButtonText: '取消',
    inputPattern: /^\d{4}-\d{4}-[12]$/,
    inputErrorMessage: '格式如 2025-2026-1',
    inputPlaceholder: '2025-2026-1'
  }).then(({ value }) => {
    proxy.$modal.loading('正在导出...')
    exportPaySummary({ semester: value }).then(res => {
      const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `绩效酬金统计_${value}.xlsx`
      link.click()
      window.URL.revokeObjectURL(url)
      proxy.$modal.closeLoading()
    }).catch(() => {
      proxy.$modal.closeLoading()
    })
  }).catch(() => {})
}

function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const auditData = [
    { value: auditCounts.draft, name: '填报中', itemStyle: { color: '#909399' } },
    { value: auditCounts.pending, name: '教务助理待审', itemStyle: { color: '#e6a23c' } },
    { value: auditCounts.signed, name: '院领导待签', itemStyle: { color: '#409eff' } },
    { value: auditCounts.completed, name: '已完结', itemStyle: { color: '#67c23a' } }
  ]

  let option
  if (chartView.value === 'pie') {
    option = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{c}条' },
        data: auditData
      }]
    }
  } else {
    option = {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: auditData.map(d => d.name)
      },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        type: 'bar',
        data: auditData,
        barMaxWidth: 60
      }]
    }
  }
  chartInstance.setOption(option, true)
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(async () => {
  await fetchStats()
  await fetchPendingList()
  await fetchAuditCounts()
  renderChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped lang="scss">
.dashboard-editor-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 84px);
}

.dashboard-title {
  margin: 0 0 5px;
  color: #303133;
  font-size: 22px;
}

.dashboard-subtitle {
  color: #909399;
  font-size: 14px;
  margin-bottom: 20px;
  .last-updated {
    margin-left: 10px;
    font-size: 12px;
    color: #c0c4cc;
  }
}

.panel-group {
  margin-bottom: 20px;

  .card-panel-col {
    margin-bottom: 20px;
  }

  .card-panel {
    height: 108px;
    cursor: pointer;
    font-size: 12px;
    position: relative;
    overflow: hidden;
    color: #666;
    background: #fff;
    box-shadow: 0 2px 12px rgba(0, 0, 0, .06);
    border-radius: 8px;
    display: flex;
    align-items: center;
    padding: 0 20px;
    transition: transform 0.2s, box-shadow 0.2s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 20px rgba(0, 0, 0, .1);
      .card-panel-icon-wrapper { color: #fff; }
      .icon-pending { background: #e6a23c; }
      .icon-done { background: #67c23a; }
      .icon-total { background: #409eff; }
      .icon-pay { background: #f4516c; }
    }

    .card-panel-icon-wrapper {
      padding: 14px;
      transition: all 0.38s ease-out;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .icon-pending { color: #e6a23c; }
    .icon-done { color: #67c23a; }
    .icon-total { color: #409eff; }
    .icon-pay { color: #f4516c; }

    .card-panel-description {
      margin-left: auto;
      font-weight: bold;
      text-align: right;

      .card-panel-text {
        line-height: 18px;
        color: rgba(0, 0, 0, 0.45);
        font-size: 15px;
        margin-bottom: 10px;
      }

      .card-panel-num {
        font-size: 28px;
        color: #303133;
        &.has-alert {
          color: #ff4949;
        }
      }
    }
  }
}

.box-card {
  margin-bottom: 20px;
  border-radius: 8px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
    font-size: 15px;
    color: #303133;
  }
}

.quick-links {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;

  .el-button {
    margin: 0;
    width: 100%;
    height: 54px;
    font-size: 14px;
    justify-content: center;
  }
}

.todo-card {
  .todo-list {
    min-height: 100px;
  }

  .todo-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    margin-bottom: 8px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    transition: background 0.2s;
    border-left: 3px solid transparent;

    .arrow-right {
      margin-left: auto;
      color: #c0c4cc;
    }

    &:hover {
      background: #f5f7fa;
    }

    &.todo-warning {
      background: #fef0f0;
      color: #e6a23c;
      border-left-color: #e6a23c;
      &:hover { background: #fde2e2; }
    }
    &.todo-success {
      background: #f0fdf4;
      color: #67c23a;
      border-left-color: #67c23a;
    }
  }
}
</style>

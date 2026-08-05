<template>
  <div class="dashboard-editor-container">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="24">
        <h2 class="dashboard-title">教学工作量智能化管理系统</h2>
        <p class="dashboard-subtitle">
          当前学期：{{ stats.semester || '2025-2026-1' }}
          <span class="last-updated" v-if="stats.lastUpdated">（数据更新于 {{ stats.lastUpdated }}）</span>
        </p>
      </el-col>
    </el-row>

    <!-- 4 统计卡片 -->
    <el-row :gutter="20" class="panel-group">
      <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
        <div class="card-panel" @click="router.push('/workload/teachingTask')">
          <div class="card-panel-icon-wrapper icon-tasks">
            <el-icon :size="36"><Document /></el-icon>
          </div>
          <div class="card-panel-description">
            <div class="card-panel-text">教学任务数</div>
            <div class="card-panel-num">{{ stats.taskCount ?? '--' }}</div>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
        <div class="card-panel" @click="router.push('/workload/workloadItem')">
          <div class="card-panel-icon-wrapper icon-workload">
            <el-icon :size="36"><DataLine /></el-icon>
          </div>
          <div class="card-panel-description">
            <div class="card-panel-text">已核算总工作量</div>
            <div class="card-panel-num">{{ formatNumber(stats.totalWorkload) }}</div>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
        <div class="card-panel" @click="router.push('/workload/teacherProfile')">
          <div class="card-panel-icon-wrapper icon-teacher">
            <el-icon :size="36"><User /></el-icon>
          </div>
          <div class="card-panel-description">
            <div class="card-panel-text">参与核算教师</div>
            <div class="card-panel-num">{{ stats.teacherCount ?? '--' }}</div>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
        <div class="card-panel" @click="router.push('/workload/workloadItem?appealStatus=1')">
          <div class="card-panel-icon-wrapper icon-warning">
            <el-icon :size="36"><Warning /></el-icon>
          </div>
          <div class="card-panel-description">
            <div class="card-panel-text">待处理异议</div>
            <div class="card-panel-num" :class="{ 'has-appeal': stats.appealCount > 0 }">
              {{ stats.appealCount ?? '--' }}
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 快捷操作 + 待办事项 -->
      <el-col :xs="24" :sm="24" :lg="8">
        <el-card class="box-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          <div class="quick-links">
            <el-button type="primary" plain icon="Upload" @click="router.push('/workload/importBatch')">
              导入教务处Excel
            </el-button>
            <el-button type="success" plain icon="Edit" @click="router.push('/workload/workloadItem')">
              录入特殊工作量
            </el-button>
            <el-button type="warning" plain icon="Download" @click="handleExportPaySummary">
              导出绩效酬金统计表
            </el-button>
            <el-button type="info" plain icon="Setting" @click="router.push('/workload/workloadRule')">
              基础系数配置
            </el-button>
          </div>
        </el-card>

        <el-card class="box-card todo-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>待办事项</span>
              <el-badge :value="stats.appealCount" :hidden="!stats.appealCount" />
            </div>
          </template>
          <div class="todo-list" v-loading="loading">
            <div v-if="stats.appealCount > 0" class="todo-item todo-warning" @click="router.push('/workload/workloadItem?appealStatus=1')">
              <el-icon><WarningFilled /></el-icon>
              <span>{{ stats.appealCount }} 条工作量异议待处理</span>
              <el-icon class="arrow-right"><ArrowRight /></el-icon>
            </div>
            <div class="todo-item todo-info" @click="router.push('/workload/workloadSummary')">
              <el-icon><Clock /></el-icon>
              <span>{{ stats.summaryCount ?? 0 }} 条学期汇总记录</span>
              <el-icon class="arrow-right"><ArrowRight /></el-icon>
            </div>
            <div class="todo-item todo-success" v-if="stats.totalExcess > 0" @click="router.push('/workload/payRecord')">
              <el-icon><Money /></el-icon>
              <span>超工作量酬金合计 ¥{{ formatMoney(stats.totalPay) }}</span>
              <el-icon class="arrow-right"><ArrowRight /></el-icon>
            </div>
            <el-empty v-if="!stats.appealCount && !stats.summaryCount && !stats.totalExcess"
              description="暂无待办事项" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <!-- ECharts 图表 -->
      <el-col :xs="24" :sm="24" :lg="16">
        <el-card class="box-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>各学院教学任务概况</span>
              <el-radio-group v-model="chartView" size="small" @change="renderChart">
                <el-radio-button value="bar">柱状图</el-radio-button>
                <el-radio-button value="line">折线图</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="chartRef" style="height: 380px" v-loading="chartLoading" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="AdminDashboard">
import { ref, reactive, onMounted, onUnmounted, nextTick, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  Document, DataLine, User, Warning, Upload, Edit,
  Download, Setting, WarningFilled, ArrowRight, Clock, Money
} from '@element-plus/icons-vue'
import { getAdminStats, getCollegeStats } from '@/api/system/dashboard'
import { exportPaySummary } from '@/api/system/export'

const router = useRouter()
const { proxy } = getCurrentInstance()

const loading = ref(false)
const chartLoading = ref(false)
const chartRef = ref(null)
const chartView = ref('bar')
let chartInstance = null

const stats = reactive({
  taskCount: 0,
  itemCount: 0,
  teacherCount: 0,
  totalWorkload: 0,
  appealCount: 0,
  summaryCount: 0,
  totalExcess: 0,
  totalPay: 0,
  semester: '',
  lastUpdated: ''
})

function formatNumber(val) {
  if (val == null) return '--'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

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

async function fetchCollegeStats() {
  chartLoading.value = true
  try {
    const res = await getCollegeStats()
    renderChart(res.data || [])
  } catch (e) {
    // fallback: 用已有 stats 画概况图
    renderChart(null)
  } finally {
    chartLoading.value = false
  }
}

function renderChart(collegeData) {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  let option
  if (collegeData && collegeData.length > 0) {
    const names = collegeData.map(d => d.deptName)
    const tasks = collegeData.map(d => d.taskCount)
    const items = collegeData.map(d => d.itemCount)
    option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      legend: {
        data: ['教学任务数', '已核算明细数'],
        top: 0
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: names,
        axisLabel: { rotate: names.length > 6 ? 30 : 0 }
      },
      yAxis: {
        type: 'value',
        minInterval: 1
      },
      series: [
        {
          name: '教学任务数',
          type: chartView.value,
          data: tasks,
          itemStyle: { color: '#40c9c6' },
          barMaxWidth: 40
        },
        {
          name: '已核算明细数',
          type: chartView.value,
          data: items,
          itemStyle: { color: '#36a3f7' },
          barMaxWidth: 40
        }
      ]
    }
  } else {
    // fallback: 概况柱
    option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: ['教学任务', '已核算明细', '参与教师', '学期汇总', '待处理异议']
      },
      yAxis: {
        type: 'value',
        minInterval: 1
      },
      series: [
        {
          name: '统计概览',
          type: chartView.value,
          data: [
            { value: stats.taskCount ?? 0, itemStyle: { color: '#40c9c6' } },
            { value: stats.itemCount ?? 0, itemStyle: { color: '#36a3f7' } },
            { value: stats.teacherCount ?? 0, itemStyle: { color: '#f4516c' } },
            { value: stats.summaryCount ?? 0, itemStyle: { color: '#34d399' } },
            { value: stats.appealCount ?? 0, itemStyle: { color: '#e6a23c' } }
          ],
          barMaxWidth: 50
        }
      ]
    }
  }
  chartInstance.setOption(option, true)
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(async () => {
  await fetchStats()
  await fetchCollegeStats()
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
      .card-panel-icon-wrapper {
        color: #fff;
      }
      .icon-tasks { background: #40c9c6; }
      .icon-workload { background: #36a3f7; }
      .icon-teacher { background: #f4516c; }
      .icon-warning { background: #e6a23c; }
    }

    .card-panel-icon-wrapper {
      padding: 14px;
      transition: all 0.38s ease-out;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .icon-tasks { color: #40c9c6; }
    .icon-workload { color: #36a3f7; }
    .icon-teacher { color: #f4516c; }
    .icon-warning { color: #e6a23c; }

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
        &.has-appeal {
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
    &.todo-info {
      background: #f0f9ff;
      color: #409eff;
      border-left-color: #409eff;
      &:hover { background: #e1f3ff; }
    }
    &.todo-success {
      background: #f0fdf4;
      color: #67c23a;
      border-left-color: #67c23a;
      &:hover { background: #dcfce7; }
    }
  }
}
</style>

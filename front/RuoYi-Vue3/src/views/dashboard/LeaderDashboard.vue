<template>
  <div class="dashboard-container">
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
        <div class="card-panel" @click="router.push('/workload/workloadSummary')">
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
        <div class="card-panel" @click="router.push('/workload/workloadSummary')">
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
        <div class="card-panel" @click="router.push('/workload/workloadSummary')">
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
        <div class="card-panel" @click="router.push('/workload/payRecord')">
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
            <el-button type="primary" plain icon="Checked" @click="router.push('/workload/workloadSummary')">
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
              @click="router.push('/workload/workloadSummary')">
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Clock, CircleCheck, DataLine, Money, Download,
  WarningFilled, ArrowRight
} from '@element-plus/icons-vue'
import { getAdminStats } from '@/api/system/dashboard'
import { listWorkloadSummary } from '@/api/system/workloadSummary'
import { useDashboard } from '@/composable/useDashboard'

const router = useRouter()

const {
  chartLoading, chartRef, chartView, auditCounts, formatMoney,
  fetchAuditCounts, handleExportPaySummary, renderChart, setupChart
} = useDashboard()

setupChart()

const loading = ref(false)
const pendingSignCount = ref(0)
const completedCount = ref(0)
const pendingList = ref([])

const stats = reactive({
  taskCount: 0,
  teacherCount: 0,
  summaryCount: 0,
  totalPay: 0,
  semester: '',
  lastUpdated: ''
})

async function fetchStats() {
  loading.value = true
  try {
    const res = await getAdminStats()
    Object.assign(stats, res.data)
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

async function fetchPendingList() {
  try {
    const res = await listWorkloadSummary({ status: 2, pageNum: 1, pageSize: 5 })
    pendingList.value = res.rows || []
    pendingSignCount.value = res.total || 0
  } catch (e) {
    // ignore
  }
}

onMounted(async () => {
  await fetchStats()
  await fetchPendingList()
  await fetchAuditCounts()
  completedCount.value = auditCounts.completed
  renderChart()
})
</script>

<style scoped lang="scss">
@use '@/styles/dashboard.scss';
</style>

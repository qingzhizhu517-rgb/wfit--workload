<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="24">
        <h2 class="dashboard-title">教务工作台</h2>
        <p class="dashboard-subtitle">
          当前学期：{{ stats.semester || fallbackSemester }}
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
            <div class="card-panel-text">待审核汇总</div>
            <div class="card-panel-num" :class="{ 'has-alert': pendingCount > 0 }">
              {{ pendingCount }}
            </div>
          </div>
        </div>
      </el-col>

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
            <el-button type="primary" plain icon="Upload" @click="router.push('/workload/teachingTask')">
              导入教学任务
            </el-button>
            <el-button type="success" plain icon="Checked" @click="router.push('/workload/workloadSummary')">
              审核汇总
            </el-button>
            <el-button type="warning" plain icon="Download" @click="handleExportPaySummary">
              导出绩效酬金表
            </el-button>
            <el-button type="info" plain icon="Document" @click="router.push('/workload/workloadItem')">
              工作量明细
            </el-button>
          </div>
        </el-card>

        <!-- 待审核列表 -->
        <el-card class="box-card todo-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>待审核汇总</span>
              <el-badge :value="pendingCount" :hidden="pendingCount === 0" />
            </div>
          </template>
          <div class="todo-list" v-loading="loading">
            <div v-for="item in pendingList" :key="item.id" class="todo-item todo-warning"
              @click="router.push('/workload/workloadSummary')">
              <el-icon><WarningFilled /></el-icon>
              <span>{{ item.userName }} — {{ item.semester }}（教务助理待审）</span>
              <el-icon class="arrow-right"><ArrowRight /></el-icon>
            </div>
            <div v-if="pendingCount === 0 && !loading" class="todo-item todo-success">
              <el-icon><CircleCheck /></el-icon>
              <span>暂无待审核汇总</span>
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

<script setup name="JiaoWuDashboard">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import {
  Document, User, Clock, Money, Upload, Checked, Download,
  WarningFilled, ArrowRight, CircleCheck
} from '@element-plus/icons-vue'
import { getAdminStats } from '@/api/system/dashboard'
import { listWorkloadSummary } from '@/api/system/workloadSummary'
import { useDashboard } from '@/composable/useDashboard'
import { getCurrentSemester } from '@/utils/bizDict'

const router = useRouter()
const { proxy } = getCurrentInstance()

/** 学期兜底：后端未返回学期时按当前日期推算，避免硬编码 */
const fallbackSemester = getCurrentSemester()

const {
  chartLoading, chartRef, chartView, formatMoney,
  fetchAuditCounts, handleExportPaySummary, renderChart, setupChart
} = useDashboard()

setupChart()

const loading = ref(false)
const pendingCount = ref(0)
const pendingList = ref([])

const stats = reactive({
  taskCount: 0,
  itemCount: 0,
  teacherCount: 0,
  totalWorkload: 0,
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
    proxy.$modal.msgError('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

async function fetchPendingList() {
  try {
    const res = await listWorkloadSummary({ status: 1, pageNum: 1, pageSize: 5 })
    pendingList.value = res.rows || []
    pendingCount.value = res.total || 0
  } catch (e) {
    proxy.$modal.msgError('获取待审核汇总失败')
  }
}

onMounted(async () => {
  await fetchStats()
  await fetchPendingList()
  await fetchAuditCounts()
  renderChart()
})
</script>

<style scoped lang="scss">
@use '@/styles/dashboard.scss';
</style>

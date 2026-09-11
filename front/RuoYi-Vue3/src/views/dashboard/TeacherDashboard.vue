<template>
  <div class="teacher-dashboard">
    <!-- 欢迎区 -->
    <el-row
      :gutter="20"
      class="mb20"
    >
      <el-col :span="24">
        <el-card
          shadow="hover"
          class="welcome-card"
        >
          <div class="user-info">
            <el-avatar
              :size="56"
              :src="userStore.avatar"
            />
            <div class="info-text">
              <h2>{{ userStore.nickName || userStore.name }} 老师，您好</h2>
              <p class="semester-context">
                当前学期：{{ currentSemester }}
                <span v-if="stats.updateTime"> · 汇总更新于 {{ parseTime(stats.updateTime, '{y}-{m}-{d} {h}:{i}') }}</span>
              </p>
              <p class="role-desc">
                <el-tag
                  v-for="role in userStore.roles"
                  :key="role"
                  size="small"
                  style="margin-right: 6px;"
                >
                  {{ role }}
                </el-tag>
                <span v-if="stats.ratedWorkload > 0">
                  额定工作量：{{ formatNumber(stats.ratedWorkload) }} 标准学时
                </span>
              </p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 3 数据卡片 -->
    <el-row
      :gutter="20"
      class="mb20"
    >
      <el-col
        :xs="12"
        :sm="12"
        :lg="8"
      >
        <div
          class="stat-card card-info"
          @click="goWorkloadSummary"
        >
          <div class="stat-label">
            本学期承担课程
          </div>
          <div class="stat-value">
            <span class="stat-big">{{ stats.courseCount ?? '--' }}</span>
            <span
              v-if="stats.courseCount != null"
              class="stat-unit"
            >门</span>
          </div>
          <div class="stat-footer">
            共 {{ formatNumber(stats.totalWorkload) }} 标准学时
          </div>
        </div>
      </el-col>

      <el-col
        :xs="12"
        :sm="12"
        :lg="8"
      >
        <div
          class="stat-card card-success"
          @click="goWorkloadSummary"
        >
          <div class="stat-label">
            已核算工作量
          </div>
          <div class="stat-value">
            <span class="stat-big">{{ formatNumber(stats.totalWorkload) }}</span>
            <span
              v-if="stats.totalWorkload != null"
              class="stat-unit"
            >标准学时</span>
          </div>
          <div class="stat-footer">
            <template v-if="stats.excessWorkload > 0">
              超额 {{ formatNumber(stats.excessWorkload) }} 学时
            </template>
            <template v-else-if="stats.totalWorkload != null">
              暂未超额
            </template>
          </div>
        </div>
      </el-col>

      <el-col
        :xs="24"
        :sm="24"
        :lg="8"
      >
        <div
          class="stat-card card-warning"
          @click="goPayRecord"
        >
          <div class="stat-label">
            预计超工作量绩效
          </div>
          <div class="stat-value">
            <span class="stat-big">¥ {{ formatAmount(stats.performancePay) }}</span>
          </div>
          <div class="stat-footer">
            <template v-if="stats.isCapped">
              <el-tag
                type="warning"
                size="small"
              >
                已达上限 ({{ formatNumber(workloadCap) }}学时)
              </el-tag>
            </template>
            <template v-else-if="stats.hasSummary">
              汇总状态：
              <el-tag
                size="small"
                :type="summaryTagType(stats.summaryStatus)"
              >
                {{ summaryLabel(stats.summaryStatus) }}
              </el-tag>
            </template>
            <template v-else>
              超出额定后方计算
            </template>
          </div>
        </div>
      </el-col>
    </el-row>

    <teacher-overview
      v-if="stats.hasSummary"
      :row="stats"
      class="mb20"
    />
    <el-empty
      v-else
      class="summary-empty mb20"
      :description="`${currentSemester} 暂无学期汇总数据`"
      :image-size="64"
    />

    <!-- 近期明细与快捷操作 -->
    <el-row :gutter="20">
      <el-col
        :xs="24"
        :lg="16"
      >
        <el-card shadow="hover">
          <template #header>
            <div class="card-title">
              <span>近期工作量核算明细</span>
              <el-button
                type="primary"
                link
                @click="goWorkloadSummary"
              >
                查看全部 &gt;&gt;
              </el-button>
            </div>
          </template>
          <el-table
            v-loading="itemLoading"
            :data="recentItems"
            style="width: 100%"
            size="small"
            empty-text="暂无核算明细"
          >
            <el-table-column
              prop="itemType"
              label="类型"
              width="100"
            >
              <template #default="{ row }">
                <el-tag
                  size="small"
                  :type="typeTagType(row.itemType)"
                >
                  {{ itemTypeLabel(row.itemType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="sourceDesc"
              label="来源"
              min-width="140"
              show-overflow-tooltip
            />
            <el-table-column
              prop="calculatedWorkload"
              label="核算学时(学时)"
              width="110"
              align="right"
            >
              <template #default="{ row }">
                <strong style="color: var(--el-color-primary);">{{ formatNumber(row.calculatedWorkload) }}</strong>
              </template>
            </el-table-column>
            <el-table-column
              prop="status"
              label="状态"
              width="90"
              align="center"
            >
              <template #default="{ row }">
                <el-tag
                  size="small"
                  :type="statusTagType(row.status)"
                >
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col
        :xs="24"
        :lg="8"
      >
        <el-card shadow="hover">
          <template #header>
            <div class="card-title">
              <span>快捷操作</span>
            </div>
          </template>
          <div class="action-list">
            <el-button
              type="primary"
              plain
              class="action-btn"
              icon="Edit"
              @click="router.push('/workload/myWorkload')"
            >
              自主申报工作量
            </el-button>
            <el-button
              type="success"
              plain
              class="action-btn"
              icon="DataLine"
              @click="goWorkloadSummary"
            >
              查看学期汇总
            </el-button>
            <el-button
              type="info"
              plain
              class="action-btn"
              icon="Money"
              @click="goPayRecord"
            >
              查看酬金记录
            </el-button>
            <el-button
              type="warning"
              plain
              class="action-btn"
              icon="Download"
              @click="handleExport"
            >
              导出个人工作量明细
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="TeacherDashboard">
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue'
import TeacherOverview from '@/components/TeacherOverview'
import { useRouter } from 'vue-router'
import { getTeacherStats } from '@/api/system/dashboard'
import { listWorkloadItem } from '@/api/system/workloadItem'
import { useDashboard } from '@/composable/useDashboard'
import { workloadItemStatusMap, summaryStatusMap, itemTypeMap, SEMESTER_WORKLOAD_CAP, formatAmount, formatNumber, getCurrentSemester } from '@/utils/bizDict'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()
const { proxy } = getCurrentInstance()
const currentSemester = getCurrentSemester()

/** 个人工作量导出复用仪表盘共享逻辑，金额/数值格式化统一用 bizDict */
const { handleExportPersonalWorkload } = useDashboard()

// 从 userStore 获取当前用户 ID（兼容不同字段名）
const currentUserId = computed(() => userStore.id || userStore.userId)

const itemLoading = ref(false)
const recentItems = ref([])

const stats = reactive({
  courseCount: 0,
  itemCount: 0,
  hasSummary: false,
  G7: 0,
  G8: 0,
  G9: 0,
  G11: 0,
  totalWorkload: 0,
  excessWorkload: 0,
  performancePay: 0,
  ratedWorkload: 0,
  basicTeachingStandard: 0,
  summaryStatus: 0,
  isCapped: 0,
  basicTeachingMet: 0,
  remark: '',
  updateTime: null,
  appealCount: 0,
  workloadCap: SEMESTER_WORKLOAD_CAP
})

const workloadCap = computed(() => {
  const value = Number(stats.workloadCap)
  return Number.isFinite(value) && value > 0 ? value : SEMESTER_WORKLOAD_CAP
})

function goWorkloadSummary() {
  router.push({ path: '/workload/workloadSummary', query: { semester: currentSemester } })
}

function goPayRecord() {
  router.push({ path: '/workload/payRecord', query: { semester: currentSemester } })
}

/** 状态文案/标签类型统一取自 bizDict，消除页面内双口径 */
function statusLabel(status) {
  return workloadItemStatusMap[status]?.label ?? status
}

function statusTagType(status) {
  return workloadItemStatusMap[status]?.type ?? ''
}

function itemTypeLabel(code) {
  return itemTypeMap[code]?.label ?? code ?? '-'
}

function typeTagType(code) {
  if (!code) return ''
  const c = String(code)
  if (c.startsWith('G1') || c.startsWith('G2')) return ''
  if (c.startsWith('G3') || c.startsWith('G4') || c.startsWith('G6')) return 'success'
  if (c.startsWith('G5') || c.startsWith('G11')) return 'warning'
  return ''
}

function summaryLabel(status) {
  return summaryStatusMap[status]?.label ?? status
}

function summaryTagType(status) {
  return summaryStatusMap[status]?.type ?? ''
}

async function fetchStats() {
  try {
    const res = await getTeacherStats(currentSemester)
    Object.assign(stats, res.data)
  } catch (e) {
    proxy.$modal.msgError('获取统计数据失败')
  }
}

async function fetchRecentItems() {
  itemLoading.value = true
  try {
    const res = await listWorkloadItem({
      pageSize: 5,
      pageNum: 1,
      userId: currentUserId.value,
      semester: currentSemester
    })
    recentItems.value = (res.rows || []).map(r => ({
      ...r,
      sourceDesc: r.courseName || r.description || `明细 #${r.id}`
    }))
  } catch (e) {
    proxy.$modal.msgError('获取近期明细失败')
  } finally {
    itemLoading.value = false
  }
}

function handleExport() {
  handleExportPersonalWorkload(currentUserId.value, userStore.nickName || currentUserId.value)
}

onMounted(() => {
  fetchStats()
  fetchRecentItems()
})
</script>

<style scoped lang="scss">
.teacher-dashboard {
  padding: 20px;
  background-color: var(--el-fill-color-lighter);
  min-height: calc(100vh - 84px);

  .mb20 { margin-bottom: 20px; }

  .welcome-card {
    background: var(--el-fill-color-light);
    .user-info {
      display: flex;
      align-items: center;
      padding: 6px 0;
      .info-text {
        margin-left: 16px;
        h2 { margin: 0 0 4px 0; color: var(--el-text-color-primary); font-size: 20px; font-weight: 600; }
        .semester-context { margin: 0 0 8px; color: var(--el-text-color-secondary); font-size: 12px; }
        .role-desc { margin: 0; color: var(--el-text-color-regular); font-size: 13px; display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
      }
    }
  }

  .stat-card {
    padding: 20px 24px;
    border-radius: 10px;
    color: #fff;
    cursor: pointer;
    transition: transform 0.2s, box-shadow 0.2s;
    box-shadow: 0 2px 12px rgba(0, 0, 0, .08);
    min-height: 120px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 20px rgba(0, 0, 0, .15);
    }

    &.card-info    { background: linear-gradient(135deg, #5b73e8 0%, #6f4bc0 100%); }
    &.card-success { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
    &.card-warning { background: linear-gradient(135deg, #f5576c 0%, #f79d3c 100%); }

    .stat-label   { font-size: 15px; opacity: 0.95; }
    .stat-value   { margin: 8px 0; display: flex; align-items: baseline; gap: 6px; }
    .stat-big     { font-size: 30px; font-weight: bold; text-shadow: 0 1px 2px rgba(0,0,0,0.18); }
    .stat-unit    { font-size: 14px; opacity: 0.8; }
    .stat-footer  { font-size: 12px; opacity: 0.9; padding-top: 8px; border-top: 1px solid rgba(255,255,255,0.28); }
  }

  .card-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
    font-size: 15px;
  }

  .goal-panel {
    .goal-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 14px;
      .goal-label { color: var(--el-text-color-regular); }
      .goal-value { font-weight: 600; font-size: 16px; }
    }
  }

  .summary-empty {
    padding: var(--wfit-space-md);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: var(--wfit-radius-md);
    background: var(--el-bg-color);
  }

  .action-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    .action-btn {
      margin: 0;
      justify-content: flex-start;
      padding-left: 18px;
      height: 46px;
    }
  }

  :deep(.el-card) { border-radius: 8px; }
}
</style>

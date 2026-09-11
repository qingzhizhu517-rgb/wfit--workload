<template>
  <div class="app-container">
    <el-form
      v-show="showSearch"
      ref="queryRef"
      :model="queryParams"
      :inline="true"
      label-width="68px"
    >
      <el-form-item
        v-if="!isTeacher"
        label="教师"
        prop="userId"
      >
        <user-select
          v-model="queryParams.userId"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item
        label="学年学期"
        prop="semester"
      >
        <semester-select
          v-model="queryParams.semester"
          width="170px"
        />
      </el-form-item>
      <el-form-item
        label="状态"
        prop="status"
      >
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          style="width: 120px"
        >
          <el-option
            v-for="(v, k) in summaryStatusMap"
            :key="k"
            :label="v.label"
            :value="Number(k)"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          icon="Search"
          @click="handleQuery"
        >
          搜索
        </el-button>
        <el-button
          icon="Refresh"
          @click="resetQuery"
        >
          重置
        </el-button>
      </el-form-item>
    </el-form>

    <el-row
      :gutter="10"
      class="mb8"
    >
      <template v-if="!isTeacher && !isLeader">
        <el-col :span="1.5">
          <el-tooltip
            content="重算明细→汇总→酬金，需先在搜索栏选择教师和学期"
            placement="top"
          >
            <el-button
              v-hasPermi="['system:workloadSummary:edit']"
              type="primary"
              plain
              icon="Cpu"
              :loading="calcLoading"
              @click="handleRecalcAll"
            >
              一键核算
            </el-button>
          </el-tooltip>
        </el-col>
        <el-col :span="1.5">
          <el-tooltip
            content="对表格中勾选的教师逐个核算，单人失败不影响其他人"
            placement="top"
          >
            <el-button
              v-hasPermi="['system:workloadSummary:edit']"
              type="primary"
              plain
              icon="Files"
              :loading="batchCalcLoading"
              :disabled="multiple"
              @click="handleRecalcSelected"
            >
              核算所选{{ selectedUserIds.length ? `(${selectedUserIds.length})` : '' }}
            </el-button>
          </el-tooltip>
        </el-col>
        <el-col :span="1.5">
          <el-tooltip
            content="按搜索栏学期，核算该学期全部有明细的教师，耗时较长"
            placement="top"
          >
            <el-button
              v-hasPermi="['system:workloadSummary:edit']"
              type="danger"
              plain
              icon="Odometer"
              :loading="batchCalcLoading"
              @click="handleRecalcSemester"
            >
              全学期核算
            </el-button>
          </el-tooltip>
        </el-col>
        <el-col :span="1.5">
          <el-tooltip
            content="按搜索栏学期，由岗位任职批量生成 G11 管理服务明细"
            placement="top"
          >
            <el-button
              v-hasPermi="['system:workloadItem:add']"
              type="warning"
              plain
              icon="MagicStick"
              @click="handleGenG11"
            >
              生成G11
            </el-button>
          </el-tooltip>
        </el-col>
        <el-col :span="1.5">
          <el-button
            v-hasPermi="['system:audit:submit']"
            type="success"
            plain
            icon="Promotion"
            :disabled="multiple"
            @click="handleBatchSubmit"
          >
            批量提交
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            v-hasPermi="['system:workloadSummary:remove']"
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
          >
            删除
          </el-button>
        </el-col>
      </template>
      <el-col :span="1.5">
        <el-tooltip
          content="不落库仿真预览汇总结果"
          placement="top"
        >
          <el-button
            v-hasPermi="['system:workloadSummary:query']"
            type="info"
            plain
            icon="View"
            @click="handlePreview"
          >
            汇总预览
          </el-button>
        </el-tooltip>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:workloadSummary:export']"
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
        >
          导出
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-tooltip
          content="表一（标准格式）：对齐教务处模板，一行一开课任务，39 列，教师级列只填首行"
          placement="top"
        >
          <el-button
            v-hasPermi="['system:export:personal']"
            type="success"
            plain
            icon="Tickets"
            @click="handleExportStdForm1()"
          >
            导出表一
          </el-button>
        </el-tooltip>
      </el-col>
      <el-col :span="1.5">
        <el-tooltip
          content="附件1明细（追溯用）：本人/所选教师该学期工作量明细，含重复次序、班级与各项系数"
          placement="top"
        >
          <el-button
            v-hasPermi="['system:export:personal']"
            type="success"
            plain
            icon="Document"
            @click="handleExportAttach1()"
          >
            导出附件1
          </el-button>
        </el-tooltip>
      </el-col>
      <el-col
        v-if="!isTeacher"
        :span="1.5"
      >
        <el-tooltip
          content="附件2：该学期绩效酬金统计表（金额取自酬金记录）"
          placement="top"
        >
          <el-button
            v-hasPermi="['system:export:paySummary']"
            type="success"
            plain
            icon="Money"
            @click="handleExportAttach2"
          >
            导出附件2
          </el-button>
        </el-tooltip>
      </el-col>
      <right-toolbar
        v-model:show-search="showSearch"
        @query-table="getList"
      />
    </el-row>

    <!-- 教师总览卡绑定当前筛选学期，不依赖列表返回顺序 -->
    <teacher-overview
      v-if="isTeacher && overviewRow"
      :row="overviewRow"
    />

    <el-table
      v-loading="loading"
      :data="workloadSummaryList"
      stripe
      empty-text="暂无数据"
      @selection-change="handleSelectionChange"
    >
      <el-table-column
        v-if="!isTeacher"
        type="selection"
        width="50"
        align="center"
      />
      <el-table-column
        v-if="!isTeacher"
        label="教师"
        align="center"
        prop="userId"
        width="100"
        fixed="left"
      >
        <template #default="scope">
          {{ userName(scope.row.userId) }}
        </template>
      </el-table-column>
      <el-table-column
        v-if="!isTeacher"
        label="工号"
        align="center"
        width="120"
        fixed="left"
      >
        <template #default="scope">
          {{ userCode(scope.row.userId) }}
        </template>
      </el-table-column>
      <el-table-column
        label="学年学期"
        align="center"
        prop="semester"
        width="110"
        fixed="left"
      />
      <el-table-column
        label="G7 第一课堂(学时)"
        align="right"
        prop="G7"
        width="130"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.G7) }}
        </template>
      </el-table-column>
      <el-table-column
        label="G8 第二课堂(学时)"
        align="right"
        prop="G8"
        width="130"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.G8) }}
        </template>
      </el-table-column>
      <el-table-column
        label="G9 其他(学时)"
        align="right"
        prop="G9"
        width="110"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.G9) }}
        </template>
      </el-table-column>
      <el-table-column
        label="G10 教学合计(学时)"
        align="right"
        prop="G10"
        width="140"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.G10) }}
        </template>
      </el-table-column>
      <el-table-column
        label="G11 管理服务(学时)"
        align="right"
        prop="G11"
        width="130"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.G11) }}
        </template>
      </el-table-column>
      <el-table-column
        label="总工作量(学时)"
        align="right"
        prop="totalWorkload"
        width="110"
      >
        <template #default="scope">
          <span class="total-num">{{ formatNumber(scope.row.totalWorkload) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="额定(学时)"
        align="right"
        prop="ratedWorkload"
        width="90"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.ratedWorkload) }}
        </template>
      </el-table-column>
      <el-table-column
        label="超额(学时)"
        align="right"
        prop="excessWorkload"
        width="90"
      >
        <template #default="scope">
          <span :class="{ 'excess-num': Number(scope.row.excessWorkload) > 0 }">{{ formatNumber(scope.row.excessWorkload) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="绩效酬金(元)"
        align="right"
        prop="performancePay"
        width="130"
      >
        <template #default="scope">
          <!-- 数值右对齐，封顶标记小字号单独一行不破坏对齐 -->
          <div class="pay-cell">
            <span>{{ formatAmount(scope.row.performancePay) }}</span>
            <el-tag
              v-if="scope.row.isCapped === 1"
              type="danger"
              size="small"
              disable-transitions
            >
              封顶
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        label="基本教学达标"
        align="center"
        prop="basicTeachingMet"
        width="100"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.basicTeachingMet"
            :map="metStatusMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="备注 / 审核说明"
        align="left"
        prop="remark"
        min-width="180"
        show-overflow-tooltip
      >
        <template #default="scope">
          <span class="remark-text">{{ displayRemark(scope.row.remark) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="状态"
        align="center"
        prop="status"
        width="85"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.status"
            :map="summaryStatusMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        :width="isTeacher ? 100 : 220"
        fixed="right"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-button
            v-hasPermi="['system:workloadSummary:query']"
            link
            type="primary"
            icon="View"
            @click="handleDetail(scope.row)"
          >
            详情
          </el-button>
          <!-- 教师只能提交，或在审批中确认本人工作量 -->
          <el-button
            v-if="isTeacher && scope.row.status === 0"
            v-hasPermi="['system:audit:submit']"
            link
            type="success"
            icon="Promotion"
            @click="handleSubmit(scope.row)"
          >
            提交
          </el-button>
          <el-button
            v-if="isTeacher && scope.row.status === 1 && scope.row.userId === userStore.id"
            v-hasPermi="['system:audit:teacherConfirm']"
            link
            type="primary"
            icon="Select"
            @click="handleTeacherConfirm(scope.row)"
          >
            确认工作量
          </el-button>
          <!-- 教务处审批操作 -->
          <template v-if="!isTeacher">
            <el-button
              v-if="scope.row.status === 0"
              v-hasPermi="['system:audit:submit']"
              link
              type="success"
              icon="Promotion"
              @click="handleSubmit(scope.row)"
            >
              提交
            </el-button>
            <el-button
              v-if="scope.row.status === 1"
              v-hasPermi="['system:audit:approve']"
              link
              type="success"
              icon="Select"
              @click="handleApprove(scope.row)"
            >
              通过
            </el-button>
            <el-button
              v-if="scope.row.status === 1"
              v-hasPermi="['system:audit:reject']"
              link
              type="warning"
              icon="Close"
              @click="handleReject(scope.row)"
            >
              驳回
            </el-button>
            <el-button
              v-if="scope.row.status === 2"
              v-hasPermi="['system:audit:unlock']"
              link
              type="info"
              icon="Unlock"
              @click="handleUnlock(scope.row)"
            >
              解锁
            </el-button>
            <!-- 更多操作下拉 -->
            <el-dropdown
              trigger="click"
              @command="(cmd) => handleMoreCmd(cmd, scope.row)"
            >
              <el-button
                link
                type="primary"
                icon="MoreFilled"
              />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-hasPermi="['system:workloadSummary:edit']"
                    command="recalc"
                    icon="Refresh"
                  >
                    重算
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-hasPermi="['system:export:personal']"
                    command="exportItem"
                    icon="Document"
                  >
                    导出附件1
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-hasPermi="['system:workloadSummary:remove']"
                    command="delete"
                    icon="Delete"
                    divided
                  >
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="getList"
    />

    <!-- 汇总详情抽屉 -->
    <el-drawer
      v-model="detailOpen"
      title="学期汇总详情"
      :size="detailDrawerSize"
    >
      <el-descriptions
        v-if="detailRow"
        :column="detailColumns"
        border
      >
        <el-descriptions-item label="教师">
          {{ userName(detailRow.userId) }}
        </el-descriptions-item>
        <el-descriptions-item label="工号">
          {{ userCode(detailRow.userId) }}
        </el-descriptions-item>
        <el-descriptions-item label="学年学期">
          {{ detailRow.semester }}
        </el-descriptions-item>
        <el-descriptions-item label="职称快照">
          {{ detailRow.title || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="单位酬金快照">
          {{ detailRow.payRate ?? '-' }} 元
        </el-descriptions-item>
        <el-descriptions-item label="G7 第一课堂">
          {{ detailRow.G7 }}
        </el-descriptions-item>
        <el-descriptions-item label="G8 第二课堂">
          {{ detailRow.G8 }}
        </el-descriptions-item>
        <el-descriptions-item label="G9 其他">
          {{ detailRow.G9 }}
        </el-descriptions-item>
        <el-descriptions-item label="G10 教学合计">
          {{ detailRow.G10 }}
        </el-descriptions-item>
        <el-descriptions-item label="G11 管理服务">
          {{ detailRow.G11 }}
        </el-descriptions-item>
        <el-descriptions-item label="总工作量">
          {{ detailRow.totalWorkload }}
        </el-descriptions-item>
        <el-descriptions-item label="额定工作量">
          {{ detailRow.ratedWorkload }}
        </el-descriptions-item>
        <el-descriptions-item label="超额工作量">
          {{ detailRow.excessWorkload }}
        </el-descriptions-item>
        <el-descriptions-item label="绩效酬金">
          {{ formatAmount(detailRow.performancePay) }} 元
        </el-descriptions-item>
        <el-descriptions-item label="是否触顶">
          <biz-tag
            :value="detailRow.isCapped"
            :map="yesNoMap"
          />
        </el-descriptions-item>
        <el-descriptions-item label="达标标准">
          {{ detailRow.basicTeachingStandard ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="是否达标">
          <biz-tag
            :value="detailRow.basicTeachingMet"
            :map="metStatusMap"
          />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <biz-tag
            :value="detailRow.status"
            :map="summaryStatusMap"
          />
        </el-descriptions-item>
        <el-descriptions-item label="教师确认">
          {{ detailRow.teacherSign || '未确认' }}<span v-if="detailRow.teacherSignTime">（{{ parseTime(detailRow.teacherSignTime, '{y}-{m}-{d}') }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="教务确认">
          {{ detailRow.academicAssistantSign || '未确认' }}<span v-if="detailRow.academicAssistantSignTime">（{{ parseTime(detailRow.academicAssistantSignTime, '{y}-{m}-{d}') }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="锁定时间">
          {{ detailRow.lockTime ? parseTime(detailRow.lockTime) : '未锁定' }}
        </el-descriptions-item>
        <el-descriptions-item label="备注">
          {{ detailRow.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 汇总预览对话框 -->
    <el-dialog
      v-model="previewOpen"
      title="汇总预览（不落库）"
      width="560px"
      append-to-body
    >
      <el-form
        :inline="true"
        class="preview-form"
      >
        <el-form-item
          v-if="!isTeacher"
          label="教师"
        >
          <user-select
            v-model="previewQuery.userId"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="学年学期">
          <semester-select
            v-model="previewQuery.semester"
            width="170px"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            icon="View"
            :loading="previewLoading"
            @click="doPreview"
          >
            预览
          </el-button>
        </el-form-item>
      </el-form>
      <template v-if="previewData">
        <el-alert
          v-if="previewData.unconfirmedCount > 0"
          type="warning"
          :title="`注意：该教师本学期还有 ${previewData.unconfirmedCount} 条明细未核对确认，预览结果不含冻结口径`"
          :closable="false"
          class="mb12"
        />
        <el-descriptions
          v-if="previewData.summary"
          :column="2"
          border
        >
          <el-descriptions-item label="G7 第一课堂">
            {{ previewData.summary.G7 }}
          </el-descriptions-item>
          <el-descriptions-item label="G8 第二课堂">
            {{ previewData.summary.G8 }}
          </el-descriptions-item>
          <el-descriptions-item label="G9 其他">
            {{ previewData.summary.G9 }}
          </el-descriptions-item>
          <el-descriptions-item label="G10 教学合计">
            {{ previewData.summary.G10 }}
          </el-descriptions-item>
          <el-descriptions-item label="G11 管理服务">
            {{ previewData.summary.G11 }}
          </el-descriptions-item>
          <el-descriptions-item label="总工作量">
            <b>{{ previewData.summary.totalWorkload }}</b>
          </el-descriptions-item>
          <el-descriptions-item label="额定">
            {{ previewData.summary.ratedWorkload }}
          </el-descriptions-item>
          <el-descriptions-item label="超额">
            {{ previewData.summary.excessWorkload }}
          </el-descriptions-item>
          <el-descriptions-item label="绩效酬金">
            {{ formatAmount(previewData.summary.performancePay) }} 元
          </el-descriptions-item>
          <el-descriptions-item label="是否达标">
            <biz-tag
              :value="previewData.summary.basicTeachingMet"
              :map="metStatusMap"
            />
          </el-descriptions-item>
        </el-descriptions>
        <el-empty
          v-else
          description="暂无汇总数据，请先录入工作量明细"
          :image-size="80"
        />
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WorkloadSummary">
import TeacherOverview from '@/components/TeacherOverview'
import { listWorkloadSummary, delWorkloadSummary } from '@/api/system/workloadSummary'
import { recalcSummary, recalcAll, recalcAllBatch, previewSummary, genG11 } from '@/api/system/calc'
import { exportPersonalWorkload, exportPaySummary, exportAttachment1 } from '@/api/system/export'
import { ElMessageBox } from 'element-plus'
import { saveBlobAsFile } from '@/utils/blobDownload'
import { auditSubmit, auditApprove, auditReject, auditUnlock, auditBatchSubmit, auditTeacherConfirm } from '@/api/system/audit'
import UserSelect from '@/components/UserSelect/index.vue'
import SemesterSelect from '@/components/SemesterSelect/index.vue'
import { useUserMap } from '@/utils/userCache'
import { useWindowSize } from '@vueuse/core'
import { useRoute, useRouter } from 'vue-router'
import { summaryStatusMap, yesNoMap, formatAmount, formatNumber, getCurrentSemester } from '@/utils/bizDict'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const { width: windowWidth } = useWindowSize()
const detailColumns = computed(() => windowWidth.value < 640 ? 1 : 2)
const detailDrawerSize = computed(() => windowWidth.value < 700 ? '94%' : '640px')
const { userLabel, userName, userCode } = useUserMap()
const userStore = useUserStore()

const isTeacher = computed(() => userStore.roles.includes('teacher'))
const isLeader = computed(() => userStore.roles.includes('leader'))

const metStatusMap = { 1: { label: '已达标', type: 'success' }, 0: { label: '未达标', type: 'danger' } }

/** 备注可能同时承载审核说明与制度提示，按原文展示，不推断结构化告警数量。 */
function displayRemark(remark) {
  const value = String(remark || '').trim()
  return value && value !== '-' ? value : '-'
}

const workloadSummaryList = ref([])
let listRequestId = 0
const loading = ref(true)
const calcLoading = ref(false)
const showSearch = ref(true)
const ids = ref([])
// 勾选行对应的教师 userId：ids 存的是汇总主键，批量核算按教师维度调用，两者不可混用
const selectedUserIds = ref([])
const multiple = ref(true)
const batchCalcLoading = ref(false)
const total = ref(0)

const detailOpen = ref(false)
const detailRow = ref(null)
const overviewRow = computed(() => {
  if (!isTeacher.value) return null
  const semester = queryParams.value.semester
  return workloadSummaryList.value.find(row => row.semester === semester) || null
})

const previewOpen = ref(false)
const previewLoading = ref(false)
const previewData = ref(null)
const previewQuery = reactive({ userId: null, semester: null })

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    semester: getCurrentSemester(),
    status: null
  }
})

const { queryParams } = toRefs(data)

/** 查询学期工作量汇总列表 */
function getList() {
  const requestId = ++listRequestId
  loading.value = true
  listWorkloadSummary(queryParams.value).then(response => {
    if (requestId !== listRequestId) return
    workloadSummaryList.value = response.rows
    total.value = response.total
  }).catch(() => {
    if (requestId !== listRequestId) return
    workloadSummaryList.value = []
    total.value = 0
    proxy.$modal.msgError('获取学期汇总失败，请稍后重试')
  }).finally(() => {
    if (requestId === listRequestId) loading.value = false
  })
}

/** 搜索按钮操作；URL 变化时由路由 watcher 统一发起请求，避免重复查询。 */
function handleQuery() {
  queryParams.value.pageNum = 1
  if (!syncQueryRoute()) getList()
}

/** 重置筛选但保留“当前学期”上下文，避免总览意外跨学期。 */
function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.semester = getCurrentSemester()
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  // 同一教师同学期只会有一条汇总，但跨学期查询时可能重复，去重后再送批量核算
  selectedUserIds.value = [...new Set(selection.map(item => item.userId).filter(Boolean))]
  multiple.value = !selection.length
}

/** 校验教师+学期已选 */
function checkTeacherSemester() {
  const { userId, semester } = queryParams.value
  const uid = isTeacher.value ? userStore.id : userId
  if (!uid || !semester) {
    proxy.$modal.alertWarning(isTeacher.value ? '请先填写「学年学期」' : '请先在搜索栏选择「教师」并填写「学年学期」')
    return null
  }
  return { userId: uid, semester }
}

/** 重算单行汇总 */
function handleRecalcSummary(row) {
  proxy.$modal.confirm(`确认重算「${userLabel(row.userId)}」${row.semester} 学期汇总吗？`).then(function() {
    return recalcSummary(row.userId, row.semester)
  }).then((res) => {
    getList()
    const unconfirmed = res.data?.unconfirmedCount
    proxy.$modal.msgSuccess(unconfirmed > 0 ? `重算完成，尚有 ${unconfirmed} 条明细未核对` : '重算完成')
  }).catch(() => {})
}

/** 一键核算：明细→汇总→酬金 */
function handleRecalcAll() {
  const checked = checkTeacherSemester()
  if (!checked) return
  proxy.$modal.confirm(`确认对「${userLabel(checked.userId)}」${checked.semester} 执行一键核算吗？将依次重算明细、汇总与酬金。`).then(function() {
    calcLoading.value = true
    return recalcAll(checked.userId, checked.semester)
  }).then((res) => {
    getList()
    const count = res.data?.recalcItemCount ?? 0
    proxy.$modal.msgSuccess(`核算完成，共重算 ${count} 条明细`)
  }).catch(() => {}).finally(() => {
    calcLoading.value = false
  })
}

/**
 * 批量核算结果提示：成功数 + 失败明细。
 * 失败按教师逐条列出原因，避免「部分成功」被一句"完成"糊过去。
 */
function notifyBatchResult(data) {
  const success = data?.successCount ?? 0
  const fail = data?.failCount ?? 0
  const items = data?.recalcItemCount ?? 0
  if (!fail) {
    proxy.$modal.msgSuccess(`核算完成：${success} 位教师，共 ${items} 条明细`)
    return
  }
  // 失败原因来自后端异常消息，可能含尖括号，转义后再拼 HTML
  const esc = (v) => String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const lines = (data?.failures || [])
    .map(f => `${esc(f.userName || f.userId)}：${esc(f.reason)}`)
    .join('<br/>')
  // 不走 $modal.alertError：它不开 HTML 渲染，多行失败原因会挤成一行
  ElMessageBox.alert(
    `成功 ${success} 位，失败 ${fail} 位：<br/>${lines}`,
    '批量核算结果',
    { type: 'warning', dangerouslyUseHTMLString: true }
  ).catch(() => {})
}

/** 批量核算所选教师 */
function handleRecalcSelected() {
  const { semester } = queryParams.value
  if (!semester) {
    proxy.$modal.alertWarning('请先在搜索栏填写「学年学期」')
    return
  }
  if (!selectedUserIds.value.length) {
    proxy.$modal.alertWarning('请先勾选需要核算的教师')
    return
  }
  const names = selectedUserIds.value.map(id => userLabel(id)).join('、')
  proxy.$modal.confirm(`确认对 ${selectedUserIds.value.length} 位教师（${names}）执行 ${semester} 一键核算吗？`).then(function() {
    batchCalcLoading.value = true
    return recalcAllBatch(semester, selectedUserIds.value)
  }).then((res) => {
    getList()
    notifyBatchResult(res.data)
  }).catch(() => {}).finally(() => {
    batchCalcLoading.value = false
  })
}

/** 全学期核算：该学期所有有明细的教师 */
function handleRecalcSemester() {
  const { semester } = queryParams.value
  if (!semester) {
    proxy.$modal.alertWarning('请先在搜索栏填写「学年学期」')
    return
  }
  proxy.$modal.confirm(`确认核算 ${semester} 学期全部有工作量明细的教师吗？人数多时耗时较长，期间请勿重复点击。`).then(function() {
    batchCalcLoading.value = true
    // 不传 userIds = 全学期语义（后端对教师角色仍强制收敛为本人）
    return recalcAllBatch(semester, [])
  }).then((res) => {
    getList()
    notifyBatchResult(res.data)
  }).catch(() => {}).finally(() => {
    batchCalcLoading.value = false
  })
}

/** 导出表一（标准格式）：对齐教务处「-新」模板，一行一开课任务 39 列 */
function handleExportStdForm1(row) {
  const uid = row ? row.userId : (isTeacher.value ? userStore.id : queryParams.value.userId)
  const semester = row ? row.semester : queryParams.value.semester
  if (!uid || !semester) {
    proxy.$modal.alertWarning(isTeacher.value
      ? '请先填写「学年学期」'
      : '请先在搜索栏选择「教师」并填写「学年学期」，或在行内「更多」中导出')
    return
  }
  proxy.$modal.loading('正在导出表一...')
  exportAttachment1({ userId: uid, semester }).then(res => {
    return saveBlobAsFile(res, `表一_${userLabel(uid)}_${semester}.xlsx`)
  }).finally(() => {
    proxy.$modal.closeLoading()
  })
}

/** 导出附件1：工作量明细（含系数与班级），学期取搜索栏，教师取搜索栏/本人 */
function handleExportAttach1(row) {
  const uid = row ? row.userId : (isTeacher.value ? userStore.id : queryParams.value.userId)
  const semester = row ? row.semester : queryParams.value.semester
  if (!uid || !semester) {
    proxy.$modal.alertWarning(isTeacher.value
      ? '请先填写「学年学期」'
      : '请先在搜索栏选择「教师」并填写「学年学期」，或在行内「更多」中导出')
    return
  }
  proxy.$modal.loading('正在导出附件1...')
  exportPersonalWorkload({ userId: uid, semester }).then(res => {
    return saveBlobAsFile(res, `工作量明细_${userLabel(uid)}_${semester}.xlsx`)
  }).finally(() => {
    proxy.$modal.closeLoading()
  })
}

/** 导出附件2：绩效酬金统计（全学期） */
function handleExportAttach2() {
  const { semester } = queryParams.value
  if (!semester) {
    proxy.$modal.alertWarning('请先在搜索栏填写「学年学期」')
    return
  }
  proxy.$modal.loading('正在导出附件2...')
  exportPaySummary({ semester }).then(res => {
    return saveBlobAsFile(res, `绩效酬金统计_${semester}.xlsx`)
  }).finally(() => {
    proxy.$modal.closeLoading()
  })
}

/** 生成 G11 管理服务明细 */
function handleGenG11() {
  const { semester } = queryParams.value
  if (!semester) {
    proxy.$modal.alertWarning('请先在搜索栏填写「学年学期」')
    return
  }
  proxy.$modal.confirm(`确认按 ${semester} 学期的岗位任职记录批量生成 G11 明细吗？`).then(function() {
    return genG11(semester, queryParams.value.userId)
  }).then((res) => {
    proxy.$modal.msgSuccess(`生成完成，共 ${res.data ?? 0} 条 G11 明细`)
  }).catch(() => {})
}

/** 打开汇总预览 */
function handlePreview() {
  previewQuery.userId = isTeacher.value ? userStore.id : queryParams.value.userId
  previewQuery.semester = queryParams.value.semester
  previewData.value = null
  previewOpen.value = true
}

/** 执行预览 */
function doPreview() {
  if (!previewQuery.userId || !previewQuery.semester) {
    proxy.$modal.alertWarning('请选择教师并填写学年学期')
    return
  }
  previewLoading.value = true
  previewSummary(previewQuery.userId, previewQuery.semester).then(res => {
    previewData.value = res.data
  }).finally(() => {
    previewLoading.value = false
  })
}

/** 详情 */
function handleDetail(row) {
  detailRow.value = row
  detailOpen.value = true
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除选中的学期汇总记录？').then(function() {
    return delWorkloadSummary(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/workloadSummary/export', {
    ...queryParams.value
  }, `workloadSummary_${new Date().getTime()}.xlsx`)
}

/** 提交审核 */
function handleSubmit(row) {
  proxy.$modal.confirm(`确认提交「${userLabel(row.userId)}」${row.semester} 的工作量汇总审核？`).then(() => {
    return auditSubmit(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('已提交审核')
  }).catch(() => {})
}

/** 教师确认工作量 */
function handleTeacherConfirm(row) {
  proxy.$modal.confirm(`确认本人 ${row.semester} 学期的工作量汇总吗？`).then(() => {
    return auditTeacherConfirm(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('已确认工作量')
  }).catch(() => {})
}

/** 审核通过（即完结） */
function handleApprove(row) {
  proxy.$modal.confirm(`确认审核通过「${userLabel(row.userId)}」${row.semester} 的工作量汇总？通过后汇总即完结锁定。`).then(() => {
    return auditApprove(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('审核通过，汇总已完结')
  }).catch(() => {})
}

/** 驳回 */
function handleReject(row) {
  proxy.$modal.prompt('请输入驳回原因（可选）', '驳回', {
    confirmButtonText: '确定驳回',
    cancelButtonText: '取消',
    inputPattern: null,
    inputErrorMessage: ''
  }).then(({ value }) => {
    return auditReject(row.id, value || '')
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('已驳回，退回填报中')
  }).catch(() => {})
}

/** 解锁 */
function handleUnlock(row) {
  proxy.$modal.confirm(`确认解锁「${userLabel(row.userId)}」${row.semester} 的工作量汇总？解锁后可重新编辑。`).then(() => {
    return auditUnlock(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('已解锁')
  }).catch(() => {})
}

/** 更多操作下拉命令 */
function handleMoreCmd(cmd, row) {
  if (cmd === 'recalc') {
    handleRecalcSummary(row)
  } else if (cmd === 'exportItem') {
    handleExportAttach1(row)
  } else if (cmd === 'delete') {
    handleDelete(row)
  }
}

/** 批量提交审核 */
function handleBatchSubmit() {
  proxy.$modal.confirm(`确认批量提交选中的 ${ids.value.length} 条汇总记录审核？`).then(() => {
    return auditBatchSubmit(ids.value)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('批量提交成功')
  }).catch(() => {})
}

const WORKLOAD_SUMMARY_PATH = '/workload/workloadSummary'
const allowedSummaryStatuses = Object.keys(summaryStatusMap).map(Number)

function parseEnumQuery(value, allowedValues) {
  if (typeof value !== 'string' || value.trim() === '') return null
  const parsed = Number(value)
  return Number.isInteger(parsed) && allowedValues.includes(parsed) ? parsed : null
}

function syncQueryRoute() {
  const query = { ...route.query }
  const semester = typeof queryParams.value.semester === 'string'
    ? queryParams.value.semester.trim()
    : ''
  const status = queryParams.value.status
  if (semester) query.semester = semester
  else delete query.semester
  if (status !== null && status !== undefined) query.status = String(status)
  else delete query.status
  const changed = query.semester !== route.query.semester
    || query.status !== route.query.status
  if (changed) router.replace({ query })
  return changed
}

watch(
  () => [route.path, route.query.semester, route.query.status],
  ([path, semester, status]) => {
    if (path !== WORKLOAD_SUMMARY_PATH) return
    queryParams.value.semester = typeof semester === 'string' && semester.trim()
      ? semester
      : getCurrentSemester()
    queryParams.value.status = parseEnumQuery(status, allowedSummaryStatuses)
    queryParams.value.pageNum = 1
    getList()
  },
  { immediate: true }
)
</script>

<style scoped>
.pay-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.4;
}
.total-num {
  font-weight: 700;
  color: var(--el-color-primary);
}
.excess-num {
  font-weight: 600;
  color: var(--el-color-warning);
}
.remark-text {
  color: var(--el-text-color-regular);
}
.mb12 {
  margin-bottom: 12px;
}
.preview-form {
  margin-bottom: 8px;
}
</style>

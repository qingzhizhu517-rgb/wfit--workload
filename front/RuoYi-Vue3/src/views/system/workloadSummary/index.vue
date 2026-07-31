<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="教师" prop="userId">
        <user-select v-model="queryParams.userId" style="width: 200px" />
      </el-form-item>
      <el-form-item label="学年学期" prop="semester">
        <semester-select v-model="queryParams.semester" width="170px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
          <el-option v-for="(v, k) in summaryStatusMap" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-tooltip content="重算明细→汇总→酬金，需先在搜索栏选择教师和学期" placement="top">
          <el-button type="primary" plain icon="Cpu" :loading="calcLoading" @click="handleRecalcAll" v-hasPermi="['system:workloadSummary:edit']">一键核算</el-button>
        </el-tooltip>
      </el-col>
      <el-col :span="1.5">
        <el-tooltip content="不落库仿真预览汇总结果" placement="top">
          <el-button type="info" plain icon="View" @click="handlePreview" v-hasPermi="['system:workloadSummary:query']">汇总预览</el-button>
        </el-tooltip>
      </el-col>
      <el-col :span="1.5">
        <el-tooltip content="按搜索栏学期，由岗位任职批量生成 G11 管理服务明细" placement="top">
          <el-button type="warning" plain icon="MagicStick" @click="handleGenG11" v-hasPermi="['system:workloadItem:add']">生成G11</el-button>
        </el-tooltip>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:workloadSummary:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Promotion" :disabled="multiple" @click="handleBatchSubmit" v-hasPermi="['system:audit:submit']">批量提交</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:workloadSummary:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="workloadSummaryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="教师" align="center" prop="userId" width="150" fixed="left">
        <template #default="scope">{{ userLabel(scope.row.userId) }}</template>
      </el-table-column>
      <el-table-column label="学年学期" align="center" prop="semester" width="105" fixed="left" />
      <el-table-column label="G7 第一课堂" align="center" prop="G7" width="95" />
      <el-table-column label="G8 第二课堂" align="center" prop="G8" width="95" />
      <el-table-column label="G9 其他" align="center" prop="G9" width="85" />
      <el-table-column label="G10 教学合计" align="center" prop="G10" width="100" />
      <el-table-column label="G11 管理服务" align="center" prop="G11" width="100" />
      <el-table-column label="总工作量" align="center" prop="totalWorkload" width="90">
        <template #default="scope">
          <span class="total-num">{{ scope.row.totalWorkload }}</span>
        </template>
      </el-table-column>
      <el-table-column label="额定" align="center" prop="ratedWorkload" width="70" />
      <el-table-column label="超额" align="center" prop="excessWorkload" width="80">
        <template #default="scope">
          <span :class="{ 'excess-num': Number(scope.row.excessWorkload) > 0 }">{{ scope.row.excessWorkload }}</span>
        </template>
      </el-table-column>
      <el-table-column label="绩效酬金(元)" align="center" prop="performancePay" width="110">
        <template #default="scope">
          {{ formatAmount(scope.row.performancePay) }}
          <el-tag v-if="scope.row.isCapped === 1" type="danger" size="small" disable-transitions>封顶</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="基本教学达标" align="center" prop="basicTeachingMet" width="100">
        <template #default="scope">
          <biz-tag :value="scope.row.basicTeachingMet" :map="metStatusMap" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="85">
        <template #default="scope">
          <biz-tag :value="scope.row.status" :map="summaryStatusMap" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['system:workloadSummary:query']">详情</el-button>
          <!-- 审批操作按钮：根据状态显示 -->
          <el-button v-if="scope.row.status === 0" link type="success" icon="Promotion" @click="handleSubmit(scope.row)" v-hasPermi="['system:audit:submit']">提交</el-button>
          <el-button v-if="scope.row.status === 1" link type="success" icon="Select" @click="handleApprove(scope.row)" v-hasPermi="['system:audit:approve']">通过</el-button>
          <el-button v-if="scope.row.status === 1" link type="warning" icon="Close" @click="handleReject(scope.row)" v-hasPermi="['system:audit:reject']">驳回</el-button>
          <el-button v-if="scope.row.status === 2" link type="primary" icon="EditPen" @click="handleSign(scope.row)" v-hasPermi="['system:audit:sign']">签字</el-button>
          <el-button v-if="scope.row.status === 3" link type="info" icon="Unlock" @click="handleUnlock(scope.row)" v-hasPermi="['system:audit:unlock']">解锁</el-button>
          <!-- 更多操作下拉 -->
          <el-dropdown v-hasPermi="['system:workloadSummary:edit']" @command="(cmd) => handleMoreCmd(cmd, scope.row)" trigger="click">
            <el-button link type="primary" icon="MoreFilled" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="recalc" icon="Refresh">重算</el-dropdown-item>
                <el-dropdown-item command="delete" icon="Delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 汇总详情抽屉 -->
    <el-drawer v-model="detailOpen" title="学期汇总详情" size="640px">
      <el-descriptions :column="2" border v-if="detailRow">
        <el-descriptions-item label="教师">{{ userLabel(detailRow.userId) }}</el-descriptions-item>
        <el-descriptions-item label="学年学期">{{ detailRow.semester }}</el-descriptions-item>
        <el-descriptions-item label="职称快照">{{ detailRow.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="单位酬金快照">{{ detailRow.payRate ?? '-' }} 元</el-descriptions-item>
        <el-descriptions-item label="G7 第一课堂">{{ detailRow.G7 }}</el-descriptions-item>
        <el-descriptions-item label="G8 第二课堂">{{ detailRow.G8 }}</el-descriptions-item>
        <el-descriptions-item label="G9 其他">{{ detailRow.G9 }}</el-descriptions-item>
        <el-descriptions-item label="G10 教学合计">{{ detailRow.G10 }}</el-descriptions-item>
        <el-descriptions-item label="G11 管理服务">{{ detailRow.G11 }}</el-descriptions-item>
        <el-descriptions-item label="总工作量">{{ detailRow.totalWorkload }}</el-descriptions-item>
        <el-descriptions-item label="额定工作量">{{ detailRow.ratedWorkload }}</el-descriptions-item>
        <el-descriptions-item label="超额工作量">{{ detailRow.excessWorkload }}</el-descriptions-item>
        <el-descriptions-item label="绩效酬金">{{ formatAmount(detailRow.performancePay) }} 元</el-descriptions-item>
        <el-descriptions-item label="是否触顶">
          <biz-tag :value="detailRow.isCapped" :map="yesNoMap" />
        </el-descriptions-item>
        <el-descriptions-item label="达标标准">{{ detailRow.basicTeachingStandard ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="是否达标">
          <biz-tag :value="detailRow.basicTeachingMet" :map="metStatusMap" />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <biz-tag :value="detailRow.status" :map="summaryStatusMap" />
        </el-descriptions-item>
        <el-descriptions-item label="教师确认">{{ detailRow.teacherSign || '未确认' }}<span v-if="detailRow.teacherSignTime">（{{ parseTime(detailRow.teacherSignTime, '{y}-{m}-{d}') }}）</span></el-descriptions-item>
        <el-descriptions-item label="院部审核">{{ detailRow.deptLeaderSign || '未审核' }}<span v-if="detailRow.deptLeaderSignTime">（{{ parseTime(detailRow.deptLeaderSignTime, '{y}-{m}-{d}') }}）</span></el-descriptions-item>
        <el-descriptions-item label="教务确认">{{ detailRow.academicAssistantSign || '未确认' }}<span v-if="detailRow.academicAssistantSignTime">（{{ parseTime(detailRow.academicAssistantSignTime, '{y}-{m}-{d}') }}）</span></el-descriptions-item>
        <el-descriptions-item label="锁定时间">{{ detailRow.lockTime ? parseTime(detailRow.lockTime) : '未锁定' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detailRow.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 汇总预览对话框 -->
    <el-dialog title="汇总预览（不落库）" v-model="previewOpen" width="560px" append-to-body>
      <el-form :inline="true" class="preview-form">
        <el-form-item label="教师">
          <user-select v-model="previewQuery.userId" style="width: 200px" />
        </el-form-item>
        <el-form-item label="学年学期">
          <semester-select v-model="previewQuery.semester" width="170px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="View" :loading="previewLoading" @click="doPreview">预览</el-button>
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
        <el-descriptions :column="2" border v-if="previewData.summary">
          <el-descriptions-item label="G7 第一课堂">{{ previewData.summary.G7 }}</el-descriptions-item>
          <el-descriptions-item label="G8 第二课堂">{{ previewData.summary.G8 }}</el-descriptions-item>
          <el-descriptions-item label="G9 其他">{{ previewData.summary.G9 }}</el-descriptions-item>
          <el-descriptions-item label="G10 教学合计">{{ previewData.summary.G10 }}</el-descriptions-item>
          <el-descriptions-item label="G11 管理服务">{{ previewData.summary.G11 }}</el-descriptions-item>
          <el-descriptions-item label="总工作量"><b>{{ previewData.summary.totalWorkload }}</b></el-descriptions-item>
          <el-descriptions-item label="额定">{{ previewData.summary.ratedWorkload }}</el-descriptions-item>
          <el-descriptions-item label="超额">{{ previewData.summary.excessWorkload }}</el-descriptions-item>
          <el-descriptions-item label="绩效酬金">{{ formatAmount(previewData.summary.performancePay) }} 元</el-descriptions-item>
          <el-descriptions-item label="是否达标">
            <biz-tag :value="previewData.summary.basicTeachingMet" :map="metStatusMap" />
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无汇总数据，请先录入工作量明细" :image-size="80" />
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WorkloadSummary">
import { listWorkloadSummary, delWorkloadSummary } from "@/api/system/workloadSummary"
import { recalcSummary, recalcAll, previewSummary, genG11 } from "@/api/system/calc"
import { auditSubmit, auditApprove, auditReject, auditSign, auditUnlock, auditBatchSubmit } from "@/api/system/audit"
import UserSelect from '@/components/UserSelect/index.vue'
import SemesterSelect from '@/components/SemesterSelect/index.vue'
import { useUserMap } from '@/utils/userCache'
import { summaryStatusMap, yesNoMap, formatAmount } from '@/utils/bizDict'

const { proxy } = getCurrentInstance()
const { userLabel } = useUserMap()

const metStatusMap = { 1: { label: '已达标', type: 'success' }, 0: { label: '未达标', type: 'danger' } }

const workloadSummaryList = ref([])
const loading = ref(true)
const calcLoading = ref(false)
const showSearch = ref(true)
const ids = ref([])
const multiple = ref(true)
const total = ref(0)

const detailOpen = ref(false)
const detailRow = ref(null)

const previewOpen = ref(false)
const previewLoading = ref(false)
const previewData = ref(null)
const previewQuery = reactive({ userId: null, semester: null })

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    semester: null,
    status: null
  }
})

const { queryParams } = toRefs(data)

/** 查询学期工作量汇总列表 */
function getList() {
  loading.value = true
  listWorkloadSummary(queryParams.value).then(response => {
    workloadSummaryList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

/** 校验教师+学期已选 */
function checkTeacherSemester() {
  const { userId, semester } = queryParams.value
  if (!userId || !semester) {
    proxy.$modal.alertWarning('请先在搜索栏选择「教师」并填写「学年学期」')
    return null
  }
  return { userId, semester }
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
  previewQuery.userId = queryParams.value.userId
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
    proxy.$modal.msgSuccess("删除成功")
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

/** 审核通过 */
function handleApprove(row) {
  proxy.$modal.confirm(`确认审核通过「${userLabel(row.userId)}」${row.semester} 的工作量汇总？`).then(() => {
    return auditApprove(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('审核通过，已转院领导签字')
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

/** 院领导签字 */
function handleSign(row) {
  proxy.$modal.confirm(`确认签字确认「${userLabel(row.userId)}」${row.semester} 的工作量汇总？签字后将锁定。`).then(() => {
    return auditSign(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('签字确认完成，汇总已锁定')
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

getList()
</script>

<style scoped>
.total-num {
  font-weight: 700;
  color: var(--el-color-primary);
}
.excess-num {
  font-weight: 600;
  color: var(--el-color-warning);
}
.mb12 {
  margin-bottom: 12px;
}
.preview-form {
  margin-bottom: 8px;
}
</style>

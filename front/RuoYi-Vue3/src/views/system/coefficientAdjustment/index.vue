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
        label="状态"
        prop="status"
      >
        <el-select
          v-model="queryParams.status"
          placeholder="全部状态"
          clearable
          style="width: 140px"
        >
          <el-option
            v-for="(v, k) in statusMap"
            :key="k"
            :label="v.label"
            :value="Number(k)"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        v-if="isReviewer"
        label="学年学期"
        prop="semester"
      >
        <semester-select
          v-model="queryParams.semester"
          width="170px"
        />
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

    <el-alert
      :title="isReviewer ? '教务审核视图：可通过或驳回教师提交的系数调整申请' : '我的申请：查看本人提交的系数调整申请及其审核结果'"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
    />

    <el-table
      v-loading="loading"
      :data="list"
      stripe
      empty-text="暂无申请"
    >
      <el-table-column
        label="序号"
        align="center"
        width="60"
      >
        <template #default="scope">
          {{ (queryParams.pageNum - 1) * queryParams.pageSize + scope.$index + 1 }}
        </template>
      </el-table-column>
      <el-table-column
        v-if="isReviewer"
        label="教师"
        align="center"
        width="100"
      >
        <template #default="scope">
          {{ userName(scope.row.userId) }}
        </template>
      </el-table-column>
      <el-table-column
        label="学年学期"
        align="center"
        prop="semester"
        width="110"
      />
      <el-table-column
        label="类别"
        align="center"
        prop="category"
        width="80"
      />
      <el-table-column
        label="因子"
        align="center"
        prop="factorCode"
        width="80"
      />
      <el-table-column
        label="原值"
        align="right"
        width="90"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.oldValue) }}
        </template>
      </el-table-column>
      <el-table-column
        label="申请值"
        align="right"
        width="90"
      >
        <template #default="scope">
          <span class="req-num">{{ formatNumber(scope.row.requestedValue) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="申请理由"
        prop="reason"
        min-width="160"
        show-overflow-tooltip
      />
      <el-table-column
        label="状态"
        align="center"
        prop="status"
        width="90"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.status"
            :map="statusMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="审核意见"
        prop="reviewReason"
        min-width="140"
        show-overflow-tooltip
      />
      <el-table-column
        v-if="isReviewer"
        label="操作"
        align="center"
        width="140"
        fixed="right"
      >
        <template #default="scope">
          <el-button
            v-if="scope.row.status === 0"
            v-hasPermi="['system:coefficientAdjustment:approve']"
            link
            type="primary"
            icon="View"
            @click="openReview(scope.row)"
          >
            审核
          </el-button>
          <span v-else>-</span>
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

    <!-- 审核对话框：并排旧值/新值 + 影响前后工作量 -->
    <el-dialog
      v-model="reviewOpen"
      title="审核系数调整申请"
      width="min(560px, 94vw)"
      append-to-body
    >
      <el-descriptions
        :column="2"
        border
      >
        <el-descriptions-item label="教师">
          {{ userName(current.userId) }}
        </el-descriptions-item>
        <el-descriptions-item label="学年学期">
          {{ current.semester }}
        </el-descriptions-item>
        <el-descriptions-item label="类别">
          {{ current.category }}
        </el-descriptions-item>
        <el-descriptions-item label="因子">
          {{ current.factorCode }}
        </el-descriptions-item>
        <el-descriptions-item label="原系数值">
          {{ formatNumber(current.oldValue) }}
        </el-descriptions-item>
        <el-descriptions-item label="申请系数值">
          <span class="req-num">{{ formatNumber(current.requestedValue) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="调整前工作量">
          {{ itemLoading ? '加载中…' : formatNumber(beforeWorkload) }}
        </el-descriptions-item>
        <el-descriptions-item label="预计调整后">
          {{ itemLoading ? '加载中…' : formatNumber(afterWorkload) }}
        </el-descriptions-item>
        <el-descriptions-item
          label="申请理由"
          :span="2"
        >
          {{ current.reason || '-' }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="current.attachmentUrl"
          label="佐证材料"
          :span="2"
        >
          <el-link
            type="primary"
            :href="current.attachmentUrl"
            target="_blank"
          >
            查看佐证材料
          </el-link>
        </el-descriptions-item>
      </el-descriptions>
      <div class="review-hint">
        预计调整后按单因子等比估算，最终以通过后系统重算为准。
      </div>
      <el-form
        :model="reviewForm"
        label-width="80px"
        class="mt8"
      >
        <el-form-item label="审核意见">
          <el-input
            v-model="reviewForm.reviewReason"
            type="textarea"
            :rows="2"
            maxlength="255"
            placeholder="驳回时必填审核意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button
            type="success"
            :loading="reviewSubmitting"
            @click="doApprove"
          >
            通 过
          </el-button>
          <el-button
            type="danger"
            :loading="reviewSubmitting"
            @click="doReject"
          >
            驳 回
          </el-button>
          <el-button @click="reviewOpen = false">
            取 消
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CoefficientAdjustment">
import {
  myCoefficientAdjustment, listCoefficientAdjustment,
  approveCoefficientAdjustment, rejectCoefficientAdjustment
} from '@/api/system/coefficientAdjustment'
import { getWorkloadItem } from '@/api/system/workloadItem'
import SemesterSelect from '@/components/SemesterSelect/index.vue'
import { useUserMap } from '@/utils/userCache'
import { checkPermi } from '@/utils/permission'
import { formatNumber } from '@/utils/bizDict'

const { proxy } = getCurrentInstance()
const { userName } = useUserMap()

// 有审核权限者走全量 list（教务视图），否则走 myList（教师视图）
const isReviewer = checkPermi(['system:coefficientAdjustment:approve'])

const statusMap = {
  0: { label: '待审', type: 'warning' },
  1: { label: '通过', type: 'success' },
  2: { label: '驳回', type: 'danger' },
  3: { label: '撤销', type: 'info' }
}

const list = ref([])
const total = ref(0)
const loading = ref(true)
const showSearch = ref(true)
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  status: null,
  semester: null
})

const reviewOpen = ref(false)
const reviewSubmitting = ref(false)
const current = ref({})
const reviewForm = ref({ reviewReason: '' })
const itemLoading = ref(false)
const beforeWorkload = ref(null)

/** 预计调整后工作量：按单因子等比估算（current / oldValue * requestedValue）。 */
const afterWorkload = computed(() => {
  const before = Number(beforeWorkload.value)
  const oldVal = Number(current.value.oldValue)
  const reqVal = Number(current.value.requestedValue)
  if (!Number.isFinite(before) || !Number.isFinite(oldVal) || !Number.isFinite(reqVal) || oldVal === 0) {
    return null
  }
  return Math.round(before / oldVal * reqVal * 100) / 100
})

function getList() {
  loading.value = true
  const api = isReviewer ? listCoefficientAdjustment : myCoefficientAdjustment
  api(queryParams.value).then(response => {
    list.value = response.rows
    total.value = response.total
  }).catch(() => {
    proxy.$modal.msgError('获取系数调整申请列表失败')
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function openReview(row) {
  current.value = { ...row }
  reviewForm.value = { reviewReason: '' }
  beforeWorkload.value = null
  reviewOpen.value = true
  if (row.itemId) {
    itemLoading.value = true
    getWorkloadItem(row.itemId).then(response => {
      beforeWorkload.value = response.data?.calculatedWorkload ?? null
    }).catch(() => {
      beforeWorkload.value = null
    }).finally(() => {
      itemLoading.value = false
    })
  }
}

function doApprove() {
  reviewSubmitting.value = true
  approveCoefficientAdjustment(current.value.id, reviewForm.value.reviewReason || null).then(() => {
    proxy.$modal.msgSuccess('已通过并触发重算')
    reviewOpen.value = false
    getList()
  }).finally(() => {
    reviewSubmitting.value = false
  })
}

function doReject() {
  if (!reviewForm.value.reviewReason) {
    proxy.$modal.msgWarning('驳回时请填写审核意见')
    return
  }
  reviewSubmitting.value = true
  rejectCoefficientAdjustment(current.value.id, reviewForm.value.reviewReason).then(() => {
    proxy.$modal.msgSuccess('已驳回')
    reviewOpen.value = false
    getList()
  }).finally(() => {
    reviewSubmitting.value = false
  })
}

getList()
</script>

<style scoped>
.req-num {
  font-weight: 600;
  color: var(--el-color-primary);
}
.review-hint {
  margin-top: var(--wfit-space-sm);
  font-size: var(--wfit-font-xs);
  color: var(--el-text-color-secondary);
}
</style>

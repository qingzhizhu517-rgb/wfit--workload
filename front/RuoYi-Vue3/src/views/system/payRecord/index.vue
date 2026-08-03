<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item v-if="!isTeacher" label="教师" prop="userId">
        <user-select v-model="queryParams.userId" style="width: 200px" />
      </el-form-item>
      <el-form-item label="学年学期" prop="semester">
        <el-input v-model="queryParams.semester" placeholder="如 2025-2026-1" clearable style="width: 150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
          <el-option v-for="(v, k) in payStatusMap" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <template v-if="!isTeacher">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:payRecord:add']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:payRecord:edit']">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:payRecord:remove']">删除</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-tooltip content="按搜索栏选中的教师+学期重算酬金（需先重算汇总）" placement="top">
            <el-button type="primary" plain icon="Cpu" :loading="calcLoading" @click="handleRecalcPay" v-hasPermi="['system:payRecord:edit']">重算酬金</el-button>
          </el-tooltip>
        </el-col>
      </template>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:payRecord:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="payRecordList" @selection-change="handleSelectionChange">
      <el-table-column v-if="!isTeacher" type="selection" width="50" align="center" />
      <el-table-column v-if="!isTeacher" label="教师" align="center" prop="userId" width="160">
        <template #default="scope">{{ userLabel(scope.row.userId) }}</template>
      </el-table-column>
      <el-table-column label="学年学期" align="center" prop="semester" width="110" />
      <el-table-column label="课时/绩效酬金" align="right" prop="courseHourPay" width="120">
        <template #default="scope">{{ formatAmount(scope.row.courseHourPay) }}</template>
      </el-table-column>
      <el-table-column label="其他酬金合计" align="right" prop="otherPayTotal" width="120">
        <template #default="scope">{{ formatAmount(scope.row.otherPayTotal) }}</template>
      </el-table-column>
      <el-table-column label="总金额(元)" align="right" prop="totalPay" width="120">
        <template #default="scope">
          <span class="pay-total">{{ formatAmount(scope.row.totalPay, 0) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <biz-tag :value="scope.row.status" :map="payStatusMap" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="120" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" :width="isTeacher ? 80 : 220" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <template v-if="!isTeacher">
            <el-button link type="primary" icon="Tickets" @click="goAllowance(scope.row)">酬金明细</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:payRecord:edit']">修改</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:payRecord:remove']">删除</el-button>
          </template>
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

    <!-- 添加或修改酬金汇总对话框 -->
    <el-dialog :title="title" v-model="open" width="640px" append-to-body>
      <el-form ref="payRecordRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="教师" prop="userId">
              <user-select v-model="form.userId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学年学期" prop="semester">
              <el-input v-model="form.semester" placeholder="如 2025-2026-1" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课时/绩效酬金" prop="courseHourPay">
              <el-input-number v-model="form.courseHourPay" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="其他酬金合计" prop="otherPayTotal">
              <el-input-number v-model="form.otherPayTotal" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总金额" prop="totalPay">
              <el-input-number v-model="form.totalPay" :min="0" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="0">未发放</el-radio>
                <el-radio :value="1">已发放</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="PayRecord">
import { listPayRecord, getPayRecord, delPayRecord, addPayRecord, updatePayRecord } from "@/api/system/payRecord"
import { recalcPay } from "@/api/system/calc"
import UserSelect from '@/components/UserSelect/index.vue'
import { useUserMap } from '@/utils/userCache'
import { formatAmount } from '@/utils/bizDict'
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const { userLabel } = useUserMap()
const router = useRouter()
const userStore = useUserStore()

const isTeacher = computed(() => userStore.roles.includes('teacher'))

const payStatusMap = { 0: { label: '未发放', type: 'info' }, 1: { label: '已发放', type: 'success' } }

const payRecordList = ref([])
const open = ref(false)
const loading = ref(true)
const calcLoading = ref(false)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    semester: null,
    status: null
  },
  rules: {
    userId: [{ required: true, message: "请选择教师", trigger: "change" }],
    semester: [
      { required: true, message: "请输入学年学期", trigger: "blur" },
      { pattern: /^\d{4}-\d{4}-[12]$/, message: "格式如 2025-2026-1", trigger: "blur" }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询酬金汇总列表 */
function getList() {
  loading.value = true
  listPayRecord(queryParams.value).then(response => {
    payRecordList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    userId: null,
    semester: null,
    summaryId: null,
    courseHourPay: 0,
    otherPayTotal: 0,
    totalPay: 0,
    status: 0,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("payRecordRef")
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
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加酬金记录"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getPayRecord(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改酬金记录"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["payRecordRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updatePayRecord(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addPayRecord(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除选中的酬金记录？').then(function() {
    return delPayRecord(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 重算酬金 */
function handleRecalcPay() {
  const { userId, semester } = queryParams.value
  const uid = isTeacher.value ? userStore.id : userId
  if (!uid || !semester) {
    proxy.$modal.alertWarning(isTeacher.value ? '请先填写「学年学期」' : '请先在搜索栏选择「教师」并填写「学年学期」')
    return
  }
  proxy.$modal.confirm(`确认重算「${userLabel(userId)}」${semester} 学期的酬金吗？需先完成汇总重算。`).then(function() {
    calcLoading.value = true
    return recalcPay(userId, semester)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("酬金重算完成")
  }).catch(() => {}).finally(() => {
    calcLoading.value = false
  })
}

/** 跳转其他酬金明细 */
function goAllowance(row) {
  router.push({ path: '/system/allowanceItem', query: { userId: row.userId, semester: row.semester } })
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/payRecord/export', {
    ...queryParams.value
  }, `payRecord_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
.pay-total {
  font-weight: 700;
  color: var(--el-color-danger);
}
</style>

<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="教师" prop="userId">
        <user-select v-model="queryParams.userId" style="width: 200px" />
      </el-form-item>
      <el-form-item label="学年学期" prop="semester">
        <el-input v-model="queryParams.semester" placeholder="如 2025-2026-1" clearable style="width: 150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="费用类型" prop="feeType">
        <el-select v-model="queryParams.feeType" placeholder="请选择类型" clearable style="width: 160px">
          <el-option v-for="o in feeTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:allowanceItem:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:allowanceItem:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:allowanceItem:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:allowanceItem:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="allowanceItemList" stripe empty-text="暂无数据" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="教师" align="center" prop="userId" width="160">
        <template #default="scope">{{ userLabel(scope.row.userId) }}</template>
      </el-table-column>
      <el-table-column label="学年学期" align="center" prop="semester" width="105" />
      <el-table-column label="费用类型" align="center" prop="feeType" width="130">
        <template #default="scope">
          <biz-tag :value="scope.row.feeType" :map="feeTypeMap" />
        </template>
      </el-table-column>
      <el-table-column label="子类型" align="center" prop="feeSubtype" width="110">
        <template #default="scope">{{ scope.row.feeSubtype || '-' }}</template>
      </el-table-column>
      <el-table-column label="核算参数" align="left" min-width="180" show-overflow-tooltip>
        <template #default="scope">{{ paramSummary(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="金额(元)" align="right" prop="amount" width="110">
        <template #default="scope">
          <span class="amount-num">{{ formatAmount(scope.row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <biz-tag :value="scope.row.status" :map="normalStatusMap" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="110" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:allowanceItem:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:allowanceItem:remove']">删除</el-button>
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

    <!-- 添加或修改其他酬金明细对话框 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="allowanceItemRef" :model="form" :rules="rules" label-width="90px">
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
            <el-form-item label="费用类型" prop="feeType">
              <el-select v-model="form.feeType" placeholder="请选择类型" style="width: 100%" @change="onFeeTypeChange">
                <el-option v-for="o in feeTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="subtypes.length > 0">
            <el-form-item label="子类型" prop="feeSubtype">
              <el-select v-model="form.feeSubtype" placeholder="请选择子类型" clearable style="width: 100%">
                <el-option v-for="o in subtypes" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">核算参数</el-divider>
        <el-row :gutter="16">
          <el-col :span="12" v-if="['A','B','C','D'].includes(form.feeType)">
            <el-form-item label="学生人数" prop="studentCount">
              <el-input-number v-model="form.studentCount" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.feeType === 'E'">
            <el-form-item label="讲座名称" prop="lectureName">
              <el-input v-model="form.lectureName" placeholder="请输入讲座名称" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.feeType === 'E'">
            <el-form-item label="时长(小时)" prop="durationHours">
              <el-input-number v-model="form.durationHours" :min="0" :precision="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.feeType === 'F'">
            <el-form-item label="运动会天数" prop="days">
              <el-input-number v-model="form.days" :min="0" :precision="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.feeType === 'F'">
            <el-form-item label="体测班数" prop="classCount">
              <el-input-number v-model="form.classCount" :min="0" :max="999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.feeType === 'G'">
            <el-form-item label="值班工作量" prop="workloadUnits">
              <el-input-number v-model="form.workloadUnits" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="金额(元)" prop="amount">
              <el-input-number v-model="form.amount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">正常</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
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

<script setup name="AllowanceItem">
import { listAllowanceItem, getAllowanceItem, delAllowanceItem, addAllowanceItem, updateAllowanceItem } from "@/api/system/allowanceItem"
import UserSelect from '@/components/UserSelect/index.vue'
import { useUserMap } from '@/utils/userCache'
import { feeTypeOptions, feeSubtypeMap, normalStatusMap, formatAmount } from '@/utils/bizDict'
import { useRoute } from 'vue-router'

const { proxy } = getCurrentInstance()
const { userLabel } = useUserMap()
const route = useRoute()

const feeTypeMap = Object.fromEntries(feeTypeOptions.map(o => [o.value, { label: o.label, type: 'primary' }]))

const allowanceItemList = ref([])
const open = ref(false)
const loading = ref(true)
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
    feeType: null
  },
  rules: {
    userId: [{ required: true, message: "请选择教师", trigger: "change" }],
    semester: [
      { required: true, message: "请输入学年学期", trigger: "blur" },
      { pattern: /^\d{4}-\d{4}-[12]$/, message: "格式如 2025-2026-1", trigger: "blur" }
    ],
    feeType: [{ required: true, message: "请选择费用类型", trigger: "change" }],
    amount: [{ required: true, message: "请输入金额", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 当前费用类型的子类型选项 */
const subtypes = computed(() => feeSubtypeMap[form.value.feeType] || [])

/** 核算参数摘要 */
function paramSummary(row) {
  const parts = []
  if (row.studentCount) parts.push(`人数 ${row.studentCount}`)
  if (row.durationHours) parts.push(`时长 ${row.durationHours}h`)
  if (row.days) parts.push(`天数 ${row.days}`)
  if (row.classCount) parts.push(`体测 ${row.classCount} 班`)
  if (row.workloadUnits) parts.push(`工作量 ${row.workloadUnits}`)
  if (row.lectureName) parts.push(row.lectureName)
  return parts.join(' · ') || '-'
}

/** 查询其他酬金明细列表 */
function getList() {
  loading.value = true
  listAllowanceItem(queryParams.value).then(response => {
    allowanceItemList.value = response.rows
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
    payRecordId: null,
    userId: null,
    semester: null,
    feeType: null,
    feeSubtype: null,
    studentCount: 0,
    durationHours: null,
    days: null,
    classCount: null,
    workloadUnits: null,
    lectureName: null,
    ext: null,
    amount: null,
    status: 1,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("allowanceItemRef")
}

/** 切换费用类型时重置子类型 */
function onFeeTypeChange() {
  form.value.feeSubtype = null
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

/** 新增按钮操作（带出列表筛选的教师/学期，减少重复录入） */
function handleAdd() {
  reset()
  form.value.userId = queryParams.value.userId
  form.value.semester = queryParams.value.semester
  open.value = true
  title.value = "添加酬金明细"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getAllowanceItem(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改酬金明细"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["allowanceItemRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateAllowanceItem(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAllowanceItem(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除选中的酬金明细？').then(function() {
    return delAllowanceItem(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/allowanceItem/export', {
    ...queryParams.value
  }, `allowanceItem_${new Date().getTime()}.xlsx`)
}

// 支持从酬金汇总页带参跳转
if (route.query.userId) queryParams.value.userId = Number(route.query.userId)
if (route.query.semester) queryParams.value.semester = route.query.semester

getList()
</script>

<style scoped>
.amount-num {
  font-weight: 600;
  color: var(--el-color-danger);
}
</style>

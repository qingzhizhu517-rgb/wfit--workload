<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="职称" prop="title">
        <el-select v-model="queryParams.title" placeholder="请选择职称" clearable style="width: 140px">
          <el-option v-for="o in teacherTitleOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:payRate:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:payRate:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:payRate:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:payRate:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="payRateList" empty-text="暂无数据" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="职称" align="center" prop="title" width="120">
        <template #default="scope">
          <biz-tag :value="scope.row.title" :map="titleMap" />
        </template>
      </el-table-column>
      <el-table-column label="单位工作量酬金(元)" align="right" prop="rate" width="150">
        <template #default="scope">
          <span class="rate-amount">{{ formatAmount(scope.row.rate) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="生效起" align="center" prop="effectiveFrom" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.effectiveFrom, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="生效止" align="center" prop="effectiveTo" width="120">
        <template #default="scope">
          <span>{{ scope.row.effectiveTo ? parseTime(scope.row.effectiveTo, '{y}-{m}-{d}') : '至今' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <biz-tag :value="scope.row.status" :map="normalStatusMap" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:payRate:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:payRate:remove']">删除</el-button>
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

    <!-- 添加或修改职称酬金费率对话框 -->
    <el-dialog :title="title" v-model="open" width="520px" append-to-body>
      <el-form ref="payRateRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="职称" prop="title">
          <el-select v-model="form.title" placeholder="请选择职称" style="width: 100%">
            <el-option v-for="o in teacherTitleOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位酬金(元)" prop="rate">
          <el-input-number v-model="form.rate" :min="0" :precision="2" :step="1" controls-position="right" style="width: 100%" />
          <div class="form-tip">每单位工作量（学时）对应的酬金金额</div>
        </el-form-item>
        <el-form-item label="生效起" prop="effectiveFrom">
          <el-date-picker clearable v-model="form.effectiveFrom" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生效止" prop="effectiveTo">
          <el-date-picker clearable v-model="form.effectiveTo" type="date" value-format="YYYY-MM-DD" placeholder="留空表示至今有效" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="PayRate">
import { listPayRate, getPayRate, delPayRate, addPayRate, updatePayRate } from "@/api/system/payRate"
import { teacherTitleOptions, normalStatusMap, formatAmount } from '@/utils/bizDict'

const { proxy } = getCurrentInstance()

const titleMap = Object.fromEntries(
  teacherTitleOptions.map(o => [o.value, { label: o.label, type: 'primary' }])
)

const payRateList = ref([])
const open = ref(false)
const submitLoading = ref(false)
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
    title: null,
    status: null
  },
  rules: {
    title: [{ required: true, message: "请选择职称", trigger: "change" }],
    rate: [{ required: true, message: "单位酬金不能为空", trigger: "blur" }],
    effectiveFrom: [{ required: true, message: "生效起日期不能为空", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询职称酬金费率列表 */
function getList() {
  loading.value = true
  listPayRate(queryParams.value).then(response => {
    payRateList.value = response.rows
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
    title: null,
    rate: null,
    effectiveFrom: null,
    effectiveTo: null,
    status: 1,
    remark: null
  }
  proxy.resetForm("payRateRef")
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
  title.value = "添加职称酬金费率"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getPayRate(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改职称酬金费率"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["payRateRef"].validate(valid => {
    if (valid) {
      submitLoading.value = true
      const req = form.value.id != null ? updatePayRate(form.value) : addPayRate(form.value)
      req.then(() => {
        proxy.$modal.msgSuccess(form.value.id != null ? "修改成功" : "新增成功")
        open.value = false
        getList()
      }).finally(() => {
        submitLoading.value = false
      })
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除选中的酬金费率记录？').then(function() {
    return delPayRate(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/payRate/export', {
    ...queryParams.value
  }, `payRate_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
.rate-amount {
  font-weight: 600;
  color: var(--el-color-primary);
}
</style>

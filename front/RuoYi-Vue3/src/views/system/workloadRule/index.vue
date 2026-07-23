<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="参数键名" prop="ruleCode">
        <el-input
          v-model="queryParams.ruleCode"
          placeholder="请输入参数键名"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="参数说明" prop="ruleDesc">
        <el-input
          v-model="queryParams.ruleDesc"
          placeholder="请输入参数说明关键词"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:workloadRule:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:workloadRule:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:workloadRule:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:workloadRule:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-alert type="info" :closable="false" class="mb8"
      title="规则参数修改后立即参与核算；同一键名可通过不同生效日期保留历史版本" />

    <el-table v-loading="loading" :data="workloadRuleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="参数键名" align="center" prop="ruleCode" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <span class="rule-code">{{ scope.row.ruleCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="参数数值" align="center" prop="ruleValue" width="110">
        <template #default="scope">
          <span class="rule-value">{{ scope.row.ruleValue }}</span>
        </template>
      </el-table-column>
      <el-table-column label="参数说明" align="center" prop="ruleDesc" min-width="220" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.ruleDesc || '-' }}</template>
      </el-table-column>
      <el-table-column label="生效区间" align="center" width="210">
        <template #default="scope">
          <span>{{ scope.row.effectiveFrom ? parseTime(scope.row.effectiveFrom, '{y}-{m}-{d}') : '不限' }} ~ {{ scope.row.effectiveTo ? parseTime(scope.row.effectiveTo, '{y}-{m}-{d}') : '至今' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <biz-tag :value="scope.row.status" :map="normalStatusMap" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:workloadRule:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:workloadRule:remove']">删除</el-button>
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

    <!-- 添加或修改核算规则参数对话框 -->
    <el-dialog :title="title" v-model="open" width="520px" append-to-body>
      <el-form ref="workloadRuleRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="参数键名" prop="ruleCode">
          <el-input v-model="form.ruleCode" maxlength="50" placeholder="如 G1_REPEAT_2" />
          <div class="form-tip">键名由计算引擎引用，请谨慎修改已有键名</div>
        </el-form-item>
        <el-form-item label="参数数值" prop="ruleValue">
          <el-input-number v-model="form.ruleValue" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="参数说明" prop="ruleDesc">
          <el-input v-model="form.ruleDesc" maxlength="255" placeholder="请输入参数说明" />
        </el-form-item>
        <el-form-item label="生效起" prop="effectiveFrom">
          <el-date-picker clearable v-model="form.effectiveFrom" type="date" value-format="YYYY-MM-DD" placeholder="留空表示不限" style="width: 100%" />
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

<script setup name="WorkloadRule">
import { listWorkloadRule, getWorkloadRule, delWorkloadRule, addWorkloadRule, updateWorkloadRule } from "@/api/system/workloadRule"
import { normalStatusMap } from '@/utils/bizDict'

const { proxy } = getCurrentInstance()

const workloadRuleList = ref([])
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
    ruleCode: null,
    ruleDesc: null,
    status: null
  },
  rules: {
    ruleCode: [{ required: true, message: "参数键名不能为空", trigger: "blur" }],
    ruleValue: [{ required: true, message: "参数数值不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询核算规则参数列表 */
function getList() {
  loading.value = true
  listWorkloadRule(queryParams.value).then(response => {
    workloadRuleList.value = response.rows
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
    ruleCode: null,
    ruleValue: null,
    ruleDesc: null,
    effectiveFrom: null,
    effectiveTo: null,
    status: 1,
    remark: null
  }
  proxy.resetForm("workloadRuleRef")
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
  title.value = "添加核算规则参数"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getWorkloadRule(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改核算规则参数"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["workloadRuleRef"].validate(valid => {
    if (valid) {
      submitLoading.value = true
      const req = form.value.id != null ? updateWorkloadRule(form.value) : addWorkloadRule(form.value)
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
  proxy.$modal.confirm('是否确认删除选中的规则参数？删除后计算引擎将使用默认值。').then(function() {
    return delWorkloadRule(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/workloadRule/export', {
    ...queryParams.value
  }, `workloadRule_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
.rule-code {
  font-family: monospace;
  color: var(--el-color-primary);
}
.rule-value {
  font-weight: 600;
}
</style>

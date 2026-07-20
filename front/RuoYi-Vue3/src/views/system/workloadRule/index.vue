<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="参数键名" prop="ruleCode">
        <el-input
          v-model="queryParams.ruleCode"
          placeholder="请输入参数键名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="参数数值" prop="ruleValue">
        <el-input
          v-model="queryParams.ruleValue"
          placeholder="请输入参数数值"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="参数说明" prop="ruleDesc">
        <el-input
          v-model="queryParams.ruleDesc"
          placeholder="请输入参数说明"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="${comment}" prop="effectiveFrom">
        <el-date-picker clearable
          v-model="queryParams.effectiveFrom"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择${comment}">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="${comment}" prop="effectiveTo">
        <el-date-picker clearable
          v-model="queryParams.effectiveTo"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择${comment}">
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['system:workloadRule:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:workloadRule:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:workloadRule:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:workloadRule:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="workloadRuleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="${comment}" align="center" prop="id" />
      <el-table-column label="参数键名" align="center" prop="ruleCode" />
      <el-table-column label="参数数值" align="center" prop="ruleValue" />
      <el-table-column label="参数说明" align="center" prop="ruleDesc" />
      <el-table-column label="${comment}" align="center" prop="effectiveFrom" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.effectiveFrom, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="${comment}" align="center" prop="effectiveTo" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.effectiveTo, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="${comment}" align="center" prop="status" />
      <el-table-column label="${comment}" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
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

    <!-- 添加或修改全局核算规则参数对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="workloadRuleRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="参数键名" prop="ruleCode">
              <el-input v-model="form.ruleCode" placeholder="请输入参数键名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="参数数值" prop="ruleValue">
              <el-input v-model="form.ruleValue" placeholder="请输入参数数值" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="参数说明" prop="ruleDesc">
              <el-input v-model="form.ruleDesc" placeholder="请输入参数说明" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="effectiveFrom">
              <el-date-picker clearable
                v-model="form.effectiveFrom"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择${comment}">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="effectiveTo">
              <el-date-picker clearable
                v-model="form.effectiveTo"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择${comment}">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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

<script setup name="WorkloadRule">
import { listWorkloadRule, getWorkloadRule, delWorkloadRule, addWorkloadRule, updateWorkloadRule } from "@/api/system/workloadRule"

const { proxy } = getCurrentInstance()

const workloadRuleList = ref([])
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
    ruleCode: null,
    ruleValue: null,
    ruleDesc: null,
    effectiveFrom: null,
    effectiveTo: null,
    status: null,
  },
  rules: {
    ruleCode: [
      { required: true, message: "参数键名不能为空", trigger: "blur" }
    ],
    ruleValue: [
      { required: true, message: "参数数值不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询全局核算规则参数列表 */
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
    status: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
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
  title.value = "添加全局核算规则参数"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getWorkloadRule(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改全局核算规则参数"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["workloadRuleRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateWorkloadRule(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addWorkloadRule(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除全局核算规则参数编号为"' + _ids + '"的数据项？').then(function() {
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

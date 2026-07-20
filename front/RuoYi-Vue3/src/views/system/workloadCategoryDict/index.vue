<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="分类名称" prop="typeName">
        <el-input
          v-model="queryParams.typeName"
          placeholder="请输入分类名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属大类(TEACHING/ADMIN/EXTRA)" prop="parentGroup">
        <el-input
          v-model="queryParams.parentGroup"
          placeholder="请输入所属大类(TEACHING/ADMIN/EXTRA)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="Java计算策略bean名" prop="calcStrategy">
        <el-input
          v-model="queryParams.calcStrategy"
          placeholder="请输入Java计算策略bean名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否计入超额核算(1是0否)" prop="isCalcExcess">
        <el-input
          v-model="queryParams.isCalcExcess"
          placeholder="请输入是否计入超额核算(1是0否)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="排序" prop="sortOrder">
        <el-input
          v-model="queryParams.sortOrder"
          placeholder="请输入排序"
          clearable
          @keyup.enter="handleQuery"
        />
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
          v-hasPermi="['system:workloadCategoryDict:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:workloadCategoryDict:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:workloadCategoryDict:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:workloadCategoryDict:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="workloadCategoryDictList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="分类代码(G1..G11)" align="center" prop="typeCode" />
      <el-table-column label="分类名称" align="center" prop="typeName" />
      <el-table-column label="所属大类(TEACHING/ADMIN/EXTRA)" align="center" prop="parentGroup" />
      <el-table-column label="Java计算策略bean名" align="center" prop="calcStrategy" />
      <el-table-column label="是否计入超额核算(1是0否)" align="center" prop="isCalcExcess" />
      <el-table-column label="排序" align="center" prop="sortOrder" />
      <el-table-column label="状态(1正常0停用)" align="center" prop="status" />
      <el-table-column label="${comment}" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:workloadCategoryDict:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:workloadCategoryDict:remove']">删除</el-button>
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

    <!-- 添加或修改工作量类别字典对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="workloadCategoryDictRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="分类名称" prop="typeName">
              <el-input v-model="form.typeName" placeholder="请输入分类名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所属大类(TEACHING/ADMIN/EXTRA)" prop="parentGroup">
              <el-input v-model="form.parentGroup" placeholder="请输入所属大类(TEACHING/ADMIN/EXTRA)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Java计算策略bean名" prop="calcStrategy">
              <el-input v-model="form.calcStrategy" placeholder="请输入Java计算策略bean名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否计入超额核算(1是0否)" prop="isCalcExcess">
              <el-input v-model="form.isCalcExcess" placeholder="请输入是否计入超额核算(1是0否)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="排序" prop="sortOrder">
              <el-input v-model="form.sortOrder" placeholder="请输入排序" />
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

<script setup name="WorkloadCategoryDict">
import { listWorkloadCategoryDict, getWorkloadCategoryDict, delWorkloadCategoryDict, addWorkloadCategoryDict, updateWorkloadCategoryDict } from "@/api/system/workloadCategoryDict"

const { proxy } = getCurrentInstance()

const workloadCategoryDictList = ref([])
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
    typeName: null,
    parentGroup: null,
    calcStrategy: null,
    isCalcExcess: null,
    sortOrder: null,
    status: null,
  },
  rules: {
    typeName: [
      { required: true, message: "分类名称不能为空", trigger: "blur" }
    ],
    parentGroup: [
      { required: true, message: "所属大类(TEACHING/ADMIN/EXTRA)不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询工作量类别字典列表 */
function getList() {
  loading.value = true
  listWorkloadCategoryDict(queryParams.value).then(response => {
    workloadCategoryDictList.value = response.rows
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
    typeCode: null,
    typeName: null,
    parentGroup: null,
    calcStrategy: null,
    isCalcExcess: null,
    sortOrder: null,
    status: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("workloadCategoryDictRef")
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
  ids.value = selection.map(item => item.typeCode)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加工作量类别字典"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _typeCode = row.typeCode || ids.value
  getWorkloadCategoryDict(_typeCode).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改工作量类别字典"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["workloadCategoryDictRef"].validate(valid => {
    if (valid) {
      if (form.value.typeCode != null) {
        updateWorkloadCategoryDict(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addWorkloadCategoryDict(form.value).then(() => {
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
  const _typeCodes = row.typeCode || ids.value
  proxy.$modal.confirm('是否确认删除工作量类别字典编号为"' + _typeCodes + '"的数据项？').then(function() {
    return delWorkloadCategoryDict(_typeCodes)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/workloadCategoryDict/export', {
    ...queryParams.value
  }, `workloadCategoryDict_${new Date().getTime()}.xlsx`)
}

getList()
</script>

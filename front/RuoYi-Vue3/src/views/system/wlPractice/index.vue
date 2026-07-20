<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="实践学时" prop="J2">
        <el-input
          v-model="queryParams.J2"
          placeholder="请输入实践学时"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="理工1.0/其他0.9" prop="K">
        <el-input
          v-model="queryParams.K"
          placeholder="请输入理工1.0/其他0.9"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="实践重复第一次1.0/第二次起0.9" prop="C2">
        <el-input
          v-model="queryParams.C2"
          placeholder="请输入实践重复第一次1.0/第二次起0.9"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="${comment}" prop="Q1">
        <el-input
          v-model="queryParams.Q1"
          placeholder="请输入${comment}"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="${comment}" prop="Q2">
        <el-input
          v-model="queryParams.Q2"
          placeholder="请输入${comment}"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="${comment}" prop="Q3">
        <el-input
          v-model="queryParams.Q3"
          placeholder="请输入${comment}"
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
          v-hasPermi="['system:wlPractice:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:wlPractice:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:wlPractice:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:wlPractice:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="wlPracticeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="${comment}" align="center" prop="itemId" />
      <el-table-column label="实践学时" align="center" prop="J2" />
      <el-table-column label="理工1.0/其他0.9" align="center" prop="K" />
      <el-table-column label="实践重复第一次1.0/第二次起0.9" align="center" prop="C2" />
      <el-table-column label="${comment}" align="center" prop="Q1" />
      <el-table-column label="${comment}" align="center" prop="Q2" />
      <el-table-column label="${comment}" align="center" prop="Q3" />
      <el-table-column label="${comment}" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:wlPractice:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:wlPractice:remove']">删除</el-button>
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

    <!-- 添加或修改G2课内实践明细对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="wlPracticeRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="实践学时" prop="J2">
              <el-input v-model="form.J2" placeholder="请输入实践学时" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="理工1.0/其他0.9" prop="K">
              <el-input v-model="form.K" placeholder="请输入理工1.0/其他0.9" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="实践重复第一次1.0/第二次起0.9" prop="C2">
              <el-input v-model="form.C2" placeholder="请输入实践重复第一次1.0/第二次起0.9" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="Q1">
              <el-input v-model="form.Q1" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="Q2">
              <el-input v-model="form.Q2" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="Q3">
              <el-input v-model="form.Q3" placeholder="请输入${comment}" />
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

<script setup name="WlPractice">
import { listWlPractice, getWlPractice, delWlPractice, addWlPractice, updateWlPractice } from "@/api/system/wlPractice"

const { proxy } = getCurrentInstance()

const wlPracticeList = ref([])
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
    J2: null,
    K: null,
    C2: null,
    Q1: null,
    Q2: null,
    Q3: null,
  },
  rules: {
    J2: [
      { required: true, message: "实践学时不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询G2课内实践明细列表 */
function getList() {
  loading.value = true
  listWlPractice(queryParams.value).then(response => {
    wlPracticeList.value = response.rows
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
    itemId: null,
    J2: null,
    K: null,
    C2: null,
    Q1: null,
    Q2: null,
    Q3: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("wlPracticeRef")
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
  ids.value = selection.map(item => item.itemId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加G2课内实践明细"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _itemId = row.itemId || ids.value
  getWlPractice(_itemId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改G2课内实践明细"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["wlPracticeRef"].validate(valid => {
    if (valid) {
      if (form.value.itemId != null) {
        updateWlPractice(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addWlPractice(form.value).then(() => {
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
  const _itemIds = row.itemId || ids.value
  proxy.$modal.confirm('是否确认删除G2课内实践明细编号为"' + _itemIds + '"的数据项？').then(function() {
    return delWlPractice(_itemIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/wlPractice/export', {
    ...queryParams.value
  }, `wlPractice_${new Date().getTime()}.xlsx`)
}

getList()
</script>

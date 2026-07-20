<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="实际天数(1天=8学时)" prop="T">
        <el-input
          v-model="queryParams.T"
          placeholder="请输入实际天数(1天=8学时)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="指导系数理工4/艺术3/文史2/单位2" prop="D">
        <el-input
          v-model="queryParams.D"
          placeholder="请输入指导系数理工4/艺术3/文史2/单位2"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="重复系数第一轮1/第二轮0.9" prop="K">
        <el-input
          v-model="queryParams.K"
          placeholder="请输入重复系数第一轮1/第二轮0.9"
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
          v-hasPermi="['system:wlInternshipTraining:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:wlInternshipTraining:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:wlInternshipTraining:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:wlInternshipTraining:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="wlInternshipTrainingList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="${comment}" align="center" prop="itemId" />
      <el-table-column label="实际天数(1天=8学时)" align="center" prop="T" />
      <el-table-column label="指导系数理工4/艺术3/文史2/单位2" align="center" prop="D" />
      <el-table-column label="重复系数第一轮1/第二轮0.9" align="center" prop="K" />
      <el-table-column label="${comment}" align="center" prop="Q1" />
      <el-table-column label="${comment}" align="center" prop="Q2" />
      <el-table-column label="${comment}" align="center" prop="Q3" />
      <el-table-column label="${comment}" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:wlInternshipTraining:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:wlInternshipTraining:remove']">删除</el-button>
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

    <!-- 添加或修改G3教学实习实训明细对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="wlInternshipTrainingRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="实际天数(1天=8学时)" prop="T">
              <el-input v-model="form.T" placeholder="请输入实际天数(1天=8学时)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="指导系数理工4/艺术3/文史2/单位2" prop="D">
              <el-input v-model="form.D" placeholder="请输入指导系数理工4/艺术3/文史2/单位2" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="重复系数第一轮1/第二轮0.9" prop="K">
              <el-input v-model="form.K" placeholder="请输入重复系数第一轮1/第二轮0.9" />
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

<script setup name="WlInternshipTraining">
import { listWlInternshipTraining, getWlInternshipTraining, delWlInternshipTraining, addWlInternshipTraining, updateWlInternshipTraining } from "@/api/system/wlInternshipTraining"

const { proxy } = getCurrentInstance()

const wlInternshipTrainingList = ref([])
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
    T: null,
    D: null,
    K: null,
    Q1: null,
    Q2: null,
    Q3: null,
  },
  rules: {
    T: [
      { required: true, message: "实际天数(1天=8学时)不能为空", trigger: "blur" }
    ],
    D: [
      { required: true, message: "指导系数理工4/艺术3/文史2/单位2不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询G3教学实习实训明细列表 */
function getList() {
  loading.value = true
  listWlInternshipTraining(queryParams.value).then(response => {
    wlInternshipTrainingList.value = response.rows
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
    T: null,
    D: null,
    K: null,
    Q1: null,
    Q2: null,
    Q3: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("wlInternshipTrainingRef")
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
  title.value = "添加G3教学实习实训明细"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _itemId = row.itemId || ids.value
  getWlInternshipTraining(_itemId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改G3教学实习实训明细"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["wlInternshipTrainingRef"].validate(valid => {
    if (valid) {
      if (form.value.itemId != null) {
        updateWlInternshipTraining(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addWlInternshipTraining(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除G3教学实习实训明细编号为"' + _itemIds + '"的数据项？').then(function() {
    return delWlInternshipTraining(_itemIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/wlInternshipTraining/export', {
    ...queryParams.value
  }, `wlInternshipTraining_${new Date().getTime()}.xlsx`)
}

getList()
</script>

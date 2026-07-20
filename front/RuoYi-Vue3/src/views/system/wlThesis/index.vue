<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="指导人数(本&lt;=10,专&lt;=15)" prop="R5">
        <el-input
          v-model="queryParams.R5"
          placeholder="请输入指导人数(本&lt;=10,专&lt;=15)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="系数理工本9/专5,文史本6/专4" prop="K5">
        <el-input
          v-model="queryParams.K5"
          placeholder="请输入系数理工本9/专5,文史本6/专4"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="本科/专科" prop="educationLevel">
        <el-input
          v-model="queryParams.educationLevel"
          placeholder="请输入本科/专科"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="理工类/文史类" prop="major">
        <el-input
          v-model="queryParams.major"
          placeholder="请输入理工类/文史类"
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
          v-hasPermi="['system:wlThesis:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:wlThesis:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:wlThesis:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:wlThesis:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="wlThesisList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="${comment}" align="center" prop="itemId" />
      <el-table-column label="指导人数(本&lt;=10,专&lt;=15)" align="center" prop="R5" />
      <el-table-column label="系数理工本9/专5,文史本6/专4" align="center" prop="K5" />
      <el-table-column label="本科/专科" align="center" prop="educationLevel" />
      <el-table-column label="理工类/文史类" align="center" prop="major" />
      <el-table-column label="${comment}" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:wlThesis:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:wlThesis:remove']">删除</el-button>
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

    <!-- 添加或修改G5毕业论文明细对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="wlThesisRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="指导人数(本&lt;=10,专&lt;=15)" prop="R5">
              <el-input v-model="form.R5" placeholder="请输入指导人数(本&lt;=10,专&lt;=15)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="系数理工本9/专5,文史本6/专4" prop="K5">
              <el-input v-model="form.K5" placeholder="请输入系数理工本9/专5,文史本6/专4" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="本科/专科" prop="educationLevel">
              <el-input v-model="form.educationLevel" placeholder="请输入本科/专科" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="理工类/文史类" prop="major">
              <el-input v-model="form.major" placeholder="请输入理工类/文史类" />
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

<script setup name="WlThesis">
import { listWlThesis, getWlThesis, delWlThesis, addWlThesis, updateWlThesis } from "@/api/system/wlThesis"

const { proxy } = getCurrentInstance()

const wlThesisList = ref([])
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
    R5: null,
    K5: null,
    educationLevel: null,
    major: null,
  },
  rules: {
    R5: [
      { required: true, message: "指导人数(本&lt;=10,专&lt;=15)不能为空", trigger: "blur" }
    ],
    K5: [
      { required: true, message: "系数理工本9/专5,文史本6/专4不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询G5毕业论文明细列表 */
function getList() {
  loading.value = true
  listWlThesis(queryParams.value).then(response => {
    wlThesisList.value = response.rows
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
    R5: null,
    K5: null,
    educationLevel: null,
    major: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("wlThesisRef")
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
  title.value = "添加G5毕业论文明细"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _itemId = row.itemId || ids.value
  getWlThesis(_itemId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改G5毕业论文明细"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["wlThesisRef"].validate(valid => {
    if (valid) {
      if (form.value.itemId != null) {
        updateWlThesis(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addWlThesis(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除G5毕业论文明细编号为"' + _itemIds + '"的数据项？').then(function() {
    return delWlThesis(_itemIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/wlThesis/export', {
    ...queryParams.value
  }, `wlThesis_${new Date().getTime()}.xlsx`)
}

getList()
</script>

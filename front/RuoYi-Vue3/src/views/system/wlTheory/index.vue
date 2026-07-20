<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="理论学时" prop="J1">
        <el-input
          v-model="queryParams.J1"
          placeholder="请输入理论学时"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="重复系数1.0/0.9/0.8" prop="C1">
        <el-input
          v-model="queryParams.C1"
          placeholder="请输入重复系数1.0/0.9/0.8"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="课程类型必修1.1/选修1.0" prop="K1">
        <el-input
          v-model="queryParams.K1"
          placeholder="请输入课程类型必修1.1/选修1.0"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="教学质量1.0/不合格0.8" prop="Q1">
        <el-input
          v-model="queryParams.Q1"
          placeholder="请输入教学质量1.0/不合格0.8"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="课程质量" prop="Q2">
        <el-input
          v-model="queryParams.Q2"
          placeholder="请输入课程质量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="全外文系数" prop="Q3">
        <el-input
          v-model="queryParams.Q3"
          placeholder="请输入全外文系数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="合堂1.1/1.2" prop="N">
        <el-input
          v-model="queryParams.N"
          placeholder="请输入合堂1.1/1.2"
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
          v-hasPermi="['system:wlTheory:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:wlTheory:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:wlTheory:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:wlTheory:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="wlTheoryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="FK biz_workload_item.id" align="center" prop="itemId" />
      <el-table-column label="理论学时" align="center" prop="J1" />
      <el-table-column label="重复系数1.0/0.9/0.8" align="center" prop="C1" />
      <el-table-column label="课程类型必修1.1/选修1.0" align="center" prop="K1" />
      <el-table-column label="教学质量1.0/不合格0.8" align="center" prop="Q1" />
      <el-table-column label="课程质量" align="center" prop="Q2" />
      <el-table-column label="全外文系数" align="center" prop="Q3" />
      <el-table-column label="合堂1.1/1.2" align="center" prop="N" />
      <el-table-column label="${comment}" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:wlTheory:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:wlTheory:remove']">删除</el-button>
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

    <!-- 添加或修改G1理论课明细对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="wlTheoryRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="理论学时" prop="J1">
              <el-input v-model="form.J1" placeholder="请输入理论学时" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="重复系数1.0/0.9/0.8" prop="C1">
              <el-input v-model="form.C1" placeholder="请输入重复系数1.0/0.9/0.8" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课程类型必修1.1/选修1.0" prop="K1">
              <el-input v-model="form.K1" placeholder="请输入课程类型必修1.1/选修1.0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="教学质量1.0/不合格0.8" prop="Q1">
              <el-input v-model="form.Q1" placeholder="请输入教学质量1.0/不合格0.8" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课程质量" prop="Q2">
              <el-input v-model="form.Q2" placeholder="请输入课程质量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="全外文系数" prop="Q3">
              <el-input v-model="form.Q3" placeholder="请输入全外文系数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="合堂1.1/1.2" prop="N">
              <el-input v-model="form.N" placeholder="请输入合堂1.1/1.2" />
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

<script setup name="WlTheory">
import { listWlTheory, getWlTheory, delWlTheory, addWlTheory, updateWlTheory } from "@/api/system/wlTheory"

const { proxy } = getCurrentInstance()

const wlTheoryList = ref([])
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
    J1: null,
    C1: null,
    K1: null,
    Q1: null,
    Q2: null,
    Q3: null,
    N: null,
  },
  rules: {
    J1: [
      { required: true, message: "理论学时不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询G1理论课明细列表 */
function getList() {
  loading.value = true
  listWlTheory(queryParams.value).then(response => {
    wlTheoryList.value = response.rows
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
    J1: null,
    C1: null,
    K1: null,
    Q1: null,
    Q2: null,
    Q3: null,
    N: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("wlTheoryRef")
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
  title.value = "添加G1理论课明细"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _itemId = row.itemId || ids.value
  getWlTheory(_itemId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改G1理论课明细"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["wlTheoryRef"].validate(valid => {
    if (valid) {
      if (form.value.itemId != null) {
        updateWlTheory(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addWlTheory(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除G1理论课明细编号为"' + _itemIds + '"的数据项？').then(function() {
    return delWlTheory(_itemIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/wlTheory/export', {
    ...queryParams.value
  }, `wlTheory_${new Date().getTime()}.xlsx`)
}

getList()
</script>

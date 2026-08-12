<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="明细ID" prop="itemId">
        <el-input v-model="queryParams.itemId" placeholder="请输入明细ID" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:wlCourseDesign:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:wlCourseDesign:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:wlCourseDesign:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:wlCourseDesign:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-alert type="info" :closable="false" class="mb8" title="核算公式：工作量 = 学分 × 指导人数(上限20) × 每学分常量（规则 CONST_COURSE_DESIGN，默认 0.4）" />

    <el-table v-loading="loading" :data="wlCourseDesignList" empty-text="暂无数据" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="明细ID" align="center" prop="itemId" width="70" />
      <el-table-column label="课程设计学分" align="right" prop="J4" width="120">
        <template #default="scope">
          <span class="coef-main">{{ formatNumber(scope.row.J4) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="指导人数" align="right" prop="R4" width="100">
        <template #default="scope">{{ formatNumber(scope.row.R4, 0) }}</template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:wlCourseDesign:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:wlCourseDesign:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改G4课程设计明细对话框 -->
    <el-dialog :title="title" v-model="open" width="520px" append-to-body>
      <el-form ref="wlCourseDesignRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="明细ID" prop="itemId">
          <el-input-number v-model="form.itemId" :min="1" controls-position="right" :disabled="title.startsWith('修改')" style="width: 100%" />
          <div class="form-tip">关联「工作量明细」主表的 ID，一般由核算引擎自动生成</div>
        </el-form-item>
        <el-form-item label="课程设计学分" prop="J4">
          <el-input-number v-model="form.J4" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="指导人数" prop="R4">
          <el-input-number v-model="form.R4" :min="0" :max="99" :precision="0" controls-position="right" style="width: 100%" />
          <div class="form-tip">最多按 20 人核算，超出部分不计</div>
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

<script setup name="WlCourseDesign">
import { listWlCourseDesign, getWlCourseDesign, delWlCourseDesign, addWlCourseDesign, updateWlCourseDesign } from "@/api/system/wlCourseDesign"
import { formatNumber } from '@/utils/bizDict'

const { proxy } = getCurrentInstance()

const wlCourseDesignList = ref([])
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
    itemId: null
  },
  rules: {
    itemId: [{ required: true, message: "明细ID不能为空", trigger: "blur" }],
    J4: [{ required: true, message: "课程设计学分不能为空", trigger: "blur" }],
    R4: [{ required: true, message: "指导人数不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询G4课程设计明细列表 */
function getList() {
  loading.value = true
  listWlCourseDesign(queryParams.value).then(response => {
    wlCourseDesignList.value = response.rows
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
    J4: null,
    R4: null,
    remark: null
  }
  proxy.resetForm("wlCourseDesignRef")
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
  title.value = "添加G4课程设计明细"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _itemId = row.itemId || ids.value
  getWlCourseDesign(_itemId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改G4课程设计明细"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["wlCourseDesignRef"].validate(valid => {
    if (valid) {
      submitLoading.value = true
      const req = title.value.startsWith('修改') ? updateWlCourseDesign(form.value) : addWlCourseDesign(form.value)
      req.then(() => {
        proxy.$modal.msgSuccess(title.value.startsWith('修改') ? "修改成功" : "新增成功")
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
  const _itemIds = row.itemId || ids.value
  proxy.$modal.confirm('是否确认删除明细ID为"' + _itemIds + '"的数据项？').then(function() {
    return delWlCourseDesign(_itemIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/wlCourseDesign/export', {
    ...queryParams.value
  }, `wlCourseDesign_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
.coef-main {
  font-weight: 600;
  color: var(--el-color-primary);
}
</style>

<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="批次号" prop="batchNo">
        <el-input
          v-model="queryParams.batchNo"
          placeholder="请输入批次号"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="导入类型" prop="importType">
        <el-select v-model="queryParams.importType" placeholder="请选择类型" clearable style="width: 140px">
          <el-option v-for="o in importTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 130px">
          <el-option v-for="(v, k) in importBatchStatusMap" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:importBatch:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:importBatch:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:importBatch:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:importBatch:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="importBatchList" empty-text="暂无数据" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="批次号" align="center" prop="batchNo" min-width="150" show-overflow-tooltip />
      <el-table-column label="导入类型" align="center" prop="importType" width="110">
        <template #default="scope">
          <biz-tag :value="scope.row.importType" :map="importTypeMap" />
        </template>
      </el-table-column>
      <el-table-column label="文件名" align="center" prop="fileName" min-width="180" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.fileName || '-' }}</template>
      </el-table-column>
      <el-table-column label="导入结果" align="center" width="170">
        <template #default="scope">
          <span>共 {{ scope.row.totalCount ?? 0 }} 条，</span>
          <span class="text-success">{{ scope.row.successCount ?? 0 }} 成功</span>
          <template v-if="scope.row.failCount">
            <span>，</span><span class="text-danger">{{ scope.row.failCount }} 失败</span>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <biz-tag :value="scope.row.status" :map="importBatchStatusMap" />
        </template>
      </el-table-column>
      <el-table-column label="错误摘要" align="center" prop="errorSummary" min-width="160" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.errorSummary || '-' }}</template>
      </el-table-column>
      <el-table-column label="导入时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:importBatch:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:importBatch:remove']">删除</el-button>
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

    <!-- 添加或修改导入批次对话框 -->
    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="importBatchRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="批次号" prop="batchNo">
          <el-input v-model="form.batchNo" maxlength="50" placeholder="请输入批次号" />
        </el-form-item>
        <el-form-item label="导入类型" prop="importType">
          <el-radio-group v-model="form.importType">
            <el-radio v-for="o in importTypeOptions" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="文件名" prop="fileName">
          <el-input v-model="form.fileName" maxlength="200" placeholder="请输入文件名" />
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

    <!-- 批次详情抽屉 -->
    <el-drawer v-model="detailOpen" title="导入批次详情" size="480px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="批次号">{{ detail.batchNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="导入类型">{{ detail.importType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ detail.fileName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <biz-tag :value="detail.status" :map="importBatchStatusMap" />
        </el-descriptions-item>
        <el-descriptions-item label="总条数">{{ detail.totalCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="成功条数">{{ detail.successCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="失败条数">{{ detail.failCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="错误摘要">{{ detail.errorSummary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="导入时间">{{ parseTime(detail.createTime) || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup name="ImportBatch">
import { listImportBatch, getImportBatch, delImportBatch, addImportBatch, updateImportBatch } from "@/api/system/importBatch"
import { importTypeOptions, importBatchStatusMap } from '@/utils/bizDict'

const { proxy } = getCurrentInstance()

const importTypeMap = Object.fromEntries(
  importTypeOptions.map(o => [o.value, { label: o.label, type: 'primary' }])
)

const importBatchList = ref([])
const open = ref(false)
const detailOpen = ref(false)
const detail = ref({})
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
    batchNo: null,
    importType: null,
    status: null
  },
  rules: {
    batchNo: [{ required: true, message: "批次号不能为空", trigger: "blur" }],
    importType: [{ required: true, message: "请选择导入类型", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询导入批次列表 */
function getList() {
  loading.value = true
  listImportBatch(queryParams.value).then(response => {
    importBatchList.value = response.rows
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
    batchNo: null,
    importType: null,
    fileName: null,
    remark: null
  }
  proxy.resetForm("importBatchRef")
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
  title.value = "添加导入批次"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getImportBatch(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改导入批次"
  })
}

/** 详情按钮操作 */
function handleView(row) {
  getImportBatch(row.id).then(response => {
    detail.value = response.data
    detailOpen.value = true
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["importBatchRef"].validate(valid => {
    if (valid) {
      submitLoading.value = true
      const req = form.value.id != null ? updateImportBatch(form.value) : addImportBatch(form.value)
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
  proxy.$modal.confirm('是否确认删除选中的导入批次记录？').then(function() {
    return delImportBatch(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/importBatch/export', {
    ...queryParams.value
  }, `importBatch_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
.text-success {
  color: var(--el-color-success);
}
.text-danger {
  color: var(--el-color-danger);
}
</style>

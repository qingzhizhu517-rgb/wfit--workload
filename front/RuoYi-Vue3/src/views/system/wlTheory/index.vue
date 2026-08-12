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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:wlTheory:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:wlTheory:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:wlTheory:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:wlTheory:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-alert type="info" :closable="false" class="mb8" title="核算公式：工作量 = 理论学时 × 重复系数 × 课程类型 × 教学质量 × 课程质量 × 全外文 × 合堂（J1×C1×K1×Q1×Q2×Q3×N）" />

    <el-table v-loading="loading" :data="wlTheoryList" empty-text="暂无数据" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="明细ID" align="center" prop="itemId" width="70" />
      <el-table-column label="理论学时" align="right" prop="J1" width="100">
        <template #default="scope">
          <span class="coef-main">{{ formatNumber(scope.row.J1) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="重复系数" align="right" prop="C1" width="90">
        <template #default="scope">{{ formatNumber(scope.row.C1) }}</template>
      </el-table-column>
      <el-table-column label="课程类型" align="right" prop="K1" width="90">
        <template #default="scope">{{ formatNumber(scope.row.K1) }}</template>
      </el-table-column>
      <el-table-column label="教学质量" align="right" prop="Q1" width="90">
        <template #default="scope">{{ formatNumber(scope.row.Q1) }}</template>
      </el-table-column>
      <el-table-column label="课程质量" align="right" prop="Q2" width="90">
        <template #default="scope">{{ formatNumber(scope.row.Q2) }}</template>
      </el-table-column>
      <el-table-column label="全外文" align="right" prop="Q3" width="80">
        <template #default="scope">{{ formatNumber(scope.row.Q3) }}</template>
      </el-table-column>
      <el-table-column label="合堂" align="right" prop="N" width="70">
        <template #default="scope">{{ formatNumber(scope.row.N) }}</template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="120" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:wlTheory:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:wlTheory:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改G1理论课明细对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="wlTheoryRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="明细ID" prop="itemId">
          <el-input-number v-model="form.itemId" :min="1" controls-position="right" :disabled="title.startsWith('修改')" style="width: 100%" />
          <div class="form-tip">关联「工作量明细」主表的 ID，一般由核算引擎自动生成</div>
        </el-form-item>
        <el-divider content-position="left">核算要素</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="理论学时" prop="J1">
              <el-input-number v-model="form.J1" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="重复系数" prop="C1">
              <el-input-number v-model="form.C1" :min="0" :max="2" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
              <div class="form-tip">第1次 1.0 / 第2次 0.9 / 第3次起 0.8</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程类型" prop="K1">
              <el-input-number v-model="form.K1" :min="0" :max="2" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
              <div class="form-tip">必修 1.1 / 选修 1.0</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="教学质量" prop="Q1">
              <el-input-number v-model="form.Q1" :min="0" :max="2" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
              <div class="form-tip">合格 1.0 / 不合格 0.8</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程质量" prop="Q2">
              <el-input-number v-model="form.Q2" :min="0" :max="2" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="全外文" prop="Q3">
              <el-input-number v-model="form.Q3" :min="0" :max="2" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合堂" prop="N">
              <el-input-number v-model="form.N" :min="0" :max="2" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
              <div class="form-tip">合堂 1.1 / 1.2</div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
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

<script setup name="WlTheory">
import { listWlTheory, getWlTheory, delWlTheory, addWlTheory, updateWlTheory } from "@/api/system/wlTheory"
import { formatNumber } from '@/utils/bizDict'

const { proxy } = getCurrentInstance()

const wlTheoryList = ref([])
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
    J1: [{ required: true, message: "理论学时不能为空", trigger: "blur" }]
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
    C1: 1.0,
    K1: 1.1,
    Q1: 1.0,
    Q2: 1.0,
    Q3: 1.0,
    N: 1.0,
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
      submitLoading.value = true
      const req = title.value.startsWith('修改') ? updateWlTheory(form.value) : addWlTheory(form.value)
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

<style scoped>
.coef-main {
  font-weight: 600;
  color: var(--el-color-primary);
}
</style>

<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="明细ID" prop="itemId">
        <el-input v-model="queryParams.itemId" placeholder="请输入明细ID" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="层次" prop="educationLevel">
        <el-select v-model="queryParams.educationLevel" placeholder="请选择层次" clearable style="width: 120px">
          <el-option v-for="o in educationLevelOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="科类" prop="major">
        <el-select v-model="queryParams.major" placeholder="请选择科类" clearable style="width: 120px">
          <el-option v-for="o in majorCategoryOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:wlThesis:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:wlThesis:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:wlThesis:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:wlThesis:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-alert type="info" :closable="false" class="mb8" title="核算公式：工作量 = 指导人数 × 系数（理工 本9/专5，文史 本6/专4；本科上限10人、专科上限15人）" />

    <el-table v-loading="loading" :data="wlThesisList" stripe empty-text="暂无数据" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="明细ID" align="center" prop="itemId" width="70" />
      <el-table-column label="指导人数" align="right" prop="R5" width="100">
        <template #default="scope">
          <span class="coef-main">{{ formatNumber(scope.row.R5, 0) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="系数" align="right" prop="K5" width="80">
        <template #default="scope">{{ formatNumber(scope.row.K5) }}</template>
      </el-table-column>
      <el-table-column label="层次" align="center" prop="educationLevel" width="120">
        <template #default="scope">
          <biz-tag :value="scope.row.educationLevel" :map="educationLevelTagMap" />
        </template>
      </el-table-column>
      <el-table-column label="科类" align="center" prop="major" width="90">
        <template #default="scope">
          <biz-tag :value="scope.row.major" :map="majorCategoryTagMap" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:wlThesis:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:wlThesis:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改G5毕业论文明细对话框 -->
    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="wlThesisRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="明细ID" prop="itemId">
          <el-input-number v-model="form.itemId" :min="1" controls-position="right" :disabled="title.startsWith('修改')" style="width: 100%" />
          <div class="form-tip">关联「工作量明细」主表的 ID，一般由核算引擎自动生成</div>
        </el-form-item>
        <el-divider content-position="left">核算要素</el-divider>
        <el-form-item label="指导人数" prop="R5">
          <el-input-number v-model="form.R5" :min="0" :max="99" :precision="0" controls-position="right" style="width: 100%" />
          <div class="form-tip">本科上限 10 人、专科上限 15 人</div>
        </el-form-item>
        <el-form-item label="系数" prop="K5">
          <el-input-number v-model="form.K5" :min="0" :precision="2" controls-position="right" style="width: 100%" />
          <div class="form-tip">理工 本9/专5，文史 本6/专4</div>
        </el-form-item>
        <el-form-item label="层次" prop="educationLevel">
          <el-radio-group v-model="form.educationLevel">
            <el-radio v-for="o in educationLevelOptions" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="科类" prop="major">
          <el-select v-model="form.major" placeholder="请选择科类" clearable style="width: 100%">
            <el-option v-for="o in majorCategoryOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
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

<script setup name="WlThesis">
import { listWlThesis, getWlThesis, delWlThesis, addWlThesis, updateWlThesis } from "@/api/system/wlThesis"
import { educationLevelOptions, majorCategoryOptions, optionsToMap, formatNumber } from '@/utils/bizDict'

const educationLevelTagMap = optionsToMap(educationLevelOptions)
const majorCategoryTagMap = optionsToMap(majorCategoryOptions)

const { proxy } = getCurrentInstance()

const wlThesisList = ref([])
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
    itemId: null,
    educationLevel: null,
    major: null
  },
  rules: {
    itemId: [{ required: true, message: "明细ID不能为空", trigger: "blur" }],
    R5: [{ required: true, message: "指导人数不能为空", trigger: "blur" }],
    K5: [{ required: true, message: "系数不能为空", trigger: "blur" }]
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
    educationLevel: '本科',
    major: null,
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
      submitLoading.value = true
      const req = title.value.startsWith('修改') ? updateWlThesis(form.value) : addWlThesis(form.value)
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

<style scoped>
.coef-main {
  font-weight: 600;
  color: var(--el-color-primary);
}
</style>

<template>
  <div class="app-container">
    <el-form
      v-show="showSearch"
      ref="queryRef"
      :model="queryParams"
      :inline="true"
      label-width="68px"
    >
      <el-form-item
        label="明细ID"
        prop="itemId"
      >
        <el-input
          v-model="queryParams.itemId"
          placeholder="请输入明细ID"
          clearable
          style="width: 160px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          icon="Search"
          @click="handleQuery"
        >
          搜索
        </el-button>
        <el-button
          icon="Refresh"
          @click="resetQuery"
        >
          重置
        </el-button>
      </el-form-item>
    </el-form>

    <el-row
      :gutter="10"
      class="mb8"
    >
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:wlPractice:add']"
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
        >
          新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:wlPractice:edit']"
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
        >
          修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:wlPractice:remove']"
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
        >
          删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:wlPractice:export']"
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
        >
          导出
        </el-button>
      </el-col>
      <right-toolbar
        v-model:show-search="showSearch"
        @query-table="getList"
      />
    </el-row>

    <el-alert
      type="info"
      :closable="false"
      class="mb8"
      title="核算公式：工作量 = 实践学时 × 专业类别 × 重复系数 × 教学质量 × 课程质量 × 全外文（J2×K×C2×Q1×Q2×Q3）"
    />

    <el-table
      v-loading="loading"
      :data="wlPracticeList"
      stripe
      empty-text="暂无数据"
      @selection-change="handleSelectionChange"
    >
      <el-table-column
        type="selection"
        width="50"
        align="center"
      />
      <el-table-column
        label="明细ID"
        align="center"
        prop="itemId"
        width="70"
      />
      <el-table-column
        label="实践学时"
        align="right"
        prop="J2"
        width="100"
      >
        <template #default="scope">
          <span class="coef-main">{{ formatNumber(scope.row.J2) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="专业类别"
        align="right"
        prop="K"
        width="90"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.K) }}
        </template>
      </el-table-column>
      <el-table-column
        label="重复系数"
        align="right"
        prop="C2"
        width="90"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.C2) }}
        </template>
      </el-table-column>
      <el-table-column
        label="教学质量"
        align="right"
        prop="Q1"
        width="90"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.Q1) }}
        </template>
      </el-table-column>
      <el-table-column
        label="课程质量"
        align="right"
        prop="Q2"
        width="90"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.Q2) }}
        </template>
      </el-table-column>
      <el-table-column
        label="全外文"
        align="right"
        prop="Q3"
        width="80"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.Q3) }}
        </template>
      </el-table-column>
      <el-table-column min-width="40" />
      <el-table-column
        label="操作"
        align="center"
        width="180"
        fixed="right"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleView(scope.row)"
          >
            详情
          </el-button>
          <el-button
            v-hasPermi="['system:wlPractice:edit']"
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['system:wlPractice:remove']"
            link
            type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <biz-detail-drawer
      v-model="detailOpen"
      title="G2 课内实践明细详情"
      :row="detailRow"
      :fields="detailFields"
    />

    <pagination
      v-show="total>0"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="getList"
    />

    <!-- 添加或修改G2课内实践明细对话框 -->
    <el-dialog
      v-model="open"
      :title="title"
      width="600px"
      append-to-body
    >
      <el-form
        ref="wlPracticeRef"
        :model="form"
        :rules="rules"
        label-width="90px"
      >
        <el-form-item
          label="明细ID"
          prop="itemId"
        >
          <el-input-number
            v-model="form.itemId"
            :min="1"
            controls-position="right"
            :disabled="title.startsWith('修改')"
            style="width: 100%"
          />
          <div class="form-tip">
            关联「工作量明细」主表的 ID，一般由核算引擎自动生成
          </div>
        </el-form-item>
        <el-divider content-position="left">
          核算要素
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item
              label="实践学时"
              prop="J2"
            >
              <el-input-number
                v-model="form.J2"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="专业类别"
              prop="K"
            >
              <el-input-number
                v-model="form.K"
                :min="0"
                :max="2"
                :precision="2"
                :step="0.1"
                controls-position="right"
                style="width: 100%"
              />
              <div class="form-tip">
                理工 1.0 / 其他 0.9
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="重复系数"
              prop="C2"
            >
              <el-input-number
                v-model="form.C2"
                :min="0"
                :max="2"
                :precision="2"
                :step="0.1"
                controls-position="right"
                style="width: 100%"
              />
              <div class="form-tip">
                第一次 1.0 / 第二次起 0.9
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="教学质量"
              prop="Q1"
            >
              <el-input-number
                v-model="form.Q1"
                :min="0"
                :max="2"
                :precision="2"
                :step="0.1"
                controls-position="right"
                style="width: 100%"
              />
              <div class="form-tip">
                合格 1.0 / 不合格 0.8
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="课程质量"
              prop="Q2"
            >
              <el-input-number
                v-model="form.Q2"
                :min="0"
                :max="2"
                :precision="2"
                :step="0.1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="全外文"
              prop="Q3"
            >
              <el-input-number
                v-model="form.Q3"
                :min="0"
                :max="2"
                :precision="2"
                :step="0.1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item
              label="备注"
              prop="remark"
            >
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="2"
                maxlength="500"
                show-word-limit
                placeholder="请输入备注"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button
            type="primary"
            :loading="submitLoading"
            @click="submitForm"
          >
            确 定
          </el-button>
          <el-button @click="cancel">
            取 消
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WlPractice">
import { listWlPractice, getWlPractice, delWlPractice, addWlPractice, updateWlPractice } from '@/api/system/wlPractice'
import { formatNumber } from '@/utils/bizDict'

const { proxy } = getCurrentInstance()

const wlPracticeList = ref([])
const open = ref(false)
const detailOpen = ref(false)
const detailRow = ref({})
const submitLoading = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

/** 详情抽屉字段（承载原备注列 + 各核算系数，列表页更清爽） */
const detailFields = [
  { label: '明细ID', prop: 'itemId', type: 'text' },
  { label: '实践学时 J2', prop: 'J2' },
  { label: '专业类别 K', prop: 'K' },
  { label: '重复系数 C2', prop: 'C2' },
  { label: '教学质量 Q1', prop: 'Q1' },
  { label: '课程质量 Q2', prop: 'Q2' },
  { label: '全外文 Q3', prop: 'Q3' }
]

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    itemId: null
  },
  rules: {
    itemId: [{ required: true, message: '明细ID不能为空', trigger: 'blur' }],
    J2: [{ required: true, message: '实践学时不能为空', trigger: 'blur' }]
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
    K: 1.0,
    C2: 1.0,
    Q1: 1.0,
    Q2: 1.0,
    Q3: 1.0,
    remark: null
  }
  proxy.resetForm('wlPracticeRef')
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm('queryRef')
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
  title.value = '添加G2课内实践明细'
}

/** 查看详情操作 */
function handleView(row) {
  detailRow.value = row
  detailOpen.value = true
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _itemId = row.itemId || ids.value
  getWlPractice(_itemId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改G2课内实践明细'
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['wlPracticeRef'].validate(valid => {
    if (valid) {
      submitLoading.value = true
      const req = title.value.startsWith('修改') ? updateWlPractice(form.value) : addWlPractice(form.value)
      req.then(() => {
        proxy.$modal.msgSuccess(title.value.startsWith('修改') ? '修改成功' : '新增成功')
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
    return delWlPractice(_itemIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
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

<style scoped>
.coef-main {
  font-weight: 600;
  color: var(--el-color-primary);
}
</style>

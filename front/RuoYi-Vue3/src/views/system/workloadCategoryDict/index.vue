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
        label="分类名称"
        prop="typeName"
      >
        <el-input
          v-model="queryParams.typeName"
          placeholder="请输入分类名称"
          clearable
          style="width: 160px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item
        label="所属大类"
        prop="parentGroup"
      >
        <el-select
          v-model="queryParams.parentGroup"
          placeholder="请选择大类"
          clearable
          style="width: 140px"
        >
          <el-option
            v-for="o in parentGroupOptions"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
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
          v-hasPermi="['system:workloadCategoryDict:add']"
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
          v-hasPermi="['system:workloadCategoryDict:edit']"
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
          v-hasPermi="['system:workloadCategoryDict:remove']"
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
          v-hasPermi="['system:workloadCategoryDict:export']"
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

    <el-table
      v-loading="loading"
      :data="workloadCategoryDictList"
      row-key="typeCode"
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
        label="分类代码"
        align="center"
        prop="typeCode"
        width="100"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.typeCode"
            :map="typeCodeMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="分类名称"
        align="center"
        prop="typeName"
        min-width="130"
        show-overflow-tooltip
      />
      <el-table-column
        label="所属大类"
        align="center"
        prop="parentGroup"
        width="110"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.parentGroup"
            :map="parentGroupMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="计算策略"
        align="center"
        prop="calcStrategy"
        min-width="150"
        show-overflow-tooltip
      >
        <template #default="scope">
          {{ scope.row.calcStrategy || '-' }}
        </template>
      </el-table-column>
      <el-table-column
        label="计入超额"
        align="center"
        prop="isCalcExcess"
        width="90"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.isCalcExcess"
            :map="yesNoMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="排序"
        align="center"
        prop="sortOrder"
        width="60"
      />
      <el-table-column
        label="状态"
        align="center"
        prop="status"
        width="90"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.status"
            :map="normalStatusMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="备注"
        align="center"
        prop="remark"
        min-width="120"
        show-overflow-tooltip
      >
        <template #default="scope">
          {{ scope.row.remark || '-' }}
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        width="140"
        fixed="right"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-button
            v-hasPermi="['system:workloadCategoryDict:edit']"
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['system:workloadCategoryDict:remove']"
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

    <pagination
      v-show="total>0"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="getList"
    />

    <!-- 添加或修改工作量类别字典对话框 -->
    <el-dialog
      v-model="open"
      :title="title"
      width="520px"
      append-to-body
    >
      <el-form
        ref="workloadCategoryDictRef"
        :model="form"
        :rules="rules"
        label-width="90px"
      >
        <el-form-item
          label="分类代码"
          prop="typeCode"
        >
          <el-input
            v-model="form.typeCode"
            maxlength="10"
            placeholder="如 G12"
            :disabled="title.startsWith('修改')"
          />
          <div class="form-tip">
            代码是主键且被明细数据引用，创建后不可修改
          </div>
        </el-form-item>
        <el-form-item
          label="分类名称"
          prop="typeName"
        >
          <el-input
            v-model="form.typeName"
            maxlength="100"
            placeholder="请输入分类名称"
          />
        </el-form-item>
        <el-form-item
          label="所属大类"
          prop="parentGroup"
        >
          <el-select
            v-model="form.parentGroup"
            placeholder="请选择大类"
            style="width: 100%"
          >
            <el-option
              v-for="o in parentGroupOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="计算策略"
          prop="calcStrategy"
        >
          <el-input
            v-model="form.calcStrategy"
            maxlength="100"
            placeholder="Java 计算策略 bean 名，如 g1TheoryCalc"
          />
          <div class="form-tip">
            需与后端计算引擎中的策略 bean 名一致，留空表示该类别不参与自动核算
          </div>
        </el-form-item>
        <el-form-item
          label="计入超额"
          prop="isCalcExcess"
        >
          <el-radio-group v-model="form.isCalcExcess">
            <el-radio :value="1">
              是
            </el-radio>
            <el-radio :value="0">
              否
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="排序"
          prop="sortOrder"
        >
          <el-input-number
            v-model="form.sortOrder"
            :min="0"
            :max="999"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="状态"
          prop="status"
        >
          <el-radio-group v-model="form.status">
            <el-radio :value="1">
              正常
            </el-radio>
            <el-radio :value="0">
              停用
            </el-radio>
          </el-radio-group>
        </el-form-item>
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

<script setup name="WorkloadCategoryDict">
import { listWorkloadCategoryDict, getWorkloadCategoryDict, delWorkloadCategoryDict, addWorkloadCategoryDict, updateWorkloadCategoryDict } from '@/api/system/workloadCategoryDict'
import { normalStatusMap, yesNoMap } from '@/utils/bizDict'

const { proxy } = getCurrentInstance()

const parentGroupOptions = [
  { label: '教学工作量', value: 'TEACHING' },
  { label: '管理服务', value: 'ADMIN' },
  { label: '额外酬金', value: 'EXTRA' }
]
const parentGroupMap = {
  TEACHING: { label: '教学工作量', type: 'primary' },
  ADMIN: { label: '管理服务', type: 'warning' },
  EXTRA: { label: '额外酬金', type: 'success' }
}
const typeCodeMap = new Proxy({}, {
  get: (_, key) => ({ label: String(key), type: 'primary' })
})

const workloadCategoryDictList = ref([])
const open = ref(false)
const submitLoading = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const codes = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    typeName: null,
    parentGroup: null
  },
  rules: {
    typeCode: [
      { required: true, message: '分类代码不能为空', trigger: 'blur' },
      { pattern: /^G\d{1,2}$/, message: '格式如 G1 ~ G12', trigger: 'blur' }
    ],
    typeName: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }],
    parentGroup: [{ required: true, message: '请选择所属大类', trigger: 'change' }]
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
    isCalcExcess: 1,
    sortOrder: 0,
    status: 1,
    remark: null
  }
  proxy.resetForm('workloadCategoryDictRef')
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
  codes.value = selection.map(item => item.typeCode)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加工作量类别'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _typeCode = row.typeCode || codes.value
  getWorkloadCategoryDict(_typeCode).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改工作量类别'
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['workloadCategoryDictRef'].validate(valid => {
    if (valid) {
      submitLoading.value = true
      const req = title.value.startsWith('修改') ? updateWorkloadCategoryDict(form.value) : addWorkloadCategoryDict(form.value)
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
  const _codes = row.typeCode || codes.value
  proxy.$modal.confirm('是否确认删除类别代码为"' + _codes + '"的字典项？删除后该类别明细将无法自动核算。').then(function() {
    return delWorkloadCategoryDict(_codes)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
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

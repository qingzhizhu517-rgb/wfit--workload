<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="教师" prop="userId">
        <user-select v-model="queryParams.userId" style="width: 200px" />
      </el-form-item>
      <el-form-item label="学年学期" prop="semester">
        <semester-select v-model="queryParams.semester" width="170px" />
      </el-form-item>
      <el-form-item label="工作量类别" prop="itemType">
        <el-select v-model="queryParams.itemType" placeholder="请选择类别" clearable style="width: 150px">
          <el-option v-for="o in itemTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
          <el-option v-for="(v, k) in workloadItemStatusMap" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:workloadItem:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:workloadItem:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:workloadItem:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:workloadItem:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-tooltip content="按搜索栏选中的教师+学期，重算全部未冻结明细" placement="top">
          <el-button type="primary" plain icon="Refresh" :loading="recalcLoading" @click="handleRecalcSemester" v-hasPermi="['system:workloadItem:edit']">重算学期明细</el-button>
        </el-tooltip>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="workloadItemList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="序号" align="center" width="60">
        <template #default="scope">
          {{ (queryParams.pageNum - 1) * queryParams.pageSize + scope.$index + 1 }}
        </template>
      </el-table-column>

      <el-table-column label="教师" align="center" prop="userId" width="150">
        <template #default="scope">{{ userLabel(scope.row.userId) }}</template>
      </el-table-column>
      <el-table-column label="学年学期" align="center" prop="semester" width="105" />
      <el-table-column label="类别" align="center" prop="itemType" width="110">
        <template #default="scope">
          <biz-tag :value="scope.row.itemType" :map="itemTypeMap" />
        </template>
      </el-table-column>
      <el-table-column label="来源" align="center" prop="sourceType" width="90">
        <template #default="scope">
          <biz-tag :value="scope.row.sourceType" :map="sourceTypeMap" />
        </template>
      </el-table-column>
      <el-table-column label="课程/事项" align="center" prop="courseName" min-width="150" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.courseName || scope.row.description || '-' }}</template>
      </el-table-column>
      <el-table-column label="核算工作量" align="center" prop="calculatedWorkload" width="100">
        <template #default="scope">
          <span class="workload-num">{{ scope.row.calculatedWorkload }}</span>
        </template>
      </el-table-column>
      <el-table-column label="超标" align="center" prop="isOverLimit" width="70">
        <template #default="scope">
          <biz-tag v-if="scope.row.isOverLimit === 1" :value="1" :map="yesNoMap" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="院部审批" align="center" prop="deanApprovalStatus" width="90">
        <template #default="scope">
          <biz-tag :value="scope.row.deanApprovalStatus" :map="approvalStatusMap" />
        </template>
      </el-table-column>
      <el-table-column label="申诉" align="center" prop="appealStatus" width="80">
        <template #default="scope">
          <biz-tag :value="scope.row.appealStatus" :map="appealStatusMap" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="85">
        <template #default="scope">
          <biz-tag :value="scope.row.status" :map="workloadItemStatusMap" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="270" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
          <el-button link type="primary" icon="Refresh" @click="handleRecalcItem(scope.row)" v-hasPermi="['system:workloadItem:edit']">重算</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:workloadItem:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:workloadItem:remove']">删除</el-button>
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

    <!-- 查看详情对话框 -->
    <el-dialog title="工作量明细详情" v-model="detailOpen" width="600px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="明细ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="教师">{{ userLabel(detailData.userId) }}</el-descriptions-item>
        <el-descriptions-item label="学年学期">{{ detailData.semester }}</el-descriptions-item>
        <el-descriptions-item label="类别">
          <biz-tag :value="detailData.itemType" :map="itemTypeMap" />
        </el-descriptions-item>
        <el-descriptions-item label="来源">
          <biz-tag :value="detailData.sourceType" :map="sourceTypeMap" />
        </el-descriptions-item>
        <el-descriptions-item label="核算工作量">
          <span class="workload-num">{{ detailData.calculatedWorkload }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="课程/事项" :span="2">{{ detailData.courseName || detailData.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="层次">{{ detailData.educationLevel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="专业类别">{{ detailData.majorCategory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="超标">
          <biz-tag v-if="detailData.isOverLimit === 1" :value="1" :map="yesNoMap" />
          <span v-else>否</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <biz-tag :value="detailData.status" :map="workloadItemStatusMap" />
        </el-descriptions-item>
        <el-descriptions-item label="院部审批">
          <biz-tag :value="detailData.deanApprovalStatus" :map="approvalStatusMap" />
        </el-descriptions-item>
        <el-descriptions-item label="申诉状态">
          <biz-tag :value="detailData.appealStatus" :map="appealStatusMap" />
        </el-descriptions-item>
        <el-descriptions-item label="教学任务ID">{{ detailData.taskId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="岗位任职ID">{{ detailData.assignmentId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申诉原因" :span="2">{{ detailData.appealReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申诉回复" :span="2">{{ detailData.appealReply || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailData.updateTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加或修改工作量明细对话框 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="workloadItemRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <!-- 编辑态后端 edit 白名单不含 userId，禁用避免静默丢弃 -->
            <el-form-item label="教师" prop="userId">
              <user-select v-model="form.userId" :disabled="form.id != null" :placeholder="form.id != null ? '编辑时不可修改归属教师' : undefined" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学年学期" prop="semester">
              <semester-select v-model="form.semester" width="100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类别" prop="itemType">
              <el-select v-model="form.itemType" placeholder="请选择类别" style="width: 100%">
                <el-option v-for="o in itemTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <!-- 编辑态后端 edit 白名单不含 calculatedWorkload，禁用避免静默丢弃；核算值请用列表「重算」 -->
            <el-form-item label="核算工作量" prop="calculatedWorkload">
              <el-input-number v-model="form.calculatedWorkload" :min="0" :precision="2" controls-position="right" :disabled="form.id != null" style="width: 100%" />
              <div v-if="form.id != null" class="field-readonly-tip">编辑时不可手改核算值，请在列表中对该明细执行「重算」</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" placeholder="课程类工作量填写" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="层次" prop="educationLevel" label-width="60px">
              <el-select v-model="form.educationLevel" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in educationLevelOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="类别" prop="majorCategory" label-width="60px">
              <el-select v-model="form.majorCategory" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in majorCategoryOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="事项说明" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="G8/G9 等其他工作量请填写说明" />
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
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WorkloadItem">
import { listWorkloadItem, getWorkloadItem, delWorkloadItem, addWorkloadItem, updateWorkloadItem } from "@/api/system/workloadItem"
import { recalcItem, recalcItems } from "@/api/system/calc"
import UserSelect from '@/components/UserSelect/index.vue'
import SemesterSelect from '@/components/SemesterSelect/index.vue'
import { useUserMap } from '@/utils/userCache'
import {
  itemTypeOptions, educationLevelOptions, majorCategoryOptions,
  workloadItemStatusMap, approvalStatusMap, appealStatusMap, yesNoMap
} from '@/utils/bizDict'

const { proxy } = getCurrentInstance()
const { userLabel } = useUserMap()

const itemTypeMap = Object.fromEntries(itemTypeOptions.map(o => [o.value, { label: o.label, type: 'primary' }]))
const sourceTypeMap = { IMPORT: { label: '导入', type: 'info' }, MANUAL: { label: '手工', type: 'success' } }

const workloadItemList = ref([])
const open = ref(false)
const detailOpen = ref(false)
const detailData = ref({})
const loading = ref(true)
const recalcLoading = ref(false)
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
    userId: null,
    semester: null,
    itemType: null,
    status: null
  },
  rules: {
    userId: [{ required: true, message: "请选择教师", trigger: "change" }],
    semester: [
      { required: true, message: "请输入学年学期", trigger: "blur" },
      { pattern: /^\d{4}-\d{4}-[12]$/, message: "格式如 2025-2026-1", trigger: "blur" }
    ],
    itemType: [{ required: true, message: "请选择工作量类别", trigger: "change" }],
    calculatedWorkload: [{ required: true, message: "请输入核算工作量", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询工作量明细主表列表 */
function getList() {
  loading.value = true
  listWorkloadItem(queryParams.value).then(response => {
    workloadItemList.value = response.rows
    total.value = response.total
  }).catch(() => {
    proxy.$modal.msgError("获取工作量明细列表失败")
  }).finally(() => {
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
    userId: null,
    semester: null,
    academicYear: null,
    itemType: null,
    sourceType: 'MANUAL',
    taskId: null,
    assignmentId: null,
    courseName: null,
    educationLevel: null,
    majorCategory: null,
    calculatedWorkload: null,
    description: null,
    isOverLimit: 0,
    deanApprovalStatus: 0,
    deanApprovalBy: null,
    deanApprovalTime: null,
    appealStatus: 0,
    appealReason: null,
    appealReply: null,
    status: 0,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("workloadItemRef")
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
  title.value = "添加工作量明细"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getWorkloadItem(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改工作量明细"
  })
}

/** 查看详情 */
function handleDetail(row) {
  getWorkloadItem(row.id).then(response => {
    detailData.value = response.data
    detailOpen.value = true
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["workloadItemRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateWorkloadItem(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addWorkloadItem(form.value).then(() => {
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
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除选中的工作量明细？').then(function() {
    return delWorkloadItem(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 单条重算 */
function handleRecalcItem(row) {
  proxy.$modal.confirm(`确认重算「${userLabel(row.userId)}」的明细 #${row.id} 吗？`).then(function() {
    return recalcItem(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("重算完成")
  }).catch(() => {})
}

/** 按教师+学期批量重算 */
function handleRecalcSemester() {
  const { userId, semester } = queryParams.value
  if (!userId || !semester) {
    proxy.$modal.alertWarning('请先在搜索栏选择「教师」并填写「学年学期」')
    return
  }
  proxy.$modal.confirm(`确认重算「${userLabel(userId)}」${semester} 学期全部未冻结明细吗？`).then(function() {
    recalcLoading.value = true
    return recalcItems(userId, semester)
  }).then((res) => {
    getList()
    proxy.$modal.msgSuccess(`重算完成，共 ${res.data ?? 0} 条明细`)
  }).catch(() => {}).finally(() => {
    recalcLoading.value = false
  })
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/workloadItem/export', {
    ...queryParams.value
  }, `workloadItem_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
.workload-num {
  font-weight: 600;
  color: var(--el-color-primary);
}
.field-readonly-tip {
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary);
}
.field-readonly-tip {
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary);
}
</style>

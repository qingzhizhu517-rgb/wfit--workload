<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span style="font-weight: 600;">自主申报工作量</span>
          <el-tag type="info">仅限 G8/G9/G11 类别</el-tag>
        </div>
      </template>

      <el-form ref="declareRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="学年学期" prop="semester">
              <el-input v-model="form.semester" placeholder="如 2025-2026-1" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工作量类别" prop="itemType">
              <el-select v-model="form.itemType" placeholder="请选择类别" style="width: 100%"
                         @change="onTypeChange">
                <el-option label="G8 第二课堂" value="G8" />
                <el-option label="G9 其他工作量" value="G9" />
                <el-option label="G11 管理服务" value="G11" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="项目名称" prop="courseName">
              <el-input v-model="form.courseName" :placeholder="namePlaceholder" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="核定工作量" prop="calculatedWorkload">
              <el-input-number v-model="form.calculatedWorkload" :min="0.1" :precision="1"
                               controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.itemType === 'G11'">
            <el-form-item label="岗位类型" prop="description">
              <el-select v-model="form.description" placeholder="请选择岗位" style="width: 100%">
                <el-option label="班主任" value="班主任" />
                <el-option label="教研室主任" value="教研室主任" />
                <el-option label="系主任" value="系主任" />
                <el-option label="其他管理岗位" value="其他管理岗位" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="说明" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="3"
                        placeholder="请输入工作量说明（如：指导学生竞赛、参与招生宣传等）" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider />
        <el-row>
          <el-col :span="24" style="text-align: center;">
            <el-button type="primary" :loading="submitting" @click="submitForm">提交申报</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 我的申报记录 -->
    <el-card shadow="never" style="margin-top: 16px;">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span style="font-weight: 600;">我的申报记录</span>
          <el-button type="primary" link icon="Refresh" @click="getMyList">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="listLoading" :data="myList" stripe>
        <el-table-column label="学期" prop="semester" width="120" />
        <el-table-column label="类别" prop="itemType" width="80">
          <template #default="scope">
            <el-tag :type="typeTagMap[scope.row.itemType]" size="small">{{ scope.row.itemType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="项目名称" prop="courseName" min-width="160" show-overflow-tooltip />
        <el-table-column label="核定工作量" prop="calculatedWorkload" width="100" align="center" />
        <el-table-column label="状态" prop="status" width="90" align="center">
          <template #default="scope">
            <el-tag :type="statusTagMap[scope.row.status]" size="small">{{ statusLabelMap[scope.row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申报时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="scope">
            <el-button link type="danger" size="small" icon="Delete"
                       v-if="scope.row.status === 0"
                       @click="handleDelete(scope.row)">撤回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                  v-model:limit="queryParams.pageSize" @pagination="getMyList" />
    </el-card>
  </div>
</template>

<script setup name="MyWorkloadDeclare">
import { listWorkloadItem, addWorkloadItem, delWorkloadItem } from "@/api/system/workloadItem"
import { getCurrentSemester } from "@/utils/bizDict"
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()

const myList = ref([])
const listLoading = ref(true)
const submitting = ref(false)
const total = ref(0)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  userId: userStore.id,
  sourceType: 'SELF'
})

const form = ref({
  semester: getCurrentSemester(),
  itemType: 'G8',
  courseName: '',
  calculatedWorkload: null,
  description: '',
  remark: ''
})

const rules = {
  semester: [
    { required: true, message: '请输入学年学期', trigger: 'blur' },
    { pattern: /^\d{4}-\d{4}-[12]$/, message: '格式如 2025-2026-1', trigger: 'blur' }
  ],
  itemType: [{ required: true, message: '请选择工作量类别', trigger: 'change' }],
  courseName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  calculatedWorkload: [{ required: true, message: '请输入核定工作量', trigger: 'blur' }]
}

const typeTagMap = { G8: 'success', G9: 'warning', G11: 'primary' }
const statusTagMap = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
const statusLabelMap = { 0: '草稿', 1: '已核对', 2: '有异议', 3: '已驳回' }

const namePlaceholder = computed(() => {
  const map = {
    G8: '如：指导学生社团活动、组织学科竞赛',
    G9: '如：参与招生宣传、社会服务',
    G11: '如：担任计科2301班班主任'
  }
  return map[form.value.itemType] || '请输入项目名称'
})

function onTypeChange() {
  form.value.description = ''
  form.value.courseName = ''
}

function getMyList() {
  listLoading.value = true
  listWorkloadItem(queryParams.value).then(res => {
    myList.value = res.rows
    total.value = res.total
    listLoading.value = false
  })
}

function submitForm() {
  proxy.$refs['declareRef'].validate(valid => {
    if (!valid) return
    submitting.value = true
    const data = {
      ...form.value,
      sourceType: 'SELF',
      status: 0
    }
    addWorkloadItem(data).then(() => {
      proxy.$modal.msgSuccess('申报成功')
      submitting.value = false
      resetForm()
      getMyList()
    }).catch(() => {
      submitting.value = false
    })
  })
}

function resetForm() {
  form.value = {
    semester: getCurrentSemester(),
    itemType: 'G8',
    courseName: '',
    calculatedWorkload: null,
    description: '',
    remark: ''
  }
  proxy.resetForm('declareRef')
}

function handleDelete(row) {
  proxy.$modal.confirm('确认撤回该申报？').then(() => {
    return delWorkloadItem(row.id)
  }).then(() => {
    getMyList()
    proxy.$modal.msgSuccess('已撤回')
  }).catch(() => {})
}

getMyList()
</script>

<style scoped>
:deep(.el-card__header) {
  padding: 12px 20px;
}
</style>

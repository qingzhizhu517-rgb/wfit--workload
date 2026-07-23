<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="教师" prop="userId">
        <user-select v-model="queryParams.userId" style="width: 200px" />
      </el-form-item>
      <el-form-item label="学年学期" prop="semester">
        <el-input v-model="queryParams.semester" placeholder="如 2025-2026-1" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="课程名称" prop="courseName">
        <el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="课程层次" prop="educationLevel">
        <el-select v-model="queryParams.educationLevel" placeholder="请选择" clearable style="width: 150px">
          <el-option v-for="o in educationLevelOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="课程类别" prop="majorCategory">
        <el-select v-model="queryParams.majorCategory" placeholder="请选择" clearable style="width: 130px">
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:teachingTask:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:teachingTask:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:teachingTask:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Upload2" @click="handleImport" v-hasPermi="['system:teachingTask:import']">Excel导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:teachingTask:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="teachingTaskList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="教师" align="center" prop="userId" width="160">
        <template #default="scope">{{ userLabel(scope.row.userId) }}</template>
      </el-table-column>
      <el-table-column label="学年学期" align="center" prop="semester" width="110" />
      <el-table-column label="课程名称" align="center" prop="courseName" min-width="160" show-overflow-tooltip />
      <el-table-column label="课程代码" align="center" prop="courseCode" width="100">
        <template #default="scope">{{ scope.row.courseCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="层次" align="center" prop="educationLevel" width="80" />
      <el-table-column label="类别" align="center" prop="majorCategory" width="90" />
      <el-table-column label="性质" align="center" prop="courseNature" width="70" />
      <el-table-column label="合堂人数" align="center" prop="studentCount" width="90" />
      <el-table-column label="理论J1" align="center" prop="theoryHours" width="80" />
      <el-table-column label="实践J2" align="center" prop="practiceHours" width="80" />
      <el-table-column label="重复次" align="center" prop="repeatOrder" width="70" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <biz-tag :value="scope.row.status" :map="normalStatusMap" />
        </template>
      </el-table-column>
      <el-table-column label="导入批次" align="center" prop="importBatch" width="130" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.importBatch || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:teachingTask:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:teachingTask:remove']">删除</el-button>
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

    <!-- Excel 导入对话框 -->
    <el-dialog title="Excel 导入教学任务" v-model="importOpen" width="500px" append-to-body>
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-exceed="handleExceed"
        :on-change="handleFileChange"
        :file-list="importFileList"
        drag
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            <el-button type="primary" link @click="handleDownloadTemplate">下载导入模板</el-button>
            <span style="margin-left: 8px; color: #999;">仅支持 .xlsx / .xls 文件</span>
          </div>
        </template>
      </el-upload>
      <div v-if="importResult" style="margin-top: 16px;">
        <el-alert :type="importResult.failCount > 0 ? 'warning' : 'success'" :closable="false">
          <template #title>
            导入完成：成功 <b>{{ importResult.successCount }}</b> 条，
            失败 <b>{{ importResult.failCount }}</b> 条，
            跳过 <b>{{ importResult.skipCount }}</b> 条
          </template>
        </el-alert>
        <div v-if="importResult.errors && importResult.errors.length > 0" style="margin-top: 8px; max-height: 200px; overflow-y: auto;">
          <p v-for="(err, idx) in importResult.errors" :key="idx" style="color: #e6a23c; font-size: 13px; margin: 4px 0;">
            {{ err }}
          </p>
        </div>
      </div>
      <template #footer>
        <el-button @click="importOpen = false">关闭</el-button>
        <el-button type="primary" :loading="importLoading" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- 添加或修改导入教学任务对话框 -->
    <el-dialog :title="title" v-model="open" width="720px" append-to-body>
      <el-form ref="teachingTaskRef" :model="form" :rules="rules" label-width="90px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="教师" prop="userId">
              <user-select v-model="form.userId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学年学期" prop="semester">
              <el-input v-model="form.semester" placeholder="如 2025-2026-1" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" placeholder="请输入课程名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程代码" prop="courseCode">
              <el-input v-model="form.courseCode" placeholder="请输入课程代码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程层次" prop="educationLevel">
              <el-select v-model="form.educationLevel" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in educationLevelOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程类别" prop="majorCategory">
              <el-select v-model="form.majorCategory" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in majorCategoryOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">核算要素</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="课程性质" prop="courseNature">
              <el-radio-group v-model="form.courseNature">
                <el-radio v-for="o in courseNatureOptions" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程级别" prop="courseLevel">
              <el-select v-model="form.courseLevel" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in courseLevelOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程角色" prop="courseRole">
              <el-select v-model="form.courseRole" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in courseRoleOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合堂人数" prop="studentCount">
              <el-input-number v-model="form.studentCount" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="理论学时" prop="theoryHours">
              <el-input-number v-model="form.theoryHours" :min="0" :precision="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实践学时" prop="practiceHours">
              <el-input-number v-model="form.practiceHours" :min="0" :precision="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="重复次序" prop="repeatOrder">
              <el-input-number v-model="form.repeatOrder" :min="1" :max="10" controls-position="right" style="width: 100%" />
            </el-form-item>
            <div class="form-tip">同名课第几次讲授：1→系数1.0，2→0.9，3及以上→0.8</div>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级名称" prop="className">
              <el-input v-model="form.className" placeholder="如 计科2301/2302合堂" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
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

<script setup name="TeachingTask">
import { listTeachingTask, getTeachingTask, delTeachingTask, addTeachingTask, updateTeachingTask, importTeachingTask } from "@/api/system/teachingTask"
import UserSelect from '@/components/UserSelect/index.vue'
import { useUserMap } from '@/utils/userCache'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  educationLevelOptions, majorCategoryOptions, courseNatureOptions,
  courseLevelOptions, courseRoleOptions, normalStatusMap
} from '@/utils/bizDict'

const { proxy } = getCurrentInstance()
const { userLabel } = useUserMap()

const teachingTaskList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

// 导入相关
const importOpen = ref(false)
const importLoading = ref(false)
const importFileList = ref([])
const importResult = ref(null)
const importFile = ref(null)

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    semester: null,
    courseName: null,
    educationLevel: null,
    majorCategory: null
  },
  rules: {
    userId: [{ required: true, message: "请选择教师", trigger: "change" }],
    semester: [
      { required: true, message: "请输入学年学期", trigger: "blur" },
      { pattern: /^\d{4}-\d{4}-[12]$/, message: "格式如 2025-2026-1", trigger: "blur" }
    ],
    courseName: [{ required: true, message: "请输入课程名称", trigger: "blur" }],
    educationLevel: [{ required: true, message: "请选择课程层次", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询导入教学任务列表 */
function getList() {
  loading.value = true
  listTeachingTask(queryParams.value).then(response => {
    teachingTaskList.value = response.rows
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
    userId: null,
    semester: null,
    academicYear: null,
    courseName: null,
    courseCode: null,
    educationLevel: '本科',
    majorCategory: '理工类',
    courseNature: '必修',
    courseLevel: '其他',
    courseRole: '独立',
    className: null,
    studentCount: 0,
    theoryHours: null,
    practiceHours: null,
    repeatOrder: 1,
    importSource: null,
    importBatch: null,
    importTime: null,
    status: 1,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("teachingTaskRef")
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
  title.value = "添加教学任务"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getTeachingTask(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改教学任务"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["teachingTaskRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateTeachingTask(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addTeachingTask(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除选中的教学任务？删除后需重新核算对应明细。').then(function() {
    return delTeachingTask(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/teachingTask/export', {
    ...queryParams.value
  }, `teachingTask_${new Date().getTime()}.xlsx`)
}

/** Excel 导入按钮操作 */
function handleImport() {
  importFileList.value = []
  importResult.value = null
  importFile.value = null
  importOpen.value = true
}

/** 文件选择 */
function handleFileChange(file) {
  importFile.value = file.raw
}

/** 文件数量超出限制 */
function handleExceed() {
  proxy.$modal.msgWarning('只能上传 1 个文件，请先移除已选文件')
}

/** 下载导入模板 */
function handleDownloadTemplate() {
  proxy.download('system/teachingTask/importTemplate', {}, '教学任务导入模板.xlsx')
}

/** 提交导入 */
function submitImport() {
  if (!importFile.value) {
    proxy.$modal.msgWarning('请先选择 Excel 文件')
    return
  }
  importLoading.value = true
  importResult.value = null
  importTeachingTask(importFile.value).then(response => {
    importResult.value = response.data
    importLoading.value = false
    if (response.data.failCount === 0) {
      proxy.$modal.msgSuccess(`导入成功，共 ${response.data.successCount} 条`)
      getList()
    }
  }).catch(err => {
    importLoading.value = false
    proxy.$modal.msgError('导入失败: ' + (err.message || '未知错误'))
  })
}

getList()
</script>

<style scoped>
.form-tip {
  margin: -14px 0 10px 90px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}
</style>

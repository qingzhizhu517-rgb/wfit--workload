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
        label="教师"
        prop="userId"
      >
        <user-select
          v-model="queryParams.userId"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item
        label="学年学期"
        prop="semester"
      >
        <semester-select
          v-model="queryParams.semester"
          width="170px"
        />
      </el-form-item>
      <el-form-item
        label="课程名称"
        prop="courseName"
      >
        <el-input
          v-model="queryParams.courseName"
          placeholder="请输入课程名称"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item
        label="课程层次"
        prop="educationLevel"
      >
        <el-select
          v-model="queryParams.educationLevel"
          placeholder="请选择"
          clearable
          style="width: 150px"
        >
          <el-option
            v-for="o in educationLevelOptions"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        label="课程类别"
        prop="majorCategory"
      >
        <el-select
          v-model="queryParams.majorCategory"
          placeholder="请选择"
          clearable
          style="width: 130px"
        >
          <el-option
            v-for="o in majorCategoryOptions"
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
          v-hasPermi="['system:teachingTask:add']"
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
          v-hasPermi="['system:teachingTask:edit']"
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
          v-hasPermi="['system:teachingTask:remove']"
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple || deletePending"
          @click="handleDelete"
        >
          删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:teachingTask:import']"
          type="info"
          plain
          icon="Upload2"
          @click="handleImport"
        >
          Excel导入
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:teachingTask:export']"
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
      :data="teachingTaskList"
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
        label="教师"
        align="center"
        prop="userId"
        width="100"
      >
        <template #default="scope">
          {{ userName(scope.row.userId) }}
        </template>
      </el-table-column>
      <el-table-column
        label="工号"
        align="center"
        width="120"
      >
        <template #default="scope">
          {{ userCode(scope.row.userId) }}
        </template>
      </el-table-column>
      <el-table-column
        label="学年学期"
        align="center"
        prop="semester"
        width="110"
      />
      <el-table-column
        label="课程名称"
        align="center"
        prop="courseName"
        min-width="160"
        show-overflow-tooltip
      />
      <el-table-column
        label="课程代码"
        align="center"
        prop="courseCode"
        width="100"
      >
        <template #default="scope">
          {{ scope.row.courseCode || '-' }}
        </template>
      </el-table-column>
      <el-table-column
        label="层次"
        align="center"
        prop="educationLevel"
        width="90"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.educationLevel"
            :map="educationLevelMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="类别"
        align="center"
        prop="majorCategory"
        width="90"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.majorCategory"
            :map="majorCategoryMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="性质"
        align="center"
        prop="courseNature"
        width="80"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.courseNature"
            :map="courseNatureMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="合堂人数"
        align="right"
        prop="studentCount"
        width="90"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.studentCount, 0) }}
        </template>
      </el-table-column>
      <el-table-column
        label="理论J1(学时)"
        align="right"
        prop="theoryHours"
        width="100"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.theoryHours) }}
        </template>
      </el-table-column>
      <el-table-column
        label="实践J2(学时)"
        align="right"
        prop="practiceHours"
        width="100"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.practiceHours) }}
        </template>
      </el-table-column>
      <el-table-column
        label="重复次"
        align="right"
        prop="repeatOrder"
        width="70"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.repeatOrder, 0) }}
        </template>
      </el-table-column>
      <el-table-column
        label="状态"
        align="center"
        prop="status"
        width="80"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.status"
            :map="normalStatusMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="导入批次"
        align="center"
        prop="importBatch"
        width="130"
        show-overflow-tooltip
      >
        <template #default="scope">
          {{ scope.row.importBatch || '-' }}
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
            v-hasPermi="['system:teachingTask:edit']"
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['system:teachingTask:remove']"
            link
            type="primary"
            icon="Delete"
            :disabled="deletePending"
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

    <!-- Excel 两阶段导入对话框（预检 → 确认） -->
    <el-dialog
      v-model="importOpen"
      title="Excel 导入教学任务"
      :close-on-click-modal="!importPending"
      :close-on-press-escape="!importPending"
      :show-close="!importPending"
      width="760px"
      append-to-body
    >
      <div class="import-type-picker">
        <div class="form-tip import-tip">
          通用双 Sheet 模板：在「导入数据」页按类别填写对应数量列
          （G1 计划学时 / G3 天数 / G4 学分+人数 / G5 人数 / G6 周数+人数）。
        </div>
        <div
          v-if="entryCategory"
          class="form-tip import-tip"
        >
          您从 <b>{{ entryCategory }}</b> 分类入口进入：请在「导入数据」页仅填写该类别的行。
        </div>
      </div>
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-exceed="handleExceed"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :file-list="importFileList"
        drag
      >
        <el-icon class="el-icon--upload">
          <upload-filled />
        </el-icon>
        <div class="el-upload__text">
          将 Excel 文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            <el-button
              type="primary"
              link
              @click="handleDownloadTemplate"
            >
              下载导入模板
            </el-button>
            <span style="margin-left: 8px; color: #999;">仅支持 .xlsx / .xls 文件；上传后先预检，确认无阻断行再导入</span>
          </div>
        </template>
      </el-upload>

      <!-- 预检结果 -->
      <div
        v-if="previewResult"
        style="margin-top: 16px;"
      >
        <el-alert
          :type="previewResult.blockedCount > 0 ? 'warning' : 'success'"
          :closable="false"
        >
          <template #title>
            预检完成：共 <b>{{ previewResult.totalCount }}</b> 行，
            可导入 <b>{{ previewResult.readyCount }}</b> 行，
            阻断 <b>{{ previewResult.blockedCount }}</b> 行
            <span v-if="previewResult.blockedCount > 0">（存在阻断行，需先下载错误行、修正后重新上传预检）</span>
          </template>
        </el-alert>
        <div
          v-if="previewResult.warnings && previewResult.warnings.length > 0"
          style="margin-top: 8px;"
        >
          <p
            v-for="(w, idx) in previewResult.warnings"
            :key="idx"
            style="color: #e6a23c; font-size: 13px; margin: 4px 0;"
          >
            {{ w }}
          </p>
        </div>
        <el-table
          :data="previewResult.rows"
          stripe
          border
          max-height="280"
          style="margin-top: 8px;"
          empty-text="无逐行明细"
        >
          <el-table-column
            label="行号"
            prop="rowNo"
            align="center"
            width="70"
          />
          <el-table-column
            label="工号"
            prop="userCode"
            align="center"
            width="110"
          >
            <template #default="scope">
              {{ scope.row.userCode || '-' }}
            </template>
          </el-table-column>
          <el-table-column
            label="姓名"
            prop="userName"
            align="center"
            width="100"
          >
            <template #default="scope">
              {{ scope.row.userName || '-' }}
            </template>
          </el-table-column>
          <el-table-column
            label="状态"
            prop="status"
            align="center"
            width="90"
          >
            <template #default="scope">
              <el-tag :type="rowStatusType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            label="错误信息"
            prop="errorMessage"
            min-width="220"
            show-overflow-tooltip
          >
            <template #default="scope">
              {{ scope.row.errorMessage || '-' }}
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 确认导入结果 -->
      <div
        v-if="confirmResult"
        style="margin-top: 16px;"
      >
        <el-alert
          :type="confirmResult.failCount > 0 ? 'warning' : 'success'"
          :closable="false"
        >
          <template #title>
            导入完成：成功 <b>{{ confirmResult.successCount }}</b> 条，
            跳过 <b>{{ confirmResult.skipCount }}</b> 条，
            失败 <b>{{ confirmResult.failCount }}</b> 条
          </template>
        </el-alert>
        <div
          v-if="confirmResult.errors && confirmResult.errors.length > 0"
          style="margin-top: 8px; max-height: 200px; overflow-y: auto;"
        >
          <p
            v-for="(err, idx) in confirmResult.errors"
            :key="idx"
            style="color: #e6a23c; font-size: 13px; margin: 4px 0;"
          >
            {{ typeof err === 'string' ? err : (err.message || JSON.stringify(err)) }}
          </p>
        </div>
      </div>

      <template #footer>
        <el-button
          :disabled="importPending"
          @click="importOpen = false"
        >
          关闭
        </el-button>
        <el-button
          v-if="errorBatchId"
          type="warning"
          plain
          :disabled="importPending"
          @click="handleDownloadErrors"
        >
          下载错误行
        </el-button>
        <el-button
          type="primary"
          plain
          :loading="previewLoading"
          :disabled="importPending || !!confirmResult"
          @click="submitPreview"
        >
          预检
        </el-button>
        <el-button
          type="primary"
          :loading="confirmLoading"
          :disabled="importPending || !canConfirm"
          @click="submitConfirm"
        >
          确认导入
        </el-button>
      </template>
    </el-dialog>

    <!-- 添加或修改导入教学任务对话框 -->
    <el-dialog
      v-model="open"
      :title="title"
      :close-on-click-modal="!submitLoading"
      :close-on-press-escape="!submitLoading"
      :show-close="!submitLoading"
      width="720px"
      append-to-body
    >
      <el-form
        ref="teachingTaskRef"
        :model="form"
        :rules="rules"
        label-width="90px"
      >
        <el-divider content-position="left">
          基本信息
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item
              label="教师"
              prop="userId"
            >
              <user-select v-model="form.userId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="学年学期"
              prop="semester"
            >
              <semester-select
                v-model="form.semester"
                width="100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="课程名称"
              prop="courseName"
            >
              <el-input
                v-model="form.courseName"
                placeholder="请输入课程名称"
                maxlength="100"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="课程代码"
              prop="courseCode"
            >
              <el-input
                v-model="form.courseCode"
                placeholder="请输入课程代码"
                maxlength="50"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="课程层次"
              prop="educationLevel"
            >
              <el-select
                v-model="form.educationLevel"
                placeholder="请选择"
                style="width: 100%"
              >
                <el-option
                  v-for="o in educationLevelOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="课程类别"
              prop="majorCategory"
            >
              <el-select
                v-model="form.majorCategory"
                placeholder="请选择"
                style="width: 100%"
              >
                <el-option
                  v-for="o in majorCategoryOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">
          核算要素
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item
              label="课程性质"
              prop="courseNature"
            >
              <el-radio-group v-model="form.courseNature">
                <el-radio
                  v-for="o in courseNatureOptions"
                  :key="o.value"
                  :value="o.value"
                >
                  {{ o.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="课程级别"
              prop="courseLevel"
            >
              <el-select
                v-model="form.courseLevel"
                placeholder="请选择"
                style="width: 100%"
              >
                <el-option
                  v-for="o in courseLevelOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="课程角色"
              prop="courseRole"
            >
              <el-select
                v-model="form.courseRole"
                placeholder="请选择"
                style="width: 100%"
              >
                <el-option
                  v-for="o in courseRoleOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="合堂人数"
              prop="studentCount"
            >
              <el-input-number
                v-model="form.studentCount"
                :min="0"
                :max="9999"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="理论学时"
              prop="theoryHours"
            >
              <el-input-number
                v-model="form.theoryHours"
                :min="0"
                :precision="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="实践学时"
              prop="practiceHours"
            >
              <el-input-number
                v-model="form.practiceHours"
                :min="0"
                :precision="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="重复次序"
              prop="repeatOrder"
            >
              <el-input-number
                v-model="form.repeatOrder"
                :min="1"
                :max="10"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
            <div class="form-tip">
              同名课第几次讲授：1→系数1.0，2→0.9，3及以上→0.8
            </div>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="班级名称"
              prop="className"
            >
              <el-input
                v-model="form.className"
                placeholder="如 计科2301/2302合堂"
                maxlength="100"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
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
          <el-button
            :disabled="submitLoading"
            @click="cancel"
          >
            取 消
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="TeachingTask">
import { listTeachingTask, getTeachingTask, delTeachingTask, addTeachingTask, updateTeachingTask, importPreviewTeachingTask, importConfirmTeachingTask, downloadImportErrors } from '@/api/system/teachingTask'
import UserSelect from '@/components/UserSelect/index.vue'
import SemesterSelect from '@/components/SemesterSelect/index.vue'
import { useUserMap } from '@/utils/userCache'
import { saveBlobAsFile } from '@/utils/blobDownload'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  educationLevelOptions, majorCategoryOptions, courseNatureOptions,
  courseLevelOptions, courseRoleOptions, normalStatusMap,
  optionsToMap, formatNumber
} from '@/utils/bizDict'

import { useRoute } from 'vue-router'

const { proxy } = getCurrentInstance()
const route = useRoute()
const { userName, userCode } = useUserMap()

/** 层次/类别/性质列 biz-tag 映射（由 bizDict Options 转换） */
const educationLevelMap = optionsToMap(educationLevelOptions)
const majorCategoryMap = optionsToMap(majorCategoryOptions)
const courseNatureMap = optionsToMap(courseNatureOptions)

const teachingTaskList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const submitLoading = ref(false)
const deletePending = ref(false)

// 导入相关（两阶段：预检 → 确认）
const importOpen = ref(false)
const importPending = ref(false)
const previewLoading = ref(false)
const confirmLoading = ref(false)
const importFileList = ref([])
const importFile = ref(null)
const previewResult = ref(null)
const confirmResult = ref(null)
// 从 G1/G2/G3 分类入口菜单进入时记录的类别：既用于提示，也作为预检/模板下载的 templateType 下发后端锁定类别
const entryCategory = ref('')

// 仅当已预检、0 阻断、且尚未确认过时才允许确认导入
const canConfirm = computed(() =>
  !!previewResult.value && previewResult.value.blockedCount === 0 && !confirmResult.value)

// 存在阻断/失败批次时提供「下载错误行」入口
const errorBatchId = computed(() => {
  if (previewResult.value && previewResult.value.blockedCount > 0) {
    return previewResult.value.batchId
  }
  if (confirmResult.value && confirmResult.value.failCount > 0) {
    return confirmResult.value.batchId
  }
  return null
})

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
    userId: [{ required: true, message: '请选择教师', trigger: 'change' }],
    semester: [
      { required: true, message: '请输入学年学期', trigger: 'blur' },
      { pattern: /^\d{4}-\d{4}-[12]$/, message: '格式如 2025-2026-1', trigger: 'blur' }
    ],
    courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
    educationLevel: [{ required: true, message: '请选择课程层次', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询导入教学任务列表 */
function getList() {
  loading.value = true
  listTeachingTask(queryParams.value).then(response => {
    teachingTaskList.value = response.rows
    total.value = response.total
  }).catch(() => {
    proxy.$modal.msgError('获取教学任务列表失败')
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
  proxy.resetForm('teachingTaskRef')
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
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加教学任务'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getTeachingTask(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改教学任务'
  })
}

/** 提交按钮 */
function submitForm() {
  if (submitLoading.value) {
    return
  }
  proxy.$refs['teachingTaskRef'].validate(async valid => {
    if (!valid || submitLoading.value) {
      return
    }
    submitLoading.value = true
    try {
      const isUpdate = form.value.id !== null && form.value.id !== undefined
      await proxy.$modal.confirm(isUpdate
        ? '确认保存对该教学任务的修改？'
        : '确认新增该教学任务？')
      await (isUpdate ? updateTeachingTask(form.value) : addTeachingTask(form.value))
      proxy.$modal.msgSuccess(isUpdate ? '修改成功' : '新增成功')
      open.value = false
      getList()
    } catch (err) {
      if (err !== 'cancel' && err !== 'close') {
        proxy.$modal.msgError('保存失败: ' + (err?.message || '未知错误'))
      }
    } finally {
      submitLoading.value = false
    }
  })
}

/** 删除按钮操作 */
async function handleDelete(row) {
  if (deletePending.value) {
    return
  }
  const _ids = row?.id || ids.value
  deletePending.value = true
  try {
    await proxy.$modal.confirm('是否确认删除选中的教学任务？删除后需重新核算对应明细。')
    await delTeachingTask(_ids)
    getList()
    proxy.$modal.msgSuccess('删除成功')
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      proxy.$modal.msgError('删除失败: ' + (err?.message || '未知错误'))
    }
  } finally {
    deletePending.value = false
  }
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/teachingTask/export', {
    ...queryParams.value
  }, `teachingTask_${new Date().getTime()}.xlsx`)
}

/** Excel 导入按钮操作 */
function handleImport(templateType) {
  resetImportState()
  // 支持从 G1/G2/G3 分类入口菜单带类别打开；作为预检 templateType 下发后端锁定该类别
  const preset = typeof templateType === 'string' ? templateType.toUpperCase() : ''
  entryCategory.value = ['G1', 'G2', 'G3'].includes(preset) ? preset : ''
  importOpen.value = true
}

/** 重置导入弹窗内的临时状态 */
function resetImportState() {
  importFileList.value = []
  importFile.value = null
  previewResult.value = null
  confirmResult.value = null
}

/** 文件选择：换文件后清空既有预检/确认结果，强制重新预检 */
function handleFileChange(file) {
  importFile.value = file.raw
  previewResult.value = null
  confirmResult.value = null
}

/** 移除已选文件 */
function handleFileRemove() {
  importFile.value = null
  previewResult.value = null
  confirmResult.value = null
}

/** 文件数量超出限制 */
function handleExceed() {
  proxy.$modal.msgWarning('只能上传 1 个文件，请先移除已选文件')
}

/** 行状态标签配色 */
function rowStatusType(status) {
  switch (status) {
  case 'READY':
  case 'IMPORTED':
    return 'success'
  case 'SKIPPED':
    return 'info'
  case 'BLOCKED':
  case 'FAILED':
    return 'danger'
  default:
    return ''
  }
}

/** 下载导入模板（V2 通用双 Sheet 模板；分类入口带 templateType 仅用于文件名标注，列集不变） */
function handleDownloadTemplate() {
  const params = { version: 'V2' }
  if (entryCategory.value) {
    params.templateType = entryCategory.value
  }
  proxy.download('system/teachingTask/importTemplate', params, `教学任务导入模板${entryCategory.value ? '_' + entryCategory.value : ''}.xlsx`)
}

/** 下载错误行清单（BLOCKED/FAILED） */
async function handleDownloadErrors() {
  const batchId = errorBatchId.value
  if (!batchId) {
    return
  }
  try {
    const data = await downloadImportErrors(batchId)
    await saveBlobAsFile(data, `导入错误行_批次${batchId}.xlsx`)
  } catch (err) {
    if (err?.message) {
      // saveBlobAsFile 已弹出后端错误消息，这里避免重复提示
      return
    }
    proxy.$modal.msgError('下载错误行失败')
  }
}

/** 第一步：预检 */
async function submitPreview() {
  if (importPending.value) {
    return
  }
  if (!importFile.value) {
    proxy.$modal.msgWarning('请先选择 Excel 文件')
    return
  }
  importPending.value = true
  previewLoading.value = true
  try {
    previewResult.value = null
    confirmResult.value = null
    // 分类入口带类别锁定：entryCategory 为空则按 ALL（通用模板放行 G1~G6）
    const response = await importPreviewTeachingTask(importFile.value, 'V2', entryCategory.value || 'ALL')
    previewResult.value = response.data
    if (response.data.blockedCount > 0) {
      proxy.$modal.msgWarning(`预检发现 ${response.data.blockedCount} 行阻断，请下载错误行修正后重新上传`)
    } else {
      proxy.$modal.msgSuccess(`预检通过，可导入 ${response.data.readyCount} 行，请点击「确认导入」`)
    }
  } catch (err) {
    proxy.$modal.msgError('预检失败: ' + (err?.message || '未知错误'))
  } finally {
    previewLoading.value = false
    importPending.value = false
  }
}

/** 第二步：确认导入 */
async function submitConfirm() {
  if (importPending.value || !canConfirm.value) {
    return
  }
  importPending.value = true
  try {
    await proxy.$modal.confirm(`确认导入本批次 ${previewResult.value.readyCount} 条教学任务？`)
    confirmLoading.value = true
    const response = await importConfirmTeachingTask({
      batchId: previewResult.value.batchId,
      confirmToken: previewResult.value.confirmToken,
      updateSupport: false
    })
    confirmResult.value = response.data
    if (response.data.failCount === 0) {
      proxy.$modal.msgSuccess(`导入完成：成功 ${response.data.successCount} 条，跳过 ${response.data.skipCount} 条`)
    } else {
      proxy.$modal.msgWarning(`导入完成，但有 ${response.data.failCount} 条失败，可下载错误行查看`)
    }
    if (response.data.successCount > 0) {
      getList()
    }
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      proxy.$modal.msgError('导入失败: ' + (err?.message || '未知错误'))
    }
  } finally {
    confirmLoading.value = false
    importPending.value = false
  }
}

getList()

// 从 G1/G2/G3 分类导入菜单进入时（query.templateType）自动打开对应类别的导入弹窗
if (['G1', 'G2', 'G3'].includes(String(route.query.templateType || '').toUpperCase())) {
  handleImport(route.query.templateType)
}
</script>

<style scoped>
.form-tip {
  margin: -14px 0 10px 90px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

.import-type-picker {
  margin-bottom: 16px;
}

.import-type-picker .import-tip {
  margin: 0 0 6px 0;
}
</style>

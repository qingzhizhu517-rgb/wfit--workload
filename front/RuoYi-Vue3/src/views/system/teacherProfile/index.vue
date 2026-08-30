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
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item
        label="职称"
        prop="title"
      >
        <el-select
          v-model="queryParams.title"
          placeholder="请选择职称"
          clearable
          style="width: 140px"
        >
          <el-option
            v-for="o in teacherTitleOptions"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        label="人员性质"
        prop="teacherNature"
      >
        <el-select
          v-model="queryParams.teacherNature"
          placeholder="请选择人员性质"
          clearable
          style="width: 140px"
        >
          <el-option
            v-for="o in teacherNatureOptions"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        label="特殊状态"
        prop="specialStatus"
      >
        <el-select
          v-model="queryParams.specialStatus"
          placeholder="请选择特殊状态"
          clearable
          style="width: 140px"
        >
          <el-option
            v-for="o in specialStatusOptions"
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
          v-hasPermi="['system:teacherProfile:add']"
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
          v-hasPermi="['system:teacherProfile:edit']"
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
          v-hasPermi="['system:teacherProfile:remove']"
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
          v-hasPermi="['system:teacherProfile:import']"
          type="info"
          plain
          icon="Upload2"
          @click="handleImport"
        >
          导入
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:teacherProfile:export']"
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
      :data="teacherProfileList"
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
        label="序号"
        align="center"
        width="60"
      >
        <template #default="scope">
          {{ (queryParams.pageNum - 1) * queryParams.pageSize + scope.$index + 1 }}
        </template>
      </el-table-column>
      <el-table-column
        label="教师"
        align="center"
        prop="userId"
        width="120"
      >
        <template #default="scope">
          {{ userName(scope.row.userId) }}
        </template>
      </el-table-column>
      <el-table-column
        label="院部"
        align="center"
        prop="deptId"
        min-width="180"
        show-overflow-tooltip
      >
        <template #default="scope">
          {{ deptName(scope.row.userId) }}
        </template>
      </el-table-column>
      <el-table-column
        label="职称"
        align="center"
        prop="title"
        width="100"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.title"
            :map="teacherTitleMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="人员性质"
        align="center"
        prop="teacherNature"
        width="100"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.teacherNature"
            :map="teacherNatureMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="特殊状态"
        align="center"
        prop="specialStatus"
        width="100"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.specialStatus"
            :map="specialStatusMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        width="220"
        fixed="right"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleDetail(scope.row)"
          >
            详情
          </el-button>
          <el-button
            v-hasPermi="['system:teacherProfile:edit']"
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['system:teacherProfile:remove']"
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

    <!-- 查看详情对话框 -->
    <el-dialog
      v-model="detailOpen"
      title="教师档案详情"
      width="580px"
      append-to-body
    >
      <el-descriptions
        :column="2"
        border
      >
        <el-descriptions-item label="教师">
          {{ userName(detailData.userId) }}
        </el-descriptions-item>
        <el-descriptions-item label="工号">
          {{ userCode(detailData.userId) }}
        </el-descriptions-item>
        <el-descriptions-item label="院部">
          {{ deptName(detailData.userId) }}
        </el-descriptions-item>
        <el-descriptions-item label="职称">
          {{ detailData.title || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="人员性质">
          {{ detailData.teacherNature || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="特殊状态">
          <biz-tag
            :value="detailData.specialStatus"
            :map="specialStatusMap"
          />
        </el-descriptions-item>
        <el-descriptions-item label="校企考核">
          {{ detailData.enterpriseEvalResult || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态起">
          {{ parseTime(detailData.specialStatusStart, '{y}-{m}-{d}') || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态止">
          {{ parseTime(detailData.specialStatusEnd, '{y}-{m}-{d}') || '-' }}
        </el-descriptions-item>
        <el-descriptions-item
          label="备注"
          :span="2"
        >
          {{ detailData.remark || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ detailData.createTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ detailData.updateTime || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailOpen = false">
            关 闭
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog
      v-model="upload.open"
      :title="upload.title"
      width="400px"
      append-to-body
    >
      <el-upload
        ref="uploadRef"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :on-error="handleFileError"
        :auto-upload="false"
        drag
      >
        <el-icon class="el-icon--upload">
          <upload-filled />
        </el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <div class="el-upload__tip">
              <el-checkbox v-model="upload.updateSupport" />是否更新已经存在的用户档案
            </div>
            <span>仅允许导入 xls、xlsx 格式文件。</span>
            <el-link
              type="primary"
              :underline="false"
              style="font-size:12px;vertical-align: baseline;"
              @click="importTemplate"
            >
              下载模板
            </el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button
            type="primary"
            @click="submitFileForm"
          >
            确 定
          </el-button>
          <el-button @click="upload.open = false">
            取 消
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加或修改教师业务档案对话框 -->
    <el-dialog
      v-model="open"
      :title="title"
      width="640px"
      append-to-body
    >
      <el-form
        ref="teacherProfileRef"
        :model="form"
        :rules="rules"
        label-width="90px"
      >
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item
              label="教师"
              prop="userId"
            >
              <user-select
                v-model="form.userId"
                :disabled="form.userId != null"
                @select="onTeacherSelect"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="职称"
              prop="title"
            >
              <el-select
                v-model="form.title"
                placeholder="请选择职称"
                style="width: 100%"
              >
                <el-option
                  v-for="o in teacherTitleOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="人员性质"
              prop="teacherNature"
            >
              <el-select
                v-model="form.teacherNature"
                placeholder="请选择人员性质"
                style="width: 100%"
              >
                <el-option
                  v-for="o in teacherNatureOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="特殊状态"
              prop="specialStatus"
            >
              <el-select
                v-model="form.specialStatus"
                placeholder="请选择特殊状态"
                style="width: 100%"
              >
                <el-option
                  v-for="o in specialStatusOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="校企考核"
              prop="enterpriseEvalResult"
            >
              <el-select
                v-model="form.enterpriseEvalResult"
                placeholder="请选择考核结果"
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="o in enterpriseEvalOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="状态起"
              prop="specialStatusStart"
            >
              <el-date-picker
                v-model="form.specialStatusStart"
                clearable
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="开始日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="状态止"
              prop="specialStatusEnd"
            >
              <el-date-picker
                v-model="form.specialStatusEnd"
                clearable
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="结束日期"
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

<script setup name="TeacherProfile">
import { listTeacherProfile, getTeacherProfile, delTeacherProfile, addTeacherProfile, updateTeacherProfile } from '@/api/system/teacherProfile'
import UserSelect from '@/components/UserSelect/index.vue'
import { useUserMap, refresh as refreshUserCache } from '@/utils/userCache'
import { getToken } from '@/utils/auth'
import {
  teacherTitleOptions, teacherNatureOptions, specialStatusOptions, enterpriseEvalOptions,
  specialStatusMap, optionsToMap
} from '@/utils/bizDict'

const { proxy } = getCurrentInstance()
const { userName, userCode, deptName } = useUserMap()

/** 职称/人员性质列 biz-tag 映射（由 bizDict Options 转换） */
const teacherTitleMap = optionsToMap(teacherTitleOptions)
const teacherNatureMap = optionsToMap(teacherNatureOptions)

const teacherProfileList = ref([])
const open = ref(false)
const detailOpen = ref(false)
const detailData = ref({})
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const upload = reactive({
  open: false,
  title: '导入教师档案',
  isUploading: false,
  updateSupport: 0,
  headers: { Authorization: 'Bearer ' + getToken() },
  url: import.meta.env.VITE_APP_BASE_API + '/system/teacherProfile/importData'
})

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    title: null,
    teacherNature: null,
    specialStatus: null
  },
  rules: {
    userId: [{ required: true, message: '请选择教师', trigger: 'change' }],
    title: [{ required: true, message: '请选择职称', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询教师业务档案列表 */
function getList() {
  loading.value = true
  listTeacherProfile(queryParams.value).then(response => {
    teacherProfileList.value = response.rows
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
    userId: null,
    title: null,
    teacherNature: '专任',
    specialStatus: '正常',
    specialStatusStart: null,
    specialStatusEnd: null,
    enterpriseEvalResult: null,
    deptId: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm('teacherProfileRef')
}

/** 选择教师后自动带出院部 */
function onTeacherSelect(user) {
  form.value.deptId = user?.deptId ?? null
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
  ids.value = selection.map(item => item.userId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加教师业务档案'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _userId = row.userId || ids.value
  getTeacherProfile(_userId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改教师业务档案'
  })
}

/** 查看详情 */
function handleDetail(row) {
  getTeacherProfile(row.userId).then(response => {
    detailData.value = response.data
    detailOpen.value = true
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['teacherProfileRef'].validate(valid => {
    if (valid) {
      if (form.value.userId != null && title.value.startsWith('修改')) {
        updateTeacherProfile(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addTeacherProfile(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _userIds = row.userId || ids.value
  proxy.$modal.confirm('是否确认删除所选教师业务档案？').then(function() {
    return delTeacherProfile(_userIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/teacherProfile/export', {
    ...queryParams.value
  }, `teacherProfile_${new Date().getTime()}.xlsx`)
}

/** 导入按钮操作 */
function handleImport() {
  upload.title = '导入教师档案'
  upload.open = true
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download('system/teacherProfile/importTemplate', {}, 'teacherProfile_template.xlsx')
}

/** 文件上传中处理 */
function handleFileUploadProgress(event, file, fileList) {
  upload.isUploading = true
}

/** 文件上传成功处理 */
function handleFileSuccess(response, file, fileList) {
  upload.open = false
  upload.isUploading = false
  proxy.$refs['uploadRef'].clearFiles()
  proxy.$alert('<div style=\'overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;\'>' + response.msg + '</div>', '导入结果', { dangerouslyUseHTMLString: true })
  // 导入会新增用户，先刷新用户缓存，教师/院部列才能正确回显姓名而非裸 userId
  refreshUserCache().then(getList)
}

/** 文件上传失败处理 */
function handleFileError(err, file, fileList) {
  upload.isUploading = false
  proxy.$modal.msgError('导入失败，请检查文件格式或网络连接')
}

/** 提交上传文件 */
function submitFileForm() {
  proxy.$refs['uploadRef'].submit()
}

getList()
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span style="font-weight: 600;">自主申报工作量</span>
          <el-tag type="info">
            仅限 G8/G9 类别（G11 由教务同步岗位减免）
          </el-tag>
        </div>
      </template>

      <el-form
        ref="declareRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-row :gutter="24">
          <el-col
            :xs="24"
            :sm="12"
          >
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
          <el-col
            :xs="24"
            :sm="12"
          >
            <el-form-item
              label="工作量类别"
              prop="itemType"
            >
              <el-select
                v-model="form.itemType"
                placeholder="请选择类别"
                style="width: 100%"
                @change="onTypeChange"
              >
                <el-option
                  label="G8 第二课堂"
                  value="G8"
                />
                <el-option
                  label="G9 其他工作量"
                  value="G9"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item
              label="项目名称"
              prop="courseName"
            >
              <el-input
                v-model="form.courseName"
                :placeholder="namePlaceholder"
                maxlength="100"
              />
            </el-form-item>
          </el-col>
          <el-col
            :xs="24"
            :sm="12"
          >
            <el-form-item
              label="核定工作量"
              prop="calculatedWorkload"
            >
              <el-input-number
                v-model="form.calculatedWorkload"
                :min="0.1"
                :precision="1"
                controls-position="right"
                style="width: 100%"
              />
              <div class="form-tip">
                请根据管理办法填写核定学时，G8/G9 由教务处确认
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item
              label="说明"
              prop="description"
            >
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="3"
                placeholder="请输入工作量说明（如：指导学生竞赛、参与招生宣传等）"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="2"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider />
        <el-row>
          <el-col
            :span="24"
            style="text-align: center;"
          >
            <el-button
              type="primary"
              :loading="submitting"
              @click="submitForm"
            >
              提交申报
            </el-button>
            <el-button @click="resetForm">
              重置
            </el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 我的申报记录 -->
    <el-card
      shadow="never"
      style="margin-top: 16px;"
    >
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span style="font-weight: 600;">我的申报记录</span>
          <el-button
            type="primary"
            link
            icon="Refresh"
            @click="getMyList"
          >
            刷新
          </el-button>
        </div>
      </template>

      <el-table
        v-loading="listLoading"
        :data="myList"
        stripe
        empty-text="暂无数据"
      >
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
          label="学期"
          align="center"
          prop="semester"
          width="110"
        />
        <el-table-column
          label="类别"
          align="center"
          prop="itemType"
          width="130"
        >
          <template #default="scope">
            <biz-tag
              :value="scope.row.itemType"
              :map="itemTypeMap"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="项目名称"
          prop="courseName"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          label="核定工作量(学时)"
          prop="calculatedWorkload"
          width="120"
          align="right"
        >
          <template #default="scope">
            {{ formatNumber(scope.row.calculatedWorkload) }}
          </template>
        </el-table-column>
        <el-table-column
          label="来源"
          prop="sourceType"
          width="100"
          align="center"
        >
          <template #default="scope">
            <biz-tag
              :value="scope.row.sourceType"
              :map="sourceTypeMap"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="状态"
          prop="status"
          width="90"
          align="center"
        >
          <template #default="scope">
            <biz-tag
              :value="scope.row.status"
              :map="workloadItemStatusMap"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="申报时间"
          align="center"
          width="160"
        >
          <template #default="scope">
            {{ scope.row.createTime ? parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') : '-' }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="140"
          align="center"
          fixed="right"
          class-name="small-padding fixed-width"
        >
          <template #default="scope">
            <el-button
              link
              type="primary"
              size="small"
              icon="View"
              @click="handleDetail(scope.row)"
            >
              详情
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              link
              type="danger"
              size="small"
              icon="Delete"
              @click="handleDelete(scope.row)"
            >
              撤回
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getMyList"
      />
    </el-card>

    <!-- 查看详情对话框 -->
    <el-dialog
      v-model="detailOpen"
      title="申报详情"
      width="min(560px, 94vw)"
      append-to-body
    >
      <el-descriptions
        :column="detailColumns"
        border
      >
        <el-descriptions-item label="明细ID">
          {{ detailData.id }}
        </el-descriptions-item>
        <el-descriptions-item label="类别">
          <biz-tag
            :value="detailData.itemType"
            :map="itemTypeMap"
          />
        </el-descriptions-item>
        <el-descriptions-item label="学期">
          {{ detailData.semester }}
        </el-descriptions-item>
        <el-descriptions-item label="核定工作量(学时)">
          <span style="font-weight: 600; color: var(--el-color-primary);">{{ formatNumber(detailData.calculatedWorkload) }}</span>
        </el-descriptions-item>
        <el-descriptions-item
          label="项目名称"
          :span="2"
        >
          {{ detailData.courseName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <biz-tag
            :value="detailData.status"
            :map="workloadItemStatusMap"
          />
        </el-descriptions-item>
        <el-descriptions-item label="来源">
          <biz-tag
            :value="detailData.sourceType"
            :map="sourceTypeMap"
          />
        </el-descriptions-item>
        <el-descriptions-item
          label="说明"
          :span="2"
        >
          {{ detailData.description || '-' }}
        </el-descriptions-item>
        <el-descriptions-item
          label="备注"
          :span="2"
        >
          {{ detailData.remark || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="申报时间">
          {{ detailData.createTime ? parseTime(detailData.createTime, '{y}-{m}-{d} {h}:{i}') : '-' }}
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
  </div>
</template>

<script setup name="MyWorkloadDeclare">
import { useWindowSize } from '@vueuse/core'
import { listWorkloadItem, getWorkloadItem, addWorkloadItem, delWorkloadItem } from '@/api/system/workloadItem'
import { getCurrentSemester, workloadItemStatusMap, itemTypeMap, sourceTypeMap, formatNumber } from '@/utils/bizDict'
import SemesterSelect from '@/components/SemesterSelect/index.vue'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()

const myList = ref([])
const listLoading = ref(true)
const submitting = ref(false)
const total = ref(0)
const detailOpen = ref(false)
const detailData = ref({})
let listRequestId = 0
let detailRequestId = 0
const { width: windowWidth } = useWindowSize()
const detailColumns = computed(() => windowWidth.value < 640 ? 1 : 2)

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
  semester: [{ required: true, message: '请选择学年学期', trigger: 'change' }],
  itemType: [{ required: true, message: '请选择工作量类别', trigger: 'change' }],
  courseName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  calculatedWorkload: [{ required: true, message: '请输入核定工作量', trigger: 'blur' }]
}

const namePlaceholder = computed(() => {
  const map = {
    G8: '如：指导学生社团活动、组织学科竞赛',
    G9: '如：参与招生宣传、社会服务'
  }
  return map[form.value.itemType] || '请输入项目名称'
})

function onTypeChange() {
  form.value.courseName = ''
}

function getMyList() {
  const requestId = ++listRequestId
  listLoading.value = true
  listWorkloadItem(queryParams.value).then(res => {
    if (requestId !== listRequestId) return
    myList.value = res.rows
    total.value = res.total
  }).catch(() => {
    if (requestId !== listRequestId) return
    proxy.$modal.msgError('获取申报记录失败')
  }).finally(() => {
    if (requestId === listRequestId) listLoading.value = false
  })
}

function submitForm() {
  // 教师自主申报仅限 G8/G9；G11 由教务导入/维护本学期岗位减免统一同步，不走自报
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
      resetForm()
      getMyList()
    }).catch(() => {
      proxy.$modal.msgError('申报提交失败，请检查填写内容后重试')
    }).finally(() => {
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

async function handleDetail(row) {
  const requestId = ++detailRequestId
  try {
    const res = await getWorkloadItem(row.id)
    if (requestId !== detailRequestId) return
    detailData.value = res.data || {}
    detailOpen.value = true
  } catch {
    if (requestId !== detailRequestId) return
    proxy.$modal.msgError('申报详情加载失败，请稍后重试')
  }
}

async function handleDelete(row) {
  try {
    await proxy.$modal.confirm('确认撤回该申报？')
  } catch {
    return
  }
  try {
    await delWorkloadItem(row.id)
    getMyList()
    proxy.$modal.msgSuccess('已撤回')
  } catch {
    proxy.$modal.msgError('撤回失败，请确认该申报仍为草稿状态')
  }
}

getMyList()
</script>

<style scoped>
:deep(.el-card__header) {
  padding: 12px 20px;
}

.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.2;
  margin-top: 4px;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-divider) {
  margin: 16px 0;
}
</style>

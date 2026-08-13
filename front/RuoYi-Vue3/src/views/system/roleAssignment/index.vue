<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="教师" prop="userId">
        <user-select v-model="queryParams.userId" style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="岗位" prop="roleType">
        <el-select v-model="queryParams.roleType" placeholder="请选择岗位" clearable style="width: 150px">
          <el-option v-for="o in roleTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="学年" prop="academicYear">
        <el-input
          v-model="queryParams.academicYear"
          placeholder="如 2025-2026"
          clearable
          style="width: 140px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:roleAssignment:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:roleAssignment:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:roleAssignment:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:roleAssignment:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="roleAssignmentList" stripe empty-text="暂无数据" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="教师" align="center" prop="userId" width="150">
        <template #default="scope">{{ userLabel(scope.row.userId) }}</template>
      </el-table-column>
      <el-table-column label="岗位" align="center" prop="roleType" width="110">
        <template #default="scope">
          <biz-tag :value="scope.row.roleType" :map="roleTypeMap" />
        </template>
      </el-table-column>
      <el-table-column label="目标班级或范围" align="center" prop="target" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.target || '-' }}</template>
      </el-table-column>
      <el-table-column label="任职区间" align="center" width="200">
        <template #default="scope">
          <span>{{ scope.row.startDate ? parseTime(scope.row.startDate, '{y}-{m}-{d}') : '-' }} ~ {{ scope.row.endDate ? parseTime(scope.row.endDate, '{y}-{m}-{d}') : '至今' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="学年" align="center" prop="academicYear" width="100">
        <template #default="scope">{{ scope.row.academicYear || '-' }}</template>
      </el-table-column>
      <el-table-column label="标准学时/学年(学时)" align="right" prop="allowanceRate" width="150">
        <template #default="scope">
          <span class="rate-num">{{ formatNumber(scope.row.allowanceRate) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <biz-tag :value="scope.row.status" :map="normalStatusMap" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:roleAssignment:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:roleAssignment:remove']">删除</el-button>
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

    <!-- 添加或修改岗位任职对话框 -->
    <el-dialog :title="title" v-model="open" width="620px" append-to-body>
      <el-form ref="roleAssignmentRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="教师" prop="userId">
              <user-select v-model="form.userId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位" prop="roleType">
              <el-select v-model="form.roleType" placeholder="请选择岗位" style="width: 100%">
                <el-option v-for="o in roleTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标范围" prop="target">
              <el-input v-model="form.target" maxlength="100" placeholder="如：23级软件1班" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任职起" prop="startDate">
              <el-date-picker clearable v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任职止" prop="endDate">
              <el-date-picker clearable v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="留空表示至今" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学年" prop="academicYear">
              <el-input v-model="form.academicYear" placeholder="如 2025-2026" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标准学时" prop="allowanceRate">
              <el-input-number v-model="form.allowanceRate" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="form-tip">标准学时：该岗位每学年折算的管理服务工作量（G11），按任职区间自动折算到学期</div>
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

<script setup name="RoleAssignment">
import { listRoleAssignment, getRoleAssignment, delRoleAssignment, addRoleAssignment, updateRoleAssignment } from "@/api/system/roleAssignment"
import UserSelect from '@/components/UserSelect/index.vue'
import { useUserMap } from '@/utils/userCache'
import { roleTypeOptions, normalStatusMap, roleTypeMap, formatNumber } from '@/utils/bizDict'

const { proxy } = getCurrentInstance()
const { userLabel } = useUserMap()

const roleAssignmentList = ref([])
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
    userId: null,
    roleType: null,
    academicYear: null
  },
  rules: {
    userId: [{ required: true, message: "请选择教师", trigger: "change" }],
    roleType: [{ required: true, message: "请选择岗位", trigger: "change" }],
    startDate: [{ required: true, message: "任职起日期不能为空", trigger: "change" }],
    allowanceRate: [{ required: true, message: "标准学时不能为空", trigger: "blur" }],
    academicYear: [{ pattern: /^\d{4}-\d{4}$/, message: "格式如 2025-2026", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询岗位任职列表 */
function getList() {
  loading.value = true
  listRoleAssignment(queryParams.value).then(response => {
    roleAssignmentList.value = response.rows
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
    roleType: null,
    target: null,
    startDate: null,
    endDate: null,
    semester: null,
    academicYear: null,
    allowanceRate: null,
    status: 1,
    remark: null
  }
  proxy.resetForm("roleAssignmentRef")
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
  title.value = "添加岗位任职"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getRoleAssignment(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改岗位任职"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["roleAssignmentRef"].validate(valid => {
    if (valid) {
      submitLoading.value = true
      const req = form.value.id != null ? updateRoleAssignment(form.value) : addRoleAssignment(form.value)
      req.then(() => {
        proxy.$modal.msgSuccess(form.value.id != null ? "修改成功" : "新增成功")
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
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除选中的岗位任职记录？').then(function() {
    return delRoleAssignment(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/roleAssignment/export', {
    ...queryParams.value
  }, `roleAssignment_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
.rate-num {
  font-weight: 600;
  color: var(--el-color-primary);
}
</style>

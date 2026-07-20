<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="职称(教授/副教授/讲师/助教/未定级)" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入职称(教授/副教授/讲师/助教/未定级)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="人员性质(专任/外聘/校企/银龄/青州外聘)" prop="teacherNature">
        <el-input
          v-model="queryParams.teacherNature"
          placeholder="请输入人员性质(专任/外聘/校企/银龄/青州外聘)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="特殊状态起" prop="specialStatusStart">
        <el-date-picker clearable
          v-model="queryParams.specialStatusStart"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择特殊状态起">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="特殊状态止" prop="specialStatusEnd">
        <el-date-picker clearable
          v-model="queryParams.specialStatusEnd"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择特殊状态止">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="校企考核结果(优秀/合格/不合格)" prop="enterpriseEvalResult">
        <el-input
          v-model="queryParams.enterpriseEvalResult"
          placeholder="请输入校企考核结果(优秀/合格/不合格)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="院部(sys_dept.dept_id)" prop="deptId">
        <el-input
          v-model="queryParams.deptId"
          placeholder="请输入院部(sys_dept.dept_id)"
          clearable
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
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['system:teacherProfile:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:teacherProfile:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:teacherProfile:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:teacherProfile:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="teacherProfileList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="教师ID(关联sys_user.user_id)" align="center" prop="userId" />
      <el-table-column label="职称(教授/副教授/讲师/助教/未定级)" align="center" prop="title" />
      <el-table-column label="人员性质(专任/外聘/校企/银龄/青州外聘)" align="center" prop="teacherNature" />
      <el-table-column label="特殊状态(正常/产假/在职读博/访学)" align="center" prop="specialStatus" />
      <el-table-column label="特殊状态起" align="center" prop="specialStatusStart" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.specialStatusStart, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="特殊状态止" align="center" prop="specialStatusEnd" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.specialStatusEnd, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="校企考核结果(优秀/合格/不合格)" align="center" prop="enterpriseEvalResult" />
      <el-table-column label="院部(sys_dept.dept_id)" align="center" prop="deptId" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:teacherProfile:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:teacherProfile:remove']">删除</el-button>
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

    <!-- 添加或修改教师业务档案对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="teacherProfileRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="职称(教授/副教授/讲师/助教/未定级)" prop="title">
              <el-input v-model="form.title" placeholder="请输入职称(教授/副教授/讲师/助教/未定级)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="人员性质(专任/外聘/校企/银龄/青州外聘)" prop="teacherNature">
              <el-input v-model="form.teacherNature" placeholder="请输入人员性质(专任/外聘/校企/银龄/青州外聘)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="特殊状态起" prop="specialStatusStart">
              <el-date-picker clearable
                v-model="form.specialStatusStart"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择特殊状态起">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="特殊状态止" prop="specialStatusEnd">
              <el-date-picker clearable
                v-model="form.specialStatusEnd"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择特殊状态止">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="校企考核结果(优秀/合格/不合格)" prop="enterpriseEvalResult">
              <el-input v-model="form.enterpriseEvalResult" placeholder="请输入校企考核结果(优秀/合格/不合格)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="院部(sys_dept.dept_id)" prop="deptId">
              <el-input v-model="form.deptId" placeholder="请输入院部(sys_dept.dept_id)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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

<script setup name="TeacherProfile">
import { listTeacherProfile, getTeacherProfile, delTeacherProfile, addTeacherProfile, updateTeacherProfile } from "@/api/system/teacherProfile"

const { proxy } = getCurrentInstance()

const teacherProfileList = ref([])
const open = ref(false)
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
    title: null,
    teacherNature: null,
    specialStatus: null,
    specialStatusStart: null,
    specialStatusEnd: null,
    enterpriseEvalResult: null,
    deptId: null,
  },
  rules: {
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
    teacherNature: null,
    specialStatus: null,
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
  proxy.resetForm("teacherProfileRef")
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
  ids.value = selection.map(item => item.userId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加教师业务档案"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _userId = row.userId || ids.value
  getTeacherProfile(_userId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改教师业务档案"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["teacherProfileRef"].validate(valid => {
    if (valid) {
      if (form.value.userId != null) {
        updateTeacherProfile(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addTeacherProfile(form.value).then(() => {
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
  const _userIds = row.userId || ids.value
  proxy.$modal.confirm('是否确认删除教师业务档案编号为"' + _userIds + '"的数据项？').then(function() {
    return delTeacherProfile(_userIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/teacherProfile/export', {
    ...queryParams.value
  }, `teacherProfile_${new Date().getTime()}.xlsx`)
}

getList()
</script>

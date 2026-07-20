<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="${comment}" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入${comment}"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="目标班级或范围" prop="target">
        <el-input
          v-model="queryParams.target"
          placeholder="请输入目标班级或范围"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="任职起" prop="startDate">
        <el-date-picker clearable
          v-model="queryParams.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择任职起">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="任职止(NULL=至今)" prop="endDate">
        <el-date-picker clearable
          v-model="queryParams.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择任职止(NULL=至今)">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="${comment}" prop="semester">
        <el-input
          v-model="queryParams.semester"
          placeholder="请输入${comment}"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="${comment}" prop="academicYear">
        <el-input
          v-model="queryParams.academicYear"
          placeholder="请输入${comment}"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="该岗位标准学时/学年" prop="allowanceRate">
        <el-input
          v-model="queryParams.allowanceRate"
          placeholder="请输入该岗位标准学时/学年"
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
          v-hasPermi="['system:roleAssignment:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:roleAssignment:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:roleAssignment:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:roleAssignment:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="roleAssignmentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="${comment}" align="center" prop="id" />
      <el-table-column label="${comment}" align="center" prop="userId" />
      <el-table-column label="班主任/系主任/教研室主任/专业负责人/俱乐部经理/实验人员/督导/中层副职/心理中心" align="center" prop="roleType" />
      <el-table-column label="目标班级或范围" align="center" prop="target" />
      <el-table-column label="任职起" align="center" prop="startDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="任职止(NULL=至今)" align="center" prop="endDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="${comment}" align="center" prop="semester" />
      <el-table-column label="${comment}" align="center" prop="academicYear" />
      <el-table-column label="该岗位标准学时/学年" align="center" prop="allowanceRate" />
      <el-table-column label="${comment}" align="center" prop="status" />
      <el-table-column label="${comment}" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
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
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="roleAssignmentRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="userId">
              <el-input v-model="form.userId" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="目标班级或范围" prop="target">
              <el-input v-model="form.target" placeholder="请输入目标班级或范围" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="任职起" prop="startDate">
              <el-date-picker clearable
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择任职起">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="任职止(NULL=至今)" prop="endDate">
              <el-date-picker clearable
                v-model="form.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择任职止(NULL=至今)">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="semester">
              <el-input v-model="form.semester" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="academicYear">
              <el-input v-model="form.academicYear" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="该岗位标准学时/学年" prop="allowanceRate">
              <el-input v-model="form.allowanceRate" placeholder="请输入该岗位标准学时/学年" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="remark">
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

<script setup name="RoleAssignment">
import { listRoleAssignment, getRoleAssignment, delRoleAssignment, addRoleAssignment, updateRoleAssignment } from "@/api/system/roleAssignment"

const { proxy } = getCurrentInstance()

const roleAssignmentList = ref([])
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
    userId: null,
    roleType: null,
    target: null,
    startDate: null,
    endDate: null,
    semester: null,
    academicYear: null,
    allowanceRate: null,
    status: null,
  },
  rules: {
    userId: [
      { required: true, message: "$comment不能为空", trigger: "blur" }
    ],
    roleType: [
      { required: true, message: "班主任/系主任/教研室主任/专业负责人/俱乐部经理/实验人员/督导/中层副职/心理中心不能为空", trigger: "change" }
    ],
    startDate: [
      { required: true, message: "任职起不能为空", trigger: "blur" }
    ],
    allowanceRate: [
      { required: true, message: "该岗位标准学时/学年不能为空", trigger: "blur" }
    ],
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
    status: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
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
      if (form.value.id != null) {
        updateRoleAssignment(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addRoleAssignment(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除岗位任职编号为"' + _ids + '"的数据项？').then(function() {
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

<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="教师ID" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入教师ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="学年学期(如2025-2026-1)" prop="semester">
        <el-input
          v-model="queryParams.semester"
          placeholder="请输入学年学期(如2025-2026-1)"
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
      <el-form-item label="课程名称" prop="courseName">
        <el-input
          v-model="queryParams.courseName"
          placeholder="请输入课程名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="课程代码" prop="courseCode">
        <el-input
          v-model="queryParams.courseCode"
          placeholder="请输入课程代码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="本科(含专升本)/专科" prop="educationLevel">
        <el-input
          v-model="queryParams.educationLevel"
          placeholder="请输入本科(含专升本)/专科"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="理工类/文史类/艺术类/其他" prop="majorCategory">
        <el-input
          v-model="queryParams.majorCategory"
          placeholder="请输入理工类/文史类/艺术类/其他"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="必修/选修" prop="courseNature">
        <el-input
          v-model="queryParams.courseNature"
          placeholder="请输入必修/选修"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="省级一流/校级精品/其他" prop="courseLevel">
        <el-input
          v-model="queryParams.courseLevel"
          placeholder="请输入省级一流/校级精品/其他"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="主持人/团队前3/独立" prop="courseRole">
        <el-input
          v-model="queryParams.courseRole"
          placeholder="请输入主持人/团队前3/独立"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="${comment}" prop="className">
        <el-input
          v-model="queryParams.className"
          placeholder="请输入${comment}"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="合堂人数" prop="studentCount">
        <el-input
          v-model="queryParams.studentCount"
          placeholder="请输入合堂人数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="理论学时J1" prop="theoryHours">
        <el-input
          v-model="queryParams.theoryHours"
          placeholder="请输入理论学时J1"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="实践学时J2" prop="practiceHours">
        <el-input
          v-model="queryParams.practiceHours"
          placeholder="请输入实践学时J2"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="同名课第几次(1/2/3+ -&gt; C1 1.0/0.9/0.8)" prop="repeatOrder">
        <el-input
          v-model="queryParams.repeatOrder"
          placeholder="请输入同名课第几次(1/2/3+ -&gt; C1 1.0/0.9/0.8)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="${comment}" prop="importSource">
        <el-input
          v-model="queryParams.importSource"
          placeholder="请输入${comment}"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="${comment}" prop="importBatch">
        <el-input
          v-model="queryParams.importBatch"
          placeholder="请输入${comment}"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="${comment}" prop="importTime">
        <el-date-picker clearable
          v-model="queryParams.importTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择${comment}">
        </el-date-picker>
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
          v-hasPermi="['system:teachingTask:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:teachingTask:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:teachingTask:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:teachingTask:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="teachingTaskList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="${comment}" align="center" prop="id" />
      <el-table-column label="教师ID" align="center" prop="userId" />
      <el-table-column label="学年学期(如2025-2026-1)" align="center" prop="semester" />
      <el-table-column label="${comment}" align="center" prop="academicYear" />
      <el-table-column label="课程名称" align="center" prop="courseName" />
      <el-table-column label="课程代码" align="center" prop="courseCode" />
      <el-table-column label="本科(含专升本)/专科" align="center" prop="educationLevel" />
      <el-table-column label="理工类/文史类/艺术类/其他" align="center" prop="majorCategory" />
      <el-table-column label="必修/选修" align="center" prop="courseNature" />
      <el-table-column label="省级一流/校级精品/其他" align="center" prop="courseLevel" />
      <el-table-column label="主持人/团队前3/独立" align="center" prop="courseRole" />
      <el-table-column label="${comment}" align="center" prop="className" />
      <el-table-column label="合堂人数" align="center" prop="studentCount" />
      <el-table-column label="理论学时J1" align="center" prop="theoryHours" />
      <el-table-column label="实践学时J2" align="center" prop="practiceHours" />
      <el-table-column label="同名课第几次(1/2/3+ -&gt; C1 1.0/0.9/0.8)" align="center" prop="repeatOrder" />
      <el-table-column label="${comment}" align="center" prop="importSource" />
      <el-table-column label="${comment}" align="center" prop="importBatch" />
      <el-table-column label="${comment}" align="center" prop="importTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.importTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="${comment}" align="center" prop="status" />
      <el-table-column label="${comment}" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
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

    <!-- 添加或修改导入教学任务对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="teachingTaskRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="教师ID" prop="userId">
              <el-input v-model="form.userId" placeholder="请输入教师ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="学年学期(如2025-2026-1)" prop="semester">
              <el-input v-model="form.semester" placeholder="请输入学年学期(如2025-2026-1)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="academicYear">
              <el-input v-model="form.academicYear" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" placeholder="请输入课程名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课程代码" prop="courseCode">
              <el-input v-model="form.courseCode" placeholder="请输入课程代码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="本科(含专升本)/专科" prop="educationLevel">
              <el-input v-model="form.educationLevel" placeholder="请输入本科(含专升本)/专科" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="理工类/文史类/艺术类/其他" prop="majorCategory">
              <el-input v-model="form.majorCategory" placeholder="请输入理工类/文史类/艺术类/其他" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="必修/选修" prop="courseNature">
              <el-input v-model="form.courseNature" placeholder="请输入必修/选修" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="省级一流/校级精品/其他" prop="courseLevel">
              <el-input v-model="form.courseLevel" placeholder="请输入省级一流/校级精品/其他" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="主持人/团队前3/独立" prop="courseRole">
              <el-input v-model="form.courseRole" placeholder="请输入主持人/团队前3/独立" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="className">
              <el-input v-model="form.className" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="合堂人数" prop="studentCount">
              <el-input v-model="form.studentCount" placeholder="请输入合堂人数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="理论学时J1" prop="theoryHours">
              <el-input v-model="form.theoryHours" placeholder="请输入理论学时J1" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="实践学时J2" prop="practiceHours">
              <el-input v-model="form.practiceHours" placeholder="请输入实践学时J2" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="同名课第几次(1/2/3+ -&gt; C1 1.0/0.9/0.8)" prop="repeatOrder">
              <el-input v-model="form.repeatOrder" placeholder="请输入同名课第几次(1/2/3+ -&gt; C1 1.0/0.9/0.8)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="importSource">
              <el-input v-model="form.importSource" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="importBatch">
              <el-input v-model="form.importBatch" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="importTime">
              <el-date-picker clearable
                v-model="form.importTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择${comment}">
              </el-date-picker>
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

<script setup name="TeachingTask">
import { listTeachingTask, getTeachingTask, delTeachingTask, addTeachingTask, updateTeachingTask } from "@/api/system/teachingTask"

const { proxy } = getCurrentInstance()

const teachingTaskList = ref([])
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
    semester: null,
    academicYear: null,
    courseName: null,
    courseCode: null,
    educationLevel: null,
    majorCategory: null,
    courseNature: null,
    courseLevel: null,
    courseRole: null,
    className: null,
    studentCount: null,
    theoryHours: null,
    practiceHours: null,
    repeatOrder: null,
    importSource: null,
    importBatch: null,
    importTime: null,
    status: null,
  },
  rules: {
    userId: [
      { required: true, message: "教师ID不能为空", trigger: "blur" }
    ],
    semester: [
      { required: true, message: "学年学期(如2025-2026-1)不能为空", trigger: "blur" }
    ],
    courseName: [
      { required: true, message: "课程名称不能为空", trigger: "blur" }
    ],
    educationLevel: [
      { required: true, message: "本科(含专升本)/专科不能为空", trigger: "blur" }
    ],
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
    educationLevel: null,
    majorCategory: null,
    courseNature: null,
    courseLevel: null,
    courseRole: null,
    className: null,
    studentCount: null,
    theoryHours: null,
    practiceHours: null,
    repeatOrder: null,
    importSource: null,
    importBatch: null,
    importTime: null,
    status: null,
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
  title.value = "添加导入教学任务"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getTeachingTask(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改导入教学任务"
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
  proxy.$modal.confirm('是否确认删除导入教学任务编号为"' + _ids + '"的数据项？').then(function() {
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

getList()
</script>

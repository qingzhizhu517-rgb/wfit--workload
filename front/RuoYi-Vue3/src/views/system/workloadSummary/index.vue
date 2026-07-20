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
      <el-form-item label="第一课堂=G1+G2+G3+G4+G5+G6" prop="G7">
        <el-input
          v-model="queryParams.G7"
          placeholder="请输入第一课堂=G1+G2+G3+G4+G5+G6"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="第二课堂" prop="G8">
        <el-input
          v-model="queryParams.G8"
          placeholder="请输入第二课堂"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="其他" prop="G9">
        <el-input
          v-model="queryParams.G9"
          placeholder="请输入其他"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="教学合计=G7+G8+G9" prop="G10">
        <el-input
          v-model="queryParams.G10"
          placeholder="请输入教学合计=G7+G8+G9"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="管理服务" prop="G11">
        <el-input
          v-model="queryParams.G11"
          placeholder="请输入管理服务"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="总工作量=G10+G11" prop="totalWorkload">
        <el-input
          v-model="queryParams.totalWorkload"
          placeholder="请输入总工作量=G10+G11"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="额定(统一180)" prop="ratedWorkload">
        <el-input
          v-model="queryParams.ratedWorkload"
          placeholder="请输入额定(统一180)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="超额定=max(0,total-rated)" prop="excessWorkload">
        <el-input
          v-model="queryParams.excessWorkload"
          placeholder="请输入超额定=max(0,total-rated)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="职称快照" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入职称快照"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="单位酬金快照" prop="payRate">
        <el-input
          v-model="queryParams.payRate"
          placeholder="请输入单位酬金快照"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="绩效酬金=(min(total,540)-180)*rate" prop="performancePay">
        <el-input
          v-model="queryParams.performancePay"
          placeholder="请输入绩效酬金=(min(total,540)-180)*rate"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="触200%封顶" prop="isCapped">
        <el-input
          v-model="queryParams.isCapped"
          placeholder="请输入触200%封顶"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="第五条达标标准/学期" prop="basicTeachingStandard">
        <el-input
          v-model="queryParams.basicTeachingStandard"
          placeholder="请输入第五条达标标准/学期"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="达标G10&gt;=standard" prop="basicTeachingMet">
        <el-input
          v-model="queryParams.basicTeachingMet"
          placeholder="请输入达标G10&gt;=standard"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="${comment}" prop="teacherSign">
        <el-input
          v-model="queryParams.teacherSign"
          placeholder="请输入${comment}"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="${comment}" prop="teacherSignTime">
        <el-date-picker clearable
          v-model="queryParams.teacherSignTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择${comment}">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="${comment}" prop="deptLeaderSign">
        <el-input
          v-model="queryParams.deptLeaderSign"
          placeholder="请输入${comment}"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="${comment}" prop="deptLeaderSignTime">
        <el-date-picker clearable
          v-model="queryParams.deptLeaderSignTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择${comment}">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="${comment}" prop="academicAssistantSign">
        <el-input
          v-model="queryParams.academicAssistantSign"
          placeholder="请输入${comment}"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="${comment}" prop="academicAssistantSignTime">
        <el-date-picker clearable
          v-model="queryParams.academicAssistantSignTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择${comment}">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="${comment}" prop="lockTime">
        <el-date-picker clearable
          v-model="queryParams.lockTime"
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
          v-hasPermi="['system:workloadSummary:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:workloadSummary:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:workloadSummary:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:workloadSummary:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="workloadSummaryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="${comment}" align="center" prop="id" />
      <el-table-column label="${comment}" align="center" prop="userId" />
      <el-table-column label="${comment}" align="center" prop="semester" />
      <el-table-column label="${comment}" align="center" prop="academicYear" />
      <el-table-column label="第一课堂=G1+G2+G3+G4+G5+G6" align="center" prop="G7" />
      <el-table-column label="第二课堂" align="center" prop="G8" />
      <el-table-column label="其他" align="center" prop="G9" />
      <el-table-column label="教学合计=G7+G8+G9" align="center" prop="G10" />
      <el-table-column label="管理服务" align="center" prop="G11" />
      <el-table-column label="总工作量=G10+G11" align="center" prop="totalWorkload" />
      <el-table-column label="额定(统一180)" align="center" prop="ratedWorkload" />
      <el-table-column label="超额定=max(0,total-rated)" align="center" prop="excessWorkload" />
      <el-table-column label="职称快照" align="center" prop="title" />
      <el-table-column label="单位酬金快照" align="center" prop="payRate" />
      <el-table-column label="绩效酬金=(min(total,540)-180)*rate" align="center" prop="performancePay" />
      <el-table-column label="触200%封顶" align="center" prop="isCapped" />
      <el-table-column label="第五条达标标准/学期" align="center" prop="basicTeachingStandard" />
      <el-table-column label="达标G10&gt;=standard" align="center" prop="basicTeachingMet" />
      <el-table-column label="0草稿/1已公示/2已审核/3已锁定" align="center" prop="status" />
      <el-table-column label="${comment}" align="center" prop="teacherSign" />
      <el-table-column label="${comment}" align="center" prop="teacherSignTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.teacherSignTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="${comment}" align="center" prop="deptLeaderSign" />
      <el-table-column label="${comment}" align="center" prop="deptLeaderSignTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.deptLeaderSignTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="${comment}" align="center" prop="academicAssistantSign" />
      <el-table-column label="${comment}" align="center" prop="academicAssistantSignTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.academicAssistantSignTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="${comment}" align="center" prop="lockTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.lockTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="${comment}" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:workloadSummary:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:workloadSummary:remove']">删除</el-button>
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

    <!-- 添加或修改学期工作量汇总对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="workloadSummaryRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="userId">
              <el-input v-model="form.userId" placeholder="请输入${comment}" />
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
            <el-form-item label="第一课堂=G1+G2+G3+G4+G5+G6" prop="G7">
              <el-input v-model="form.G7" placeholder="请输入第一课堂=G1+G2+G3+G4+G5+G6" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="第二课堂" prop="G8">
              <el-input v-model="form.G8" placeholder="请输入第二课堂" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="其他" prop="G9">
              <el-input v-model="form.G9" placeholder="请输入其他" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="教学合计=G7+G8+G9" prop="G10">
              <el-input v-model="form.G10" placeholder="请输入教学合计=G7+G8+G9" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="管理服务" prop="G11">
              <el-input v-model="form.G11" placeholder="请输入管理服务" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="总工作量=G10+G11" prop="totalWorkload">
              <el-input v-model="form.totalWorkload" placeholder="请输入总工作量=G10+G11" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="额定(统一180)" prop="ratedWorkload">
              <el-input v-model="form.ratedWorkload" placeholder="请输入额定(统一180)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="超额定=max(0,total-rated)" prop="excessWorkload">
              <el-input v-model="form.excessWorkload" placeholder="请输入超额定=max(0,total-rated)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="职称快照" prop="title">
              <el-input v-model="form.title" placeholder="请输入职称快照" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="单位酬金快照" prop="payRate">
              <el-input v-model="form.payRate" placeholder="请输入单位酬金快照" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="绩效酬金=(min(total,540)-180)*rate" prop="performancePay">
              <el-input v-model="form.performancePay" placeholder="请输入绩效酬金=(min(total,540)-180)*rate" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="触200%封顶" prop="isCapped">
              <el-input v-model="form.isCapped" placeholder="请输入触200%封顶" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="第五条达标标准/学期" prop="basicTeachingStandard">
              <el-input v-model="form.basicTeachingStandard" placeholder="请输入第五条达标标准/学期" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="达标G10&gt;=standard" prop="basicTeachingMet">
              <el-input v-model="form.basicTeachingMet" placeholder="请输入达标G10&gt;=standard" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="teacherSign">
              <el-input v-model="form.teacherSign" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="teacherSignTime">
              <el-date-picker clearable
                v-model="form.teacherSignTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择${comment}">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="deptLeaderSign">
              <el-input v-model="form.deptLeaderSign" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="deptLeaderSignTime">
              <el-date-picker clearable
                v-model="form.deptLeaderSignTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择${comment}">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="academicAssistantSign">
              <el-input v-model="form.academicAssistantSign" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="academicAssistantSignTime">
              <el-date-picker clearable
                v-model="form.academicAssistantSignTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择${comment}">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="lockTime">
              <el-date-picker clearable
                v-model="form.lockTime"
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

<script setup name="WorkloadSummary">
import { listWorkloadSummary, getWorkloadSummary, delWorkloadSummary, addWorkloadSummary, updateWorkloadSummary } from "@/api/system/workloadSummary"

const { proxy } = getCurrentInstance()

const workloadSummaryList = ref([])
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
    G7: null,
    G8: null,
    G9: null,
    G10: null,
    G11: null,
    totalWorkload: null,
    ratedWorkload: null,
    excessWorkload: null,
    title: null,
    payRate: null,
    performancePay: null,
    isCapped: null,
    basicTeachingStandard: null,
    basicTeachingMet: null,
    status: null,
    teacherSign: null,
    teacherSignTime: null,
    deptLeaderSign: null,
    deptLeaderSignTime: null,
    academicAssistantSign: null,
    academicAssistantSignTime: null,
    lockTime: null,
  },
  rules: {
    userId: [
      { required: true, message: "$comment不能为空", trigger: "blur" }
    ],
    semester: [
      { required: true, message: "$comment不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询学期工作量汇总列表 */
function getList() {
  loading.value = true
  listWorkloadSummary(queryParams.value).then(response => {
    workloadSummaryList.value = response.rows
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
    G7: null,
    G8: null,
    G9: null,
    G10: null,
    G11: null,
    totalWorkload: null,
    ratedWorkload: null,
    excessWorkload: null,
    title: null,
    payRate: null,
    performancePay: null,
    isCapped: null,
    basicTeachingStandard: null,
    basicTeachingMet: null,
    status: null,
    teacherSign: null,
    teacherSignTime: null,
    deptLeaderSign: null,
    deptLeaderSignTime: null,
    academicAssistantSign: null,
    academicAssistantSignTime: null,
    lockTime: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("workloadSummaryRef")
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
  title.value = "添加学期工作量汇总"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getWorkloadSummary(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改学期工作量汇总"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["workloadSummaryRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateWorkloadSummary(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addWorkloadSummary(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除学期工作量汇总编号为"' + _ids + '"的数据项？').then(function() {
    return delWorkloadSummary(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/workloadSummary/export', {
    ...queryParams.value
  }, `workloadSummary_${new Date().getTime()}.xlsx`)
}

getList()
</script>

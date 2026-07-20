<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="FK biz_pay_record" prop="payRecordId">
        <el-input
          v-model="queryParams.payRecordId"
          placeholder="请输入FK biz_pay_record"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
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
      <el-form-item label="${comment}" prop="studentCount">
        <el-input
          v-model="queryParams.studentCount"
          placeholder="请输入${comment}"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="E讲座时长" prop="durationHours">
        <el-input
          v-model="queryParams.durationHours"
          placeholder="请输入E讲座时长"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="F运动会天数" prop="days">
        <el-input
          v-model="queryParams.days"
          placeholder="请输入F运动会天数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="F体测班数" prop="classCount">
        <el-input
          v-model="queryParams.classCount"
          placeholder="请输入F体测班数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="G夜间值班工作量" prop="workloadUnits">
        <el-input
          v-model="queryParams.workloadUnits"
          placeholder="请输入G夜间值班工作量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="E讲座名称" prop="lectureName">
        <el-input
          v-model="queryParams.lectureName"
          placeholder="请输入E讲座名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="计算金额" prop="amount">
        <el-input
          v-model="queryParams.amount"
          placeholder="请输入计算金额"
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
          v-hasPermi="['system:allowanceItem:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:allowanceItem:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:allowanceItem:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:allowanceItem:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="allowanceItemList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="${comment}" align="center" prop="id" />
      <el-table-column label="FK biz_pay_record" align="center" prop="payRecordId" />
      <el-table-column label="${comment}" align="center" prop="userId" />
      <el-table-column label="${comment}" align="center" prop="semester" />
      <el-table-column label="A/B/C/D/E/F/G" align="center" prop="feeType" />
      <el-table-column label="A重修:跟班/单独开班/自学辅导;B实习:分散/集中不跟班" align="center" prop="feeSubtype" />
      <el-table-column label="${comment}" align="center" prop="studentCount" />
      <el-table-column label="E讲座时长" align="center" prop="durationHours" />
      <el-table-column label="F运动会天数" align="center" prop="days" />
      <el-table-column label="F体测班数" align="center" prop="classCount" />
      <el-table-column label="G夜间值班工作量" align="center" prop="workloadUnits" />
      <el-table-column label="E讲座名称" align="center" prop="lectureName" />
      <el-table-column label="扩展字段" align="center" prop="ext" />
      <el-table-column label="计算金额" align="center" prop="amount" />
      <el-table-column label="1正常0停用(D代阅卷默认0)" align="center" prop="status" />
      <el-table-column label="${comment}" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:allowanceItem:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:allowanceItem:remove']">删除</el-button>
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

    <!-- 添加或修改其他酬金明细对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="allowanceItemRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="FK biz_pay_record" prop="payRecordId">
              <el-input v-model="form.payRecordId" placeholder="请输入FK biz_pay_record" />
            </el-form-item>
          </el-col>
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
            <el-form-item label="${comment}" prop="studentCount">
              <el-input v-model="form.studentCount" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="E讲座时长" prop="durationHours">
              <el-input v-model="form.durationHours" placeholder="请输入E讲座时长" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="F运动会天数" prop="days">
              <el-input v-model="form.days" placeholder="请输入F运动会天数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="F体测班数" prop="classCount">
              <el-input v-model="form.classCount" placeholder="请输入F体测班数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="G夜间值班工作量" prop="workloadUnits">
              <el-input v-model="form.workloadUnits" placeholder="请输入G夜间值班工作量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="E讲座名称" prop="lectureName">
              <el-input v-model="form.lectureName" placeholder="请输入E讲座名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="计算金额" prop="amount">
              <el-input v-model="form.amount" placeholder="请输入计算金额" />
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

<script setup name="AllowanceItem">
import { listAllowanceItem, getAllowanceItem, delAllowanceItem, addAllowanceItem, updateAllowanceItem } from "@/api/system/allowanceItem"

const { proxy } = getCurrentInstance()

const allowanceItemList = ref([])
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
    payRecordId: null,
    userId: null,
    semester: null,
    feeType: null,
    feeSubtype: null,
    studentCount: null,
    durationHours: null,
    days: null,
    classCount: null,
    workloadUnits: null,
    lectureName: null,
    ext: null,
    amount: null,
    status: null,
  },
  rules: {
    userId: [
      { required: true, message: "$comment不能为空", trigger: "blur" }
    ],
    semester: [
      { required: true, message: "$comment不能为空", trigger: "blur" }
    ],
    feeType: [
      { required: true, message: "A/B/C/D/E/F/G不能为空", trigger: "change" }
    ],
    amount: [
      { required: true, message: "计算金额不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询其他酬金明细列表 */
function getList() {
  loading.value = true
  listAllowanceItem(queryParams.value).then(response => {
    allowanceItemList.value = response.rows
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
    payRecordId: null,
    userId: null,
    semester: null,
    feeType: null,
    feeSubtype: null,
    studentCount: null,
    durationHours: null,
    days: null,
    classCount: null,
    workloadUnits: null,
    lectureName: null,
    ext: null,
    amount: null,
    status: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("allowanceItemRef")
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
  title.value = "添加其他酬金明细"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getAllowanceItem(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改其他酬金明细"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["allowanceItemRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateAllowanceItem(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAllowanceItem(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除其他酬金明细编号为"' + _ids + '"的数据项？').then(function() {
    return delAllowanceItem(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/allowanceItem/export', {
    ...queryParams.value
  }, `allowanceItem_${new Date().getTime()}.xlsx`)
}

getList()
</script>

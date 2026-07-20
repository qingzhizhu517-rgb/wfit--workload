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
      <el-form-item label="FK biz_workload_summary" prop="summaryId">
        <el-input
          v-model="queryParams.summaryId"
          placeholder="请输入FK biz_workload_summary"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="课时/绩效酬金" prop="courseHourPay">
        <el-input
          v-model="queryParams.courseHourPay"
          placeholder="请输入课时/绩效酬金"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="其他酬金合计A+B+C+D+E+F+G" prop="otherPayTotal">
        <el-input
          v-model="queryParams.otherPayTotal"
          placeholder="请输入其他酬金合计A+B+C+D+E+F+G"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="总金额(四舍五入取整)" prop="totalPay">
        <el-input
          v-model="queryParams.totalPay"
          placeholder="请输入总金额(四舍五入取整)"
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
          v-hasPermi="['system:payRecord:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:payRecord:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:payRecord:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:payRecord:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="payRecordList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="${comment}" align="center" prop="id" />
      <el-table-column label="${comment}" align="center" prop="userId" />
      <el-table-column label="${comment}" align="center" prop="semester" />
      <el-table-column label="FK biz_workload_summary" align="center" prop="summaryId" />
      <el-table-column label="课时/绩效酬金" align="center" prop="courseHourPay" />
      <el-table-column label="其他酬金合计A+B+C+D+E+F+G" align="center" prop="otherPayTotal" />
      <el-table-column label="总金额(四舍五入取整)" align="center" prop="totalPay" />
      <el-table-column label="${comment}" align="center" prop="status" />
      <el-table-column label="${comment}" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:payRecord:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:payRecord:remove']">删除</el-button>
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

    <!-- 添加或修改酬金汇总对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="payRecordRef" :model="form" :rules="rules" label-width="100px">
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
            <el-form-item label="FK biz_workload_summary" prop="summaryId">
              <el-input v-model="form.summaryId" placeholder="请输入FK biz_workload_summary" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课时/绩效酬金" prop="courseHourPay">
              <el-input v-model="form.courseHourPay" placeholder="请输入课时/绩效酬金" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="其他酬金合计A+B+C+D+E+F+G" prop="otherPayTotal">
              <el-input v-model="form.otherPayTotal" placeholder="请输入其他酬金合计A+B+C+D+E+F+G" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="总金额(四舍五入取整)" prop="totalPay">
              <el-input v-model="form.totalPay" placeholder="请输入总金额(四舍五入取整)" />
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

<script setup name="PayRecord">
import { listPayRecord, getPayRecord, delPayRecord, addPayRecord, updatePayRecord } from "@/api/system/payRecord"

const { proxy } = getCurrentInstance()

const payRecordList = ref([])
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
    summaryId: null,
    courseHourPay: null,
    otherPayTotal: null,
    totalPay: null,
    status: null,
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

/** 查询酬金汇总列表 */
function getList() {
  loading.value = true
  listPayRecord(queryParams.value).then(response => {
    payRecordList.value = response.rows
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
    summaryId: null,
    courseHourPay: null,
    otherPayTotal: null,
    totalPay: null,
    status: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("payRecordRef")
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
  title.value = "添加酬金汇总"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getPayRecord(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改酬金汇总"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["payRecordRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updatePayRecord(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addPayRecord(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除酬金汇总编号为"' + _ids + '"的数据项？').then(function() {
    return delPayRecord(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/payRecord/export', {
    ...queryParams.value
  }, `payRecord_${new Date().getTime()}.xlsx`)
}

getList()
</script>

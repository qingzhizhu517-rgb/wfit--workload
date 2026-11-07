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
        v-if="!isTeacher"
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
        label="状态"
        prop="status"
      >
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          style="width: 120px"
        >
          <el-option
            v-for="(v, k) in summaryStatusMap"
            :key="k"
            :label="v.label"
            :value="Number(k)"
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
      <template v-if="!isTeacher && !isLeader">
        <el-col :span="1.5">
          <el-tooltip
            content="校验→同步G11(可选)→明细→汇总→酬金，需先在搜索栏选择教师和学期"
            placement="top"
          >
            <el-button
              v-hasPermi="['system:workloadSummary:edit']"
              type="primary"
              plain
              icon="Cpu"
              :loading="calcLoading"
              @click="handleStartRun"
            >
              开始核算
            </el-button>
          </el-tooltip>
        </el-col>
        <el-col :span="1.5">
          <el-tooltip
            content="对表格中勾选的教师逐个核算，单人失败不影响其他人"
            placement="top"
          >
            <el-button
              v-hasPermi="['system:workloadSummary:edit']"
              type="primary"
              plain
              icon="Files"
              :loading="batchCalcLoading"
              :disabled="multiple"
              @click="handleRunSelected"
            >
              核算所选{{ selectedUserIds.length ? `(${selectedUserIds.length})` : '' }}
            </el-button>
          </el-tooltip>
        </el-col>
        <el-col :span="1.5">
          <el-tooltip
            content="按搜索栏学期，核算该学期全部有明细的教师，耗时较长"
            placement="top"
          >
            <el-button
              v-hasPermi="['system:workloadSummary:edit']"
              type="danger"
              plain
              icon="Odometer"
              :loading="batchCalcLoading"
              @click="handleRunSemester"
            >
              全学期核算
            </el-button>
          </el-tooltip>
        </el-col>
        <el-col :span="1.5">
          <el-tooltip
            content="按搜索栏学期，同步教务确认的本学期岗位减免值到 G11"
            placement="top"
          >
            <el-button
              v-hasPermi="['system:workloadItem:add']"
              type="warning"
              plain
              icon="MagicStick"
              @click="handleGenG11"
            >
              同步岗位减免
            </el-button>
          </el-tooltip>
        </el-col>
        <el-col :span="1.5">
          <el-button
            v-hasPermi="['system:audit:submit']"
            type="success"
            plain
            icon="Promotion"
            :disabled="multiple"
            @click="handleBatchSubmit"
          >
            批量提交
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            v-hasPermi="['system:workloadSummary:remove']"
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
          >
            删除
          </el-button>
        </el-col>
      </template>
      <el-col :span="1.5">
        <el-tooltip
          content="不落库仿真预览汇总结果"
          placement="top"
        >
          <el-button
            v-hasPermi="['system:workloadSummary:query']"
            type="info"
            plain
            icon="View"
            @click="handlePreview"
          >
            汇总预览
          </el-button>
        </el-tooltip>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:workloadSummary:export']"
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
        >
          导出
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-tooltip
          content="表一（标准格式）：对齐教务处模板，一行一开课任务，39 列，教师级列只填首行"
          placement="top"
        >
          <el-button
            v-hasPermi="['system:export:personal']"
            type="success"
            plain
            icon="Tickets"
            @click="handleExportStdForm1()"
          >
            导出表一
          </el-button>
        </el-tooltip>
      </el-col>
      <el-col
        v-if="!isTeacher"
        :span="1.5"
      >
        <el-tooltip
          content="批量导出表一：需先在搜索栏选定「学年学期」（批量导出为单学期操作），按跨页勾选的教师批量导出，1-20人可选单工作簿(xlsx)，21-100人只能压缩包(zip)；无明细/未汇总教师写入失败清单"
          placement="top"
        >
          <el-button
            v-hasPermi="['system:export:personal']"
            type="success"
            plain
            icon="Files"
            :disabled="!queryParams.semester || !crossPageSelectedIds.length"
            @click="handleBatchExport"
          >
            批量导出表一{{ crossPageSelectedIds.length ? `(${crossPageSelectedIds.length})` : '' }}
          </el-button>
        </el-tooltip>
      </el-col>
      <el-col :span="1.5">
        <el-tooltip
          content="附件1明细（追溯用）：本人/所选教师该学期工作量明细，含重复次序、班级与各项系数"
          placement="top"
        >
          <el-button
            v-hasPermi="['system:export:personal']"
            type="success"
            plain
            icon="Document"
            @click="handleExportAttach1()"
          >
            导出附件1
          </el-button>
        </el-tooltip>
      </el-col>
      <el-col
        v-if="!isTeacher"
        :span="1.5"
      >
        <el-tooltip
          content="附件2：该学期绩效酬金统计表（金额取自酬金记录）"
          placement="top"
        >
          <el-button
            v-hasPermi="['system:export:paySummary']"
            type="success"
            plain
            icon="Money"
            @click="handleExportAttach2"
          >
            导出附件2
          </el-button>
        </el-tooltip>
      </el-col>
      <el-col
        v-if="!isTeacher && !isLeader"
        :span="1.5"
      >
        <el-button
          v-hasPermi="['system:auditRecord:list']"
          type="primary"
          plain
          @click="router.push('/workload/auditCenter')"
        >
          审核中心
        </el-button>
      </el-col>
      <right-toolbar
        v-model:show-search="showSearch"
        @query-table="getList"
      />
    </el-row>

    <!-- 教师总览卡绑定当前筛选学期，不依赖列表返回顺序 -->
    <teacher-overview
      v-if="isTeacher && overviewRow"
      :row="overviewRow"
    />

    <el-table
      ref="summaryTableRef"
      v-loading="loading"
      :data="workloadSummaryList"
      row-key="id"
      stripe
      empty-text="暂无数据"
      @selection-change="handleSelectionChange"
    >
      <el-table-column
        v-if="!isTeacher"
        type="selection"
        width="50"
        align="center"
        :reserve-selection="true"
      />
      <el-table-column
        v-if="!isTeacher"
        label="教师"
        align="center"
        prop="userId"
        width="100"
        fixed="left"
      >
        <template #default="scope">
          {{ userName(scope.row.userId) }}
        </template>
      </el-table-column>
      <el-table-column
        v-if="!isTeacher"
        label="工号"
        align="center"
        width="120"
        fixed="left"
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
        fixed="left"
      />
      <el-table-column
        label="G7 第一课堂(学时)"
        align="right"
        prop="G7"
        width="130"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.G7) }}
        </template>
      </el-table-column>
      <el-table-column
        label="G8 第二课堂(学时)"
        align="right"
        prop="G8"
        width="130"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.G8) }}
        </template>
      </el-table-column>
      <el-table-column
        label="G9 其他(学时)"
        align="right"
        prop="G9"
        width="110"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.G9) }}
        </template>
      </el-table-column>
      <el-table-column
        label="G10 教学合计(学时)"
        align="right"
        prop="G10"
        width="140"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.G10) }}
        </template>
      </el-table-column>
      <el-table-column
        label="G11 管理服务(学时)"
        align="right"
        prop="G11"
        width="130"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.G11) }}
        </template>
      </el-table-column>
      <el-table-column
        label="总工作量(学时)"
        align="right"
        prop="totalWorkload"
        width="110"
      >
        <template #default="scope">
          <span class="total-num">{{ formatNumber(scope.row.totalWorkload) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="额定(学时)"
        align="right"
        prop="ratedWorkload"
        width="90"
      >
        <template #default="scope">
          {{ formatNumber(scope.row.ratedWorkload) }}
        </template>
      </el-table-column>
      <el-table-column
        label="超额(学时)"
        align="right"
        prop="excessWorkload"
        width="90"
      >
        <template #default="scope">
          <span :class="{ 'excess-num': Number(scope.row.excessWorkload) > 0 }">{{ formatNumber(scope.row.excessWorkload) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="绩效酬金(元)"
        align="right"
        prop="performancePay"
        width="130"
      >
        <template #default="scope">
          <!-- 数值右对齐，封顶标记小字号单独一行不破坏对齐 -->
          <div class="pay-cell">
            <span>{{ formatAmount(scope.row.performancePay) }}</span>
            <el-tag
              v-if="scope.row.isCapped === 1"
              type="danger"
              size="small"
              disable-transitions
            >
              封顶
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        label="基本教学达标"
        align="center"
        prop="basicTeachingMet"
        width="100"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.basicTeachingMet"
            :map="metStatusMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="备注 / 审核说明"
        align="left"
        prop="remark"
        min-width="180"
        show-overflow-tooltip
      >
        <template #default="scope">
          <span class="remark-text">{{ displayRemark(scope.row.remark) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="状态"
        align="center"
        prop="status"
        width="85"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.status"
            :map="summaryStatusMap"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="核算 / 结算依据"
        align="center"
        width="145"
      >
        <template #default="scope">
          <biz-tag
            :value="scope.row.needsRecalculation"
            :map="recalculationMap"
          />
          <el-tooltip
            :content="settlementHint(scope.row)"
            placement="top"
          >
            <el-tag
              :type="settlementTagType(scope.row)"
              size="small"
              class="mt5"
            >
              {{ settlementLabel(scope.row) }}
            </el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        :width="isTeacher ? 100 : 220"
        fixed="right"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-button
            v-hasPermi="['system:workloadSummary:query']"
            link
            type="primary"
            icon="View"
            @click="handleDetail(scope.row)"
          >
            详情
          </el-button>
          <!-- 教师只能提交，或在审批中确认本人工作量 -->
          <el-button
            v-if="isTeacher && scope.row.status === 0"
            v-hasPermi="['system:audit:submit']"
            link
            type="success"
            icon="Promotion"
            :disabled="scope.row.needsRecalculation === 1 || settlementSubmitBlocked(scope.row)"
            @click="handleSubmit(scope.row)"
          >
            提交
          </el-button>
          <el-button
            v-if="isTeacher && scope.row.status === 1 && scope.row.userId === userStore.id"
            v-hasPermi="['system:audit:teacherConfirm']"
            link
            type="primary"
            icon="Select"
            @click="handleTeacherConfirm(scope.row)"
          >
            确认工作量
          </el-button>
          <!-- 教务处审批操作 -->
          <template v-if="!isTeacher">
            <el-button
              v-if="scope.row.status === 0"
              v-hasPermi="['system:audit:submit']"
              link
              type="success"
              icon="Promotion"
              :disabled="scope.row.needsRecalculation === 1 || settlementSubmitBlocked(scope.row)"
              @click="handleSubmit(scope.row)"
            >
              提交
            </el-button>
            <el-button
              v-if="scope.row.status === 1"
              v-hasPermi="['system:audit:approve']"
              link
              type="success"
              icon="Select"
              @click="handleApprove(scope.row)"
            >
              通过
            </el-button>
            <el-button
              v-if="scope.row.status === 1"
              v-hasPermi="['system:audit:reject']"
              link
              type="warning"
              icon="Close"
              @click="handleReject(scope.row)"
            >
              驳回
            </el-button>
            <!-- 更多操作下拉 -->
            <el-dropdown
              v-if="checkPermi(['system:workloadSummary:edit', 'system:export:personal', 'system:workloadSummary:remove'])"
              trigger="click"
              @command="(cmd) => handleMoreCmd(cmd, scope.row)"
            >
              <el-button
                link
                type="primary"
                icon="MoreFilled"
              />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-if="checkPermi(['system:workloadSummary:edit'])"
                    command="recalc"
                    icon="Refresh"
                  >
                    重算
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="checkPermi(['system:export:personal'])"
                    command="exportItem"
                    icon="Document"
                  >
                    导出附件1
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="checkPermi(['system:workloadSummary:remove'])"
                    command="delete"
                    icon="Delete"
                    :disabled="!!scope.row.settlementSnapshotId"
                    divided
                  >
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
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

    <el-dialog
      v-model="rejectOpen"
      title="驳回汇总"
      width="540px"
      :close-on-click-modal="false"
      :before-close="closeReject"
    >
      <p>原因保留到本次页面退出；重新打开同一汇总可继续填写。</p>
      <el-form label-position="top">
        <el-form-item label="驳回原因（必填，原文最多500字）">
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            :disabled="rejectBusy"
          />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="rejectError"
        :title="rejectError"
        type="error"
        :closable="false"
      />
      <template #footer>
        <el-button
          :disabled="rejectBusy"
          @click="rejectOpen = false"
        >
          保留原因并关闭
        </el-button><el-button
          type="primary"
          :loading="rejectBusy"
          :disabled="rejectCurrentStatus !== 1"
          @click="submitReject"
        >
          确认驳回
        </el-button>
      </template>
    </el-dialog>
    <summary-audit-drawer
      v-model="detailOpen"
      :summary-id="detailRow?.id"
    />

    <!-- 汇总预览对话框 -->
    <el-dialog
      v-model="previewOpen"
      title="汇总预览（不落库）"
      width="560px"
      append-to-body
    >
      <el-form
        :inline="true"
        class="preview-form"
      >
        <el-form-item
          v-if="!isTeacher"
          label="教师"
        >
          <user-select
            v-model="previewQuery.userId"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="学年学期">
          <semester-select
            v-model="previewQuery.semester"
            width="170px"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            icon="View"
            :loading="previewLoading"
            @click="doPreview"
          >
            预览
          </el-button>
        </el-form-item>
      </el-form>
      <template v-if="previewData">
        <el-alert
          v-if="previewData.summary?.settlementEvidenceStatus === 'PREVIEW_INELIGIBLE'"
          title="本次预览未满足绩效结算条件"
          :description="previewData.summary.eligibilityReason"
          type="warning"
          show-icon
          :closable="false"
          class="mb12"
        />
        <el-alert
          v-if="previewData.unconfirmedCount > 0"
          type="warning"
          :title="`注意：该教师本学期还有 ${previewData.unconfirmedCount} 条明细未核对确认，预览结果不含冻结口径`"
          :closable="false"
          class="mb12"
        />
        <el-descriptions
          v-if="previewData.summary"
          :column="2"
          border
        >
          <el-descriptions-item label="G7 第一课堂">
            {{ previewData.summary.G7 }}
          </el-descriptions-item>
          <el-descriptions-item label="G8 第二课堂">
            {{ previewData.summary.G8 }}
          </el-descriptions-item>
          <el-descriptions-item label="G9 其他">
            {{ previewData.summary.G9 }}
          </el-descriptions-item>
          <el-descriptions-item label="G10 教学合计">
            {{ previewData.summary.G10 }}
          </el-descriptions-item>
          <el-descriptions-item label="G11 管理服务">
            {{ previewData.summary.G11 }}
          </el-descriptions-item>
          <el-descriptions-item label="总工作量">
            <b>{{ previewData.summary.totalWorkload }}</b>
          </el-descriptions-item>
          <el-descriptions-item label="额定">
            {{ previewData.summary.ratedWorkload }}
          </el-descriptions-item>
          <el-descriptions-item label="超额">
            {{ previewData.summary.excessWorkload }}
          </el-descriptions-item>
          <el-descriptions-item label="绩效酬金">
            {{ formatAmount(previewData.summary.performancePay) }} 元
          </el-descriptions-item>
          <el-descriptions-item label="是否达标">
            <biz-tag
              :value="previewData.summary.basicTeachingMet"
              :map="metStatusMap"
            />
          </el-descriptions-item>
        </el-descriptions>
        <el-empty
          v-else
          description="暂无汇总数据，请先录入工作量明细"
          :image-size="80"
        />
      </template>
    </el-dialog>

    <!-- 一键核算：确认 + 分阶段结果 -->
    <el-dialog
      v-model="runOpen"
      :title="runResult ? '核算结果' : '开始核算'"
      width="600px"
      append-to-body
    >
      <template v-if="!runResult">
        <el-alert
          :title="runConfirmText"
          type="info"
          :closable="false"
          class="mb12"
        />
        <el-checkbox v-model="runIncludeG11">
          同步教务岗位减免到 G11
        </el-checkbox>
        <div class="run-hint">
          勾选后先按教务确认的本学期岗位减免同步 G11，再依次重算明细、汇总与酬金；不勾选则跳过 G11 同步。
        </div>
      </template>
      <template v-else>
        <el-steps
          direction="vertical"
          :active="runResult.stages.length"
          class="run-steps"
        >
          <el-step
            v-for="stage in runResult.stages"
            :key="stage.code"
            :title="stageLabel(stage.code)"
            :description="stageDesc(stage)"
            :status="stage.ok ? 'success' : 'error'"
          />
        </el-steps>
        <el-descriptions
          :column="2"
          border
          class="mb12"
        >
          <el-descriptions-item label="同步 G11">
            {{ runResult.generatedG11Count }} 条
          </el-descriptions-item>
          <el-descriptions-item label="重算明细">
            {{ runResult.recalcItemCount }} 条
          </el-descriptions-item>
          <el-descriptions-item label="未核对明细">
            {{ runResult.unconfirmedCount }} 条
          </el-descriptions-item>
          <el-descriptions-item label="总工作量">
            {{ runResult.summary ? runResult.summary.totalWorkload : '-' }}
          </el-descriptions-item>
        </el-descriptions>
        <el-alert
          v-if="runResult.unconfirmedCount > 0"
          type="warning"
          :closable="false"
          title="仍有明细未核对确认，暂不可提交审核"
        />
      </template>
      <template #footer>
        <template v-if="!runResult">
          <el-button @click="runOpen = false">
            取消
          </el-button>
          <el-button
            type="primary"
            :loading="calcLoading"
            @click="doRun"
          >
            开始核算
          </el-button>
        </template>
        <template v-else>
          <el-button @click="runOpen = false">
            关闭
          </el-button>
          <el-button
            v-if="canSubmitAfterRun"
            v-hasPermi="['system:audit:submit']"
            type="success"
            icon="Promotion"
            @click="handleSubmitAfterRun"
          >
            提交审核
          </el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 批量导出表一：格式确认 -->
    <el-dialog
      v-model="batchExportOpen"
      title="批量导出表一"
      width="480px"
      append-to-body
    >
      <el-alert
        :title="batchExportTip"
        type="info"
        :closable="false"
        class="mb12"
      />
      <el-descriptions
        :column="1"
        border
        class="mb12"
      >
        <el-descriptions-item label="学年学期">
          {{ batchExportSemester }}
        </el-descriptions-item>
        <el-descriptions-item label="已选教师">
          {{ batchExportCount }} 位
        </el-descriptions-item>
      </el-descriptions>
      <el-form label-width="88px">
        <el-form-item label="输出格式">
          <el-radio-group v-model="batchExportFormat">
            <el-radio
              value="xlsx"
              :disabled="!batchExportAllowXlsx"
            >
              单个工作簿(.xlsx)
            </el-radio>
            <el-radio value="zip">
              压缩包(.zip)
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div class="run-hint">
        无明细 / 未汇总的教师会写入「失败清单」，不影响其他教师导出。
      </div>
      <template #footer>
        <el-button @click="batchExportOpen = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="batchExportLoading"
          @click="doBatchExport"
        >
          导出
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WorkloadSummary">
import { checkPermi } from '@/utils/permission'
import SummaryAuditDrawer from './SummaryAuditDrawer.vue'
import { getSummaryAuditDetail } from '@/api/system/workloadSummary'
import { approvalBlock, createReasonDrafts, createSubmitGuard, sendReasonDraft } from '@/utils/reviewUi'
import TeacherOverview from '@/components/TeacherOverview'
import { settlementLabel, settlementTagType, settlementHint, settlementSubmitBlocked } from '@/utils/settlementEvidence'
import { listWorkloadSummary, delWorkloadSummary } from '@/api/system/workloadSummary'
import { recalcSummary, previewSummary, genG11, runCalc, runCalcBatch } from '@/api/system/calc'
import { exportPersonalWorkload, exportPaySummary, exportAttachment1, exportAttachment1Batch } from '@/api/system/export'
import { ElMessageBox } from 'element-plus'
import { saveBlobAsFile } from '@/utils/blobDownload'
import { mergePageSelection, selectedIds, distinctUserIds } from '@/utils/crossPageSelection'
import { auditSubmit, auditApprove, auditReject, auditBatchSubmit, auditTeacherConfirm } from '@/api/system/audit'
import UserSelect from '@/components/UserSelect/index.vue'
import SemesterSelect from '@/components/SemesterSelect/index.vue'
import { useUserMap } from '@/utils/userCache'
import { useRoute, useRouter } from 'vue-router'
import { summaryStatusMap, formatAmount, formatNumber, getCurrentSemester } from '@/utils/bizDict'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const { userLabel, userName, userCode } = useUserMap()
const userStore = useUserStore()

const isTeacher = computed(() => userStore.roles.includes('teacher'))
const isLeader = computed(() => userStore.roles.includes('leader'))

const recalculationMap = { 1: { label: '需重新核算', type: 'warning' }, 0: { label: '已核算', type: 'success' } }

const metStatusMap = { 1: { label: '已达标', type: 'success' }, 0: { label: '未达标', type: 'danger' } }

/** 备注可能同时承载审核说明与制度提示，按原文展示，不推断结构化告警数量。 */
function displayRemark(remark) {
  const value = String(remark || '').trim()
  return value && value !== '-' ? value : '-'
}

const workloadSummaryList = ref([])
let listRequestId = 0
const loading = ref(true)
const calcLoading = ref(false)
const showSearch = ref(true)
const batchCalcLoading = ref(false)
const total = ref(0)

// 跨页选择：以汇总主键 id 为键的全局已选集合（翻页保留、当前页取消即时生效；筛选变化必清空）。
// 该 Map 是所有派生选择状态的唯一真源，避免在 reserve-selection 下依赖 Element Plus 事件的时序怪癖。
// 用 id（每行唯一）而非 userId 为键：清空学期筛选后列表跨多学期，同一教师多行 userId 相同，
// 以 userId 为键会折叠导致计数错误；导出/核算按教师维度调用时再用 distinctUserIds 折算并去重。
const summaryTableRef = ref(null)
const selectedRowMap = ref(new Map())
// 已选行数（= 汇总主键 id 数），与表格勾选一一对应，用于计数展示与删除/批量提交
const crossPageSelectedIds = computed(() => selectedIds(selectedRowMap.value))
const ids = crossPageSelectedIds
// 折算到教师维度并去重，用于「核算所选」（按教师逐个调用，跨页语义与勾选一致）
const selectedUserIds = computed(() => distinctUserIds(selectedRowMap.value))
const multiple = computed(() => selectedRowMap.value.size === 0)

// 批量导出表一：格式确认对话框
const batchExportOpen = ref(false)
const batchExportLoading = ref(false)
const batchExportSemester = ref('')
const batchExportCount = ref(0)
const batchExportFormat = ref('xlsx')
const batchExportAllowXlsx = computed(() => batchExportCount.value <= 20)
const batchExportTip = computed(() => {
  if (batchExportCount.value > 20) {
    return `已选 ${batchExportCount.value} 位教师，超过 20 位只能导出压缩包(zip)`
  }
  return `已选 ${batchExportCount.value} 位教师，默认单个工作簿(xlsx)，也可切换为压缩包(zip)`
})

const detailOpen = ref(false)
const detailRow = ref(null)
const overviewRow = computed(() => {
  if (!isTeacher.value) return null
  const semester = queryParams.value.semester
  return workloadSummaryList.value.find(row => row.semester === semester) || null
})

const previewOpen = ref(false)
const previewLoading = ref(false)
const previewData = ref(null)
const previewQuery = reactive({ userId: null, semester: null })

// 一键核算对话框：确认阶段 runResult 为空，核算完成后填入分阶段结果
const runOpen = ref(false)
const runResult = ref(null)
const runIncludeG11 = ref(true)
const runMode = ref('single') // single / selected / semester
const runContext = reactive({ userId: null, semester: null })

const STAGE_LABELS = {
  VALIDATE: '一致性校验',
  GENERATE_G11: '同步岗位减免到 G11',
  RECALC_ITEMS: '重算明细',
  RECALC_SUMMARY: '重算汇总',
  RECALC_PAY: '重算酬金'
}
function stageLabel(code) {
  return STAGE_LABELS[code] || code
}
function stageDesc(stage) {
  const msg = stage.message || (stage.ok ? '完成' : '失败')
  return stage.count ? `${msg}（${stage.count} 条）` : msg
}
// 仅单教师核算成功、且无未核对明细时，才允许在结果框内直接提交审核
const canSubmitAfterRun = computed(() =>
  runMode.value === 'single'
  && runResult.value
  && runResult.value.summary
  && Number(runResult.value.unconfirmedCount) === 0
)
const runConfirmText = computed(() => {
  if (runMode.value === 'single') {
    return `将对「${userLabel(runContext.userId)}」${runContext.semester} 执行一键核算`
  }
  if (runMode.value === 'selected') {
    return `将对 ${selectedUserIds.value.length} 位所选教师执行 ${runContext.semester} 一键核算，单人失败不影响其他人`
  }
  return `将核算 ${runContext.semester} 学期全部有明细的教师，耗时较长，期间请勿重复点击`
})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    semester: getCurrentSemester(),
    status: null
  }
})

const { queryParams } = toRefs(data)

/** 查询学期工作量汇总列表 */
function getList() {
  const requestId = ++listRequestId
  loading.value = true
  listWorkloadSummary(queryParams.value).then(response => {
    if (requestId !== listRequestId) return
    workloadSummaryList.value = response.rows
    total.value = response.total
  }).catch(() => {
    if (requestId !== listRequestId) return
    workloadSummaryList.value = []
    total.value = 0
    proxy.$modal.msgError('获取学期汇总失败，请稍后重试')
  }).finally(() => {
    if (requestId === listRequestId) loading.value = false
  })
}

/** 搜索按钮操作；URL 变化时由路由 watcher 统一发起请求，避免重复查询。 */
function handleQuery() {
  queryParams.value.pageNum = 1
  // 筛选条件变化即清空跨页选择，避免把不同学期/教师/状态混进同一批次
  clearCrossPageSelection()
  if (!syncQueryRoute()) getList()
}

/** 重置筛选但保留“当前学期”上下文，避免总览意外跨学期。 */
function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.semester = getCurrentSemester()
  handleQuery()
}

// 多选框选中数据：仅更新跨页 Map（唯一真源），ids/selectedUserIds/multiple 皆由其派生。
// reserve-selection 下 selection 已含跨页勾选，「核算所选」与批量导出因此同为跨页语义，
// 与按钮文案「表格中勾选的教师」「跨页勾选的教师」一致；二者均要求先选定学期。
function handleSelectionChange(selection) {
  selectedRowMap.value = mergePageSelection(
    selectedRowMap.value,
    workloadSummaryList.value,
    selection
  )
}

/** 清空跨页选择：筛选范围（学期/教师/状态）变化时调用，防止跨学期混批 */
function clearCrossPageSelection() {
  selectedRowMap.value = new Map()
  const table = summaryTableRef.value
  if (!table) return
  table.clearSelection()
  // reserve-selection 下 clearSelection 对已渲染行的勾选偶发不同步，下一帧再清一次兜底；
  // 且行键为唯一 id，切换筛选后新行 id 不同，旧勾选不会重新附着到新数据上
  nextTick(() => {
    if (summaryTableRef.value) summaryTableRef.value.clearSelection()
  })
}

/** 校验教师+学期已选 */
function checkTeacherSemester() {
  const { userId, semester } = queryParams.value
  const uid = isTeacher.value ? userStore.id : userId
  if (!uid || !semester) {
    proxy.$modal.alertWarning(isTeacher.value ? '请先填写「学年学期」' : '请先在搜索栏选择「教师」并填写「学年学期」')
    return null
  }
  return { userId: uid, semester }
}

/** 重算单行汇总 */
function handleRecalcSummary(row) {
  proxy.$modal.confirm(`确认重算「${userLabel(row.userId)}」${row.semester} 学期汇总吗？`).then(function() {
    return recalcSummary(row.userId, row.semester)
  }).then((res) => {
    getList()
    const unconfirmed = res.data?.unconfirmedCount
    proxy.$modal.msgSuccess(unconfirmed > 0 ? `重算完成，尚有 ${unconfirmed} 条明细未核对` : '重算完成')
  }).catch(() => {})
}

/** 打开单教师一键核算确认框 */
function handleStartRun() {
  const checked = checkTeacherSemester()
  if (!checked) return
  runMode.value = 'single'
  runContext.userId = checked.userId
  runContext.semester = checked.semester
  runResult.value = null
  runIncludeG11.value = true
  runOpen.value = true
}

/** 打开「核算所选」确认框 */
function handleRunSelected() {
  const { semester } = queryParams.value
  if (!semester) {
    proxy.$modal.alertWarning('请先在搜索栏填写「学年学期」')
    return
  }
  if (!selectedUserIds.value.length) {
    proxy.$modal.alertWarning('请先勾选需要核算的教师')
    return
  }
  runMode.value = 'selected'
  runContext.userId = null
  runContext.semester = semester
  runResult.value = null
  runIncludeG11.value = true
  runOpen.value = true
}

/** 打开「全学期核算」确认框 */
function handleRunSemester() {
  const { semester } = queryParams.value
  if (!semester) {
    proxy.$modal.alertWarning('请先在搜索栏填写「学年学期」')
    return
  }
  runMode.value = 'semester'
  runContext.userId = null
  runContext.semester = semester
  runResult.value = null
  runIncludeG11.value = true
  runOpen.value = true
}

/** 执行核算：按 runMode 分单教师 / 批量 */
function doRun() {
  if (runMode.value === 'single') {
    calcLoading.value = true
    runCalc(runContext.userId, runContext.semester, runIncludeG11.value).then((res) => {
      runResult.value = res.data
      getList()
    }).catch(() => {}).finally(() => {
      calcLoading.value = false
    })
    return
  }
  // 批量：核算所选 / 全学期
  const userIds = runMode.value === 'selected' ? selectedUserIds.value : []
  calcLoading.value = true
  batchCalcLoading.value = true
  runCalcBatch(runContext.semester, userIds, runIncludeG11.value).then((res) => {
    runOpen.value = false
    getList()
    notifyBatchResult(res.data)
  }).catch(() => {}).finally(() => {
    calcLoading.value = false
    batchCalcLoading.value = false
  })
}

/** 结果框内提交审核（仅单教师、无未核对明细时可用） */
function handleSubmitAfterRun() {
  const summaryId = runResult.value?.summary?.id
  if (!summaryId) {
    proxy.$modal.alertWarning('未获取到汇总记录，请刷新列表后在行内提交')
    return
  }
  proxy.$modal.confirm(`确认提交「${userLabel(runContext.userId)}」${runContext.semester} 的工作量汇总审核？`).then(() => {
    return auditSubmit(summaryId)
  }).then(() => {
    runOpen.value = false
    getList()
    proxy.$modal.msgSuccess('已提交审核')
  }).catch(() => {})
}

/**
 * 批量核算结果提示：成功数 + 失败明细。
 * 失败按教师逐条列出原因，避免「部分成功」被一句"完成"糊过去。
 */
function notifyBatchResult(data) {
  const success = data?.successCount ?? 0
  const fail = data?.failCount ?? 0
  const items = data?.recalcItemCount ?? 0
  if (!fail) {
    proxy.$modal.msgSuccess(`核算完成：${success} 位教师，共 ${items} 条明细`)
    return
  }
  // 失败原因来自后端异常消息，可能含尖括号，转义后再拼 HTML
  const esc = (v) => String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const lines = (data?.failures || [])
    .map(f => `${esc(f.userName || f.userId)}：${esc(f.reason)}`)
    .join('<br/>')
  // 不走 $modal.alertError：它不开 HTML 渲染，多行失败原因会挤成一行
  ElMessageBox.alert(
    `成功 ${success} 位，失败 ${fail} 位：<br/>${lines}`,
    '批量核算结果',
    { type: 'warning', dangerouslyUseHTMLString: true }
  ).catch(() => {})
}

/** 导出表一（标准格式）：对齐教务处「-新」模板，一行一开课任务 39 列 */
function handleExportStdForm1(row) {
  const uid = row ? row.userId : (isTeacher.value ? userStore.id : queryParams.value.userId)
  const semester = row ? row.semester : queryParams.value.semester
  if (!uid || !semester) {
    proxy.$modal.alertWarning(isTeacher.value
      ? '请先填写「学年学期」'
      : '请先在搜索栏选择「教师」并填写「学年学期」，或在行内「更多」中导出')
    return
  }
  proxy.$modal.loading('正在导出表一...')
  exportAttachment1({ userId: uid, semester }).then(res => {
    return saveBlobAsFile(res, `表一_${userLabel(uid)}_${semester}.xlsx`)
  }).finally(() => {
    proxy.$modal.closeLoading()
  })
}

/** 导出附件1：工作量明细（含系数与班级），学期取搜索栏，教师取搜索栏/本人 */
function handleExportAttach1(row) {
  const uid = row ? row.userId : (isTeacher.value ? userStore.id : queryParams.value.userId)
  const semester = row ? row.semester : queryParams.value.semester
  if (!uid || !semester) {
    proxy.$modal.alertWarning(isTeacher.value
      ? '请先填写「学年学期」'
      : '请先在搜索栏选择「教师」并填写「学年学期」，或在行内「更多」中导出')
    return
  }
  proxy.$modal.loading('正在导出附件1...')
  exportPersonalWorkload({ userId: uid, semester }).then(res => {
    return saveBlobAsFile(res, `工作量明细_${userLabel(uid)}_${semester}.xlsx`)
  }).finally(() => {
    proxy.$modal.closeLoading()
  })
}

/** 导出附件2：绩效酬金统计（全学期） */
function handleExportAttach2() {
  const { semester } = queryParams.value
  if (!semester) {
    proxy.$modal.alertWarning('请先在搜索栏填写「学年学期」')
    return
  }
  proxy.$modal.loading('正在导出附件2...')
  exportPaySummary({ semester }).then(res => {
    return saveBlobAsFile(res, `绩效酬金统计_${semester}.xlsx`)
  }).finally(() => {
    proxy.$modal.closeLoading()
  })
}

/** 取当前跨页选中、且属于指定学期的教师 userId（批量导出为单学期操作，须按学期过滤后去重） */
function collectExportUserIds(semester) {
  const inSemester = new Map(
    [...selectedRowMap.value].filter(([, row]) => row.semester === semester)
  )
  return distinctUserIds(inSemester)
}

/** 打开批量导出表一的格式确认框（按跨页勾选、当前学期的教师） */
function handleBatchExport() {
  const semester = queryParams.value.semester
  if (!semester) {
    proxy.$modal.alertWarning('请先在搜索栏选定「学年学期」（批量导出为单学期操作）')
    return
  }
  const count = collectExportUserIds(semester).length
  if (!count) {
    proxy.$modal.alertWarning('请先勾选需要导出的教师')
    return
  }
  if (count > 100) {
    // 后端同样限制 100 人，这里先行拦截并提示拆批，避免无谓的大请求
    proxy.$modal.alertWarning(`已选 ${count} 位教师，单次最多 100 位，请拆分批次后再导出`)
    return
  }
  batchExportSemester.value = semester
  batchExportCount.value = count
  // 1-20 人默认 xlsx；21-100 人只能 zip
  batchExportFormat.value = count > 20 ? 'zip' : 'xlsx'
  batchExportOpen.value = true
}

/** 执行批量导出：xlsx 走默认 MIME，zip 显式 application/zip */
function doBatchExport() {
  const semester = batchExportSemester.value
  const userIds = collectExportUserIds(semester)
  const count = userIds.length
  if (!count) {
    proxy.$modal.msgWarning('已选教师为空，请重新勾选后再导出')
    batchExportOpen.value = false
    return
  }
  const format = batchExportFormat.value === 'zip' ? 'zip' : 'xlsx'
  const ext = format
  const mime = format === 'zip' ? 'application/zip' : undefined
  batchExportLoading.value = true
  exportAttachment1Batch({ semester, userIds, format }).then(res => {
    return saveBlobAsFile(res, `表一批量_${semester}_${count}人.${ext}`, mime)
  }).then(() => {
    batchExportOpen.value = false
  }).catch(() => {
    // saveBlobAsFile 已就 JSON/纯文本错误体弹过提示、request 拦截器兜底网络错误；
    // 此处只负责收拾对话框：关闭并复位，避免留下卡在 loading 的半截对话框
    batchExportOpen.value = false
  }).finally(() => {
    batchExportLoading.value = false
  })
}

/** 生成 G11 管理服务明细 */
function handleGenG11() {
  const { semester } = queryParams.value
  if (!semester) {
    proxy.$modal.alertWarning('请先在搜索栏填写「学年学期」')
    return
  }
  proxy.$modal.confirm(`确认按 ${semester} 学期的岗位任职记录批量生成 G11 明细吗？`).then(function() {
    return genG11(semester, queryParams.value.userId)
  }).then((res) => {
    proxy.$modal.msgSuccess(`生成完成，共 ${res.data ?? 0} 条 G11 明细`)
  }).catch(() => {})
}

/** 打开汇总预览 */
function handlePreview() {
  previewQuery.userId = isTeacher.value ? userStore.id : queryParams.value.userId
  previewQuery.semester = queryParams.value.semester
  previewData.value = null
  previewOpen.value = true
}

/** 执行预览 */
function doPreview() {
  if (!previewQuery.userId || !previewQuery.semester) {
    proxy.$modal.alertWarning('请选择教师并填写学年学期')
    return
  }
  previewLoading.value = true
  previewSummary(previewQuery.userId, previewQuery.semester).then(res => {
    previewData.value = res.data
  }).finally(() => {
    previewLoading.value = false
  })
}

/** 详情 */
function handleDetail(row) {
  detailRow.value = row
  detailOpen.value = true
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除选中的学期汇总记录？').then(function() {
    return delWorkloadSummary(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/workloadSummary/export', {
    ...queryParams.value
  }, `workloadSummary_${new Date().getTime()}.xlsx`)
}

/** 提交审核 */
function handleSubmit(row) {
  if (row.needsRecalculation === 1) {
    proxy.$modal.msgWarning('来源数据已变化，请先重新核算汇总')
    return
  }
  if (settlementSubmitBlocked(row)) {
    proxy.$modal.msgWarning(settlementHint(row))
    return
  }
  proxy.$modal.confirm(`确认提交「${userLabel(row.userId)}」${row.semester} 的工作量汇总审核？`).then(() => {
    return auditSubmit(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('已提交审核')
  }).catch(() => {})
}

/** 教师确认工作量 */
function handleTeacherConfirm(row) {
  proxy.$modal.confirm(`确认本人 ${row.semester} 学期的工作量汇总吗？`).then(() => {
    return auditTeacherConfirm(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('已确认工作量')
  }).catch(() => {})
}

/** 审核通过（即完结） */
const approving = ref(false)
async function handleApprove(row) {
  if (approving.value) return
  approving.value = true
  try {
    const detail = (await getSummaryAuditDetail(row.id)).data
    const block = approvalBlock(detail)
    const counts = detail.reviewCounts || {}
    if (block) { proxy.$modal.alertWarning(`未处理 ${counts.PENDING || 0} 条，已驳回 ${counts.REJECT || 0} 条。${block}`); return }
    await proxy.$modal.confirm(`未处理 ${counts.PENDING || 0} 条，已驳回 ${counts.REJECT || 0} 条。确认终审「${userLabel(row.userId)}」${row.semester}？通过后永久完结锁定；服务端仍将校验全部证据。`)
    await auditApprove(row.id)
    getList()
    proxy.$modal.msgSuccess('审核通过，汇总已完结')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') proxy.$modal.msgError(error?.message || '终审失败，请刷新查看审核证据')
  } finally { approving.value = false }
}

/** 驳回：每条汇总独立保留原因，失败刷新状态且不关闭表单。 */
const rejectOpen = ref(false), rejectBusy = ref(false), rejectError = ref(''), rejectId = ref(null), rejectCurrentStatus = ref(1)
const rejectionDrafts = createReasonDrafts(reactive({}))
const rejectReason = computed({ get: () => rejectionDrafts.get(rejectId.value), set: value => rejectionDrafts.set(rejectId.value, value) })
const rejectGuard = createSubmitGuard()
function closeReject(done) { if (!rejectBusy.value) done() }
function handleReject(row) {
  if (rejectBusy.value) return
  rejectId.value = row.id; rejectCurrentStatus.value = Number(row.status); rejectError.value = ''; rejectOpen.value = true
}
async function submitReject() {
  await rejectGuard(async () => {
    const reason = rejectReason.value, id = rejectId.value
    if (!reason.trim() || reason.length > 500) { rejectError.value = '原因必填，原文最多500字'; return }
    if (rejectCurrentStatus.value !== 1) return
    rejectBusy.value = true; rejectError.value = ''
    try {
      await sendReasonDraft(rejectionDrafts, id, auditReject)
      rejectOpen.value = false
      getList(); proxy.$modal.msgSuccess('已驳回，退回填报中')
    } catch (error) {
      rejectError.value = `${error?.code === 409 || error?.response?.status === 409 ? '汇总状态已变化' : error?.message || '驳回失败'}；原因原文已保留，请核对最新状态后重试。`
      getList()
      try {
        const latest = (await getSummaryAuditDetail(id)).data.summary
        rejectCurrentStatus.value = Number(latest.status)
        rejectError.value += ` 最新状态：${summaryStatusMap[latest.status]?.label || latest.status}。`
      } catch { rejectError.value += ' 最新详情刷新失败，请关闭后从列表重新打开。'; rejectCurrentStatus.value = null }
    } finally { rejectBusy.value = false }
  })
}

/** 更多操作下拉命令 */
function handleMoreCmd(cmd, row) {
  if (cmd === 'recalc') {
    handleRecalcSummary(row)
  } else if (cmd === 'exportItem') {
    handleExportAttach1(row)
  } else if (cmd === 'delete') {
    handleDelete(row)
  }
}

/** 批量提交审核 */
function handleBatchSubmit() {
  proxy.$modal.confirm(`确认批量提交选中的 ${ids.value.length} 条汇总记录审核？`).then(() => {
    return auditBatchSubmit(ids.value)
  }).then((res) => {
    getList()
    const receipt = res.data || {}
    const message = `已提交 ${receipt.successCount ?? 0} 条，失败 ${receipt.failCount ?? 0} 条`
    if (receipt.failCount > 0) {
      const reasons = (receipt.failDetails || []).map(item => `汇总 ${item.id}：${item.reason}`).join('；')
      proxy.$modal.alertWarning(`${message}。${reasons}`)
    } else {
      proxy.$modal.msgSuccess(message)
    }
  }).catch(() => {})
}

const WORKLOAD_SUMMARY_PATH = '/workload/workloadSummary'
const allowedSummaryStatuses = Object.keys(summaryStatusMap).map(Number)

function parseEnumQuery(value, allowedValues) {
  if (typeof value !== 'string' || value.trim() === '') return null
  const parsed = Number(value)
  return Number.isInteger(parsed) && allowedValues.includes(parsed) ? parsed : null
}

function syncQueryRoute() {
  const query = { ...route.query }
  const semester = typeof queryParams.value.semester === 'string'
    ? queryParams.value.semester.trim()
    : ''
  const status = queryParams.value.status
  if (semester) query.semester = semester
  else delete query.semester
  if (status !== null && status !== undefined) query.status = String(status)
  else delete query.status
  const changed = query.semester !== route.query.semester
    || query.status !== route.query.status
  if (changed) router.replace({ query })
  return changed
}

watch(
  () => [route.path, route.query.semester, route.query.status],
  ([path, semester, status]) => {
    if (path !== WORKLOAD_SUMMARY_PATH) return
    queryParams.value.semester = typeof semester === 'string' && semester.trim()
      ? semester
      : getCurrentSemester()
    queryParams.value.status = parseEnumQuery(status, allowedSummaryStatuses)
    queryParams.value.pageNum = 1
    // 学期/状态经 URL 变化也要清跨页选择，防止跨筛选范围混批
    clearCrossPageSelection()
    getList()
  },
  { immediate: true }
)
</script>

<style scoped>
.pay-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.4;
}
.total-num {
  font-weight: 700;
  color: var(--el-color-primary);
}
.excess-num {
  font-weight: 600;
  color: var(--el-color-warning);
}
.remark-text {
  color: var(--el-text-color-regular);
}
.mb12 {
  margin-bottom: 12px;
}
.preview-form {
  margin-bottom: 8px;
}
.run-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.run-steps {
  margin-bottom: 12px;
}
</style>

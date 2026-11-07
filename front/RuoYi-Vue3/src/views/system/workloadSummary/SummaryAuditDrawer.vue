<template>
  <el-drawer
    :model-value="modelValue"
    title="学期汇总 · 审核与历史证据"
    size="min(1100px, 96vw)"
    :close-on-click-modal="!saving"
    :before-close="closeDrawer"
  >
    <el-button
      :disabled="saving"
      @click="reload"
    >
      刷新全部证据
    </el-button>
    <el-alert
      v-if="detail.state.error"
      :title="detail.state.error"
      type="error"
      :closable="false"
    >
      <el-button @click="reload">
        重试
      </el-button>
    </el-alert>
    <div v-loading="detail.state.loading">
      <template v-if="d">
        <h3>{{ userName(d.summary.userId) }} · {{ d.summary.semester }}</h3>
        <el-alert
          :title="evidenceLabel"
          :description="d.reviewEvidenceMissingReason || '仅展示提交时保存的证据；不从当前政策补造历史。'"
          :type="d.reviewEvidenceStatus === 'VERIFIED' ? 'success' : 'warning'"
          :closable="false"
        />
        <el-descriptions
          :column="2"
          border
          class="section"
        >
          <el-descriptions-item label="状态">
            {{ summaryStatusMap[d.summary.status]?.label }}
          </el-descriptions-item>
          <el-descriptions-item label="当前数据版本">
            {{ d.summary.dataVersion ?? '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="总工作量">
            {{ d.summary.totalWorkload }} 学时
          </el-descriptions-item>
          <el-descriptions-item label="绩效酬金">
            {{ d.summary.performancePay }} 元
          </el-descriptions-item>
          <el-descriptions-item label="最近汇总核算">
            {{ d.summary.lastCalculatedAt || '未保存' }}
          </el-descriptions-item>
          <el-descriptions-item label="批次版本">
            {{ d.currentBatch?.summaryDataVersion ?? '未保存' }}
          </el-descriptions-item>
          <el-descriptions-item
            label="批次号"
            :span="2"
          >
            {{ d.currentBatch?.reviewBatchNo || '未保存' }}
          </el-descriptions-item>
        </el-descriptions>
        <el-collapse class="section">
          <el-collapse-item
            title="当前汇总明细、达标情况与签字"
            name="current-summary"
          >
            <el-descriptions
              :column="2"
              border
            >
              <el-descriptions-item
                v-for="(label, key) in currentLabels"
                :key="key"
                :label="label"
              >
                {{ d.summary[key] ?? '未保存' }}
              </el-descriptions-item>
            </el-descriptions>
          </el-collapse-item>
        </el-collapse>
        <h3>提交时保存的汇总与费率</h3>
        <el-descriptions
          :column="2"
          border
        >
          <el-descriptions-item
            v-for="(label, key) in savedLabels"
            :key="key"
            :label="label"
          >
            {{ d.summarySnapshot?.[key] ?? '未保存历史字段' }}
          </el-descriptions-item>
        </el-descriptions>
        <p>当前教师确认：{{ d.summary.teacherSign || '未签字' }} {{ d.summary.teacherSignTime || '' }}；教师确认属于独立动作，不重写原批次签字。</p>
        <h3>来源统计（提交时）</h3>
        <el-descriptions
          :column="2"
          border
        >
          <el-descriptions-item
            v-for="(label, key) in sourceLabels"
            :key="key"
            :label="label"
          >
            {{ d.sourceCounts?.[key] ?? '未保存' }}
          </el-descriptions-item>
        </el-descriptions>
        <p>{{ d.sourceCounts?.importRawMissingReason || (d.sourceCounts ? '' : '缺少历史批次，无法还原当时来源数量。') }}</p>
        <el-alert
          v-if="Number(d.summary.status) === 1"
          :title="approvalBlock(d) || `未处理 ${d.reviewCounts.PENDING || 0} 条、已驳回 ${d.reviewCounts.REJECT || 0} 条；终审仍须服务端完整校验`"
          type="info"
          :closable="false"
        />
        <el-collapse class="section">
          <el-collapse-item
            title="保存的汇总、酬金与结算引用 / 费率证据"
            name="saved"
          >
            <p>批次哈希：{{ d.currentBatch?.dataHash || '未保存' }}</p>
            <p>原结算快照：{{ d.currentBatch?.settlementSnapshotId || '历史缺失' }} · {{ d.currentBatch?.settlementSnapshotHash || '未保存哈希' }}</p>
            <p>下方为实际保存的汇总费率、人员、签字与酬金输出；明细规则来源见逐条审核展开区。接口未返回的完整结算政策不以当前政策替代。</p>
            <pre>{{ JSON.stringify({ summary: d.summarySnapshot, pay: d.paySnapshot }, null, 2) }}</pre>
          </el-collapse-item>
        </el-collapse>
        <h3>逐条审核记录（只读）</h3>
        <el-form :inline="true">
          <el-form-item label="历史批次号">
            <el-input
              v-model="historyBatch"
              clearable
              maxlength="50"
              placeholder="留空为汇总指向批次"
            />
          </el-form-item><el-form-item>
            <el-button @click="changeBatch">
              查看批次
            </el-button>
          </el-form-item>
        </el-form>
        <el-alert
          v-if="pages.reviews.state.error"
          :title="pages.reviews.state.error"
          type="error"
          :closable="false"
        >
          <el-button @click="loadPage('reviews')">
            重试
          </el-button>
        </el-alert>
        <review-evidence-table
          v-loading="pages.reviews.state.loading"
          :rows="pages.reviews.state.data?.rows || []"
        />
        <pagination
          v-show="pages.reviews.state.data?.total > 0"
          v-model:page="paging.reviews.pageNum"
          v-model:limit="paging.reviews.pageSize"
          :page-sizes="[10,20,50,100]"
          :total="pages.reviews.state.data?.total || 0"
          @pagination="loadPage('reviews')"
        />
        <template
          v-for="section in ['auditLogs', 'offlineAdjustmentNotes']"
          :key="section"
        >
          <h3>{{ section === 'auditLogs' ? '操作时间线' : '线下补差说明（只追加，不改原结算）' }}</h3>
          <el-alert
            v-if="pages[section].state.error"
            :title="pages[section].state.error"
            type="error"
            :closable="false"
          >
            <el-button @click="loadPage(section)">
              重试
            </el-button>
          </el-alert>
          <p>列表省略长文本；展开每条记录可查看完整原文、凭证与附件引用。</p>
          <el-table
            v-loading="pages[section].state.loading"
            :data="pages[section].state.data?.rows || []"
            stripe
          >
            <el-table-column type="expand">
              <template #default="{ row }">
                <el-descriptions
                  :column="1"
                  border
                  class="record-original"
                >
                  <el-descriptions-item
                    v-for="col in columns[section]"
                    :key="col[0]"
                    :label="col[1]"
                  >
                    <div class="full-value">
                      {{ row[col[0]] ?? '—' }}
                    </div>
                  </el-descriptions-item>
                </el-descriptions>
              </template>
            </el-table-column>
            <el-table-column
              v-for="col in columns[section]"
              :key="col[0]"
              :prop="col[0]"
              :label="col[1]"
              min-width="130"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <span class="raw">{{ section === 'auditLogs' && col[0] === 'action' ? auditActionLabel(row.action) : row[col[0]] ?? '—' }}</span>
              </template>
            </el-table-column>
          </el-table>
          <pagination
            v-show="pages[section].state.data?.total > 0"
            v-model:page="paging[section].pageNum"
            v-model:limit="paging[section].pageSize"
            :page-sizes="[10,20,50,100]"
            :total="pages[section].state.data?.total || 0"
            @pagination="loadPage(section)"
          />
        </template>
        <template v-if="canAppend">
          <h3>追加线下说明</h3><p>仅记录已在线下处理的补差，不触发线上支付。原说明更正请另追加并注明前条编号。未确认提交的草稿与重试标识在本页面内按汇总保留，关闭抽屉后重开可继续重试。</p>
          <el-form
            label-width="140px"
            :disabled="saving"
          >
            <el-form-item
              label="补差金额（元）"
              required
            >
              <el-input
                v-model="note.adjustmentAmount"
                placeholder="如 -123.45，最多两位小数"
                inputmode="decimal"
              />
            </el-form-item>
            <el-form-item
              label="处理日期"
              required
            >
              <el-date-picker
                v-model="note.handledAt"
                type="date"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
            <el-form-item
              label="原因"
              required
            >
              <el-input
                v-model="note.reason"
                type="textarea"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="凭证号">
              <el-input
                v-model="note.referenceNo"
                maxlength="100"
              />
            </el-form-item>
            <el-form-item label="备注">
              <el-input
                v-model="note.noteText"
                type="textarea"
                maxlength="10000"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="受控附件引用">
              <el-input
                v-model="note.attachmentRef"
                maxlength="500"
                placeholder="仅填写已有 /profile/upload/… 引用"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="saving"
                @click="append"
              >
                追加说明
              </el-button>
            </el-form-item>
          </el-form>
          <el-alert
            v-if="noteMessage"
            :title="noteMessage"
            :type="noteSuccess ? 'success' : 'error'"
            :closable="false"
          />
        </template>
      </template>
    </div>
  </el-drawer>
</template>
<script setup>
import { computed, reactive, ref, watch } from 'vue'
import auth from '@/plugins/auth'
import useUserStore from '@/store/modules/user'
import { useUserMap } from '@/utils/userCache'
import { summaryStatusMap } from '@/utils/bizDict'
import { getSummaryAuditDetail, listSummaryEvidence } from '@/api/system/workloadSummary'
import { addOfflineNote } from '@/api/system/itemReview'
import { createResource, createNoteDrafts, createSubmitGuard, approvalBlock, pageParams, auditActionLabel } from '@/utils/reviewUi'
import ReviewEvidenceTable from '../auditCenter/ReviewEvidenceTable.vue'
const props = defineProps({ modelValue: Boolean, summaryId: [Number, String] })
const emit = defineEmits(['update:modelValue'])
const user = useUserStore(), { userName } = useUserMap()
const newState = () => reactive({ loading: false, error: '', data: null })
const detail = createResource(newState()), d = computed(() => detail.state.data?.data)
const sections = ['reviews', 'auditLogs', 'offlineAdjustmentNotes']
const pages = Object.fromEntries(sections.map(key => [key, createResource(newState())]))
const paging = reactive(Object.fromEntries(sections.map(key => [key, { pageNum: 1, pageSize: 10 }])))
const historyBatch = ref(''), saving = ref(false), noteMessage = ref(''), noteSuccess = ref(false)
const noteDrafts = createNoteDrafts(reactive({}))
const noteScope = computed(() => `${user.id}:${props.summaryId}`)
const note = computed(() => noteDrafts.get(noteScope.value).form)
const guard = createSubmitGuard()
const canAppend = computed(() => Number(d.value?.summary.status) === 2 && !user.roles.some(r => ['teacher', 'leader'].includes(r)) && auth.hasPermi('system:offlineAdjustmentNote:add'))
const evidenceLabel = computed(() => ({ VERIFIED: '当前审核批次', HISTORY_ONLY: '历史审核批次：草稿已退回，此批次只读', STALE: '审核批次已失效', NOT_SUBMITTED: '尚未提交审核', HISTORICAL_MISSING: '历史审核批次缺失' }[d.value?.reviewEvidenceStatus]))
const currentLabels = { G7: '第一课堂（学时）', G8: '第二课堂（学时）', G9: '其他工作量（学时）', G10: '教学合计（学时）', G11: '管理服务（学时）', ratedWorkload: '额定工作量（学时）', excessWorkload: '超额工作量（学时）', title: '核算时职称', payRate: '核算时费率（元）', basicTeachingStandard: '基本教学标准（学时）', basicTeachingMet: '达标标记（1达标/0未达标）', academicAssistantSign: '教务签字', academicAssistantSignTime: '教务签字时间', lockTime: '完结时间', g8Remark: '第二课堂备注', g11Remark: '管理服务备注', remark: '汇总备注' }
const savedLabels = { g7: '第一课堂（学时）', g8: '第二课堂（学时）', g9: '其他工作量（学时）', g10: '教学合计（学时）', g11: '管理服务（学时）', totalWorkload: '总工作量（学时）', ratedWorkload: '额定工作量（学时）', excessWorkload: '超额工作量（学时）', title: '核算时职称', payRate: '核算时费率（元）', performancePay: '绩效酬金（元）', teacherSign: '提交时教师签字' }
const sourceLabels = { importRawRowCount: '原始导入行', distinctTeachingTaskReferenceCount: '关联有效教学任务', includedReviewItemCount: '纳入审核明细', excludedRejectedItemCount: '排除已驳回明细', distinctRoleAssignmentReferenceCount: '岗位来源', manualDeclarationItemCount: '手工申报明细' }
const columns = { auditLogs: [['action','动作'],['operatorName','操作人'],['createTime','记录时间'],['reason','操作说明 / 引用']], offlineAdjustmentNotes: [['id','编号'],['adjustmentAmount','金额（元）'],['handledAt','处理日期'],['handledBy','经办人'],['reason','原因原文'],['referenceNo','凭证号'],['noteText','备注'],['attachmentRef','受控附件引用'],['createdAt','记录时间']] }
function closeDrawer(done) { if (!saving.value) { emit('update:modelValue', false); done() } }
function loadPage(section) {
  const id = props.summaryId
  const params = pageParams(paging[section].pageNum, paging[section].pageSize)
  if (section === 'reviews' && historyBatch.value) params.batchNo = historyBatch.value
  return pages[section].load(() => listSummaryEvidence(id, section, params))
}
function changeBatch() { paging.reviews.pageNum = 1; loadPage('reviews') }
async function reload() {
  if (!props.summaryId || !props.modelValue) return
  const id = props.summaryId
  await Promise.all([detail.load(() => getSummaryAuditDetail(id)), ...sections.map(loadPage)])
}
async function append() {
  await guard(async () => {
    saving.value = true; noteMessage.value = ''; noteSuccess.value = false
    try {
      if (!canAppend.value) throw new Error('当前状态或权限不允许追加')
      const scope = noteScope.value, id = props.summaryId
      const request = noteDrafts.request(scope)
      await addOfflineNote(id, request.body, request.key)
      noteMessage.value = '说明已追加；原结算未变更'; noteSuccess.value = true
      noteDrafts.confirm(scope)
      paging.offlineAdjustmentNotes.pageNum = 1
      await loadPage('offlineAdjustmentNotes')
    } catch (e) { noteMessage.value = `${e?.message || '追加失败或版本冲突'}；输入与重试标识已保留。可刷新证据后重试。` }
    finally { saving.value = false }
  })
}
watch(() => [props.modelValue, props.summaryId, user.id], ([open], previous) => {
  if (previous?.[0]) noteDrafts.close(`${previous[2]}:${previous[1]}`)
  detail.invalidate(); sections.forEach(key => { pages[key].invalidate(); paging[key].pageNum = 1; paging[key].pageSize = 10 })
  historyBatch.value = ''; noteMessage.value = ''
  if (open) reload()
}, { immediate: true })
</script>
<style scoped>
h3 { margin: 24px 0 12px; font-size: 16px; }
p { font-size: 14px; line-height: 1.6; color: var(--el-text-color-regular); }
.section { margin-top: 16px; }
pre { white-space: pre-wrap; overflow-wrap: anywhere; max-height: 400px; overflow: auto; background: var(--el-fill-color-light); padding: 12px; }
.raw { display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.record-original { padding: 12px 24px; }
.full-value { white-space: pre-wrap; overflow-wrap: anywhere; max-height: 300px; overflow: auto; }
</style>

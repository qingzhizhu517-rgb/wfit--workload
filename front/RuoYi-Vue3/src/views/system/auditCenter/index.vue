<template>
  <div class="app-container">
    <h2>工作量审核中心</h2>
    <p>逐条核对提交时保存的证据。展开来源查看历史计算快照；完成逐条审核后，到汇总页终审。</p>
    <el-form
      :inline="true"
      :model="query"
      label-width="72px"
      @submit.prevent="search"
    >
      <el-form-item label="汇总编号">
        <el-input
          v-model="query.summaryId"
          clearable
        />
      </el-form-item>
      <el-form-item label="教师">
        <user-select v-model="query.userId" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select
          v-model="query.reviewType"
          clearable
          style="width:150px"
        >
          <el-option
            v-for="(label, value) in reviewTypes"
            :key="value"
            :value="value"
            :label="label"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="结果">
        <el-select
          v-model="query.result"
          clearable
          style="width:120px"
        >
          <el-option
            v-for="(label, value) in reviewResults"
            :key="value"
            :value="value"
            :label="label"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="批次">
        <el-input
          v-model="query.batchNo"
          clearable
          maxlength="50"
        />
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          native-type="submit"
        >
          搜索
        </el-button><el-button @click="reset">
          重置
        </el-button>
      </el-form-item>
    </el-form>
    <div class="toolbar">
      <el-button
        v-if="canReview"
        :disabled="!selected.length || busy"
        @click="openCommand('PASS', selected)"
      >
        批量通过
      </el-button><el-button
        v-if="canReview"
        :disabled="!selected.length || busy"
        @click="openCommand('REJECT', selected)"
      >
        批量驳回
      </el-button><el-button
        v-if="canWaive"
        :disabled="!selected.length || busy"
        @click="openCommand('WAIVE', selected)"
      >
        批量豁免
      </el-button><span>每批最多100条；仅当前待审批次可审核</span>
    </div>
    <el-alert
      v-if="receipt"
      :title="receipt"
      type="info"
      :closable="false"
    />
    <el-alert
      v-if="resource.state.error"
      :title="resource.state.error"
      type="error"
      :closable="false"
    >
      <el-button @click="load">
        重试
      </el-button>
    </el-alert>
    <summary-audit-drawer
      v-model="drawerOpen"
      :summary-id="drawerId"
    />
    <review-evidence-table
      v-loading="resource.state.loading"
      :rows="resource.state.data?.rows || []"
      :selectable="canReview || canWaive"
      :actions="true"
      @selection="selected = $event"
    >
      <template #actions="{ row }">
        <el-button
          v-hasPermi="['system:workloadSummary:query']"
          link
          type="primary"
          @click="drawerId = row.summaryId; drawerOpen = true"
        >
          汇总详情
        </el-button><template v-if="row.result === 'PENDING'">
          <el-button
            v-if="canReview"
            link
            type="primary"
            :disabled="busy"
            @click="openCommand('PASS', [row])"
          >
            通过
          </el-button><el-button
            v-if="canReview"
            link
            type="danger"
            :disabled="busy"
            @click="openCommand('REJECT', [row])"
          >
            驳回
          </el-button><el-button
            v-if="canWaive"
            link
            :disabled="busy"
            @click="openCommand('WAIVE', [row])"
          >
            豁免
          </el-button>
        </template><span v-else>已处理，只读</span>
      </template>
    </review-evidence-table>
    <pagination
      v-show="resource.state.data?.total > 0"
      v-model:page="query.pageNum"
      v-model:limit="query.pageSize"
      :page-sizes="[10,20,50,100]"
      :total="resource.state.data?.total || 0"
      @pagination="load"
    />
    <el-dialog
      v-model="commandOpen"
      :title="`${reviewResults[action]}审核记录`"
      width="540px"
      :close-on-click-modal="false"
      :before-close="closeCommand"
    >
      <p>将处理 {{ targets.length }} 条已选记录。终态不能改审。</p>
      <el-form label-position="top">
        <el-form-item :label="action === 'PASS' ? '原因（可选）' : '原因（必填）'">
          <el-input
            v-model="reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="commandError"
        :title="commandError"
        type="error"
        :closable="false"
      />
      <template #footer>
        <el-button
          :disabled="busy"
          @click="commandOpen = false"
        >
          {{ requiresReselect ? '保留原因并返回列表重新选择' : '取消' }}
        </el-button><el-button
          type="primary"
          :loading="busy"
          :disabled="requiresReselect"
          @click="submit"
        >
          确认{{ reviewResults[action] }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import auth from '@/plugins/auth'
import useUserStore from '@/store/modules/user'
import UserSelect from '@/components/UserSelect/index.vue'
import SummaryAuditDrawer from '../workloadSummary/SummaryAuditDrawer.vue'
import ReviewEvidenceTable from './ReviewEvidenceTable.vue'
import { listItemReviews, reviewItem, reviewBatch } from '@/api/system/itemReview'
import { getSummaryAuditDetail } from '@/api/system/workloadSummary'
import { reviewTypes, reviewResults, batchCommand, reviewCommand, createResource, createSubmitGuard, batchReceipt, batchOutcome, pageParams } from '@/utils/reviewUi'
const drawerOpen = ref(false), drawerId = ref(null)
const route = useRoute(), user = useUserStore()
const allowedRole = computed(() => !user.roles.some(role => ['teacher', 'leader'].includes(role)))
const canReview = computed(() => allowedRole.value && auth.hasPermi('system:auditRecord:review'))
const canWaive = computed(() => allowedRole.value && auth.hasPermi('system:auditRecord:waive'))
const query = reactive({ pageNum: 1, pageSize: 10, summaryId: '', userId: null, reviewType: '', result: '', batchNo: '' })
const resource = createResource(reactive({ loading: false, error: '', data: null }))
const selected = ref([]), commandOpen = ref(false), reason = ref(''), action = ref('PASS'), targets = ref([]), busy = ref(false), commandError = ref(''), receipt = ref('')
const requiresReselect = ref(false)
const guard = createSubmitGuard()
function load() {
  selected.value = []
  const params = Object.fromEntries(Object.entries(query).filter(([, v]) => v !== '' && v !== null))
  return resource.load(() => listItemReviews({ ...params, ...pageParams(query.pageNum, query.pageSize) }))
}
function search() { query.pageNum = 1; load() }
function reset() { Object.assign(query, { pageNum: 1, pageSize: 10, summaryId: '', userId: null, reviewType: '', result: '', batchNo: '' }); load() }
function closeCommand(done) { if (!busy.value) done() }
async function openCommand(kind, rows) {
  if (busy.value) return
  busy.value = true
  receipt.value = ''
  try {
    batchCommand(rows, kind, kind === 'PASS' ? '' : '预检')
    const details = new Map()
    for (const id of new Set(rows.map(r => r.summaryId))) details.set(id, (await getSummaryAuditDetail(id)).data)
    for (const row of rows) {
      const d = details.get(row.summaryId)
      if (Number(d.summary.status) !== 1 || !d.currentBatch?.current || d.reviewEvidenceStatus !== 'VERIFIED' || d.currentBatch.reviewBatchNo !== row.reviewBatchNo) throw new Error('所选包含历史或失效批次，请刷新后选择当前待审记录')
    }
    targets.value = rows.map(r => ({ ...r })); action.value = kind; requiresReselect.value = false; commandError.value = ''; commandOpen.value = true
  } catch (e) { receipt.value = e?.message || '预检失败，请重试' } finally { busy.value = false }
}
async function submit() {
  if (requiresReselect.value) return
  await guard(async () => {
    busy.value = true; commandError.value = ''
    let dispatched = false
    try {
      if (action.value === 'WAIVE' ? !canWaive.value : !canReview.value) throw new Error('无此审核权限')
      if (targets.value.length === 1) {
        const row = targets.value[0]
        const command = reviewCommand(row, action.value, reason.value)
        dispatched = true
        await reviewItem(row.id, action.value, command)
        receipt.value = '成功处理1条审核记录'
        targets.value = []; commandOpen.value = false; reason.value = ''
      } else {
        const command = batchCommand(targets.value, action.value, reason.value)
        dispatched = true
        const data = (await reviewBatch(action.value, command)).data
        receipt.value = batchReceipt(data)
        const outcome = batchOutcome(data, targets.value, reason.value)
        targets.value = outcome.targets; reason.value = outcome.reason
        commandOpen.value = outcome.open; requiresReselect.value = outcome.requiresReselect; commandError.value = outcome.error
      }
      await load()
    } catch (e) {
      requiresReselect.value = dispatched
      if (!dispatched) { commandError.value = e?.message || '请检查输入'; return }
      commandError.value = `${e?.code === 409 || e?.response?.status === 409 ? '审核版本或批次已过期' : e?.message || '审核失败'}；输入已保留，列表已刷新。请核对后重新选择记录。`
      await load()
    } finally { busy.value = false }
  })
}
watch(() => route.query.summaryId, id => { query.summaryId = typeof id === 'string' ? id : ''; query.pageNum = 1; load() }, { immediate: true })
</script>
<style scoped>
h2 { margin-top: 0; font-size: 20px; }
p, .toolbar { font-size: 14px; color: var(--el-text-color-regular); }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
</style>

<template>
  <el-table
    :data="rows"
    stripe
    row-key="id"
    @selection-change="$emit('selection', $event)"
  >
    <el-table-column
      v-if="selectable"
      type="selection"
      width="48"
      :selectable="row => row.result === 'PENDING'"
    />
    <el-table-column type="expand">
      <template #default="{ row }">
        <div class="evidence">
          <p>批次 {{ row.reviewBatchNo }} · 审核版本 {{ row.version }} · 保存哈希 {{ row.dataHash }}</p>
          <el-alert
            v-if="snapshot(row).values?.evidenceStatus !== 'VERIFIED'"
            title="历史计算快照缺失；不使用当前规则补造"
            type="warning"
            :closable="false"
          />
          <el-descriptions
            :column="2"
            border
          >
            <el-descriptions-item label="保存的规则版本">
              {{ calculationEvidence(row).ruleVersion || '未保存' }}
            </el-descriptions-item>
            <el-descriptions-item label="保存的核算版本">
              {{ snapshot(row).values?.calculationVersion ?? '未保存' }}
            </el-descriptions-item>
            <el-descriptions-item
              label="计算公式"
              :span="2"
            >
              {{ calculationEvidence(row).formulaExpression || '未保存' }}
            </el-descriptions-item>
            <el-descriptions-item label="核定工作量">
              {{ snapshot(row).values?.calculatedWorkload ?? '未保存' }} 学时
            </el-descriptions-item>
            <el-descriptions-item label="审核原因">
              <pre>{{ row.reason || '无' }}</pre>
            </el-descriptions-item>
          </el-descriptions>
          <p>保存的计算快照（含规则/费率来源与因子原文）：</p>
          <pre>{{ snapshot(row).values?.calculationSnapshotJson || '未保存历史计算快照' }}</pre>
          <p>保存的完整审核来源证据：</p><pre>{{ JSON.stringify(snapshot(row), null, 2) }}</pre>
        </div>
      </template>
    </el-table-column>
    <el-table-column
      label="教师 / 学期"
      min-width="150"
    >
      <template #default="{ row }">
        {{ userName(snapshot(row).userId) }}<br>{{ snapshot(row).semester }}
      </template>
    </el-table-column>
    <el-table-column
      label="项目 / 分类"
      min-width="170"
    >
      <template #default="{ row }">
        {{ snapshot(row).values?.courseName || snapshot(row).values?.description || snapshot(row).values?.itemType || '—' }}<br>{{ reviewTypes[row.reviewType] }}
      </template>
    </el-table-column>
    <el-table-column
      label="来源"
      min-width="150"
    >
      <template #default="{ row }">
        {{ sourceTypeMap[snapshot(row).values?.sourceType]?.label || snapshot(row).values?.sourceType || '未保存' }}<br>任务 {{ snapshot(row).values?.taskId || '—' }} / 岗位 {{ snapshot(row).values?.assignmentId || '—' }} / 明细 {{ row.itemId }}
      </template>
    </el-table-column>
    <el-table-column
      label="核定值 / 异常"
      min-width="120"
    >
      <template #default="{ row }">
        {{ snapshot(row).values?.calculatedWorkload ?? '—' }} 学时<br>{{ snapshot(row).values?.isOverLimit === '1' ? '超限' : '未标记超限' }}
      </template>
    </el-table-column>
    <el-table-column
      label="审核结果 / 原因"
      min-width="170"
    >
      <template #default="{ row }">
        <el-tag :type="resultType(row.result)">
          {{ reviewResults[row.result] }}
        </el-tag><p class="raw">
          {{ row.reason || '—' }}
        </p>{{ row.reviewerName }} {{ row.reviewedAt }}
      </template>
    </el-table-column>
    <el-table-column
      v-if="actions"
      label="操作"
      width="255"
      fixed="right"
    >
      <template #default="{ row }">
        <slot
          name="actions"
          :row="row"
        />
      </template>
    </el-table-column>
  </el-table>
</template>
<script setup>
import { snapshot, reviewTypes, reviewResults, calculationEvidence } from '@/utils/reviewUi'
import { sourceTypeMap } from '@/utils/bizDict'
import { useUserMap } from '@/utils/userCache'
defineProps({ rows: { type: Array, default: () => [] }, selectable: Boolean, actions: Boolean })
defineEmits(['selection'])
const { userName } = useUserMap()
const resultType = result => ({ PENDING: 'warning', PASS: 'success', REJECT: 'danger', WAIVE: 'info' }[result])
</script>
<style scoped>
.evidence { padding: 12px 24px; font-size: 14px; }
pre { white-space: pre-wrap; overflow-wrap: anywhere; max-height: 360px; overflow: auto; padding: 12px; background: var(--el-fill-color-light); }
.raw { white-space: pre-wrap; overflow-wrap: anywhere; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
</style>

import request from '@/utils/request'

// 提交审核（草稿 → 待审）
export function auditSubmit(id) {
  return request({
    url: '/system/audit/submit',
    method: 'post',
    params: { id }
  })
}

// 教务处审核通过（待审 → 已完结，审核即终态）—— 2026-09-10 审批简化为两级
export function auditApprove(id) {
  return request({
    url: '/system/audit/approve',
    method: 'post',
    params: { id }
  })
}

// 教务处驳回（待审 → 草稿）
export function auditReject(id, reason) {
  return request({
    url: '/system/audit/reject',
    method: 'post',
    params: { id, reason }
  })
}

// 解锁（已完结 → 草稿）
export function auditUnlock(id) {
  return request({
    url: '/system/audit/unlock',
    method: 'post',
    params: { id }
  })
}

// 批量提交审核
export function auditBatchSubmit(ids) {
  return request({
    url: '/system/audit/batchSubmit',
    method: 'post',
    params: { ids: ids.join(',') }
  })
}

// 教师确认工作量（待教务审核阶段）
export function auditTeacherConfirm(id) {
  return request({
    url: '/system/audit/teacherConfirm',
    method: 'post',
    params: { id }
  })
}

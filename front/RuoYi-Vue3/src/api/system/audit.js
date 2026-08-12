import request from '@/utils/request'

// 提交审核（草稿 → 待审）
export function auditSubmit(id) {
  return request({
    url: '/system/audit/submit',
    method: 'post',
    params: { id }
  })
}

// 教务助理审核通过（待审 → 待签）
export function auditApprove(id) {
  return request({
    url: '/system/audit/approve',
    method: 'post',
    params: { id }
  })
}

// 教务助理驳回（待审 → 草稿）
export function auditReject(id, reason) {
  return request({
    url: '/system/audit/reject',
    method: 'post',
    params: { id, reason }
  })
}

// 院领导签字确认（待签 → 已完结）
export function auditSign(id) {
  return request({
    url: '/system/audit/sign',
    method: 'post',
    params: { id }
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

// 教师确认工作量（待教务审核/待院领导签字阶段）
export function auditTeacherConfirm(id) {
  return request({
    url: '/system/audit/teacherConfirm',
    method: 'post',
    params: { id }
  })
}

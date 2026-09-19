import request from '@/utils/request'

// 教师提交系数调整申请（itemId/category/factorCode/requestedValue/reason/attachmentUrl）
export function submitCoefficientAdjustment(data) {
  return request({
    url: '/system/coefficientAdjustment',
    method: 'post',
    data: data
  })
}

// 我的申请列表（教师视角，强制只看本人）
export function myCoefficientAdjustment(query) {
  return request({
    url: '/system/coefficientAdjustment/myList',
    method: 'get',
    params: query
  })
}

// 全部申请列表（教务审批视角）
export function listCoefficientAdjustment(query) {
  return request({
    url: '/system/coefficientAdjustment/list',
    method: 'get',
    params: query
  })
}

// 通过申请（原子应用系数并重算）
export function approveCoefficientAdjustment(id, reviewReason) {
  return request({
    url: '/system/coefficientAdjustment/' + id + '/approve',
    method: 'post',
    data: { reviewReason }
  })
}

// 驳回申请（reviewReason 必填）
export function rejectCoefficientAdjustment(id, reviewReason) {
  return request({
    url: '/system/coefficientAdjustment/' + id + '/reject',
    method: 'post',
    data: { reviewReason }
  })
}

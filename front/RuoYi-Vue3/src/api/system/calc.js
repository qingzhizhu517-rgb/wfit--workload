import request from '@/utils/request'

// 重算单条明细工作量
export function recalcItem(itemId) {
  return request({
    url: '/system/calc/recalcItem/' + itemId,
    method: 'post'
  })
}

// 重算某教师某学期全部未冻结明细
export function recalcItems(userId, semester) {
  return request({
    url: '/system/calc/recalcItems',
    method: 'post',
    params: { userId, semester }
  })
}

// 重算学期汇总（落库）
export function recalcSummary(userId, semester) {
  return request({
    url: '/system/calc/recalcSummary',
    method: 'post',
    params: { userId, semester }
  })
}

// 汇总预览（不落库）
export function previewSummary(userId, semester) {
  return request({
    url: '/system/calc/preview',
    method: 'get',
    params: { userId, semester }
  })
}

// 重算酬金（需先重算汇总）
export function recalcPay(userId, semester) {
  return request({
    url: '/system/calc/recalcPay',
    method: 'post',
    params: { userId, semester }
  })
}

// 由岗位任职生成/更新 G11 管理服务明细
export function genG11(semester, userId) {
  return request({
    url: '/system/calc/genG11',
    method: 'post',
    params: { semester, userId }
  })
}

// 一把梭：重算全部未冻结明细 -> 汇总 -> 酬金
export function recalcAll(userId, semester) {
  return request({
    url: '/system/calc/recalcAll',
    method: 'post',
    params: { userId, semester }
  })
}

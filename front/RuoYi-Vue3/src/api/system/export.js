import request from '@/utils/request'

// 导出个人工作量明细表（附件1格式）
export function exportPersonalWorkload(params) {
  return request({
    url: '/system/export/personalWorkload',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

// 导出标准表一（附件1「-新」模板：一行一开课任务，39 列）
export function exportAttachment1(params) {
  return request({
    url: '/system/export/attachment1',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

// 导出绩效酬金统计表（附件2格式）
export function exportPaySummary(params) {
  return request({
    url: '/system/export/paySummary',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

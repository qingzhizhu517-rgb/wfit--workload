import request from '@/utils/request'

// 查询G6集中实习明细列表
export function listWlConcentratedInternship(query) {
  return request({
    url: '/system/wlConcentratedInternship/list',
    method: 'get',
    params: query
  })
}

// 查询G6集中实习明细详细
export function getWlConcentratedInternship(itemId) {
  return request({
    url: '/system/wlConcentratedInternship/' + itemId,
    method: 'get'
  })
}

// 新增G6集中实习明细
export function addWlConcentratedInternship(data) {
  return request({
    url: '/system/wlConcentratedInternship',
    method: 'post',
    data: data
  })
}

// 修改G6集中实习明细
export function updateWlConcentratedInternship(data) {
  return request({
    url: '/system/wlConcentratedInternship',
    method: 'put',
    data: data
  })
}

// 删除G6集中实习明细
export function delWlConcentratedInternship(itemId) {
  return request({
    url: '/system/wlConcentratedInternship/' + itemId,
    method: 'delete'
  })
}

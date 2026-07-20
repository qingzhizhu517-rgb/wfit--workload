import request from '@/utils/request'

// 查询G2课内实践明细列表
export function listWlPractice(query) {
  return request({
    url: '/system/wlPractice/list',
    method: 'get',
    params: query
  })
}

// 查询G2课内实践明细详细
export function getWlPractice(itemId) {
  return request({
    url: '/system/wlPractice/' + itemId,
    method: 'get'
  })
}

// 新增G2课内实践明细
export function addWlPractice(data) {
  return request({
    url: '/system/wlPractice',
    method: 'post',
    data: data
  })
}

// 修改G2课内实践明细
export function updateWlPractice(data) {
  return request({
    url: '/system/wlPractice',
    method: 'put',
    data: data
  })
}

// 删除G2课内实践明细
export function delWlPractice(itemId) {
  return request({
    url: '/system/wlPractice/' + itemId,
    method: 'delete'
  })
}

import request from '@/utils/request'

// 查询G5毕业论文明细列表
export function listWlThesis(query) {
  return request({
    url: '/system/wlThesis/list',
    method: 'get',
    params: query
  })
}

// 查询G5毕业论文明细详细
export function getWlThesis(itemId) {
  return request({
    url: '/system/wlThesis/' + itemId,
    method: 'get'
  })
}

// 新增G5毕业论文明细
export function addWlThesis(data) {
  return request({
    url: '/system/wlThesis',
    method: 'post',
    data: data
  })
}

// 修改G5毕业论文明细
export function updateWlThesis(data) {
  return request({
    url: '/system/wlThesis',
    method: 'put',
    data: data
  })
}

// 删除G5毕业论文明细
export function delWlThesis(itemId) {
  return request({
    url: '/system/wlThesis/' + itemId,
    method: 'delete'
  })
}

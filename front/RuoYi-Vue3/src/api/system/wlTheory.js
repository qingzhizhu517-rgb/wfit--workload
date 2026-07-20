import request from '@/utils/request'

// 查询G1理论课明细列表
export function listWlTheory(query) {
  return request({
    url: '/system/wlTheory/list',
    method: 'get',
    params: query
  })
}

// 查询G1理论课明细详细
export function getWlTheory(itemId) {
  return request({
    url: '/system/wlTheory/' + itemId,
    method: 'get'
  })
}

// 新增G1理论课明细
export function addWlTheory(data) {
  return request({
    url: '/system/wlTheory',
    method: 'post',
    data: data
  })
}

// 修改G1理论课明细
export function updateWlTheory(data) {
  return request({
    url: '/system/wlTheory',
    method: 'put',
    data: data
  })
}

// 删除G1理论课明细
export function delWlTheory(itemId) {
  return request({
    url: '/system/wlTheory/' + itemId,
    method: 'delete'
  })
}

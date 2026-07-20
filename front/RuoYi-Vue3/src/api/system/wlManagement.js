import request from '@/utils/request'

// 查询G11管理服务明细列表
export function listWlManagement(query) {
  return request({
    url: '/system/wlManagement/list',
    method: 'get',
    params: query
  })
}

// 查询G11管理服务明细详细
export function getWlManagement(itemId) {
  return request({
    url: '/system/wlManagement/' + itemId,
    method: 'get'
  })
}

// 新增G11管理服务明细
export function addWlManagement(data) {
  return request({
    url: '/system/wlManagement',
    method: 'post',
    data: data
  })
}

// 修改G11管理服务明细
export function updateWlManagement(data) {
  return request({
    url: '/system/wlManagement',
    method: 'put',
    data: data
  })
}

// 删除G11管理服务明细
export function delWlManagement(itemId) {
  return request({
    url: '/system/wlManagement/' + itemId,
    method: 'delete'
  })
}

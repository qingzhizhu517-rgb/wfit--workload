import request from '@/utils/request'

// 查询其他酬金明细列表
export function listAllowanceItem(query) {
  return request({
    url: '/system/allowanceItem/list',
    method: 'get',
    params: query
  })
}

// 查询其他酬金明细详细
export function getAllowanceItem(id) {
  return request({
    url: '/system/allowanceItem/' + id,
    method: 'get'
  })
}

// 新增其他酬金明细
export function addAllowanceItem(data) {
  return request({
    url: '/system/allowanceItem',
    method: 'post',
    data: data
  })
}

// 修改其他酬金明细
export function updateAllowanceItem(data) {
  return request({
    url: '/system/allowanceItem',
    method: 'put',
    data: data
  })
}

// 删除其他酬金明细
export function delAllowanceItem(id) {
  return request({
    url: '/system/allowanceItem/' + id,
    method: 'delete'
  })
}

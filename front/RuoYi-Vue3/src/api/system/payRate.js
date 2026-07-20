import request from '@/utils/request'

// 查询职称单位酬金费率列表
export function listPayRate(query) {
  return request({
    url: '/system/payRate/list',
    method: 'get',
    params: query
  })
}

// 查询职称单位酬金费率详细
export function getPayRate(id) {
  return request({
    url: '/system/payRate/' + id,
    method: 'get'
  })
}

// 新增职称单位酬金费率
export function addPayRate(data) {
  return request({
    url: '/system/payRate',
    method: 'post',
    data: data
  })
}

// 修改职称单位酬金费率
export function updatePayRate(data) {
  return request({
    url: '/system/payRate',
    method: 'put',
    data: data
  })
}

// 删除职称单位酬金费率
export function delPayRate(id) {
  return request({
    url: '/system/payRate/' + id,
    method: 'delete'
  })
}

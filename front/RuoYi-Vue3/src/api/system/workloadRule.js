import request from '@/utils/request'

// 查询全局核算规则参数列表
export function listWorkloadRule(query) {
  return request({
    url: '/system/workloadRule/list',
    method: 'get',
    params: query
  })
}

// 查询全局核算规则参数详细
export function getWorkloadRule(id) {
  return request({
    url: '/system/workloadRule/' + id,
    method: 'get'
  })
}

// 新增全局核算规则参数
export function addWorkloadRule(data) {
  return request({
    url: '/system/workloadRule',
    method: 'post',
    data: data
  })
}

// 修改全局核算规则参数
export function updateWorkloadRule(data) {
  return request({
    url: '/system/workloadRule',
    method: 'put',
    data: data
  })
}

// 删除全局核算规则参数
export function delWorkloadRule(id) {
  return request({
    url: '/system/workloadRule/' + id,
    method: 'delete'
  })
}

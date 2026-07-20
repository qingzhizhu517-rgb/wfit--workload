import request from '@/utils/request'

// 查询学期工作量汇总列表
export function listWorkloadSummary(query) {
  return request({
    url: '/system/workloadSummary/list',
    method: 'get',
    params: query
  })
}

// 查询学期工作量汇总详细
export function getWorkloadSummary(id) {
  return request({
    url: '/system/workloadSummary/' + id,
    method: 'get'
  })
}

// 新增学期工作量汇总
export function addWorkloadSummary(data) {
  return request({
    url: '/system/workloadSummary',
    method: 'post',
    data: data
  })
}

// 修改学期工作量汇总
export function updateWorkloadSummary(data) {
  return request({
    url: '/system/workloadSummary',
    method: 'put',
    data: data
  })
}

// 删除学期工作量汇总
export function delWorkloadSummary(id) {
  return request({
    url: '/system/workloadSummary/' + id,
    method: 'delete'
  })
}

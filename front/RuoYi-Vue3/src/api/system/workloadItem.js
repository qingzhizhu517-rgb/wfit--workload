import request from '@/utils/request'

// 查询工作量明细主表列表
export function listWorkloadItem(query) {
  return request({
    url: '/system/workloadItem/list',
    method: 'get',
    params: query
  })
}

// 查询工作量明细主表详细
export function getWorkloadItem(id) {
  return request({
    url: '/system/workloadItem/' + id,
    method: 'get'
  })
}

// 新增工作量明细主表
export function addWorkloadItem(data) {
  return request({
    url: '/system/workloadItem',
    method: 'post',
    data: data
  })
}

// 修改工作量明细主表
export function updateWorkloadItem(data) {
  return request({
    url: '/system/workloadItem',
    method: 'put',
    data: data
  })
}

// 删除工作量明细主表
export function delWorkloadItem(id) {
  return request({
    url: '/system/workloadItem/' + id,
    method: 'delete'
  })
}

// 教务核对：版本和学时均须与所查看的结果一致。
export function confirmWorkloadItem(id, data) {
  return request({ url: `/system/workloadItem/${id}/confirm`, method: 'post', data })
}

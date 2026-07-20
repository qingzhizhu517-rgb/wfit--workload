import request from '@/utils/request'

// 查询工作量类别字典列表
export function listWorkloadCategoryDict(query) {
  return request({
    url: '/system/workloadCategoryDict/list',
    method: 'get',
    params: query
  })
}

// 查询工作量类别字典详细
export function getWorkloadCategoryDict(typeCode) {
  return request({
    url: '/system/workloadCategoryDict/' + typeCode,
    method: 'get'
  })
}

// 新增工作量类别字典
export function addWorkloadCategoryDict(data) {
  return request({
    url: '/system/workloadCategoryDict',
    method: 'post',
    data: data
  })
}

// 修改工作量类别字典
export function updateWorkloadCategoryDict(data) {
  return request({
    url: '/system/workloadCategoryDict',
    method: 'put',
    data: data
  })
}

// 删除工作量类别字典
export function delWorkloadCategoryDict(typeCode) {
  return request({
    url: '/system/workloadCategoryDict/' + typeCode,
    method: 'delete'
  })
}

import request from '@/utils/request'

// 查询导入批次记录列表
export function listImportBatch(query) {
  return request({
    url: '/system/importBatch/list',
    method: 'get',
    params: query
  })
}

// 查询导入批次记录详细
export function getImportBatch(id) {
  return request({
    url: '/system/importBatch/' + id,
    method: 'get'
  })
}

// 新增导入批次记录
export function addImportBatch(data) {
  return request({
    url: '/system/importBatch',
    method: 'post',
    data: data
  })
}

// 修改导入批次记录
export function updateImportBatch(data) {
  return request({
    url: '/system/importBatch',
    method: 'put',
    data: data
  })
}

// 删除导入批次记录
export function delImportBatch(id) {
  return request({
    url: '/system/importBatch/' + id,
    method: 'delete'
  })
}

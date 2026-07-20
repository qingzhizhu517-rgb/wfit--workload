import request from '@/utils/request'

// 查询酬金汇总列表
export function listPayRecord(query) {
  return request({
    url: '/system/payRecord/list',
    method: 'get',
    params: query
  })
}

// 查询酬金汇总详细
export function getPayRecord(id) {
  return request({
    url: '/system/payRecord/' + id,
    method: 'get'
  })
}

// 新增酬金汇总
export function addPayRecord(data) {
  return request({
    url: '/system/payRecord',
    method: 'post',
    data: data
  })
}

// 修改酬金汇总
export function updatePayRecord(data) {
  return request({
    url: '/system/payRecord',
    method: 'put',
    data: data
  })
}

// 删除酬金汇总
export function delPayRecord(id) {
  return request({
    url: '/system/payRecord/' + id,
    method: 'delete'
  })
}

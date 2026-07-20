import request from '@/utils/request'

// 查询岗位任职列表
export function listRoleAssignment(query) {
  return request({
    url: '/system/roleAssignment/list',
    method: 'get',
    params: query
  })
}

// 查询岗位任职详细
export function getRoleAssignment(id) {
  return request({
    url: '/system/roleAssignment/' + id,
    method: 'get'
  })
}

// 新增岗位任职
export function addRoleAssignment(data) {
  return request({
    url: '/system/roleAssignment',
    method: 'post',
    data: data
  })
}

// 修改岗位任职
export function updateRoleAssignment(data) {
  return request({
    url: '/system/roleAssignment',
    method: 'put',
    data: data
  })
}

// 删除岗位任职
export function delRoleAssignment(id) {
  return request({
    url: '/system/roleAssignment/' + id,
    method: 'delete'
  })
}

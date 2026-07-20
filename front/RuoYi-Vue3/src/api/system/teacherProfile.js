import request from '@/utils/request'

// 查询教师业务档案列表
export function listTeacherProfile(query) {
  return request({
    url: '/system/teacherProfile/list',
    method: 'get',
    params: query
  })
}

// 查询教师业务档案详细
export function getTeacherProfile(userId) {
  return request({
    url: '/system/teacherProfile/' + userId,
    method: 'get'
  })
}

// 新增教师业务档案
export function addTeacherProfile(data) {
  return request({
    url: '/system/teacherProfile',
    method: 'post',
    data: data
  })
}

// 修改教师业务档案
export function updateTeacherProfile(data) {
  return request({
    url: '/system/teacherProfile',
    method: 'put',
    data: data
  })
}

// 删除教师业务档案
export function delTeacherProfile(userId) {
  return request({
    url: '/system/teacherProfile/' + userId,
    method: 'delete'
  })
}

import request from '@/utils/request'

// 查询导入教学任务列表
export function listTeachingTask(query) {
  return request({
    url: '/system/teachingTask/list',
    method: 'get',
    params: query
  })
}

// 查询导入教学任务详细
export function getTeachingTask(id) {
  return request({
    url: '/system/teachingTask/' + id,
    method: 'get'
  })
}

// 新增导入教学任务
export function addTeachingTask(data) {
  return request({
    url: '/system/teachingTask',
    method: 'post',
    data: data
  })
}

// 修改导入教学任务
export function updateTeachingTask(data) {
  return request({
    url: '/system/teachingTask',
    method: 'put',
    data: data
  })
}

// 删除导入教学任务
export function delTeachingTask(id) {
  return request({
    url: '/system/teachingTask/' + id,
    method: 'delete'
  })
}

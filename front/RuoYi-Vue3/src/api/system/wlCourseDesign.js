import request from '@/utils/request'

// 查询G4课程设计明细列表
export function listWlCourseDesign(query) {
  return request({
    url: '/system/wlCourseDesign/list',
    method: 'get',
    params: query
  })
}

// 查询G4课程设计明细详细
export function getWlCourseDesign(itemId) {
  return request({
    url: '/system/wlCourseDesign/' + itemId,
    method: 'get'
  })
}

// 新增G4课程设计明细
export function addWlCourseDesign(data) {
  return request({
    url: '/system/wlCourseDesign',
    method: 'post',
    data: data
  })
}

// 修改G4课程设计明细
export function updateWlCourseDesign(data) {
  return request({
    url: '/system/wlCourseDesign',
    method: 'put',
    data: data
  })
}

// 删除G4课程设计明细
export function delWlCourseDesign(itemId) {
  return request({
    url: '/system/wlCourseDesign/' + itemId,
    method: 'delete'
  })
}

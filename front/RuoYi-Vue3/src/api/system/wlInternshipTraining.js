import request from '@/utils/request'

// 查询G3教学实习实训明细列表
export function listWlInternshipTraining(query) {
  return request({
    url: '/system/wlInternshipTraining/list',
    method: 'get',
    params: query
  })
}

// 查询G3教学实习实训明细详细
export function getWlInternshipTraining(itemId) {
  return request({
    url: '/system/wlInternshipTraining/' + itemId,
    method: 'get'
  })
}

// 新增G3教学实习实训明细
export function addWlInternshipTraining(data) {
  return request({
    url: '/system/wlInternshipTraining',
    method: 'post',
    data: data
  })
}

// 修改G3教学实习实训明细
export function updateWlInternshipTraining(data) {
  return request({
    url: '/system/wlInternshipTraining',
    method: 'put',
    data: data
  })
}

// 删除G3教学实习实训明细
export function delWlInternshipTraining(itemId) {
  return request({
    url: '/system/wlInternshipTraining/' + itemId,
    method: 'delete'
  })
}

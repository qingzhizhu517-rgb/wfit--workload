import request from '@/utils/request'

export function getAdminStats(semester) {
  return request({
    url: '/system/dashboard/adminStats',
    method: 'get',
    params: { semester }
  })
}

export function getTeacherStats(semester) {
  return request({
    url: '/system/dashboard/teacherStats',
    method: 'get',
    params: { semester }
  })
}

export function getCollegeStats(semester) {
  return request({
    url: '/system/dashboard/collegeStats',
    method: 'get',
    params: { semester }
  })
}

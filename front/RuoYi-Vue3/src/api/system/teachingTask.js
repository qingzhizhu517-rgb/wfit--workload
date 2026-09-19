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

// Excel 导入教学任务
// templateType 省略或 'ALL' 走通用导入（放行 G1~G6）；G1/G2/G3 为分类导入（后端写库前锁定类别）
export function importTeachingTask(file, templateType) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/system/teachingTask/importExcel',
    method: 'post',
    // 必须显式指定 multipart，否则会命中 axios 全局默认的 application/json 头，
    // 浏览器不生成 boundary，后端 @RequestParam("file") 解析失败（MultipartException）
    headers: { 'Content-Type': 'multipart/form-data' },
    // templateType 作为查询参数透传；省略/ALL 时不带，保持向后兼容
    params: templateType && templateType !== 'ALL' ? { templateType } : {},
    data: formData
  })
}

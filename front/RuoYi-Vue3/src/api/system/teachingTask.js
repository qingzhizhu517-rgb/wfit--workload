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

// 两阶段导入 · 预检：仅解析「导入数据」页并逐行落暂存表，零业务写入。
// 返回 AjaxResult.data = ImportPreviewResponse
// （batchId / importType / templateVersion / totalCount / readyCount / blockedCount /
//   rows[rowNo,userCode,userName,status,errorCode,errorMessage] / warnings / confirmToken）
// templateVersion 省略时不带参数，后端默认 V2。
export function importPreviewTeachingTask(file, templateVersion) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/system/teachingTask/importPreview',
    method: 'post',
    // 必须显式指定 multipart，否则会命中 axios 全局默认的 application/json 头，
    // 浏览器不生成 boundary，后端 @RequestParam("file") 解析失败（MultipartException）
    headers: { 'Content-Type': 'multipart/form-data' },
    params: templateVersion ? { templateVersion } : {},
    data: formData
  })
}

// 两阶段导入 · 确认：校验令牌与 0 阻断后按 row_no 回放写库。
// data = { batchId, confirmToken, updateSupport }
// 返回 AjaxResult.data = ImportResult（successCount / failCount / skipCount / getTotalCount / errors / batchId）
export function importConfirmTeachingTask(data) {
  return request({
    url: '/system/teachingTask/importConfirm',
    method: 'post',
    data: data
  })
}

// 下载导入批次的错误行清单（BLOCKED/FAILED），仅 6 列白名单字段。
// 后端为 GET + HttpServletResponse 直接写字节流，需配合 saveBlobAsFile 落盘。
export function downloadImportErrors(batchId) {
  return request({
    url: '/system/importBatch/' + batchId + '/errors',
    method: 'get',
    responseType: 'blob'
  })
}

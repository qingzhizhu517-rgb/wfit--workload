/**
 * 二进制流下载（responseType: 'blob' 的接口专用）
 *
 * 后端报表导出走 HttpServletResponse 直接写字节流，不是 RuoYi 通用的
 * download() 通道，故需要自己拼 Blob 并触发下载。原先这段逻辑在
 * useDashboard.js 里抄了两遍，收敛到此处，学期汇总页新增的导出入口复用同一份。
 */
import { ElMessage } from 'element-plus'
import { blobValidate } from '@/utils/ruoyi'

const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

/**
 * @param {Blob|ArrayBuffer} data 接口返回的原始响应体
 * @param {string} fileName 保存的文件名（含扩展名）
 * @param {string} [mime] MIME 类型，默认 xlsx
 */
export async function saveBlobAsFile(data, fileName, mime = XLSX_MIME) {
  const blob = data instanceof Blob ? data : new Blob([data], { type: mime })
  const blobType = blob.type || ''
  // 后端错误体有两种：application/json（标准 AjaxResult）与 text/plain（如导出接口在
  // 「未找到该教师该学期的工作量明细」时直接 200 写纯文本）。二者都不是文件流，若不拦截
  // 会把提示语当成 .xlsx 存成损坏文件。text/plain 正文即人类可读提示，直接展示。
  const isTextPlain = blobType.includes('text/plain')
  if (!blobValidate(blob) || isTextPlain) {
    const text = await blob.text()
    let message = '导出失败'
    if (isTextPlain) {
      message = text.trim() || message
      ElMessage.warning(message)
    } else {
      try {
        const result = JSON.parse(text)
        message = result.msg || message
      } catch {
        // 后端未返回标准 JSON 时保留统一错误消息。
      }
      ElMessage.error(message)
    }
    throw new Error(message)
  }
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  // 及时释放，否则整页生命周期内 blob 都驻留内存
  window.URL.revokeObjectURL(url)
}

export default saveBlobAsFile

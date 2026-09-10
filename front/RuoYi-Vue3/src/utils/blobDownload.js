/**
 * 二进制流下载（responseType: 'blob' 的接口专用）
 *
 * 后端报表导出走 HttpServletResponse 直接写字节流，不是 RuoYi 通用的
 * download() 通道，故需要自己拼 Blob 并触发下载。原先这段逻辑在
 * useDashboard.js 里抄了两遍，收敛到此处，学期汇总页新增的导出入口复用同一份。
 */
const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

/**
 * @param {Blob|ArrayBuffer} data 接口返回的原始响应体
 * @param {string} fileName 保存的文件名（含扩展名）
 * @param {string} [mime] MIME 类型，默认 xlsx
 */
export function saveBlobAsFile(data, fileName, mime = XLSX_MIME) {
  const blob = new Blob([data], { type: mime })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  // 及时释放，否则整页生命周期内 blob 都驻留内存
  window.URL.revokeObjectURL(url)
}

export default saveBlobAsFile

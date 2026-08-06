import request from './request'

export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  // 不手动设置 Content-Type,让浏览器自动生成带 boundary 的 multipart 请求头
  return request.post('/api/file/upload', formData)
}

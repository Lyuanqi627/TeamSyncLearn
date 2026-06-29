import request from './request'

export function uploadAchievement(formData: FormData) {
  // 不手动设置 Content-Type，让浏览器自动生成带 boundary 的 multipart 请求头
  return request.post('/api/achievement/upload', formData)
}

export function getAchievementByScheduleId(scheduleId: number) {
  return request.get(`/api/achievement/bySchedule/${scheduleId}`)
}

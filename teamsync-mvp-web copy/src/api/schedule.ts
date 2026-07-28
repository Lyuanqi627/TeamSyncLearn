import request from './request'

export function getSchedules(params?: { userId?: number; date?: string }) {
  return request.get('/api/schedule/list', { params })
}

export function createSchedule(data: { title: string; goalDesc?: string; planDate: string }) {
  return request.post('/api/schedule', data)
}

export function updateSchedule(id: number, data: { title: string; goalDesc?: string; planDate: string }) {
  return request.put(`/api/schedule/${id}`, data)
}

export function deleteSchedule(id: number) {
  return request.delete(`/api/schedule/${id}`)
}

export function updateScheduleStatus(id: number, status: number) {
  return request.put(`/api/schedule/${id}/status`, null, { params: { status } })
}

export function getDashboard(userId?: number) {
  return request.get('/api/schedule/dashboard', { params: { userId } })
}

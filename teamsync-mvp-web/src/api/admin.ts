import request from './request'

export function getMembers() {
  return request.get('/api/admin/members')
}

export function getTeamBoard() {
  return request.get('/api/admin/teamboard')
}

export function getWordCloud(params?: { startDate?: string; endDate?: string }) {
  return request.get('/api/admin/wordcloud', { params })
}

export function getMemberDetail(userId: number) {
  return request.get(`/api/admin/member/${userId}`)
}

import request from './request'

export function getAiResult(achievementId: number) {
  return request.get(`/api/ai/result/${achievementId}`)
}

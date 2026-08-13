import request from './request'

export function login(data: { username: string; password?: string }) {
  return request.post('/api/user/login', data)
}

export function register(data: { username: string; password?: string }) {
  return request.post('/api/user/register', data)
}

export function getUserInfo() {
  return request.get('/api/user/info')
}

export function updateProfile(data: any) {
  return request.put('/api/user/profile', data)
}

export function logout() {
  return request.post('/api/user/logout')
}

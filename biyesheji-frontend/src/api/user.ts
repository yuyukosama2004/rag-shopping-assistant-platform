import request from './request'

export function login(username: string, password: string) {
  return request.post('/api/user/login', { username, password })
}

export function register(data: { username: string; password: string; nickname?: string; phone?: string; email?: string }) {
  return request.post('/api/user/register', data)
}

export function getUserInfo() {
  return request.get('/api/user/info')
}

export function updateUserInfo(data: any) {
  return request.put('/api/user/info', data)
}

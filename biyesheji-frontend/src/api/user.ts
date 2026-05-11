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

// 地址管理
export function getAddressList() {
  return request.get('/api/user/address')
}
export function addAddress(data: any) {
  return request.post('/api/user/address', data)
}
export function updateAddress(id: number, data: any) {
  return request.put('/api/user/address/' + id, data)
}
export function deleteAddress(id: number) {
  return request.delete('/api/user/address/' + id)
}
export function setDefaultAddress(id: number) {
  return request.put('/api/user/address/' + id + '/default')
}

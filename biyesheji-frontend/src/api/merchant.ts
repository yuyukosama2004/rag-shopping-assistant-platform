import request from './request'

export interface StoreSetting {
  storeName: string
  logo?: string
  servicePhone?: string
  serviceEmail?: string
  address?: string
  businessStatus?: number
  shippingNotice?: string
  afterSalesNotice?: string
}

export interface Staff {
  id: number
  username: string
  nickname?: string
  phone?: string
  email?: string
  status: number
}

export function getPublicStoreSetting() {
  return request.get('/api/store/setting')
}

export function getMerchantStoreSetting() {
  return request.get('/api/merchant/store/setting')
}

export function updateMerchantStoreSetting(data: StoreSetting) {
  return request.put('/api/merchant/store/setting', data)
}

export function getStaffList() { return request.get('/api/merchant/staff') }
export function createStaff(data: Omit<Staff, 'id' | 'status'> & { password: string }) { return request.post('/api/merchant/staff', data) }
export function updateStaffStatus(id: number, status: number) { return request.put(`/api/merchant/staff/${id}/status`, { status }) }

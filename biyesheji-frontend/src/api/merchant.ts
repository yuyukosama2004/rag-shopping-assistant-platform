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

export interface MerchantProduct {
  id: number
  name: string
  brand: string
  category: string
  price: number
  originalPrice?: number
  mainImage?: string
  description?: string
  status: number
}

export interface MerchantProductInput {
  name: string
  brand: string
  category: string
  price: number | null
  originalPrice?: number | null
  mainImage?: string
  description?: string
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
export function getMerchantProducts(pageNum = 1, pageSize = 20, keyword = '') { return request.get('/api/merchant/products', { params: { pageNum, pageSize, keyword: keyword || undefined } }) }
export function createMerchantProduct(data: MerchantProductInput) { return request.post('/api/merchant/products', data) }
export function updateMerchantProduct(id: number, data: MerchantProductInput) { return request.put(`/api/merchant/products/${id}`, data) }
export function updateMerchantProductStatus(id: number, status: number) { return request.put(`/api/merchant/products/${id}/status`, { status }) }

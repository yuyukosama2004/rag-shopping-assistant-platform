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

export interface MerchantSku {
  id: number
  skuCode: string
  specJson?: string
  price: number
  originalPrice?: number
  status: number
}

export interface MerchantSkuInput {
  skuCode: string
  specJson?: string
  price: number | null
  originalPrice?: number | null
  initialStock: number | null
}

export interface MerchantSkuUpdateInput {
  skuCode: string
  specJson?: string
  price: number | null
  originalPrice?: number | null
  status: number
}

export interface MerchantSkuStock {
  id: number
  productId: number
  skuId: number
  total: number
  locked: number
  available: number
  version: number
}

export interface MerchantStockLedger {
  id: number
  skuId: number
  action: string
  quantity: number
  beforeAvailable: number
  afterAvailable: number
  operatorId?: number
  referenceNo?: string
  createTime?: string
}

export interface MerchantOrder {
  id: number
  orderNo: string
  totalAmount: number
  status: number
  statusDesc: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  shippingCarrier?: string
  trackingNo?: string
  createdAt: string
  items: Array<{ id: number; productName: string; skuCode?: string; skuSpecJson?: string; price: number; quantity: number; subtotal: number }>
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
export function resetStaffPassword(id: number, newPassword: string) { return request.put(`/api/merchant/staff/${id}/password`, { newPassword }) }
export function getMerchantProducts(pageNum = 1, pageSize = 20, keyword = '') { return request.get('/api/merchant/products', { params: { pageNum, pageSize, keyword: keyword || undefined } }) }
export function createMerchantProduct(data: MerchantProductInput) { return request.post('/api/merchant/products', data) }
export function updateMerchantProduct(id: number, data: MerchantProductInput) { return request.put(`/api/merchant/products/${id}`, data) }
export function updateMerchantProductStatus(id: number, status: number) { return request.put(`/api/merchant/products/${id}/status`, { status }) }
export function copyMerchantProduct(id: number) { return request.post(`/api/merchant/products/${id}/copy`) }
export function deleteMerchantProduct(id: number) { return request.delete(`/api/merchant/products/${id}`) }
export function updateMerchantProductBatchStatus(ids: number[], status: number) { return request.put('/api/merchant/products/batch-status', { ids, status }) }
export function uploadMerchantMedia(file: File) { const data = new FormData(); data.append('file', file); return request.post('/api/merchant/media', data) }
export function getMerchantSkus(productId: number) { return request.get(`/api/merchant/products/${productId}/skus`) }
export function createMerchantSku(productId: number, data: MerchantSkuInput) { return request.post(`/api/merchant/products/${productId}/skus`, data) }
export function updateMerchantSku(skuId: number, data: MerchantSkuUpdateInput) { return request.put(`/api/merchant/products/skus/${skuId}`, data) }
export function getMerchantSkuStock(skuId: number) { return request.get(`/api/merchant/products/skus/${skuId}/stock`) }
export function adjustMerchantSkuStock(skuId: number, quantity: number, reason: string) { return request.put(`/api/merchant/products/skus/${skuId}/stock`, { quantity, reason }) }
export function getMerchantSkuStockLedger(skuId: number) { return request.get(`/api/merchant/products/skus/${skuId}/stock/ledger`) }
export function getMerchantOrders(pageNum = 1, pageSize = 20, status?: number) { return request.get('/api/merchant/orders', { params: { pageNum, pageSize, status } }) }
export function confirmMerchantOrderPayment(orderNo: string) { return request.post(`/api/merchant/orders/${orderNo}/confirm-payment`) }
export function shipMerchantOrder(orderNo: string, data: { carrier: string; trackingNo: string; note?: string }) { return request.post(`/api/merchant/orders/${orderNo}/ship`, data) }

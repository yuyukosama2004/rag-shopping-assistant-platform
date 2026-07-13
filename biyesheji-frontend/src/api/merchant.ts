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

export function getPublicStoreSetting() {
  return request.get('/api/store/setting')
}

export function getMerchantStoreSetting() {
  return request.get('/api/merchant/store/setting')
}

export function updateMerchantStoreSetting(data: StoreSetting) {
  return request.put('/api/merchant/store/setting', data)
}

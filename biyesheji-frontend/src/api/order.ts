import request from './request'

export interface ShippingRule {
  id: number
  ruleType: 'DELIVERY' | 'PICKUP'
  name: string
  baseFee: number
  freeShippingThreshold?: number
}

export function addToCart(productId: number, skuId: number, quantity = 1) {
  return request.post('/api/order/cart', { productId, skuId, quantity })
}

export function updateCartOptions(cartId: number, color: string, storage: string) {
  return request.put(`/api/order/cart/${cartId}/options`, { color, storage })
}

export function getCartList() {
  return request.get('/api/order/cart')
}

export function updateCartQuantity(cartId: number, quantity: number) {
  return request.put(`/api/order/cart/${cartId}`, { quantity })
}

export function removeCartItem(cartId: number) {
  return request.delete(`/api/order/cart/${cartId}`)
}

export function removeCartBatch(ids: number[]) {
  return request.delete('/api/order/cart/batch', { data: { ids } })
}

export function toggleCartCheck(cartId: number) {
  return request.put(`/api/order/cart/${cartId}/check`)
}

export function checkAllCart(checked: boolean) {
  return request.put('/api/order/cart/check-all', { checked })
}

export function getCartCount() {
  return request.get('/api/order/cart/count')
}

export function submitOrder(data: unknown) {
  return request.post('/api/order/submit', data)
}

export function getShippingRules() {
  return request.get('/api/shipping-rules')
}

export function getOrderPage(params: unknown) {
  return request.get('/api/order/page', { params })
}

export function getOrderDetail(orderNo: string) {
  return request.get(`/api/order/${orderNo}`)
}

export function cancelOrder(orderNo: string) {
  return request.post(`/api/order/${orderNo}/cancel`)
}

export function completeOrder(orderNo: string) {
  return request.post(`/api/order/${orderNo}/complete`)
}

export function getRefunds(orderNo: string) {
  return request.get(`/api/order/${orderNo}/refunds`)
}

export function requestRefund(orderNo: string, data: { amount: number; reason: string }) {
  return request.post(`/api/order/${orderNo}/refunds`, data)
}

import request from './request'

// 购物车
export function addToCart(productId: number, quantity = 1, color?: string, storage?: string) {
  return request.post('/api/order/cart', { productId, quantity, color, storage })
}

export function updateCartOptions(cartId: number, color: string, storage: string) {
  return request.put('/api/order/cart/' + cartId + '/options', { color, storage })
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

// 订单
export function submitOrder(data: any) {
  return request.post('/api/order/submit', data)
}

export function getOrderPage(params: any) {
  return request.get('/api/order/page', { params })
}

export function getOrderDetail(orderNo: string) {
  return request.get(`/api/order/${orderNo}`)
}

export function payOrder(orderNo: string) {
  return request.post(`/api/order/${orderNo}/pay`)
}

export function cancelOrder(orderNo: string) {
  return request.post(`/api/order/${orderNo}/cancel`)
}

export function createTestOrder(productId: number, quantity = 1) {
  return request.post('/api/order/test-create', { productId, quantity })
}

// AI 导购
export function aiChat(query: string): EventSource {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  const url = `${baseUrl}/api/order/ai/chat?query=${encodeURIComponent(query)}`
  return new EventSource(url)
}

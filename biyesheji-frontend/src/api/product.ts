import request from './request'

export function getProductPage(params: any) {
  return request.get('/api/product/page', { params })
}

export function getProductDetail(id: string | number) {
  return request.get(`/api/product/${id}`)
}

export function getProductSkus(id: string | number) {
  return request.get(`/api/product/${id}/skus`)
}

export function getHotProducts(limit = 8) {
  return request.get('/api/product/hot', { params: { limit } })
}

export function getFilters() {
  return request.get('/api/product/filters')
}

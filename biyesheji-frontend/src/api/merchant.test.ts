import { beforeEach, describe, expect, it, vi } from 'vitest'

const request = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}))

vi.mock('./request', () => ({ default: request }))

import {
  getMerchantProducts,
  shipMerchantOrder,
  updateMerchantProductStatus,
} from './merchant'

describe('merchant API client', () => {
  beforeEach(() => vi.clearAllMocks())

  it('omits an empty product keyword', () => {
    getMerchantProducts(2, 30, '')
    expect(request.get).toHaveBeenCalledWith('/api/merchant/products', {
      params: { pageNum: 2, pageSize: 30, keyword: undefined },
    })
  })

  it('publishes a product through the status endpoint', () => {
    updateMerchantProductStatus(42, 1)
    expect(request.put).toHaveBeenCalledWith('/api/merchant/products/42/status', { status: 1 })
  })

  it('sends shipment tracking through the order endpoint', () => {
    const shipment = { carrier: '顺丰速运', trackingNo: 'SF-E2E-001', note: '自动化验收' }
    shipMerchantOrder('ORDER-42', shipment)
    expect(request.post).toHaveBeenCalledWith('/api/merchant/orders/ORDER-42/ship', shipment)
  })
})

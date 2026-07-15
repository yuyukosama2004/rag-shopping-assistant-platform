import { describe, expect, it, vi } from 'vitest'

const request = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('./request', () => ({ default: request }))

import { getProductDetail, getProductSkus } from './product'

describe('product API client', () => {
  it('preserves 64-bit Snowflake identifiers as strings', () => {
    const id = '2077264325434212354'

    getProductDetail(id)
    getProductSkus(id)

    expect(request.get).toHaveBeenNthCalledWith(1, `/api/product/${id}`)
    expect(request.get).toHaveBeenNthCalledWith(2, `/api/product/${id}/skus`)
  })
})

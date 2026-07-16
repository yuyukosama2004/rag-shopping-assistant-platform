import { expect, test, type APIRequestContext, type APIResponse } from '@playwright/test'
import { createHmac, randomUUID } from 'node:crypto'

const apiBaseUrl = process.env.E2E_API_URL || 'http://127.0.0.1:18080'
const ownerInitToken = process.env.E2E_OWNER_INIT_TOKEN || 'ci-owner-initialization-token'
const jwtSecret = process.env.E2E_JWT_SECRET || 'ci-jwt-secret-that-is-at-least-thirty-two-bytes'

type ApiEnvelope<T> = { code: number; message: string; data: T }

async function envelope<T>(response: APIResponse): Promise<ApiEnvelope<T>> {
  const body = await response.json() as ApiEnvelope<T>
  expect(response.ok(), `${response.status()} ${response.url()}: ${JSON.stringify(body)}`).toBeTruthy()
  expect(body.code, body.message).toBe(200)
  return body
}

async function login(request: APIRequestContext, username: string, password: string) {
  const response = await request.post(`${apiBaseUrl}/api/user/login`, { data: { username, password } })
  return (await envelope<{ accessToken: string; refreshToken: string; userId: number }>(response)).data
}

function customerAccessToken(userId: number) {
  const encode = (value: object) => Buffer.from(JSON.stringify(value)).toString('base64url')
  const now = Math.floor(Date.now() / 1000)
  const unsigned = `${encode({ alg: 'HS256', typ: 'JWT' })}.${encode({
    sub: `concurrent_${userId}`,
    userId,
    role: 0,
    tokenType: 'access',
    jti: randomUUID(),
    iat: now,
    exp: now + 600,
  })}`
  return `${unsigned}.${createHmac('sha256', jwtSecret).update(unsigned).digest('base64url')}`
}

test('merchant publishes a product, customer orders it, and merchant ships it', async ({ request, page }) => {
  const suffix = Date.now().toString(36)
  const ownerUsername = `owner_${suffix}`
  const customerUsername = `customer_${suffix}`
  const password = 'E2e-Strong-Password-2026!'

  await envelope(await request.post(`${apiBaseUrl}/api/merchant/initialize`, {
    headers: { 'X-Owner-Init-Token': ownerInitToken },
    data: { username: ownerUsername, password, nickname: '验收店主', storeName: '全栈验收店铺' },
  }))
  const owner = await login(request, ownerUsername, password)
  const ownerHeaders = { Authorization: `Bearer ${owner.accessToken}` }

  const product = (await envelope<{ id: number }>(await request.post(`${apiBaseUrl}/api/merchant/products`, {
    headers: ownerHeaders,
    data: {
      name: `Playwright 验收商品 ${suffix}`,
      brand: 'E2E',
      category: '自动化测试',
      price: 199.00,
      originalPrice: 299.00,
      description: '由全栈端到端测试创建',
    },
  }))).data
  const sku = (await envelope<{ id: number }>(await request.post(`${apiBaseUrl}/api/merchant/products/${product.id}/skus`, {
    headers: ownerHeaders,
    data: { skuCode: `E2E-${suffix}`, specJson: '{"版本":"标准版"}', price: 199.00, initialStock: 10 },
  }))).data
  await envelope(await request.put(`${apiBaseUrl}/api/merchant/products/${product.id}/status`, {
    headers: ownerHeaders,
    data: { status: 1 },
  }))
  const shippingRule = (await envelope<{ id: number }>(await request.post(`${apiBaseUrl}/api/merchant/shipping-rules`, {
    headers: ownerHeaders,
    data: { ruleType: 'DELIVERY', name: 'E2E 标准配送', baseFee: 8.00, status: 1, sortOrder: 1 },
  }))).data

  await envelope(await request.post(`${apiBaseUrl}/api/user/register`, {
    data: { username: customerUsername, password, nickname: '验收消费者' },
  }))
  const customer = await login(request, customerUsername, password)
  const customerHeaders = { Authorization: `Bearer ${customer.accessToken}` }

  const unauthenticated = await request.get(`${apiBaseUrl}/api/merchant/products`)
  expect(unauthenticated.status()).toBe(401)
  const spoofedRole = await request.get(`${apiBaseUrl}/api/merchant/products`, {
    headers: { ...customerHeaders, 'X-User-Role': '1', 'X-User-Id': String(owner.userId) },
  })
  expect(spoofedRole.status()).toBe(403)
  const refreshAsAccess = await request.get(`${apiBaseUrl}/api/merchant/products`, {
    headers: { Authorization: `Bearer ${customer.refreshToken}` },
  })
  expect(refreshAsAccess.status()).toBe(401)

  const submit = (await envelope<{ orderNo: string }>(await request.post(`${apiBaseUrl}/api/order/submit`, {
    headers: customerHeaders,
    data: {
      items: [{ productId: product.id, skuId: sku.id, quantity: 2 }],
      receiverName: '端到端收件人',
      receiverPhone: '13800000000',
      receiverAddress: '测试省测试市测试区 1 号',
      paymentMethod: 'COD',
      shippingRuleId: shippingRule.id,
    },
  }))).data

  await envelope(await request.put(`${apiBaseUrl}/api/merchant/orders/${submit.orderNo}/note`, {
    headers: ownerHeaders,
    data: { note: 'Playwright 商家内部备注' },
  }))
  const merchantDetail = (await envelope<{
    order: { orderNo: string }
    merchantNote: string
    operations: Array<{ action: string }>
  }>(await request.get(`${apiBaseUrl}/api/merchant/orders/${submit.orderNo}`, { headers: ownerHeaders }))).data
  expect(merchantDetail.order.orderNo).toBe(submit.orderNo)
  expect(merchantDetail.merchantNote).toBe('Playwright 商家内部备注')
  expect(merchantDetail.operations.some(operation => operation.action === 'MERCHANT_NOTE')).toBeTruthy()

  await envelope(await request.post(`${apiBaseUrl}/api/merchant/orders/${submit.orderNo}/accept`, { headers: ownerHeaders }))
  const duplicateAccept = await request.post(`${apiBaseUrl}/api/merchant/orders/${submit.orderNo}/accept`, { headers: ownerHeaders })
  expect(duplicateAccept.status()).toBe(400)
  const trackingNo = `SF-${suffix}`
  await envelope(await request.post(`${apiBaseUrl}/api/merchant/orders/${submit.orderNo}/ship`, {
    headers: ownerHeaders,
    data: { carrier: '顺丰速运', trackingNo, note: 'Playwright 自动发货' },
  }))
  const duplicateShipment = await request.post(`${apiBaseUrl}/api/merchant/orders/${submit.orderNo}/ship`, {
    headers: ownerHeaders,
    data: { carrier: '顺丰速运', trackingNo: `${trackingNo}-DUPLICATE` },
  })
  expect(duplicateShipment.status()).toBe(400)
  const closeShippedOrder = await request.post(`${apiBaseUrl}/api/merchant/orders/${submit.orderNo}/close`, {
    headers: ownerHeaders,
    data: { reason: '不应允许关闭已发货订单' },
  })
  expect(closeShippedOrder.status()).toBe(400)

  const closableOrder = (await envelope<{ orderNo: string }>(await request.post(`${apiBaseUrl}/api/order/submit`, {
    headers: { Authorization: `Bearer ${customerAccessToken(2_900_000_000)}` },
    data: {
      items: [{ productId: product.id, skuId: sku.id, quantity: 1 }],
      receiverName: '关单库存验收',
      receiverPhone: '13800000000',
      receiverAddress: '关单测试地址 1 号',
      paymentMethod: 'COD',
      shippingRuleId: shippingRule.id,
    },
  }))).data
  await envelope(await request.post(`${apiBaseUrl}/api/merchant/orders/${closableOrder.orderNo}/close`, {
    headers: ownerHeaders,
    data: { reason: 'Playwright 验证释放预占库存' },
  }))
  const closedDetail = (await envelope<{
    order: { status: number }
    operations: Array<{ action: string }>
  }>(await request.get(`${apiBaseUrl}/api/merchant/orders/${closableOrder.orderNo}`, { headers: ownerHeaders }))).data
  expect(closedDetail.order.status).toBe(4)
  expect(closedDetail.operations.some(operation => operation.action === 'MERCHANT_CLOSE')).toBeTruthy()
  const originalSkuStock = (await envelope<{ total: number; locked: number; available: number }>(
    await request.get(`${apiBaseUrl}/api/merchant/products/skus/${sku.id}/stock`, { headers: ownerHeaders }),
  )).data
  expect(originalSkuStock).toMatchObject({ total: 10, locked: 2, available: 8 })

  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/merchant/login')
  await page.evaluate(({ accessToken, refreshToken, userId, username }) => {
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    localStorage.setItem('userInfo', JSON.stringify({ id: userId, username, nickname: '验收店主', role: 1 }))
  }, { accessToken: owner.accessToken, refreshToken: owner.refreshToken, userId: owner.userId, username: ownerUsername })
  await page.goto('/merchant/inventory')
  await expect(page.locator('.toolbar strong', { hasText: '库存管理' })).toBeVisible()
  await expect(page.locator('.merchant-menu')).toBeVisible()
  const mobileViewport = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }))
  expect(mobileViewport.scrollWidth).toBeLessThanOrEqual(mobileViewport.clientWidth)
  await page.setViewportSize({ width: 1280, height: 720 })

  const detail = (await envelope<{ status: number; shippingCarrier: string; trackingNo: string }>(
    await request.get(`${apiBaseUrl}/api/order/${submit.orderNo}`, { headers: customerHeaders }),
  )).data
  expect(detail).toMatchObject({ status: 2, shippingCarrier: '顺丰速运', trackingNo })

  await page.addInitScript(({ accessToken, refreshToken, userId, username }) => {
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    localStorage.setItem('userInfo', JSON.stringify({ id: userId, username, nickname: '验收消费者', role: 0 }))
  }, { accessToken: customer.accessToken, refreshToken: customer.refreshToken, userId: customer.userId, username: customerUsername })
  await page.goto(`/order/${submit.orderNo}`)
  await expect(page.getByText(submit.orderNo)).toBeVisible()
  await expect(page.getByText(trackingNo, { exact: false })).toBeVisible()

  const xssName = `<img src=x onerror="window.__biyeshejiXss=1"> ${suffix}`
  const xssDescription = `<script>window.__biyeshejiXss=1</script>`
  const xssProduct = (await envelope<{ id: number }>(await request.post(`${apiBaseUrl}/api/merchant/products`, {
    headers: ownerHeaders,
    data: { name: xssName, brand: 'E2E', category: '安全测试', price: 1.00, description: xssDescription },
  }))).data
  const xssSku = (await envelope<{ id: number }>(await request.post(`${apiBaseUrl}/api/merchant/products/${xssProduct.id}/skus`, {
    headers: ownerHeaders,
    data: { skuCode: `XSS-${suffix}`, price: 1.00, initialStock: 1 },
  }))).data
  expect(String(xssSku.id)).not.toBe('')
  await envelope(await request.put(`${apiBaseUrl}/api/merchant/products/${xssProduct.id}/status`, {
    headers: ownerHeaders,
    data: { status: 1 },
  }))
  const inventorySummary = (await envelope<{ lowStockCount: number; threshold: number }>(
    await request.get(`${apiBaseUrl}/api/merchant/inventory/summary`, { headers: ownerHeaders }),
  )).data
  expect(inventorySummary.threshold).toBe(5)
  expect(inventorySummary.lowStockCount).toBeGreaterThanOrEqual(1)
  const lowStockInventory = (await envelope<{ records: Array<{ skuId: number; available: number }> }>(
    await request.get(`${apiBaseUrl}/api/merchant/inventory`, {
      headers: ownerHeaders,
      params: { keyword: `XSS-${suffix}`, lowStockOnly: true },
    }),
  )).data
  expect(lowStockInventory.records).toContainEqual(expect.objectContaining({ skuId: xssSku.id, available: 1 }))
  await page.goto(`/product/${xssProduct.id}`)
  await expect(page.getByText(xssName)).toBeVisible()
  await expect(page.getByText(xssDescription)).toBeVisible()
  expect(await page.locator('img[src="x"]').count()).toBe(0)
  expect(await page.evaluate(() => (window as typeof window & { __biyeshejiXss?: number }).__biyeshejiXss)).toBeUndefined()

  await envelope(await request.put(`${apiBaseUrl}/api/merchant/products/skus/${xssSku.id}/stock`, {
    headers: ownerHeaders,
    data: { quantity: -1, reason: 'Playwright 验证售罄状态' },
  }))
  await page.reload()
  await expect(page.getByText('缺货', { exact: false })).toBeVisible()
  await expect(page.getByRole('button', { name: '加入购物车' })).toBeDisabled()

  await page.route('**/api/product/page**', route => route.abort())
  await page.goto(`/products?keyword=network-${suffix}`)
  await expect(page.locator('.el-alert__title').filter({ hasText: '网络连接失败，请检查网络后重试' })).toBeVisible()
  await page.unroute('**/api/product/page**')
  await page.goto(`/products?keyword=missing-${suffix}`)
  await expect(page.getByText(`没有找到“missing-${suffix}”相关商品`)).toBeVisible()

  const maliciousUpload = await request.post(`${apiBaseUrl}/api/merchant/media`, {
    headers: ownerHeaders,
    multipart: {
      file: { name: 'attack.png', mimeType: 'image/png', buffer: Buffer.from('<script>alert(1)</script>') },
    },
  })
  expect(maliciousUpload.status()).toBe(400)

  const lockedUsername = `locked_${suffix}`
  await envelope(await request.post(`${apiBaseUrl}/api/user/register`, {
    data: { username: lockedUsername, password, nickname: '锁定测试用户' },
  }))
  for (let attempt = 0; attempt < 5; attempt += 1) {
    const failedLogin = await request.post(`${apiBaseUrl}/api/user/login`, {
      data: { username: lockedUsername, password: 'Definitely-Wrong-Password!' },
    })
    expect(failedLogin.status()).toBe(400)
    expect((await failedLogin.json() as ApiEnvelope<unknown>).code).toBe(1003)
  }
  const lockedLogin = await request.post(`${apiBaseUrl}/api/user/login`, {
    data: { username: lockedUsername, password },
  })
  expect(lockedLogin.status()).toBe(403)

  const concurrencyProduct = (await envelope<{ id: number }>(await request.post(`${apiBaseUrl}/api/merchant/products`, {
    headers: ownerHeaders,
    data: {
      name: `并发库存验收商品 ${suffix}`,
      brand: 'E2E',
      category: '并发测试',
      price: 10.00,
      description: '库存 100，接受超过 100 个并发购买请求',
    },
  }))).data
  const concurrencySku = (await envelope<{ id: number }>(await request.post(`${apiBaseUrl}/api/merchant/products/${concurrencyProduct.id}/skus`, {
    headers: ownerHeaders,
    data: { skuCode: `CONCURRENT-${suffix}`, price: 10.00, initialStock: 100 },
  }))).data
  await envelope(await request.put(`${apiBaseUrl}/api/merchant/products/${concurrencyProduct.id}/status`, {
    headers: ownerHeaders,
    data: { status: 1 },
  }))

  const attempts = await Promise.all(Array.from({ length: 110 }, async (_, index) => {
    const token = customerAccessToken(3_000_000_000 + index)
    const response = await request.post(`${apiBaseUrl}/api/order/submit`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        items: [{ productId: concurrencyProduct.id, skuId: concurrencySku.id, quantity: 1 }],
        receiverName: `并发消费者 ${index}`,
        receiverPhone: '13800000000',
        receiverAddress: '并发测试地址 1 号',
        paymentMethod: 'COD',
        shippingRuleId: shippingRule.id,
      },
    })
    return { status: response.status(), body: await response.json() as ApiEnvelope<unknown> }
  }))
  const successfulOrders = attempts.filter(({ status, body }) => status === 200 && body.code === 200)
  const failedOrders = attempts.filter(({ status, body }) => status !== 200 || body.code !== 200)
  expect(successfulOrders.length).toBeGreaterThan(0)
  expect(successfulOrders.length).toBeLessThanOrEqual(100)
  expect(failedOrders.every(({ status }) => status === 409 || status === 429)).toBeTruthy()

  const stock = (await envelope<{ total: number; locked: number; available: number }>(
    await request.get(`${apiBaseUrl}/api/merchant/products/skus/${concurrencySku.id}/stock`, { headers: ownerHeaders }),
  )).data
  expect(stock.total).toBe(100)
  expect(stock.available).toBeGreaterThanOrEqual(0)
  expect(stock.locked).toBeGreaterThanOrEqual(0)
  expect(stock.available + stock.locked).toBe(100)
  expect(stock.locked).toBe(successfulOrders.length)
})

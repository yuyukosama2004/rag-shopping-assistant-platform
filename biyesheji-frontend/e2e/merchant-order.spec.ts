import { expect, test, type APIRequestContext, type APIResponse } from '@playwright/test'

const apiBaseUrl = process.env.E2E_API_URL || 'http://127.0.0.1:18080'
const ownerInitToken = process.env.E2E_OWNER_INIT_TOKEN || 'ci-owner-initialization-token'

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

  await envelope(await request.post(`${apiBaseUrl}/api/merchant/orders/${submit.orderNo}/accept`, { headers: ownerHeaders }))
  const trackingNo = `SF-${suffix}`
  await envelope(await request.post(`${apiBaseUrl}/api/merchant/orders/${submit.orderNo}/ship`, {
    headers: ownerHeaders,
    data: { carrier: '顺丰速运', trackingNo, note: 'Playwright 自动发货' },
  }))

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
})

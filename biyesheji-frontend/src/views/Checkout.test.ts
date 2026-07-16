import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import Checkout from './Checkout.vue'

const api = vi.hoisted(() => ({
  getCartList: vi.fn(),
  getAddressList: vi.fn(),
  getShippingRules: vi.fn(),
  submitOrder: vi.fn(),
  removeCartBatch: vi.fn(),
}))
const replace = vi.hoisted(() => vi.fn())

vi.mock('../api/order', () => ({
  getCartList: api.getCartList,
  getShippingRules: api.getShippingRules,
  submitOrder: api.submitOrder,
  removeCartBatch: api.removeCartBatch,
}))
vi.mock('../api/user', () => ({ getAddressList: api.getAddressList }))
vi.mock('../api/request', () => ({ getErrorMessage: (_error: unknown, fallback: string) => fallback }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn(), replace }) }))
vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn(), success: vi.fn() } }))

describe('Checkout', () => {
  it('submits only once while the first order request is pending', async () => {
    api.getCartList.mockResolvedValue({ data: { data: [{ id: 1, productId: 10, skuId: 11, checked: 1, quantity: 1, productPrice: 99, productName: '测试商品' }] } })
    api.getAddressList.mockResolvedValue({ data: { data: [{ id: 2, isDefault: 1, receiverName: '收件人', receiverPhone: '13800000000', detail: '测试地址' }] } })
    api.getShippingRules.mockResolvedValue({ data: { data: [{ id: 3, ruleType: 'DELIVERY', name: '标准配送', baseFee: 0 }] } })
    let resolveOrder!: (value: unknown) => void
    api.submitOrder.mockImplementation(() => new Promise(resolve => { resolveOrder = resolve }))
    api.removeCartBatch.mockResolvedValue(undefined)

    const wrapper = mount(Checkout, {
      global: {
        directives: { loading: () => undefined },
        stubs: {
          'el-button': { props: ['loading', 'disabled'], emits: ['click'], template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>' },
          'el-input': { template: '<input />' },
          'el-radio-group': { template: '<div><slot /></div>' },
          'el-radio': { template: '<span><slot /></span>' },
          'el-empty': { template: '<div><slot /></div>' },
          'el-alert': { template: '<div><slot /></div>' },
        },
      },
    })
    await flushPromises()

    const submit = wrapper.findAll('button').find(button => button.text() === '提交订单')!
    await submit.trigger('click')
    await submit.trigger('click')

    expect(api.submitOrder).toHaveBeenCalledTimes(1)
    resolveOrder({ data: { data: { orderNo: 'ORDER-1' } } })
    await flushPromises()
    expect(replace).toHaveBeenCalledWith('/order/ORDER-1')
  })
})

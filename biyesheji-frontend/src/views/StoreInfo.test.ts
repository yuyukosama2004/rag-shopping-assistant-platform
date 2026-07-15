import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import StoreInfo from './StoreInfo.vue'

const getPublicStoreSetting = vi.hoisted(() => vi.fn())
vi.mock('../api/merchant', () => ({ getPublicStoreSetting }))

describe('StoreInfo', () => {
  it('renders merchant contact and policy data returned by the API', async () => {
    getPublicStoreSetting.mockResolvedValue({
      data: {
        data: {
          storeName: '验收店铺',
          servicePhone: '400-123-4567',
          serviceEmail: 'service@example.test',
          shippingNotice: '工作日发货',
          afterSalesNotice: '七日内联系商家',
        },
      },
    })

    const wrapper = mount(StoreInfo, {
      global: {
        stubs: {
          'el-card': { template: '<section><slot /></section>' },
          'el-divider': { template: '<hr />' },
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('400-123-4567')
    expect(wrapper.text()).toContain('service@example.test')
    expect(wrapper.text()).toContain('工作日发货')
    expect(wrapper.text()).toContain('七日内联系商家')
  })
})

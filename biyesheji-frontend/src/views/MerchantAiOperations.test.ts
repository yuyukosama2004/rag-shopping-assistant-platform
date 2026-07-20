import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MerchantAiOperations from './MerchantAiOperations.vue'

const api = vi.hoisted(() => ({
  getMerchantInventoryInsights: vi.fn(),
  getMerchantInventoryInsightSummary: vi.fn(),
  getMerchantInventoryInsightEvidence: vi.fn(),
}))
const router = vi.hoisted(() => ({ push: vi.fn() }))
const requestHelpers = vi.hoisted(() => {
  class MockApiRequestError extends Error {
    status: number

    constructor(message: string, status = 0) {
      super(message)
      this.status = status
    }
  }
  return {
    ApiRequestError: MockApiRequestError,
    getErrorMessage: vi.fn((error: unknown, fallback = '请求失败') => error instanceof Error ? error.message : fallback),
  }
})

vi.mock('../api/merchant', () => api)
vi.mock('vue-router', () => ({ useRouter: () => router }))
vi.mock('../api/request', () => requestHelpers)

const item = {
  productId: '9007199254740993',
  productName: '测试手机',
  skuId: '9223372036854775806',
  skuCode: 'PHONE-BLUE-256',
  skuSpecJson: '{"颜色":"蓝色","容量":"256GB"}',
  productStatus: 1,
  skuStatus: 1,
  total: 42,
  locked: 2,
  available: 40,
  confirmedQty7d: 0,
  confirmedQty30d: 0,
  confirmedQty90d: 4,
  demandQty30d: 3,
  confirmedRevenue30d: 5998,
  dailyVelocity30d: 0,
  daysOfCover: null,
  lastConfirmedSaleAt: '2026-06-20T10:20:00',
  daysSinceLastSale: 30,
  skuAgeDays: 180,
  sellThrough90d: 0.0909,
  risk: 'SLOW_MOVING',
  ruleCode: 'NO_CONFIRMED_SALES_30D_BUT_SALES_90D',
  calculatedAt: '2026-07-20T12:00:00+08:00',
}

const pageResponse = {
  data: {
    data: {
      records: [item],
      total: 1,
      pageNum: 1,
      pageSize: 20,
      pages: 1,
    },
  },
}
const summaryResponse = {
  data: {
    data: {
      riskCounts: {
        OUT_OF_STOCK: 0,
        LOW_STOCK: 0,
        DEAD_STOCK: 0,
        SLOW_MOVING: 1,
        OVERSTOCK: 0,
        HEALTHY: 0,
      },
      totalAvailable: 40,
      confirmedQty30d: 0,
      noSalesAvailable: 40,
      calculatedAt: '2026-07-20T12:00:00+08:00',
    },
  },
}
const evidenceResponse = {
  data: {
    data: {
      insight: item,
      recentConfirmedSales: [
        { paidAt: '2026-06-20T10:20:00', quantity: 1, subtotal: 2999 },
      ],
      recentStockLedgers: [
        {
          action: 'RESERVE',
          quantity: 1,
          beforeTotal: 42,
          afterTotal: 42,
          beforeLocked: 1,
          afterLocked: 2,
          beforeAvailable: 41,
          afterAvailable: 40,
          referenceNo: 'ORDER-SAFE-1',
          createdAt: '2026-07-20T11:00:00',
        },
      ],
      limitations: ['退款数量尚不能按 SKU 可靠回冲，确认销量未扣除退款数量。'],
    },
  },
}

const stubs = {
  'el-alert': {
    props: ['title'],
    template: '<div role="alert">{{ title }}<slot /></div>',
  },
  'el-button': {
    emits: ['click'],
    template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
  },
  'el-card': { template: '<section><slot /></section>' },
  'el-descriptions': { template: '<dl><slot /></dl>' },
  'el-descriptions-item': {
    props: ['label'],
    template: '<div><dt>{{ label }}</dt><dd><slot /></dd></div>',
  },
  'el-drawer': {
    props: ['title'],
    template: '<aside><h2>{{ title }}</h2><slot /></aside>',
  },
  'el-empty': {
    props: ['description'],
    template: '<div>{{ description }}<slot /></div>',
  },
  'el-input': { template: '<input />' },
  'el-pagination': true,
  'el-result': {
    props: ['title', 'subTitle'],
    template: '<section><h2>{{ title }}</h2><p>{{ subTitle }}</p><slot name="extra" /></section>',
  },
  'el-table': true,
  'el-table-column': true,
  'el-tag': { template: '<span><slot /></span>' },
}

function mountPage() {
  return mount(MerchantAiOperations, { global: { stubs } })
}

describe('MerchantAiOperations', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.getMerchantInventoryInsights.mockResolvedValue(pageResponse)
    api.getMerchantInventoryInsightSummary.mockResolvedValue(summaryResponse)
    api.getMerchantInventoryInsightEvidence.mockResolvedValue(evidenceResponse)
  })

  it('loads deterministic facts only and explains risk, windows, limitations, and null cover', async () => {
    let resolvePage!: (value: typeof pageResponse) => void
    api.getMerchantInventoryInsights.mockReturnValueOnce(
      new Promise(resolve => { resolvePage = resolve }),
    )
    const wrapper = mountPage()
    await nextTick()
    expect(wrapper.text()).toContain('正在加载库存洞察')
    resolvePage(pageResponse)
    await flushPromises()

    expect(api.getMerchantInventoryInsights).toHaveBeenCalledWith()
    expect(api.getMerchantInventoryInsightSummary).toHaveBeenCalledOnce()
    expect(api.getMerchantInventoryInsightEvidence).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('近 7/30/90 天窗口')
    expect(wrapper.text()).toContain('当前未扣除退款数量')
    expect(wrapper.text()).toContain('确认销量 7/30/90 天')
    expect(wrapper.text()).toContain('0 / 0 / 4')
    expect(wrapper.text()).toContain('近 30 天无确认销量')
    expect(wrapper.text()).toContain('近 30 天无确认销量，但近 90 天有销量')
    expect(wrapper.find('.mobile-list').exists()).toBe(true)
  })

  it('loads privacy-minimized evidence only after a merchant asks for it', async () => {
    const wrapper = mountPage()
    await flushPromises()

    const evidenceButton = wrapper.findAll('button').find(button => button.text() === '查看证据')
    expect(evidenceButton).toBeDefined()
    await evidenceButton!.trigger('click')
    await flushPromises()

    expect(api.getMerchantInventoryInsightEvidence).toHaveBeenCalledWith('9223372036854775806')
    expect(wrapper.text()).toContain('证据只包含 SKU 聚合指标')
    expect(wrapper.text()).toContain('不包含消费者姓名、电话、地址或内部备注')
    expect(wrapper.text()).toContain('NO_CONFIRMED_SALES_30D_BUT_SALES_90D')
    expect(wrapper.text()).toContain('退款数量尚不能按 SKU 可靠回冲')
  })

  it('applies a readable risk filter while keeping the backend stable sort', async () => {
    const wrapper = mountPage()
    await flushPromises()

    const deadStockCard = wrapper.findAll('button').find(button => button.text().includes('滞销'))
    expect(deadStockCard).toBeDefined()
    await deadStockCard!.trigger('click')
    await flushPromises()

    expect(api.getMerchantInventoryInsights).toHaveBeenLastCalledWith(1, 20, '', 'DEAD_STOCK')
    expect(wrapper.text()).toContain('当前筛选：滞销')
    expect(wrapper.text()).toContain('固定排序：风险优先级')
  })

  it('renders explicit permission and empty states', async () => {
    api.getMerchantInventoryInsights.mockRejectedValueOnce(
      new requestHelpers.ApiRequestError('仅店主或店员可查看库存洞察', 403),
    )
    const forbidden = mountPage()
    await flushPromises()

    expect(forbidden.text()).toContain('权限不足')
    expect(forbidden.text()).toContain('仅店主或店员可查看库存洞察')
    forbidden.unmount()

    api.getMerchantInventoryInsights.mockResolvedValueOnce({
      data: { data: { records: [], total: 0, pageNum: 1, pageSize: 20, pages: 0 } },
    })
    const empty = mountPage()
    await flushPromises()

    expect(empty.text()).toContain('当前筛选条件下没有库存洞察数据')
  })

  it('labels a request timeout and offers a safe retry', async () => {
    api.getMerchantInventoryInsights.mockRejectedValueOnce(new Error('请求超时，请稍后重试'))
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('加载超时')
    expect(wrapper.text()).toContain('请求超时，请稍后重试')
    expect(wrapper.findAll('button').some(button => button.text() === '重新加载')).toBe(true)
  })
})

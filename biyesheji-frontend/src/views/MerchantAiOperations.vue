<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getMerchantInventoryInsightEvidence,
  getMerchantInventoryInsights,
  getMerchantInventoryInsightSummary,
  type InventoryInsightEvidence,
  type InventoryInsightItem,
  type InventoryInsightPage,
  type InventoryInsightRisk,
  type InventoryInsightSummary,
} from '../api/merchant'
import { ApiRequestError, getErrorMessage } from '../api/request'

type ErrorKind = 'general' | 'permission' | 'timeout'
type TagType = 'danger' | 'warning' | 'info' | 'success' | 'primary'

interface RiskPresentation {
  risk: InventoryInsightRisk
  label: string
  tagType: TagType
  rule: string
}

const router = useRouter()
const riskPresentations: RiskPresentation[] = [
  { risk: 'OUT_OF_STOCK', label: '缺货', tagType: 'danger', rule: '销售中且可用库存为 0' },
  { risk: 'LOW_STOCK', label: '低库存', tagType: 'warning', rule: '销售中、可用库存不超过 5，且近 30 天有确认销量' },
  { risk: 'DEAD_STOCK', label: '滞销', tagType: 'danger', rule: '有可用库存、SKU 上线至少 30 天，且近 90 天无确认销量' },
  { risk: 'SLOW_MOVING', label: '慢销', tagType: 'warning', rule: '有可用库存，近 30 天无确认销量，但近 90 天有销量' },
  { risk: 'OVERSTOCK', label: '超储', tagType: 'info', rule: '可用库存不少于 10，且预计库存覆盖超过 90 天' },
  { risk: 'HEALTHY', label: '健康', tagType: 'success', rule: '未命中其他库存风险规则' },
]
const riskByCode = Object.fromEntries(
  riskPresentations.map(presentation => [presentation.risk, presentation]),
) as Record<InventoryInsightRisk, RiskPresentation>

const emptyPage = (): InventoryInsightPage => ({
  records: [],
  total: 0,
  pageNum: 1,
  pageSize: 20,
  pages: 0,
})

const page = ref<InventoryInsightPage>(emptyPage())
const summary = ref<InventoryInsightSummary | null>(null)
const keyword = ref('')
const selectedRisk = ref<InventoryInsightRisk | ''>('')
const loading = ref(false)
const errorMessage = ref('')
const errorKind = ref<ErrorKind>('general')
const drawerVisible = ref(false)
const evidenceLoading = ref(false)
const evidenceError = ref('')
const evidence = ref<InventoryInsightEvidence | null>(null)

const errorTitle = computed(() => {
  if (errorKind.value === 'permission') return '权限不足'
  if (errorKind.value === 'timeout') return '加载超时'
  return '库存洞察加载失败'
})
const calculatedAt = computed(() => summary.value?.calculatedAt || page.value.records[0]?.calculatedAt)
const drawerTitle = computed(() => {
  const item = evidence.value?.insight
  return item ? `${item.productName} · ${item.skuCode}` : '库存洞察证据'
})

function statusFromError(error: unknown) {
  if (error instanceof ApiRequestError) return error.status
  if (typeof error !== 'object' || error === null || !('response' in error)) return 0
  return Number((error as { response?: { status?: number } }).response?.status || 0)
}

function setViewError(error: unknown) {
  const message = getErrorMessage(error, '库存洞察加载失败，请稍后重试')
  const status = statusFromError(error)
  errorMessage.value = message
  errorKind.value = status === 403 || message.includes('权限')
    ? 'permission'
    : message.includes('超时')
      ? 'timeout'
      : 'general'
}

async function loadPage() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getMerchantInventoryInsights(
      page.value.pageNum,
      page.value.pageSize,
      keyword.value.trim(),
      selectedRisk.value || undefined,
    )
    page.value = response.data.data as InventoryInsightPage
  } catch (error) {
    setViewError(error)
  } finally {
    loading.value = false
  }
}

async function loadInitial() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [pageResponse, summaryResponse] = await Promise.all([
      getMerchantInventoryInsights(),
      getMerchantInventoryInsightSummary(),
    ])
    page.value = pageResponse.data.data as InventoryInsightPage
    summary.value = summaryResponse.data.data as InventoryInsightSummary
  } catch (error) {
    setViewError(error)
  } finally {
    loading.value = false
  }
}

async function retry() {
  if (summary.value) await loadPage()
  else await loadInitial()
}

async function applyFilters() {
  page.value.pageNum = 1
  await loadPage()
}

async function selectRisk(risk: InventoryInsightRisk) {
  selectedRisk.value = selectedRisk.value === risk ? '' : risk
  await applyFilters()
}

async function changePage(pageNum: number) {
  page.value.pageNum = pageNum
  await loadPage()
}

async function openEvidence(item: InventoryInsightItem) {
  drawerVisible.value = true
  evidenceLoading.value = true
  evidenceError.value = ''
  evidence.value = null
  try {
    const response = await getMerchantInventoryInsightEvidence(item.skuId)
    evidence.value = response.data.data as InventoryInsightEvidence
  } catch (error) {
    evidenceError.value = getErrorMessage(error, '证据加载失败，请稍后重试')
  } finally {
    evidenceLoading.value = false
  }
}

function formatDateTime(value?: string | null) {
  if (!value) return '暂无'
  return value.replace('T', ' ').replace(/(?:Z|[+-]\d{2}:\d{2})$/, '').slice(0, 19)
}

function formatMoney(value: number) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatDecimal(value: number | null, digits = 2) {
  return value === null ? '暂无' : Number(value).toFixed(digits)
}

function formatPercent(value: number | null) {
  return value === null ? '暂无' : `${(Number(value) * 100).toFixed(1)}%`
}

function formatCover(value: number | null) {
  return value === null ? '近 30 天无确认销量' : `${Number(value).toFixed(1)} 天`
}

function formatSpec(value?: string | null) {
  if (!value) return '默认规格'
  try {
    const parsed = JSON.parse(value) as Record<string, unknown>
    return Object.entries(parsed).map(([key, item]) => `${key}：${String(item)}`).join(' / ') || '默认规格'
  } catch {
    return value
  }
}

function itemStatus(item: InventoryInsightItem) {
  return item.productStatus === 1 && item.skuStatus === 1 ? '销售中' : '非销售中'
}

function getRiskPresentation(risk: InventoryInsightRisk) {
  return riskByCode[risk]
}

function ledgerAction(action: string) {
  const labels: Record<string, string> = {
    RESERVE: '订单预留',
    RELEASE: '订单释放',
    CONFIRM: '确认扣减',
    ADJUST: '人工调整',
    STOCKTAKE: '盘点调整',
  }
  return labels[action] || action
}

onMounted(loadInitial)
</script>

<template>
  <div class="insight-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">确定性经营指标</p>
        <h1>AI 经营助手</h1>
        <p class="subtitle">先看可核对的库存事实与风险规则；本页首次加载不会调用大模型。</p>
      </div>
      <div class="header-actions">
        <span class="calculated-at">最后计算：{{ formatDateTime(calculatedAt) }}</span>
        <el-button @click="router.push('/merchant/inventory')">查看库存管理</el-button>
      </div>
    </header>

    <el-alert
      class="scope-alert"
      type="warning"
      :closable="false"
      show-icon
      title="口径说明：按 Asia/Shanghai 的近 7/30/90 天窗口统计；确认销售以收款时间为准，当前未扣除退款数量。建议仅供经营决策参考，执行前请核对库存和成本。"
    />

    <section v-if="summary" class="summary-grid" aria-label="库存洞察汇总">
      <article class="summary-card">
        <span>总可用库存</span>
        <strong>{{ summary.totalAvailable }}</strong>
        <small>当前 SKU 可用数量合计</small>
      </article>
      <article class="summary-card">
        <span>近 30 天确认销量</span>
        <strong>{{ summary.confirmedQty30d }}</strong>
        <small>仅统计已确认收款</small>
      </article>
      <article class="summary-card">
        <span>无确认销量库存</span>
        <strong>{{ summary.noSalesAvailable }}</strong>
        <small>近 30 天销量为 0 的可用库存</small>
      </article>
    </section>

    <section v-if="summary" class="risk-grid" aria-label="风险筛选">
      <button
        v-for="presentation in riskPresentations"
        :key="presentation.risk"
        type="button"
        class="risk-card"
        :class="{ active: selectedRisk === presentation.risk }"
        :aria-pressed="selectedRisk === presentation.risk"
        @click="selectRisk(presentation.risk)"
      >
        <span class="risk-card-heading">
          <el-tag :type="presentation.tagType" effect="dark">{{ presentation.label }}</el-tag>
          <strong>{{ summary.riskCounts[presentation.risk] || 0 }}</strong>
        </span>
        <small>{{ presentation.rule }}</small>
      </button>
    </section>

    <el-card class="insight-panel" shadow="never">
      <div class="toolbar">
        <div>
          <h2>SKU 风险明细</h2>
          <p>固定排序：风险优先级 → 商品 ID → SKU ID</p>
        </div>
        <div class="filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索商品、SKU 编码或规格"
            aria-label="库存洞察关键词"
            @keyup.enter="applyFilters"
          />
          <el-button type="primary" :loading="loading" @click="applyFilters">查询</el-button>
        </div>
      </div>

      <p v-if="selectedRisk" class="active-filter">
        当前筛选：{{ riskByCode[selectedRisk].label }}（再次点击对应风险卡可取消）
      </p>

      <div v-if="loading" class="state-copy" role="status">正在加载库存洞察，请稍候…</div>

      <el-result
        v-else-if="errorMessage"
        :icon="errorKind === 'permission' ? 'warning' : 'error'"
        :title="errorTitle"
        :sub-title="errorMessage"
      >
        <template #extra>
          <el-button v-if="errorKind !== 'permission'" type="primary" @click="retry">重新加载</el-button>
        </template>
      </el-result>

      <el-empty
        v-else-if="page.records.length === 0"
        description="当前筛选条件下没有库存洞察数据，请调整关键词或风险筛选。"
      />

      <template v-else>
        <el-table class="desktop-table" :data="page.records" stripe>
          <el-table-column label="商品 / SKU" min-width="230">
            <template #default="{ row }">
              <strong>{{ row.productName }}</strong>
              <div class="muted">{{ row.skuCode }} · {{ formatSpec(row.skuSpecJson) }}</div>
              <div class="muted">状态：{{ itemStatus(row) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="风险与规则" min-width="260">
            <template #default="{ row }">
              <el-tag :type="getRiskPresentation(row.risk).tagType" effect="dark">{{ getRiskPresentation(row.risk).label }}</el-tag>
              <div class="rule-copy">{{ getRiskPresentation(row.risk).rule }}</div>
              <code>{{ row.ruleCode }}</code>
            </template>
          </el-table-column>
          <el-table-column label="库存" width="120">
            <template #default="{ row }">
              <div>可用 {{ row.available }}</div>
              <div class="muted">锁定 {{ row.locked }} / 总量 {{ row.total }}</div>
            </template>
          </el-table-column>
          <el-table-column label="确认销量 7/30/90 天" width="180">
            <template #default="{ row }">{{ row.confirmedQty7d }} / {{ row.confirmedQty30d }} / {{ row.confirmedQty90d }}</template>
          </el-table-column>
          <el-table-column label="库存覆盖" min-width="180">
            <template #default="{ row }">
              <strong>{{ formatCover(row.daysOfCover) }}</strong>
              <div class="muted">30 天日均 {{ formatDecimal(row.dailyVelocity30d, 4) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="105" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEvidence(row)">查看证据</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="mobile-list">
          <article v-for="item in page.records" :key="item.skuId" class="mobile-item">
            <div class="mobile-item-heading">
              <div>
                <strong>{{ item.productName }}</strong>
                <p>{{ item.skuCode }} · {{ formatSpec(item.skuSpecJson) }}</p>
              </div>
              <el-tag :type="riskByCode[item.risk].tagType" effect="dark">{{ riskByCode[item.risk].label }}</el-tag>
            </div>
            <p class="rule-copy">{{ riskByCode[item.risk].rule }}</p>
            <dl>
              <div><dt>可用 / 锁定 / 总量</dt><dd>{{ item.available }} / {{ item.locked }} / {{ item.total }}</dd></div>
              <div><dt>确认销量 7/30/90 天</dt><dd>{{ item.confirmedQty7d }} / {{ item.confirmedQty30d }} / {{ item.confirmedQty90d }}</dd></div>
              <div><dt>库存覆盖</dt><dd>{{ formatCover(item.daysOfCover) }}</dd></div>
              <div><dt>近 30 天确认销售额</dt><dd>{{ formatMoney(item.confirmedRevenue30d) }}</dd></div>
            </dl>
            <el-button type="primary" plain @click="openEvidence(item)">查看证据</el-button>
          </article>
        </div>

        <el-pagination
          v-if="page.total > page.pageSize"
          class="pagination"
          background
          layout="prev, pager, next, total"
          :current-page="page.pageNum"
          :page-size="page.pageSize"
          :total="page.total"
          @current-change="changePage"
        />
      </template>
    </el-card>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="min(680px, 92vw)">
      <div v-if="evidenceLoading" class="state-copy" role="status">正在加载脱敏证据…</div>
      <el-alert v-else-if="evidenceError" type="error" :closable="false" :title="evidenceError" show-icon />
      <div v-else-if="evidence" class="evidence-content">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="证据只包含 SKU 聚合指标、确认销售摘要和库存流水，不包含消费者姓名、电话、地址或内部备注。"
        />
        <h3>指标与命中规则</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="风险">
            {{ riskByCode[evidence.insight.risk].label }}：{{ riskByCode[evidence.insight.risk].rule }}
          </el-descriptions-item>
          <el-descriptions-item label="规则编码">{{ evidence.insight.ruleCode }}</el-descriptions-item>
          <el-descriptions-item label="确认销量 7/30/90 天">
            {{ evidence.insight.confirmedQty7d }} / {{ evidence.insight.confirmedQty30d }} / {{ evidence.insight.confirmedQty90d }}
          </el-descriptions-item>
          <el-descriptions-item label="履约需求 30 天">{{ evidence.insight.demandQty30d }}</el-descriptions-item>
          <el-descriptions-item label="确认销售额 30 天">{{ formatMoney(evidence.insight.confirmedRevenue30d) }}</el-descriptions-item>
          <el-descriptions-item label="库存覆盖">{{ formatCover(evidence.insight.daysOfCover) }}</el-descriptions-item>
          <el-descriptions-item label="90 天售罄率">{{ formatPercent(evidence.insight.sellThrough90d) }}</el-descriptions-item>
          <el-descriptions-item label="最近确认销售">{{ formatDateTime(evidence.insight.lastConfirmedSaleAt) }}</el-descriptions-item>
          <el-descriptions-item label="SKU 上线天数">{{ evidence.insight.skuAgeDays }} 天</el-descriptions-item>
          <el-descriptions-item label="计算时间">{{ formatDateTime(evidence.insight.calculatedAt) }}</el-descriptions-item>
        </el-descriptions>

        <h3>最近确认销售摘要</h3>
        <el-empty v-if="evidence.recentConfirmedSales.length === 0" description="暂无确认销售记录" />
        <el-table v-else :data="evidence.recentConfirmedSales" size="small">
          <el-table-column label="确认时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.paidAt) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="120">
            <template #default="{ row }">{{ formatMoney(row.subtotal) }}</template>
          </el-table-column>
        </el-table>

        <h3>最近库存流水</h3>
        <el-empty v-if="evidence.recentStockLedgers.length === 0" description="暂无库存流水" />
        <el-table v-else :data="evidence.recentStockLedgers" size="small">
          <el-table-column label="时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="动作" min-width="100">
            <template #default="{ row }">{{ ledgerAction(row.action) }}</template>
          </el-table-column>
          <el-table-column label="可用库存快照" min-width="140">
            <template #default="{ row }">{{ row.beforeAvailable }} → {{ row.afterAvailable }}</template>
          </el-table-column>
          <el-table-column prop="referenceNo" label="业务参考号" min-width="150" />
        </el-table>

        <h3>数据限制</h3>
        <ul class="limitations">
          <li v-for="limitation in evidence.limitations" :key="limitation">{{ limitation }}</li>
        </ul>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.insight-page{display:grid;gap:18px;color:#1f2937}.page-header{display:flex;align-items:flex-end;justify-content:space-between;gap:24px}.eyebrow{margin:0 0 6px;color:#2563eb;font-size:12px;font-weight:700;letter-spacing:.12em;text-transform:uppercase}.page-header h1{margin:0;font-size:30px}.subtitle{margin:8px 0 0;color:#64748b}.header-actions{display:flex;align-items:center;gap:12px;flex-wrap:wrap;justify-content:flex-end}.calculated-at{color:#64748b;font-size:13px}.scope-alert{line-height:1.6}.summary-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.summary-card{padding:18px;border:1px solid #dbe4f0;border-radius:12px;background:linear-gradient(145deg,#fff,#f7faff);box-shadow:0 8px 24px rgba(15,23,42,.05)}.summary-card span,.summary-card small{display:block;color:#64748b}.summary-card strong{display:block;margin:8px 0 4px;font-size:28px;color:#0f172a}.risk-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}.risk-card{appearance:none;width:100%;padding:14px;text-align:left;border:1px solid #dbe4f0;border-radius:10px;background:#fff;cursor:pointer;transition:border-color .2s,box-shadow .2s,transform .2s}.risk-card:hover,.risk-card.active{border-color:#2563eb;box-shadow:0 8px 20px rgba(37,99,235,.12);transform:translateY(-1px)}.risk-card:focus-visible{outline:3px solid rgba(37,99,235,.25);outline-offset:2px}.risk-card-heading{display:flex;align-items:center;justify-content:space-between;margin-bottom:10px}.risk-card-heading strong{font-size:24px;color:#0f172a}.risk-card small{display:block;min-height:36px;color:#64748b;line-height:1.5}.insight-panel{border-radius:12px}.toolbar{display:flex;align-items:flex-end;justify-content:space-between;gap:18px;margin-bottom:14px}.toolbar h2{margin:0;font-size:20px}.toolbar p{margin:5px 0 0;color:#64748b;font-size:13px}.filters{display:flex;gap:8px;width:min(460px,100%)}.active-filter{padding:9px 12px;border-radius:8px;background:#eff6ff;color:#1d4ed8;font-size:13px}.state-copy{padding:48px 12px;text-align:center;color:#64748b}.muted{margin-top:5px;color:#64748b;font-size:12px;line-height:1.45}.rule-copy{margin:8px 0;color:#475569;font-size:13px;line-height:1.5}.desktop-table code{font-size:11px;color:#64748b;white-space:normal}.mobile-list{display:none}.pagination{justify-content:flex-end;margin-top:18px}.evidence-content{display:grid;gap:12px}.evidence-content h3{margin:12px 0 0;font-size:16px}.limitations{margin:0;padding-left:20px;color:#475569;line-height:1.7}
@media(max-width:980px){.risk-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.page-header{align-items:flex-start}}
@media(max-width:767px){.page-header{display:grid}.page-header h1{font-size:25px}.header-actions{justify-content:flex-start}.summary-grid,.risk-grid{grid-template-columns:1fr}.toolbar{display:grid}.filters{width:100%}.desktop-table{display:none}.mobile-list{display:grid;gap:12px}.mobile-item{padding:14px;border:1px solid #dbe4f0;border-radius:10px;background:#fff}.mobile-item-heading{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.mobile-item-heading p{margin:5px 0 0;color:#64748b;font-size:12px}.mobile-item dl{display:grid;gap:8px;margin:12px 0}.mobile-item dl>div{display:flex;justify-content:space-between;gap:12px;padding-bottom:7px;border-bottom:1px dashed #e2e8f0}.mobile-item dt{color:#64748b;font-size:12px}.mobile-item dd{margin:0;text-align:right;font-weight:600}.mobile-item .el-button{width:100%}.pagination{justify-content:center;overflow-x:auto}.evidence-content :deep(.el-descriptions__body){overflow-x:auto}.evidence-content :deep(.el-descriptions__table){min-width:540px}}
</style>

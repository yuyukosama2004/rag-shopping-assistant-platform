<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  adjustMerchantSkuStock,
  getMerchantInventory,
  getMerchantInventorySummary,
  getMerchantSkuStockLedger,
  type MerchantInventoryItem,
  type MerchantStockLedger,
} from '../api/merchant'

const loading = ref(false)
const adjusting = ref(false)
const ledgerLoading = ref(false)
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const lowStockOnly = ref(false)
const threshold = ref(5)
const items = ref<MerchantInventoryItem[]>([])
const selected = ref<MerchantInventoryItem | null>(null)
const adjustVisible = ref(false)
const ledgerVisible = ref(false)
const ledgers = ref<MerchantStockLedger[]>([])
const adjustment = reactive({ quantity: 0, reason: '' })

const load = async () => {
  loading.value = true
  try {
    const response = await getMerchantInventory(page.value, 20, keyword.value.trim(), lowStockOnly.value)
    items.value = response.data.data.records
    total.value = response.data.data.total
  } finally { loading.value = false }
}

const search = () => { page.value = 1; load() }
const openAdjust = (item: MerchantInventoryItem) => {
  selected.value = item
  adjustment.quantity = 0
  adjustment.reason = ''
  adjustVisible.value = true
}

const adjust = async () => {
  if (!selected.value || adjustment.quantity === 0) return ElMessage.warning('调整数量不能为 0')
  if (!adjustment.reason.trim()) return ElMessage.warning('请填写库存调整原因')
  adjusting.value = true
  try {
    await adjustMerchantSkuStock(selected.value.skuId, adjustment.quantity, adjustment.reason.trim())
    ElMessage.success('库存已调整并写入流水')
    adjustVisible.value = false
    await load()
  } finally { adjusting.value = false }
}

const openLedger = async (item: MerchantInventoryItem) => {
  selected.value = item
  ledgerVisible.value = true
  ledgerLoading.value = true
  try { ledgers.value = (await getMerchantSkuStockLedger(item.skuId)).data.data } finally { ledgerLoading.value = false }
}

const stockType = (item: MerchantInventoryItem) => item.available <= threshold.value && item.productStatus === 1 && item.skuStatus === 1 ? 'danger' : item.available === 0 ? 'info' : 'success'
const statusLabel = (item: MerchantInventoryItem) => item.productStatus !== 1 ? '商品未上架' : item.skuStatus !== 1 ? 'SKU 已停用' : '销售中'

onMounted(async () => {
  const summary = (await getMerchantInventorySummary()).data.data
  threshold.value = summary.threshold
  await load()
})
</script>

<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="toolbar">
        <div><strong>库存管理</strong><span class="hint">低库存阈值：{{ threshold }}</span></div>
        <div class="filters">
          <el-input v-model="keyword" clearable placeholder="商品名或 SKU 编码" @keyup.enter="search" />
          <el-checkbox v-model="lowStockOnly" @change="search">只看销售中的低库存</el-checkbox>
          <el-button type="primary" @click="search">查询</el-button>
        </div>
      </div>
    </template>
    <el-table :data="items">
      <el-table-column prop="productName" label="商品" min-width="190" />
      <el-table-column label="SKU" min-width="180"><template #default="{ row }"><div>{{ row.skuCode }}</div><div class="hint">{{ row.specJson || '默认规格' }}</div></template></el-table-column>
      <el-table-column label="销售状态" width="120"><template #default="{ row }"><el-tag :type="row.productStatus === 1 && row.skuStatus === 1 ? 'success' : 'info'">{{ statusLabel(row) }}</el-tag></template></el-table-column>
      <el-table-column prop="total" label="总库存" width="90" />
      <el-table-column prop="locked" label="预占" width="80" />
      <el-table-column label="可用" width="90"><template #default="{ row }"><el-tag :type="stockType(row)">{{ row.available }}</el-tag></template></el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
      <el-table-column label="操作" width="150"><template #default="{ row }"><el-button link type="primary" @click="openAdjust(row)">调整</el-button><el-button link @click="openLedger(row)">流水</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="prev, pager, next" style="margin-top:16px" @current-change="load" />
  </el-card>

  <el-dialog v-model="adjustVisible" :title="`调整库存 · ${selected?.skuCode || ''}`" width="480px">
    <el-alert v-if="selected" :title="`当前总库存 ${selected.total}，预占 ${selected.locked}，可用 ${selected.available}`" type="info" :closable="false" />
    <el-form label-width="90px" style="margin-top:18px" @submit.prevent="adjust">
      <el-form-item label="调整数量" required><el-input-number v-model="adjustment.quantity" :min="-999999" :max="999999" /><span class="hint">正数入库，负数出库</span></el-form-item>
      <el-form-item label="调整原因" required><el-input v-model="adjustment.reason" maxlength="100" show-word-limit placeholder="盘点、采购入库、报损等" /></el-form-item>
      <el-form-item><el-button type="primary" :loading="adjusting" @click="adjust">确认调整</el-button></el-form-item>
    </el-form>
  </el-dialog>

  <el-drawer v-model="ledgerVisible" :title="`库存流水 · ${selected?.skuCode || ''}`" size="min(680px, 92vw)">
    <el-table v-loading="ledgerLoading" :data="ledgers" size="small">
      <el-table-column prop="createTime" label="时间" min-width="150" />
      <el-table-column prop="action" label="类型" min-width="120" />
      <el-table-column prop="quantity" label="变动" width="75" />
      <el-table-column label="可用库存" width="110"><template #default="{ row }">{{ row.beforeAvailable }} → {{ row.afterAvailable }}</template></el-table-column>
      <el-table-column prop="referenceNo" label="原因/关联单号" min-width="160" />
    </el-table>
  </el-drawer>
</template>

<style scoped>
.toolbar,.filters{display:flex;align-items:center;justify-content:space-between;gap:12px}.filters{justify-content:flex-end}.filters .el-input{width:240px}.hint{font-size:12px;color:#909399;margin-left:10px}@media(max-width:768px){.toolbar,.filters{align-items:stretch;flex-direction:column}.filters .el-input{width:100%}}
</style>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMerchantDashboard, getMerchantInventorySummary } from '../api/merchant'

const router = useRouter()
const loading = ref(true)
const dashboard = ref({ pendingOrderCount: 0, todayOrderCount: 0, todayConfirmedSales: 0 })
const inventory = ref({ lowStockCount: 0, threshold: 5 })

onMounted(async () => {
  try {
    const [orderResponse, inventoryResponse] = await Promise.all([
      getMerchantDashboard(),
      getMerchantInventorySummary(),
    ])
    dashboard.value = orderResponse.data.data
    inventory.value = inventoryResponse.data.data
  } finally { loading.value = false }
})
</script>

<template>
  <div v-loading="loading">
    <div class="section-title">经营概览</div>
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :lg="6"><el-card><div class="label">待处理订单</div><div class="metric">{{ dashboard.pendingOrderCount }}</div></el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card><div class="label">今日订单</div><div class="metric">{{ dashboard.todayOrderCount }}</div></el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card><div class="label">今日已确认销售额</div><div class="metric">¥{{ Number(dashboard.todayConfirmedSales).toFixed(2) }}</div></el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card class="clickable" @click="router.push('/merchant/inventory')"><div class="label">低库存 SKU（≤ {{ inventory.threshold }}）</div><div class="metric" :class="{ danger: inventory.lowStockCount > 0 }">{{ inventory.lowStockCount }}</div></el-card></el-col>
    </el-row>
    <el-alert title="销售额按今日创建且已确认收款的订单统计；货到付款会在商家确认收款后计入。低库存仅统计已上架商品的启用 SKU。" type="info" :closable="false" style="margin-top:16px" />
  </div>
</template>

<style scoped>
.label{color:#909399}.metric{font-size:30px;font-weight:700;margin-top:10px}.danger{color:#f56c6c}.clickable{cursor:pointer}.el-col{margin-bottom:16px}
</style>

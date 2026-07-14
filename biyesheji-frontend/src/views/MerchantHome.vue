<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getMerchantDashboard } from '../api/merchant'

const loading = ref(true)
const dashboard = ref({ pendingOrderCount: 0, todayOrderCount: 0, todayConfirmedSales: 0 })
onMounted(async () => { try { dashboard.value = (await getMerchantDashboard()).data.data } finally { loading.value = false } })
</script>

<template>
  <div v-loading="loading"><div class="section-title">经营概览</div><el-row :gutter="16"><el-col :xs="24" :sm="8"><el-card><div style="color:#909399">待处理订单</div><div style="font-size:30px;font-weight:700;margin-top:10px">{{ dashboard.pendingOrderCount }}</div></el-card></el-col><el-col :xs="24" :sm="8"><el-card><div style="color:#909399">今日订单</div><div style="font-size:30px;font-weight:700;margin-top:10px">{{ dashboard.todayOrderCount }}</div></el-card></el-col><el-col :xs="24" :sm="8"><el-card><div style="color:#909399">今日已确认销售额</div><div style="font-size:30px;font-weight:700;margin-top:10px">¥{{ Number(dashboard.todayConfirmedSales).toFixed(2) }}</div></el-card></el-col></el-row><el-alert title="销售额按今日创建且已确认收款的订单统计；货到付款会在商家确认收款后计入。" type="info" :closable="false" style="margin-top:16px" /></div>
</template>

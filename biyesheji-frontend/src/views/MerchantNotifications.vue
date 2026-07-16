<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMerchantOrderNotifications, retryMerchantOrderNotification, type MerchantOrderNotification } from '../api/merchant'

const loading = ref(false)
const retrying = ref<number>()
const events = ref<MerchantOrderNotification[]>([])

const load = async () => {
  loading.value = true
  try { events.value = (await getMerchantOrderNotifications()).data.data || [] }
  finally { loading.value = false }
}

const retry = async (event: MerchantOrderNotification) => {
  retrying.value = event.id
  try { await retryMerchantOrderNotification(event.id); ElMessage.success('通知已加入重试队列'); await load() }
  finally { retrying.value = undefined }
}

const eventLabel: Record<string, string> = {
  ORDER_CREATED: '订单创建', ORDER_CANCELLED: '订单取消', ORDER_PAYMENT_CONFIRMED: '确认收款',
  ORDER_SHIPPED: '订单发货', ORDER_COMPLETED: '订单完成',
}
const statusType = (status: string) => status === 'SUCCESS' ? 'success' : status === 'FAILED' ? 'danger' : status === 'RETRY' ? 'warning' : 'info'

onMounted(load)
</script>

<template>
  <el-card v-loading="loading">
    <template #header><div class="header"><div><strong>订单通知记录</strong><div class="hint">Webhook 地址与签名密钥由部署者在 .env 中配置；密钥不会在页面或 API 中返回。</div></div><el-button @click="load">刷新</el-button></div></template>
    <el-empty v-if="!loading && !events.length" description="暂无通知事件；未启用 Webhook 时不会生成记录" />
    <el-table v-else :data="events">
      <el-table-column prop="createdAt" label="事件时间" min-width="160" />
      <el-table-column label="事件" min-width="110"><template #default="{ row }">{{ eventLabel[row.eventType] || row.eventType }}</template></el-table-column>
      <el-table-column prop="orderNo" label="订单号" min-width="190" />
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.status }}</el-tag></template></el-table-column>
      <el-table-column prop="attempts" label="尝试" width="70" />
      <el-table-column prop="deliveredAt" label="送达时间" min-width="160" />
      <el-table-column prop="lastError" label="最近错误" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="90"><template #default="{ row }"><el-button v-if="row.status === 'FAILED'" link type="primary" :loading="retrying === row.id" @click="retry(row)">重试</el-button></template></el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>
.header{display:flex;align-items:center;justify-content:space-between;gap:12px}.hint{font-size:12px;color:#909399;margin-top:5px}@media(max-width:767px){.header{align-items:flex-start;flex-direction:column}}
</style>

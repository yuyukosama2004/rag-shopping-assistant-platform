<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { acceptMerchantOrder, confirmMerchantOrderPayment, getMerchantOrders, shipMerchantOrder, type MerchantOrder } from '../api/merchant'

const loading = ref(false)
const shipping = ref(false)
const page = ref(1)
const total = ref(0)
const status = ref<number | undefined>()
const orders = ref<MerchantOrder[]>([])
const shipDialogVisible = ref(false)
const selectedOrder = ref<MerchantOrder | null>(null)
const shipment = reactive({ carrier: '', trackingNo: '', note: '' })

const load = async () => {
  loading.value = true
  try {
    const data = (await getMerchantOrders(page.value, 20, status.value)).data.data
    orders.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}
const confirmPayment = async (order: MerchantOrder) => {
  await confirmMerchantOrderPayment(order.orderNo)
  ElMessage.success('已确认收款')
  await load()
}
const accept = async (order: MerchantOrder) => { await acceptMerchantOrder(order.orderNo); ElMessage.success('订单已接单处理'); await load() }
const openShip = (order: MerchantOrder) => {
  selectedOrder.value = order
  Object.assign(shipment, { carrier: '', trackingNo: '', note: '' })
  shipDialogVisible.value = true
}
const ship = async () => {
  if (!selectedOrder.value || !shipment.carrier.trim() || !shipment.trackingNo.trim()) return ElMessage.warning('请填写承运商和运单号')
  shipping.value = true
  try {
    await shipMerchantOrder(selectedOrder.value.orderNo, shipment)
    ElMessage.success('订单已发货')
    shipDialogVisible.value = false
    await load()
  } finally { shipping.value = false }
}
const statusType = (value: number) => value === 0 ? 'warning' : value === 1 ? 'success' : value === 2 ? 'primary' : value >= 4 ? 'info' : ''
onMounted(load)
</script>

<template>
  <el-card v-loading="loading">
    <template #header><div style="display:flex;justify-content:space-between;align-items:center"><strong>订单管理</strong><el-select v-model="status" clearable placeholder="全部状态" style="width:150px" @change="load"><el-option label="待商家确认" :value="0" /><el-option label="已确认收款" :value="1" /><el-option label="处理中" :value="6" /><el-option label="已发货" :value="2" /><el-option label="已完成" :value="3" /><el-option label="已取消" :value="4" /></el-select></div></template>
    <el-table :data="orders">
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column label="商品" min-width="180"><template #default="{ row }"><div v-for="item in row.items" :key="item.id">{{ item.productName }} × {{ item.quantity }}<span style="color:#909399;font-size:12px"> {{ item.skuCode }}</span></div></template></el-table-column>
      <el-table-column label="收货信息" min-width="190"><template #default="{ row }"><div>{{ row.receiverName }} {{ row.receiverPhone }}</div><div style="font-size:12px;color:#909399">{{ row.receiverAddress }}</div></template></el-table-column>
      <el-table-column label="金额" width="100"><template #default="{ row }">¥{{ Number(row.totalAmount).toFixed(2) }}</template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.statusDesc }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="180"><template #default="{ row }"><el-button v-if="row.status === 0 && row.paymentMethod !== 'COD'" text type="success" @click="confirmPayment(row)">确认收款</el-button><el-button v-if="(row.status === 0 && row.paymentMethod === 'COD') || row.status === 1" text type="primary" @click="accept(row)">接单</el-button><el-button v-if="row.status === 6" text type="primary" @click="openShip(row)">发货</el-button><el-button v-if="row.paymentMethod === 'COD' && (row.status === 2 || row.status === 3) && !row.payTime" text type="success" @click="confirmPayment(row)">确认货款</el-button><span v-if="row.status === 2" style="font-size:12px;color:#909399">{{ row.shippingCarrier }} {{ row.trackingNo }}</span></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="prev, pager, next" style="margin-top:16px" @current-change="load" />
  </el-card>
  <el-dialog v-model="shipDialogVisible" title="填写发货信息" width="520px"><el-form label-width="80px" @submit.prevent="ship"><el-form-item label="承运商" required><el-input v-model="shipment.carrier" /></el-form-item><el-form-item label="运单号" required><el-input v-model="shipment.trackingNo" /></el-form-item><el-form-item label="备注"><el-input v-model="shipment.note" type="textarea" /></el-form-item><el-form-item><el-button type="primary" :loading="shipping" @click="ship">确认发货</el-button></el-form-item></el-form></el-dialog>
</template>

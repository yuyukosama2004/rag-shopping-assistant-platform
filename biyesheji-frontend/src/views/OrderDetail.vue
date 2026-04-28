<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail, payOrder, cancelOrder } from '../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const order = ref<any>(null)
const statusMap: Record<number, string> = { 0: '待支付', 1: '已支付', 2: '已发货', 3: '已完成', 4: '已取消', 5: '已超时' }

onMounted(async () => {
  try { const r = await getOrderDetail(route.params.orderNo as string); order.value = r.data.data } catch {}
})

const doPay = async () => {
  await payOrder(order.value.orderNo)
  ElMessage.success('支付成功')
  const r = await getOrderDetail(order.value.orderNo); order.value = r.data.data
}
const doCancel = async () => {
  await ElMessageBox.confirm('确定取消？')
  await cancelOrder(order.value.orderNo)
  ElMessage.success('已取消')
  const r = await getOrderDetail(order.value.orderNo); order.value = r.data.data
}
</script>

<template>
  <div v-if="order">
    <h1 class="page-header">订单详情</h1>
    <el-card>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusMap[order.status] }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ order.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ order.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ order.receiverAddress }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ order.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ order.payTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总金额"><span class="price" style="font-size:20px">¥{{ order.totalAmount }}</span></el-descriptions-item>
      </el-descriptions>
      <div style="margin-top:16px">
        <el-button v-if="order.status === 0" type="success" @click="doPay">立即支付</el-button>
        <el-button v-if="order.status === 0" type="danger" @click="doCancel">取消订单</el-button>
      </div>
    </el-card>

    <el-card header="商品清单" style="margin-top:16px">
      <div v-for="item in order.items" :key="item.id" style="display:flex;align-items:center;padding:12px 0;border-bottom:1px solid #f5f5f5;gap:12px">
        <img :src="item.productImage||'https://picsum.photos/60/60'" style="width:60px;height:60px;object-fit:cover;border-radius:8px" />
        <div style="flex:1"><strong>{{ item.productName }}</strong></div>
        <span>¥{{ item.price }} × {{ item.quantity }}</span>
        <span class="price">¥{{ item.subtotal }}</span>
      </div>
    </el-card>
  </div>
  <el-empty v-else description="订单不存在" />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'; import { useRoute } from 'vue-router'
import { getOrderDetail, payOrder, cancelOrder } from '../api/order'; import { ElMessage, ElMessageBox } from 'element-plus'
const route = useRoute(); const order = ref<any>(null)
const sm: Record<number,string> = {0:'待支付',1:'已支付',2:'已发货',3:'已完成',4:'已取消',5:'已超时'}
onMounted(async () => { try { const r = await getOrderDetail(route.params.orderNo as string); order.value = r.data.data } catch {} })
const pay = async () => { await payOrder(order.value.orderNo); ElMessage.success('支付成功'); const r = await getOrderDetail(order.value.orderNo); order.value = r.data.data }
const cancel = async () => { await ElMessageBox.confirm('确定取消？'); await cancelOrder(order.value.orderNo); ElMessage.success('已取消'); const r = await getOrderDetail(order.value.orderNo); order.value = r.data.data }
</script>
<template>
  <div v-if="order" style="max-width:800px;margin:0 auto">
    <div class="section-title">订单详情</div>
    <div style="background:#fff;padding:20px;margin-bottom:14px">
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;margin-bottom:14px">
        <span style="font-size:13px;color:#999">{{ order.orderNo }}</span>
        <el-tag :type="order.status===0?'danger':order.status===1?'success':''">{{ sm[order.status] }}</el-tag>
      </div>
      <div style="font-size:13px;color:#555;line-height:2">收货人: {{ order.receiverName }} {{ order.receiverPhone }}<br/>地址: {{ order.receiverAddress }}<br/>下单: {{ order.createdAt }}<br/>支付: {{ order.payTime||'-' }}</div>
      <div style="text-align:right;margin-top:12px"><span class="price" style="font-size:24px">¥{{ order.totalAmount }}</span></div>
      <div v-if="order.status===0" style="display:flex;gap:8px;margin-top:14px"><el-button type="danger" @click="pay" style="flex:1">立即支付</el-button><el-button @click="cancel" style="flex:1">取消</el-button></div>
    </div>
    <div style="background:#fff;padding:20px"><div style="font-weight:600;margin-bottom:8px">商品清单</div>
      <div v-for="it in order.items" :key="it.id" style="display:flex;gap:12px;align-items:center;padding:8px 0;border-bottom:1px solid #f5f5f5">
        <img :src="it.productImage||''" style="width:50px;height:50px;border-radius:4px;object-fit:cover" />
        <div style="flex:1;font-size:13px">{{ it.productName }}</div>
        <span style="font-size:13px;color:#999">¥{{ it.price }}×{{ it.quantity }}</span>
        <span class="price">¥{{ it.subtotal }}</span>
      </div>
    </div>
  </div>
  <el-empty v-else description="订单不存在" style="padding-top:60px" />
</template>

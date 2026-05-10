<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail, payOrder, cancelOrder } from '../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
const route = useRoute(); const order = ref<any>(null)
const sm: Record<number,string> = {0:'待支付',1:'已支付',2:'已发货',3:'已完成',4:'已取消',5:'已超时'}
onMounted(async () => { try { const r = await getOrderDetail(route.params.orderNo as string); order.value = r.data.data } catch {} })
const pay = async () => { await payOrder(order.value.orderNo); ElMessage.success('支付成功'); const r = await getOrderDetail(order.value.orderNo); order.value = r.data.data }
const cancel = async () => { await ElMessageBox.confirm('确定取消？'); await cancelOrder(order.value.orderNo); ElMessage.success('已取消'); const r = await getOrderDetail(order.value.orderNo); order.value = r.data.data }
</script>
<template>
  <div class="page-content" v-if="order">
    <div class="page-title">订单详情</div>
    <div style="background:#fff;border-radius:12px;padding:16px;margin-bottom:12px;box-shadow:var(--shadow-card)">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
        <span style="font-size:13px;color:#999">{{ order.orderNo }}</span>
        <el-tag :type="order.status===0?'warning':order.status===1?'success':''">{{ sm[order.status] }}</el-tag>
      </div>
      <div style="font-size:13px;color:#555;line-height:2">
        <div>收货人: {{ order.receiverName }} {{ order.receiverPhone }}</div>
        <div>地址: {{ order.receiverAddress }}</div>
        <div>下单: {{ order.createdAt }}</div>
        <div>支付: {{ order.payTime||'-' }}</div>
      </div>
      <div style="text-align:right;margin-top:12px">
        <span style="color:var(--jd-red);font-size:24px;font-weight:700">¥{{ order.totalAmount }}</span>
      </div>
      <div style="display:flex;gap:8px;margin-top:12px" v-if="order.status===0">
        <el-button type="primary" @click="pay" style="flex:1">立即支付</el-button>
        <el-button type="danger" @click="cancel" style="flex:1">取消订单</el-button>
      </div>
    </div>
    <div style="background:#fff;border-radius:12px;padding:16px;box-shadow:var(--shadow-card)">
      <div style="font-weight:600;margin-bottom:8px">商品清单</div>
      <div v-for="it in order.items" :key="it.id" style="display:flex;gap:10px;padding:8px 0;border-bottom:1px solid #f5f5f5;align-items:center">
        <img :src="it.productImage||'https://picsum.photos/60/60'" style="width:48px;height:48px;border-radius:8px;object-fit:cover">
        <div style="flex:1;font-size:13px">{{ it.productName }}</div>
        <div style="font-size:13px;color:#999">¥{{ it.price }}×{{ it.quantity }}</div>
        <div style="color:var(--jd-red);font-weight:700">¥{{ it.subtotal }}</div>
      </div>
    </div>
  </div>
  <el-empty v-else description="订单不存在" style="padding-top:80px" />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'; import { useRoute } from 'vue-router'
import { getOrderDetail, cancelOrder, completeOrder } from '../api/order'; import { ElMessage, ElMessageBox } from 'element-plus'
const route = useRoute(); const order = ref<any>(null)
const sm: Record<number,string> = {0:'待商家确认',1:'已确认收款',2:'已发货',3:'已完成',4:'已取消',5:'已超时',6:'处理中'}
onMounted(async () => { try { const r = await getOrderDetail(route.params.orderNo as string); order.value = r.data.data } catch {} })
const cancel = async () => { await ElMessageBox.confirm('确定取消？'); await cancelOrder(order.value.orderNo); ElMessage.success('已取消'); const r = await getOrderDetail(order.value.orderNo); order.value = r.data.data }
const complete = async () => { await ElMessageBox.confirm('确认已收到商品？'); await completeOrder(order.value.orderNo); ElMessage.success('已确认收货'); const r = await getOrderDetail(order.value.orderNo); order.value = r.data.data }
</script>
<template>
  <div v-if="order" style="max-width:800px;margin:0 auto">
    <div class="section-title">订单详情</div>
    <div style="background:#fff;padding:20px;margin-bottom:14px">
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;margin-bottom:14px">
        <span style="font-size:13px;color:#999">{{ order.orderNo }}</span>
        <el-tag :type="order.status===0?'danger':order.status===1?'success':''">{{ sm[order.status] }}</el-tag>
      </div>
      <div style="font-size:13px;color:#555;line-height:2">收货人: {{ order.receiverName }} {{ order.receiverPhone }}<br/>地址: {{ order.receiverAddress }}<br/>付款方式: {{ order.paymentMethod === 'COD' ? '货到付款' : '线下付款' }}<br/>下单: {{ order.createdAt }}<br/>收款确认: {{ order.payTime||'-' }}<template v-if="order.status >= 2"><br/>物流: {{ order.shippingCarrier || '-' }} {{ order.trackingNo || '-' }}<br/>发货: {{ order.shippedAt || '-' }}</template></div>
      <div style="text-align:right;margin-top:12px;line-height:1.7"><div>{{ order.shippingRuleName || (order.shippingMethod === 'PICKUP' ? '门店自提' : '配送') }}：¥{{ Number(order.shippingFee || 0).toFixed(2) }}</div><div>商品金额：¥{{ Number(order.productAmount ?? order.totalAmount).toFixed(2) }}</div><span class="price" style="font-size:24px">¥{{ order.totalAmount }}</span></div>
      <div v-if="order.status===0" style="display:flex;gap:8px;margin-top:14px"><span style="flex:1;color:#909399">{{ order.paymentMethod === 'COD' ? '等待商家接单后发货' : '请按店铺说明完成线下付款，商家确认后发货' }}</span><el-button @click="cancel">取消</el-button></div>
      <div v-else-if="order.status===2" style="margin-top:14px"><el-button type="success" style="width:100%" @click="complete">确认收货</el-button></div>
    </div>
    <div style="background:#fff;padding:20px"><div style="font-weight:600;margin-bottom:8px">商品清单</div>
      <div v-for="it in order.items" :key="it.id" style="display:flex;gap:12px;align-items:center;padding:8px 0;border-bottom:1px solid #f5f5f5">
        <img :src="it.productImage||''" style="width:50px;height:50px;border-radius:4px;object-fit:cover" />
        <div style="flex:1;font-size:13px">{{ it.productName }}<div style="font-size:12px;color:#999;margin-top:4px">{{ it.skuCode }} · {{ it.skuSpecJson }}</div></div>
        <span style="font-size:13px;color:#999">¥{{ it.price }}×{{ it.quantity }}</span>
        <span class="price">¥{{ it.subtotal }}</span>
      </div>
    </div>
  </div>
  <el-empty v-else description="订单不存在" style="padding-top:60px" />
</template>

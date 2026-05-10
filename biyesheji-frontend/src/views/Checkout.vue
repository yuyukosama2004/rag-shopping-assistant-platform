<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCartList, submitOrder } from '../api/order'
import { ElMessage } from 'element-plus'
const router = useRouter(); const items = ref<any[]>([])
const rcv = ref({ name: '', phone: '', address: '' })
onMounted(async () => { const r = await getCartList(); items.value = r.data.data.filter((i: any) => i.checked === 1) })
const total = () => items.value.reduce((s, i) => s + i.productPrice * i.quantity, 0)
const submit = async () => {
  if (!rcv.value.name || !rcv.value.phone || !rcv.value.address) return ElMessage.warning('请填写收货信息')
  try {
    const r = await submitOrder({ items: items.value.map(i => ({ productId: i.productId, quantity: i.quantity })), receiverName: rcv.value.name, receiverPhone: rcv.value.phone, receiverAddress: rcv.value.address })
    ElMessage.success('下单成功! ' + r.data.data.orderNo); router.push('/orders')
  } catch {}
}
</script>
<template>
  <div style="max-width:800px;margin:0 auto">
    <div class="section-title">确认订单</div>
    <div style="background:#fff;padding:16px;margin-bottom:14px"><div style="font-weight:600;margin-bottom:8px">收货信息</div>
      <el-input v-model="rcv.name" placeholder="收货人" style="margin-bottom:8px;width:200px" />
      <el-input v-model="rcv.phone" placeholder="手机号" style="margin-bottom:8px;width:200px" />
      <el-input v-model="rcv.address" placeholder="详细地址" />
    </div>
    <div style="background:#fff;padding:16px;margin-bottom:14px"><div style="font-weight:600;margin-bottom:8px">商品清单</div>
      <div v-for="it in items" :key="it.id" style="display:flex;gap:12px;align-items:center;padding:8px 0;border-bottom:1px solid #f5f5f5">
        <img :src="it.productImage||'https://picsum.photos/60/60'" style="width:50px;height:50px;border-radius:4px;object-fit:cover" />
        <div style="flex:1;font-size:13px">{{ it.productName }}</div>
        <span style="font-size:13px;color:#999">¥{{ it.productPrice }}×{{ it.quantity }}</span>
        <span class="price">¥{{ (it.productPrice*it.quantity).toFixed(2) }}</span>
      </div>
      <div style="text-align:right;margin-top:14px"><span>合计 <span style="font-size:22px" class="price">¥{{ total().toFixed(2) }}</span></span><el-button type="danger" size="large" @click="submit" style="margin-left:12px">提交订单</el-button></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCartList, submitOrder } from '../api/order'
import { ElMessage } from 'element-plus'

const router = useRouter()
const items = ref<any[]>([])
const receiver = ref({ name: '', phone: '', address: '' })

onMounted(async () => {
  const r = await getCartList()
  items.value = r.data.data.filter((i: any) => i.checked === 1)
})

const totalAmount = () => items.value.reduce((s, i) => s + i.productPrice * i.quantity, 0)

const onSubmit = async () => {
  if (!receiver.value.name || !receiver.value.phone || !receiver.value.address) {
    return ElMessage.warning('请填写收货信息')
  }
  const orderItems = items.value.map(i => ({ productId: i.productId, quantity: i.quantity }))
  // 简单的幂等键（后端也会生成）
  const itemsMd5 = btoa(JSON.stringify(orderItems)).substring(0, 32)
  try {
    const r = await submitOrder({
      items: orderItems,
      receiverName: receiver.value.name,
      receiverPhone: receiver.value.phone,
      receiverAddress: receiver.value.address,
      md5: itemsMd5,
    })
    ElMessage.success('下单成功! 订单号: ' + r.data.data.orderNo)
    router.push('/orders')
  } catch {}
}
</script>

<template>
  <h1 class="page-header">确认订单</h1>
  <el-card header="收货信息" style="margin-bottom:16px">
    <el-form :model="receiver" inline>
      <el-form-item label="收货人"><el-input v-model="receiver.name" placeholder="姓名" /></el-form-item>
      <el-form-item label="电话"><el-input v-model="receiver.phone" placeholder="手机号" /></el-form-item>
      <el-form-item label="地址" style="width:100%"><el-input v-model="receiver.address" placeholder="详细地址" /></el-form-item>
    </el-form>
  </el-card>

  <el-card header="商品清单">
    <div v-for="item in items" :key="item.id" style="display:flex;align-items:center;padding:12px 0;border-bottom:1px solid #f5f5f5;gap:12px">
      <img :src="item.productImage||'https://picsum.photos/60/60'" style="width:60px;height:60px;object-fit:cover;border-radius:8px" />
      <div style="flex:1"><strong>{{ item.productName }}</strong></div>
      <span>¥{{ item.productPrice }} × {{ item.quantity }}</span>
      <span class="price">¥{{ (item.productPrice * item.quantity).toFixed(2) }}</span>
    </div>
    <div style="text-align:right;margin-top:16px">
      <span>合计: <span class="price" style="font-size:24px">¥{{ totalAmount().toFixed(2) }}</span></span>
      <el-button size="large" type="primary" @click="onSubmit" style="margin-left:16px">提交订单</el-button>
    </div>
  </el-card>
</template>

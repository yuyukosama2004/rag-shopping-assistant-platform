<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCartList, getShippingRules, submitOrder, removeCartBatch, type ShippingRule } from '../api/order'
import { getAddressList } from '../api/user'
import { ElMessage } from 'element-plus'
const router = useRouter(); const items = ref<any[]>([]); const addresses = ref<any[]>([])
const rcv = ref({ name: '', phone: '', address: '' }); const selectedAddr = ref(0)
const paymentMethod = ref<'OFFLINE' | 'COD'>('OFFLINE')
const shippingRules = ref<ShippingRule[]>([])
const shippingRuleId = ref<number>()

onMounted(async () => {
  const [cartRes, addrRes, shippingRes] = await Promise.all([getCartList(), getAddressList(), getShippingRules()])
  items.value = cartRes.data.data.filter((i: any) => i.checked === 1)
  addresses.value = addrRes.data.data || []
  const def = addresses.value.find((a: any) => a.isDefault === 1)
  if (def) { selectedAddr.value = def.id; rcv.value = { name: def.receiverName, phone: def.receiverPhone, address: def.detail } }
  shippingRules.value = shippingRes.data.data || []
  shippingRuleId.value = shippingRules.value[0]?.id
})
const total = () => items.value.reduce((s, i) => s + i.productPrice * i.quantity, 0)
const selectedShipping = () => shippingRules.value.find(rule => rule.id === shippingRuleId.value)
const shippingFee = () => {
  const rule = selectedShipping()
  if (!rule || rule.ruleType === 'PICKUP') return 0
  return rule.freeShippingThreshold && total() >= rule.freeShippingThreshold ? 0 : Number(rule.baseFee || 0)
}
const shippingLabel = (rule: ShippingRule) => rule.ruleType === 'PICKUP' ? `${rule.name}（门店自提，免运费）` : `${rule.name}（运费 ¥${Number(rule.baseFee).toFixed(2)}${rule.freeShippingThreshold ? `，满 ¥${Number(rule.freeShippingThreshold).toFixed(2)} 免运费` : ''}）`
const selectAddr = (a: any) => { selectedAddr.value = a.id; rcv.value = { name: a.receiverName, phone: a.receiverPhone, address: a.detail } }
const submit = async () => {
  if (!rcv.value.name || !rcv.value.phone || !rcv.value.address) return ElMessage.warning('请填写收货信息')
  if (!shippingRuleId.value) return ElMessage.warning('请选择配送方式')
  try {
    const r = await submitOrder({ items: items.value.map(i => ({ productId: i.productId, skuId: i.skuId, quantity: i.quantity })), receiverName: rcv.value.name, receiverPhone: rcv.value.phone, receiverAddress: rcv.value.address, paymentMethod: paymentMethod.value, shippingRuleId: shippingRuleId.value })
    removeCartBatch(items.value.map(i => i.id))
    ElMessage.success('下单成功! 订单号: ' + r.data.data.orderNo)
    setTimeout(() => router.push('/orders'), 3000)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '下单失败，请重试')
  }
}
</script>
<template>
  <div style="max-width:800px;margin:0 auto">
    <div class="section-title">确认订单</div>

    <!-- 收货信息 -->
    <div style="background:#fff;padding:16px;margin-bottom:14px">
      <div style="font-weight:600;margin-bottom:8px">收货信息</div>
      <div v-if="addresses.length > 0" style="margin-bottom:10px">
        <div style="font-size:12px;color:#999;margin-bottom:4px">选择已保存的地址</div>
        <div v-for="a in addresses" :key="a.id" @click="selectAddr(a)"
          style="padding:8px;border:1px solid #eee;margin-bottom:4px;cursor:pointer;font-size:13px;border-radius:4px"
          :style="selectedAddr===a.id ? {borderColor:'var(--jd-red)',background:'#FFF0F0'} : {}">
          <span v-if="a.isDefault" style="background:var(--jd-red);color:#fff;font-size:10px;padding:0 4px;margin-right:4px">默认</span>
          {{ a.receiverName }} · {{ a.receiverPhone }} · {{ a.detail }}
        </div>
      </div>
      <div style="font-size:12px;color:#999;margin-bottom:4px">或手动填写</div>
      <el-input v-model="rcv.name" placeholder="收货人" style="margin-bottom:8px;width:200px" />
      <el-input v-model="rcv.phone" placeholder="手机号" style="margin-bottom:8px;width:200px" />
      <el-input v-model="rcv.address" placeholder="详细地址" />
    </div>

    <div style="background:#fff;padding:16px;margin-bottom:14px"><div style="font-weight:600;margin-bottom:8px">配送方式</div><el-empty v-if="!shippingRules.length" description="商家暂未配置配送方式" :image-size="40" /><el-radio-group v-else v-model="shippingRuleId" style="display:flex;flex-direction:column;align-items:flex-start;gap:8px"><el-radio v-for="rule in shippingRules" :key="rule.id" :value="rule.id">{{ shippingLabel(rule) }}</el-radio></el-radio-group></div>
    <!-- 商品清单 -->
    <div style="background:#fff;padding:16px;margin-bottom:14px"><div style="font-weight:600;margin-bottom:8px">付款方式</div><el-radio-group v-model="paymentMethod"><el-radio value="OFFLINE">线下付款（商家确认收款后发货）</el-radio><el-radio value="COD">货到付款（商家接单后发货）</el-radio></el-radio-group></div>
    <div style="background:#fff;padding:16px;margin-bottom:14px"><div style="font-weight:600;margin-bottom:8px">商品清单</div>
      <div v-for="it in items" :key="it.id" style="display:flex;gap:12px;align-items:center;padding:8px 0;border-bottom:1px solid #f5f5f5">
        <img :src="it.productImage||''" style="width:50px;height:50px;border-radius:4px;object-fit:cover" />
        <div style="flex:1;font-size:13px">{{ it.productName }}</div>
        <span style="font-size:13px;color:#999">¥{{ it.productPrice }}×{{ it.quantity }}</span>
        <span class="price">¥{{ (it.productPrice*it.quantity).toFixed(2) }}</span>
      </div>
      <div style="text-align:right;margin-top:14px;line-height:1.8"><div>商品金额 ¥{{ total().toFixed(2) }}</div><div>配送费用 ¥{{ shippingFee().toFixed(2) }}</div><span>合计 <span style="font-size:22px" class="price">¥{{ (total() + shippingFee()).toFixed(2) }}</span></span><el-button size="large" @click="router.push('/cart')" style="margin-left:12px">返回购物车</el-button><el-button type="danger" size="large" @click="submit" style="margin-left:8px">提交订单</el-button></div>
    </div>
  </div>
</template>

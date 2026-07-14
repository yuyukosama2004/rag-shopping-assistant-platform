<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getPublicStoreSetting, type StoreSetting } from '../api/merchant'
const store = ref<StoreSetting>({ storeName: '店铺' })
onMounted(async () => { const data = (await getPublicStoreSetting()).data.data; if (data) store.value = data })
</script>

<template>
  <div style="max-width:820px;margin:0 auto"><div class="section-title">店铺服务说明</div><el-card><h3>联系方式</h3><p v-if="store.servicePhone">客服电话：{{ store.servicePhone }}</p><p v-if="store.serviceEmail">客服邮箱：{{ store.serviceEmail }}</p><p v-if="store.address">店铺地址：{{ store.address }}</p><el-divider /><h3>配送说明</h3><p style="white-space:pre-wrap">{{ store.shippingNotice || '配送方式、费用和时效以结算页展示的规则为准。' }}</p><el-divider /><h3>售后说明</h3><p style="white-space:pre-wrap">{{ store.afterSalesNotice || '如需售后，请在订单详情中提交退款申请，商家会按店铺规则处理。' }}</p><el-divider /><h3>隐私说明</h3><p>我们仅为完成订单履约、客户服务和账户安全处理必要的联系与收货信息，不会将其用于与订单无关的用途。</p></el-card></div>
</template>

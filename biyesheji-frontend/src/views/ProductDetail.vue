<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail } from '../api/product'
import { addToCart } from '../api/order'
import { ElMessage } from 'element-plus'
const route = useRoute(); const router = useRouter()
const product = ref<any>(null); const spec = ref<any>(null); const qty = ref(1)
onMounted(async () => {
  const r = await getProductDetail(Number(route.params.id))
  product.value = r.data.data
  try { spec.value = JSON.parse(product.value.specJson) } catch {}
})
const addCart = async () => { try { await addToCart(product.value.id, qty.value); ElMessage.success('已加入购物车') } catch {} }
const goBuy = () => { addToCart(product.value.id, qty.value).then(() => router.push('/checkout')) }
</script>
<template>
  <div v-if="product">
    <img :src="product.mainImage||'https://picsum.photos/400/400'" style="width:100%;aspect-ratio:1;object-fit:cover" />
    <div style="padding:16px;background:#fff">
      <h1 style="font-size:18px;font-weight:600;margin-bottom:8px">{{ product.name }}</h1>
      <div style="margin-bottom:12px">
        <span style="color:var(--jd-red);font-size:26px;font-weight:700">¥{{ product.price }}</span>
        <span v-if="product.originalPrice > product.price" style="color:#999;font-size:14px;text-decoration:line-through;margin-left:8px">¥{{ product.originalPrice }}</span>
      </div>
      <p style="color:#999;font-size:13px;margin-bottom:8px">{{ product.brand }} · 月销 {{ product.sales }}</p>
      <p style="font-size:14px;color:#555;line-height:1.6;margin-bottom:16px">{{ product.description }}</p>
      <div v-if="spec" style="background:#f8f8f8;border-radius:8px;padding:12px;margin-bottom:16px">
        <div v-for="(v,k) in spec" :key="k" style="display:flex;justify-content:space-between;padding:4px 0;font-size:13px;border-bottom:1px solid #eee">
          <span style="color:#999">{{ k }}</span><span style="font-weight:500">{{ v }}</span>
        </div>
      </div>
    </div>
    <div style="position:sticky;bottom:56px;background:#fff;padding:12px 16px;display:flex;gap:12px;align-items:center;box-shadow:0 -2px 8px rgba(0,0,0,0.04)">
      <el-input-number v-model="qty" :min="1" :max="99" size="small" style="width:100px" />
      <el-button @click="addCart" style="flex:1">加入购物车</el-button>
      <el-button type="primary" @click="goBuy" style="flex:1">立即购买</el-button>
    </div>
  </div>
  <el-empty v-else description="商品不存在或已下架" style="padding-top:80px" />
</template>

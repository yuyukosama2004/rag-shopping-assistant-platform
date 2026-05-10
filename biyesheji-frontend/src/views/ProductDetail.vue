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
    <div class="detail-wrap">
      <div class="detail-img"><img :src="product.mainImage||'https://picsum.photos/400/400'" /></div>
      <div class="detail-info">
        <h1>{{ product.name }}</h1>
        <div class="jd-price"><span style="font-size:16px">¥</span>{{ product.price }}<span class="original-price" v-if="product.originalPrice>product.price" style="font-size:14px">¥{{ product.originalPrice }}</span></div>
        <p style="color:#999;font-size:13px;margin-bottom:8px">{{ product.brand }} · 月销 {{ product.sales }}</p>
        <p style="font-size:14px;color:#555;line-height:1.7;margin-bottom:16px">{{ product.description }}</p>
        <table class="spec-table" v-if="spec">
          <tr v-for="(v,k) in spec" :key="k"><td>{{ k }}</td><td>{{ v }}</td></tr>
        </table>
      </div>
    </div>
    <div class="detail-bottom">
      <div style="flex:1"></div>
      <el-input-number v-model="qty" :min="1" :max="99" size="small" />
      <el-button size="large" @click="addCart">加入购物车</el-button>
      <el-button size="large" type="primary" @click="goBuy">立即购买</el-button>
    </div>
  </div>
  <el-empty v-else description="商品不存在" style="padding-top:60px" />
</template>

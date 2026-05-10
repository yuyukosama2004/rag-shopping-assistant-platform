<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail } from '../api/product'
import { addToCart } from '../api/order'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const product = ref<any>(null)
const spec = ref<any>(null)
const quantity = ref(1)

onMounted(async () => {
  const res = await getProductDetail(Number(route.params.id))
  product.value = res.data.data
  try { spec.value = JSON.parse(product.value.specJson) } catch {}
})

const goBuy = () => {
  addToCart(product.value.id, quantity.value).then(() => router.push('/checkout'))
}
const addCart = async () => {
  try { await addToCart(product.value.id, quantity.value); ElMessage.success('已加入购物车') } catch {}
}
</script>

<template>
  <div v-if="product" style="display:flex;gap:32px;flex-wrap:wrap;margin-top:24px">
    <div style="flex:1;min-width:350px">
      <img :src="product.mainImage||'https://picsum.photos/400/400'" style="width:100%;border-radius:8px" />
    </div>
    <div style="flex:1;min-width:350px">
      <h1>{{ product.name }}</h1>
      <div style="margin:16px 0"><span class="price" style="font-size:28px">¥{{ product.price }}</span><span class="original-price" v-if="product.originalPrice > product.price" style="font-size:16px">¥{{ product.originalPrice }}</span></div>
      <p style="color:#666">{{ product.brand }} | {{ product.category }} | 月销 {{ product.sales }}</p>
      <p style="margin:16px 0">{{ product.description }}</p>

      <!-- 参数对比表 -->
      <el-card v-if="spec" header="核心参数" style="margin:16px 0">
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item v-for="(v,k) in spec" :key="k" :label="k">{{ v }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <div style="display:flex;align-items:center;gap:12px;margin:24px 0">
        <el-input-number v-model="quantity" :min="1" :max="99" />
        <el-button size="large" @click="addCart">加入购物车</el-button>
        <el-button size="large" type="primary" @click="goBuy">立即购买</el-button>
      </div>
    </div>
  </div>
  <el-empty v-else description="商品不存在" />
</template>

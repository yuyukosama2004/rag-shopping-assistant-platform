<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProductPage, getHotProducts, getFilters } from '../api/product'
import { addToCart } from '../api/order'
import { ElMessage } from 'element-plus'

const router = useRouter()
const products = ref<any[]>([])
const hotProducts = ref<any[]>([])
const brands = ref<string[]>([])
const categories = ref<string[]>([])
const keyword = ref('')

const loadData = async () => {
  const [pageRes, hotRes, filterRes] = await Promise.all([
    getProductPage({ pageNum: 1, pageSize: 8 }),
    getHotProducts(4),
    getFilters(),
  ])
  products.value = pageRes.data.data.records
  hotProducts.value = hotRes.data.data
  brands.value = filterRes.data.data.brands
  categories.value = filterRes.data.data.categories
}

onMounted(loadData)

const search = () => router.push({ path: '/products', query: { keyword: keyword.value } })
const goDetail = (id: number) => router.push(`/product/${id}`)
const goProducts = (filter: any) => router.push({ path: '/products', query: filter })
const quickAdd = async (id: number) => {
  try { await addToCart(id); ElMessage.success('已加入购物车') } catch {}
}
</script>

<template>
  <!-- 搜索 Hero -->
  <div style="background:linear-gradient(135deg,#667eea,#764ba2);padding:48px 16px;text-align:center;border-radius:8px;color:#fff;margin-bottom:24px">
    <h1 style="font-size:32px;margin-bottom:16px">找到最适合你的手机</h1>
    <el-input v-model="keyword" placeholder="搜索手机品牌、型号..." size="large" style="max-width:500px" @keyup.enter="search">
      <template #append><el-button @click="search" :icon="'Search'">搜索</el-button></template>
    </el-input>
    <div style="margin-top:12px;display:flex;gap:8px;justify-content:center;flex-wrap:wrap">
      <el-tag v-for="b in brands.slice(0,6)" :key="b" @click="goProducts({brand:b})" style="cursor:pointer">{{ b }}</el-tag>
    </div>
  </div>

  <!-- 分类 -->
  <h2 style="margin-bottom:12px">手机分类</h2>
  <div style="display:flex;gap:12px;margin-bottom:24px;flex-wrap:wrap">
    <el-button v-for="c in categories" :key="c" @click="goProducts({category:c})">{{ c }}</el-button>
  </div>

  <!-- 热门推荐 -->
  <h2 style="margin-bottom:12px">热门推荐</h2>
  <div class="product-grid" style="grid-template-columns:repeat(4,1fr);margin-bottom:32px">
    <el-card v-for="p in hotProducts" :key="p.id" shadow="hover" @click="goDetail(p.id)" style="cursor:pointer">
      <img :src="p.mainImage||'https://picsum.photos/200/200'" style="width:100%;height:200px;object-fit:cover;border-radius:8px" />
      <div style="padding:8px 0"><strong>{{ p.name }}</strong></div>
      <div style="display:flex;align-items:baseline;gap:4px">
        <span class="price"><span class="unit">¥</span>{{ p.price }}</span>
        <span class="original-price" v-if="p.originalPrice > p.price">¥{{ p.originalPrice }}</span>
      </div>
      <div style="color:#999;font-size:13px;margin-top:4px">{{ p.brand }} | 月销 {{ p.sales }}</div>
    </el-card>
  </div>

  <!-- 全部商品 -->
  <h2 style="margin-bottom:12px">全部机型</h2>
  <div class="product-grid" style="grid-template-columns:repeat(4,1fr)">
    <el-card v-for="p in products" :key="p.id" shadow="hover" style="cursor:pointer">
      <img :src="p.mainImage||'https://picsum.photos/200/200'" style="width:100%;height:200px;object-fit:cover;border-radius:8px" @click="goDetail(p.id)" />
      <div style="padding:8px 0"><strong @click="goDetail(p.id)">{{ p.name }}</strong></div>
      <div style="display:flex;align-items:baseline;gap:4px">
        <span class="price"><span class="unit">¥</span>{{ p.price }}</span>
        <span class="original-price" v-if="p.originalPrice > p.price">¥{{ p.originalPrice }}</span>
      </div>
      <div style="display:flex;justify-content:space-between;align-items:center;margin-top:8px">
        <span style="color:#999;font-size:13px">月销 {{ p.sales }}</span>
        <el-button size="small" type="primary" @click.stop="quickAdd(p.id)">加入购物车</el-button>
      </div>
    </el-card>
  </div>
</template>

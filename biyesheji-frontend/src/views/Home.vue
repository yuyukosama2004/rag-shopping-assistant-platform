<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProductPage, getHotProducts, getFilters } from '../api/product'
import { addToCart } from '../api/order'
import { ElMessage } from 'element-plus'

const router = useRouter()
const products = ref<any[]>([])
const hots = ref<any[]>([])
const brands = ref<string[]>([])
const keyword = ref('')

onMounted(async () => {
  const [pr, hr, fr] = await Promise.all([
    getProductPage({ pageNum: 1, pageSize: 8, sort: 'sales' }),
    getHotProducts(4), getFilters()
  ])
  products.value = pr.data.data.records
  hots.value = hr.data.data
  brands.value = fr.data.data.brands
})

const search = () => { if (keyword.value.trim()) router.push({ path: '/products', query: { keyword: keyword.value.trim() } }) }
const goD = (id: number) => router.push(`/product/${id}`)
const goP = (f: any) => router.push({ path: '/products', query: f })
const add = async (e: Event, id: number) => { e.stopPropagation(); try { await addToCart(id); ElMessage.success('已加入购物车') } catch {} }
</script>

<template>
  <!-- Banner -->
  <div class="home-banner">
    <h1>找到最适合你的手机</h1>
    <div style="display:flex;justify-content:center">
      <div style="display:flex;max-width:550px;width:100%">
        <input v-model="keyword" placeholder="搜索手机品牌、型号..." @keyup.enter="search" style="flex:1;height:40px;border:none;padding:0 14px;font-size:15px;outline:none" />
        <button @click="search" style="width:70px;height:40px;background:#A0121A;color:#fff;border:none;font-size:18px;cursor:pointer">🔍</button>
      </div>
    </div>
    <div class="brand-tags">
      <span v-for="b in brands.slice(0,8)" :key="b" @click="goP({brand:b})">{{ b }}</span>
    </div>
  </div>

  <!-- 热门推荐 -->
  <div class="container">
    <div class="section-title">热门推荐</div>
    <div class="product-grid">
      <div class="card" v-for="p in hots" :key="p.id" @click="goD(p.id)">
        <img :src="p.mainImage||'https://picsum.photos/300/300'" :alt="p.name" />
        <div class="info">
          <div class="title">{{ p.name }}</div>
          <div class="price-row"><span class="p"><span style="font-size:12px">¥</span>{{ p.price }}</span><span class="original-price" v-if="p.originalPrice>p.price">¥{{ p.originalPrice }}</span></div>
          <div class="meta"><span>{{ p.brand }} · 月销{{ p.sales }}</span><button class="add-btn" @click="add($event,p.id)">加入购物车</button></div>
        </div>
      </div>
    </div>

    <!-- 全部机型 -->
    <div class="section-title">全部机型</div>
    <div class="product-grid">
      <div class="card" v-for="p in products" :key="p.id" @click="goD(p.id)">
        <img :src="p.mainImage||'https://picsum.photos/300/300'" :alt="p.name" />
        <div class="info">
          <div class="title">{{ p.name }}</div>
          <div class="price-row"><span class="p"><span style="font-size:12px">¥</span>{{ p.price }}</span><span class="original-price" v-if="p.originalPrice>p.price">¥{{ p.originalPrice }}</span></div>
          <div class="meta"><span>{{ p.brand }} · 月销{{ p.sales }}</span><button class="add-btn" @click="add($event,p.id)">加入购物车</button></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProductPage } from '../api/product'
// addToCart removed from Home (not used in waterfall cards)

const router = useRouter()
const products = ref<any[]>([])
const loading = ref(false)
const pageNum = ref(1)
const hasMore = ref(true)

const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  loading.value = true
  try {
    const res = await getProductPage({ pageNum: pageNum.value, pageSize: 12, sort: 'sales' })
    const data = res.data.data
    products.value.push(...data.records)
    hasMore.value = data.current < data.pages
    pageNum.value++
  } catch {} finally { loading.value = false }
}

onMounted(loadMore)

// 滚动触底加载
const onScroll = () => {
  const { scrollTop, scrollHeight, clientHeight } = document.documentElement
  if (scrollHeight - scrollTop - clientHeight < 100) loadMore()
}
onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))

const goDetail = (id: number) => router.push(`/product/${id}`)
// quickAdd removed — card click navigates to detail
</script>

<template>
  <div class="waterfall">
    <div v-for="p in products" :key="p.id" class="product-card" @click="goDetail(p.id)">
      <img :src="p.mainImage || 'https://picsum.photos/400/400'" :alt="p.name" loading="lazy" />
      <div class="card-info">
        <div class="card-title">{{ p.name }}</div>
        <div class="card-price-row">
          <span class="price-current"><span class="yen">¥</span>{{ p.price }}</span>
          <span class="price-original" v-if="p.originalPrice > p.price">¥{{ p.originalPrice }}</span>
        </div>
        <div class="card-meta">{{ p.brand }} · 月销 {{ p.sales }}</div>
      </div>
    </div>
    <div v-if="loading" class="loading-more">加载中...</div>
  </div>
</template>

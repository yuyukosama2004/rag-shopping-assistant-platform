<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductPage, getFilters } from '../api/product'

const route = useRoute()
const router = useRouter()
const products = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)
const hasMore = ref(true)
const brands = ref<string[]>([])

const filters = ref({
  brand: (route.query.brand as string) || '',
  keyword: (route.query.keyword as string) || '',
  sort: 'sales' as string,
})

const sortOptions = [
  { label: '综合', value: 'sales' },
  { label: '价格升序', value: 'price_asc' },
  { label: '价格降序', value: 'price_desc' },
]

const loadProducts = async (reset = false) => {
  if (loading.value) return
  if (reset) { pageNum.value = 1; products.value = []; hasMore.value = true }
  loading.value = true
  try {
    const res = await getProductPage({
      pageNum: pageNum.value, pageSize: 12,
      brand: filters.value.brand || undefined,
      keyword: filters.value.keyword || undefined,
      sort: filters.value.sort,
    })
    const data = res.data.data
    products.value.push(...data.records)
    hasMore.value = data.current < data.pages
    pageNum.value++
    total.value = data.total
  } catch {} finally { loading.value = false }
}

onMounted(async () => {
  const f = await getFilters()
  brands.value = f.data.data.brands
  loadProducts()
})

const setSort = (s: string) => { filters.value.sort = s; loadProducts(true) }
const setBrand = (b: string) => { filters.value.brand = b; loadProducts(true) }
const more = () => { if (hasMore.value) loadProducts() }

const goDetail = (id: number) => router.push(`/product/${id}`)
// quickAdd removed from ProductList
</script>

<template>
  <!-- 排序栏 -->
  <div class="filter-bar">
    <button v-for="s in sortOptions" :key="s.value"
      class="filter-chip" :class="{ active: filters.sort === s.value }"
      @click="setSort(s.value)">{{ s.label }}</button>
    <span style="color:var(--text-secondary);margin:0 4px">|</span>
    <button class="filter-chip" :class="{ active: !filters.brand }" @click="setBrand('')">全部</button>
    <button v-for="b in brands.slice(0,8)" :key="b"
      class="filter-chip" :class="{ active: filters.brand === b }"
      @click="setBrand(b)">{{ b }}</button>
  </div>

  <!-- 瀑布流 -->
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
    <div v-if="!loading && hasMore" class="loading-more" style="cursor:pointer" @click="more">加载更多</div>
  </div>
</template>

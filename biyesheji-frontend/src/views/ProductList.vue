<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductPage, getFilters } from '../api/product'
import { getErrorMessage } from '../api/request'

const route = useRoute(); const router = useRouter()
const products = ref<any[]>([]); const total = ref(0); const pageNum = ref(1); const brands = ref<string[]>([])
const filters = ref({ brand: (route.query.brand as string)||'', keyword: (route.query.keyword as string)||'', sort: 'sales' as string })
const loading = ref(false)
const error = ref('')
let loadSequence = 0

const load = async (reset = false) => {
  if (reset) { pageNum.value = 1; products.value = [] }
  const sequence = ++loadSequence
  loading.value = true
  error.value = ''
  try {
    const r = await getProductPage({ pageNum: 1, pageSize: 100, brand: filters.value.brand||undefined, keyword: filters.value.keyword||undefined, sort: filters.value.sort })
    if (sequence !== loadSequence) return
    products.value = r.data.data.records
    total.value = r.data.data.total
  } catch (cause) {
    if (sequence !== loadSequence) return
    error.value = getErrorMessage(cause, '商品加载失败，请稍后重试')
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

onMounted(async () => {
  try { const f = await getFilters(); brands.value = f.data.data.brands } catch { brands.value = [] }
  load()
})
watch(() => route.query, (q) => {
  filters.value.brand = (q.brand as string) || ''
  filters.value.keyword = (q.keyword as string) || ''
  load(true)
})
const goD = (id: number) => router.push(`/product/${id}`)
const add = (e: Event, id: number) => { e.stopPropagation(); goD(id) }
const setSort = (s: string) => { filters.value.sort = s; load(true) }
const setBrand = (b: string) => { filters.value.brand = b; load(true) }
</script>

<template>
  <div class="plist-wrap">
    <!-- 左侧品牌 -->
    <div class="plist-side">
      <div class="side-item" :class="{active:!filters.brand}" @click="setBrand('')">全部品牌</div>
      <div class="side-item" v-for="b in brands" :key="b" :class="{active:filters.brand===b}" @click="setBrand(b)">{{ b }}</div>
    </div>
    <!-- 右侧商品区 -->
    <div class="plist-main" v-loading="loading">
      <div class="sort-bar">
        <span :class="{active:filters.sort==='sales'}" @click="setSort('sales')">综合排序</span>
        <span :class="{active:filters.sort==='price_asc'}" @click="setSort('price_asc')">价格升序</span>
        <span :class="{active:filters.sort==='price_desc'}" @click="setSort('price_desc')">价格降序</span>
        <span style="margin-left:auto;color:#999">{{ total }} 件商品</span>
      </div>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom:10px"><template #default><el-button size="small" @click="load()">重新加载</el-button></template></el-alert>
      <el-empty v-else-if="!loading && products.length === 0" :description="filters.keyword ? `没有找到“${filters.keyword}”相关商品` : '暂无可售商品'" />
      <div v-else class="product-grid grid-3">
        <div class="card" v-for="p in products" :key="p.id" @click="goD(p.id)">
          <img :src="p.mainImage||''" :alt="p.name" />
          <div class="info">
            <div class="title">{{ p.name }}</div>
            <div class="price-row"><span class="p"><span style="font-size:12px">¥</span>{{ p.price }}</span><span class="original-price" v-if="p.originalPrice>p.price">¥{{ p.originalPrice }}</span></div>
            <div class="meta"><span>{{ p.brand }} · 月销{{ p.sales }}</span><button class="add-btn" @click="add($event,p.id)">选择规格</button></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

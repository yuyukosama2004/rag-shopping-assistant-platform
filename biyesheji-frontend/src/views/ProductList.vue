<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductPage, getFilters } from '../api/product'
import { addToCart } from '../api/order'
import { ElMessage } from 'element-plus'

const route = useRoute(); const router = useRouter()
const products = ref<any[]>([]); const total = ref(0); const pageNum = ref(1); const brands = ref<string[]>([])
const filters = ref({ brand: (route.query.brand as string)||'', keyword: (route.query.keyword as string)||'', sort: 'sales' as string })

const load = async (reset = false) => {
  if (reset) { pageNum.value = 1; products.value = [] }
  const r = await getProductPage({ pageNum: pageNum.value, pageSize: 12, brand: filters.value.brand||undefined, keyword: filters.value.keyword||undefined, sort: filters.value.sort })
  products.value = r.data.data.records; total.value = r.data.data.total
}

onMounted(async () => {
  const f = await getFilters(); brands.value = f.data.data.brands
  load()
})
watch(() => route.query, (q) => {
  filters.value.brand = (q.brand as string) || ''
  filters.value.keyword = (q.keyword as string) || ''
  load(true)
})
const goD = (id: number) => router.push(`/product/${id}`)
const add = async (e: Event, id: number) => { e.stopPropagation(); try { await addToCart(id); ElMessage.success('已加入购物车') } catch {} }
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
    <div class="plist-main">
      <div class="sort-bar">
        <span :class="{active:filters.sort==='sales'}" @click="setSort('sales')">综合排序</span>
        <span :class="{active:filters.sort==='price_asc'}" @click="setSort('price_asc')">价格升序</span>
        <span :class="{active:filters.sort==='price_desc'}" @click="setSort('price_desc')">价格降序</span>
        <span style="margin-left:auto;color:#999">{{ total }} 件商品</span>
      </div>
      <div class="product-grid" style="grid-template-columns:repeat(3,1fr)">
        <div class="card" v-for="p in products" :key="p.id" @click="goD(p.id)">
          <img :src="p.mainImage||''" :alt="p.name" />
          <div class="info">
            <div class="title">{{ p.name }}</div>
            <div class="price-row"><span class="p"><span style="font-size:12px">¥</span>{{ p.price }}</span><span class="original-price" v-if="p.originalPrice>p.price">¥{{ p.originalPrice }}</span></div>
            <div class="meta"><span>{{ p.brand }} · 月销{{ p.sales }}</span><button class="add-btn" @click="add($event,p.id)">加购</button></div>
          </div>
        </div>
      </div>
      <el-pagination v-if="total > 12" v-model:current-page="pageNum" :page-size="12" :total="total" layout="prev,pager,next" @current-change="load" />
    </div>
  </div>
</template>

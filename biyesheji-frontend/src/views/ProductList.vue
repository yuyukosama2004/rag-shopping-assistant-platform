<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductPage, getFilters } from '../api/product'
import { addToCart } from '../api/order'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const products = ref<any[]>([])
const total = ref(0)
const brands = ref<string[]>([])
const categories = ref<string[]>([])
const pageNum = ref(1)
const pageSize = 12

const filters = ref({
  brand: route.query.brand as string || '',
  category: route.query.category as string || '',
  minPrice: undefined as number | undefined,
  maxPrice: undefined as number | undefined,
  keyword: route.query.keyword as string || '',
  sort: 'sales' as string,
})

const loadProducts = async () => {
  const res = await getProductPage({ pageNum: pageNum.value, pageSize, ...filters.value })
  products.value = res.data.data.records
  total.value = res.data.data.total
}
const loadFilters = async () => {
  const res = await getFilters()
  brands.value = res.data.data.brands
  categories.value = res.data.data.categories
}

onMounted(() => { loadFilters(); loadProducts() })

watch(() => route.query, (q) => {
  Object.assign(filters.value, { brand: q.brand||'', category: q.category||'', keyword: q.keyword||'' })
  pageNum.value = 1
  loadProducts()
})

const goDetail = (id: number) => router.push(`/product/${id}`)
const quickAdd = async (id: number) => {
  try { await addToCart(id); ElMessage.success('已加入购物车') } catch {}
}
</script>

<template>
  <h1 class="page-header">全部手机</h1>
  <!-- 筛选 -->
  <el-card style="margin-bottom:16px">
    <el-form inline>
      <el-form-item label="品牌"><el-select v-model="filters.brand" clearable @change="loadProducts"><el-option v-for="b in brands" :key="b" :label="b" :value="b"/></el-select></el-form-item>
      <el-form-item label="分类"><el-select v-model="filters.category" clearable @change="loadProducts"><el-option v-for="c in categories" :key="c" :label="c" :value="c"/></el-select></el-form-item>
      <el-form-item label="价格"><el-input-number v-model="filters.minPrice" :min="0" :step="500" size="small" style="width:110px"/> - <el-input-number v-model="filters.maxPrice" :min="0" :step="500" size="small" style="width:110px;margin-left:6px"/></el-form-item>
      <el-form-item><el-button type="primary" @click="loadProducts">筛选</el-button></el-form-item>
      <el-form-item label="排序"><el-radio-group v-model="filters.sort" @change="loadProducts"><el-radio value="sales">销量</el-radio><el-radio value="price_asc">价格升序</el-radio><el-radio value="price_desc">价格降序</el-radio></el-radio-group></el-form-item>
    </el-form>
  </el-card>

  <div class="product-grid">
    <el-card v-for="p in products" :key="p.id" shadow="hover" style="cursor:pointer">
      <img :src="p.mainImage||'https://picsum.photos/200/200'" style="width:100%;height:200px;object-fit:cover;border-radius:8px" @click="goDetail(p.id)" />
      <div style="padding:8px 0"><strong @click="goDetail(p.id)">{{ p.name }}</strong></div>
      <span class="price"><span class="unit">¥</span>{{ p.price }}</span>
      <span class="original-price" v-if="p.originalPrice > p.price">¥{{ p.originalPrice }}</span>
      <div style="display:flex;justify-content:space-between;align-items:center;margin-top:8px">
        <span style="color:#999;font-size:13px">{{ p.brand }} | 月销 {{ p.sales }}</span>
        <el-button size="small" type="primary" @click.stop="quickAdd(p.id)">加购</el-button>
      </div>
    </el-card>
  </div>

  <el-pagination v-if="total > pageSize" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev,pager,next" @current-change="loadProducts" style="margin-top:24px;justify-content:center" />
</template>

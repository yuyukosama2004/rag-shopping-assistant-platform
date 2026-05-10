<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getCartCount } from '../api/order'
import { onMounted, ref, computed } from 'vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartCount = ref(0)
const searchKeyword = ref('')

const loadCartCount = async () => {
  if (!userStore.isLoggedIn()) return
  try { const r = await getCartCount(); cartCount.value = r.data.data } catch {}
}
onMounted(loadCartCount)

const activeTab = computed(() => {
  const p = route.path
  if (p === '/' || p.startsWith('/product')) return 'home'
  if (p.startsWith('/ai')) return 'ai'
  if (p.startsWith('/cart') || p.startsWith('/checkout')) return 'cart'
  if (p.startsWith('/order') || p.startsWith('/login')) return 'me'
  return 'home'
})

const tabs = [
  { key: 'home', label: '首页', icon: '🏠', path: '/' },
  { key: 'ai', label: 'AI导购', icon: '🤖', path: '/ai-assistant' },
  { key: 'cart', label: '购物车', icon: '🛒', path: '/cart' },
  { key: 'me', label: '我的', icon: '👤', path: '/orders' },
]

const goTab = (t: any) => {
  if ((t.key === 'cart' || t.key === 'me') && !userStore.isLoggedIn()) {
    return router.push('/login')
  }
  router.push(t.path)
}

const doSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/products', query: { keyword: searchKeyword.value.trim() } })
    searchKeyword.value = ''
  }
}
</script>

<template>
  <div>
    <!-- ===== 顶部橙色导航 ===== -->
    <div class="top-nav">
      <div class="nav-header">
        <span class="brand" @click="goTab(tabs[0])">PhoneMall</span>
        <div class="search-box" @click="router.push('/products')">
          <span class="search-icon">🔍</span>
          <input
            v-model="searchKeyword"
            placeholder="搜索手机品牌、型号..."
            @keyup.enter="doSearch"
          />
        </div>
      </div>
      <div class="category-row">
        <span
          v-for="cat in ['推荐','Apple','Samsung','Xiaomi','Huawei','OPPO','vivo']"
          :key="cat"
          class="category-tag"
          :class="{ active: cat === '推荐' }"
          @click="cat === '推荐' ? router.push('/') : router.push({ path: '/products', query: { brand: cat } })"
        >{{ cat }}</span>
      </div>
    </div>

    <!-- ===== 页面内容 ===== -->
    <router-view @cart-update="loadCartCount" />

    <!-- ===== 底部 TabBar ===== -->
    <div class="bottom-tab">
      <div
        v-for="t in tabs" :key="t.key"
        class="tab-item"
        :class="{ active: activeTab === t.key }"
        @click="goTab(t)"
      >
        <span class="tab-icon">{{ t.icon }}</span>
        <span>{{ t.label }}</span>
        <span
          v-if="t.key==='cart' && cartCount > 0"
          style="position:absolute;top:0;right:calc(50% - 20px);background:#FF3D00;color:#fff;font-size:10px;padding:1px 5px;border-radius:8px;min-width:16px;text-align:center"
        >{{ cartCount > 99 ? '99+' : cartCount }}</span>
      </div>
    </div>
  </div>
</template>

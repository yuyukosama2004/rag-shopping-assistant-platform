<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getCartCount } from '../api/order'
import { onMounted, ref } from 'vue'

const router = useRouter()
const userStore = useUserStore()
const cartCount = ref(0)
const keyword = ref('')

const loadCartCount = async () => {
  if (!userStore.isLoggedIn()) return
  try { const r = await getCartCount(); cartCount.value = r.data.data } catch {}
}

onMounted(loadCartCount)

const doSearch = () => {
  if (keyword.value.trim()) {
    router.push({ path: '/products', query: { keyword: keyword.value.trim() } })
    keyword.value = ''
  }
}
</script>

<template>
  <div>
    <!-- Row 1: mini-bar -->
    <div class="mini-bar">
      <div class="container">
        <div>
          <span v-if="!userStore.isLoggedIn()" class="red" @click="router.push('/login')">你好，请登录</span>
          <span v-else @click="router.push('/orders')">你好，{{ userStore.user?.nickname || userStore.user?.username }}</span>
        </div>
        <div>
          <span @click="router.push('/orders')">我的订单</span>
          <span @click="router.push('/ai-assistant')">AI导购</span>
          <span @click="router.push('/cart')">我的购物车</span>
          <span v-if="userStore.isLoggedIn()" @click="userStore.logout()">退出</span>
        </div>
      </div>
    </div>

    <!-- Row 2: header-bar -->
    <div class="header-bar">
      <div class="container">
        <div class="logo" @click="router.push('/')">PhoneMall</div>
        <div class="search-wrap">
          <input v-model="keyword" placeholder="搜索手机品牌、型号..." @keyup.enter="doSearch" />
          <button class="search-btn" @click="doSearch">🔍</button>
        </div>
        <div class="header-cart" @click="router.push('/cart')">
          🛒 我的购物车
          <span class="cart-count" v-if="cartCount > 0">{{ cartCount }}</span>
        </div>
      </div>
    </div>

    <!-- Row 3: cat-nav -->
    <div class="cat-nav">
      <div class="container">
        <span class="all-cats">全部商品分类</span>
        <a href="/products" @click.prevent="router.push('/products')">手机</a>
        <a href="#" @click.prevent="router.push({path:'/products',query:{brand:'Apple'}})">Apple</a>
        <a href="#" @click.prevent="router.push({path:'/products',query:{brand:'Samsung'}})">Samsung</a>
        <a href="#" @click.prevent="router.push({path:'/products',query:{brand:'Xiaomi'}})">Xiaomi</a>
        <a href="#" @click.prevent="router.push({path:'/products',query:{brand:'Huawei'}})">Huawei</a>
        <a href="#" @click.prevent="router.push({path:'/products',query:{brand:'OPPO'}})">OPPO</a>
        <a href="#" @click.prevent="router.push({path:'/products',query:{brand:'vivo'}})">vivo</a>
        <a href="#" @click.prevent="router.push('/ai-assistant')">AI智能导购</a>
      </div>
    </div>

    <!-- main content -->
    <div class="container" style="padding-top:14px;padding-bottom:24px;min-height:60vh">
      <router-view />
    </div>

    <!-- footer -->
    <div class="site-footer">
      <p>PhoneMall 手机电商平台 &copy; 2026 · 基于Spring Cloud微服务架构</p>
    </div>
  </div>
</template>

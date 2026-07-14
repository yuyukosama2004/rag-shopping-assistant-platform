<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getCartCount } from '../api/order'
import { onMounted, ref } from 'vue'
import { getPublicStoreSetting } from '../api/merchant'

const router = useRouter()
const userStore = useUserStore()
const cartCount = ref(0)
const keyword = ref('')
const store = ref({ storeName: 'PhoneMall', logo: '', servicePhone: '', serviceEmail: '', businessStatus: 1 })

const loadCartCount = async () => {
  if (!userStore.isLoggedIn()) return
  try { const r = await getCartCount(); cartCount.value = r.data.data } catch {}
}

onMounted(async () => {
  loadCartCount()
  try {
    const setting = (await getPublicStoreSetting()).data.data
    if (setting) Object.assign(store.value, setting)
  } catch {}
})

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
          <span v-else @click="router.push('/account')">你好，{{ userStore.user?.nickname || userStore.user?.username }}</span>
        </div>
        <div>
          <span @click="router.push('/orders')">我的订单</span>
          <span @click="router.push('/account')">我的账号</span>
          <span @click="router.push('/cart')">购物车</span>
          <span v-if="userStore.isLoggedIn()" @click="userStore.logout()">退出</span>
        </div>
      </div>
    </div>

    <!-- Row 2: header-bar -->
    <div class="header-bar">
      <div class="container">
        <div class="logo" @click="router.push('/')"><img v-if="store.logo" :src="store.logo" alt="店铺Logo" style="height:34px;max-width:120px;object-fit:contain;vertical-align:middle;margin-right:8px" />{{ store.storeName }}</div>
        <div class="search-wrap">
          <input v-model="keyword" placeholder="搜索手机品牌、型号..." @keyup.enter="doSearch" />
          <button class="search-btn" @click="doSearch">🔍</button>
        </div>
        <div class="hdr-btn-red" @click="router.push('/ai-assistant')">AI导购</div>
        <div style="position:relative;flex-shrink:0" @click="router.push('/cart')">
          <div class="hdr-btn">🛒 购物车</div>
          <span class="cart-count" v-if="cartCount > 0" style="position:absolute;top:-6px;right:-4px;background:var(--jd-red);color:#fff;font-size:11px;padding:0 5px;border-radius:10px">{{ cartCount }}</span>
        </div>
      </div>
    </div>

    <!-- Row 3: cat-nav -->
    <div v-if="store.businessStatus === 0" style="background:#fff7e6;color:#d46b08;text-align:center;padding:8px">店铺当前休息中，暂不接受新订单。</div>
    <div class="cat-nav">
      <div class="container">
        <span class="cat-link" @click="router.push('/')">首页</span>
        <span class="cat-link" @click="router.push({path:'/products'})">全部品牌</span>
        <span class="cat-link" @click="router.push({path:'/products',query:{brand:'Huawei'}})">华为</span>
        <span class="cat-link" @click="router.push({path:'/products',query:{brand:'Apple'}})">苹果</span>
        <span class="cat-link" @click="router.push({path:'/products',query:{brand:'Samsung'}})">三星</span>
        <span class="cat-link" @click="router.push({path:'/products',query:{brand:'vivo'}})">vivo</span>
        <span class="cat-link" @click="router.push({path:'/products',query:{brand:'OPPO'}})">OPPO</span>
        <span class="cat-link" @click="router.push({path:'/products',query:{brand:'Xiaomi'}})">小米</span>
        <span class="cat-link" @click="router.push({path:'/products',query:{brand:'Honor'}})">荣耀</span>
        <span class="cat-link" @click="router.push({path:'/products',query:{brand:'iQOO'}})">iQOO</span>
        <span class="cat-link" @click="router.push({path:'/products',query:{brand:'Redmi'}})">Redmi</span>
        <span class="cat-link" @click="router.push({path:'/products',query:{brand:'OnePlus'}})">一加</span>
        <span class="cat-link" @click="router.push({path:'/products',query:{brand:'realme'}})">realme</span>
        <span class="cat-link" @click="router.push({path:'/products',query:{brand:'Nubia'}})">红魔</span>
        <span class="cat-link" @click="router.push({path:'/products',query:{brand:'Motorola'}})">摩托罗拉</span>
      </div>
    </div>

    <!-- main content -->
    <div class="container" style="padding-top:14px;padding-bottom:24px;min-height:80vh">
      <router-view />
    </div>

    <!-- footer -->
    <div class="site-footer">
      <p>{{ store.storeName }} &copy; 2026 <span v-if="store.servicePhone">· 客服电话：{{ store.servicePhone }}</span><span v-if="store.serviceEmail">· {{ store.serviceEmail }}</span></p>
    </div>
  </div>
</template>

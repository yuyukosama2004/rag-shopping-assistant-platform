<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getCartCount } from '../api/order'
import { onMounted, ref } from 'vue'

const router = useRouter()
const userStore = useUserStore()
const cartCount = ref(0)

const loadCartCount = async () => {
  if (!userStore.isLoggedIn()) return
  try { const r = await getCartCount(); cartCount.value = r.data.data } catch {}
}

onMounted(loadCartCount)

const goHome = () => router.push('/')
const goLogin = () => router.push('/login')
const goCart = () => router.push('/cart')
const goOrders = () => router.push('/orders')
const goAi = () => router.push('/ai-assistant')
const logout = () => userStore.logout()
</script>

<template>
  <div>
    <el-menu mode="horizontal" :ellipsis="false" router>
      <el-menu-item index="/" @click="goHome">
        <el-icon><Shop /></el-icon>
        <span style="font-weight:700;font-size:18px">PhoneMall</span>
      </el-menu-item>
      <div style="flex-grow:1" />
      <el-menu-item index="/ai-assistant" @click="goAi">
        <el-icon><ChatDotRound /></el-icon> AI导购
      </el-menu-item>
      <el-menu-item v-if="userStore.isLoggedIn()" @click="goCart">
        <el-icon><ShoppingCart /></el-icon>
        购物车
        <el-badge v-if="cartCount > 0" :value="cartCount" style="margin-left:4px" />
      </el-menu-item>
      <el-menu-item v-if="userStore.isLoggedIn()" @click="goOrders">我的订单</el-menu-item>
      <el-menu-item v-if="userStore.isLoggedIn()" @click="logout">退出</el-menu-item>
      <el-menu-item v-else index="/login" @click="goLogin">登录</el-menu-item>
    </el-menu>
    <main class="container" style="padding-top:20px; padding-bottom:40px">
      <router-view />
    </main>
  </div>
</template>

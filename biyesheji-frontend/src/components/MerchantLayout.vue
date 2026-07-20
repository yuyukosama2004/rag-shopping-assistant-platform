<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
</script>

<template>
  <el-container class="merchant-shell">
    <el-aside width="220px" class="merchant-aside">
      <div class="merchant-title">商家工作台</div>
      <el-menu class="merchant-menu" router background-color="#1f2937" text-color="#cbd5e1" active-text-color="#fff" :default-active="router.currentRoute.value.path">
        <el-menu-item index="/merchant"><el-icon><HomeFilled /></el-icon><span>概览</span></el-menu-item>
        <el-menu-item v-if="userStore.user?.role === 1" index="/merchant/store"><el-icon><Setting /></el-icon><span>店铺设置</span></el-menu-item>
        <el-menu-item v-if="userStore.user?.role === 1" index="/merchant/staff"><el-icon><User /></el-icon><span>店员管理</span></el-menu-item>
        <el-menu-item index="/merchant/catalog"><el-icon><Collection /></el-icon><span>品牌与分类</span></el-menu-item>
        <el-menu-item index="/merchant/products"><el-icon><Goods /></el-icon><span>商品管理</span></el-menu-item>
        <el-menu-item index="/merchant/inventory"><el-icon><Box /></el-icon><span>库存管理</span></el-menu-item>
        <el-menu-item index="/merchant/ai-operations"><el-icon><DataAnalysis /></el-icon><span>AI 经营助手</span></el-menu-item>
        <el-menu-item index="/merchant/shipping-rules"><el-icon><Van /></el-icon><span>配送规则</span></el-menu-item>
        <el-menu-item index="/merchant/orders"><el-icon><Tickets /></el-icon><span>订单管理</span></el-menu-item>
        <el-menu-item index="/merchant/refunds"><el-icon><RefreshLeft /></el-icon><span>退款管理</span></el-menu-item>
        <el-menu-item index="/merchant/notifications"><el-icon><Bell /></el-icon><span>通知记录</span></el-menu-item>
        <el-menu-item index="/merchant/ai"><el-icon><ChatDotRound /></el-icon><span>AI 导购</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="merchant-header">
        <span>单店自托管商城</span>
        <div>
          <span style="margin-right:16px">{{ userStore.user?.nickname || userStore.user?.username }}</span>
          <el-button text @click="router.push('/')">查看商城</el-button>
          <el-button text type="danger" @click="userStore.logout()">退出</el-button>
        </div>
      </el-header>
      <el-main class="merchant-main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.merchant-shell{min-height:100vh;background:#f5f7fa}.merchant-aside{background:#1f2937;color:#fff}.merchant-title{padding:24px 20px;font-size:18px;font-weight:700}.merchant-header{display:flex;align-items:center;justify-content:space-between;background:#fff;border-bottom:1px solid #e5e7eb}
@media(max-width:767px){.merchant-shell{display:block}.merchant-aside{width:100%!important}.merchant-title{padding:12px 14px}.merchant-menu{display:flex;overflow-x:auto;border-right:0}.merchant-menu :deep(.el-menu-item){flex:0 0 auto;height:44px;padding:0 14px}.merchant-header{height:auto;min-height:52px;padding:8px 12px;gap:8px;flex-wrap:wrap}.merchant-main{padding:12px}}
</style>

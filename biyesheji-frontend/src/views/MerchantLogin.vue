<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const form = ref({ username: '', password: '' })

const handle = async () => {
  try {
    await userStore.login(form.value.username, form.value.password)
    if (userStore.user?.role !== 1) {
      userStore.clearSession()
      ElMessage.error('此账号没有店主权限')
      return
    }
    ElMessage.success('店主登录成功')
    router.push('/merchant')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || e.message || '登录失败')
  }
}
</script>

<template>
  <div style="display:flex;min-height:100vh;align-items:center;justify-content:center;background:#f3f4f6">
    <div style="width:400px;background:#fff;padding:36px;border-radius:8px;box-shadow:0 8px 24px rgb(0 0 0 / 8%)">
      <h1 style="margin:0 0 8px;font-size:24px">商家工作台</h1>
      <p style="margin:0 0 24px;color:#6b7280">仅限本店店主登录</p>
      <el-form :model="form" size="large" @submit.prevent="handle">
        <el-form-item><el-input v-model="form.username" autocomplete="username" placeholder="店主用户名" /></el-form-item>
        <el-form-item><el-input v-model="form.password" autocomplete="current-password" type="password" placeholder="密码" show-password /></el-form-item>
        <el-button type="primary" native-type="submit" style="width:100%" @click="handle">登录工作台</el-button>
      </el-form>
      <el-button text style="margin-top:16px" @click="router.push('/')">返回消费者商城</el-button>
    </div>
  </div>
</template>

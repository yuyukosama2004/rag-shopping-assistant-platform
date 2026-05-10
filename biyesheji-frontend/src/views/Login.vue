<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const form = ref({ username: '', password: '' })

const handleLogin = async () => {
  try {
    await userStore.login(form.value.username, form.value.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '登录失败') }
}
</script>

<template>
  <div style="max-width:400px;margin:80px auto">
    <h2 style="text-align:center;margin-bottom:24px">登录 PhoneMall</h2>
    <el-form :model="form" size="large">
      <el-form-item><el-input v-model="form.username" placeholder="用户名" /></el-form-item>
      <el-form-item><el-input v-model="form.password" type="password" placeholder="密码" show-password /></el-form-item>
      <el-form-item><el-button type="primary" @click="handleLogin" style="width:100%">登录</el-button></el-form-item>
    </el-form>
    <p style="text-align:center">还没有账号？<router-link to="/register" style="color:#409EFF">立即注册</router-link></p>
  </div>
</template>

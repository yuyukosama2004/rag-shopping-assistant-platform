<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/user'

const router = useRouter()
const form = ref({ username: '', password: '', password2: '', nickname: '', phone: '' })

const handleRegister = async () => {
  if (!form.value.username || !form.value.password) return ElMessage.warning('请填写用户名和密码')
  if (form.value.password !== form.value.password2) return ElMessage.warning('两次密码不一致')
  try {
    await register(form.value)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '注册失败') }
}
</script>

<template>
  <div style="max-width:400px;margin:80px auto">
    <h2 style="text-align:center;margin-bottom:24px">注册账号</h2>
    <el-form :model="form" size="large">
      <el-form-item><el-input v-model="form.username" placeholder="用户名 *" /></el-form-item>
      <el-form-item><el-input v-model="form.nickname" placeholder="昵称" /></el-form-item>
      <el-form-item><el-input v-model="form.phone" placeholder="手机号" /></el-form-item>
      <el-form-item><el-input v-model="form.password" type="password" placeholder="密码 *" show-password /></el-form-item>
      <el-form-item><el-input v-model="form.password2" type="password" placeholder="确认密码 *" show-password /></el-form-item>
      <el-form-item><el-button type="primary" @click="handleRegister" style="width:100%">注册</el-button></el-form-item>
    </el-form>
    <p style="text-align:center">已有账号？<router-link to="/login" style="color:#409EFF">去登录</router-link></p>
  </div>
</template>

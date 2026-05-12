<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
const router = useRouter(); const userStore = useUserStore()
const form = ref({ username: '', password: '' })
const handle = async () => {
  try { await userStore.login(form.value.username, form.value.password); ElMessage.success('登录成功'); router.push('/') }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '登录失败') }
}
</script>

<template>
  <div style="display:flex;justify-content:center;padding:60px 0">
    <div style="max-width:400px;width:100%;background:#fff;padding:40px 30px">
      <div style="background:var(--jd-red);color:#fff;text-align:center;padding:16px;margin:-40px -30px 30px;font-size:18px;font-weight:600">PhoneMall 登录</div>
      <el-form :model="form" size="large">
        <el-form-item><el-input v-model="form.username" placeholder="用户名" /></el-form-item>
        <el-form-item><el-input v-model="form.password" type="password" placeholder="密码" show-password /></el-form-item>
        <el-form-item><el-button type="danger" @click="handle" style="width:100%">登 录</el-button></el-form-item>
      </el-form>
      <p style="text-align:center;font-size:13px;color:#999">还没有账号？<router-link to="/register" style="color:var(--jd-red)">立即注册</router-link></p>
    </div>
  </div>
</template>

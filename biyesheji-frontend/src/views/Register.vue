<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/user'
const router = useRouter()
const form = ref({ username: '', password: '', password2: '', nickname: '' })
const submit = async () => {
  if (!form.value.username || !form.value.password) return ElMessage.warning('请填写用户名和密码')
  if (form.value.password !== form.value.password2) return ElMessage.warning('两次密码不一致')
  try { await register(form.value); ElMessage.success('注册成功'); router.push('/login') }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '注册失败') }
}
</script>
<template>
  <div style="display:flex;justify-content:center;padding:60px 0">
    <div style="max-width:400px;width:100%;background:#fff;padding:40px 30px">
      <div style="background:var(--jd-red);color:#fff;text-align:center;padding:16px;margin:-40px -30px 30px;font-size:18px;font-weight:600">注册账号</div>
      <el-form :model="form" size="large">
        <el-form-item><el-input v-model="form.username" placeholder="用户名" /></el-form-item>
        <el-form-item><el-input v-model="form.nickname" placeholder="昵称" /></el-form-item>
        <el-form-item><el-input v-model="form.password" type="password" placeholder="密码" show-password /></el-form-item>
        <el-form-item><el-input v-model="form.password2" type="password" placeholder="确认密码" show-password /></el-form-item>
        <el-form-item><el-button type="danger" @click="submit" style="width:100%">注 册</el-button></el-form-item>
      </el-form>
      <p style="text-align:center;font-size:13px;color:#999">已有账号？<router-link to="/login" style="color:var(--jd-red)">去登录</router-link></p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createStaff, getStaffList, updateStaffStatus, type Staff } from '../api/merchant'

const loading = ref(false)
const creating = ref(false)
const dialogVisible = ref(false)
const staff = ref<Staff[]>([])
const form = reactive({ username: '', password: '', nickname: '', phone: '', email: '' })

const load = async () => {
  loading.value = true
  try { staff.value = (await getStaffList()).data.data } finally { loading.value = false }
}

const create = async () => {
  if (!form.username.trim() || !form.password) return ElMessage.warning('请填写用户名和初始密码')
  creating.value = true
  try {
    await createStaff(form)
    ElMessage.success('店员已创建')
    Object.assign(form, { username: '', password: '', nickname: '', phone: '', email: '' })
    dialogVisible.value = false
    await load()
  } finally { creating.value = false }
}

const toggleStatus = async (item: Staff) => {
  await updateStaffStatus(item.id, item.status === 1 ? 0 : 1)
  ElMessage.success(item.status === 1 ? '店员已禁用' : '店员已启用')
  await load()
}

onMounted(load)
</script>

<template>
  <el-card v-loading="loading">
    <template #header><div style="display:flex;justify-content:space-between;align-items:center"><strong>店员管理</strong><el-button type="primary" @click="dialogVisible = true">新增店员</el-button></div></template>
    <el-table :data="staff">
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column label="状态"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="120"><template #default="{ row }"><el-button text :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button></template></el-table-column>
    </el-table>
  </el-card>
  <el-dialog v-model="dialogVisible" title="新增店员" width="480px">
    <el-form label-width="90px" @submit.prevent="create">
      <el-form-item label="用户名" required><el-input v-model="form.username" /></el-form-item>
      <el-form-item label="初始密码" required><el-input v-model="form.password" type="password" show-password /></el-form-item>
      <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      <el-form-item><el-button type="primary" :loading="creating" @click="create">创建</el-button></el-form-item>
    </el-form>
  </el-dialog>
</template>

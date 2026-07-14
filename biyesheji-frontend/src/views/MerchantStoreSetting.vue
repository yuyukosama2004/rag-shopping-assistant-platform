<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMerchantStoreSetting, updateMerchantStoreSetting, type StoreSetting } from '../api/merchant'

const loading = ref(false)
const saving = ref(false)
const form = reactive<StoreSetting>({ storeName: '', businessStatus: 1 })

const load = async () => {
  loading.value = true
  try {
    const data = (await getMerchantStoreSetting()).data.data
    Object.assign(form, data || { storeName: '', businessStatus: 1 })
  } finally {
    loading.value = false
  }
}

const save = async () => {
  if (!form.storeName.trim()) {
    ElMessage.warning('请填写店铺名称')
    return
  }
  saving.value = true
  try {
    await updateMerchantStoreSetting(form)
    ElMessage.success('店铺设置已保存')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <el-card v-loading="loading" style="max-width:760px">
    <template #header><strong>店铺设置</strong></template>
    <el-form label-width="110px" @submit.prevent="save">
      <el-form-item label="店铺名称" required><el-input v-model="form.storeName" maxlength="100" show-word-limit /></el-form-item>
      <el-form-item label="营业状态"><el-switch v-model="form.businessStatus" :active-value="1" :inactive-value="0" active-text="营业中" inactive-text="休息中" /></el-form-item>
      <el-form-item label="Logo 地址"><el-input v-model="form.logo" placeholder="https://..." /></el-form-item>
      <el-form-item label="客服电话"><el-input v-model="form.servicePhone" /></el-form-item>
      <el-form-item label="客服邮箱"><el-input v-model="form.serviceEmail" /></el-form-item>
      <el-form-item label="店铺地址"><el-input v-model="form.address" /></el-form-item>
      <el-form-item label="配送说明"><el-input v-model="form.shippingNotice" type="textarea" :rows="3" maxlength="2000" show-word-limit /></el-form-item>
      <el-form-item label="售后说明"><el-input v-model="form.afterSalesNotice" type="textarea" :rows="3" maxlength="2000" show-word-limit /></el-form-item>
      <el-form-item><el-button type="primary" :loading="saving" @click="save">保存设置</el-button></el-form-item>
    </el-form>
  </el-card>
</template>

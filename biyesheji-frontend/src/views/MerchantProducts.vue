<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createMerchantProduct, getMerchantProducts, updateMerchantProduct, updateMerchantProductStatus, type MerchantProduct, type MerchantProductInput } from '../api/merchant'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const items = ref<MerchantProduct[]>([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const editingId = ref<number | null>(null)
const form = reactive<MerchantProductInput>({ name: '', brand: '', category: '', price: null, originalPrice: null, mainImage: '', description: '' })

const load = async () => {
  loading.value = true
  try {
    const data = (await getMerchantProducts(page.value, 20, keyword.value)).data.data
    items.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
const openCreate = () => { editingId.value = null; Object.assign(form, { name: '', brand: '', category: '', price: null, originalPrice: null, mainImage: '', description: '' }); dialogVisible.value = true }
const openEdit = (item: MerchantProduct) => { editingId.value = item.id; Object.assign(form, item); dialogVisible.value = true }
const save = async () => {
  if (!form.name.trim() || !form.brand.trim() || !form.category.trim() || !form.price) return ElMessage.warning('请填写名称、品牌、分类和售价')
  saving.value = true
  try {
    if (editingId.value) await updateMerchantProduct(editingId.value, form); else await createMerchantProduct(form)
    ElMessage.success(editingId.value ? '商品已保存' : '草稿已创建'); dialogVisible.value = false; await load()
  } finally { saving.value = false }
}
const toggleStatus = async (item: MerchantProduct) => {
  const target = item.status === 1 ? 0 : 1
  await updateMerchantProductStatus(item.id, target)
  ElMessage.success(target === 1 ? '商品已上架' : '商品已下架'); await load()
}
const statusText = (status: number) => status === 1 ? '已上架' : status === 2 ? '草稿' : '已下架'
const statusType = (status: number) => status === 1 ? 'success' : status === 2 ? 'info' : 'warning'
onMounted(load)
</script>

<template>
  <el-card v-loading="loading">
    <template #header><div style="display:flex;justify-content:space-between;align-items:center"><strong>商品管理</strong><div><el-input v-model="keyword" placeholder="搜索商品或品牌" style="width:220px;margin-right:8px" @keyup.enter="load" /><el-button @click="load">搜索</el-button><el-button type="primary" @click="openCreate">新建商品</el-button></div></div></template>
    <el-table :data="items">
      <el-table-column prop="name" label="商品" min-width="180" />
      <el-table-column prop="brand" label="品牌" width="110" />
      <el-table-column prop="category" label="分类" width="110" />
      <el-table-column label="售价" width="110"><template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="170"><template #default="{ row }"><el-button text @click="openEdit(row)">编辑</el-button><el-button text :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="prev, pager, next" style="margin-top:16px" @current-change="load" />
  </el-card>
  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑商品' : '新建商品草稿'" width="620px">
    <el-form label-width="90px" @submit.prevent="save">
      <el-form-item label="商品名称" required><el-input v-model="form.name" /></el-form-item>
      <el-row :gutter="12"><el-col :span="12"><el-form-item label="品牌" required><el-input v-model="form.brand" /></el-form-item></el-col><el-col :span="12"><el-form-item label="分类" required><el-input v-model="form.category" /></el-form-item></el-col></el-row>
      <el-row :gutter="12"><el-col :span="12"><el-form-item label="售价" required><el-input-number v-model="form.price" :min="0.01" :precision="2" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="划线价"><el-input-number v-model="form.originalPrice" :min="0.01" :precision="2" style="width:100%" /></el-form-item></el-col></el-row>
      <el-form-item label="主图地址"><el-input v-model="form.mainImage" /></el-form-item>
      <el-form-item label="商品描述"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
      <el-form-item><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></el-form-item>
    </el-form>
  </el-dialog>
</template>

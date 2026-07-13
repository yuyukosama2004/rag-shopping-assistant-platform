<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createMerchantProduct, createMerchantSku, getMerchantProducts, getMerchantSkus, updateMerchantProduct, updateMerchantProductStatus, type MerchantProduct, type MerchantProductInput, type MerchantSku, type MerchantSkuInput } from '../api/merchant'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const items = ref<MerchantProduct[]>([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const editingId = ref<number | null>(null)
const skuDialogVisible = ref(false)
const skuSaving = ref(false)
const skuProductId = ref<number | null>(null)
const skus = ref<MerchantSku[]>([])
const form = reactive<MerchantProductInput>({ name: '', brand: '', category: '', price: null, originalPrice: null, mainImage: '', description: '' })
const skuForm = reactive<MerchantSkuInput>({ skuCode: '', specJson: '', price: null, originalPrice: null, initialStock: 0 })

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
const openSkus = async (item: MerchantProduct) => {
  skuProductId.value = item.id; Object.assign(skuForm, { skuCode: '', specJson: '', price: item.price, originalPrice: item.originalPrice || null, initialStock: 0 })
  skus.value = (await getMerchantSkus(item.id)).data.data; skuDialogVisible.value = true
}
const createSku = async () => {
  if (!skuProductId.value || !skuForm.skuCode.trim() || !skuForm.price || skuForm.initialStock === null) return ElMessage.warning('请填写SKU编码、售价和初始库存')
  skuSaving.value = true
  try {
    await createMerchantSku(skuProductId.value, skuForm); ElMessage.success('SKU已创建')
    skus.value = (await getMerchantSkus(skuProductId.value)).data.data
    Object.assign(skuForm, { skuCode: '', specJson: '', price: null, originalPrice: null, initialStock: 0 })
  } finally { skuSaving.value = false }
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
      <el-table-column label="操作" width="230"><template #default="{ row }"><el-button text @click="openEdit(row)">编辑</el-button><el-button text @click="openSkus(row)">SKU</el-button><el-button text :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button></template></el-table-column>
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
  <el-dialog v-model="skuDialogVisible" title="SKU 与初始库存" width="720px">
    <el-table :data="skus" size="small" style="margin-bottom:18px"><el-table-column prop="skuCode" label="SKU编码" /><el-table-column prop="specJson" label="规格" /><el-table-column label="售价"><template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template></el-table-column></el-table>
    <el-form label-width="90px" @submit.prevent="createSku">
      <el-row :gutter="12"><el-col :span="12"><el-form-item label="SKU编码" required><el-input v-model="skuForm.skuCode" /></el-form-item></el-col><el-col :span="12"><el-form-item label="初始库存" required><el-input-number v-model="skuForm.initialStock" :min="0" style="width:100%" /></el-form-item></el-col></el-row>
      <el-row :gutter="12"><el-col :span="12"><el-form-item label="售价" required><el-input-number v-model="skuForm.price" :min="0.01" :precision="2" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="划线价"><el-input-number v-model="skuForm.originalPrice" :min="0.01" :precision="2" style="width:100%" /></el-form-item></el-col></el-row>
      <el-form-item label="规格JSON"><el-input v-model="skuForm.specJson" placeholder='例如 {"颜色":"黑色","容量":"128GB"}' /></el-form-item>
      <el-form-item><el-button type="primary" :loading="skuSaving" @click="createSku">新增SKU</el-button></el-form-item>
    </el-form>
  </el-dialog>
</template>

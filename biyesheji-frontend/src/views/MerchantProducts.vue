<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adjustMerchantSkuStock, copyMerchantProduct, createMerchantProduct, createMerchantSku, deleteMerchantMedia, deleteMerchantProduct, exportMerchantProducts, getMerchantCatalog, getMerchantProducts, getMerchantSkus, getMerchantSkuStock, getMerchantSkuStockLedger, importMerchantProducts, updateMerchantProduct, updateMerchantProductBatchStatus, updateMerchantProductStatus, updateMerchantSku, uploadMerchantMedia, type MerchantProduct, type MerchantProductInput, type MerchantSku, type MerchantSkuInput, type MerchantSkuStock, type MerchantStockLedger, type ProductCatalog } from '../api/merchant'

const loading = ref(false)
const saving = ref(false)
const uploadingImage = ref(false)
const importing = ref(false)
const importErrors = ref<Array<{ line: number, message: string }>>([])
const dialogVisible = ref(false)
const items = ref<MerchantProduct[]>([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const selectedIds = ref<number[]>([])
const editingId = ref<number | null>(null)
const skuDialogVisible = ref(false)
const skuSaving = ref(false)
const skuProductId = ref<number | null>(null)
const editingSkuId = ref<number | null>(null)
const skus = ref<MerchantSku[]>([])
const stockDialogVisible = ref(false)
const stockLoading = ref(false)
const stockSaving = ref(false)
const selectedSku = ref<MerchantSku | null>(null)
const stock = ref<MerchantSkuStock | null>(null)
const stockLedgers = ref<MerchantStockLedger[]>([])
const brands = ref<ProductCatalog[]>([])
const categories = ref<ProductCatalog[]>([])
const imageUrls = ref<string[]>([])
const removedMedia = ref<string[]>([])
const form = reactive<MerchantProductInput>({ name: '', brand: '', category: '', price: null, originalPrice: null, mainImage: '', images: '[]', description: '' })
const skuForm = reactive<MerchantSkuInput>({ skuCode: '', specJson: '', price: null, originalPrice: null, initialStock: 0 })
const stockForm = reactive({ quantity: null as number | null, reason: '' })

const load = async () => {
  loading.value = true
  try {
    const data = (await getMerchantProducts(page.value, 20, keyword.value)).data.data
    items.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
const loadCatalogs = async () => {
  const [brandResponse, categoryResponse] = await Promise.all([getMerchantCatalog('BRAND'), getMerchantCatalog('CATEGORY')])
  brands.value = brandResponse.data.data.filter((item: ProductCatalog) => item.status === 1)
  categories.value = categoryResponse.data.data.filter((item: ProductCatalog) => item.status === 1)
}
const parseImages = (value?: string) => {
  try { const parsed = JSON.parse(value || '[]'); return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : [] } catch { return [] }
}
const resetImages = (value?: string) => { imageUrls.value = parseImages(value); removedMedia.value = [] }
const openCreate = () => { editingId.value = null; Object.assign(form, { name: '', brand: '', category: '', price: null, originalPrice: null, mainImage: '', images: '[]', description: '' }); resetImages(); dialogVisible.value = true }
const openEdit = (item: MerchantProduct) => { editingId.value = item.id; Object.assign(form, item); resetImages(item.images); if (form.mainImage && !imageUrls.value.includes(form.mainImage)) imageUrls.value.unshift(form.mainImage); dialogVisible.value = true }
const save = async () => {
  if (!form.name.trim() || !form.brand.trim() || !form.category.trim() || !form.price) return ElMessage.warning('请填写名称、品牌、分类和售价')
  saving.value = true
  try {
    const payload = { ...form, images: JSON.stringify(imageUrls.value) }
    if (editingId.value) await updateMerchantProduct(editingId.value, payload); else await createMerchantProduct(payload)
    const deletionResults = await Promise.allSettled(removedMedia.value.map(filename => deleteMerchantMedia(filename)))
    if (deletionResults.some(result => result.status === 'rejected')) ElMessage.warning('图片已从商品移除，但仍被其他商品引用，文件会继续保留')
    ElMessage.success(editingId.value ? '商品已保存' : '草稿已创建'); dialogVisible.value = false; await load()
  } finally { saving.value = false }
}
const toggleStatus = async (item: MerchantProduct) => {
  const target = item.status === 1 ? 0 : 1
  await updateMerchantProductStatus(item.id, target)
  ElMessage.success(target === 1 ? '商品已上架' : '商品已下架'); await load()
}
const copyProduct = async (item: MerchantProduct) => { await copyMerchantProduct(item.id); ElMessage.success('商品副本已创建，请补充 SKU 后上架'); await load() }
const deleteProduct = async (item: MerchantProduct) => { await ElMessageBox.confirm(`确定删除“${item.name}”吗？`); await deleteMerchantProduct(item.id); ElMessage.success('商品已删除'); await load() }
const updateBatchStatus = async (status: number) => {
  if (!selectedIds.value.length) return ElMessage.warning('请先选择商品')
  await updateMerchantProductBatchStatus(selectedIds.value, status); ElMessage.success('批量状态已更新'); selectedIds.value = []; await load()
}
const uploadMainImage = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  uploadingImage.value = true
  try {
    const url = (await uploadMerchantMedia(file)).data.data.url
    imageUrls.value.push(url)
    if (!form.mainImage) form.mainImage = url
    ElMessage.success('图片已上传')
  } finally { uploadingImage.value = false }
}
const selectMainImage = (url: string) => { form.mainImage = url }
const removeImage = (url: string) => {
  imageUrls.value = imageUrls.value.filter(item => item !== url)
  if (form.mainImage === url) form.mainImage = imageUrls.value[0] || ''
  const filename = url.startsWith('/api/media/') ? url.substring(url.lastIndexOf('/') + 1) : ''
  if (filename && !removedMedia.value.includes(filename)) removedMedia.value.push(filename)
}
const importCsv = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  importing.value = true
  importErrors.value = []
  try {
    const result = (await importMerchantProducts(file)).data.data
    importErrors.value = result.errors || []
    if (importErrors.value.length) return ElMessage.warning(`导入未执行：请修复 ${importErrors.value.length} 行错误后重试`)
    ElMessage.success(`已导入${result.products}个商品、${result.skus}个SKU`)
    await load()
  } finally { importing.value = false; input.value = '' }
}
const exportCsv = async () => { const response = await exportMerchantProducts(); const url = URL.createObjectURL(response.data); const link = document.createElement('a'); link.href = url; link.download = 'products.csv'; link.click(); URL.revokeObjectURL(url) }
const onSelectionChange = (rows: MerchantProduct[]) => { selectedIds.value = rows.map(row => row.id) }
const openSkus = async (item: MerchantProduct) => {
  skuProductId.value = item.id; editingSkuId.value = null; Object.assign(skuForm, { skuCode: '', specJson: '', price: item.price, originalPrice: item.originalPrice || null, initialStock: 0 })
  skus.value = (await getMerchantSkus(item.id)).data.data; skuDialogVisible.value = true
}
const createSku = async () => {
  if (!skuProductId.value || !skuForm.skuCode.trim() || !skuForm.price || skuForm.initialStock === null) return ElMessage.warning('请填写SKU编码、售价和初始库存')
  skuSaving.value = true
  try {
    if (editingSkuId.value) await updateMerchantSku(editingSkuId.value, { ...skuForm, status: 1 }); else await createMerchantSku(skuProductId.value, skuForm)
    ElMessage.success(editingSkuId.value ? 'SKU已保存' : 'SKU已创建')
    skus.value = (await getMerchantSkus(skuProductId.value)).data.data
    editingSkuId.value = null; Object.assign(skuForm, { skuCode: '', specJson: '', price: null, originalPrice: null, initialStock: 0 })
  } finally { skuSaving.value = false }
}
const editSku = (sku: MerchantSku) => { editingSkuId.value = sku.id; Object.assign(skuForm, { ...sku, initialStock: 0 }) }
const loadStock = async (skuId: number) => {
  const [stockResponse, ledgerResponse] = await Promise.all([getMerchantSkuStock(skuId), getMerchantSkuStockLedger(skuId)])
  stock.value = stockResponse.data.data
  stockLedgers.value = ledgerResponse.data.data
}
const openStock = async (sku: MerchantSku) => {
  selectedSku.value = sku
  Object.assign(stockForm, { quantity: null, reason: '' })
  stockLoading.value = true
  stockDialogVisible.value = true
  try { await loadStock(sku.id) } finally { stockLoading.value = false }
}
const adjustStock = async () => {
  if (!selectedSku.value || !stockForm.quantity || !stockForm.reason.trim()) return ElMessage.warning('请输入非零调整数量和调整原因')
  stockSaving.value = true
  try {
    await adjustMerchantSkuStock(selectedSku.value.id, stockForm.quantity, stockForm.reason.trim())
    ElMessage.success('库存已调整')
    Object.assign(stockForm, { quantity: null, reason: '' })
    await loadStock(selectedSku.value.id)
  } finally { stockSaving.value = false }
}
const statusText = (status: number) => status === 1 ? '已上架' : status === 2 ? '草稿' : '已下架'
const statusType = (status: number) => status === 1 ? 'success' : status === 2 ? 'info' : 'warning'
onMounted(async () => { await Promise.all([load(), loadCatalogs()]) })
</script>

<template>
  <el-card v-loading="loading">
    <template #header><div style="display:flex;justify-content:space-between;align-items:center"><strong>商品管理</strong><div><el-input v-model="keyword" placeholder="搜索商品或品牌" style="width:180px;margin-right:8px" @keyup.enter="load" /><el-button @click="load">搜索</el-button><el-button @click="exportCsv">导出CSV</el-button><label><el-button :loading="importing">导入CSV</el-button><input type="file" accept=".csv,text/csv" style="display:none" @change="importCsv" /></label><el-button type="primary" @click="openCreate">新建商品</el-button></div></div></template>
    <div style="margin-bottom:12px"><el-button size="small" :disabled="!selectedIds.length" @click="updateBatchStatus(1)">批量上架</el-button><el-button size="small" :disabled="!selectedIds.length" @click="updateBatchStatus(0)">批量下架</el-button></div>
    <el-alert v-if="importErrors.length" title="CSV 导入未执行，请修复以下行后重新导入" type="warning" :closable="false" style="margin-bottom:12px"><template #default><div v-for="error in importErrors" :key="`${error.line}-${error.message}`">第 {{ error.line }} 行：{{ error.message }}</div></template></el-alert>
    <el-table :data="items" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="42" />
      <el-table-column prop="name" label="商品" min-width="180" />
      <el-table-column prop="brand" label="品牌" width="110" />
      <el-table-column prop="category" label="分类" width="110" />
      <el-table-column label="售价" width="110"><template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="290"><template #default="{ row }"><el-button text @click="openEdit(row)">编辑</el-button><el-button text @click="openSkus(row)">SKU</el-button><el-button text @click="copyProduct(row)">复制</el-button><el-button text :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button><el-button text type="danger" @click="deleteProduct(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="prev, pager, next" style="margin-top:16px" @current-change="load" />
  </el-card>
  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑商品' : '新建商品草稿'" width="620px">
    <el-form label-width="90px" @submit.prevent="save">
      <el-form-item label="商品名称" required><el-input v-model="form.name" /></el-form-item>
      <el-row :gutter="12"><el-col :span="12"><el-form-item label="品牌" required><el-select v-model="form.brand" filterable style="width:100%"><el-option v-for="item in brands" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="分类" required><el-select v-model="form.category" filterable style="width:100%"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col></el-row>
      <el-row :gutter="12"><el-col :span="12"><el-form-item label="售价" required><el-input-number v-model="form.price" :min="0.01" :precision="2" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="划线价"><el-input-number v-model="form.originalPrice" :min="0.01" :precision="2" style="width:100%" /></el-form-item></el-col></el-row>
      <el-form-item label="商品图片"><input type="file" accept="image/png,image/jpeg" :disabled="uploadingImage" @change="uploadMainImage" /><div v-if="imageUrls.length" style="display:flex;gap:8px;flex-wrap:wrap;margin-top:8px"><div v-for="url in imageUrls" :key="url" style="position:relative"><img :src="url" :style="{ width: '88px', height: '88px', objectFit: 'cover', cursor: 'pointer', border: form.mainImage === url ? '2px solid #f56c6c' : '1px solid #dcdfe6' }" @click="selectMainImage(url)" /><el-button size="small" text type="danger" style="display:block;margin:auto" @click="removeImage(url)">移除</el-button></div></div><div style="font-size:12px;color:#909399;margin-top:6px">点击图片设为主图；图片按当前顺序展示。</div></el-form-item>
      <el-form-item label="商品描述"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
      <el-form-item><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></el-form-item>
    </el-form>
  </el-dialog>
  <el-dialog v-model="skuDialogVisible" title="SKU 与初始库存" width="720px">
    <el-table :data="skus" size="small" style="margin-bottom:18px"><el-table-column prop="skuCode" label="SKU编码" /><el-table-column prop="specJson" label="规格" /><el-table-column label="售价"><template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template></el-table-column><el-table-column label="操作" width="130"><template #default="{ row }"><el-button text @click="editSku(row)">编辑</el-button><el-button text @click="openStock(row)">库存</el-button></template></el-table-column></el-table>
    <el-form label-width="90px" @submit.prevent="createSku">
      <el-row :gutter="12"><el-col :span="12"><el-form-item label="SKU编码" required><el-input v-model="skuForm.skuCode" /></el-form-item></el-col><el-col :span="12"><el-form-item v-if="!editingSkuId" label="初始库存" required><el-input-number v-model="skuForm.initialStock" :min="0" style="width:100%" /></el-form-item></el-col></el-row>
      <el-row :gutter="12"><el-col :span="12"><el-form-item label="售价" required><el-input-number v-model="skuForm.price" :min="0.01" :precision="2" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="划线价"><el-input-number v-model="skuForm.originalPrice" :min="0.01" :precision="2" style="width:100%" /></el-form-item></el-col></el-row>
      <el-form-item label="规格JSON"><el-input v-model="skuForm.specJson" placeholder='例如 {"颜色":"黑色","容量":"128GB"}' /></el-form-item>
      <el-form-item><el-button type="primary" :loading="skuSaving" @click="createSku">{{ editingSkuId ? '保存SKU' : '新增SKU' }}</el-button><el-button v-if="editingSkuId" @click="editingSkuId = null; Object.assign(skuForm, { skuCode: '', specJson: '', price: null, originalPrice: null, initialStock: 0 })">取消编辑</el-button></el-form-item>
    </el-form>
  </el-dialog>
  <el-dialog v-model="stockDialogVisible" :title="`库存：${selectedSku?.skuCode || ''}`" width="760px">
    <div v-loading="stockLoading">
      <el-descriptions v-if="stock" :column="3" border style="margin-bottom:18px"><el-descriptions-item label="总库存">{{ stock.total }}</el-descriptions-item><el-descriptions-item label="锁定库存">{{ stock.locked }}</el-descriptions-item><el-descriptions-item label="可用库存">{{ stock.available }}</el-descriptions-item></el-descriptions>
      <el-form label-width="90px" @submit.prevent="adjustStock">
        <el-row :gutter="12"><el-col :span="10"><el-form-item label="调整数量" required><el-input-number v-model="stockForm.quantity" :min="-999999" :max="999999" style="width:100%" /><div style="font-size:12px;color:#909399">正数入库，负数出库</div></el-form-item></el-col><el-col :span="14"><el-form-item label="调整原因" required><el-input v-model="stockForm.reason" maxlength="64" show-word-limit /></el-form-item></el-col></el-row>
        <el-form-item><el-button type="primary" :loading="stockSaving" @click="adjustStock">确认调整</el-button></el-form-item>
      </el-form>
      <el-table :data="stockLedgers" size="small"><el-table-column prop="createTime" label="时间" min-width="160" /><el-table-column prop="action" label="动作" width="130" /><el-table-column prop="quantity" label="变更" width="90" /><el-table-column prop="beforeAvailable" label="调整前" width="90" /><el-table-column prop="afterAvailable" label="调整后" width="90" /><el-table-column prop="referenceNo" label="原因" min-width="140" /></el-table>
    </div>
  </el-dialog>
</template>

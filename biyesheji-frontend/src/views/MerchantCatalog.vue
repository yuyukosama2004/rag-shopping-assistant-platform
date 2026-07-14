<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createMerchantCatalog, deleteMerchantCatalog, getMerchantCatalog, updateMerchantCatalog, type ProductCatalog, type ProductCatalogInput } from '../api/merchant'

type CatalogType = ProductCatalog['catalogType']

const brands = ref<ProductCatalog[]>([])
const categories = ref<ProductCatalog[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref<ProductCatalog | null>(null)
const currentType = ref<CatalogType>('BRAND')
const form = reactive<ProductCatalogInput>({ name: '', sortOrder: 0, status: 1 })

const load = async () => {
  loading.value = true
  try {
    const [brandResponse, categoryResponse] = await Promise.all([getMerchantCatalog('BRAND'), getMerchantCatalog('CATEGORY')])
    brands.value = brandResponse.data.data
    categories.value = categoryResponse.data.data
  } finally { loading.value = false }
}
const openCreate = (type: CatalogType) => {
  currentType.value = type
  editing.value = null
  Object.assign(form, { name: '', sortOrder: 0, status: 1 })
  dialogVisible.value = true
}
const openEdit = (item: ProductCatalog) => {
  currentType.value = item.catalogType
  editing.value = item
  Object.assign(form, { name: item.name, sortOrder: item.sortOrder, status: item.status })
  dialogVisible.value = true
}
const save = async () => {
  if (!form.name.trim()) return ElMessage.warning('请填写名称')
  saving.value = true
  try {
    const data = { ...form, name: form.name.trim() }
    if (editing.value) await updateMerchantCatalog(currentType.value, editing.value.id, data)
    else await createMerchantCatalog(currentType.value, data)
    ElMessage.success('目录已保存')
    dialogVisible.value = false
    await load()
  } finally { saving.value = false }
}
const disable = async (item: ProductCatalog) => {
  await ElMessageBox.confirm(`停用“${item.name}”后，历史商品不受影响。确定继续吗？`, '停用目录项')
  await deleteMerchantCatalog(item.catalogType, item.id)
  ElMessage.success('目录项已停用')
  await load()
}
const label = (type: CatalogType) => type === 'BRAND' ? '品牌' : '分类'
onMounted(load)
</script>

<template>
  <el-row :gutter="16" v-loading="loading">
    <el-col :span="12" v-for="(items, type) in { BRAND: brands, CATEGORY: categories }" :key="type">
      <el-card>
        <template #header><div style="display:flex;justify-content:space-between;align-items:center"><strong>{{ label(type as CatalogType) }}管理</strong><el-button type="primary" @click="openCreate(type as CatalogType)">新增{{ label(type as CatalogType) }}</el-button></div></template>
        <el-table :data="items" size="small">
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="130"><template #default="{ row }"><el-button text @click="openEdit(row)">编辑</el-button><el-button v-if="row.status === 1" text type="danger" @click="disable(row)">停用</el-button></template></el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>
  <el-dialog v-model="dialogVisible" :title="editing ? `编辑${label(currentType)}` : `新增${label(currentType)}`" width="440px">
    <el-form label-width="80px" @submit.prevent="save">
      <el-form-item label="名称" required><el-input v-model="form.name" maxlength="50" /></el-form-item>
      <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" style="width:100%" /></el-form-item>
      <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item>
      <el-form-item><el-button type="primary" :loading="saving" @click="save">保存</el-button></el-form-item>
    </el-form>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getCartList, updateCartQuantity, removeCartItem, removeCartBatch, toggleCartCheck, checkAllCart } from '../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const items = ref<any[]>([])

const load = async () => {
  try { const r = await getCartList(); items.value = r.data.data } catch {}
}

onMounted(load)

const checkedItems = computed(() => items.value.filter(i => i.checked === 1))
const totalAmount = computed(() => checkedItems.value.reduce((s, i) => s + i.productPrice * i.quantity, 0))

const changeQty = async (item: any, q: number) => {
  if (q < 1) return
  await updateCartQuantity(item.id, q)
  load()
}
const removeOne = async (id: number) => {
  await removeCartItem(id)
  ElMessage.success('已删除')
  load()
}
const clearAll = async () => {
  await ElMessageBox.confirm('确定清空购物车？')
  const ids = items.value.map(i => i.id)
  await removeCartBatch(ids)
  load()
}
const toggleCheck = async (id: number) => {
  await toggleCartCheck(id)
  load()
}
const toggleAll = async (checked: boolean) => {
  await checkAllCart(checked)
  load()
}
const goCheckout = () => {
  if (checkedItems.value.length === 0) return ElMessage.warning('请选择商品')
  router.push('/checkout')
}
</script>

<template>
  <h1 class="page-header">购物车</h1>
  <el-empty v-if="items.length === 0" description="购物车为空，去逛逛吧" />
  <template v-else>
    <el-card>
      <div style="display:flex;align-items:center;padding:8px 0;border-bottom:1px solid #eee">
        <el-checkbox :model-value="items.every(i=>i.checked)" @change="toggleAll">全选</el-checkbox>
        <el-button style="margin-left:auto" size="small" type="danger" text @click="clearAll">清空</el-button>
      </div>
      <div v-for="item in items" :key="item.id" style="display:flex;align-items:center;padding:16px 0;border-bottom:1px solid #f5f5f5;gap:12px">
        <el-checkbox :model-value="item.checked === 1" @change="toggleCheck(item.id)" />
        <img :src="item.productImage||'https://picsum.photos/80/80'" style="width:80px;height:80px;object-fit:cover;border-radius:8px" />
        <div style="flex:1"><strong>{{ item.productName }}</strong></div>
        <span class="price">¥{{ item.productPrice }}</span>
        <el-input-number :model-value="item.quantity" :min="1" :max="99" size="small" @change="(v: number | undefined) => v && changeQty(item, v)" />
        <span class="price">¥{{ (item.productPrice * item.quantity).toFixed(2) }}</span>
        <el-button size="small" type="danger" text @click="removeOne(item.id)">删除</el-button>
      </div>
    </el-card>
    <div style="display:flex;align-items:center;justify-content:flex-end;margin-top:16px;gap:16px">
      <span>已选 <strong>{{ checkedItems.length }}</strong> 件</span>
      <span>合计: <span class="price" style="font-size:24px">¥{{ totalAmount.toFixed(2) }}</span></span>
      <el-button size="large" type="primary" @click="goCheckout">去结算</el-button>
    </div>
  </template>
</template>

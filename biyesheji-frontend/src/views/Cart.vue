<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getCartList, updateCartQuantity, removeCartItem, removeCartBatch, toggleCartCheck, checkAllCart } from '../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
const router = useRouter(); const items = ref<any[]>([])
const load = async () => { try { const r = await getCartList(); items.value = r.data.data } catch {} }
onMounted(load)
const checked = computed(() => items.value.filter(i => i.checked === 1))
const total = computed(() => checked.value.reduce((s, i) => s + i.productPrice * i.quantity, 0))
const chgQty = async (it: any, q: number) => { if (q < 1) return; await updateCartQuantity(it.id, q); load() }
const del = async (id: number) => { await removeCartItem(id); ElMessage.success('已删除'); load() }
const clear = async () => { await ElMessageBox.confirm('确定清空？'); await removeCartBatch(items.value.map(i => i.id)); load() }
const tgl = async (id: number) => { await toggleCartCheck(id); load() }
const tglAll = async (v: boolean) => { await checkAllCart(v); load() }
</script>
<template>
  <div class="page-content">
    <div class="page-title">购物车</div>
    <el-empty v-if="items.length===0" description="购物车为空" />
    <template v-else>
      <div style="display:flex;align-items:center;padding:8px 0;gap:8px">
        <el-checkbox :model-value="items.every(i=>i.checked)" @change="tglAll" label="全选" />
        <el-button size="small" type="danger" text @click="clear" style="margin-left:auto">清空</el-button>
      </div>
      <div v-for="it in items" :key="it.id" style="display:flex;align-items:center;gap:10px;padding:12px;background:#fff;border-radius:12px;margin-bottom:8px;box-shadow:var(--shadow-card)">
        <el-checkbox :model-value="it.checked===1" @change="tgl(it.id)" />
        <img :src="it.productImage||'https://picsum.photos/80/80'" style="width:64px;height:64px;border-radius:8px;object-fit:cover" />
        <div style="flex:1;min-width:0">
          <div style="font-size:14px;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ it.productName }}</div>
          <div style="color:var(--jd-red);font-weight:700;margin-top:4px">¥{{ it.productPrice }}</div>
        </div>
        <el-input-number :model-value="it.quantity" :min="1" :max="99" size="small" style="width:90px" @change="(v:any)=>v&&chgQty(it,v)" />
        <el-button size="small" type="danger" text @click="del(it.id)">删</el-button>
      </div>
      <div style="position:sticky;bottom:56px;background:#fff;padding:12px 16px;display:flex;align-items:center;justify-content:space-between;box-shadow:0 -2px 8px rgba(0,0,0,0.04);border-radius:12px 12px 0 0;margin-top:16px">
        <span>已选 <b>{{ checked.length }}</b> 件</span>
        <span>合计: <span style="color:var(--jd-red);font-size:22px;font-weight:700">¥{{ total.toFixed(2) }}</span></span>
        <el-button type="primary" size="large" @click="router.push('/checkout')">去结算</el-button>
      </div>
    </template>
  </div>
</template>

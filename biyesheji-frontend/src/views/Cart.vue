<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getCartList, updateCartQuantity, removeCartItem, toggleCartCheck, checkAllCart } from '../api/order'
import { ElMessage } from 'element-plus'

const router = useRouter(); const items = ref<any[]>([])
const load = async () => { try { const r = await getCartList(); items.value = r.data.data } catch {} }
onMounted(load)
const checked = computed(() => items.value.filter(i => i.checked === 1))
const total = computed(() => checked.value.reduce((s, i) => s + i.productPrice * i.quantity, 0))
const chg = async (it: any, q: number) => { if (q < 1) return; await updateCartQuantity(it.id, q); load() }
const del = async (id: number) => { await removeCartItem(id); ElMessage.success('已删除'); load() }
const tgl = async (id: number) => { await toggleCartCheck(id); load() }
const tglAll = async (v: boolean) => { await checkAllCart(v); load() }
</script>

<template>
  <div style="background:#fff;padding:16px">
    <h2 style="font-size:18px;font-weight:600;margin-bottom:14px">我的购物车</h2>
    <el-empty v-if="items.length===0" description="购物车还是空的，去逛逛吧" />
    <template v-else>
      <table class="cart-table">
        <thead><tr><th style="width:60px"><el-checkbox :model-value="items.every(i=>i.checked)" @change="tglAll">全选</el-checkbox></th><th style="width:90px">图片</th><th>商品名称</th><th style="width:100px">单价</th><th style="width:140px">数量</th><th style="width:100px">小计</th><th style="width:80px">操作</th></tr></thead>
        <tbody>
          <tr v-for="it in items" :key="it.id">
            <td><el-checkbox :model-value="it.checked===1" @change="tgl(it.id)" /></td>
            <td><img :src="it.productImage||'https://picsum.photos/60/60'" class="cart-img" /></td>
            <td style="font-weight:500">{{ it.productName }}</td>
            <td class="price">¥{{ it.productPrice }}</td>
            <td><el-input-number :model-value="it.quantity" :min="1" :max="99" size="small" style="width:100px" @change="(v:any)=>v&&chg(it,v)" /></td>
            <td class="price">¥{{ (it.productPrice*it.quantity).toFixed(2) }}</td>
            <td><el-button size="small" type="danger" text @click="del(it.id)">删除</el-button></td>
          </tr>
        </tbody>
      </table>
      <div class="cart-footer">
        <span>已选 <b>{{ checked.length }}</b> 件</span>
        <span class="total">合计: <span class="num">¥{{ total.toFixed(2) }}</span></span>
        <el-button size="large" type="danger" @click="router.push('/checkout')">去结算</el-button>
      </div>
    </template>
  </div>
</template>

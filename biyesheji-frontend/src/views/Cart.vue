<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getCartList, updateCartQuantity, removeCartItem, toggleCartCheck, checkAllCart, updateCartOptions } from '../api/order'
import { ElMessage } from 'element-plus'

const router = useRouter(); const items = ref<any[]>([])
const load = async () => { try { const r = await getCartList(); items.value = r.data.data } catch {} }
onMounted(load)
const checked = computed(() => items.value.filter(i => i.checked === 1))
const total = computed(() => checked.value.reduce((s, i) => s + i.productPrice * i.quantity, 0))
const chgQty = async (it: any, q: number) => { if (q < 1) return; await updateCartQuantity(it.id, q); load() }
const del = async (id: number) => { await removeCartItem(id); ElMessage.success('已删除'); load() }
const tgl = async (id: number) => { await toggleCartCheck(id); load() }
const tglAll = async (v: boolean) => { await checkAllCart(v); load() }

const parseColors = (it: any) => { try { return JSON.parse(it.colorOptions || '[]') } catch { return [] } }
const parseStorages = (it: any) => { try { return JSON.parse(it.storageOptions || '[]') } catch { return [] } }
const chgColor = async (it: any, c: string) => { await updateCartOptions(it.id, c, it.selectedStorage); load() }
const chgStorage = async (it: any, s: string) => { await updateCartOptions(it.id, it.selectedColor, s); load() }
</script>

<template>
  <div style="background:#fff;padding:16px">
    <h2 style="font-size:18px;font-weight:600;margin-bottom:14px">购物车</h2>
    <el-empty v-if="items.length===0" description="购物车还是空的，去逛逛吧" />
    <template v-else>
      <!-- 桌面端表格 -->
      <table class="cart-table">
        <thead><tr>
          <th style="width:50px;text-align:center"><el-checkbox :model-value="items.every(i=>i.checked)" @change="tglAll">全选</el-checkbox></th>
          <th style="width:70px;text-align:center">图片</th>
          <th style="text-align:left">商品名称</th>
          <th style="width:90px;text-align:center">外观</th>
          <th style="width:100px;text-align:center">规格</th>
          <th style="width:80px;text-align:center">单价</th>
          <th style="width:110px;text-align:center">数量</th>
          <th style="width:80px;text-align:center">小计</th>
          <th style="width:60px;text-align:center">操作</th>
        </tr></thead>
        <tbody>
          <tr v-for="it in items" :key="it.id">
            <td style="text-align:center"><el-checkbox :model-value="it.checked===1" @change="tgl(it.id)" /></td>
            <td style="text-align:center"><img :src="it.productImage||''" class="cart-img" /></td>
            <td style="font-weight:500;text-align:left">{{ it.productName }}</td>
            <td style="text-align:center">
              <select v-model="it.selectedColor" @change="chgColor(it, it.selectedColor)" style="font-size:12px;padding:2px;border:1px solid #ddd">
                <option v-for="c in parseColors(it)" :key="c" :value="c">{{ c }}</option>
              </select>
            </td>
            <td style="text-align:center">
              <select v-model="it.selectedStorage" @change="chgStorage(it, it.selectedStorage)" style="font-size:12px;padding:2px;border:1px solid #ddd">
                <option v-for="s in parseStorages(it)" :key="s" :value="s">{{ s }}</option>
              </select>
            </td>
            <td class="price" style="text-align:center">¥{{ it.productPrice }}</td>
            <td style="text-align:center"><el-input-number :model-value="it.quantity" :min="1" :max="99" size="small" style="width:80px" @change="(v:any)=>v&&chgQty(it,v)" /></td>
            <td class="price" style="text-align:center">¥{{ (it.productPrice*it.quantity).toFixed(2) }}</td>
            <td style="text-align:center"><el-button size="small" type="danger" text @click="del(it.id)">删除</el-button></td>
          </tr>
        </tbody>
      </table>
      <!-- 手机端卡片 -->
      <div class="cart-mobile-cards">
        <div class="cart-mobile-card" v-for="it in items" :key="'m'+it.id">
          <div class="cm-row">
            <el-checkbox :model-value="it.checked===1" @change="tgl(it.id)" />
            <img :src="it.productImage||''" class="cm-img" />
            <span class="cm-name">{{ it.productName }}</span>
          </div>
          <div class="cm-options" v-if="it.selectedColor||it.selectedStorage">
            外观: {{ it.selectedColor || '-' }} / 规格: {{ it.selectedStorage || '-' }}
          </div>
          <div class="cm-bottom">
            <span class="cm-price">¥{{ (it.productPrice*it.quantity).toFixed(2) }}</span>
            <div class="cm-qty">
              <button @click="chgQty(it, it.quantity-1)">−</button>
              <span>{{ it.quantity }}</span>
              <button @click="chgQty(it, it.quantity+1)">+</button>
            </div>
            <el-button size="small" type="danger" text @click="del(it.id)">删除</el-button>
          </div>
        </div>
      </div>
      <div class="cart-footer">
        <span>已选 <b>{{ checked.length }}</b> 件</span>
        <span class="total">合计: <span class="num">¥{{ total.toFixed(2) }}</span></span>
        <el-button size="large" type="danger" @click="router.push('/checkout')">去结算</el-button>
      </div>
    </template>
  </div>
</template>

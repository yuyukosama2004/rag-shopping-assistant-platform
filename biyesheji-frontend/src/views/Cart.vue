<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getCartList, updateCartQuantity, removeCartItem, toggleCartCheck, checkAllCart } from '../api/order'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '../api/request'

const router = useRouter(); const items = ref<any[]>([])
const loading = ref(false); const error = ref(''); const pendingIds = ref(new Set<number>()); const checkingAll = ref(false)
const load = async () => {
  loading.value = true; error.value = ''
  try { const r = await getCartList(); items.value = r.data.data }
  catch(cause) { error.value = getErrorMessage(cause, '购物车加载失败，请稍后重试') }
  finally { loading.value = false }
}
onMounted(load)
const checked = computed(() => items.value.filter(i => i.checked === 1))
const total = computed(() => checked.value.reduce((s, i) => s + i.productPrice * i.quantity, 0))
const mutate = async (id: number, operation: () => Promise<unknown>, success?: string) => {
  if (pendingIds.value.has(id)) return
  pendingIds.value.add(id)
  try { await operation(); if (success) ElMessage.success(success); await load() } catch {}
  finally { pendingIds.value.delete(id) }
}
const chgQty = (it: any, q: number) => { if (q >= 1) return mutate(it.id, () => updateCartQuantity(it.id, q)) }
const del = (id: number) => mutate(id, () => removeCartItem(id), '已删除')
const tgl = (id: number) => mutate(id, () => toggleCartCheck(id))
const tglAll = async (v: boolean) => {
  if (checkingAll.value) return
  checkingAll.value = true
  try { await checkAllCart(v); await load() } catch {}
  finally { checkingAll.value = false }
}

const skuLabel = (it: any) => { try { return Object.entries(JSON.parse(it.skuSpecJson || '{}')).map(([key, value]) => `${key}: ${String(value)}`).join(' / ') || '-' } catch { return '-' } }
</script>

<template>
  <div v-loading="loading" style="background:#fff;padding:16px;min-height:240px">
    <h2 style="font-size:18px;font-weight:600;margin-bottom:14px">购物车</h2>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false"><template #default><el-button size="small" @click="load">重新加载</el-button></template></el-alert>
    <el-empty v-else-if="!loading && items.length===0" description="购物车还是空的，去逛逛吧"><el-button type="primary" @click="router.push('/products')">浏览商品</el-button></el-empty>
    <template v-else>
      <!-- 桌面端表格 -->
      <table class="cart-table">
        <thead><tr>
          <th style="width:50px;text-align:center"><el-checkbox :model-value="items.every(i=>i.checked)" :disabled="checkingAll" @change="tglAll">全选</el-checkbox></th>
          <th style="width:70px;text-align:center">图片</th>
          <th style="text-align:left">商品名称</th>
          <th style="width:110px;text-align:center">SKU</th>
          <th style="width:150px;text-align:center">规格</th>
          <th style="width:80px;text-align:center">单价</th>
          <th style="width:110px;text-align:center">数量</th>
          <th style="width:80px;text-align:center">小计</th>
          <th style="width:60px;text-align:center">操作</th>
        </tr></thead>
        <tbody>
          <tr v-for="it in items" :key="it.id">
            <td style="text-align:center"><el-checkbox :model-value="it.checked===1" :disabled="pendingIds.has(it.id)" @change="tgl(it.id)" /></td>
            <td style="text-align:center"><img :src="it.productImage||''" class="cart-img" /></td>
            <td style="font-weight:500;text-align:left">{{ it.productName }}</td>
            <td style="text-align:center">{{ it.skuCode }}</td>
            <td style="text-align:center">{{ skuLabel(it) }}</td>
            <td class="price" style="text-align:center">¥{{ it.productPrice }}</td>
            <td style="text-align:center"><el-input-number :model-value="it.quantity" :min="1" :max="99" :disabled="pendingIds.has(it.id)" size="small" style="width:80px" @change="(v:any)=>v&&chgQty(it,v)" /></td>
            <td class="price" style="text-align:center">¥{{ (it.productPrice*it.quantity).toFixed(2) }}</td>
            <td style="text-align:center"><el-button size="small" type="danger" text :loading="pendingIds.has(it.id)" @click="del(it.id)">删除</el-button></td>
          </tr>
        </tbody>
      </table>
      <!-- 手机端卡片 -->
      <div class="cart-mobile-cards">
        <div class="cart-mobile-card" v-for="it in items" :key="'m'+it.id">
          <div class="cm-row">
            <el-checkbox :model-value="it.checked===1" :disabled="pendingIds.has(it.id)" @change="tgl(it.id)" />
            <img :src="it.productImage||''" class="cm-img" />
            <span class="cm-name">{{ it.productName }}</span>
          </div>
          <div class="cm-options">
            {{ it.skuCode }} · {{ skuLabel(it) }}
          </div>
          <div class="cm-bottom">
            <span class="cm-price">¥{{ (it.productPrice*it.quantity).toFixed(2) }}</span>
            <div class="cm-qty">
              <button :disabled="pendingIds.has(it.id)" @click="chgQty(it, it.quantity-1)">−</button>
              <span>{{ it.quantity }}</span>
              <button :disabled="pendingIds.has(it.id)" @click="chgQty(it, it.quantity+1)">+</button>
            </div>
            <el-button size="small" type="danger" text :loading="pendingIds.has(it.id)" @click="del(it.id)">删除</el-button>
          </div>
        </div>
      </div>
      <div class="cart-footer">
        <span>已选 <b>{{ checked.length }}</b> 件</span>
        <span class="total">合计: <span class="num">¥{{ total.toFixed(2) }}</span></span>
        <el-button size="large" type="danger" :disabled="checked.length === 0 || pendingIds.size > 0" @click="router.push('/checkout')">去结算</el-button>
      </div>
    </template>
  </div>
</template>

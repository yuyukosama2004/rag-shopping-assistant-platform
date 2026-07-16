<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, getProductSkus } from '../api/product'
import { addToCart } from '../api/order'
import { getAddressList } from '../api/user'
import { ElMessage } from 'element-plus'
import { ApiRequestError, getErrorMessage } from '../api/request'

const route = useRoute(); const router = useRouter()
const product = ref<any>(null); const spec = ref<any>(null); const qty = ref(1)
const skus = ref<any[]>([]); const selectedSku = ref<any>(null)
const addresses = ref<any[]>([]); const selectedAddr = ref(0); const manualAddr = ref({ name: '', phone: '', address: '' })
const loading = ref(true)
const error = ref('')
const unavailable = ref(false)
const adding = ref(false)

const load = async () => {
  loading.value = true
  error.value = ''
  unavailable.value = false
  try {
    const r = await getProductDetail(String(route.params.id))
    if (!r.data || !r.data.data) { return }
    product.value = r.data.data
    try { spec.value = JSON.parse(product.value.specJson || '{}') } catch { spec.value = {} }
    const skuResponse = await getProductSkus(product.value.id)
    skus.value = skuResponse.data.data || []
    selectedSku.value = skus.value.find((sku: any) => sku.available > 0) || null
    try { const ar = await getAddressList(); addresses.value = ar.data.data || []
      const def = addresses.value.find((a: any) => a.isDefault === 1)
      if (def) { selectedAddr.value = def.id; manualAddr.value = { name: def.receiverName, phone: def.receiverPhone, address: def.detail } }
    } catch {}
  } catch (cause: any) {
    const status = cause instanceof ApiRequestError ? cause.status : cause?.response?.status
    unavailable.value = status === 404
    error.value = unavailable.value ? '商品不存在或已下架' : getErrorMessage(cause, '商品加载失败，请稍后重试')
  } finally { loading.value = false }
}
onMounted(load)
const selectAddr = (a: any) => { selectedAddr.value = a.id; manualAddr.value = { name: a.receiverName, phone: a.receiverPhone, address: a.detail } }
const selectSku = (sku: any) => { if (sku.available > 0) { selectedSku.value = sku; qty.value = 1 } }

const skuLabel = (sku: any) => {
  try { return Object.entries(JSON.parse(sku.specJson || '{}')).map(([key, value]) => `${key}: ${String(value)}`).join(' / ') || sku.skuCode } catch { return sku.skuCode }
}
const addCart = async () => {
  if (adding.value) return false
  if (!selectedSku.value) { ElMessage.warning('当前商品暂无可售规格'); return false }
  if (qty.value > selectedSku.value.available) { ElMessage.warning('购买数量超过当前库存'); return false }
  adding.value = true
  try { await addToCart(product.value.id, selectedSku.value.id, qty.value); ElMessage.success('已加入购物车'); return true } catch { return false }
  finally { adding.value = false }
}
const goBuy = async () => { if (await addCart()) router.push('/checkout') }
</script>

<template>
  <div v-if="loading" v-loading="true" style="min-height:360px" />
  <el-result v-else-if="error" :icon="unavailable ? 'warning' : 'error'" :title="error" :sub-title="unavailable ? '商品可能已被商家下架，请返回商品列表选择其他商品。' : '请检查网络后重试。'">
    <template #extra><el-button v-if="!unavailable" type="primary" @click="load">重新加载</el-button><el-button @click="router.push('/products')">返回商品列表</el-button></template>
  </el-result>
  <div v-else-if="product">
    <div class="detail-wrap">
      <div class="detail-img"><img :src="product.mainImage||''" /></div>
      <div class="detail-info">
        <h1 style="font-size:18px;font-weight:600;line-height:1.5;margin-bottom:12px">{{ product.name }}</h1>

        <div style="background:#fdf2f2;padding:12px 16px;margin-bottom:12px;border-radius:4px">
          <span style="color:var(--jd-red);font-size:28px;font-weight:700">¥{{ selectedSku?.price ?? product.price }}</span>
          <span v-if="(selectedSku?.originalPrice ?? product.originalPrice) > (selectedSku?.price ?? product.price)" style="color:#999;font-size:14px;text-decoration:line-through;margin-left:8px">¥{{ selectedSku?.originalPrice ?? product.originalPrice }}</span>
        </div>

        <div style="margin-bottom:12px;font-size:13px">
          <span style="color:var(--jd-red);font-weight:600">{{ product.brand }}</span>
          <span style="color:#999;margin-left:16px">月销 {{ product.sales }} 件</span>
        </div>

        <div style="margin-bottom:14px">
          <div style="font-size:13px;color:#999;margin-bottom:6px">选择规格</div>
          <div style="display:flex;flex-wrap:wrap;gap:8px">
            <span v-for="sku in skus" :key="sku.id"
              @click="selectSku(sku)"
              style="padding:6px 14px;border:1px solid #ddd;border-radius:2px;font-size:13px;cursor:pointer;min-width:80px;text-align:center"
              :style="selectedSku?.id===sku.id ? {borderColor:'var(--jd-red)',color:'var(--jd-red)',background:'#FFF0F0'} : sku.available > 0 ? {} : {color:'#bbb',background:'#f5f5f5',cursor:'not-allowed'}"
            >{{ skuLabel(sku) }}（{{ sku.available > 0 ? `库存 ${sku.available}` : '缺货' }}）</span>
          </div>
        </div>

        <!-- 送至 -->
        <div style="background:#fafafa;padding:12px;margin-bottom:12px;font-size:13px;color:#555;line-height:2">
          <div style="font-weight:600;margin-bottom:4px">送至</div>
          <div v-if="addresses.length > 0" style="margin-bottom:6px">
            <div v-for="a in addresses" :key="a.id" @click="selectAddr(a)"
              style="padding:6px 8px;border:1px solid #eee;margin-bottom:3px;cursor:pointer;font-size:12px;border-radius:3px"
              :style="selectedAddr===a.id ? {borderColor:'var(--jd-red)',background:'#FFF0F0'} : {}">
              <span v-if="a.isDefault" style="background:var(--jd-red);color:#fff;font-size:10px;padding:0 3px;margin-right:4px">默认</span>
              {{ a.detail }}
            </div>
          </div>
          <div style="font-size:12px;color:#999;margin-bottom:4px">或手动输入</div>
          <el-input v-model="manualAddr.name" placeholder="收货人" size="small" style="width:120px;margin-right:4px" />
          <el-input v-model="manualAddr.phone" placeholder="手机号" size="small" style="width:140px;margin-right:4px" />
          <el-input v-model="manualAddr.address" placeholder="详细地址" size="small" style="width:200px" />
          <div style="margin-top:6px">现在下单，预计 <b>2-3天</b> 内送达 · 包邮</div>
        </div>

        <div style="display:flex;gap:16px;font-size:12px;color:#999;margin-bottom:16px">
          <span>✓ 正品保证</span><span>✓ 全国联保</span><span>✓ 7天价保</span><span>✓ 免费退换</span>
        </div>

        <div style="display:flex;align-items:center;gap:12px">
          <span style="font-size:13px;color:#999">数量</span>
          <el-input-number v-model="qty" :min="1" :max="selectedSku?.available || 1" :disabled="!selectedSku || adding" size="small" style="width:120px" />
          <el-button size="large" :loading="adding" :disabled="!selectedSku" @click="addCart" style="width:160px">加入购物车</el-button>
          <el-button size="large" type="danger" :loading="adding" :disabled="!selectedSku" @click="goBuy" style="width:160px">立即购买</el-button>
        </div>
      </div>
    </div>

    <div style="background:#fff;padding:20px;margin-top:14px" v-if="spec">
      <h3 style="font-size:16px;font-weight:600;margin-bottom:14px;padding-left:10px;border-left:4px solid var(--jd-red)">商品描述</h3>
      <p style="font-size:14px;color:#555;line-height:1.8;margin-bottom:20px">{{ product.description }}</p>
      <h3 style="font-size:16px;font-weight:600;margin-bottom:14px;padding-left:10px;border-left:4px solid var(--jd-red)">商品参数</h3>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:0">
        <div v-for="(v,k) in spec" :key="k" style="display:flex;border-bottom:1px solid #f0f0f0;padding:10px 0;font-size:13px">
          <span style="color:#999;width:100px;flex-shrink:0">{{ k }}</span>
          <span style="color:#333;font-weight:500">{{ v }}</span>
        </div>
      </div>
    </div>
  </div>
  <el-empty v-else description="商品暂无可售内容" style="padding-top:60px" />
</template>

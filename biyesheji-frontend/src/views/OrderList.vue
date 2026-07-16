<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderPage, cancelOrder, completeOrder } from '../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getErrorMessage } from '../api/request'
const router = useRouter(); const orders = ref<any[]>([]); const total = ref(0); const page = ref(1); const st = ref<number>(-1)
const loading = ref(false); const error = ref(''); const activeOrder = ref('')
const sm: Record<number,string> = {0:'待商家确认',1:'已确认收款',2:'已发货',3:'已完成',4:'已取消',5:'已超时',6:'处理中'}
const load = async () => {
  loading.value = true; error.value = ''
  try { const r = await getOrderPage({ pageNum: page.value, pageSize: 10, status: st.value === -1 ? undefined : st.value }); orders.value = r.data.data.records; total.value = r.data.data.total }
  catch(cause) { error.value = getErrorMessage(cause, '订单加载失败，请稍后重试') }
  finally { loading.value = false }
}
onMounted(load)
const fmtDate = (d: any) => { try { const a = Array.isArray(d) ? d : String(d).split(','); return a[0]+'-'+a[1]+'-'+a[2] } catch { return String(d).substring(0,10) } }
const goDet = (no: string) => router.push('/order/' + no)
const operate = async (no: string, confirmText: string, operation: () => Promise<unknown>, success: string) => {
  if (activeOrder.value) return
  try { await ElMessageBox.confirm(confirmText) } catch { return }
  activeOrder.value = no
  try { await operation(); ElMessage.success(success); await load() } catch {}
  finally { activeOrder.value = '' }
}
const cancel = (no: string) => operate(no, '确定取消该订单？', () => cancelOrder(no), '已取消')
const complete = (no: string) => operate(no, '确认已经收到商品？', () => completeOrder(no), '已确认收货')
</script>
<template>
  <div>
    <div class="section-title">我的订单</div>
    <div style="background:#fff;padding:14px;margin-bottom:14px;display:flex;gap:8px">
      <el-radio-group v-model="st" @change="load" size="small"><el-radio-button :value="-1">全部</el-radio-button><el-radio-button :value="0">待商家确认</el-radio-button><el-radio-button :value="1">已确认收款</el-radio-button><el-radio-button :value="6">处理中</el-radio-button></el-radio-group>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom:12px"><template #default><el-button size="small" @click="load">重新加载</el-button></template></el-alert>
    <el-empty v-else-if="!loading && orders.length===0" description="暂无订单" />
    <div v-loading="loading" style="min-height:100px">
    <div v-for="o in orders" :key="o.orderNo" style="background:#fff;padding:14px 16px;margin-bottom:8px">
      <div style="display:flex;align-items:center">
        <div style="flex:1"><el-tag :type="o.status===0?'danger':o.status===1?'success':''" size="small">{{ sm[o.status] }}</el-tag><span style="margin-left:8px;font-size:13px;color:#999">{{ o.orderNo }}</span></div>
        <span class="price" style="flex:1;font-size:18px;text-align:center">¥{{ o.totalAmount }}</span>
        <span style="flex:1;font-size:12px;color:#999;text-align:center">{{ fmtDate(o.createdAt) }}</span>
        <div style="width:170px;flex-shrink:0;display:flex;gap:6px;justify-content:flex-start">
          <el-button size="small" @click="goDet(o.orderNo)">详情</el-button>
          <span v-if="o.status===0" style="font-size:12px;color:#909399">等待商家处理</span>
          <el-button v-if="o.status===0" size="small" :loading="activeOrder===o.orderNo" :disabled="!!activeOrder" @click="cancel(o.orderNo)">取消</el-button>
          <el-button v-if="o.status===2" size="small" type="success" :loading="activeOrder===o.orderNo" :disabled="!!activeOrder" @click="complete(o.orderNo)">确认收货</el-button>
        </div>
      </div>
    </div>
    </div>
    <el-pagination v-if="total>10" v-model:current-page="page" :page-size="10" :total="total" layout="prev,pager,next" @current-change="load" />
  </div>
</template>

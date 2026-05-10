<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderPage, cancelOrder, payOrder } from '../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
const router = useRouter(); const orders = ref<any[]>([]); const total = ref(0); const page = ref(1); const st = ref<number|undefined>()
const sm: Record<number,string> = {0:'待支付',1:'已支付',2:'已发货',3:'已完成',4:'已取消',5:'已超时'}
const load = async () => { const r = await getOrderPage({ pageNum: page.value, pageSize: 10, status: st.value }); orders.value = r.data.data.records; total.value = r.data.data.total }
onMounted(load)
const pay = async (no: string) => { await payOrder(no); ElMessage.success('支付成功'); load() }
const cancel = async (no: string) => { await ElMessageBox.confirm('确定取消？'); await cancelOrder(no); ElMessage.success('已取消'); load() }
const goDet = (no: string) => router.push('/order/' + no)
</script>
<template>
  <div class="page-content">
    <div class="page-title">我的订单</div>
    <div style="display:flex;gap:6px;margin-bottom:12px;overflow-x:auto">
      <button v-for="(_,k) of {undefined:'全部',0:'待支付',1:'已支付'}" :key="k" class="filter-chip" :class="{active:st==(k==='undefined'?undefined:Number(k))}" @click="st=(k==='undefined'?undefined:Number(k));load()">{{ (k==='undefined'?'全部':['待支付','已支付'][Number(k)]) }}</button>
    </div>
    <div v-if="orders.length===0" style="text-align:center;padding:60px 0;color:#999">暂无订单</div>
    <div v-for="o in orders" :key="o.orderNo" style="background:#fff;border-radius:12px;padding:14px;margin-bottom:10px;box-shadow:var(--shadow-card)">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
        <span style="font-size:13px;color:#999">{{ o.orderNo }}</span>
        <el-tag :type="o.status===0?'warning':o.status===1?'success':''" size="small">{{ sm[o.status] }}</el-tag>
      </div>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="color:var(--jd-red);font-size:20px;font-weight:700">¥{{ o.totalAmount }}</span>
        <span style="font-size:12px;color:#999">{{ o.createdAt?.substring(0,10) }}</span>
        <div style="display:flex;gap:6px">
          <el-button size="small" @click="goDet(o.orderNo)">详情</el-button>
          <el-button v-if="o.status===0" size="small" type="primary" @click="pay(o.orderNo)">支付</el-button>
          <el-button v-if="o.status===0" size="small" type="danger" @click="cancel(o.orderNo)">取消</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

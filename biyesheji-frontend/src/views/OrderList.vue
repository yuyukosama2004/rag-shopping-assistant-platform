<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderPage, cancelOrder, payOrder } from '../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
const router = useRouter(); const orders = ref<any[]>([]); const total = ref(0); const page = ref(1); const st = ref<number|undefined>()
const sm: Record<number,string> = {0:'待支付',1:'已支付',2:'已发货',3:'已完成',4:'已取消',5:'已超时'}
const load = async () => { const r = await getOrderPage({ pageNum: page.value, pageSize: 10, status: st.value }); orders.value = r.data.data.records; total.value = r.data.data.total }
onMounted(load)
const goDet = (no: string) => router.push('/order/' + no)
const pay = async (no: string) => { await payOrder(no); ElMessage.success('支付成功'); load() }
const cancel = async (no: string) => { await ElMessageBox.confirm('确定取消？'); await cancelOrder(no); ElMessage.success('已取消'); load() }
</script>
<template>
  <div>
    <div class="section-title">我的订单</div>
    <div style="background:#fff;padding:14px;margin-bottom:14px;display:flex;gap:8px">
      <el-radio-group v-model="st" @change="load" size="small"><el-radio-button :value="undefined">全部</el-radio-button><el-radio-button :value="0">待支付</el-radio-button><el-radio-button :value="1">已支付</el-radio-button></el-radio-group>
    </div>
    <el-empty v-if="orders.length===0" description="暂无订单" />
    <div v-for="o in orders" :key="o.orderNo" style="background:#fff;padding:16px;margin-bottom:10px">
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
        <div><el-tag :type="o.status===0?'danger':o.status===1?'success':''" size="small">{{ sm[o.status] }}</el-tag><span style="margin-left:8px;font-size:13px;color:#999">{{ o.orderNo }}</span></div>
        <span class="price" style="font-size:20px">¥{{ o.totalAmount }}</span>
        <span style="font-size:12px;color:#999">{{ o.createdAt?.substring(0,10) }}</span>
        <div style="display:flex;gap:6px">
          <el-button size="small" @click="goDet(o.orderNo)">详情</el-button>
          <el-button v-if="o.status===0" size="small" type="danger" @click="pay(o.orderNo)">支付</el-button>
          <el-button v-if="o.status===0" size="small" @click="cancel(o.orderNo)">取消</el-button>
        </div>
      </div>
    </div>
    <el-pagination v-if="total>10" v-model:current-page="page" :page-size="10" :total="total" layout="prev,pager,next" @current-change="load" />
  </div>
</template>

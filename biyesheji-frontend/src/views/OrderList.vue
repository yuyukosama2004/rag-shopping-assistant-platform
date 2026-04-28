<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderPage, cancelOrder, payOrder } from '../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const orders = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const statusFilter = ref<number | undefined>(undefined)

const statusMap: Record<number, string> = { 0: '待支付', 1: '已支付', 2: '已发货', 3: '已完成', 4: '已取消', 5: '已超时' }
const statusType: Record<number, string> = { 0: 'warning', 1: 'success', 2: '', 3: 'info', 4: 'info', 5: 'info' }

const load = async () => {
  const r = await getOrderPage({ pageNum: pageNum.value, pageSize: 10, status: statusFilter.value })
  orders.value = r.data.data.records
  total.value = r.data.data.total
}

onMounted(load)

const goDetail = (no: string) => router.push(`/order/${no}`)
const doPay = async (no: string) => {
  await payOrder(no)
  ElMessage.success('支付成功')
  load()
}
const doCancel = async (no: string) => {
  await ElMessageBox.confirm('确定取消订单？')
  await cancelOrder(no)
  ElMessage.success('已取消')
  load()
}
</script>

<template>
  <h1 class="page-header">我的订单</h1>
  <el-radio-group v-model="statusFilter" @change="load" style="margin-bottom:16px">
    <el-radio-button :value="undefined">全部</el-radio-button>
    <el-radio-button :value="0">待支付</el-radio-button>
    <el-radio-button :value="1">已支付</el-radio-button>
    <el-radio-button :value="2">已发货</el-radio-button>
    <el-radio-button :value="3">已完成</el-radio-button>
  </el-radio-group>

  <div v-if="orders.length === 0" style="text-align:center;padding:60px 0">
    <el-empty description="暂无订单" />
  </div>

  <el-card v-for="order in orders" :key="order.orderNo" style="margin-bottom:12px">
    <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:8px">
      <div>
        <el-tag :type="statusType[order.status]">{{ statusMap[order.status] }}</el-tag>
        <span style="margin-left:8px">订单号: {{ order.orderNo }}</span>
      </div>
      <span class="price">¥{{ order.totalAmount }}</span>
      <span style="color:#999;font-size:13px">{{ order.createdAt }}</span>
      <div>
        <el-button size="small" @click="goDetail(order.orderNo)">详情</el-button>
        <el-button v-if="order.status === 0" size="small" type="success" @click="doPay(order.orderNo)">支付</el-button>
        <el-button v-if="order.status === 0" size="small" type="danger" @click="doCancel(order.orderNo)">取消</el-button>
      </div>
    </div>
  </el-card>

  <el-pagination v-if="total > 10" v-model:current-page="pageNum" :page-size="10" :total="total" layout="prev,pager,next" @current-change="load" style="margin-top:24px;justify-content:center" />
</template>

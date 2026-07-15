<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  acceptMerchantOrder,
  closeMerchantOrder,
  confirmMerchantOrderPayment,
  getMerchantOrderDetail,
  getMerchantOrders,
  shipMerchantOrder,
  updateMerchantOrderNote,
  type MerchantOrder,
  type MerchantOrderDetail,
} from '../api/merchant'

const loading = ref(false)
const shipping = ref(false)
const detailLoading = ref(false)
const noteSaving = ref(false)
const page = ref(1)
const total = ref(0)
const status = ref<number | undefined>()
const orders = ref<MerchantOrder[]>([])
const shipDialogVisible = ref(false)
const detailVisible = ref(false)
const selectedOrder = ref<MerchantOrder | null>(null)
const detail = ref<MerchantOrderDetail | null>(null)
const merchantNote = ref('')
const shipment = reactive({ carrier: '', trackingNo: '', note: '' })

const load = async () => {
  loading.value = true
  try {
    const data = (await getMerchantOrders(page.value, 20, status.value)).data.data
    orders.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

const confirmPayment = async (order: MerchantOrder) => {
  await confirmMerchantOrderPayment(order.orderNo)
  ElMessage.success('已确认收款')
  await load()
}

const accept = async (order: MerchantOrder) => {
  await acceptMerchantOrder(order.orderNo)
  ElMessage.success('订单已接单处理')
  await load()
}

const openShip = (order: MerchantOrder) => {
  selectedOrder.value = order
  Object.assign(shipment, { carrier: '', trackingNo: '', note: '' })
  shipDialogVisible.value = true
}

const ship = async () => {
  if (!selectedOrder.value || !shipment.carrier.trim() || !shipment.trackingNo.trim()) {
    return ElMessage.warning('请填写承运商和运单号')
  }
  shipping.value = true
  try {
    await shipMerchantOrder(selectedOrder.value.orderNo, shipment)
    ElMessage.success('订单已发货')
    shipDialogVisible.value = false
    await load()
  } finally { shipping.value = false }
}

const openDetail = async (order: MerchantOrder) => {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = (await getMerchantOrderDetail(order.orderNo)).data.data
    merchantNote.value = detail.value?.merchantNote || ''
  } finally { detailLoading.value = false }
}

const saveNote = async () => {
  if (!detail.value) return
  noteSaving.value = true
  try {
    await updateMerchantOrderNote(detail.value.order.orderNo, merchantNote.value)
    ElMessage.success('商家内部备注已保存')
    await openDetail(detail.value.order)
  } finally { noteSaving.value = false }
}

const canClose = (order: MerchantOrder) => order.status === 0
  || (order.status === 6 && order.paymentMethod === 'COD' && !order.payTime)

const closeOrder = async (order: MerchantOrder) => {
  try {
    const { value } = await ElMessageBox.prompt(
      '关单会释放尚未确认的预占库存，且不可在此页面恢复。已收款订单必须走退款流程。',
      `关闭订单 ${order.orderNo}`,
      { confirmButtonText: '确认关单', cancelButtonText: '取消', inputPlaceholder: '请输入关单原因', inputValidator: value => !!value?.trim() || '请填写关单原因', inputPattern: /^.{1,255}$/s, inputErrorMessage: '关单原因不能超过255个字符' },
    )
    await closeMerchantOrder(order.orderNo, value.trim())
    ElMessage.success('订单已关闭，预占库存已释放')
    detailVisible.value = false
    await load()
  } catch {
    // The request interceptor reports server errors; cancelling the prompt needs no message.
  }
}

const statusType = (value: number) => value === 0 ? 'warning' : value === 1 ? 'success' : value === 2 ? 'primary' : value >= 4 ? 'info' : ''
const operationLabel = (action: string) => ({
  MERCHANT_CONFIRM_PAYMENT: '确认收款',
  MERCHANT_CONFIRM_COD_PAYMENT: '确认货到付款',
  MERCHANT_ACCEPT: '接单',
  MERCHANT_SHIP: '发货',
  MERCHANT_NOTE: '更新备注',
  MERCHANT_CLOSE: '关闭订单',
  CUSTOMER_CANCEL: '消费者取消',
  CUSTOMER_COMPLETE: '消费者确认收货',
}[action] || action)

onMounted(load)
</script>

<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="header-row">
        <strong>订单管理</strong>
        <el-select v-model="status" clearable placeholder="全部状态" style="width:150px" @change="page = 1; load()">
          <el-option label="待商家确认" :value="0" />
          <el-option label="已确认收款" :value="1" />
          <el-option label="处理中" :value="6" />
          <el-option label="已发货" :value="2" />
          <el-option label="已完成" :value="3" />
          <el-option label="已取消" :value="4" />
        </el-select>
      </div>
    </template>
    <el-table :data="orders">
      <el-table-column label="订单号" min-width="180"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">{{ row.orderNo }}</el-button></template></el-table-column>
      <el-table-column label="商品" min-width="180"><template #default="{ row }"><div v-for="item in row.items" :key="item.id">{{ item.productName }} × {{ item.quantity }}<span class="muted"> {{ item.skuCode }}</span></div></template></el-table-column>
      <el-table-column label="收货信息" min-width="190"><template #default="{ row }"><div>{{ row.receiverName }} {{ row.receiverPhone }}</div><div class="muted">{{ row.receiverAddress }}</div></template></el-table-column>
      <el-table-column label="配送" min-width="130"><template #default="{ row }"><div>{{ row.shippingRuleName || (row.shippingMethod === 'PICKUP' ? '门店自提' : '配送') }}</div><div class="muted">商品 ¥{{ Number(row.productAmount ?? row.totalAmount).toFixed(2) }} + 配送 ¥{{ Number(row.shippingFee || 0).toFixed(2) }}</div></template></el-table-column>
      <el-table-column label="金额" width="100"><template #default="{ row }">¥{{ Number(row.totalAmount).toFixed(2) }}</template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.statusDesc }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="280"><template #default="{ row }">
        <el-button text @click="openDetail(row)">详情</el-button>
        <el-button v-if="row.status === 0 && row.paymentMethod !== 'COD'" text type="success" @click="confirmPayment(row)">确认收款</el-button>
        <el-button v-if="(row.status === 0 && row.paymentMethod === 'COD') || row.status === 1" text type="primary" @click="accept(row)">接单</el-button>
        <el-button v-if="row.status === 6" text type="primary" @click="openShip(row)">发货</el-button>
        <el-button v-if="row.paymentMethod === 'COD' && (row.status === 2 || row.status === 3) && !row.payTime" text type="success" @click="confirmPayment(row)">确认货款</el-button>
        <el-button v-if="canClose(row)" text type="danger" @click="closeOrder(row)">关单</el-button>
      </template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="prev, pager, next" style="margin-top:16px" @current-change="load" />
  </el-card>

  <el-dialog v-model="shipDialogVisible" title="填写发货信息" width="520px">
    <el-form label-width="80px" @submit.prevent="ship">
      <el-form-item label="承运商" required><el-input v-model="shipment.carrier" maxlength="64" /></el-form-item>
      <el-form-item label="运单号" required><el-input v-model="shipment.trackingNo" maxlength="64" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="shipment.note" type="textarea" maxlength="255" show-word-limit /></el-form-item>
      <el-form-item><el-button type="primary" :loading="shipping" @click="ship">确认发货</el-button></el-form-item>
    </el-form>
  </el-dialog>

  <el-drawer v-model="detailVisible" title="订单详情" size="min(720px, 92vw)">
    <div v-loading="detailLoading">
      <template v-if="detail">
        <div class="detail-heading"><strong>{{ detail.order.orderNo }}</strong><el-tag :type="statusType(detail.order.status)">{{ detail.order.statusDesc }}</el-tag></div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="付款方式">{{ detail.order.paymentMethod === 'COD' ? '货到付款' : '线下付款' }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">¥{{ Number(detail.order.totalAmount).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ detail.order.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="确认收款">{{ detail.order.payTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ detail.order.receiverName }} {{ detail.order.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="配送方式">{{ detail.order.shippingRuleName || detail.order.shippingMethod }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ detail.order.receiverAddress }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.order.shippingCarrier" label="物流" :span="2">{{ detail.order.shippingCarrier }} {{ detail.order.trackingNo }}</el-descriptions-item>
        </el-descriptions>

        <h4>商品明细</h4>
        <el-table :data="detail.order.items" size="small">
          <el-table-column prop="productName" label="商品" min-width="180" />
          <el-table-column prop="skuCode" label="SKU" min-width="120" />
          <el-table-column prop="price" label="单价" width="90" />
          <el-table-column prop="quantity" label="数量" width="70" />
          <el-table-column prop="subtotal" label="小计" width="90" />
        </el-table>

        <h4>商家内部备注</h4>
        <el-input v-model="merchantNote" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="仅商家后台可见，不会展示给消费者" />
        <div class="note-actions"><el-button type="primary" :loading="noteSaving" @click="saveNote">保存备注</el-button><el-button v-if="canClose(detail.order)" type="danger" plain @click="closeOrder(detail.order)">关闭订单</el-button></div>

        <h4>操作记录</h4>
        <el-empty v-if="!detail.operations.length" description="暂无操作记录" :image-size="48" />
        <el-timeline v-else>
          <el-timeline-item v-for="operation in detail.operations" :key="operation.id" :timestamp="operation.createdAt">
            <strong>{{ operationLabel(operation.action) }}</strong><span v-if="operation.note">：{{ operation.note }}</span>
          </el-timeline-item>
        </el-timeline>
      </template>
    </div>
  </el-drawer>
</template>

<style scoped>
.header-row,.detail-heading,.note-actions{display:flex;justify-content:space-between;align-items:center;gap:12px}.muted{font-size:12px;color:#909399}.detail-heading{margin-bottom:16px}.note-actions{justify-content:flex-end;margin:12px 0 22px}h4{margin:22px 0 10px}
</style>

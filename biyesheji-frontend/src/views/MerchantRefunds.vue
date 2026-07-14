<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveMerchantRefund, getMerchantRefunds, rejectMerchantRefund, type MerchantRefund } from '../api/merchant'

const loading = ref(false); const page = ref(1); const total = ref(0); const status = ref<string>(); const records = ref<MerchantRefund[]>([])
const load = async () => { loading.value = true; try { const data = (await getMerchantRefunds(page.value, 20, status.value)).data.data; records.value = data.records; total.value = data.total } finally { loading.value = false } }
const process = async (record: MerchantRefund, approved: boolean) => {
  const { value } = await ElMessageBox.prompt(approved ? '确认已完成线下退款后，可填写处理说明。' : '请填写拒绝原因。', approved ? '确认退款' : '拒绝退款', { inputPlaceholder: '处理说明（可选）', inputValue: '' })
  if (approved) await approveMerchantRefund(record.id, value || ''); else await rejectMerchantRefund(record.id, value || '')
  ElMessage.success(approved ? '已记录退款完成' : '已拒绝退款申请'); await load()
}
onMounted(load)
</script>

<template>
  <el-card v-loading="loading"><template #header><div style="display:flex;justify-content:space-between;align-items:center"><strong>退款管理</strong><el-select v-model="status" clearable placeholder="全部状态" style="width:140px" @change="load"><el-option label="待处理" value="PENDING" /><el-option label="已完成" value="APPROVED" /><el-option label="已拒绝" value="REJECTED" /></el-select></div></template>
    <el-alert title="当前仅记录线下/人工退款。请先在实际收款渠道完成退款，再点击“确认退款”；系统不会伪造渠道退款。" type="warning" :closable="false" style="margin-bottom:16px" />
    <el-table :data="records"><el-table-column prop="orderNo" label="订单号" min-width="160" /><el-table-column label="金额" width="100"><template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template></el-table-column><el-table-column prop="reason" label="申请原因" min-width="180" /><el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'APPROVED' ? 'success' : 'info'">{{ row.status === 'PENDING' ? '待处理' : row.status === 'APPROVED' ? '已完成' : '已拒绝' }}</el-tag></template></el-table-column><el-table-column prop="merchantNote" label="处理说明" min-width="160" /><el-table-column label="操作" width="160"><template #default="{ row }"><el-button v-if="row.status === 'PENDING'" text type="success" @click="process(row, true)">确认退款</el-button><el-button v-if="row.status === 'PENDING'" text type="danger" @click="process(row, false)">拒绝</el-button></template></el-table-column></el-table>
    <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="prev, pager, next" style="margin-top:16px" @current-change="load" />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createMerchantShippingRule, deleteMerchantShippingRule, getMerchantShippingRules, updateMerchantShippingRule, type MerchantShippingRule, type MerchantShippingRuleInput } from '../api/merchant'

const rules = ref<MerchantShippingRule[]>([])
const dialogVisible = ref(false)
const editingId = ref<number>()
const form = reactive<MerchantShippingRuleInput>({ ruleType: 'DELIVERY', name: '', baseFee: 0, freeShippingThreshold: null, status: 1, sortOrder: 0 })
const load = async () => { rules.value = (await getMerchantShippingRules()).data.data || [] }
const open = (rule?: MerchantShippingRule) => {
  editingId.value = rule?.id
  Object.assign(form, rule ? { ruleType: rule.ruleType, name: rule.name, baseFee: rule.baseFee, freeShippingThreshold: rule.freeShippingThreshold || null, status: rule.status, sortOrder: rule.sortOrder } : { ruleType: 'DELIVERY', name: '', baseFee: 0, freeShippingThreshold: null, status: 1, sortOrder: 0 })
  dialogVisible.value = true
}
const save = async () => {
  if (!form.name.trim() || form.baseFee === null) return ElMessage.warning('请填写规则名称和配送费用')
  if (editingId.value) await updateMerchantShippingRule(editingId.value, form); else await createMerchantShippingRule(form)
  ElMessage.success('已保存'); dialogVisible.value = false; await load()
}
const disable = async (rule: MerchantShippingRule) => { await ElMessageBox.confirm(`停用“${rule.name}”后消费者将无法选择它，历史订单不受影响。`, '确认停用'); await deleteMerchantShippingRule(rule.id); ElMessage.success('已停用'); await load() }
onMounted(load)
</script>

<template>
  <el-card>
    <template #header><div style="display:flex;justify-content:space-between;align-items:center"><strong>配送规则</strong><el-button type="primary" @click="open()">新增规则</el-button></div></template>
    <el-alert title="配送费用将在下单时写入订单快照；修改规则不会影响历史订单。" type="info" :closable="false" style="margin-bottom:16px" />
    <el-table :data="rules"><el-table-column prop="name" label="名称" /><el-table-column label="方式"><template #default="{ row }">{{ row.ruleType === 'PICKUP' ? '门店自提' : '配送' }}</template></el-table-column><el-table-column label="费用"><template #default="{ row }">{{ row.ruleType === 'PICKUP' ? '免运费' : `¥${Number(row.baseFee).toFixed(2)}` }}</template></el-table-column><el-table-column label="免运费门槛"><template #default="{ row }">{{ row.freeShippingThreshold ? `¥${Number(row.freeShippingThreshold).toFixed(2)}` : '-' }}</template></el-table-column><el-table-column label="状态"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="150"><template #default="{ row }"><el-button text type="primary" @click="open(row)">编辑</el-button><el-button v-if="row.status === 1" text type="danger" @click="disable(row)">停用</el-button></template></el-table-column></el-table>
  </el-card>
  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑配送规则' : '新增配送规则'" width="520px"><el-form label-width="110px"><el-form-item label="配送方式"><el-radio-group v-model="form.ruleType"><el-radio value="DELIVERY">配送</el-radio><el-radio value="PICKUP">门店自提</el-radio></el-radio-group></el-form-item><el-form-item label="规则名称"><el-input v-model="form.name" maxlength="50" /></el-form-item><el-form-item label="配送费用" v-if="form.ruleType === 'DELIVERY'"><el-input-number v-model="form.baseFee" :min="0" :precision="2" /></el-form-item><el-form-item label="免运费门槛" v-if="form.ruleType === 'DELIVERY'"><el-input-number v-model="form.freeShippingThreshold" :min="0.01" :precision="2" placeholder="不设置" /></el-form-item><el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item><el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item><el-form-item><el-button type="primary" @click="save">保存</el-button></el-form-item></el-form></el-dialog>
</template>

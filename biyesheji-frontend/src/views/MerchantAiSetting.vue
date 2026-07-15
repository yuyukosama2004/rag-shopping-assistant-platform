<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createMerchantAiKnowledge,
  deleteMerchantAiKnowledge,
  getMerchantAiKnowledge,
  getMerchantAiSetting,
  updateMerchantAiKnowledge,
  updateMerchantAiSetting,
  type MerchantAiKnowledge,
  type MerchantAiKnowledgeInput,
  type MerchantAiSettingInput,
} from '../api/merchant'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const knowledge = ref<MerchantAiKnowledge[]>([])
const knowledgeDialog = ref(false)
const editingKnowledgeId = ref<number>()
const canEdit = computed(() => userStore.user?.role === 1)
const form = reactive<MerchantAiSettingInput>({
  enabled: 1,
  model: 'deepseek-v4-flash',
  temperature: 0.7,
  maxOutputTokens: 1200,
  perUserDailyLimit: 30,
  disclaimer: '',
  systemPrompt: '',
})
const knowledgeForm = reactive<MerchantAiKnowledgeInput>({ category: 'FAQ', title: '', content: '', status: 1, sortOrder: 0 })
const categoryLabel: Record<MerchantAiKnowledge['category'], string> = {
  FAQ: '常见问题', SHIPPING: '配送说明', AFTER_SALES: '售后政策', STORE: '店铺信息',
}

const load = async () => {
  loading.value = true
  try {
    const [settingResponse, knowledgeResponse] = await Promise.all([getMerchantAiSetting(), getMerchantAiKnowledge()])
    const data = settingResponse.data.data
    Object.assign(form, data)
    knowledge.value = knowledgeResponse.data.data || []
  } finally {
    loading.value = false
  }
}

const openKnowledge = (item?: MerchantAiKnowledge) => {
  editingKnowledgeId.value = item?.id
  Object.assign(knowledgeForm, item
    ? { category: item.category, title: item.title, content: item.content, status: item.status, sortOrder: item.sortOrder }
    : { category: 'FAQ', title: '', content: '', status: 1, sortOrder: 0 })
  knowledgeDialog.value = true
}

const saveKnowledge = async () => {
  if (!knowledgeForm.title.trim() || !knowledgeForm.content.trim()) return ElMessage.warning('请填写标题和内容')
  if (editingKnowledgeId.value) await updateMerchantAiKnowledge(editingKnowledgeId.value, knowledgeForm)
  else await createMerchantAiKnowledge(knowledgeForm)
  ElMessage.success('知识条目已保存')
  knowledgeDialog.value = false
  await load()
}

const removeKnowledge = async (item: MerchantAiKnowledge) => {
  await ElMessageBox.confirm(`确定删除“${item.title}”吗？`, '删除知识条目', { type: 'warning' })
  await deleteMerchantAiKnowledge(item.id)
  ElMessage.success('已删除')
  await load()
}

const save = async () => {
  if (!form.model.trim()) return ElMessage.warning('请填写模型名称')
  saving.value = true
  try {
    await updateMerchantAiSetting(form)
    ElMessage.success('AI 设置已保存并立即生效')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <el-card v-loading="loading">
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between">
        <strong>AI 导购设置</strong>
        <el-tag :type="form.enabled === 1 ? 'success' : 'info'">{{ form.enabled === 1 ? '服务中' : '已暂停' }}</el-tag>
      </div>
    </template>

    <el-alert
      title="这些配置会直接影响消费者的 AI 对话。暂停后新请求将被拒绝；每日次数按登录用户独立计算。"
      type="info"
      :closable="false"
      style="margin-bottom:20px"
    />
    <el-alert v-if="!canEdit" title="店员账号只能查看，只有店主可以修改 AI 设置。" type="warning" :closable="false" style="margin-bottom:20px" />

    <el-form label-width="150px" style="max-width:800px" :disabled="!canEdit">
      <el-form-item label="启用 AI 导购">
        <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="对话模型">
        <el-input v-model="form.model" maxlength="100" placeholder="例如 deepseek-v4-flash" />
      </el-form-item>
      <el-form-item label="回答随机度">
        <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input />
        <div style="color:#909399;font-size:12px">数值越低回答越稳定；商品导购建议使用 0.3–0.8。</div>
      </el-form-item>
      <el-form-item label="最大输出 Token">
        <el-input-number v-model="form.maxOutputTokens" :min="100" :max="4000" :step="100" />
      </el-form-item>
      <el-form-item label="每用户每日次数">
        <el-input-number v-model="form.perUserDailyLimit" :min="1" :max="1000" />
      </el-form-item>
      <el-form-item label="系统提示词">
        <el-input v-model="form.systemPrompt" type="textarea" :rows="5" maxlength="4000" show-word-limit />
      </el-form-item>
      <el-form-item label="回答免责声明">
        <el-input v-model="form.disclaimer" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item v-if="canEdit">
        <el-button type="primary" :loading="saving" @click="save">保存并生效</el-button>
      </el-form-item>
    </el-form>
  </el-card>

  <el-card style="margin-top:20px">
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between">
        <strong>商家知识库</strong>
        <el-button type="primary" :disabled="knowledge.length >= 100" @click="openKnowledge()">新增知识</el-button>
      </div>
    </template>
    <el-alert title="仅启用的条目会进入 AI 对话上下文。适合维护常见问题、配送范围、售后规则和店铺说明，最多 100 条。" type="info" :closable="false" style="margin-bottom:16px" />
    <el-table :data="knowledge" empty-text="暂无知识条目">
      <el-table-column label="类型" width="120"><template #default="{ row }">{{ categoryLabel[row.category as MerchantAiKnowledge['category']] }}</template></el-table-column>
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="content" label="内容" min-width="320" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="150"><template #default="{ row }"><el-button text type="primary" @click="openKnowledge(row)">编辑</el-button><el-button text type="danger" @click="removeKnowledge(row)">删除</el-button></template></el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="knowledgeDialog" :title="editingKnowledgeId ? '编辑知识' : '新增知识'" width="620px">
    <el-form label-width="90px">
      <el-form-item label="类型"><el-select v-model="knowledgeForm.category" style="width:100%"><el-option v-for="(label, value) in categoryLabel" :key="value" :label="label" :value="value" /></el-select></el-form-item>
      <el-form-item label="标题"><el-input v-model="knowledgeForm.title" maxlength="100" show-word-limit /></el-form-item>
      <el-form-item label="内容"><el-input v-model="knowledgeForm.content" type="textarea" :rows="7" maxlength="2000" show-word-limit /></el-form-item>
      <el-form-item label="排序"><el-input-number v-model="knowledgeForm.sortOrder" :min="0" :max="10000" /></el-form-item>
      <el-form-item label="启用"><el-switch v-model="knowledgeForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="knowledgeDialog = false">取消</el-button><el-button type="primary" @click="saveKnowledge">保存</el-button></template>
  </el-dialog>
</template>

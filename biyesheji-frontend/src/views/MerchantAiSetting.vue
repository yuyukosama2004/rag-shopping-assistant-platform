<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMerchantAiSetting, updateMerchantAiSetting, type MerchantAiSettingInput } from '../api/merchant'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
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

const load = async () => {
  loading.value = true
  try {
    const data = (await getMerchantAiSetting()).data.data
    Object.assign(form, data)
  } finally {
    loading.value = false
  }
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
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref } from 'vue'
import { getErrorMessage } from '../api/request'
import { streamSse } from '../api/sse'

interface Msg { role: 'user' | 'assistant'; content: string }

const msgs = ref<Msg[]>([])
const query = ref('')
const loading = ref(false)
const box = ref<HTMLElement>()
let activeRequest: AbortController | null = null

const scroll = () => nextTick(() => { if (box.value) box.value.scrollTop = box.value.scrollHeight })

const send = async () => {
  const text = query.value.trim()
  if (!text || loading.value) return
  msgs.value.push({ role: 'user', content: text })
  query.value = ''
  loading.value = true
  const answer: Msg = { role: 'assistant', content: '' }
  msgs.value.push(answer)
  const controller = new AbortController()
  activeRequest = controller
  await scroll()

  try {
    await streamSse(`/api/order/ai/chat?query=${encodeURIComponent(text)}`, chunk => {
      answer.content += chunk
      scroll()
    }, { signal: controller.signal, connectTimeoutMs: 15000, idleTimeoutMs: 60000 })
    if (!answer.content) answer.content = 'AI 未返回内容，请稍后重试。'
  } catch (error) {
    if (!(error instanceof Error && error.name === 'AbortError')) {
      answer.content = getErrorMessage(error, '抱歉，AI 助手暂时无法响应，请重试。')
    }
  } finally {
    loading.value = false
    if (activeRequest === controller) activeRequest = null
    scroll()
  }
}

onBeforeUnmount(() => activeRequest?.abort())
</script>

<template>
  <div style="max-width:800px;margin:0 auto">
    <div class="section-title">AI 智能导购</div>
    <p style="color:#999;font-size:13px;margin-bottom:12px">告诉我你的预算和需求，AI 帮你挑选当前店铺真实可售商品。例如：“预算 3000 元左右，平时打游戏和拍照”。</p>
    <div ref="box" style="height:400px;overflow-y:auto;padding:14px;background:#fff;margin-bottom:12px" aria-live="polite">
      <div v-if="msgs.length === 0" style="text-align:center;color:#ccc;padding:60px 0">AI 导购已就绪</div>
      <div v-for="(message, index) in msgs" :key="index" :style="{ textAlign: message.role === 'user' ? 'right' : 'left', marginBottom: '10px' }">
        <div class="message" :class="message.role">
          <span v-if="message.role === 'assistant' && !message.content && loading" class="ai-thinking">AI 思考中</span>
          <span style="white-space:pre-wrap">{{ message.content }}</span>
        </div>
      </div>
    </div>
    <div style="display:flex;gap:8px"><el-input v-model="query" placeholder="输入购机需求..." size="large" :disabled="loading" @keyup.enter="send" /><el-button type="danger" size="large" :loading="loading" :disabled="loading || !query.trim()" @click="send">发送</el-button></div>
  </div>
</template>

<style scoped>
.message{display:inline-block;max-width:80%;padding:10px 14px;border-radius:6px;text-align:left;font-size:14px;line-height:1.6}.message.user{background:#e04a4f;color:#fff}.message.assistant{background:#f5f5f5;color:#333}.ai-thinking{color:#999;font-style:italic}
</style>

<script setup lang="ts">
import { ref, nextTick } from 'vue'


interface Message {
  role: 'user' | 'assistant'
  content: string
}

const messages = ref<Message[]>([])
const query = ref('')
const loading = ref(false)
const chatContainer = ref<HTMLElement>()

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  })
}

const send = () => {
  if (!query.value.trim()) return
  const userMsg = query.value.trim()
  messages.value.push({ role: 'user', content: userMsg })
  query.value = ''
  loading.value = true

  const assistantMsg: Message = { role: 'assistant', content: '' }
  messages.value.push(assistantMsg)
  scrollToBottom()

  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  const url = `${baseUrl}/api/order/ai/chat?query=${encodeURIComponent(userMsg)}`

  fetch(url)
    .then(res => {
      if (!res.ok || !res.body) throw new Error('Stream error')
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      const read = () => {
        reader.read().then(({ done, value }) => {
          if (done) { loading.value = false; return }
          buffer += decoder.decode(value, { stream: true })
          // Parse SSE lines
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''
          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.slice(5).trim()
              if (data && data !== '[DONE]') {
                // 后端已完成 JSON 解析，直接拼接纯文本
                assistantMsg.content += data
              }
            }
          }
          scrollToBottom()
          read()
        })
      }
      read()
    })
    .catch(() => {
      loading.value = false
      assistantMsg.content = '抱歉，AI助手暂时无法响应，请重试。'
    })
}
</script>

<template>
  <h1 class="page-header">AI 智能导购</h1>
  <p style="color:#666;margin-bottom:16px">告诉我你的预算和需求，AI帮你挑手机。例如："预算3000元左右，平时打游戏和拍照"</p>

  <el-card>
    <div ref="chatContainer" style="height:450px;overflow-y:auto;padding:16px;background:#fafafa;border-radius:8px;margin-bottom:16px">
      <div v-if="messages.length === 0" style="text-align:center;color:#999;padding:60px 0">
        <el-icon :size="48"><ChatDotRound /></el-icon>
        <p style="margin-top:12px">AI导购已就绪，输入需求开始对话</p>
      </div>
      <div v-for="(msg, idx) in messages" :key="idx" style="margin-bottom:12px" :style="{ textAlign: msg.role === 'user' ? 'right' : 'left' }">
        <div style="display:inline-block;max-width:80%;padding:10px 16px;border-radius:12px;text-align:left"
          :style="msg.role === 'user'
            ? 'background:#409EFF;color:#fff'
            : 'background:#fff;border:1px solid #e5e7eb'">
          <span style="white-space:pre-wrap">{{ msg.content }}</span>
          <el-icon v-if="loading && msg === messages[messages.length-1] && !msg.content" class="is-loading" style="margin-left:8px"><Loading /></el-icon>
        </div>
      </div>
    </div>

    <div style="display:flex;gap:8px">
      <el-input v-model="query" placeholder="输入你的购机需求..." size="large" @keyup.enter="send" :disabled="loading" />
      <el-button type="primary" size="large" @click="send" :disabled="loading || !query.trim()" :icon="'Position'">发送</el-button>
    </div>
  </el-card>
</template>

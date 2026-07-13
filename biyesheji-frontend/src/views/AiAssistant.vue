<script setup lang="ts">
import { ref, nextTick } from 'vue'
interface Msg { role:'user'|'assistant',content:string }
const msgs = ref<Msg[]>([]); const query = ref(''); const loading = ref(false); const box = ref<HTMLElement>()
const scroll = () => nextTick(() => { if (box.value) box.value.scrollTop = box.value.scrollHeight })
const send = () => {
  if (!query.value.trim()) return
  msgs.value.push({ role:'user', content: query.value.trim() }); const t = query.value.trim(); query.value = ''; loading.value = true
  const am: Msg = { role:'assistant', content:'' }; msgs.value.push(am); scroll()
  const base = import.meta.env.VITE_API_BASE_URL || ''
  fetch(`${base}/api/order/ai/chat?query=${encodeURIComponent(t)}`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('accessToken') || ''}` },
  })
    .then(res => { if (!res.ok || !res.body) throw new Error(); const r = res.body.getReader(); const d = new TextDecoder(); let b = ''
      const read = () => { r.read().then(({ done, value }) => { if (done) { loading.value = false; return }; b += d.decode(value, { stream:true }); const ls = b.split('\n'); b = ls.pop()||''
        for (const l of ls) { if (l.startsWith('data:')) { const dt = l.slice(5).trim(); if (dt && dt !== '[DONE]') am.content += dt } }; scroll(); read() }) }; read() })
    .catch(() => { loading.value = false; am.content = '抱歉，AI助手暂时无法响应，请重试。' })
}
</script>
<template>
  <div style="max-width:800px;margin:0 auto">
    <div class="section-title">AI 智能导购</div>
    <p style="color:#999;font-size:13px;margin-bottom:12px">告诉我你的预算和需求，AI帮你挑手机。例如："预算3000元左右，平时打游戏和拍照"</p>
    <div ref="box" style="height:400px;overflow-y:auto;padding:14px;background:#fff;margin-bottom:12px">
      <div v-if="msgs.length===0" style="text-align:center;color:#ccc;padding:60px 0">AI导购已就绪</div>
      <div v-for="(m,i) in msgs" :key="i" :style="{textAlign:m.role==='user'?'right':'left',marginBottom:'10px'}">
        <div style="display:inline-block;max-width:80%;padding:10px 14px;border-radius:6px;text-align:left;font-size:14px;line-height:1.6"
          :style="m.role==='user'?{background:'#E04A4F',color:'#fff'}:{background:'#f5f5f5',color:'#333'}">
          <span v-if="m.role==='assistant' && !m.content && loading" style="color:#999;font-style:italic">
            <span class="ai-thinking">AI 思考中</span>
          </span>
          <span style="white-space:pre-wrap">{{ m.content }}</span>
        </div>
      </div>
    </div>
    <div style="display:flex;gap:8px"><el-input v-model="query" placeholder="输入购机需求..." size="large" @keyup.enter="send" :disabled="loading" /><el-button type="danger" size="large" @click="send" :disabled="loading||!query.trim()">发送</el-button></div>
  </div>
</template>

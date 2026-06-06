<template>
  <div class="algo-page">
    <el-card shadow="hover" class="chat-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">AI智能对话助手</span>
          <el-tag type="primary">DeepSeek 大模型</el-tag>
        </div>
      </template>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px">
        <template #title>为管理人员提供系统操作答疑、数据统计分析建议，为医生提供老人健康管理辅助方案。</template>
      </el-alert>

      <div class="chat-container">
        <div class="chat-messages" ref="messagesRef">
          <template v-for="(msg, idx) in displayMessages" :key="msg.id">
            <div class="chat-msg" :class="msg.role">
              <div class="msg-avatar">
                <el-icon v-if="msg.role === 'assistant'" size="20" color="#1890ff"><Service /></el-icon>
                <el-icon v-else size="20" color="#52c41a"><User /></el-icon>
              </div>
              <div class="msg-bubble">
                <div class="msg-content" v-html="formatContent(msg.content)"></div>
                <span v-if="loading && idx === displayMessages.length - 1 && msg.role === 'assistant'" class="cursor-blink">▌</span>
              </div>
            </div>
          </template>
          <div v-if="loading && !streamingText" class="chat-msg assistant">
            <div class="msg-avatar"><el-icon size="20" color="#1890ff"><Service /></el-icon></div>
            <div class="msg-bubble"><span class="typing-dots">AI正在思考<span>...</span></span></div>
          </div>
        </div>

        <div class="chat-input">
          <div class="quick-questions">
            <el-tag v-for="q in quickQuestions" :key="q" size="small" effect="plain" class="quick-tag" @click="sendMessage(q)">{{ q }}</el-tag>
          </div>
          <div class="input-row">
            <el-input
              v-model="inputText"
              placeholder="输入您的问题..."
              size="large"
              @keyup.enter="sendMessage()"
              :disabled="loading"
            />
            <el-button type="primary" size="large" @click="sendMessage()" :loading="loading" :icon="Promotion">发送</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { Promotion } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'

let msgIdCounter = 0
const inputText = ref('')
const loading = ref(false)
const streamingText = ref('')
const messagesRef = ref(null)
const messages = ref([
  { id: ++msgIdCounter, role: 'assistant', content: '您好！我是智慧医养平台AI助手，可以为您提供：\n\n1. **系统操作指导** - 如何使用各功能模块\n2. **数据分析建议** - 统计数据解读和决策支持\n3. **健康管理方案** - 老人健康管理的专业建议\n\n请问有什么可以帮您的？' }
])

// 过滤掉空内容的消息（流式输出还没开始时隐藏空气泡）
const displayMessages = computed(() =>
  messages.value.filter(m => m.content.length > 0)
)

const quickQuestions = [
  '高血压老人日常护理建议',
  '如何降低老人跌倒风险？',
  '糖尿病老人饮食管理方案',
  '如何使用健康预警模块？'
]

function formatContent(text) {
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br/>')
}

async function scrollToBottom() {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

async function sendMessage(text) {
  const msg = text || inputText.value.trim()
  if (!msg || loading.value) return
  inputText.value = ''

  messages.value.push({ id: ++msgIdCounter, role: 'user', content: msg })
  // 预先添加空 assistant 消息占位（displayMessages 会过滤掉空的）
  messages.value.push({ id: ++msgIdCounter, role: 'assistant', content: '' })
  const assistantIdx = messages.value.length - 1
  await scrollToBottom()

  loading.value = true
  streamingText.value = ''

  try {
    // history 排除最后两条（刚加的 user + 空 assistant）
    const history = messages.value.slice(0, -2).map(m => ({ role: m.role, content: m.content }))
    const resp = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`
      },
      body: JSON.stringify({ message: msg, history })
    })

    if (!resp.ok) {
      messages.value[assistantIdx].content = '抱歉，AI服务暂时不可用（HTTP ' + resp.status + '）'
      return
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let fullText = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      const lines = buffer.split('\n')
      buffer = lines.pop()
      for (const line of lines) {
        if (!line.startsWith('data: ')) continue
        const data = line.slice(6).trim()
        if (data === '[DONE]') continue
        try {
          const json = JSON.parse(data)
          if (json.error) {
            fullText += '\n\n⚠️ ' + json.error
          } else if (json.content) {
            fullText += json.content
          }
        } catch {}
      }
      // 通过直接赋值 reactive 数组元素来触发 Vue 响应式更新
      messages.value[assistantIdx].content = fullText
      streamingText.value = fullText
      await scrollToBottom()
    }

    if (!messages.value[assistantIdx].content) {
      messages.value[assistantIdx].content = '抱歉，暂时无法回答。'
    }
  } catch (err) {
    messages.value[assistantIdx].content = '抱歉，AI服务暂时不可用。错误信息：' + (err?.message || '未知错误')
  } finally {
    loading.value = false
    streamingText.value = ''
    await scrollToBottom()
  }
}
</script>

<style scoped>
.algo-page { padding: 0; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-weight: 600; font-size: 16px; }
.chat-card :deep(.el-card__body) { padding: 0 20px 20px; }

.chat-container { display: flex; flex-direction: column; height: calc(100vh - 300px); min-height: 400px; }

.chat-messages {
  flex: 1; overflow-y: auto; padding: 16px 0;
  display: flex; flex-direction: column; gap: 16px;
}
.chat-msg { display: flex; gap: 10px; max-width: 80%; }
.chat-msg.user { align-self: flex-end; flex-direction: row-reverse; }
.chat-msg.assistant { align-self: flex-start; }

.msg-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; background: #f0f5ff;
}
.chat-msg.user .msg-avatar { background: #f6ffed; }

.msg-bubble {
  background: #f0f5ff; border-radius: 12px; padding: 12px 16px;
  font-size: 14px; line-height: 1.6; color: #333; word-break: break-word;
}
.chat-msg.user .msg-bubble { background: #e6f7ff; }

.typing-dots span { animation: blink 1.2s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }

.chat-input { border-top: 1px solid #f0f0f0; padding-top: 12px; }
.quick-questions { display: flex; gap: 8px; margin-bottom: 10px; flex-wrap: wrap; }
.quick-tag { cursor: pointer; transition: all 0.2s; }
.quick-tag:hover { color: #1890ff; border-color: #1890ff; }
.input-row { display: flex; gap: 10px; }
.input-row .el-input { flex: 1; }
.cursor-blink { display: inline; animation: cursorBlink 0.8s step-end infinite; color: #1890ff; }
@keyframes cursorBlink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }
</style>

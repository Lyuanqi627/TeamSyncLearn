<template>
  <div class="ai-assistant">
    <div class="chat-header">
      <div class="header-info">
        <h2>AI 助手</h2>
        <p class="subtitle">基于 Dify 智能 Agent，随时为你解答学习问题</p>
      </div>
      <div class="header-actions">
        <el-button size="small" @click="clearConversation" :disabled="!messages.length">
          <el-icon><Delete /></el-icon>
          清空会话
        </el-button>
      </div>
    </div>

    <div class="chat-body" ref="chatBodyRef">
      <div v-if="!messages.length" class="welcome">
        <div class="welcome-icon">
          <el-icon :size="48"><ChatDotSquare /></el-icon>
        </div>
        <h3>你好！我是 AI 学习助手</h3>
        <p>你可以问我任何学习相关的问题，例如：</p>
        <div class="suggestions">
          <el-tag
            v-for="(item, index) in suggestions"
            :key="index"
            class="suggestion-tag"
            @click="sendQuickChat(item)"
          >
            {{ item }}
          </el-tag>
        </div>
      </div>

      <div v-for="(msg, index) in messages" :key="index" class="message-wrapper">
        <div class="message" :class="msg.role">
          <div class="message-avatar">
            <el-avatar v-if="msg.role === 'user'" :size="36" icon="UserFilled" />
            <el-avatar v-else :size="36" style="background: #409eff">
              <el-icon><MagicStick /></el-icon>
            </el-avatar>
          </div>
          <div class="message-content">
            <div class="message-bubble" v-html="renderMessage(msg.content)"></div>
            <div v-if="msg.loading" class="typing-indicator">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input-area">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="3"
        placeholder="请输入你的问题..."
        :disabled="sending"
        @keydown.enter.prevent="sendMessage"
        resize="none"
      />
      <div class="input-actions">
        <span class="input-hint">按 Enter 发送</span>
        <el-button type="primary" :loading="sending" @click="sendMessage" :disabled="!inputText.trim()">
          <el-icon><Promotion /></el-icon>
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { sendChatMessageStream } from '@/api/ai-assistant'
import { Delete, ChatDotSquare, MagicStick, Promotion, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
}

const inputText = ref('')
const messages = ref<ChatMessage[]>([])
const sending = ref(false)
const conversationId = ref('')
const chatBodyRef = ref<HTMLElement | null>(null)

const suggestions = [
  '帮我总结今天的学习重点',
  '如何制定有效的学习计划？',
  '解释一下项目管理中的关键路径法',
  '推荐一些提高学习效率的方法'
]

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

function renderMessage(content: string): string {
  // 1. 先转义 HTML，防止 XSS 和意外标签解析
  let html = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // 2. Markdown 块级元素（代码块优先，避免内部内容被后续规则误处理）
  html = html.replace(/```(\w*)\n?([\s\S]*?)```/g, (_, lang, code) => {
    const langClass = lang ? ` class="language-${lang}"` : ''
    return `<pre class="code-block"><code${langClass}>${code}</code></pre>`
  })
  // 内联代码
  html = html.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
  // 加粗
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  // 斜体
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

  return html
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  inputText.value = ''
  messages.value.push({ role: 'user', content: text })

  // 添加加载中的占位
  const loadingIdx = messages.value.length
  messages.value.push({ role: 'assistant', content: '', loading: true })
  sending.value = true
  scrollToBottom()

  let streamEnded = false

  sendChatMessageStream(
    text,
    conversationId.value,
    (data) => {
      const { event, answer, conversation_id } = data

      // 追加增量文本（Dify Agent 模式每个 event 只发一个字）
      if (answer) {
        messages.value[loadingIdx].content += answer
        messages.value[loadingIdx].loading = false
      }

      // 保存会话 ID
      if (conversation_id) {
        conversationId.value = conversation_id
      }

      // 结束事件
      if (event === 'message_end') {
        streamEnded = true
        sending.value = false
        scrollToBottom()
      }

      // 错误事件
      if (event === 'error') {
        streamEnded = true
        messages.value[loadingIdx] = {
          role: 'assistant',
          content: '抱歉，AI 服务出错，请稍后再试。'
        }
        sending.value = false
        scrollToBottom()
      }
    },
    (error) => {
      if (streamEnded) return
      streamEnded = true
      messages.value[loadingIdx] = {
        role: 'assistant',
        content: '抱歉，网络异常，请检查连接后重试。'
      }
      sending.value = false
      scrollToBottom()
    }
  )
}

function sendQuickChat(text: string) {
  inputText.value = text
  sendMessage()
}

function clearConversation() {
  if (!messages.value.length) return
  ElMessageBox.confirm('确定清空当前会话吗？', '提示')
    .then(() => {
      messages.value = []
      conversationId.value = ''
      ElMessage.success('已清空')
    })
    .catch(() => {})
}
</script>

<style scoped>
.ai-assistant {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px - 48px);
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

/* ===== Header ===== */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}

.header-info h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #909399;
}

/* ===== Chat Body ===== */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #f8f9fb;
}

/* ===== Welcome ===== */
.welcome {
  text-align: center;
  padding: 60px 20px;
}

.welcome-icon {
  color: #409eff;
  margin-bottom: 16px;
}

.welcome h3 {
  font-size: 20px;
  color: #303133;
  margin: 0 0 8px;
}

.welcome p {
  color: #909399;
  margin: 0 0 20px;
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  max-width: 500px;
  margin: 0 auto;
}

.suggestion-tag {
  cursor: pointer;
  padding: 6px 16px;
  font-size: 13px;
  border-radius: 20px;
  transition: all 0.2s;
}

.suggestion-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

/* ===== Messages ===== */
.message-wrapper {
  margin-bottom: 16px;
}

.message {
  display: flex;
  gap: 12px;
  max-width: 80%;
  width: fit-content;
}

.message.assistant {
  margin-right: auto;
}

.message.user {
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.message.user .message-bubble {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message.assistant .message-bubble {
  background: #fff;
  color: #303133;
  border: 1px solid #e4e7ed;
  border-bottom-left-radius: 4px;
}

/* ===== Code Styling ===== */
:deep(.code-block) {
  background: #282c34;
  color: #abb2bf;
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 13px;
  margin: 8px 0;
}

:deep(.inline-code) {
  background: #f0f2f5;
  color: #e64231;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

/* ===== Typing Indicator ===== */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  border-bottom-left-radius: 4px;
  align-items: center;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  animation: bounce 1.4s ease-in-out infinite;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  40% {
    transform: translateY(-8px);
    opacity: 1;
  }
}

/* ===== Input Area ===== */
.chat-input-area {
  padding: 16px 24px;
  border-top: 1px solid #ebeef5;
  background: #fff;
  flex-shrink: 0;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.input-hint {
  font-size: 12px;
  color: #c0c4cc;
}
</style>

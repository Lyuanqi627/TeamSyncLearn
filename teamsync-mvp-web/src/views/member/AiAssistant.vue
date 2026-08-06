<template>
  <div class="ai-assistant">
    <!-- 左侧会话列表 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <el-button type="primary" size="small" class="new-chat-btn" :disabled="sending" @click="newChat">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>
      </div>
      <div class="conversation-list" v-loading="conversationsLoading">
        <div v-if="!conversationsLoading && !conversations.length" class="no-conversations">
          暂无历史会话
        </div>
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: conv.id === activeConversationId }"
          @click="switchConversation(conv.id)"
        >
          <div class="conv-main">
            <span class="conv-name" :title="conv.name">{{ conv.name || '新对话' }}</span>
            <span class="conv-time">{{ formatTime(conv.updated_at) }}</span>
          </div>
          <el-dropdown trigger="click" @click.stop @command="(cmd: string) => handleConvCommand(cmd, conv)">
            <el-icon class="conv-more"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">重命名</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区 -->
    <div class="chat-area">
      <div class="chat-header">
        <div class="header-info">
          <h2>AI 助手</h2>
          <p class="subtitle">基于 Dify 智能 Agent，随时为你解答学习问题</p>
        </div>
        <div class="header-actions">
          <el-button size="small" @click="deleteCurrentConversation" :disabled="!activeConversationId">
            <el-icon><Delete /></el-icon>
            删除会话
          </el-button>
        </div>
      </div>

      <div class="chat-body" ref="chatBodyRef">
        <div v-if="hasMoreMessages && messages.length" class="load-more-bar">
          <el-button size="small" text type="primary" :loading="loadingMore" @click="loadMore">
            加载更早的消息
          </el-button>
        </div>

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
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, computed } from 'vue'
import { sendChatMessageStream, getConversations, getMessages, deleteConversation, renameConversation } from '@/api/ai-assistant'
import { Delete, ChatDotSquare, MagicStick, Promotion, UserFilled, Plus, MoreFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
}

interface Conversation {
  id: string
  name: string
  created_at?: number
  updated_at?: number
}

const inputText = ref('')
const messages = ref<ChatMessage[]>([])
const sending = ref(false)
const activeConversationId = ref('')
const chatBodyRef = ref<HTMLElement | null>(null)

// 会话列表状态
const conversations = ref<Conversation[]>([])
const conversationsLoading = ref(false)
const hasMoreMessages = ref(false)
const loadingMore = ref(false)
const firstMessageId = ref<string | null>(null)

const userStore = useUserStore()
const uid = computed(() => userStore.userInfo?.userId ?? 'anonymous')
const lastActiveKey = computed(() => `ai-chat:${uid.value}:active`)

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

function formatTime(ts?: number): string {
  if (!ts) return ''
  const d = new Date(ts * 1000)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}小时前`
  if (now.getFullYear() === d.getFullYear()) return `${d.getMonth() + 1}-${d.getDate()}`
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
}

// Dify /messages 接口按升序（旧→新）返回（pagination_by_first_id 默认 order="asc"），直接顺序遍历即可
function mapMessages(data: any[]): ChatMessage[] {
  const result: ChatMessage[] = []
  for (const item of data) {
    result.push({ role: 'user', content: item.query || '' })
    if (item.answer) {
      result.push({ role: 'assistant', content: item.answer })
    }
  }
  return result
}

async function fetchConversations() {
  conversationsLoading.value = true
  try {
    const res: any = await getConversations({ limit: 30 })
    if (res.code === 200 && res.data?.data) {
      conversations.value = res.data.data
    }
  } catch {
    ElMessage.error('加载会话列表失败')
  } finally {
    conversationsLoading.value = false
  }
}

async function loadMessages(conversationId: string, prepend = false) {
  const params: any = { conversation_id: conversationId, limit: 20 }
  if (prepend && firstMessageId.value) {
    params.first_id = firstMessageId.value
  }
  const res: any = await getMessages(params)
  if (res.code === 200 && res.data?.data) {
    const mapped = mapMessages(res.data.data)
    messages.value = prepend ? [...mapped, ...messages.value] : mapped
    hasMoreMessages.value = !!res.data.has_more
    const data = res.data.data
    firstMessageId.value = data.length ? data[data.length - 1].id : null
    scrollToBottom()
  }
}

async function switchConversation(id: string) {
  if (sending.value || id === activeConversationId.value) return
  activeConversationId.value = id
  localStorage.setItem(lastActiveKey.value, id)
  messages.value = []
  hasMoreMessages.value = false
  firstMessageId.value = null
  try {
    await loadMessages(id)
  } catch {
    ElMessage.error('加载历史消息失败')
  }
}

async function loadMore() {
  if (loadingMore.value || !hasMoreMessages.value || !activeConversationId.value) return
  loadingMore.value = true
  try {
    await loadMessages(activeConversationId.value, true)
  } catch {
    ElMessage.error('加载更早消息失败')
  } finally {
    loadingMore.value = false
  }
}

function newChat() {
  if (sending.value) return
  activeConversationId.value = ''
  localStorage.removeItem(lastActiveKey.value)
  messages.value = []
  hasMoreMessages.value = false
  firstMessageId.value = null
}

async function deleteCurrentConversation() {
  if (!activeConversationId.value) return
  try {
    await ElMessageBox.confirm('确定删除当前会话吗？删除后不可恢复。', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteConversation(activeConversationId.value)
    conversations.value = conversations.value.filter(c => c.id !== activeConversationId.value)
    newChat()
    ElMessage.success('已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

function handleConvCommand(cmd: string, conv: Conversation) {
  if (cmd === 'rename') {
    ElMessageBox.prompt('请输入新的会话名称', '重命名会话', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: conv.name || ''
    }).then(async ({ value }) => {
      const name = (value || '').trim()
      if (!name) {
        ElMessage.warning('名称不能为空')
        return
      }
      const res: any = await renameConversation(conv.id, name)
      if (res.code === 200) {
        ElMessage.success('已重命名')
        fetchConversations()
      }
    }).catch(() => {})
  } else if (cmd === 'delete') {
    ElMessageBox.confirm(`确定删除会话「${conv.name || '新对话'}」吗？删除后不可恢复。`, '提示', { type: 'warning' })
      .then(async () => {
        await deleteConversation(conv.id)
        conversations.value = conversations.value.filter(c => c.id !== conv.id)
        if (conv.id === activeConversationId.value) {
          newChat()
        }
        ElMessage.success('已删除')
      })
      .catch(() => {})
  }
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
    activeConversationId.value,
    (data) => {
      const { event, answer, conversation_id } = data

      // 追加增量文本（Dify Agent 模式每个 event 只发一个字）
      if (answer) {
        messages.value[loadingIdx].content += answer
        messages.value[loadingIdx].loading = false
      }

      // 保存会话 ID（新会话首次回复时产生）
      if (conversation_id) {
        activeConversationId.value = conversation_id
      }

      // 结束事件
      if (event === 'message_end') {
        streamEnded = true
        sending.value = false
        if (activeConversationId.value) {
          localStorage.setItem(lastActiveKey.value, activeConversationId.value)
          fetchConversations()
        }
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

onMounted(() => {
  fetchConversations().then(() => {
    // 恢复上次活跃的会话（如果还在列表里）
    const lastActive = localStorage.getItem(lastActiveKey.value)
    if (lastActive && conversations.value.some(c => c.id === lastActive)) {
      switchConversation(lastActive)
    }
  })
})
</script>

<style scoped>
.ai-assistant {
  display: flex;
  height: calc(100vh - 60px - 48px);
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

/* ===== 左侧会话列表 ===== */
.sidebar {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #ebeef5;
  background: #fafafa;
}

.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
}

.new-chat-btn {
  width: 100%;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.no-conversations {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 32px 0;
}

.conversation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.15s;
}

.conversation-item:hover {
  background: #f0f2f5;
}

.conversation-item.active {
  background: #e6f0ff;
}

.conv-main {
  flex: 1;
  min-width: 0;
}

.conv-name {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #303133;
}

.conv-time {
  display: block;
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}

.conv-more {
  color: #909399;
  cursor: pointer;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s;
}

.conversation-item:hover .conv-more,
.conversation-item.active .conv-more {
  opacity: 1;
}

/* ===== 右侧聊天区 ===== */
.chat-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

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

.load-more-bar {
  text-align: center;
  padding-bottom: 8px;
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

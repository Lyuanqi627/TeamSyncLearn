# Dify Agent API 接入文档

## 概述

本文档说明如何将 Dify AI Agent 接入到 TeamSync 项目。后端使用 Spring Boot 代理 Dify 的 SSE 流式接口，前端使用 XHR 接收流式响应并逐字渲染。

---

## 1. 配置

### Dify 侧

创建 Agent 应用后，在 **应用设置 → API 密钥** 中生成一个密钥。

### 后端配置 (`application.yml`)

```yaml
dify:
  api-base-url: http://localhost/v1      # Dify API 地址（通过 nginx 反向代理）
  api-key: ${DIFY_API_KEY:app-xxxxx}     # API 密钥
```

> Dify 官方 API 端口为 5001，通常通过 nginx 将 `/v1` 代理到 `http://api:5001`。

### 前端代理 (`vite.config.ts`)

```ts
proxy: {
  '/api': 'http://localhost:8080'   # 将前端请求代理到 Spring Boot 后端
}
```

---

## 2. API 接口

### POST `/api/ai-assistant/chat`

调用 Dify Agent 的流式对话接口。

**请求体：**

```json
{
  "query": "用户问题",
  "conversation_id": ""      // 空字符串表示新会话
}
```

**响应格式：** `text/event-stream`（SSE）

每条数据以 `data: {json}\n\n` 格式推送。

### GET `/api/ai-assistant/conversations`

查询会话历史列表。

### POST `/api/ai-assistant/conversations/delete`

删除指定会话。

```json
{
  "conversation_id": "xxx"
}
```

---

## 3. 后端实现要点

### SSE 流式转发

后端通过 `HttpURLConnection` 调用 Dify API，逐行读取 SSE 数据并直接写入 `HttpServletResponse`：

```java
// 设置 SSE 响应头
response.setContentType("text/event-stream");
response.setCharacterEncoding("UTF-8");
response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
response.setHeader("X-Accel-Buffering", "no");

// 构造请求体（关键：inputs 中必须传 userId）
JSONObject inputs = JSONUtil.createObj();
inputs.set("userId", userId.toString());

JSONObject body = JSONUtil.createObj()
    .set("inputs", inputs)
    .set("query", query)
    .set("response_mode", "streaming")     // 必须为 streaming
    .set("conversation_id", conversationId)
    .set("user", userId.toString());

// 逐行转发并立即 flush
while ((line = reader.readLine()) != null) {
    writer.write(line + "\n");
    writer.flush();
}
```

### 关键细节

| 要点 | 说明 |
|------|------|
| `response_mode` | 必须设为 `"streaming"`，Agent 模式不支持 `"blocking"` |
| `inputs.userId` | Dify Agent 工作流要求传入用户标识变量 |
| 显式 flush | 每次 `writer.write()` 后必须 `writer.flush()`，否则 Tomcat 缓冲区会延迟发送 |
| `X-Accel-Buffering: no` | 禁用 nginx 缓冲，确保流式数据实时到达 |

---

## 4. 前端实现要点

### XHR 流式接收

使用 `XMLHttpRequest` 的 `onprogress` 事件而非 `fetch` + `ReadableStream`（后者在部分浏览器和代理环境下存在兼容性问题）：

```typescript
export function sendChatMessageStream(
  query: string,
  conversationId: string,
  onData: (data: any) => void,
  onError: (error: string) => void
): AbortController {
  const controller = new AbortController()
  const token = localStorage.getItem('token') || ''
  const xhr = new XMLHttpRequest()

  xhr.open('POST', '/api/ai-assistant/chat')
  xhr.setRequestHeader('Content-Type', 'application/json')
  xhr.setRequestHeader('Authorization', token)

  let lastIndex = 0
  let buffer = ''

  xhr.onprogress = () => {
    const newText = xhr.responseText.substring(lastIndex)
    lastIndex = xhr.responseText.length
    processChunk(newText)
  }

  xhr.onerror = () => onError('网络请求失败')

  xhr.send(JSON.stringify({ query, conversation_id: conversationId }))

  controller.signal.addEventListener('abort', () => xhr.abort())
  return controller
}
```

### 增量文本追加

Dify Agent 模式下，每个 `agent_message` 事件只包含**一个字符或标点**作为增量，必须追加而非覆盖：

```typescript
// ✅ 正确：追加增量文本
if (answer) {
  messages.value[loadingIdx].content += answer
}

// ❌ 错误：会丢失之前累积的文本
if (answer !== undefined) {
  messages.value[loadingIdx].content = answer
}
```

---

## 5. Dify Agent 事件流

### 事件顺序

```
agent_thought    →  Agent 的思考过程（无 answer 字段）
agent_message    →  增量文本块（answer: 单个字符/标点）
agent_message    →  ...
agent_message    →  answer: ""（空字符串，需跳过）
agent_thought    →  完整思考内容（thought 字段包含完整消息）
message_end      →  流结束（无 answer 字段，含 usage 元数据）
```

### 事件类型

| event | 说明 | 关键字段 |
|-------|------|----------|
| `agent_thought` | 中间思考过程 | `thought`, `observation`, `tool` |
| `agent_message` | 增量文本输出 | `answer`（每次一个字） |
| `message_end` | 流结束 | `metadata.usage`（token 消耗） |

### 注意事项

1. **空 answer 事件**：Dify 在最后一条 `agent_message` 中会发送 `answer:""`，前端判断时需用 `if (answer)` 而非 `if (answer !== undefined)` 跳过空字符串
2. **增量而非完整文本**：每个 `agent_message` 只携带一个字，不能在赋值时用 `=` 覆盖
3. **结束检测**：检测到 `message_end` 事件后停止加载状态

---

## 6. 完整数据流

```
用户输入 → 前端 XHR POST → Spring Boot 后端
  → HttpURLConnection POST Dify API
    → Dify 返回 SSE 流
  → 后端逐行读取并 flush 到 response
→ 前端 onprogress 接收行数据
  → 拆分行、解析 JSON
  → 逐字追加到消息内容
  → 检测 message_end 结束
```

---

## 7. 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 气泡只显示最后一两个字 | `=` 赋值覆盖了增量文本 | 改为 `+=` 追加 |
| 气泡一直加载中 | Tomcat 缓冲未 flush | 每次 write 后显式调用 flush() |
| 消息内容被清空 | 空字符串 `answer:""` 覆盖了内容 | 用 `if (answer)` 判断真值 |
| 接口返回 401 | 未传 Authorization 头 | 检查前端 token 传递和后端 userId 获取 |

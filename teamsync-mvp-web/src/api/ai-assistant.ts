import request from './request'

export function getConversations(params?: { last_id?: string; limit?: number }) {
  return request.get('/api/ai-assistant/conversations', { params })
}

export function getMessages(params: { conversation_id: string; first_id?: string; limit?: number }) {
  return request.get('/api/ai-assistant/messages', { params })
}

export function deleteConversation(conversationId: string) {
  return request.post('/api/ai-assistant/conversations/delete', {
    conversation_id: conversationId
  })
}

export function renameConversation(conversationId: string, name: string) {
  return request.post('/api/ai-assistant/conversations/rename', {
    conversation_id: conversationId,
    name
  })
}

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

  function processChunk(chunk: string) {
    buffer += chunk
    const lines = buffer.split('\n')
    buffer = lines.pop() || '' // keep incomplete line in buffer

    for (const line of lines) {
      const trimmed = line.trim()
      if (trimmed.startsWith('data: ')) {
        try {
          const data = JSON.parse(trimmed.slice(6))
          onData(data)
        } catch {
          // skip malformed JSON lines
        }
      }
    }
  }

  xhr.onprogress = () => {
    const newText = xhr.responseText.substring(lastIndex)
    lastIndex = xhr.responseText.length
    processChunk(newText)
  }

  xhr.onerror = () => onError('网络请求失败')

  xhr.onloadend = () => {
    // flush remaining buffer
    const trimmed = buffer.trim()
    if (trimmed.startsWith('data: ')) {
      try {
        onData(JSON.parse(trimmed.slice(6)))
      } catch { /* skip */ }
    }
  }

  xhr.send(JSON.stringify({
    query,
    conversation_id: conversationId
  }))

  controller.signal.addEventListener('abort', () => {
    xhr.abort()
  })

  return controller
}

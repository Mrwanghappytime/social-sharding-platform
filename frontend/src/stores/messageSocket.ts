import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useMessageStore } from '@/stores/message'

const IDLE_CLOSE_MS = 5 * 60 * 1000
const HEARTBEAT_MS = 30 * 1000

const buildMessageWebSocketUrl = (token: string): string => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/message?token=${token}`
}

export const useMessageSocketStore = defineStore('messageSocket', () => {
  const connected = ref(false)
  const activeConversationId = ref<number | null>(null)
  let ws: WebSocket | null = null
  let idleTimer: ReturnType<typeof setTimeout> | null = null
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null

  const clearIdleTimer = () => {
    if (idleTimer) {
      clearTimeout(idleTimer)
      idleTimer = null
    }
  }

  const clearHeartbeat = () => {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  const joinConversation = (conversationId: number) => {
    clearIdleTimer()
    activeConversationId.value = conversationId
    connect()
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'JOIN_CONVERSATION', conversationId }))
    }
  }

  const connect = () => {
    if (ws && ws.readyState === WebSocket.OPEN) return
    const token = localStorage.getItem('token')
    if (!token) return
    ws = new WebSocket(buildMessageWebSocketUrl(token))

    ws.onopen = () => {
      console.log('Connecting to WebSocket:', buildMessageWebSocketUrl(token).replace(/token=[^&]+/, 'token=***'))
      connected.value = true
      clearHeartbeat()
      heartbeatTimer = setInterval(() => {
        if (ws?.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({ type: 'ping' }))
        }
      }, HEARTBEAT_MS)
      if (activeConversationId.value) {
        joinConversation(activeConversationId.value)
      }
    }

    ws.onmessage = (event) => {
      console.log("recieve message:" + event.data);
      const data = JSON.parse(event.data)
      if (data.type === 'pong' || data.type === 'JOINED') return
      if (data.type === 'MESSAGE' && data.conversationId === activeConversationId.value) {
        useMessageStore().appendIncomingMessage(data.message)
      }
    }

    ws.onclose = () => {
      connected.value = false
      clearHeartbeat()
      const shouldReconnect = activeConversationId.value != null
      ws = null
      if (shouldReconnect) {
        setTimeout(connect, 1000)
      }
    }
  }

  const leaveConversation = (conversationId: number) => {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'LEAVE_CONVERSATION', conversationId }))
    }
    if (activeConversationId.value === conversationId) {
      activeConversationId.value = null
    }
  }

  const scheduleIdleClose = () => {
    clearIdleTimer()
    idleTimer = setTimeout(() => {
      disconnect()
    }, IDLE_CLOSE_MS)
  }

  const disconnect = () => {
    clearIdleTimer()
    clearHeartbeat()
    activeConversationId.value = null
    if (ws) {
      ws.close()
      ws = null
    }
    connected.value = false
  }

  return {
    connected,
    activeConversationId,
    connect,
    joinConversation,
    leaveConversation,
    scheduleIdleClose,
    disconnect
  }
})

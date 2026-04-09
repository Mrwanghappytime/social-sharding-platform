import { ElMessage } from 'element-plus'

let ws: WebSocket | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null

export const useWebSocket = (
  userId: number,
  onMessage: (data: any) => void,
  onConnect?: (connected: boolean) => void
) => {
  const connect = () => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      return
    }

    const token = localStorage.getItem('token')
    if (!token) {
      console.warn('No token found, cannot connect to WebSocket')
      return
    }

    // Pass token as query parameter for JWT validation
    // Connect directly to notification-service, userId extracted from JWT on server
    const wsUrl = `ws://localhost:8085/ws/notify?token=${token}`

    try {
      ws = new WebSocket(wsUrl)

      ws.onopen = () => {
        console.log('WebSocket connected')
        onConnect?.(true)

        // Start heartbeat
        heartbeatTimer = setInterval(() => {
          if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ type: 'ping' }))
          }
        }, 30000)
      }

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (data.type === 'pong') return
          onMessage(data)
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error)
        }
      }

      ws.onerror = (error) => {
        console.error('WebSocket error:', error)
        ElMessage.error('WebSocket connection error')
      }

      ws.onclose = () => {
        console.log('WebSocket disconnected')
        onConnect?.(false)

        if (heartbeatTimer) {
          clearInterval(heartbeatTimer)
          heartbeatTimer = null
        }

        // Reconnect after 5 seconds
        if (reconnectTimer) {
          clearTimeout(reconnectTimer)
        }
        reconnectTimer = setTimeout(() => {
          connect()
        }, 5000)
      }
    } catch (error) {
      console.error('Failed to create WebSocket:', error)
    }
  }

  const disconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }

    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }

    if (ws) {
      ws.close()
      ws = null
    }
  }

  const send = (data: any) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(data))
    }
  }

  // Connect immediately
  connect()

  return {
    connect,
    disconnect,
    send
  }
}

import { ElMessage } from 'element-plus'

let ws: WebSocket | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null

// 指数退避配置
const INITIAL_RECONNECT_DELAY = 1000   // 初始 1s
const MAX_RECONNECT_DELAY = 30000      // 最大 30s
let currentReconnectDelay = INITIAL_RECONNECT_DELAY
let manualClosed = false               // 标记是否主动关闭，避免重连

/**
 * 构造 WebSocket URL，通过 Gateway 反向代理到 notification-service 集群。
 * - 开发模式：vite dev server 端口 3000，vite.config.ts 中 /ws 已代理到 ws://localhost:8080
 * - 生产模式：直接走当前 host（部署时 Gateway 应在同一域名/端口下）
 *
 * 这样即可：
 *   1. 不再写死 notification-service 的 IP/端口
 *   2. Gateway 自动通过 Nacos 做负载均衡和故障剔除
 *   3. 协议自动跟随页面（http→ws, https→wss）
 */
const buildWebSocketUrl = (token: string): string => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/notify?token=${token}`
}

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

    manualClosed = false
    const wsUrl = buildWebSocketUrl(token)
    console.log('Connecting to WebSocket:', wsUrl.replace(/token=[^&]+/, 'token=***'))

    try {
      ws = new WebSocket(wsUrl)

      ws.onopen = () => {
        console.log('WebSocket connected')
        currentReconnectDelay = INITIAL_RECONNECT_DELAY  // 重置退避延迟
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
      }

      ws.onclose = (event) => {
        console.log(`WebSocket disconnected (code=${event.code}, reason=${event.reason})`)
        onConnect?.(false)

        if (heartbeatTimer) {
          clearInterval(heartbeatTimer)
          heartbeatTimer = null
        }

        // 主动关闭不重连
        if (manualClosed) return

        // 指数退避重连（1s → 2s → 4s → ... → 30s 封顶）
        if (reconnectTimer) {
          clearTimeout(reconnectTimer)
        }
        const delay = currentReconnectDelay
        console.log(`Reconnecting in ${delay}ms...`)
        reconnectTimer = setTimeout(() => {
          connect()
        }, delay)
        currentReconnectDelay = Math.min(currentReconnectDelay * 2, MAX_RECONNECT_DELAY)
      }
    } catch (error) {
      console.error('Failed to create WebSocket:', error)
    }
  }

  const disconnect = () => {
    manualClosed = true

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
    currentReconnectDelay = INITIAL_RECONNECT_DELAY
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

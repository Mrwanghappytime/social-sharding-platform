import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElNotification } from 'element-plus'
import { useWebSocket } from '@/utils/websocket'
import { getUnreadCount as apiGetUnreadCount, markAsRead as apiMarkAsRead } from '@/api/notification'

export interface Notification {
  id: number
  type: string
  actorId: number
  actorUsername?: string
  actorAvatar?: string
  targetId: number
  targetType: string
  isRead: boolean
  createdAt?: string
}

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<Notification[]>([])
  const unreadCount = ref(0)
  const wsConnected = ref(false)
  const currentUserId = ref<number | null>(null)

  // Fetch unread count from API
  const fetchUnreadCount = async (userId: number) => {
    try {
      const res = await apiGetUnreadCount(userId)
      if (res.data !== null && res.data !== undefined) {
        unreadCount.value = res.data as number
      }
    } catch (error) {
      console.error('Failed to fetch unread count:', error)
    }
  }

  // Mark single notification as read via API
  const markAsRead = async (notificationId: number, userId: number) => {
    try {
      await apiMarkAsRead(notificationId, userId)
      const notification = notifications.value.find(n => n.id === notificationId)
      if (notification && !notification.isRead) {
        notification.isRead = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch (error) {
      console.error('Failed to mark as read:', error)
    }
  }

  // Mark all as read
  const markAllAsRead = () => {
    notifications.value.forEach(n => {
      n.isRead = true
    })
    unreadCount.value = 0
  }

  // Clear notifications
  const clearNotifications = () => {
    notifications.value = []
    unreadCount.value = 0
  }

  // Push notification (for API data already in correct order)
  const pushNotification = (notification: Notification) => {
    notifications.value.push(notification)
    if (!notification.isRead) {
      unreadCount.value++
    }
  }

  // Get notification message content
  const getNotificationMessage = (notification: Notification): string => {
    const username = notification.actorUsername || 'Someone'
    switch (notification.type) {
      case 'LIKE':
        return `${username} liked your post`
      case 'COMMENT':
        return `${username} commented on your post`
      case 'FOLLOW':
        return `${username} started following you`
      default:
        return `${username} interacted with you`
    }
  }

  // Add new notification from WebSocket
  const addNotification = (notification: Notification, showToast: boolean = false) => {
    // Avoid duplicates
    const exists = notifications.value.some(n => n.id === notification.id)
    if (!exists) {
      notifications.value.unshift(notification)
      if (!notification.isRead) {
        unreadCount.value++
      }

      // Show toast notification only for real-time new notifications
      if (showToast) {
        ElNotification({
          title: '新通知',
          message: getNotificationMessage(notification),
          type: 'info',
          duration: 3000
        })
      }
    }
  }

  // Initialize WebSocket connection
  const initWebSocket = (userId: number) => {
    currentUserId.value = userId
    // Fetch initial unread count
    fetchUnreadCount(userId)

    useWebSocket(userId, (data: any) => {
      // Handle WebSocket notification message
      const notification: Notification = {
        id: data.id,
        type: data.type,
        actorId: data.actorId,
        actorUsername: data.actorUsername,
        actorAvatar: data.actorAvatar,
        targetId: data.targetId,
        targetType: data.targetType,
        isRead: false,
        createdAt: new Date().toISOString()
      }
      addNotification(notification, true)
    }, (connected: boolean) => {
      wsConnected.value = connected
      if (connected) {
        fetchUnreadCount(userId)
      }
    })
  }

  return {
    notifications,
    unreadCount,
    wsConnected,
    currentUserId,
    fetchUnreadCount,
    markAsRead,
    markAllAsRead,
    clearNotifications,
    pushNotification,
    addNotification,
    initWebSocket
  }
})

import request from '@/utils/axios'

export interface Notification {
  id: number
  type: string
  actorId: number
  targetId: number
  targetType: string
  isRead: boolean
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// 获取通知列表（分页）
export const getNotificationList = (recipientId: number, page: number = 1, size: number = 10) => {
  return request.get<PageResult<Notification>>('/notifications', {
    params: { recipientId, page, size }
  })
}

// 标记单条通知为已读
export const markAsRead = (id: number, recipientId: number) => {
  return request.put(`/notifications/${id}/read`, null, {
    params: { recipientId }
  })
}

// 标记所有通知为已读
export const markAllAsRead = (recipientId: number) => {
  return request.put('/notifications/read-all', null, {
    params: { recipientId }
  })
}

// 获取未读通知数量
export const getUnreadCount = (recipientId: number) => {
  return request.get<number>('/notifications/unread-count', {
    params: { recipientId }
  })
}

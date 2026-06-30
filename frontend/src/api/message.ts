import request from '@/utils/axios'

export const createConversation = (targetUserId: number) => {
  return request.post('/messages/conversations', { targetUserId })
}

export const getConversation = (conversationId: number) => {
  return request.get(`/messages/conversations/${conversationId}`)
}

export const getMessages = (conversationId: number, page: number = 1, size: number = 30) => {
  return request.get(`/messages/conversations/${conversationId}/messages`, {
    params: { page, size }
  })
}

export const sendTextMessage = (conversationId: number, content: string) => {
  return request.post(`/messages/conversations/${conversationId}/messages/text`, { content })
}

export const sendImageMessage = (conversationId: number, imageUrl: string, originalImageUrl?: string) => {
  return request.post(`/messages/conversations/${conversationId}/messages/image`, { imageUrl, originalImageUrl })
}

export const markConversationAsRead = (conversationId: number) => {
  return request.put(`/messages/conversations/${conversationId}/read`)
}

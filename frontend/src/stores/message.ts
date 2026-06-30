import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Conversation, Message } from '@/types'
import {
  createConversation,
  getConversation,
  getMessages,
  sendTextMessage,
  sendImageMessage,
  markConversationAsRead
} from '@/api/message'

export const useMessageStore = defineStore('message', () => {
  const currentConversation = ref<Conversation | null>(null)
  const messages = ref<Message[]>([])
  const loading = ref(false)

  const openConversationWithUser = async (targetUserId: number) => {
    const res = await createConversation(targetUserId)
    currentConversation.value = res.data || res
    return currentConversation.value
  }

  const loadConversation = async (conversationId: number) => {
    const res = await getConversation(conversationId)
    currentConversation.value = res.data || res
    return currentConversation.value
  }

  const loadMessages = async (conversationId: number) => {
    loading.value = true
    try {
      const res = await getMessages(conversationId)
      const records = res.data?.records || []
      messages.value = [...records].reverse()
      return messages.value
    } finally {
      loading.value = false
    }
  }

  const sendText = async (conversationId: number, content: string) => {
    const res = await sendTextMessage(conversationId, content)
    const message = res.data || res
    messages.value.push(message)
    return message
  }

  const sendImage = async (conversationId: number, imageUrl: string, originalImageUrl?: string) => {
    const res = await sendImageMessage(conversationId, imageUrl, originalImageUrl)
    const message = res.data || res
    messages.value.push(message)
    return message
  }

  const appendIncomingMessage = (message: Message) => {
    if (messages.value.some(m => m.id === message.id)) return
    messages.value.push(message)
  }

  const markRead = async (conversationId: number) => {
    await markConversationAsRead(conversationId)
    messages.value.forEach(message => {
      if (message.conversationId === conversationId) {
        message.isRead = true
      }
    })
    if (currentConversation.value?.id === conversationId) {
      currentConversation.value.unreadCount = 0
    }
  }

  return {
    currentConversation,
    messages,
    loading,
    openConversationWithUser,
    loadConversation,
    loadMessages,
    sendText,
    sendImage,
    appendIncomingMessage,
    markRead
  }
})

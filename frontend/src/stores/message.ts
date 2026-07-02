import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Conversation, Message } from '@/types'
import {
  createConversation,
  getConversation,
  getMessages,
  getMessagesAfter,
  sendTextMessage,
  sendImageMessage,
  markConversationAsRead
} from '@/api/message'

export const useMessageStore = defineStore('message', () => {
  const currentConversation = ref<Conversation | null>(null)
  const messages = ref<Message[]>([])
  const loading = ref(false)

  // 幂等插入：按 id 去重（同 id 覆盖），按 id 升序排列。
  // 推送、发送回显、补拉、全量加载全部走它，保证任何来源交错都不重复、不乱序。
  const upsertMessages = (incoming: Message[]) => {
    if (!incoming || incoming.length === 0) return
    const map = new Map<number, Message>(messages.value.map(m => [m.id, m]))
    for (const m of incoming) {
      if (m && m.id != null) {
        map.set(m.id, m)
      }
    }
    messages.value = Array.from(map.values()).sort((a, b) => a.id - b.id)
  }

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
      // 全量加载：重置为本次结果（初始化场景），随后所有增量走 upsert
      messages.value = [...records].reverse()
      return messages.value
    } finally {
      loading.value = false
    }
  }

  // 断线重连后补拉：只取当前最大 id 之后的新消息，幂等合并
  const syncMissedMessages = async (conversationId: number) => {
    const maxId = messages.value.reduce((max, m) => (m.id > max ? m.id : max), 0)
    const res = await getMessagesAfter(conversationId, maxId)
    const records = (res.data || res || []) as Message[]
    upsertMessages(records)
    return records
  }

  const sendText = async (conversationId: number, content: string) => {
    const res = await sendTextMessage(conversationId, content)
    const message = res.data || res
    upsertMessages([message])
    return message
  }

  const sendImage = async (conversationId: number, imageUrl: string, originalImageUrl?: string) => {
    const res = await sendImageMessage(conversationId, imageUrl, originalImageUrl)
    const message = res.data || res
    upsertMessages([message])
    return message
  }

  const appendIncomingMessage = (message: Message) => {
    upsertMessages([message])
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
    syncMissedMessages,
    upsertMessages,
    sendText,
    sendImage,
    appendIncomingMessage,
    markRead
  }
})

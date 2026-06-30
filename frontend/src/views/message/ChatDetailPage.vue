<template>
  <div class="chat-detail-page">
    <AppHeader />
    <AppLayout>
      <div class="chat-container" v-if="conversation">
        <div class="chat-header">
          <el-button text @click="router.back()">←</el-button>
          <UserAvatar :user="{ avatar: conversation.peerAvatar, username: conversation.peerUsername }" :size="40" />
          <span class="peer-name">{{ conversation.peerUsername }}</span>
        </div>

        <div class="message-list" ref="messageListRef" v-loading="messageStore.loading">
          <div
            v-for="message in messageStore.messages"
            :key="message.id"
            class="message-row"
            :class="{ mine: message.senderId === authStore.userInfo?.id }"
          >
            <div class="message-bubble text" v-if="message.messageType === 'TEXT'">
              {{ message.content }}
            </div>
            <img
              v-else
              class="message-image"
              :src="message.imageUrl"
              alt="图片消息"
              @click="previewImage(message.originalImageUrl || message.imageUrl || '')"
            />
          </div>
        </div>

        <div class="composer">
          <input ref="imageInputRef" type="file" accept="image/*" hidden @change="handleImageSelected" />
          <el-button @click="imageInputRef?.click()">图片</el-button>
          <el-checkbox v-model="sendOriginal">原图</el-checkbox>
          <el-input
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入消息"
            @keydown.enter.exact.prevent="sendText"
          />
          <el-button type="primary" :disabled="!inputText.trim()" @click="sendText">发送</el-button>
        </div>
      </div>
    </AppLayout>

    <teleport to="body">
      <el-image-viewer
        v-if="previewUrl"
        :url-list="[previewUrl]"
        @close="previewUrl = ''"
      />
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import UserAvatar from '@/components/user/UserAvatar.vue'
import { useAuthStore } from '@/stores/auth'
import { useMessageStore } from '@/stores/message'
import { useMessageSocketStore } from '@/stores/messageSocket'
import { uploadFile } from '@/api/upload'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const messageStore = useMessageStore()
const socketStore = useMessageSocketStore()

const conversationId = computed(() => Number(route.params.id))
const conversation = computed(() => messageStore.currentConversation)
const inputText = ref('')
const sendOriginal = ref(false)
const previewUrl = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const imageInputRef = ref<HTMLInputElement | null>(null)

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const loadAll = async () => {
  await messageStore.loadConversation(conversationId.value)
  await messageStore.loadMessages(conversationId.value)
  await messageStore.markRead(conversationId.value)
  socketStore.joinConversation(conversationId.value)
  await scrollToBottom()
}

const sendText = async () => {
  const content = inputText.value.trim()
  if (!content) return
  inputText.value = ''
  await messageStore.sendText(conversationId.value, content)
  await scrollToBottom()
}

const compressImage = (file: File): Promise<File> => {
  return new Promise((resolve, reject) => {
    const img = new Image()
    const objectUrl = URL.createObjectURL(file)
    img.onload = () => {
      const maxSide = 1280
      const scale = Math.min(1, maxSide / Math.max(img.width, img.height))
      const canvas = document.createElement('canvas')
      canvas.width = Math.round(img.width * scale)
      canvas.height = Math.round(img.height * scale)
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        URL.revokeObjectURL(objectUrl)
        reject(new Error('无法压缩图片'))
        return
      }
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
      canvas.toBlob(blob => {
        URL.revokeObjectURL(objectUrl)
        if (!blob) {
          reject(new Error('图片压缩失败'))
          return
        }
        resolve(new File([blob], file.name.replace(/\.[^.]+$/, '.jpg'), { type: 'image/jpeg' }))
      }, 'image/jpeg', 0.8)
    }
    img.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      reject(new Error('图片读取失败'))
    }
    img.src = objectUrl
  })
}

const handleImageSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  try {
    const compressed = await compressImage(file)
    const compressedRes = await uploadFile(compressed, 'image')
    const imageUrl = compressedRes.data?.url || compressedRes.url
    let originalImageUrl: string | undefined

    if (sendOriginal.value) {
      const originalRes = await uploadFile(file, 'image')
      originalImageUrl = originalRes.data?.url || originalRes.url
    }

    await messageStore.sendImage(conversationId.value, imageUrl, originalImageUrl)
    await scrollToBottom()
  } catch (error: any) {
    ElMessage.error(error.message || '图片发送失败')
  }
}

const previewImage = (url: string) => {
  previewUrl.value = url
}

onMounted(loadAll)

watch(() => route.params.id, async (_newId, oldId) => {
  if (oldId) {
    socketStore.leaveConversation(Number(oldId))
  }
  await loadAll()
})

onUnmounted(() => {
  socketStore.leaveConversation(conversationId.value)
  socketStore.scheduleIdleClose()
})
</script>

<style scoped lang="scss">
.chat-detail-page {
  min-height: 100vh;
  background: #fafafa;
}

.chat-container {
  max-width: 720px;
  height: calc(100vh - 80px);
  margin: 0 auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 16px 16px 0 0;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;

  .peer-name {
    font-weight: 600;
    color: #333;
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  background: #fff;
  padding: 16px;
}

.message-row {
  display: flex;
  margin-bottom: 12px;

  &.mine {
    justify-content: flex-end;
  }
}

.message-bubble {
  max-width: 70%;
  padding: 10px 12px;
  border-radius: 14px;
  background: #f2f3f5;
  color: #333;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-row.mine .message-bubble {
  background: #4CAF82;
  color: #fff;
}

.message-image {
  max-width: 240px;
  max-height: 320px;
  border-radius: 12px;
  object-fit: cover;
  cursor: pointer;
}

.composer {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border-radius: 0 0 16px 16px;
  padding: 12px;
  border-top: 1px solid #f0f0f0;
}
</style>

<template>
  <div class="comment-item">
    <UserAvatar :user="comment" :size="36" />
    <div class="comment-content">
      <div class="comment-header">
        <router-link :to="`/user/${comment.userId}`" class="username">
          {{ comment.username }}
        </router-link>
        <span class="time">{{ formatTime(comment.createTime || comment.createdAt) }}</span>
      </div>
      <p class="comment-text">{{ comment.content }}</p>
    </div>
    <el-button
      v-if="canDelete"
      type="text"
      class="delete-btn"
      @click="$emit('delete')"
    >
      删除
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/user/UserAvatar.vue'
import type { Comment } from '@/types'

const props = defineProps<{
  comment: Comment
}>()

defineEmits(['delete'])

const authStore = useAuthStore()
const canDelete = computed(() => {
  return authStore.userInfo?.id === props.comment.userId
})

const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return time
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped lang="scss">
.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;

  .username {
    font-size: 14px;
    font-weight: 600;
    color: #333;
    text-decoration: none;

    &:hover {
      color: #4CAF82;
    }
  }

  .time {
    font-size: 12px;
    color: #999;
  }
}

.comment-text {
  font-size: 14px;
  color: #555;
  line-height: 1.5;
}

.delete-btn {
  color: #999;
  font-size: 12px;

  &:hover {
    color: #FF7F7F;
  }
}
</style>

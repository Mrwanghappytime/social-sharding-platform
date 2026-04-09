<template>
  <div class="notification-item" :class="{ unread: !notification.isRead }" @click="$emit('click')">
    <UserAvatar :user="{ avatar: notification.actorAvatar, username: notification.actorUsername }" :size="44" />
    <div class="notification-content">
      <p class="text">
        <span class="username">{{ notification.actorUsername || '未知用户' }}</span>
        {{ getActionText() }}
      </p>
      <span class="time">{{ formatTime(notification.createdAt || '') }}</span>
    </div>
    <span v-if="!notification.isRead" class="unread-dot"></span>
  </div>
</template>

<script setup lang="ts">
import type { Notification } from '@/stores/notification'
import UserAvatar from '@/components/user/UserAvatar.vue'

const props = defineProps<{
  notification: Notification
}>()

defineEmits(['click'])

const getActionText = () => {
  switch (props.notification.type) {
    case 'LIKE':
      return '点赞了你的动态'
    case 'COMMENT':
      return '评论了你的动态'
    case 'FOLLOW':
      return '关注了你'
    default:
      return '有新通知'
  }
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
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
.notification-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f8faf8;
  }

  &.unread {
    background: #f0f9f5;
  }
}

.notification-content {
  flex: 1;

  .text {
    font-size: 14px;
    color: #333;
    margin-bottom: 4px;
    line-height: 1.4;

    .username {
      font-weight: 600;
    }
  }

  .time {
    font-size: 12px;
    color: #999;
  }
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: #4CAF82;
  border-radius: 50%;
}
</style>

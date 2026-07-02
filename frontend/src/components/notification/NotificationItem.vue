<template>
  <div class="notification-item" :class="{ unread: !notification.isRead }" @click="$emit('click')">
    <router-link
      :to="`/user/${notification.actorId}`"
      class="avatar-link"
      @click.stop="$emit('actor-click')"
    >
      <UserAvatar :user="{ avatar: notification.actorAvatar, username: notification.actorUsername }" :size="44" />
    </router-link>
    <div class="notification-content">
      <p class="text">
        <router-link
          :to="`/user/${notification.actorId}`"
          class="username"
          @click.stop="$emit('actor-click')"
        >
          {{ notification.actorUsername || '未知用户' }}
        </router-link>
        {{ getActionText() }}
      </p>
      <router-link
        v-if="showTargetPost"
        :to="`/post/${notification.targetId}`"
        class="target-post"
        @click.stop="$emit('target-click')"
      >
        {{ targetPostText }}
      </router-link>
      <div v-if="notification.type === 'MESSAGE'" class="conversation-preview">
        <span>{{ conversationPreview }}</span>
        <span v-if="unreadCount > 0" class="message-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </div>
      <span class="time">{{ formatTime(notification.updatedAt || notification.createdAt || '') }}</span>
    </div>
    <span v-if="!notification.isRead" class="unread-dot"></span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Notification } from '@/stores/notification'
import UserAvatar from '@/components/user/UserAvatar.vue'

const props = defineProps<{
  notification: Notification
}>()

defineEmits(['click', 'actor-click', 'target-click'])

const showTargetPost = computed(() => {
  return props.notification.targetType === 'POST' && !!props.notification.targetId
})

const targetPostText = computed(() => {
  const title = props.notification.targetTitle || '查看动态'
  return `「${title}」`
})

const conversationPreview = computed(() => {
  if (props.notification.type !== 'MESSAGE') return ''
  const preview = props.notification.conversation?.lastMessagePreview
  return preview || '查看私信'
})

const unreadCount = computed(() => props.notification.conversation?.unreadCount || 0)

const getActionText = () => {
  switch (props.notification.type) {
    case 'LIKE':
      return '点赞了你的动态'
    case 'COMMENT':
      return '评论了你的动态'
    case 'FOLLOW':
      return '关注了你'
    case 'MESSAGE':
      return '给你发了私信'
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

.avatar-link {
  display: flex;
  text-decoration: none;
}

.notification-content {
  flex: 1;
  min-width: 0;

  .text {
    font-size: 14px;
    color: #333;
    margin-bottom: 4px;
    line-height: 1.4;

    .username {
      font-weight: 600;
      color: #333;
      text-decoration: none;

      &:hover {
        color: #4CAF82;
      }
    }
  }

  .target-post {
    display: block;
    max-width: 100%;
    margin-bottom: 4px;
    color: #666;
    font-size: 13px;
    line-height: 1.4;
    text-decoration: none;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &:hover {
      color: #4CAF82;
    }
  }

  .conversation-preview {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 4px;
    color: #666;
    font-size: 13px;
    line-height: 1.4;
  }

  .message-badge {
    min-width: 18px;
    height: 18px;
    padding: 0 6px;
    border-radius: 9px;
    background: #ff7f7f;
    color: #fff;
    font-size: 11px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
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
  flex-shrink: 0;
}
</style>

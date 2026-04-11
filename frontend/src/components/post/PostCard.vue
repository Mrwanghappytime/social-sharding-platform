<template>
  <div class="post-card" @click="goToDetail">
    <div class="card-header">
      <UserAvatar :user="post" :size="44" />
      <div class="user-info">
        <router-link :to="`/user/${post.userId}`" class="username" @click.stop>
          {{ post.username }}
        </router-link>
        <span class="time">{{ formatTime(post.createTime || post.createdAt) }}</span>
      </div>
    </div>

    <div class="card-body">
      <h3 class="post-title">{{ post.title }}</h3>
      <p class="post-content">{{ post.content }}</p>

      <MediaGallery
        v-if="displayImages.length"
        :images="displayImages"
        @click.stop
      />

      <MediaGallery
        v-if="displayVideos.length"
        :videos="displayVideos"
        @click.stop
      />
    </div>

    <div class="card-footer" @click.stop>
      <div class="action-item" :class="{ active: post.isLiked }" @click="toggleLike">
        <span class="action-icon">{{ post.isLiked ? '❤️' : '🤍' }}</span>
        <span>{{ post.likeCount || 0 }}</span>
      </div>

      <div class="action-item" @click="goToDetail">
        <span class="action-icon">💬</span>
        <span>{{ post.commentCount || 0 }}</span>
      </div>

      <div class="action-item">
        <span class="action-icon">🔗</span>
        <span>分享</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/user/UserAvatar.vue'
import MediaGallery from '@/components/media/MediaGallery.vue'
import type { Post, MediaFile } from '@/types'

const props = defineProps<{
  post: Post
}>()

const emit = defineEmits(['like', 'unlike', 'comment'])
const router = useRouter()
const authStore = useAuthStore()

const displayImages = computed(() => {
  if (props.post.imageUrls?.length) return props.post.imageUrls
  if (props.post.mediaFiles?.length) {
    return props.post.mediaFiles
      .filter(f => f.type === 'IMAGE')
      .map(f => f.url)
  }
  return []
})

const displayVideos = computed(() => {
  if (props.post.videoUrl) return [props.post.videoUrl]
  if (props.post.mediaFiles?.length) {
    return props.post.mediaFiles
      .filter(f => f.type === 'VIDEO')
      .map(f => f.url)
  }
  return []
})

const goToDetail = () => {
  router.push(`/post/${props.post.id}`)
}

const formatTime = (time?: string) => {
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

const toggleLike = async () => {
  if (!authStore.isLoggedIn()) {
    router.push('/login')
    return
  }

  // Emit event, let parent/store handle API call
  if (props.post.isLiked) {
    emit('unlike', props.post)
  } else {
    emit('like', props.post)
  }
}
</script>

<style scoped lang="scss">
.post-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.2s;
  cursor: pointer;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.user-info {
  flex: 1;

  .username {
    font-size: 15px;
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
    margin-left: 8px;
  }
}

.card-body {
  margin-bottom: 16px;
}

.post-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  line-height: 1.4;
}

.post-content {
  font-size: 15px;
  color: #555;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;

  &:hover {
    color: #4CAF82;
  }

  &.active {
    color: #FF7F7F;
  }

  .action-icon {
    font-size: 18px;
  }
}
</style>

<template>
  <div class="post-detail-page">
    <AppHeader />
    <AppLayout>
      <div class="detail-container">
        <div class="post-detail-card" v-if="postStore.currentPost">
          <div class="card-header">
            <UserAvatar :user="postStore.currentPost" :size="48" />
            <div class="user-info">
              <router-link :to="`/user/${postStore.currentPost.userId}`" class="username">
                {{ postStore.currentPost.username }}
              </router-link>
              <span class="time">{{ formatTime(postStore.currentPost.createTime || postStore.currentPost.createdAt) }}</span>
            </div>
          </div>

          <div class="card-body">
            <h1 class="post-title">{{ postStore.currentPost.title }}</h1>
            <p class="post-content">{{ postStore.currentPost.content }}</p>

            <MediaGallery
              v-if="(postStore.currentPost.type === PostType.IMAGE || postStore.currentPost.type === 'IMAGE') && postStore.currentPost.imageUrls?.length"
              :images="postStore.currentPost.imageUrls"
            />

            <div v-if="(postStore.currentPost.type === PostType.VIDEO || postStore.currentPost.type === 'VIDEO') && postStore.currentPost.videoUrl" class="video-container">
              <video :src="postStore.currentPost.videoUrl" controls></video>
            </div>
          </div>

          <div class="card-footer">
            <div class="action-item" :class="{ active: postStore.currentPost.isLiked }" @click="toggleLike">
              <span class="action-icon">{{ postStore.currentPost.isLiked ? '❤️' : '🤍' }}</span>
              <span>{{ postStore.currentPost.likeCount || 0 }} 点赞</span>
            </div>
          </div>
        </div>

        <div class="comments-section">
          <h3>评论 ({{ comments.length }})</h3>

          <CommentInput v-if="isLoggedIn" :postId="postId" @submit="addComment" />

          <CommentList :comments="comments" @delete="deleteComment" />
        </div>
      </div>
    </AppLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { PostType } from '@/types'
import { useAuthStore } from '@/stores/auth'
import { usePostStore } from '@/stores/post'
import { getComments, likePost, unlikePost } from '@/api/interaction'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import UserAvatar from '@/components/user/UserAvatar.vue'
import MediaGallery from '@/components/media/MediaGallery.vue'
import CommentInput from '@/components/interaction/CommentInput.vue'
import CommentList from '@/components/interaction/CommentList.vue'
import type { Comment } from '@/types'

const route = useRoute()
const authStore = useAuthStore()
const postStore = usePostStore()

const postId = computed(() => Number(route.params.id))
const comments = ref<Comment[]>([])
const isLoggedIn = computed(() => authStore.isLoggedIn())

const formatTime = (time: string) => {
  return new Date(time).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const toggleLike = async () => {
  if (!isLoggedIn.value) return
  const currentPost = postStore.currentPost
  if (!currentPost) return

  try {
    if (currentPost.isLiked) {
      await unlikePost(postId.value)
      currentPost.isLiked = false
    } else {
      await likePost(postId.value)
      currentPost.isLiked = true
    }
  } catch (error) {
    console.error('Failed to toggle like:', error)
  }
}

const addComment = (content: string) => {
  // TODO: implement add comment
}

const deleteComment = (commentId: number) => {
  // TODO: implement delete comment
}

const loadComments = async () => {
  try {
    const res = await getComments(postId.value)
    comments.value = res.data || []
  } catch (error) {
    console.error('Failed to load comments:', error)
  }
}

onMounted(async () => {
  await postStore.fetchPostById(postId.value)
  await loadComments()
})
</script>

<style scoped lang="scss">
.post-detail-page {
  min-height: 100vh;
  background: #fafafa;
}

.detail-container {
  max-width: 680px;
  margin: 0 auto;
  padding: 24px;
}

.post-detail-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;

  .user-info {
    .username {
      font-size: 16px;
      font-weight: 600;
      color: #333;
      text-decoration: none;

      &:hover {
        color: #4CAF82;
      }
    }

    .time {
      font-size: 13px;
      color: #999;
      margin-left: 8px;
    }
  }
}

.post-title {
  font-size: 22px;
  font-weight: 700;
  color: #333;
  margin-bottom: 16px;
  line-height: 1.4;
}

.post-content {
  font-size: 16px;
  color: #444;
  line-height: 1.8;
  margin-bottom: 20px;
}

.video-container {
  margin-top: 16px;
  border-radius: 12px;
  overflow: hidden;

  video {
    width: 100%;
    display: block;
  }
}

.card-footer {
  display: flex;
  gap: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  margin-top: 20px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;

  &:hover {
    color: #4CAF82;
  }

  &.active {
    color: #FF7F7F;
  }
}

.comments-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

  h3 {
    font-size: 17px;
    font-weight: 600;
    color: #333;
    margin-bottom: 20px;
  }
}
</style>

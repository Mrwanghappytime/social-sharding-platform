<template>
  <div class="post-list" ref="listRef">
    <PostCard
      v-for="post in posts"
      :key="post.id"
      :post="post"
      @like="handleLike"
      @unlike="handleUnlike"
      @comment="handleComment"
    />

    <div v-if="loading" class="loading">
      <span class="loading-icon">⏳</span>
      <span>加载中...</span>
    </div>

    <div v-if="!hasMore && posts.length > 0" class="no-more">
      <span>没有更多了</span>
    </div>

    <div v-if="posts.length === 0 && !loading" class="empty">
      <span class="empty-icon">📭</span>
      <span>还没有动态，快去发布吧</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { usePostStore } from '@/stores/post'
import PostCard from './PostCard.vue'
import type { Post } from '@/types'

const props = defineProps<{
  userId?: number
}>()

const postStore = usePostStore()
const posts = ref<Post[]>([])
const loading = ref(false)
const hasMore = ref(true)
const listRef = ref<HTMLElement>()
const currentUserId = ref<number | undefined>(undefined)

const loadMore = async (reset = false) => {
  if (loading.value) return
  if (!reset && !hasMore.value) return

  loading.value = true
  try {
    if (reset) {
      posts.value = []
      hasMore.value = true
    }

    if (props.userId) {
      await postStore.fetchUserPosts(props.userId, reset)
    } else {
      await postStore.fetchFeed(reset)
    }
    posts.value = postStore.posts
    hasMore.value = postStore.hasMore
  } finally {
    loading.value = false
  }
}

const handleLike = async (post: Post) => {
  try {
    await postStore.toggleLike(post.id!)
  } catch (error) {
    console.error('Failed to like:', error)
  }
}

const handleUnlike = async (post: Post) => {
  try {
    await postStore.toggleLike(post.id!)
  } catch (error) {
    console.error('Failed to unlike:', error)
  }
}

const handleComment = (post: Post) => {
  // TODO: navigate to post detail
}

onMounted(() => {
  currentUserId.value = props.userId
  loadMore(true)
})

watch(() => props.userId, (newUserId) => {
  if (newUserId !== currentUserId.value) {
    currentUserId.value = newUserId
    loadMore(true)
  }
})
</script>

<style scoped lang="scss">
.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.loading, .no-more, .empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: #999;
  font-size: 14px;
}

.loading-icon {
  font-size: 18px;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 8px;
}
</style>

<template>
  <div class="search-page">
    <AppHeader />
    <AppLayout>
      <div class="search-container">
        <div class="search-header">
          <el-input
            v-model="keyword"
            placeholder="搜索动态..."
            size="large"
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <span>🔍</span>
            </template>
          </el-input>
        </div>

        <div class="results">
          <PostList v-if="posts.length" :posts="posts" />
          <div v-else-if="searched" class="empty">
            <span class="empty-icon">🔍</span>
            <span>没有找到相关动态</span>
          </div>
          <div v-else class="hint">
            输入关键词搜索动态
          </div>
        </div>
      </div>
    </AppLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { usePostStore } from '@/stores/post'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import PostList from '@/components/post/PostList.vue'
import type { Post } from '@/types'

const route = useRoute()
const postStore = usePostStore()

const keyword = ref('')
const posts = ref<Post[]>([])
const searched = ref(false)

const handleSearch = async () => {
  if (!keyword.value.trim()) return

  searched.value = true
  await postStore.search(keyword.value, true)
  posts.value = postStore.posts
}

onMounted(() => {
  const k = route.query.keyword as string
  if (k) {
    keyword.value = k
    handleSearch()
  }
})
</script>

<style scoped lang="scss">
.search-page {
  min-height: 100vh;
  background: #fafafa;
}

.search-container {
  max-width: 680px;
  margin: 0 auto;
  padding: 24px;
}

.search-header {
  margin-bottom: 24px;

  .search-input {
    :deep(.el-input__wrapper) {
      border-radius: 24px;
      padding: 8px 20px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
    }
  }
}

.empty, .hint {
  text-align: center;
  padding: 48px;
  color: #999;

  .empty-icon {
    font-size: 48px;
    display: block;
    margin-bottom: 16px;
  }
}
</style>

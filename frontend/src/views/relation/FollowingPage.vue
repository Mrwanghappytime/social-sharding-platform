<template>
  <div class="following-page">
    <AppHeader />
    <AppLayout>
      <div class="list-container">
        <div class="list-header">
          <h2>关注</h2>
        </div>

        <UserList :users="users" :loading="loading" />

        <div v-if="users.length === 0 && !loading" class="empty">
          <span class="empty-icon">👥</span>
          <span>还没有关注任何人</span>
        </div>
      </div>
    </AppLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getFollowing } from '@/api/relation'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import UserList from '@/components/relation/UserList.vue'
import type { User } from '@/types'

const route = useRoute()
const userId = Number(route.params.id)
const users = ref<User[]>([])
const loading = ref(false)

const loadFollowing = async () => {
  loading.value = true
  try {
    const res = await getFollowing(userId)
    users.value = res.data || []
  } catch (error) {
    console.error('Failed to load following:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadFollowing()
})
</script>

<style scoped lang="scss">
.following-page {
  min-height: 100vh;
  background: #fafafa;
}

.list-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 24px;
}

.list-header {
  margin-bottom: 20px;

  h2 {
    font-size: 20px;
    font-weight: 600;
    color: #333;
  }
}

.empty {
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

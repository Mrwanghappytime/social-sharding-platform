<template>
  <div class="user-profile-page">
    <AppHeader />
    <AppLayout>
      <div class="profile-container">
        <div class="profile-card" v-if="user">
          <div class="profile-header">
            <UserAvatar :user="user" :size="80" />
            <div class="user-info">
              <h1 class="username">{{ user.username }}</h1>
              <p class="bio" v-if="user.bio">{{ user.bio }}</p>
              <div class="stats">
                <router-link :to="`/user/${userId}/following`" class="stat-item">
                  <span class="stat-value">{{ user.followingCount || 0 }}</span>
                  <span class="stat-label">关注</span>
                </router-link>
                <router-link :to="`/user/${userId}/followers`" class="stat-item">
                  <span class="stat-value">{{ user.followersCount || 0 }}</span>
                  <span class="stat-label">粉丝</span>
                </router-link>
                <div class="stat-item">
                  <span class="stat-value">{{ user.postsCount || 0 }}</span>
                  <span class="stat-label">动态</span>
                </div>
              </div>
            </div>
            <div class="profile-actions">
              <FollowButton v-if="!isOwnProfile" :userId="userId" />
              <el-button v-else @click="$router.push('/settings')">编辑资料</el-button>
            </div>
          </div>
        </div>

        <div class="posts-section">
          <PostList :userId="userId" />
        </div>
      </div>
    </AppLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getUserById } from '@/api/user'
import { getRelationCounts } from '@/api/relation'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import UserAvatar from '@/components/user/UserAvatar.vue'
import FollowButton from '@/components/relation/FollowButton.vue'
import PostList from '@/components/post/PostList.vue'
import type { User } from '@/types'

const route = useRoute()
const authStore = useAuthStore()

const userId = computed(() => Number(route.params.id))
const user = ref<User | null>(null)
const isOwnProfile = computed(() => authStore.userInfo?.id === userId.value)

const loadUser = async () => {
  try {
    const res = await getUserById(userId.value)
    user.value = res.data || res
  } catch (error) {
    console.error('Failed to load user:', error)
  }
}

const loadRelationCounts = async () => {
  try {
    const res = await getRelationCounts(userId.value)
    if (user.value && res.data) {
      user.value.followingCount = res.data.followingCount
      user.value.followersCount = res.data.followerCount
    }
  } catch (error) {
    console.error('Failed to load relation counts:', error)
  }
}

onMounted(async () => {
  await loadUser()
  await loadRelationCounts()
})
</script>

<style scoped lang="scss">
.user-profile-page {
  min-height: 100vh;
  background: #fafafa;
}

.profile-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.profile-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}

.profile-header {
  display: flex;
  gap: 24px;
}

.user-info {
  flex: 1;

  .username {
    font-size: 22px;
    font-weight: 700;
    color: #333;
    margin-bottom: 8px;
  }

  .bio {
    font-size: 14px;
    color: #666;
    margin-bottom: 16px;
    line-height: 1.5;
  }
}

.stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  text-decoration: none;
  display: flex;
  flex-direction: column;
  align-items: center;

  .stat-value {
    font-size: 18px;
    font-weight: 600;
    color: #333;
  }

  .stat-label {
    font-size: 13px;
    color: #999;
  }

  &:hover .stat-value {
    color: #4CAF82;
  }
}

.profile-actions {
  display: flex;
  align-items: flex-start;
}

.posts-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
</style>

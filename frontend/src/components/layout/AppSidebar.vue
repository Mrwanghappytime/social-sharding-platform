<template>
  <aside class="sidebar">
    <nav class="nav-menu">
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
      >
        <span class="nav-icon">{{ item.icon }}</span>
        <span class="nav-text">{{ item.text }}</span>
      </router-link>
    </nav>

    <div class="sidebar-footer" v-if="userInfo">
      <UserAvatar :user="userInfo" :size="40" />
      <div class="user-info">
        <div class="username">{{ userInfo.username }}</div>
        <div class="user-stats">
          <span>关注 {{ userInfo.followingCount || 0 }}</span>
          <span>粉丝 {{ userInfo.followersCount || 0 }}</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/user/UserAvatar.vue'

const route = useRoute()
const authStore = useAuthStore()
const userInfo = computed(() => authStore.userInfo)

const menuItems = [
  { path: '/', icon: '🏠', text: '首页' },
  { path: '/explore', icon: '✨', text: '发现' },
  { path: '/notifications', icon: '🔔', text: '通知' },
]

const isActive = (path: string) => {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(path)
}
</script>

<style scoped lang="scss">
.sidebar {
  position: fixed;
  left: 0;
  top: 60px;
  bottom: 0;
  width: 200px;
  background: #fff;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 12px;
  text-decoration: none;
  color: #666;
  font-size: 15px;
  transition: all 0.2s;

  &:hover {
    background: #f0f9f5;
    color: #4CAF82;
  }

  &.active {
    background: linear-gradient(135deg, #4CAF82, #5DC495);
    color: #fff;
    font-weight: 500;

    .nav-icon {
      transform: scale(1.1);
    }
  }

  .nav-icon {
    font-size: 18px;
    margin-right: 12px;
    transition: transform 0.2s;
  }
}

.sidebar-footer {
  margin-top: auto;
  padding: 16px;
  background: #f8faf8;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  flex: 1;
  min-width: 0;

  .username {
    font-size: 14px;
    font-weight: 500;
    color: #333;
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .user-stats {
    font-size: 12px;
    color: #999;

    span {
      margin-right: 8px;
    }
  }
}
</style>

<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <div class="user-menu-trigger">
      <UserAvatar :user="userInfo" :size="36" />
      <span class="username">{{ userInfo?.username || '用户' }}</span>
      <span class="arrow">▼</span>
    </div>

    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="profile">
          <span class="menu-icon">👤</span> 个人主页
        </el-dropdown-item>
        <el-dropdown-item command="settings">
          <span class="menu-icon">⚙️</span> 设置
        </el-dropdown-item>
        <el-dropdown-item divided command="logout">
          <span class="menu-icon">🚪</span> 退出登录
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from './UserAvatar.vue'

const router = useRouter()
const authStore = useAuthStore()
const userInfo = computed(() => authStore.userInfo)

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      if (userInfo.value) {
        router.push(`/user/${userInfo.value.id}`)
      }
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      authStore.logout()
      router.push('/login')
      break
  }
}
</script>

<style scoped lang="scss">
.user-menu-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 20px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #f5f5f5;
  }

  .username {
    font-size: 14px;
    color: #333;
    font-weight: 500;
  }

  .arrow {
    font-size: 10px;
    color: #999;
  }
}

.menu-icon {
  margin-right: 8px;
}
</style>

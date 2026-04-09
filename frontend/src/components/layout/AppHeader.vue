<template>
  <header class="header">
    <div class="header-content">
      <div class="logo" @click="$router.push('/')">
        <span class="logo-icon">🍃</span>
        <span class="logo-text">小清新</span>
      </div>

      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索动态..."
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <span class="search-icon">🔍</span>
          </template>
        </el-input>
      </div>

      <div class="header-actions">
        <el-button type="primary" class="create-btn" @click="$router.push('/create')">
          <span class="btn-icon">+</span>
          发布动态
        </el-button>

        <NotificationBell />

        <UserMenu />
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import NotificationBell from '@/components/notification/NotificationBell.vue'
import UserMenu from '@/components/user/UserMenu.vue'

const router = useRouter()
const searchKeyword = ref('')

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/search', query: { keyword: searchKeyword.value } })
  }
}
</script>

<style scoped lang="scss">
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  z-index: 1000;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.logo {
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.8;
  }

  .logo-icon {
    font-size: 28px;
    margin-right: 8px;
  }

  .logo-text {
    font-size: 20px;
    font-weight: 600;
    color: #4CAF82;
  }
}

.search-box {
  flex: 1;
  max-width: 400px;
  margin: 0 24px;

  .search-input {
    :deep(.el-input__wrapper) {
      border-radius: 20px;
      background: #f5f5f5;
      box-shadow: none;
      border: 1px solid transparent;
      transition: all 0.2s;

      &:hover, &.is-focus {
        border-color: #4CAF82;
        background: #fff;
      }
    }
  }

  .search-icon {
    font-size: 14px;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.create-btn {
  border-radius: 20px;
  padding: 8px 20px;
  background: linear-gradient(135deg, #4CAF82, #5DC495);
  border: none;
  font-weight: 500;
  transition: all 0.2s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(76, 175, 130, 0.3);
  }

  .btn-icon {
    margin-right: 4px;
    font-weight: bold;
  }
}
</style>

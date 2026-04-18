<template>
  <div class="user-list">
    <div
      v-for="user in users"
      :key="user.userId"
      class="user-item"
      @click="$router.push(`/user/${user.userId}`)"
    >
      <UserAvatar :user="user" :size="50" />
      <div class="user-info">
        <div class="username">{{ user.username }}</div>
        <div class="bio" v-if="user.bio">{{ user.bio }}</div>
        <div class="stats">
          <span>关注 {{ user.followingCount || 0 }}</span>
          <span>粉丝 {{ user.followersCount || 0 }}</span>
        </div>
      </div>
      <div @click.stop>
        <FollowButton :userId="user.userId" />
      </div>
    </div>

    <div v-if="loading" class="loading">
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import UserAvatar from '@/components/user/UserAvatar.vue'
import FollowButton from './FollowButton.vue'
import type { User } from '@/types'

defineProps<{
  users: User[]
  loading?: boolean
}>()
</script>

<style scoped lang="scss">
.user-list {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.user-item {
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
}

.user-info {
  flex: 1;
  min-width: 0;

  .username {
    font-size: 15px;
    font-weight: 600;
    color: #333;
    margin-bottom: 4px;
  }

  .bio {
    font-size: 13px;
    color: #666;
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .stats {
    font-size: 12px;
    color: #999;

    span {
      margin-right: 12px;
    }
  }
}

.loading {
  text-align: center;
  padding: 16px;
  color: #999;
}
</style>

<template>
  <div class="notification-bell" @click="handleClick">
    <span class="bell-icon">🔔</span>
    <span v-if="unreadCount > 0" class="badge">{{ displayCount }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const notificationStore = useNotificationStore()

const unreadCount = computed(() => notificationStore.unreadCount)

const displayCount = computed(() => {
  return unreadCount.value > 99 ? '99+' : unreadCount.value
})

const handleClick = () => {
  router.push('/notifications')
}
</script>

<style scoped lang="scss">
.notification-bell {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #f0f9f5;
  }

  .bell-icon {
    font-size: 20px;
  }

  .badge {
    position: absolute;
    top: 2px;
    right: 2px;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    background: #FF7F7F;
    color: #fff;
    font-size: 11px;
    font-weight: 600;
    border-radius: 9px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>

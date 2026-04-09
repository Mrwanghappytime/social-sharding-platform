<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const authStore = useAuthStore()
const notificationStore = useNotificationStore()

// Initialize WebSocket when user is logged in
const initNotificationWs = () => {
  if (authStore.token && authStore.userInfo?.id) {
    notificationStore.initWebSocket(authStore.userInfo.id)
  }
}

onMounted(() => {
  // If token exists but userInfo is missing, fetch user info
  if (authStore.token && !authStore.userInfo) {
    authStore.fetchUserInfo()
  }
  initNotificationWs()
})

// Watch for userInfo changes to initialize WebSocket
watch(() => authStore.userInfo, (newUserInfo) => {
  if (newUserInfo?.id) {
    initNotificationWs()
  }
})
</script>

<style>
@import './styles/index.scss';

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #333;
  background: #fafafa;
}

a {
  text-decoration: none;
  color: inherit;
}
</style>

<template>
  <div class="user-avatar" :style="{ width: size + 'px', height: size + 'px' }">
    <img
      v-if="user?.avatar"
      :src="user.avatar"
      :alt="user.username"
      class="avatar-img"
      @error="handleImgError"
    />
    <span v-else class="avatar-placeholder">{{ placeholderText }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { User } from '@/types'

const props = withDefaults(defineProps<{
  user?: User | null
  size?: number
}>(), {
  size: 40
})

const placeholderText = computed(() => {
  return props.user?.username?.charAt(0).toUpperCase() || 'U'
})

const handleImgError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
}
</script>

<style scoped lang="scss">
.user-avatar {
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #4CAF82, #5DC495);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  color: #fff;
  font-weight: 600;
  font-size: 16px;
}
</style>

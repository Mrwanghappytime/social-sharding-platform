<template>
  <el-button
    :type="isFollowing ? 'default' : 'primary'"
    :class="{ following: isFollowing }"
    @click="toggleFollow"
    :loading="loading"
  >
    {{ isFollowing ? '已关注' : '关注' }}
  </el-button>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { checkIsFollowing, followUser, unfollowUser } from '@/api/relation'

const props = defineProps<{
  userId: number
}>()

const loading = ref(false)
const isFollowingState = ref(false)

const isFollowing = computed(() => isFollowingState.value)

const toggleFollow = async () => {
  loading.value = true
  try {
    if (isFollowing.value) {
      await unfollowUser(props.userId)
      isFollowingState.value = false
    } else {
      await followUser(props.userId)
      isFollowingState.value = true
    }
  } catch (error) {
    console.error('Failed to toggle follow:', error)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const res = await checkIsFollowing(props.userId)
    isFollowingState.value = res.data || false
  } catch (error) {
    console.error('Failed to check following status:', error)
  }
})
</script>

<style scoped lang="scss">
.el-button {
  border-radius: 20px;
  padding: 8px 20px;

  &.following {
    background: #f5f5f5;
    border-color: #ddd;
    color: #666;

    &:hover {
      background: #fff;
      border-color: #4CAF82;
      color: #4CAF82;
    }
  }
}
</style>

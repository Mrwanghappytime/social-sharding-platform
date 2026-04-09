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
import { useRelationStore } from '@/stores/relation'

const props = defineProps<{
  userId: number
}>()

const relationStore = useRelationStore()
const loading = ref(false)

const isFollowing = computed(() => relationStore.isFollowing(props.userId))

const toggleFollow = async () => {
  loading.value = true
  try {
    if (isFollowing.value) {
      await relationStore.unfollow(props.userId)
    } else {
      await relationStore.follow(props.userId)
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // Check if already following
  relationStore.fetchFollowing
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

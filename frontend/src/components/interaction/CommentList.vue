<template>
  <div class="comment-list">
    <CommentItem
      v-for="comment in comments"
      :key="comment.id"
      :comment="comment"
      @delete="$emit('delete', comment.id)"
    />

    <div v-if="comments.length === 0" class="empty">
      还没有评论，快来抢沙发吧
    </div>
  </div>
</template>

<script setup lang="ts">
import CommentItem from './CommentItem.vue'
import type { Comment } from '@/types'

defineProps<{
  comments: Comment[]
}>()

defineEmits(['delete'])
</script>

<style scoped lang="scss">
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty {
  text-align: center;
  padding: 24px;
  color: #999;
  font-size: 14px;
}
</style>

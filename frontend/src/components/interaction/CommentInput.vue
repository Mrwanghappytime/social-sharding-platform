<template>
  <div class="comment-input">
    <el-input
      v-model="content"
      type="textarea"
      :rows="2"
      placeholder="写下你的评论..."
      resize="none"
    />
    <el-button type="primary" size="small" @click="submit" :loading="loading">
      发布
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { commentPost } from '@/api/interaction'

const props = defineProps<{
  postId: number
}>()

const emit = defineEmits(['submit'])

const content = ref('')
const loading = ref(false)

const submit = async () => {
  if (!content.value.trim()) return

  loading.value = true
  try {
    await commentPost(props.postId, content.value)
    content.value = ''
    ElMessage.success('评论成功')
    emit('submit', content.value)
  } catch (error) {
    ElMessage.error('评论失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.comment-input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 20px;

  :deep(.el-textarea__inner) {
    border-radius: 12px;
    padding: 12px;
  }
}
</style>

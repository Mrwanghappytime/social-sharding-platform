<template>
  <div class="create-post-page">
    <AppHeader />
    <AppLayout>
      <div class="create-container">
        <div class="create-card">
          <h2>发布动态</h2>

          <el-form ref="formRef" :model="form" :rules="rules">
            <el-form-item prop="title">
              <el-input
                v-model="form.title"
                placeholder="标题（选填）"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>

            <el-form-item prop="content">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="6"
                placeholder="分享你的想法..."
                resize="none"
                maxlength="2000"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="类型">
              <el-radio-group v-model="form.type">
                <el-radio :value="PostType.TEXT">纯文字</el-radio>
                <el-radio :value="PostType.IMAGE">图片</el-radio>
                <el-radio :value="PostType.VIDEO">视频</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item v-if="form.type === PostType.IMAGE">
              <ImageUploader @change="handleImagesChange" />
            </el-form-item>

            <el-form-item v-if="form.type === PostType.VIDEO">
              <VideoUploader @change="handleVideoChange" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleSubmit">
                发布
              </el-button>
              <el-button @click="$router.back()">取消</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </AppLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { PostType } from '@/types'
import { usePostStore } from '@/stores/post'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import ImageUploader from '@/components/upload/ImageUploader.vue'
import VideoUploader from '@/components/upload/VideoUploader.vue'

const router = useRouter()
const postStore = usePostStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  title: '',
  content: '',
  type: PostType.TEXT,
  imageUrls: [] as string[],
  videoUrl: ''
})

const rules = {
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' }
  ]
}

const handleImagesChange = (urls: string[]) => {
  form.imageUrls = urls
}

const handleVideoChange = (url: string) => {
  form.videoUrl = url
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await postStore.create({
      title: form.title,
      content: form.content,
      type: form.type,
      imageUrls: form.type === PostType.IMAGE ? form.imageUrls : undefined,
      videoUrl: form.type === PostType.VIDEO ? form.videoUrl : undefined
    })
    ElMessage.success('发布成功')
    router.push('/')
  } catch (error) {
    ElMessage.error('发布失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.create-post-page {
  min-height: 100vh;
  background: #fafafa;
}

.create-container {
  max-width: 680px;
  margin: 0 auto;
  padding: 24px;
}

.create-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

  h2 {
    font-size: 20px;
    font-weight: 600;
    color: #333;
    margin-bottom: 24px;
  }

  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  :deep(.el-textarea__inner) {
    border-radius: 12px;
    padding: 16px;
  }

  :deep(.el-input__wrapper) {
    border-radius: 12px;
    padding: 4px 16px;
  }

  :deep(.el-radio__input.is-checked .el-radio__inner) {
    background: #4CAF82;
    border-color: #4CAF82;
  }

  :deep(.el-radio__input.is-checked + .el-radio__label) {
    color: #4CAF82;
  }
}
</style>

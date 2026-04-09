<template>
  <div class="video-uploader">
    <div class="upload-list" v-if="videoUrl">
      <div class="upload-item">
        <video :src="videoUrl"></video>
        <span class="delete-btn" @click="removeVideo">×</span>
      </div>
    </div>

    <div class="upload-trigger" v-else @click="triggerUpload">
      <span class="upload-icon">🎬</span>
      <span class="upload-text">添加视频</span>
      <span class="upload-hint">最大50MB</span>
    </div>

    <input
      ref="fileInput"
      type="file"
      accept="video/*"
      hidden
      @change="handleFileChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUploadStore } from '@/stores/upload'

const emit = defineEmits(['change'])

const uploadStore = useUploadStore()
const fileInput = ref<HTMLInputElement>()
const videoUrl = ref('')

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('视频大小不能超过50MB')
    return
  }

  const url = await uploadStore.uploadVideo(file)
  if (url) {
    videoUrl.value = url
    emit('change', url)
  }

  // Reset input
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const removeVideo = () => {
  videoUrl.value = ''
  emit('change', '')
}
</script>

<style scoped lang="scss">
.video-uploader {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.upload-list {
  .upload-item {
    position: relative;
    width: 200px;
    height: 150px;
    border-radius: 8px;
    overflow: hidden;

    video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .delete-btn {
      position: absolute;
      top: 8px;
      right: 8px;
      width: 24px;
      height: 24px;
      background: rgba(0, 0, 0, 0.5);
      color: #fff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      font-size: 16px;

      &:hover {
        background: rgba(255, 0, 0, 0.7);
      }
    }
  }
}

.upload-trigger {
  width: 200px;
  height: 150px;
  border: 2px dashed #ddd;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #4CAF82;
    background: #f0f9f5;
  }

  .upload-icon {
    font-size: 32px;
    margin-bottom: 4px;
  }

  .upload-text {
    font-size: 14px;
    color: #666;
  }

  .upload-hint {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }
}
</style>

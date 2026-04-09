<template>
  <div class="image-uploader">
    <div class="upload-list" v-if="imageUrls.length">
      <div v-for="(url, index) in imageUrls" :key="index" class="upload-item">
        <img :src="url" alt="" />
        <span class="delete-btn" @click="removeImage(index)">×</span>
      </div>
    </div>

    <div class="upload-trigger" v-if="imageUrls.length < 9" @click="triggerUpload">
      <span class="upload-icon">📷</span>
      <span class="upload-text">添加图片</span>
    </div>

    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      multiple
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
const imageUrls = ref<string[]>([])

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = async (e: Event) => {
  const files = (e.target as HTMLInputElement).files
  if (!files) return

  for (const file of Array.from(files)) {
    if (imageUrls.value.length >= 9) {
      ElMessage.warning('最多只能上传9张图片')
      break
    }

    if (file.size > 10 * 1024 * 1024) {
      ElMessage.error('图片大小不能超过10MB')
      continue
    }

    const url = await uploadStore.uploadImage(file)
    if (url) {
      imageUrls.value.push(url)
    }
  }

  emit('change', imageUrls.value)

  // Reset input
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const removeImage = (index: number) => {
  imageUrls.value.splice(index, 1)
  emit('change', imageUrls.value)
}
</script>

<style scoped lang="scss">
.image-uploader {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.upload-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.upload-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .delete-btn {
    position: absolute;
    top: 4px;
    right: 4px;
    width: 20px;
    height: 20px;
    background: rgba(0, 0, 0, 0.5);
    color: #fff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 14px;

    &:hover {
      background: rgba(255, 0, 0, 0.7);
    }
  }
}

.upload-trigger {
  width: 100px;
  height: 100px;
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
    font-size: 28px;
    margin-bottom: 4px;
  }

  .upload-text {
    font-size: 12px;
    color: #999;
  }
}
</style>

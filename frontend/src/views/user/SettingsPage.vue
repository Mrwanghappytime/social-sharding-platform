<template>
  <div class="settings-page">
    <AppHeader />
    <AppLayout>
      <div class="settings-container">
        <h1 class="page-title">编辑资料</h1>

        <div class="settings-card">
          <div class="avatar-section">
            <UserAvatar :user="userInfo" :size="80" />
            <el-button size="small" @click="showAvatarDialog = true">更换头像</el-button>
          </div>

          <el-form :model="form" label-width="80px" class="settings-form">
            <el-form-item label="用户名">
              <el-input v-model="form.username" />
            </el-form-item>

            <el-form-item label="个人简介">
              <el-input v-model="form.bio" type="textarea" :rows="3" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </AppLayout>

    <el-dialog v-model="showAvatarDialog" title="更换头像" width="400px">
      <div class="avatar-upload-container">
        <el-upload
          class="avatar-uploader"
          :show-file-list="false"
          :before-upload="beforeAvatarUpload"
          :http-request="handleFileSelect"
        >
          <img v-if="previewUrl" :src="previewUrl" class="avatar-preview" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>

        <div v-if="previewUrl" class="upload-actions">
          <el-button @click="cancelUpload">取消</el-button>
          <el-button type="primary" :loading="uploading" @click="confirmUpload">确认更新</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { updateAvatar } from '@/api/user'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import UserAvatar from '@/components/user/UserAvatar.vue'

const authStore = useAuthStore()
const userInfo = authStore.userInfo

const form = reactive({
  username: userInfo?.username || '',
  bio: (userInfo as any)?.bio || ''
})

const saving = ref(false)
const showAvatarDialog = ref(false)
const previewUrl = ref('')
const uploading = ref(false)
const pendingFile = ref<File | null>(null)

// Compress image if larger than maxSize (in MB)
const compressImage = (file: File, maxSizeMB: number = 2): Promise<Blob> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        let width = img.width
        let height = img.height

        // Calculate scaling factor if image is larger than max dimensions
        const maxDimension = 800
        if (width > maxDimension || height > maxDimension) {
          if (width > height) {
            height = (height / width) * maxDimension
            width = maxDimension
          } else {
            width = (width / height) * maxDimension
            height = maxDimension
          }
        }

        canvas.width = width
        canvas.height = height

        const ctx = canvas.getContext('2d')
        ctx?.drawImage(img, 0, 0, width, height)

        // Try original quality first
        let quality = 0.9
        let blob = canvas.toBlob

        const tryCompress = () => {
          canvas.toBlob(
            (b) => {
              if (b) {
                if (b.size / (1024 * 1024) > maxSizeMB && quality > 0.1) {
                  quality -= 0.1
                  tryCompress()
                } else {
                  resolve(b)
                }
              } else {
                reject(new Error('图片压缩失败'))
              }
            },
            'image/jpeg',
            quality
          )
        }

        tryCompress()
      }
      img.onerror = () => reject(new Error('图片加载失败'))
      img.src = e.target?.result as string
    }
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsDataURL(file)
  })
}

const beforeAvatarUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  return true
}

const handleFileSelect = async (options: any) => {
  const { file } = options
  pendingFile.value = file

  // Create preview URL
  previewUrl.value = URL.createObjectURL(file)
}

const cancelUpload = () => {
  previewUrl.value = ''
  pendingFile.value = null
}

const confirmUpload = async () => {
  if (!pendingFile.value) return

  uploading.value = true
  try {
    let fileToUpload: File | Blob = pendingFile.value
    const originalName = pendingFile.value.name

    // Compress if larger than 2MB
    if (pendingFile.value.size > 2 * 1024 * 1024) {
      ElMessage.info('正在压缩图片...')
      const compressedBlob = await compressImage(pendingFile.value, 2)
      // Create File from Blob with original filename
      fileToUpload = new File([compressedBlob], originalName, { type: compressedBlob.type })
    }

    // Upload file
    const formData = new FormData()
    formData.append('file', fileToUpload)

    const res = await fetch('/api/files/upload', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${authStore.token}` },
      body: formData
    })

    const data = await res.json()
    if (data.code === 200) {
      // Update avatar with returned URL
      await updateAvatar(data.data.url)
      authStore.fetchUserInfo()
      ElMessage.success('头像更新成功')
      showAvatarDialog.value = false
      previewUrl.value = ''
      pendingFile.value = null
    } else {
      ElMessage.error(data.message || '上传失败')
    }
  } catch (error) {
    console.error('Upload error:', error)
    ElMessage.error('头像上传失败')
  } finally {
    uploading.value = false
  }
}

const saveProfile = async () => {
  saving.value = true
  try {
    ElMessage.success('资料更新成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.settings-page {
  min-height: 100vh;
  background: #fafafa;
}

.settings-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 20px;
}

.settings-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.settings-form {
  margin-top: 20px;
}

.avatar-upload-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.avatar-uploader {
  display: flex;
  justify-content: center;
  cursor: pointer;
}

.avatar-preview {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  object-fit: cover;
}

.upload-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>

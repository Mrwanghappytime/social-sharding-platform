import { defineStore } from 'pinia'
import { ref } from 'vue'
import { uploadFile } from '@/api/upload'

export interface UploadProgress {
  fileName: string
  progress: number
  url?: string
  error?: string
}

export const useUploadStore = defineStore('upload', () => {
  const uploads = ref<Map<string, UploadProgress>>(new Map())
  const isUploading = ref(false)

  const uploadImage = async (file: File): Promise<string | null> => {
    const fileName = file.name
    uploads.value.set(fileName, { fileName, progress: 0 })

    try {
      const res = await uploadFile(file, 'image', (progress: number) => {
        const upload = uploads.value.get(fileName)
        if (upload) {
          upload.progress = progress
        }
      })

      const url = res.data?.url || res.url
      if (url) {
        const upload = uploads.value.get(fileName)
        if (upload) {
          upload.progress = 100
          upload.url = url
        }
        return url
      }
      throw new Error('Upload failed')
    } catch (error: any) {
      const upload = uploads.value.get(fileName)
      if (upload) {
        upload.error = error.message || 'Upload failed'
      }
      return null
    }
  }

  const uploadVideo = async (file: File): Promise<string | null> => {
    const fileName = file.name
    uploads.value.set(fileName, { fileName, progress: 0 })

    try {
      const res = await uploadFile(file, 'video', (progress: number) => {
        const upload = uploads.value.get(fileName)
        if (upload) {
          upload.progress = progress
        }
      })

      const url = res.data?.url || res.url
      if (url) {
        const upload = uploads.value.get(fileName)
        if (upload) {
          upload.progress = 100
          upload.url = url
        }
        return url
      }
      throw new Error('Upload failed')
    } catch (error: any) {
      const upload = uploads.value.get(fileName)
      if (upload) {
        upload.error = error.message || 'Upload failed'
      }
      return null
    }
  }

  const removeUpload = (fileName: string) => {
    uploads.value.delete(fileName)
  }

  const clearUploads = () => {
    uploads.value.clear()
  }

  const getUpload = (fileName: string) => {
    return uploads.value.get(fileName)
  }

  return {
    uploads,
    isUploading,
    uploadImage,
    uploadVideo,
    removeUpload,
    clearUploads,
    getUpload
  }
})

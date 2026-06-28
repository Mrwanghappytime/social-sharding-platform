import { defineStore } from 'pinia'
import { ref } from 'vue'
import { uploadFile } from '@/api/upload'

export interface UploadProgress {
  fileName: string
  progress: number
  url?: string
  error?: string
}

export interface VideoUploadResult {
  url: string
  width: number
  height: number
}

const probeVideoMetadata = (file: File): Promise<{ width: number; height: number }> => {
  return new Promise((resolve) => {
    const video = document.createElement('video')
    video.preload = 'metadata'
    const objectUrl = URL.createObjectURL(file)

    const cleanup = () => {
      URL.revokeObjectURL(objectUrl)
      video.removeAttribute('src')
      video.load()
    }

    video.onloadedmetadata = () => {
      const width = video.videoWidth || 0
      const height = video.videoHeight || 0
      cleanup()
      resolve({ width, height })
    }
    video.onerror = () => {
      cleanup()
      resolve({ width: 0, height: 0 })
    }

    video.src = objectUrl
  })
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

  const uploadVideo = async (file: File): Promise<VideoUploadResult | null> => {
    const fileName = file.name
    uploads.value.set(fileName, { fileName, progress: 0 })

    try {
      const { width, height } = await probeVideoMetadata(file)

      const res = await uploadFile(
        file,
        'video',
        (progress: number) => {
          const upload = uploads.value.get(fileName)
          if (upload) {
            upload.progress = progress
          }
        },
        width && height ? { width, height } : undefined
      )

      const url = res.data?.url || res.url
      if (url) {
        const upload = uploads.value.get(fileName)
        if (upload) {
          upload.progress = 100
          upload.url = url
        }
        return { url, width, height }
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

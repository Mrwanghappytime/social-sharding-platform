import request from '@/utils/axios'

export const uploadFile = (
  file: File,
  type: 'image' | 'video',
  onProgress?: (percent: number) => void
) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)

  return request.post('/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (progressEvent: ProgressEvent) => {
      if (progressEvent.total) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress?.(percent)
      }
    }
  })
}

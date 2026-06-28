import request from '@/utils/axios'

export const uploadFile = (
  file: File,
  type: 'image' | 'video',
  onProgress?: (percent: number) => void,
  extra?: { width?: number; height?: number }
) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)
  if (extra?.width != null) {
    formData.append('width', String(extra.width))
  }
  if (extra?.height != null) {
    formData.append('height', String(extra.height))
  }

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

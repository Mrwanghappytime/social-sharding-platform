# 文件上传模块 (media-upload)

## 概述

图片和视频上传功能，支持拖拽、多文件选择和预览。

## 功能列表

### 3.1 图片上传
- **描述**：上传图片到服务器
- **API 调用**：`POST /api/files/upload`
- **表单字段**：`file` (multipart/form-data)
- **Query 参数**：`type=IMAGE&postId=可选`
- **响应**：`{ id, url, type, sortOrder }`
- **限制**：
  - 格式：jpg, jpeg, png, gif, webp
  - 大小：单文件最大 10MB
  - 数量：最多 9 张

### 3.2 视频上传
- **描述**：上传视频到服务器
- **API 调用**：`POST /api/files/upload`
- **Query 参数**：`type=VIDEO&postId=可选`
- **响应**：`{ id, url, type, sortOrder }`
- **限制**：
  - 格式：mp4, mov, avi, webm
  - 大小：单文件最大 50MB

### 3.3 文件预览
- **描述**：上传前预览选择的文件
- **图片**：缩略图预览
- **视频**：视频播放器预览

### 3.4 拖拽上传
- **描述**：支持拖拽文件到上传区域
- **视觉反馈**：拖拽时高亮显示上传区域

## 组件列表

- `ImageUploader.vue` - 图片上传组件
- `VideoUploader.vue` - 视频上传组件
- `MediaPreview.vue` - 媒体预览组件
- `UploadProgress.vue` - 上传进度组件

## 状态管理

- `useUploadStore` - 上传队列状态

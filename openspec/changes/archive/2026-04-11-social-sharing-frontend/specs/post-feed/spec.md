# 动态模块 (post-feed)

## 概述

动态发布和浏览功能，支持图片/视频类型动态。

## 功能列表

### 2.1 首页动态流
- **描述**：展示所有用户动态
- **API 调用**：`GET /api/posts/feed?page=1&size=10`
- **响应**：`{ records: [{ id, userId, title, content, type, images[], videoUrl, createdAt }], total, page, size }`
- **展示**：无限滚动加载
- **排序**：按创建时间倒序

### 2.2 发布动态
- **描述**：创建新动态
- **API 调用**：`POST /api/posts`
- **Header**：携带 JWT Token
- **请求体**：
  ```json
  {
    "title": "标题",
    "content": "内容",
    "type": "IMAGE | VIDEO",
    "imageUrls": ["url1", "url2"],
    "videoUrl": "url"
  }
  ```
- **校验**：
  - 图片动态：1-9张图片
  - 视频动态：1个视频
  - 图片和视频不能混搭

### 2.3 动态详情
- **描述**：查看单条动态
- **API 调用**：`GET /api/posts/{id}`
- **展示**：完整标题、内容、媒体、发布时间、作者信息

### 2.4 删除动态
- **描述**：删除自己发布的动态
- **API 调用**：`DELETE /api/posts/{id}`
- **条件**：仅动态作者可删除
- **删除后**：返回列表页

### 2.5 我的动态
- **描述**：查看当前用户的动态列表
- **API 调用**：`GET /api/posts/user/{userId}?page=1&size=10`

## 组件列表

- `PostCard.vue` - 动态卡片组件
- `PostList.vue` - 动态列表组件（无限滚动）
- `PostDetail.vue` - 动态详情页
- `CreatePost.vue` - 发布动态组件
- `MediaGallery.vue` - 媒体展示组件（图片/视频）

## 状态管理

- `usePostStore` - 动态列表状态

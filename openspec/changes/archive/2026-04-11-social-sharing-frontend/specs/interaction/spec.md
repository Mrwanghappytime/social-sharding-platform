# 互动模块 (interaction)

## 概述

评论和点赞功能，与后端 interaction-service 对接。

## 功能列表

### 4.1 点赞动态
- **描述**：对动态进行点赞
- **API 调用**：`POST /api/interactions/like`
- **Header**：携带 JWT Token
- **请求体**：`{ postId }`
- **响应**：`{ success: true }`
- **限制**：每用户每动态仅能点赞一次

### 4.2 取消点赞
- **描述**：取消已点赞的动态
- **API 调用**：`DELETE /api/interactions/like?postId={id}`
- **Header**：携带 JWT Token

### 4.3 获取点赞状态
- **描述**：获取当前用户对动态的点赞状态
- **API 调用**：`GET /api/interactions/like/status?postId={id}`
- **响应**：`{ liked: boolean, likeCount: number }`

### 4.4 评论动态
- **描述**：对动态发表评论
- **API 调用**：`POST /api/interactions/comment`
- **Header**：携带 JWT Token
- **请求体**：`{ postId, content }`
- **响应**：`{ id, postId, userId, content, createdAt, username, userAvatar }`

### 4.5 删除评论
- **描述**：删除已发表的评论
- **API 调用**：`DELETE /api/interactions/comment/{id}`
- **条件**：评论作者或动态作者可删除

### 4.6 获取评论列表
- **描述**：获取动态的所有评论
- **API 调用**：`GET /api/interactions/comments?postId={id}&page=1&size=20`
- **响应**：`{ records: [{ id, content, createdAt, username, userAvatar }], total }`

## 组件列表

- `LikeButton.vue` - 点赞按钮组件
- `CommentList.vue` - 评论列表组件
- `CommentItem.vue` - 单条评论组件
- `CommentInput.vue` - 评论输入组件

## 状态管理

- `useLikeStore` - 点赞状态管理
- `useCommentStore` - 评论列表管理

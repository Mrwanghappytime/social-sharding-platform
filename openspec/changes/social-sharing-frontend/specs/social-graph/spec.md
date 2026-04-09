# 社交关系模块 (social-graph)

## 概述

关注/粉丝关系管理，与后端 relation-service 对接。

## 功能列表

### 5.1 关注用户
- **描述**：关注其他用户
- **API 调用**：`POST /api/relations/follow`
- **Header**：携带 JWT Token
- **请求体**：`{ followingId }`
- **限制**：不能关注自己

### 5.2 取消关注
- **描述**：取消已关注的用户
- **API 调用**：`DELETE /api/relations/follow?followingId={id}`
- **Header**：携带 JWT Token

### 5.3 获取关注列表
- **描述**：获取指定用户的关注列表
- **API 调用**：`GET /api/relations/following?userId={id}`
- **响应**：`{ records: [{ userId, username, avatar }], total }`

### 5.4 获取粉丝列表
- **描述**：获取指定用户的粉丝列表
- **API 调用**：`GET /api/relations/followers?userId={id}`
- **响应**：`{ records: [{ userId, username, avatar }], total }`

### 5.5 获取关注数和粉丝数
- **描述**：获取用户的关注数和粉丝数
- **API 调用**：`GET /api/relations/counts?userId={id}`
- **响应**：`{ followingCount, followerCount }`

### 5.6 判断关注状态
- **描述**：判断当前用户是否关注了指定用户
- **API 调用**：`GET /api/relations/is-following?followingId={id}`
- **响应**：`{ isFollowing: boolean }`

## 组件列表

- `FollowButton.vue` - 关注/取消关注按钮
- `UserList.vue` - 用户列表组件
- `FollowStats.vue` - 关注/粉丝数展示组件

## 页面

- `/user/:id/following` - 关注列表页
- `/user/:id/followers` - 粉丝列表页

## 状态管理

- `useRelationStore` - 社交关系状态管理

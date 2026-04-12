# 前端开发任务清单

## 1. 项目初始化

- [x] 1.1 初始化 Vue 3 + Vite + TypeScript 项目
- [x] 1.2 安装依赖（Vue Router, Pinia, Axios, Element Plus）
- [x] 1.3 配置项目结构（src/api, src/components, src/views, src/stores）
- [x] 1.4 配置 Vite 代理（开发环境 API 代理到 localhost:8080）
- [x] 1.5 配置路径别名（@/）

## 2. 基础组件和工具

- [x] 2.1 创建 Axios 请求封装（拦截器、Token 处理）
- [x] 2.2 创建 Pinia stores（auth, post, notification, relation, upload）
- [x] 2.3 创建 Layout 组件（Header, Sidebar, MainContent）
- [x] 2.4 配置 Vue Router（路由守卫、登录验证）
- [x] 2.5 封装 WebSocket 连接工具

## 3. 用户认证模块 (user-auth)

- [x] 3.1 实现 RegisterForm.vue 注册表单组件
- [x] 3.2 实现 LoginForm.vue 登录表单组件
- [x] 3.3 实现 useAuthStore 认证状态管理
- [x] 3.4 实现 UserAvatar.vue 用户头像组件
- [x] 3.5 实现 UserMenu.vue 用户菜单组件
- [x] 3.6 实现 /register 注册页面
- [x] 3.7 实现 /login 登录页面

## 4. 动态模块 (post-feed)

- [x] 4.1 实现 PostCard.vue 动态卡片组件
- [x] 4.2 实现 PostList.vue 动态列表组件（无限滚动）
- [x] 4.3 实现 usePostStore 动态状态管理
- [x] 4.4 实现 CreatePost.vue 发布动态组件
- [x] 4.5 实现 MediaGallery.vue 媒体展示组件
- [x] 4.6 实现 / 首页（动态流）
- [x] 4.7 实现 /post/:id 动态详情页
- [x] 4.8 实现 /user/:id 用户个人页
- [x] 4.9 实现 /create 创建动态页面

## 5. 文件上传模块 (media-upload)

- [x] 5.1 实现 ImageUploader.vue 图片上传组件
- [x] 5.2 实现 VideoUploader.vue 视频上传组件
- [ ] 5.3 实现 MediaPreview.vue 媒体预览组件
- [ ] 5.4 实现 UploadProgress.vue 上传进度组件
- [x] 5.5 实现 useUploadStore 上传状态管理

## 6. 互动模块 (interaction)

- [x] 6.1 实现 LikeButton.vue 点赞按钮组件
- [x] 6.2 实现 CommentList.vue 评论列表组件
- [x] 6.3 实现 CommentItem.vue 单条评论组件
- [x] 6.4 实现 CommentInput.vue 评论输入组件
- [x] 6.5 实现 useLikeStore 点赞状态管理
- [x] 6.6 实现 useCommentStore 评论列表管理

## 7. 社交关系模块 (social-graph)

- [x] 7.1 实现 FollowButton.vue 关注/取消关注按钮
- [x] 7.2 实现 UserList.vue 用户列表组件
- [ ] 7.3 实现 FollowStats.vue 关注/粉丝数展示
- [x] 7.4 实现 useRelationStore 社交关系状态管理
- [x] 7.5 实现 /user/:id/following 关注列表页
- [x] 7.6 实现 /user/:id/followers 粉丝列表页

## 8. 通知模块 (notification)

- [x] 8.1 实现 NotificationBell.vue 通知铃铛组件
- [x] 8.2 实现 NotificationList.vue 通知列表组件
- [x] 8.3 实现 NotificationItem.vue 单条通知组件
- [ ] 8.4 实现 NotificationToast.vue 通知弹窗组件
- [x] 8.5 实现 useWebSocket.ts WebSocket 组合式函数
- [x] 8.6 实现 useNotificationStore 通知状态管理

## 9. 搜索模块 (search)

- [x] 9.1 实现 SearchBar.vue 搜索框组件
- [ ] 9.2 实现 SearchResults.vue 搜索结果列表组件
- [ ] 9.3 实现 SearchHistory.vue 搜索历史组件
- [x] 9.4 实现 useSearchStore 搜索状态管理
- [x] 9.5 实现 /search 搜索结果页面

## 10. 样式和适配

- [x] 10.1 全局样式变量定义（清新色板、字体、间距）
- [x] 10.2 全局样式初始化（CSS Reset）
- [ ] 10.3 响应式布局适配（移动端）
- [x] 10.4 清新风格主题实现（圆角、阴影、动效）
- [x] 10.5 加载状态和错误处理 UI

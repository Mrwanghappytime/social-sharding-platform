## 上下文

**项目背景**：社交分享平台前后端分离架构
- **后端**：Spring Boot + Dubbo + Nacos 微服务（已完成）
- **前端**：Vue 3 + Vite SPA（待开发）
- **API 网关**：Spring Cloud Gateway (端口 8080)
- **WebSocket**：通知实时推送

**技术栈**：
- Vue 3 + Composition API + TypeScript
- Vite 构建工具
- Vue Router 路由管理
- Pinia 状态管理
- Axios HTTP 客户端
- Element Plus UI 组件库

**后端 API 路由**：
- `POST /api/users/register` - 用户注册
- `POST /api/users/login` - 用户登录
- `GET /api/users/info` - 获取用户信息
- `GET /api/posts/feed` - 首页动态流
- `POST /api/posts` - 发布动态
- `POST /api/interactions/like` - 点赞
- `POST /api/interactions/comment` - 评论
- `POST /api/relations/follow` - 关注
- `WebSocket /ws/notifications` - 实时通知

## 目标 / 非目标

**目标：**
- 实现完整的用户认证流程（注册、登录、Token 管理）
- 实现动态发布和浏览功能（图片/视频）
- 实现评论和点赞互动
- 实现关注/粉丝社交关系
- 实现实时通知推送（WebSocket）
- 实现动态搜索功能
- 响应式布局，适配移动端

**非目标：**
- 不实现复杂的后台管理功能
- 不实现私信功能
- 不实现视频播放高级功能

## 决策

### 1. Vue 3 + Composition API
**决策**：采用 Vue 3 + Composition API + TypeScript
**理由**：
- 更好的 TypeScript 支持
- Composition API 更灵活的逻辑复用
- 更好的性能（Tree-shaking）
**替代方案**：Vue 2 + Options API → 缺乏 TypeScript 良好支持

### 2. Vite 构建工具
**决策**：使用 Vite 而非 Webpack
**理由**：
- 更快的冷启动速度
- 即时热更新（HMR）
- 更简单的配置
**替代方案**：Webpack → 配置复杂，冷启动慢

### 3. 状态管理 - Pinia
**决策**：使用 Pinia 替代 Vuex
**理由**：
- 更简洁的 API
- 更好的 TypeScript 支持
- 模块化设计更直观
**替代方案**：Vuex → API 较复杂

### 4. UI 组件库 - Element Plus
**决策**：使用 Element Plus
**理由**：
- 完善的组件生态
- 良好的移动端适配
- 活跃的社区支持
**替代方案**：Ant Design Vue → 更适合企业级后台

### 5. HTTP 客户端 - Axios
**决策**：使用 Axios
**理由**：
- 请求/响应拦截器
- 请求取消
- 自动 JSON 转换
- 广泛的浏览器支持

### 6. WebSocket 通知
**决策**：前端通过 Gateway 建立 WebSocket 连接
**理由**：
- Gateway 统一入口
- 后端 notification-service 处理订阅
- 支持断线重连

## 风险 / 权衡

| 风险 | 缓解措施 |
|------|----------|
| WebSocket 断线 | 实现心跳检测和自动重连机制 |
| Token 过期 | 监听 401 响应，跳转登录页 |
| 大文件上传 | 前端限制文件大小，显示进度条 |
| 移动端适配 | 使用 Element Plus 响应式组件 |

## Open Questions

1. 是否需要实现图片懒加载？
2. 视频上传是否支持断点续传？
3. 是否需要支持 Markdown 评论格式？

---

## 设计规范：清新风格 (Fresh Style)

### 视觉风格定义

**小清新风格** - 简洁、明亮、通透、自然

### 色彩方案

| 用途 | 颜色 | 色值 |
|------|------|------|
| 主色 | 薄荷绿 | `#4CAF82` |
| 辅助色 | 天空蓝 | `#87CEEB` |
| 强调色 | 珊瑚粉 | `#FF7F7F` |
| 背景色 | 米白 | `#FAFAFA` |
| 卡片背景 | 纯白 | `#FFFFFF` |
| 文字主色 | 深灰 | `#2C3E50` |
| 文字次要 | 浅灰 | `#95A5A6` |
| 边框色 | 淡灰 | `#E8E8E8` |
| 成功色 | 草绿 | `#66BB6A` |
| 错误色 | 浅红 | `#EF5350` |

### 字体规范

- **主字体**：`"PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif`
- **英文/数字**：`"SF Pro Display", "Helvetica Neue", sans-serif`
- **正文字号**：14px
- **标题字号**：18px-24px
- **行高**：1.6-1.8

### 间距系统

- **基础单位**：8px
- **小间距**：8px
- **中间距**：16px
- **大间距**：24px
- **页面边距**：24px（移动端 16px）

### 圆角规范

- **小组件**（按钮、输入框）：8px
- **卡片组件**：12px
- **图片/头像**：50%（圆形）或 8px
- **模态框**：16px

### 阴影规范

```css
/* 卡片悬浮阴影 */
box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

/* 弹窗阴影 */
box-shadow: 0 4px 24px rgba(0, 0, 0, 0.12);
```

### 动效规范

- **过渡时长**：200ms-300ms
- **缓动函数**：`cubic-bezier(0.4, 0, 0.2, 1)`
- **悬停效果**：轻微上移 + 阴影加深
- **页面切换**：淡入淡出

### 组件设计原则

1. **留白充足**：内容区域与边框保持呼吸空间
2. **圆角柔和**：避免直角，使用柔和圆角
3. **图标简洁**：使用线性图标，线条轻盈
4. **图片处理**：圆角裁剪，保持比例
5. **文字层次**：主次分明，使用字重区分

### 页面布局特点

- **顶部导航栏**：简洁图标 + 文字，高度 56px
- **内容区**：单列布局，最大宽度 680px（居中）
- **卡片间距**：每个卡片间 16px 间距
- **底部留白**：页面底部至少 32px 空白

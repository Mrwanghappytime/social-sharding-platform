## 为什么

需要一个社交分享平台，支持用户发布动态、互动（评论、点赞）以及社交关系（关注/粉丝），并提供实时通知。用户规模预计数千到数万，需要可扩展架构。

## 变更内容

构建全新的社交分享平台后端系统，包含以下服务：

- **用户服务**：用户注册、登录（JWT）、个人信息管理
- **动态服务**：动态发布、编辑、删除、列表查询、标题搜索
- **互动服务**：评论、点赞、计数统计
- **关系服务**：关注/粉丝管理（16张分表）
- **通知服务**：实时 WebSocket 推送 + Redis Pub/Sub + 消息持久化（KOL混合模型）
- **文件服务**：图片/视频上传、静态文件访问

媒体约束：
- 图片：最多9张，默认展示第一张
- 视频：最大50MB，与图片不可混搭

## 功能 (Capabilities)

### 新增功能

- `user-management`: 用户注册、登录、认证（JWT）
- `post-management`: 动态 CRUD、标题搜索（MySQL LIKE）、媒体关联
- `interaction`: 评论和点赞功能
- `social-graph`: 关注/粉丝关系（16张分表，按 user_id % 16 分片）
- `notification`: 实时推送（WebSocket + Redis）+ 离线消息持久化 + KOL 混合模型
- `file-management`: 图片/视频上传服务，静态文件直接访问
- `search`: 按动态标题搜索

### 修改功能

（无）

## 影响

- 新增 6 个微服务（Dubbo + Nacos 服务发现）
- 新增 MySQL 表：users, posts, comments, likes, follows, notifications, files
- 新增 Redis 用于 Session 缓存、Pub/Sub 通知、热点数据缓存
- 新增本地文件存储（/data/files/images/, /data/files/videos/）
- Gateway 处理 JWT 验证 + WebSocket 连接管理

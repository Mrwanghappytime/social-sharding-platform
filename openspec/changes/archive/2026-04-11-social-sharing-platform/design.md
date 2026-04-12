## 上下文

构建一个社交分享平台，用户可以发布动态、评论、点赞、关注其他用户。系统采用微服务架构（Spring Boot + Dubbo + Gateway + Nacos），支持实时通知。

**技术约束：**
- 后端：Spring Boot + Dubbo + Gateway
- 服务发现：Dubbo + Nacos
- 认证：JWT
- 数据库：MySQL
- 缓存/消息：Redis
- 文件存储：本地磁盘 + 独立文件服务

**规模目标：** 数千到数万用户

## 目标 / 非目标

**目标：**
- 实现完整的用户认证体系（注册、登录、JWT）
- 实现动态发布功能，支持图片（最多9张）和视频（最大50MB）
- 实现评论和点赞互动
- 实现关注/粉丝系统（16张分表）
- 实现实时通知（WebSocket + Redis Pub/Sub + 持久化）
- 实现文件上传服务
- 实现按标题搜索动态

**非目标：**
- 不实现私信功能
- 不实现深度的推荐算法
- 暂不考虑分库（仅分表）
- 暂不使用 ES（未来可扩展）

## 决策

### 1. 服务拆分

| 服务 | 职责 |
|------|------|
| user-service | 用户注册、登录、Profile 管理 |
| post-service | 动态 CRUD、媒体关联、标题搜索 |
| interaction-service | 评论、点赞、计数 |
| relation-service | 关注/粉丝（分表管理） |
| notification-service | 通知持久化、Redis Pub/Sub、推送 |
| file-service | 文件上传、静态文件访问 |

**决策理由：** 按业务领域拆分，职责清晰，便于独立扩展。

### 2. 通知推送模型（混合模型）

```
普通用户（粉丝 < 10000）：
  点赞/评论 → 写库 → 立即 WebSocket 推送

KOL 用户（粉丝 >= 10000）：
  点赞/评论 → 写库 → 不立即推送
  用户上线 → 拉取未读通知
```

**决策理由：** 避免高粉丝数用户的通知广播风暴，同时保证实时性。

**KOL 阈值：** 10000 粉丝（待上线前配置）

### 3. 关注/粉丝分表策略

采用两张物理表 + 组合分片键：

```
following_table (我关注的人):
  following_id % 16 → 分表键
  WHERE follower_id = ? → 查询"我关注的人"

followers_table (我的粉丝):
  follower_id % 16 → 分表键
  WHERE following_id = ? → 查询"我的粉丝"
```

**决策理由：** 两表分离使得"查我关注的人"和"查我的粉丝"都能快速定位分表，避免跨表查询。

### 4. 实时通知架构

```
                    ┌──────────────────────────────────┐
                    │           Gateway                │
                    │  • JWT 验证                      │
                    │  • WebSocket 连接管理             │
                    │  • Nacos 服务发现                 │
                    └──────────────────┬───────────────┘
                                       │
                    ┌──────────────────▼───────────────┐
                    │    Notification Service          │
                    │  • Redis Pub/Sub 订阅            │
                    │  • 通知持久化到 MySQL             │
                    │  • KOL 阈值判断                  │
                    └──────────────────┬───────────────┘
                                       │
                    ┌──────────────────▼───────────────┐
                    │         Redis                     │
                    │  • Pub/Sub 频道                   │
                    │  • 会话缓存                       │
                    │  • 热点数据缓存                   │
                    └──────────────────────────────────┘
```

### 5. 文件服务架构

```
POST /files/upload
  ↓
file-service 验证文件类型和大小
  ↓
保存到本地磁盘 /data/files/{type}/{uuid}.{ext}
  ↓
返回 URL: /files/{type}/{uuid}.{ext}

GET /files/{type}/{uuid}.{ext}
  ↓
file-service 直接 serve 静态文件
```

**媒体类型：**
- 图片：images/
- 视频：videos/

**约束校验：**
- 图片：最多9张，单次上传仅支持一种类型
- 视频：最大 50MB，与图片不可混搭

### 6. 数据库设计

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   users    │     │   posts    │     │  comments   │
├─────────────┤     ├─────────────┤     ├─────────────┤
│ id (PK)    │────▶│ id (PK)    │────▶│ id (PK)    │
│ username   │     │ user_id    │     │ post_id    │
│ password   │     │ title       │     │ user_id    │
│ avatar     │     │ content     │     │ content    │
│ created_at │     │ type        │     │ created_at │
└─────────────┘     │ created_at │     └─────────────┘
                    └─────────────┘            │
                          │                    │
                          ▼                    ▼
                    ┌─────────────┐     ┌─────────────┐
                    │   likes     │     │ follows     │
                    ├─────────────┤     ├─────────────┤
                    │ id (PK)    │     │ id (PK)    │
                    │ post_id    │     │ follower_id │
                    │ user_id    │     │ following_id│
                    │ created_at │     │ created_at  │
                    └─────────────┘     └─────────────┘
                                             │
┌─────────────┐                               │
│notifications│   (16张分表: notifications_0 ~ 15)
├─────────────┤     按 recipient_id % 16 分片
│ id (PK)    │
│ recipient_id│
│ type        │
│ actor_id    │
│ target_id   │
│ target_type │
│ is_read     │
│ created_at  │
└─────────────┘

┌─────────────┐
│   files     │
├─────────────┤
│ id (PK)     │
│ post_id     │
│ url         │
│ type        │
│ sort_order  │
│ created_at  │
└─────────────┘
```

## 风险 / 权衡

| 风险 | 缓解措施 |
|------|----------|
| 分表后跨表查询（如共同关注）性能差 | 限制查询范围，必要时走应用层聚合 |
| KOL 阈值配置后需调整 | 预留配置中心动态调整阈值 |
| 本地文件存储无法水平扩展 | 未来迁移到 OSS/S3，URL 格式兼容 |
| WebSocket 连接数上限 | Gateway 层做好连接管理，支持横向扩展 |
| MySQL LIKE 搜索性能在大数据量下差 | 未来平滑迁移到 ES |

## 待定事项

1. KOL 阈值最终数值（当前设定 10000）
2. 分表初始数量（当前设定 16）
3. JWT Token 过期时间配置

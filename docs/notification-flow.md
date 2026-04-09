# 消息通知流程文档

## 1. 点赞通知流程概述

当用户 A 点赞用户 B 的帖子时，系统通过以下组件完成实时通知推送：

```mermaid
sequenceDiagram
    participant UA as 用户A (操作者)
    participant FE as 前端
    participant GW as Gateway
    participant IS as Interaction Service
    participant NS as Notification Service
    participant Redis as Redis Pub/Sub
    participant US as User Service
    participant DB as MySQL

    UA->>FE: 点击点赞按钮
    FE->>GW: POST /api/interactions/posts/{postId}/like
    GW->>IS: Dubbo RPC: likePost(postId, userId)

    rect rgb(240, 248, 255)
        Note over IS,NS: 点赞处理与通知触发
        IS->>DB: 更新帖子点赞数
        IS->>NS: Dubbo RPC: sendNotification(recipientId, LIKE, ...)

        NS->>DB: 保存通知记录 (notifications表)
        NS->>US: Dubbo RPC: getFollowerCount(recipientId)
        US-->>NS: 返回粉丝数量

        alt 粉丝数 < 10000 (非KOL)
            NS->>Redis: PUBLISH notification:channel:{recipientId}
            Redis-->>NS: 消息已发布
        else 粉丝数 >= 10000 (KOL)
            Note over NS: 跳过实时推送，用户登录时轮询
        end
    end

    NS-->>IS: 通知发送完成
    IS-->>FE: 返回点赞结果

    rect rgb(255, 250, 240)
        Note over Redis,UA: 实时推送阶段
        Redis->>GW: 订阅消息通知
        GW->>UA: WebSocket 推送通知
    end
```

## 2. 核心组件职责

| 组件 | 职责 |
|------|------|
| **Gateway** | JWT鉴权、WebSocket服务、路由分发 |
| **Interaction Service** | 处理点赞/评论等互动操作，调用通知服务 |
| **Notification Service** | 通知持久化、KOL判断、Redis消息发布 |
| **User Service** | 提供用户信息查询服务 |
| **Redis** | Pub/Sub消息通道、缓存 |
| **MySQL** | 通知数据持久化存储 |

## 3. 详细流程步骤

```mermaid
flowchart TD
    A[用户A点击点赞] --> B{请求是否有效?}
    B -->|否| C[返回错误]
    B -->|是| D[调用likePost接口]

    D --> E[更新帖子点赞数+1]
    E --> F[调用NotificationService发送通知]

    F --> G[保存通知到数据库]
    G --> H[查询被点赞用户粉丝数]

    H --> I{粉丝数 >= 10000?}
    I -->|是 KOL| J[跳过实时推送]
    I -->|否 非KOL| K[发布Redis消息到channel]

    K --> L[通知发送成功]
    J --> L

    L --> M[WebSocket推送目标用户]
```

## 4. KOL通知策略

根据 CLAUDE.md 中的设计：

> **KOL Notification Model:** 用户 with >= 10,000 followers don't receive real-time WebSocket pushes; they poll on login instead.

```mermaid
flowchart LR
    A[收到通知请求] --> B{粉丝数 >= 10000?}
    B -->|是| C[KOL用户]
    B -->|否| D[普通用户]

    C --> E[仅持久化到数据库]
    C --> F[登录时轮询获取]
    D --> G[持久化 + Redis发布]
    G --> H[实时WebSocket推送]
```

## 5. 消息格式

### 5.1 Redis Pub/Sub 消息

```
Channel: notification:channel:{recipientId}
Message: {notificationId}:{type}:{actorId}:{targetId}:{targetType}
```

示例：
```
Channel: notification:channel:2
Message: 123:LIKE:1:456:POST
```

### 5.2 消息字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| notificationId | Long | 通知ID |
| type | String | 通知类型 (LIKE/COMMENT/FOLLOW) |
| actorId | Long | 触发通知的用户ID |
| targetId | Long | 目标ID (帖子ID/评论ID) |
| targetType | String | 目标类型 (POST/COMMENT) |

## 6. 数据库表结构

```sql
-- notifications 表
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,      -- 接收通知的用户ID
    type VARCHAR(20) NOT NULL,         -- LIKE/COMMENT/FOLLOW
    actor_id BIGINT NOT NULL,          -- 触发通知的用户ID
    target_id BIGINT NOT NULL,          -- 目标ID
    target_type VARCHAR(50),            -- 目标类型
    is_read TINYINT(1) DEFAULT 0,     -- 是否已读
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_recipient_id (recipient_id),
    INDEX idx_created_at (created_at)
);
```

## 7. WebSocket连接流程

```mermaid
sequenceDiagram
    participant User as 用户 (WebSocket客户端)
    participant GW as Gateway WebSocket

    User->>GW: 连接 /ws/notifications
    Note over GW: JWT Token 验证
    GW-->>User: 连接成功

    loop 持续连接
        User->>GW: 心跳检测 (每30秒)
        GW-->>User: Pong
    end

    Note over GW,Redis: 后台通过Redis Pub/Sub接收通知
    Redis->>GW: 新通知消息
    GW->>User: 推送通知数据
```

## 8. 错误处理

### 8.1 UserService不可用时

当 `getFollowerCount()` 调用失败时，系统默认将用户视为非KOL，直接发送实时通知：

```java
try {
    Long followerCount = userService.getFollowerCount(recipientId);
    isKol = followerCount != null && followerCount >= kolFollowerThreshold;
} catch (Exception e) {
    log.warn("Failed to get follower count, treating as non-KOL: {}", e.getMessage());
    isKol = false; // 降级处理，确保通知发送
}
```

### 8.2 通知发送失败

| 错误场景 | 处理方式 |
|---------|---------|
| Redis连接失败 | 记录日志，通知持久化成功即可 |
| 数据库保存失败 | 向上抛出异常 |
| WebSocket断开 | 用户重连后通过轮询获取离线通知 |

## 9. 相关配置

### 9.1 Notification Service配置

```yaml
notification:
  kol:
    follower-threshold: 10000  # KOL粉丝数阈值
```

### 9.2 Dubbo服务版本

```yaml
# NotificationService
@DubboService(interfaceClass = NotificationService.class, version = "1.0.0")

# UserService引用
@DubboReference(version = "1.0.0", check = false)
private UserService userService;
```

## 10. 性能优化

- **通知持久化**: 所有通知先落库，保证可靠性
- **KOL降级**: 大V用户不进行实时推送，减少Redis压力
- **异步处理**: 通知发送为异步调用，不阻塞主流程
- **连接复用**: Dubbo连接池管理长连接

# Controller 接口调用链路文档

## 重构目标检查

| 目标 | 状态 | 说明 |
|------|------|------|
| Facade层处理跨服务编排 | ✅ 完成 | 所有Controller在facade-service |
| Facade层负责Enrichment | ✅ 完成 | PostFacadeController.enrichPostDTO() |
| Facade层负责通知发送 | ✅ 完成 | InteractionFacadeController.sendNotification() |
| Dubbo接口返回纯净DTO | ✅ 完成 | Dubbo接口只返回PostDTO/CommentDTO等 |
| 严格DTO边界 | ✅ 完成 | facade有独立的Request/Response DTO |
| 移除微服务间不必要的调用 | ✅ 完成 | interaction-service不再调用user-service/notification-service |

---

## 1. UserFacadeController (`/users`)

| 接口 | HTTP | 调用链 |
|------|------|--------|
| `/users/register` | POST | → userService.register() |
| `/users/login` | POST | → userService.login() |
| `/users/me` | GET | → userService.getUserById() |
| `/users/avatar` | PUT | → userService.updateAvatar() |
| `/users/{id}` | GET | → userService.getUserById() |

**依赖服务**: user-service

---

## 2. PostFacadeController (`/posts`)

| 接口 | HTTP | 调用链 |
|------|------|--------|
| `/posts` (创建) | POST | → postService.createPost() |
| `/posts/{id}` | GET | → postService.getPostById() → enrichPostDTO() → userService.getUserById(), interactionService.getLikeStatus() |
| `/posts/{id}` | DELETE | → postService.deletePost() |
| `/posts/user/{userId}` | GET | → postService.getUserPosts() → enrichPostDTO() → userService.getUserById(), interactionService.getLikeStatus() |
| `/posts/feed` | GET | → postService.getFeed() → enrichPostDTO() → userService.getUserById(), interactionService.getLikeStatus() |
| `/posts/search` | GET | → postService.searchPosts() → enrichPostDTO() → userService.getUserById(), interactionService.getLikeStatus() |

**依赖服务**: post-service, user-service, interaction-service

**Enrichment逻辑**:
```
PostDTO → PostFacadeResponse
  ├── userService.getUserById() → username, userAvatar
  └── interactionService.getLikeStatus() → isLiked
```

---

## 3. InteractionFacadeController (`/interactions`)

| 接口 | HTTP | 调用链 |
|------|------|--------|
| `/interactions/posts/{postId}/like` | POST | 1. postService.isPostExists() → 2. interactionService.likePost() → 3. postService.incrementLikeCount() → 4. userService.getUserById() + notificationService.sendNotification() |
| `/interactions/posts/{postId}/like` | DELETE | 1. interactionService.unlikePost() → 2. postService.decrementLikeCount() |
| `/interactions/posts/{postId}/like` | GET | → interactionService.getLikeStatus() |
| `/interactions/posts/{postId}/comments` | POST | 1. postService.isPostExists() → 2. interactionService.commentOnPost() → 3. postService.incrementCommentCount() → 4. userService.getUserById() → 5. notificationService.sendNotification() |
| `/interactions/comments/{id}` | DELETE | 1. interactionService.getComments() → 2. interactionService.deleteComment() → 3. postService.decrementCommentCount() |
| `/interactions/posts/{postId}/comments` | GET | 1. interactionService.getComments() → 2. enrichComments() → userService.getUserById() |

**依赖服务**: post-service, interaction-service, user-service, notification-service

**通知发送逻辑**:
```
点赞: notificationService.sendNotification(LIKE, postOwnerId, actorId, postId, "POST")
评论: notificationService.sendNotification(COMMENT, postOwnerId, actorId, postId, "POST")
```

---

## 4. RelationFacadeController (`/relations`)

| 接口 | HTTP | 调用链 |
|------|------|--------|
| `/relations/follow/{userId}` | POST | 1. userService.isUserExists() → 2. relationService.follow() → 3. userService.getUserById() + notificationService.sendNotification() |
| `/relations/follow/{userId}` | DELETE | → relationService.unfollow() |
| `/relations/following` | GET | → relationService.getFollowingList() → enrichRelationList() → userService.getUserById() |
| `/relations/followers` | GET | → relationService.getFollowersList() → enrichRelationList() → userService.getUserById() |
| `/relations/following/{userId}` | GET | → relationService.getFollowingList() → enrichRelationList() → userService.getUserById() |
| `/relations/followers/{userId}` | GET | → relationService.getFollowersList() → enrichRelationList() → userService.getUserById() |
| `/relations/counts/{userId}` | GET | → relationService.getRelationCounts() |
| `/relations/is-following/{userId}` | GET | → relationService.isFollowing() |

**依赖服务**: relation-service, user-service, notification-service

**Enrichment逻辑**:
```
UserRelationDTO → UserRelationFacadeResponse
  └── userService.getUserById() → username, avatar
```

**通知发送逻辑**:
```
关注: notificationService.sendNotification(FOLLOW, targetUserId, actorId, targetUserId, "USER")
```

---

## 5. NotificationFacadeController (`/notifications`)

| 接口 | HTTP | 调用链 |
|------|------|--------|
| `/notifications` | GET | → notificationService.getNotificationList() |
| `/notifications/{id}/read` | PUT | → notificationService.markAsRead() |
| `/notifications/read-all` | PUT | → notificationService.markAllAsRead() |
| `/notifications/unread-count` | GET | → notificationService.getUnreadCount() |

**依赖服务**: notification-service

---

## 6. FileFacadeController (`/files`)

| 接口 | HTTP | 调用链 |
|------|------|--------|
| `/files/upload` | POST | → RestTemplate → file-service |
| `/files/{id}` | DELETE | → RestTemplate → file-service |
| `/files/{type}/{filename}` | GET | → RestTemplate → file-service |

**依赖服务**: file-service (通过HTTP)

---

## 架构图

```
                        ┌─────────────────────────────────────────────────────────────┐
                        │                      Gateway (8080)                        │
                        │                   JWT Auth + Route                         │
                        └─────────────────────────┬───────────────────────────────────┘
                                                  │
                                                  ▼
                        ┌─────────────────────────────────────────────────────────────┐
                        │                Facade Service (8087)                       │
                        │                                                              │
                        │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐   │
                        │  │UserFacade   │  │PostFacade   │  │InteractionFacade   │   │
                        │  │Controller   │  │Controller   │  │Controller          │   │
                        │  └──────┬──────┘  └──────┬──────┘  └──────────┬────────┘   │
                        │         │                │                      │            │
                        │  ┌──────┴──────┐  ┌──────┴──────┐  ┌──────────┴────────┐   │
                        │  │Enrichment   │  │Enrichment   │  │Cross-service       │   │
                        │  │(username,   │  │(username,   │  │Orchestration       │   │
                        │  │ avatar)     │  │isLiked)     │  │(notification)      │   │
                        │  └─────────────┘  └─────────────┘  └───────────────────┘   │
                        └─────────────────────────────────────────────────────────────┘
                                  │                │                      │
                                  ▼                ▼                      ▼
         ┌───────────────────────────────────────────────────────────────────────────────┐
         │                           Dubbo RPC (Nacos)                                    │
         ├───────────────┬───────────────┬───────────────┬───────────────┬───────────────┤
         │ user-service  │ post-service  │interaction-svc│relation-svc   │notification-svc│
         │   (8081)      │   (8082)      │   (8083)      │   (8084)      │   (8085)      │
         │               │               │               │               │               │
         │ 纯CRUD        │ 纯CRUD        │ 纯CRUD        │ 纯CRUD        │ 纯CRUD        │
         │ 不调用其他服务  │ 不调用其他服务  │ 不调用其他服务  │ 不调用其他服务  │ 不调用其他服务  │
         └───────────────┴───────────────┴───────────────┴───────────────┴───────────────┘
```

---

## DTO 边界

| 层 | DTO类型 | 说明 |
|----|---------|------|
| Dubbo接口入参 | PostDTO, CommentDTO, UserRelationDTO | 纯净的业务主体数据，不包含关联数据 |
| Dubbo接口返回 | PostDTO, CommentDTO, UserRelationDTO, NotificationDTO | 纯净的业务主体数据 |
| Facade入参 | CreatePostRequest, CreateCommentRequest | 独立Request类 |
| Facade返回 | PostFacadeResponse, CommentFacadeResponse, UserRelationFacadeResponse | 包含enrichment数据 |

---

## 微服务职责

| 服务 | 职责 | 是否调用其他服务 |
|------|------|----------------|
| user-service | 用户CRUD | 否 |
| post-service | 帖子CRUD + 计数更新 | 否 |
| interaction-service | 点赞/评论CRUD | 否 |
| relation-service | 关注/粉丝CRUD | 否 |
| notification-service | 通知存储 + Redis Pub/Sub | 否 |
| file-service | 文件上传/下载 | 否 |
| facade-service | 跨服务编排 + Enrichment + 通知发送 | 是（调用所有底层服务） |

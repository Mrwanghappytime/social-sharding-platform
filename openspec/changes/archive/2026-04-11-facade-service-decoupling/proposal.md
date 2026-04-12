## 为什么

当前 facade-service 层已引入，但底层微服务之间仍存在大量不必要的相互调用。例如 InteractionService 调用 PostService 增减计数、调用 NotificationService 发通知、调用 UserService 做 enrichment；RelationService 和 NotificationService 也存在类似问题。这违反了单一职责原则，使服务边界模糊，facade 层无法真正统一编排业务。

## 变更内容

### 核心原则
- **底层微服务**：只做自己主体相关的业务，不做跨服务聚合
- **Facade 层**：负责所有跨服务编排、enrichment、notifications、业务验证

### 需要改造的服务

**1. InteractionServiceImpl 移除以下跨服务调用：**
- `PostService.isPostExists()` - facade 验证
- `PostService.incrementLikeCount()` / `decrementLikeCount()` - facade 调用
- `PostService.getUserIdByPostId()` - facade 获取用于发通知
- `UserService.getUserById()` - 移除评论 enrichment，facade 负责
- `NotificationService.sendNotification()` - facade 调用

**2. RelationServiceImpl 移除以下跨服务调用：**
- `UserService.isUserExists()` - facade 验证
- `NotificationService.sendNotification()` - facade 调用

**3. NotificationServiceImpl 移除以下跨服务调用：**
- `UserService.getFollowerCount()` - KOL 判断，facade 做
- `UserService.getUserById()` - 通知 enrichment，facade 负责

### Facade 层新增编排能力

```
likePost(postId, userId):
  1. postService.isPostExists(postId)  // 验证
  2. interactionService.likePost(postId, userId)
  3. postService.incrementLikeCount(postId)
  4. userService.getUserById(userId)  // 获取actor信息
  5. notificationService.sendNotification(postOwnerId, ..., actorUsername, actorAvatar)

commentOnPost(postId, userId, content):
  1. postService.isPostExists(postId)  // 验证
  2. comment = interactionService.commentOnPost(postId, userId, content)
  3. postService.incrementCommentCount(postId)
  4. user = userService.getUserById(userId)  // enrich
  5. comment.setUsername(user.getUsername())
  6. comment.setUserAvatar(user.getAvatar())
  7. notificationService.sendNotification(...)

follow(followerId, followingId):
  1. userService.isUserExists(followingId)  // 验证
  2. relationService.follow(followerId, followingId)
  3. user = userService.getUserById(followerId)  // enrich actor
  4. notificationService.sendNotification(followingId, ..., actorUsername, actorAvatar)

getFollowingList(userId):
  1. relations = relationService.getFollowingList(userId)
  2. enrich each relation with userService.getUserById()

getFollowersList(userId):
  1. relations = relationService.getFollowersList(userId)
  2. enrich each relation with userService.getUserById()

sendNotification(recipientId, type, actorId, targetId, targetType):
  1. actor = userService.getUserById(actorId)  // enrich
  2. followerCount = userService.getFollowerCount(recipientId)  // KOL判断
  3. notificationService.sendNotification(recipientId, type, actorId, targetId, targetType, actorUsername, actorAvatar, isKol)
```

### DTO 边界定义

**原则：Dubbo接口返回的DTO只能包含自己业务主体的信息，不能包含其他业务的数据。**

#### 底层Dubbo接口 DTO（只含主体数据）

| DTO | 包含字段 | 不包含字段 |
|-----|---------|-----------|
| `PostDTO` | id, userId, title, content, type, likeCount, commentCount, imageUrls, videoUrl, createdAt | ~~username~~, ~~userAvatar~~, ~~isLiked~~, ~~mediaFiles~~ |
| `CommentDTO` | id, postId, userId, content, createdAt | ~~username~~, ~~userAvatar~~ |
| `UserRelationDTO` | userId | ~~username~~, ~~avatar~~ (这些由facade enrichment) |
| `LikeStatusDTO` | liked, likeCount | (保持不变) |

#### Facade 层 DTO（包含enrichment后的完整数据）

| Facade DTO | 用途 | 包含字段 |
|------------|------|---------|
| `PostFacadeResponse` | 帖子详情响应 | PostDTO全部 + username, userAvatar, isLiked |
| `CommentFacadeResponse` | 评论响应 | CommentDTO全部 + username, userAvatar |
| `UserRelationFacadeResponse` | 关注/粉丝响应 | UserRelationDTO全部 + username, avatar |
| `PageResultFacadeResponse<T>` | 分页响应包装 | records, total, page, size |

#### DTO 不得共用

- **Dubbo接口入参**：使用 `common.dto` 中的基础DTO（如 `CreatePostRequest`）
- **Facade层入参**：使用 `facade-service.dto` 中的 `*FacadeRequest`（如 `CreatePostFacadeRequest`）
- **Dubbo接口出参**：使用 `common.dto` 中的纯净DTO（如 `PostDTO`）
- **Facade层出参**：使用 `facade-service.dto` 中的 `*FacadeResponse`（如 `PostFacadeResponse`）

## 功能 (Capabilities)

### 新增功能
- `facade-service-orchestration`: facade 层新增业务编排能力，负责跨服务调用编排、enrichment、notifications、KOL判断
- `dto-boundaries`: 严格定义 Dubbo 接口与 Facade 层之间的 DTO 边界，防止职责不清

### 修改功能
- 无现有规范需要修改

## 影响

### 服务改造
- **InteractionServiceImpl**: 移除 @DubboReference 依赖 PostService、UserService（enrich）、NotificationService
- **RelationServiceImpl**: 移除 @DubboReference 依赖 UserService、NotificationService
- **NotificationServiceImpl**: 移除 @DubboReference 依赖 UserService
- **Facade 层**: 新增大量编排逻辑，各 FacadeController 需要改造
- **PostServiceImpl**: 保持不变（无不合理依赖）

### DTO 改造
- **common.dto.PostDTO**: 移除 username, userAvatar, isLiked, mediaFiles 字段
- **common.dto.CommentDTO**: 移除 username, userAvatar 字段
- **common.dto.UserRelationDTO**: 移除 username, avatar 字段
- **facade-service.dto**: 新增 PostFacadeResponse, CommentFacadeResponse, UserRelationFacadeResponse 等

### 新增模块
- `facade-service/src/main/java/com/social/facade/dto/`: Facade层专用Request/Response DTO

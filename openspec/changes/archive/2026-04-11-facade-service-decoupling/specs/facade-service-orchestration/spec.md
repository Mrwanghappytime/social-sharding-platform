## 新增需求

### 需求：Facade 层业务编排能力

Facade 层必须统一负责跨服务调用编排、enrichment、notifications 和业务验证，底层微服务禁止直接调用其他服务。

#### 场景：点赞业务编排
- **当** 用户调用 POST /api/interactions/posts/{postId}/like
- **那么** Facade 层必须依次执行：
  1. 调用 PostService.isPostExists() 验证动态存在
  2. 调用 InteractionService.likePost() 保存点赞
  3. 调用 PostService.incrementLikeCount() 增加计数
  4. 调用 UserService.getUserById() 获取点赞者信息
  5. 调用 NotificationService.sendNotification() 发送通知（包含 actorUsername、actorAvatar）

#### 场景：评论业务编排
- **当** 用户调用 POST /api/interactions/posts/{postId}/comments
- **那么** Facade 层必须依次执行：
  1. 调用 PostService.isPostExists() 验证动态存在
  2. 调用 InteractionService.commentOnPost() 保存评论
  3. 调用 PostService.incrementCommentCount() 增加计数
  4. 调用 UserService.getUserById() 获取评论者信息
  5. 组装返回结果包含 username 和 userAvatar
  6. 调用 NotificationService.sendNotification() 发送通知

#### 场景：关注业务编排
- **当** 用户调用 POST /api/relations/follow/{followingId}
- **那么** Facade 层必须依次执行：
  1. 调用 UserService.isUserExists() 验证目标用户存在
  2. 调用 RelationService.follow() 保存关注关系
  3. 调用 UserService.getUserById() 获取关注者信息
  4. 调用 NotificationService.sendNotification() 发送通知

#### 场景：获取关注列表编排
- **当** 用户调用 GET /api/relations/following/{userId}
- **那么** Facade 层必须执行：
  1. 调用 RelationService.getFollowingList() 获取关注关系列表
  2. 对每条关系调用 UserService.getUserById() 填充用户信息

#### 场景：获取粉丝列表编排
- **当** 用户调用 GET /api/relations/followers/{userId}
- **那么** Facade 层必须执行：
  1. 调用 RelationService.getFollowersList() 获取粉丝关系列表
  2. 对每条关系调用 UserService.getUserById() 填充用户信息

#### 场景：发送通知编排
- **当** 需要发送通知时
- **那么** Facade 层必须执行：
  1. 调用 UserService.getUserById() 获取行为者信息
  2. 调用 UserService.getFollowerCount() 获取接收者粉丝数
  3. 根据粉丝数判断是否为 KOL
  4. 调用 NotificationService.sendNotification() 发送通知（包含 actorUsername、actorAvatar、isKol）

### 需求：底层服务职责限定

底层微服务必须只做自己主体相关的业务，禁止调用其他微服务。

#### 场景：InteractionService 职责限定
- **当** 调用 InteractionService 的点赞或评论方法
- **那么** 必须只操作 likes 和 comments 表，禁止调用 PostService、UserService、NotificationService

#### 场景：RelationService 职责限定
- **当** 调用 RelationService 的关注方法
- **那么** 必须只操作 following 和 followers 表，禁止调用 UserService、NotificationService

#### 场景：NotificationService 职责限定
- **当** 调用 NotificationService 的发送或查询方法
- **那么** 必须只操作 notifications 表，禁止调用 UserService

### 需求：Dubbo 接口 DTO 纯净度

Dubbo接口返回的DTO必须只包含自己业务主体的数据，禁止包含其他业务的数据。

#### 场景：PostDTO 纯净度
- **当** 调用 PostService 的任意方法返回 PostDTO
- **那么** PostDTO 必须只包含：id, userId, title, content, type, likeCount, commentCount, imageUrls, videoUrl, createdAt
- **那么** PostDTO 必须禁止包含：username, userAvatar, isLiked, mediaFiles

#### 场景：CommentDTO 纯净度
- **当** 调用 InteractionService 的方法返回 CommentDTO
- **那么** CommentDTO 必须只包含：id, postId, userId, content, createdAt
- **那么** CommentDTO 必须禁止包含：username, userAvatar

#### 场景：UserRelationDTO 纯净度
- **当** 调用 RelationService 的方法返回 UserRelationDTO
- **那么** UserRelationDTO 必须只包含：userId
- **那么** UserRelationDTO 必须禁止包含：username, avatar

### 需求：Facade 层 DTO 边界

Facade 层的 Request/Response DTO 必须与底层 Dubbo 接口的 DTO 严格分离。

#### 场景：Facade Response DTO 包含 Enrichment 数据
- **当** Facade 层返回帖子详情
- **那么** 必须返回 PostFacadeResponse，包含 PostDTO 全部字段 + username + userAvatar + isLiked

#### 场景：Facade Response DTO 包含评论 Enrichment
- **当** Facade 层返回评论列表
- **那么** 必须返回 CommentFacadeResponse，包含 CommentDTO 全部字段 + username + userAvatar

#### 场景：Facade 层禁止透传 Dubbo DTO
- **当** Facade 层处理请求
- **那么** 必须使用 Facade 层自己的 Request DTO（如 CreatePostFacadeRequest）
- **那么** 必须返回 Facade 层自己的 Response DTO（如 PostFacadeResponse）
- **那么** 禁止直接将 Dubbo 接口返回的 DTO 暴露给客户端

#### 场景：DTO 包路径分离
- **当** 项目结构
- **那么** Dubbo接口使用的DTO位于 common/src/main/java/com/social/common/dto/
- **那么** Facade层使用的DTO位于 facade-service/src/main/java/com/social/facade/dto/

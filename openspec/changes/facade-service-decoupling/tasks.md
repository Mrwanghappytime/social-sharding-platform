## 1. DTO 边界定义与创建

- [x] 1.1 创建 facade-service/src/main/java/com/social/facade/dto/ 目录
- [x] 1.2 创建 PostFacadeResponse 包含 PostDTO + username + userAvatar + isLiked
- [x] 1.3 创建 CommentFacadeResponse 包含 CommentDTO + username + userAvatar
- [x] 1.4 创建 UserRelationFacadeResponse 包含 userId + username + avatar
- [x] 1.5 创建 PageResultFacadeResponse<T> 包装类
- [x] 1.6 修改 PostFacadeRequest / CreatePostFacadeRequest（可选，验证是否需要）

## 2. 改造 common.dto 纯净度

- [x] 2.1 修改 PostDTO：移除 username, userAvatar, isLiked, mediaFiles 字段
- [x] 2.2 修改 CommentDTO：移除 username, userAvatar 字段
- [x] 2.3 修改 UserRelationDTO：移除 username, avatar 字段

## 3. 改造 NotificationService

- [x] 3.1 修改 NotificationService 接口，sendNotification 增加 actorUsername、actorAvatar、isKol 参数
- [x] 3.2 移除 NotificationServiceImpl 中的 @DubboReference UserService
- [x] 3.3 更新 NotificationServiceImpl.sendNotification() 使用传入的 actorUsername、actorAvatar、isKol

## 4. 改造 InteractionService

- [x] 4.1 移除 InteractionServiceImpl 中的 @DubboReference PostService
- [x] 4.2 移除 InteractionServiceImpl 中的 @DubboReference UserService
- [x] 4.3 移除 InteractionServiceImpl 中的 @DubboReference NotificationService
- [x] 4.4 简化 likePost() 方法，只保存点赞记录
- [x] 4.5 简化 unlikePost() 方法，只删除点赞记录
- [x] 4.6 简化 commentOnPost() 方法，只保存评论（不 enrich 用户信息）
- [x] 4.7 简化 getComments() 方法，只返回评论列表（不 enrich 用户信息）

## 5. 改造 RelationService

- [x] 5.1 移除 RelationServiceImpl 中的 @DubboReference UserService
- [x] 5.2 移除 RelationServiceImpl 中的 @DubboReference NotificationService
- [x] 5.3 简化 follow() 方法，只保存关注关系
- [x] 5.4 简化 getFollowingList() 方法，只返回用户 ID 列表（不 enrich）
- [x] 5.5 简化 getFollowersList() 方法，只返回用户 ID 列表（不 enrich）

## 6. 改造 PostFacadeController

- [x] 6.1 修改 createPost() 返回 PostFacadeResponse（enriched）
- [x] 6.2 修改 getPostById() 返回 PostFacadeResponse（enriched）
- [x] 6.3 修改 getUserPosts() 返回包含 PostFacadeResponse 的分页结果
- [x] 6.4 修改 getFeed() 返回包含 PostFacadeResponse 的分页结果
- [x] 6.5 修改 searchPosts() 返回包含 PostFacadeResponse 的分页结果
- [x] 6.6 实现 enrichPostDTO() 方法，将 PostDTO 转换为 PostFacadeResponse

## 7. 改造 InteractionFacadeController

- [x] 7.1 likePost() 方法新增编排：验证 post 存在、调用 PostService.incrementLikeCount、发通知
- [x] 7.2 unlikePost() 方法新增编排：调用 PostService.decrementLikeCount
- [x] 7.3 commentOnPost() 方法新增编排 + enrichment：验证 post、调用计数、enrich用户信息、发通知
- [x] 7.4 getComments() 方法返回 CommentFacadeResponse（enriched）
- [x] 7.5 getLikeStatus() 方法保持不变（LikeStatusDTO 无需 enrichment）

## 8. 改造 RelationFacadeController

- [x] 8.1 follow() 方法新增编排：验证用户存在、调用 NotificationService 发通知
- [x] 8.2 getFollowingList() 方法返回 UserRelationFacadeResponse 列表（enriched）
- [x] 8.3 getFollowersList() 方法返回 UserRelationFacadeResponse 列表（enriched）
- [x] 8.4 unfollow() 方法保持不变
- [x] 8.5 getRelationCounts() 方法保持不变

## 9. 改造 NotificationFacadeController

- [x] 9.1 sendNotification() 方法新增编排：获取 actor 信息、判断 KOL、发通知（已在 InteractionFacadeController 和 RelationFacadeController 中实现）
- [x] 9.2 getNotificationList() 方法返回 NotificationDTO 列表（facade层不做额外enrichment，NotificationService已处理）
- [x] 9.3 markAsRead() / markAllAsRead() 方法保持不变

## 10. 验证测试

- [ ] 10.1 启动所有服务，验证点赞功能正常
- [ ] 10.2 验证评论功能正常，包含用户信息（username, userAvatar）
- [ ] 10.3 验证关注/取关功能正常
- [ ] 10.4 验证通知功能正常
- [ ] 10.5 验证 Feed 流功能正常，返回 PostFacadeResponse 包含 enrichment 数据
- [ ] 10.6 验证用户主页功能正常
- [ ] 10.7 验证搜索功能正常

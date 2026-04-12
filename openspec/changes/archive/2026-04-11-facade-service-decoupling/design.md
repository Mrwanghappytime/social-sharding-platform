## 上下文

当前系统已引入 facade-service 聚合层，但底层微服务仍存在不必要的相互调用：

| 服务 | 问题 |
|------|------|
| InteractionServiceImpl | 调用 PostService 增减计数、调用 NotificationService 发通知、调用 UserService 做 enrichment |
| RelationServiceImpl | 调用 UserService 验证用户、调用 NotificationService 发通知 |
| NotificationServiceImpl | 调用 UserService 做 KOL 判断和 enrichment |

这种架构导致：
- 服务边界模糊，违反单一职责
- facade 层无法真正统一编排业务
- 服务间耦合度高，难以独立演进

## 目标 / 非目标

**目标：**
- 底层微服务只做自己主体相关的业务逻辑
- Facade 层统一负责跨服务编排、enrichment、notifications
- 消除底层服务间的循环依赖

**非目标：**
- 不改变底层微服务的数据模型
- 不改变 Dubbo 接口的基本签名
- PostServiceImpl 无需改造（无不合理依赖）

## 决策

### 决策 1：底层服务简化 vs 保留必要调用

**选择**：移除所有跨服务调用，底层只做纯粹的主体业务

**理由**：
- 当前问题的根源是底层服务承担了太多聚合职责
- Facade 层已经存在，应该承担这些职责
- 简化底层服务可提高独立性和可测试性

**替代方案考虑**：
- 保留部分必要调用（如 InteractionService 调用 PostService.isPostExists 验证）
- **拒绝原因**：Facade 层应该做这个验证，底层服务不应该知道业务上下文

### 决策 2：NotificationService 接口变更

**选择**：NotificationService.sendNotification 增加 actorUsername、actorAvatar 参数，移除 KOL 判断

**理由**：
- 避免 NotificationService 调用 UserService 获取用户信息
- KOL 判断由 Facade 做，sendNotification 接口需要更多参数

**变更后的接口**：
```java
void sendNotification(Long recipientId, NotificationType type, Long actorId, Long targetId,
                     String targetType, String actorUsername, String actorAvatar, boolean isKol);
```

### 决策 3：Facade 层编排方式

**选择**：在 FacadeController 中直接编排多个 Dubbo 调用

**理由**：
- 简单直接，易于理解和调试
- 与现有 facade 架构一致
- 可以利用 Spring 的依赖注入和事务管理

### 决策 4：DTO 边界划分

**选择**：Dubbo接口与Facade层严格隔离DTO，不能共用

**理由**：
- 防止职责不清：Dubbo接口应该只返回自己业务主体的数据
- Facade层负责 enrichment，需要自己的Response DTO包含完整数据
- Facade层入参可能需要额外字段（如currentUserId用于权限校验），不能与Dubbo接口共用

**Dubbo接口返回的纯净DTO**：
```java
// PostService 返回的 - 只有主体数据
PostDTO {
    Long id;
    Long userId;
    String title;
    String content;
    PostType type;
    Integer likeCount;
    Integer commentCount;
    List<String> imageUrls;  // 只有URL字符串，不含FileDTO
    String videoUrl;
    LocalDateTime createdAt;
    // 无: username, userAvatar, isLiked
}
```

**Facade层Enriched Response**：
```java
// Facade层返回的 - 包含enrichment数据
PostFacadeResponse {
    PostDTO base;           // 嵌入纯净的PostDTO
    String username;         // facade enrichment
    String userAvatar;       // facade enrichment
    Boolean isLiked;        // facade enrichment
}
```

### 决策 5：Request DTO 分离

**选择**：Facade层入参与Dubbo接口入参分离

**理由**：
- Facade层可能需要额外上下文（如从header获取的currentUserId）
- Dubbo接口入参应该最简化
- 避免facade层污染底层接口契约

**示例**：
```java
// Dubbo接口
interface PostService {
    PostDTO createPost(Long userId, String title, String content, PostType type, String imageUrls, String videoUrl);
}

// Facade层入参（可包含更多字段）
class CreatePostFacadeRequest {
    String title;
    String content;
    PostType type;
    List<String> imageUrls;
    String videoUrl;
    // 注意：userId从header获取，不在request中
}
```

## 风险 / 权衡

| 风险 | 缓解措施 |
|------|----------|
| Facade 层调用链变长，性能下降 |Facade 可以并行调用独立服务（如 enrichment） |
| 多个 Dubbo 调用增加失败概率 | Facade 层需要做好异常处理和重试 |
| NotificationService 接口签名变化 | 同步更新调用方 |

## Migration Plan

1. **Phase 1**: 改造 DTO 边界（同时进行）
   - 修改 common.dto.PostDTO，移除 username, userAvatar, isLiked, mediaFiles
   - 修改 common.dto.CommentDTO，移除 username, userAvatar
   - 修改 common.dto.UserRelationDTO，移除 username, avatar
   - 创建 facade-service/src/main/java/com/social/facade/dto/ 目录
   - 创建 PostFacadeResponse, CommentFacadeResponse, UserRelationFacadeResponse 等

2. **Phase 2**: 改造 NotificationService
   - 修改 sendNotification 接口，增加 actorUsername、actorAvatar、isKol 参数
   - 移除 @DubboReference UserService

3. **Phase 3**: 改造 InteractionService
   - 移除 @DubboReference PostService、UserService、NotificationService
   - 简化后的方法只做纯粹的点赞/评论 CRUD

4. **Phase 4**: 改造 RelationService
   - 移除 @DubboReference UserService、NotificationService
   - 简化后的方法只做纯粹的关注关系 CRUD

5. **Phase 5**: 改造 Facade 层
   - 修改 InteractionFacadeController，添加编排逻辑 + enrichment
   - 修改 RelationFacadeController，添加编排逻辑 + enrichment
   - 修改 NotificationFacadeController，添加 KOL 判断和 enrichment
   - 修改 PostFacadeController，使用新的FacadeResponse DTO

6. **Phase 6**: 测试验证
   - 单元测试验证各服务独立工作
   - 集成测试验证 Facade 编排正确
   - 验证API返回数据完整（username, avatar等）

## Open Questions

1. **评论列表 enrichment**：InteractionService.getComments() 当前会 enrich 用户信息。是否在 Facade 层做？
   - 当前提案：在 Facade 层做，InteractionService 返回纯数据

2. **是否需要新增 Dubbo 接口方法**：如 PostService.getPostOwnerId()
   - 当前方案：Facade 通过 getUserIdByPostId() 获取，InteractionService 保留此方法但 facade 不调用

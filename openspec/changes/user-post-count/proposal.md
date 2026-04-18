## 为什么

用户主页需要展示动态数量（postsCount），但目前：
1. UserDTO 没有 postsCount 字段
2. 没有任何 API 返回用户的动态数量
3. followingCount 和 followerCount 已通过 relation-count 表解决了

## 变更内容

1. **新增 `user_post_count` 表**：类似 `user_relation_count`，存储用户的动态数量
2. **PostService 新增方法**：`getPostCount(userId)` 和 `incrementPostCount(userId)`, `decrementPostCount(userId)`
3. **RelationFacadeController 聚合返回**：在 `/relations/counts/{userId}` 中同时返回 followingCount、followerCount、postsCount
4. **前端调用统一**：`UserProfilePage.vue` 调用 `getRelationCounts()` 即可获取三个数量

## 设计决策

**为什么用独立表而非在 UserDTO 添加字段？**
- 遵循现有架构：关系数量用独立表，动态数量也用独立表
- 避免 UserService 和 PostService 之间的跨服务数据依赖
- facade 层负责聚合，体现 facade 的编排职责

**为什么不在 getRelationCounts 返回值中添加 postsCount？**
- getRelationCounts 语义是"关系数量"
- 聚合在 facade 层，controller 可以返回包含所有用户维度数量的统一响应

## 功能 (Capabilities)

### 新增功能
- `user-post-count-table`: 新增 user_post_count 表和对应 Repository
- `post-service-post-count`: PostService 新增 getPostCount、incrementPostCount、decrementPostCount
- `facade-relation-counts-aggregation`: RelationFacadeController 返回聚合的 counts（包含 postsCount）

### 修改功能
- 无

## 影响

**受影响的文件：**

| 层级 | 文件 | 变更 |
|------|------|------|
| 数据库 | post-service | 新增 `user_post_count` 表 |
| 公共接口 | `common/src/main/java/com/social/common/api/PostService.java` | 新增方法 |
| 后端实现 | `post-service/.../PostServiceImpl.java` | 实现数量读写 |
| Facade | `facade-service/.../RelationFacadeController.java` | counts 接口增加 postsCount |
| 前端 | `frontend/src/api/relation.ts` | 可选：统一数量获取 |

**不影响的文件：**
- common/src/main/java/com/social/common/dto/UserDTO.java - 保持不变
- relation-service - 保持不变
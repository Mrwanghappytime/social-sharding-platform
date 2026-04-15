## 为什么

前端关注/粉丝功能存在多个bug：粉丝列表页无法正确获取指定用户的关注列表（API端点错误），列表没有分页，是否关注的查询效率低（逐个查询），数量每次实时 COUNT(*) 计算没有缓存。

## 变更内容

1. **修复前端API调用路径**：修改 `getFollowing` 和 `getFollowers` 使用正确的后端API路径 `/{userId}` 而非 query参数
2. **列表分页**：后端接口支持 `page` 和 `size` 参数
3. **批量查询是否关注**：新增 `areFollowing(currentUserId, targetUserIds)` 方法，一次查询返回当前用户对列表中每个用户是否关注
4. **数量表**：添加 `user_relation_count` 表存储关注/粉丝数量（反范式化），follow/unfollow 时更新，读取时直接查表

## 功能 (Capabilities)

### 新增功能
- `follow-list-api-fix`: 修复关注/粉丝列表API调用路径问题，支持分页和批量关注状态查询
- `relation-count-table`: 添加数量表，反范式化存储关注/粉丝数量

### 修改功能
- 无

## 影响

**受影响的文件：**

| 层级 | 文件 | 变更 |
|------|------|------|
| 数据库 | relation-service | 新增 `user_relation_count` 表 |
| 公共接口 | `common/src/main/java/com/social/common/api/RelationService.java` | 新增方法 |
| 后端实现 | `relation-service/.../RelationServiceImpl.java` | 实现分页、批量查询、数量表读写 |
| Facade | `facade-service/.../RelationFacadeController.java` | 调用新方法 |
| Facade DTO | `facade-service/.../UserRelationFacadeResponse.java` | 新增字段 |
| 前端API | `frontend/src/api/relation.ts` | 修复路径参数 |

**不影响的文件：**
- `common/src/main/java/com/social/common/dto/UserDTO.java` - 不添加数量字段
- user-service - 保持不变

## 上下文

前端关注/粉丝功能存在问题：
1. 粉丝列表页调用 API 时使用 query 参数 `userId`，但后端该端点使用 `X-User-Id` header 获取当前登录用户
2. 列表没有分页，返回所有数据
3. 前端需要逐个查询是否关注（isFollowing），效率低
4. 数量每次通过 COUNT(*) 计算，没有缓存

**重要**：关注/粉丝数据在 relation-service，不在 user 表。

## 目标 / 非目标

**目标：**
- 修复前端 API 调用路径
- 列表接口支持分页
- 批量查询当前用户对列表中每个用户的是否关注状态
- 添加数量表存储关注/粉丝数量（反范式化）

**非目标：**
- 不修改 UserDTO
- 不修改 user-service

## 决策

### 决策 1: 前端 API 调用路径修复

| 文件 | 修改前 | 修改后 |
|------|--------|--------|
| `frontend/src/api/relation.ts` | `get('/relations/following', { params: { userId } })` | `get('/relations/following/${userId}')` |
| `frontend/src/api/relation.ts` | `get('/relations/followers', { params: { userId } })` | `get('/relations/followers/${userId}')` |

### 决策 2: 列表分页

```java
// RelationService 接口变更
PageResult<UserRelationDTO> getFollowingList(Long userId, int page, int size);
PageResult<UserRelationDTO> getFollowersList(Long userId, int page, int size);
```

前端传入 `page` 和 `size` 参数，后端返回分页结果。

### 决策 3: 批量查询是否关注

```java
// RelationService 新增方法
Map<Long, Boolean> areFollowing(Long currentUserId, List<Long> targetUserIds);
```

返回 Map：key 是 targetUserId，value 是是否关注。facade 调用一次即可获取整页的关注状态。

### 决策 4: 数量表

新增 `user_relation_count` 表：

```sql
CREATE TABLE user_relation_count (
    user_id BIGINT PRIMARY KEY,
    following_count BIGINT DEFAULT 0,
    follower_count BIGINT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**更新逻辑**：
- `follow()` 成功后：目标用户 `follower_count + 1`，当前用户 `following_count + 1`
- `unfollow()` 成功后：目标用户 `follower_count - 1`，当前用户 `following_count - 1`

**读取逻辑**：
- `getRelationCounts(userId)` 直接从 `user_relation_count` 读取，不再 COUNT(*)

### 决策 5: UserRelationFacadeResponse 字段

```java
@Data
public class UserRelationFacadeResponse implements Serializable {
    private Long userId;
    private String username;
    private String avatar;
    private Long followingCount;   // 来自数量表
    private Long followersCount;   // 来自数量表
    private Boolean isFollowing;   // 当前用户是否关注此人（批量查询结果）
}
```

## 数据库变更

### relation-service 数据库新增表

```sql
CREATE TABLE user_relation_count (
    user_id BIGINT PRIMARY KEY,
    following_count BIGINT DEFAULT 0,
    follower_count BIGINT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 风险 / 权衡

- [风险] 数量表与实际关系表可能不一致
- [缓解] follow/unfollow 操作在同一个事务中更新数量表
- [风险] 分页接口变更影响前端
- [缓解] 前端已有分页参数支持，只需修改后端实现

## Open Questions

- 无

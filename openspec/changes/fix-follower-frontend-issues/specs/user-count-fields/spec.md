## 新增需求

### 需求:UserRelationFacadeResponse必须包含关注数量、粉丝数量和关注状态字段

`UserRelationFacadeResponse` 必须包含 `followingCount`、`followersCount` 和 `isFollowing` 字段，用于在列表页展示每个用户的社交统计和当前用户的关注状态。

#### 场景:获取关注列表时返回每个用户的数量和关注状态
- **当** 调用 `GET /relations/following/{userId}` 获取关注列表
- **那么** 列表中每个用户对象包含 `followingCount`、`followersCount` 和 `isFollowing` 字段

#### 场景:获取粉丝列表时返回每个用户的数量和关注状态
- **当** 调用 `GET /relations/followers/{userId}` 获取粉丝列表
- **那么** 列表中每个用户对象包含 `followingCount`、`followersCount` 和 `isFollowing` 字段

### 需求:数量必须从数量表读取而非实时COUNT(*)

为了性能，数量必须存储在反范式化的 `user_relation_count` 表中，读取时直接从表查询，不进行 COUNT(*) 计算。

#### 场景:查询用户数量
- **当** 调用 `GET /relations/counts/{userId}`
- **那么** 返回的 `followingCount` 和 `followerCount` 从 `user_relation_count` 表读取

### 需求:关注/取消关注时必须更新数量表

follow 和 unfollow 操作成功后，必须在同一个事务中更新 `user_relation_count` 表。

#### 场景:关注成功后更新数量
- **当** 用户 A 关注用户 B
- **那么** 事务中同时执行：用户 A 的 `following_count + 1`，用户 B 的 `follower_count + 1`

#### 场景:取消关注成功后更新数量
- **当** 用户 A 取消关注用户 B
- **那么** 事务中同时执行：用户 A 的 `following_count - 1`，用户 B 的 `follower_count - 1`

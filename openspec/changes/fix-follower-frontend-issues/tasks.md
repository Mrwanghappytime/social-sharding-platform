## 1. 前端 API 修复

- [x] 1.1 修复 `frontend/src/api/relation.ts` 中 `getFollowing` 使用路径参数 `/relations/following/${userId}`
- [x] 1.2 修复 `frontend/src/api/relation.ts` 中 `getFollowers` 使用路径参数 `/relations/followers/${userId}`

## 2. 数据库变更

- [x] 2.1 在 relation-service 数据库创建 `user_relation_count` 表

## 3. RelationService 接口变更

- [x] 3.1 `RelationService` 接口新增 `getFollowingListPaged(userId, page, size)` 方法
- [x] 3.2 `RelationService` 接口新增 `getFollowersListPaged(userId, page, size)` 方法
- [x] 3.3 `RelationService` 接口新增 `areFollowing(currentUserId, targetUserIds)` 批量查询方法

## 4. RelationServiceImpl 实现

- [x] 4.1 实现 `getFollowingListPaged` - 分页查询
- [x] 4.2 实现 `getFollowersListPaged` - 分页查询
- [x] 4.3 实现 `areFollowing` - 批量查询返回 `Map<Long, Boolean>`
- [x] 4.4 修改 `getRelationCounts` - 从 `user_relation_count` 表读取而非 COUNT(*)
- [x] 4.5 修改 `follow()` - 事务中更新 `user_relation_count` 表
- [x] 4.6 修改 `unfollow()` - 事务中更新 `user_relation_count` 表

## 5. RelationFacadeController 变更

- [x] 5.1 修改 `getFollowingList` 接口 - 调用分页方法 + 批量 isFollowing
- [x] 5.2 修改 `getFollowersList` 接口 - 调用分页方法 + 批量 isFollowing

## 6. UserRelationFacadeResponse 增强

- [x] 6.1 添加 `followingCount`、`followersCount` 和 `isFollowing` 字段
- [x] 6.2 修改 `enrichRelationList` - 填充数量和关注状态

## 7. 测试验证

- [x] 7.1 启动后端所有服务
- [ ] 7.2 启动前端开发服务器 (npm run dev)
- [ ] 7.3 登录用户 A，进入用户 B 的主页
- [ ] 7.4 点击"关注"按钮，验证粉丝数量 +1
- [ ] 7.5 刷新页面，验证关注状态保持正确
- [ ] 7.6 进入粉丝列表，验证分页正常
- [ ] 7.7 进入关注列表，验证列表正确且关注状态显示正确

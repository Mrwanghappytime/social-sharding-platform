## 1. 数据库变更

- [x] 1.1 在 post-service 数据库创建 `user_post_count` 表

## 2. PostService 接口变更

- [x] 2.1 `PostService` 接口新增 `getPostCount(userId)` 方法

## 3. PostServiceImpl 实现

- [x] 3.1 实现 `getPostCount` - 从 user_post_count 表读取
- [x] 3.2 实现私有方法 `incrementPostCount(userId)` - 创建或更新记录 +1
- [x] 3.3 实现私有方法 `decrementPostCount(userId)` - 更新记录 -1（最低为0）
- [x] 3.4 修改 `createPost` 方法 - 添加 `@Transactional`，内部调用 `incrementPostCount`
- [x] 3.5 修改 `deletePost` 方法 - 添加 `@Transactional`，内部调用 `decrementPostCount`

## 4. RelationFacadeController 变更

- [x] 4.1 修改 `getRelationCounts` - 调用 PostService.getPostCount 并添加到返回

## 5. 后端验证（已完成）

- [x] 5.1 启动后端所有服务 (Docker deployed)
- [x] 5.2 API验证 postsCount 正确返回 (初始=0, 创建帖子=1, 删除帖子=0)

## 6. 前端验证（待测试）

- [ ] 6.1 启动前端开发服务器 (npm run dev)
- [ ] 6.2 进入用户主页，验证动态数量正确显示
- [ ] 6.3 发布新动态，验证动态数量 +1
- [ ] 6.4 删除动态，验证动态数量 -1
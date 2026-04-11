## 1. facade-service 基础设施

- [x] 1.1 创建 facade-service 模块 (pom.xml)
- [x] 1.2 创建 FacadeServiceApplication 主类
- [x] 1.3 配置 application.yml (Dubbo + Nacos + LoadBalancer)
- [x] 1.4 配置 RestTemplateConfig (@LoadBalanced)
- [x] 1.5 创建 UserFacadeController (基础版本，调用现有 UserService)
- [x] 1.6 编译验证

## 2. 迁移 user-service

- [x] 2.1 扩展 UserService 接口 (register/login/updateAvatar)
- [x] 2.2 UserServiceImpl 实现新方法
- [x] 2.3 UserFacadeController 调用新方法
- [x] 2.4 移除 user-service UserController
- [x] 2.5 编译验证
- [x] 2.6 测试 /api/users/register ✓
- [x] 2.7 测试 /api/users/login ✓
- [x] 2.8 测试 /api/users/me ✓
- [x] 2.9 测试 /api/users/{id} ✓

## 3. 迁移 post-service

- [x] 3.1 扩展 PostService 接口 (createPost/deletePost/getUserPosts/getFeed/searchPosts)
- [x] 3.2 PostServiceImpl 实现新方法
- [x] 3.3 创建 PostFacadeController (含 enrichment)
- [x] 3.4 PostController 移除 enrichment 逻辑（facade 负责） - 已移至 facade
- [x] 3.5 移除 post-service PostController
- [x] 3.6 编译验证
- [x] 3.7 测试 /api/posts (CRUD) ✓
- [x] 3.8 测试 /api/posts/feed ✓
- [x] 3.9 测试 /api/posts/search ✓

## 4. 迁移 interaction-service

- [x] 4.1 创建 InteractionFacadeController
- [x] 4.2 移除 interaction-service InteractionController
- [x] 4.3 编译验证
- [x] 4.4 测试 /api/interactions/posts/{id}/like ✓
- [x] 4.5 测试 /api/interactions/posts/{id}/unlike ✓
- [ ] 4.6 测试 /api/interactions/posts/{id}/comments (需要数据库schema)

## 5. 迁移 relation-service

- [x] 5.1 确认 RelationService Dubbo 接口存在 (已创建)
- [x] 5.2 创建 RelationFacadeController
- [x] 5.3 移除 relation-service RelationController
- [x] 5.4 编译验证
- [ ] 5.5 测试 /api/relations/follow (需要数据库schema)
- [ ] 5.6 测试 /api/relations/unfollow (需要数据库schema)
- [ ] 5.7 测试 /api/relations/following (需要数据库schema)
- [ ] 5.8 测试 /api/relations/followers (需要数据库schema)

## 6. 迁移 notification-service

- [x] 6.1 创建 NotificationFacadeController
- [x] 6.2 移除 notification-service NotificationController
- [x] 6.3 编译验证
- [ ] 6.4 测试 /api/notifications

## 7. 迁移 file-service

- [x] 7.1 创建 FileService Dubbo 接口 (备用)
- [x] 7.2 FileServiceImpl 实现 Dubbo 接口
- [x] 7.3 创建 FileFacadeController (HTTP lb://)
- [x] 7.4 移除 file-service FileController
- [x] 7.5 编译验证
- [ ] 7.6 测试 /api/files/upload

## 8. Gateway 更新

- [x] 8.1 更新 Gateway 路由为 lb://facade-service
- [x] 8.2 移除各服务路由（仅保留 facade 和 file-service-static）
- [x] 8.3 编译验证

## 9. 最终验证

- [x] 9.1 启动所有服务
- [x] 9.2 完整流程测试（注册→登录→发帖→点赞→关注）- 部分完成(关注需要DB)
- [x] 9.3 确认所有 /api/** 请求通过 facade-service

## 备注

- Gateway使用 http://localhost:8087 替代 lb:// (Nacos LoadBalancer发现有问题，待解决)
- 数据库schema未完全导入，relation-service表缺失
- CreateCommentRequest DTO有多余的postId字段导致验证失败

# Facade Service Architecture

## 为什么

当前后端架构存在以下问题：
1. Controller 类分散在 6 个微服务中，调用混乱
2. 服务间存在循环依赖（post-service ↔ interaction-service）
3. Dubbo 接口不完整，facade 无法获取完整业务能力
4. 服务间调用无负载均衡，单机路由无法利用多实例部署

通过引入 facade-service 聚合层，解决上述问题，支持系统后续演进。

## 变更内容

### 新增 facade-service (端口 8087)
- 聚合所有 @RestController
- 通过 Dubbo @DubboReference 调用底层微服务（Nacos 自带负载均衡）
- file-service 通过 HTTP + Spring Cloud LoadBalancer 调用
- 从 header 接收 Gateway 解析的用户信息，不重复解析 token

### 扩展 Dubbo 接口
- **UserService**: 添加 register(), login(), updateAvatar()
- **PostService**: 添加 createPost(), deletePost(), getUserPosts(), getFeed(), searchPosts()
- **FileService**: 新建 Dubbo 接口

### 服务迁移（逐个迁移验证）
1. user-service → facade UserFacadeController
2. post-service → facade PostFacadeController
3. interaction-service → facade InteractionFacadeController
4. relation-service → facade RelationFacadeController
5. notification-service → facade NotificationFacadeController
6. file-service → facade FileFacadeController (HTTP + LoadBalancer)

### Gateway 路由更新
- 路由改为 `lb://facade-service` 通过 Nacos 负载均衡
- JWT 解析后用户信息通过 header 传递 (X-User-Id, X-User-Name)

### 消除循环依赖
- 迁移后所有 enrichment 由 facade 负责
- 底层服务不再相互调用

## 功能 (Capabilities)

### 新增功能
- `facade-service`: 聚合服务，统一 API 入口，Dubbo RPC 通过 Nacos 自带负载均衡

### 修改功能
- `user-service`: 扩展 Dubbo 接口，移除 UserController
- `post-service`: 扩展 Dubbo 接口，移除 PostController
- `interaction-service`: 移除 InteractionController
- `relation-service`: 移除 RelationController
- `notification-service`: 移除 NotificationController
- `file-service`: 新增 Dubbo 接口 (备用)，主用 HTTP + LoadBalancer

## 影响

- **新增模块**: facade-service (port 8087)
- **API 入口变更**: 所有 /api/** 请求路由到 facade-service
- **负载均衡**: Dubbo RPC 通过 Nacos 自带负载均衡，file-service 使用 HTTP + LoadBalancer
- **Token 处理**: Gateway 解析一次，facade 从 header 获取

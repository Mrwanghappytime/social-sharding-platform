# Facade Service

## 新增需求

### 需求:facade-service 必须作为统一 API 入口

facade-service 必须聚合所有 HTTP API，所有 /api/** 请求必须路由到 facade-service。底层微服务必须禁止直接暴露 @RestController。

#### 场景:用户注册
- **当** 客户端 POST /api/users/register
- **那么** Gateway 路由到 facade-service，facade 调用 UserService.register()

#### 场景:用户登录
- **当** 客户端 POST /api/users/login
- **那么** Gateway 路由到 facade-service，facade 调用 UserService.login()

#### 场景:获取当前用户
- **当** 客户端 GET /api/users/me 带 X-User-Id header
- **那么** facade 从 header 获取用户 ID，调用 UserService.getUserById()

### 需求:facade-service 必须通过 Dubbo RPC 调用底层服务

facade 调用各微服务使用 `@DubboReference`，Dubbo RPC 通过 Nacos 注册中心自带负载均衡。

#### 场景:调用用户服务
- **当** facade 需要获取用户信息
- **那么** 使用 `@DubboReference private UserService userService` 调用

#### 场景:调用文件服务
- **当** facade 需要上传文件
- **那么** 使用 RestTemplate + `@LoadBalanced` 调用 `http://file-service/files/upload`

### 需求:facade-service 必须从 header 获取用户信息

Gateway 解析 JWT 后，用户信息通过 header 传递。facade 禁止重复解析 token。

#### 场景:需要用户 ID 的接口
- **当** 接口需要用户 ID（如发帖、点赞）
- **那么** facade 从 X-User-Id header 获取，不解析 token

#### 场景:可选用户 ID 的接口
- **当** 接口可选择接受用户 ID（如获取帖子列表）
- **那么** facade 检查 X-User-Id header 是否存在，有则传递

### 需求:facade-service 必须负责 enrichment

所有数据的 enrichment（用户信息、点赞状态等）必须由 facade 负责，底层服务禁止相互调用。

#### 场景:获取帖子列表并 enrichment
- **当** 客户端 GET /api/posts/feed
- **那么** facade 调用 PostService 获取列表，调用 UserService 获取用户信息，调用 InteractionService 获取点赞状态

#### 场景:创建帖子
- **当** 客户端 POST /api/posts
- **那么** facade 调用 PostService.createPost()，然后调用 UserService 和 InteractionService 做 enrichment

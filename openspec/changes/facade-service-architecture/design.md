# Facade Service Architecture Design

## 上下文

**当前状态:**
- Gateway (8080) 路由到各微服务，使用 `http://localhost:xxxx` 硬编码
- 各微服务 Controller 分散: user/post/interaction/relation/notification/file-service
- Dubbo 接口不完整，缺少 register/login/createPost 等核心方法
- post-service ↔ interaction-service 存在循环依赖
- 服务间调用无负载均衡

**约束:**
- 逐个服务迁移，每迁移一个验证一个
- Gateway 只做 JWT 解析，facade 不重复解析 token
- Dubbo RPC 通过 Nacos 注册中心自带负载均衡
- file-service 使用 HTTP + Spring Cloud LoadBalancer 调用

## 目标 / 非目标

**目标:**
- facade-service 作为唯一 API 入口
- Dubbo RPC 通过 Nacos 注册中心自带负载均衡（支持多实例部署）
- facade 从 header 获取用户信息，不解析 token
- 消除服务间循环依赖

**非目标:**
- 不改变底层微服务的业务逻辑
- 不引入消息队列等异步机制
- 不做微服务拆分（服务粒度已定）

## 决策

### 决策 1: facade-service 定位

**选择:** facade-service 仅做 API 聚合和 enrichment，不含业务逻辑

**理由:**
- 业务逻辑在底层服务，facade 只负责编排
- 方便前端对接，统一的 API 入口
- 后续可在此层做认证、限流等横切关注点

### 决策 2: Dubbo RPC 自带负载均衡

**选择:** facade 调用各服务使用 `@DubboReference`，通过 Nacos 注册中心实现负载均衡

**理由:**
- Dubbo 协议本身支持负载均衡（random/round-robin/least-active）
- Nacos 作为注册中心，自动发现多实例
- 配置简化，无需硬编码地址
- 故障自动剔除，提高可用性

### 决策 3: file-service HTTP + LoadBalancer 调用

**选择:** facade 通过 RestTemplate + `@LoadBalanced` 调用 file-service:8086

**理由:**
- 文件上传不适合 Dubbo RPC（大文件二进制序列化效率低）
- HTTP 是最合适的大文件传输协议
- Spring Cloud LoadBalancer + Nacos 实现负载均衡

**实现方式:**
```java
@Configuration
public class RestTemplateConfig {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

// facade 调用
restTemplate.postForObject("http://file-service/files/upload", ...)
```

### 决策 4: 用户信息传递

**选择:** Gateway 解析 JWT，用户信息通过 header 传递

**Header 字段:**
- `X-User-Id`: 用户 ID
- `X-User-Name`: 用户名

**理由:**
- Gateway 已经做了 JWT 解析，facade 只需从 header 获取
- 减少重复解析开销
- facade 无需配置 JWT secret

### 决策 5: 逐个服务迁移

**迁移顺序:** user → post → interaction → relation → notification → file

**理由:**
- user-service 最简单，先验证流程
- post-service 涉及 enrichment，最后处理
- 每迁移一个，验证一个，降低风险

### 决策 6: 消除循环依赖

**现状:** post-service 调用 interactionService.getLikeStatus()，interaction-service 调用 postService.isPostExists()

**解决方案:** facade 负责 enrichment
- facade 调用 PostService.getPostById() 获取帖子
- facade 调用 InteractionService.getLikeStatus() 获取点赞状态
- 底层服务不再相互调用

## 风险 / 权衡

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| facade 成为单点 | 所有请求经过 facade | facade 无状态，可水平扩展 |
| 多一次网络跳 | 延迟增加 | Nacos 同机房调用，延迟<1ms |
| 服务启动顺序 | facade 需要底层服务先启动 | 使用 Nacos 健康检查 + 重试 |
| 迁移过程 API 不可用 | 切换时短暂不可用 | 蓝绿部署或滚动更新 |

## Migration Plan

### Phase 1: facade-service 基础设施
1. 创建 facade-service 模块
2. 配置 Nacos + Dubbo（@DubboReference 自动负载均衡）
3. 配置 Spring Cloud LoadBalancer（file-service HTTP 调用）
4. 创建 UserFacadeController（调用现有 UserService Dubbo）
5. 验证 /api/users/** 能正常访问

### Phase 2: 扩展 UserService + 迁移
1. 扩展 UserService 接口 (register/login/updateAvatar)
2. UserFacadeController 调用新方法
3. 移除 user-service UserController
4. 验证完整用户流程

### Phase 3-7: 其他服务迁移
类似 Phase 2，逐个服务扩展 Dubbo 接口、创建 Facade Controller、验证、移除原 Controller

## Open Questions

1. **file-service 多实例**: 是否真的需要多实例部署？静态文件服务通常 1-2 个实例足够
2. **监控追踪**: facade 层引入后，如何追踪请求链路？
3. **熔断降级**: facade 调用失败时的降级策略？

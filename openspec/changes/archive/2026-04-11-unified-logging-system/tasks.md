## 1. 通用组件开发

- [x] 1.1 在 common 模块创建 LogUtil 统一日志消息构建工具
- [x] 1.2 在 common 模块创建 DubboTraceFilter（从 RpcContext 获取 traceId → MDC）
- [x] 1.3 验证 DubboTraceFilter 在 common 模块可被各服务复用

## 2. Logback 日志格式配置（含日志轮转）

- [x] 2.1 创建通用 logback-base.xml 配置（日志格式 pattern + 轮转：单文件最大30MB，最多保存500MB）
- [x] 2.2 在 gateway 配置 Logback（日志格式 + 轮转）
- [x] 2.3 在 facade-service 配置 Logback（日志格式 + 轮转）
- [x] 2.4 在 user-service 配置 Logback（日志格式 + 轮转）
- [x] 2.5 在 post-service 配置 Logback（日志格式 + 轮转）
- [x] 2.6 在 interaction-service 配置 Logback（日志格式 + 轮转）
- [x] 2.7 在 relation-service 配置 Logback（日志格式 + 轮转）
- [x] 2.8 在 notification-service 配置 Logback（日志格式 + 轮转）
- [x] 2.9 在 file-service 配置 Logback（日志格式 + 轮转）

## 3. Facade Service 特定组件

- [x] 3.1 实现 TraceIdFilter（从 HTTP Header 获取 traceId → MDC）
- [x] 3.2 实现 ControllerAop（打印入参/出参）
- [x] 3.3 在 Dubbo 调用时传递 traceId 到 RpcContext（在 Facade 层）

## 4. 各服务 Dubbo 接口日志

- [x] 4.1 user-service: UserServiceImpl 添加入口/出口日志
- [x] 4.2 post-service: PostServiceImpl 添加入口/出口日志
- [x] 4.3 interaction-service: InteractionServiceImpl 添加入口/出口日志
- [x] 4.4 relation-service: RelationServiceImpl 添加入口/出口日志
- [x] 4.5 notification-service: NotificationServiceImpl 添加入口/出口日志

## 5. 关键业务日志

- [x] 5.1 user-service: 关键业务点添加日志（注册、登录、修改头像）
- [x] 5.2 post-service: 关键业务点添加日志（创建、删除帖子）
- [x] 5.3 interaction-service: 关键业务点添加日志（点赞、评论）
- [x] 5.4 relation-service: 关键业务点添加日志（关注、取关）
- [x] 5.5 notification-service: 关键业务点添加日志（发送通知）

## 6. 测试验证

- [x] 6.1 启动所有服务，验证日志格式正确
- [x] 6.2 测试 traceId 从 HTTP → Facade → Dubbo 传播链
- [x] 6.3 测试 Controller AOP 日志记录
- [x] 6.4 验证各服务关键日志输出

## 上下文

当前大部分微服务缺少日志记录，问题定位困难。需要实现统一的日志追踪体系。

## 目标 / 非目标

**目标：**
- 统一日志格式，便于解析和搜索
- 实现全链路 traceId 追踪
- Controller 层通过 AOP 自动记录入参/出参
- Dubbo 接口手动记录入口/出口日志
- 关键业务点（数据库操作、外部调用）添加日志

**非目标：**
- 不实现分布式追踪系统（如 Zipkin）
- 不改变现有业务逻辑

## 决策

### 1. 日志格式

```
[serviceName[IP:PORT]][timestamp][traceId][threadId][level] message (class:method:line)
```

**日志轮转配置：**
- 单文件最大 30MB
- 最多保存 500MB 日志（约 17 个历史文件）
- 使用 SizeBasedTriggeringPolicy + FixedWindowRollingPolicy
- **日志目录可配置**：通过 application.yaml 的 `logging.log-path` 配置
- **Linux 兼容**：Docker 环境使用绝对路径 `/var/log/{service-name}/`

```
[serviceName[IP:PORT]][timestamp][traceId][threadId][level] message (class:method:line)
```

示例：
```
[facade-service[192.168.1.1:8080]][2026-04-11 16:30:00.123][abc123][http-nio-8080-exec-1][INFO] User login attempt (UserFacadeController.login:28)
```

**字段说明：**

| 字段 | 说明 | 来源 |
|------|------|------|
| serviceName | 服务名称 | 静态配置 |
| IP:PORT | 服务地址 | 静态配置 |
| timestamp | 时间戳 | Logback |
| traceId | 调用链追踪ID | MDC |
| threadId | 线程ID | Logback |
| level | 日志级别 | Logback |
| message | 业务消息 | 手动 |
| location | 代码位置 | Logback |

### 2. TraceId 生成与传播

```
HTTP Request
    │
    ▼
Gateway: 生成 traceId → 放入 HTTP Header "X-Trace-Id"
    │
    ▼
Facade Service:
    - TraceIdFilter: 从 HTTP Header 获取 traceId → 存入 MDC
    - Dubbo 调用: 将 traceId 从 MDC → 放入 RpcContext
    │
    ▼
各微服务:
    - DubboTraceFilter: 从 RpcContext 获取 traceId → 存入 MDC
```

### 3. 组件实现

| 组件 | 位置 | 职责 |
|------|------|------|
| TraceIdFilter | facade-service | HTTP Filter: Header → MDC |
| DubboTraceFilter | common | Dubbo Filter: RpcContext ↔ MDC |
| ControllerAop | facade-service | 切面打印 Controller 入参/出参 |
| LogbackConfig | 各服务 | 统一日志格式配置 |
| LogUtil | common | 统一日志消息构建 |

### 4. 日志级别策略

| 场景 | 级别 |
|------|------|
| 接口入口/出口 | INFO |
| 业务操作成功 | INFO |
| 数据库操作 | DEBUG |
| 外部服务调用 | DEBUG |
| 业务异常 | WARN |
| 系统异常 | ERROR |

### 5. 服务范围

| 服务 | 组件 |
|------|------|
| gateway | LogbackConfig |
| facade-service | TraceIdFilter, ControllerAop, LogbackConfig, DubboTraceFilter |
| user-service | DubboTraceFilter, LogbackConfig |
| post-service | DubboTraceFilter, LogbackConfig |
| interaction-service | DubboTraceFilter, LogbackConfig |
| relation-service | DubboTraceFilter, LogbackConfig |
| notification-service | DubboTraceFilter, LogbackConfig |
| file-service | DubboTraceFilter, LogbackConfig |

### 6. 日志目录配置

**application.yaml 配置项：**
```yaml
logging:
  log-path: /var/log/social-platform/${spring.application.name}
  service-name: ${spring.application.name}
```

| 环境 | 日志路径示例 |
|------|-------------|
| 开发 (Windows) | `logs/${service-name}` |
| 生产 (Linux/Docker) | `/var/log/social-platform/${service-name}` |

## 风险 / 权衡

| 风险 | 缓解 |
|------|------|
| 日志量过大 | 只记录必要信息，避免记录大对象 |
| 性能影响 | Dubbo接口日志使用DEBUG级别，生产可关闭 |
| traceId丢失 | RpcContext传递失败时生成新ID |

## 实现步骤

1. 在 common 模块创建 DubboTraceFilter、LogUtil
2. 在各服务配置统一 Logback pattern
3. 在 facade-service 实现 TraceIdFilter 和 ControllerAop
4. 在各 Dubbo 接口方法添加入口/出口日志

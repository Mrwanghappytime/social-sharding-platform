## 为什么

当前服务大部分业务缺少日志记录，调用链路不可追踪，问题定位困难。需要在所有服务中实现统一的日志追踪体系。

## 变更内容

1. **统一日志格式**: 所有服务使用相同的日志格式，包含 traceId、serviceName、IP:PORT、timestamp、threadId、level、message、location
2. **TraceId 生成与传播**: Facade 层生成 TraceId，通过 MDC 存储，Dubbo Filter 透传到下游服务
3. **Controller 日志**: 通过 AOP 统一打印入参/出参
4. **Dubbo 接口日志**: 手动在入口/出口添加日志
5. **关键业务日志**: 数据库操作、外部调用、异常等关键点添加日志

## 功能 (Capabilities)

### 新增功能

- `unified-logging`: 统一日志追踪体系

## 影响

- 需要在 gateway、facade-service、user-service、post-service、interaction-service、relation-service、notification-service、file-service 添加日志配置和组件

## 新增需求

### 需求:统一日志格式

所有服务必须使用统一日志格式，包含 serviceName、IP:PORT、timestamp、traceId、threadId、level、message、location。

日志格式：
```
[serviceName[IP:PORT]][timestamp][traceId][threadId][level] message (class:method:line)
```

#### 场景:日志格式验证
- **当** 服务输出一条日志
- **那么** 日志必须包含所有必需字段

### 需求:TraceId 生成

Facade 服务必须为每个 HTTP 请求生成唯一的 traceId。

#### 场景:生成 TraceId
- **当** HTTP 请求进入 Facade 服务
- **那么** 必须生成 16 位十六进制 traceId 并存储到 MDC

### 需求:TraceId 传播

TraceId 必须通过 Dubbo RpcContext 传播到下游服务。

#### 场景:HTTP 到 Dubbo 传播
- **当** Facade 服务发起 Dubbo 调用
- **那么** 必须将 traceId 从 MDC 放入 RpcContext

#### 场景:Dubbo 到 MDC 传播
- **当** 下游服务接收 Dubbo 请求
- **那么** 必须从 RpcContext 取出 traceId 并存入当前线程 MDC

### 需求:Controller 入参出参日志

Facade 服务的 Controller 接口必须通过 AOP 记录入参和出参。

#### 场景:Controller 方法日志
- **当** Controller 方法被调用
- **那么** 必须打印方法名、入参、返回值

### 需求:Dubbo 接口日志

所有 Dubbo 接口必须手动添加入口和出口日志。

#### 场景:Dubbo 方法日志
- **当** Dubbo 接口方法被调用
- **那么** 必须打印方法名、入参（IN）和返回值（OUT）

### 需求:关键业务日志

关键业务点必须添加详细日志。

#### 场景:数据库操作日志
- **当** 执行 save、update、delete 操作
- **那么** 必须打印操作类型、涉及的实体ID

#### 场景:外部服务调用日志
- **当** 调用其他微服务或外部系统
- **那么** 必须打印调用目标、操作结果

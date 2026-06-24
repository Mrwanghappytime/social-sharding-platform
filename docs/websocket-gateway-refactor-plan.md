# WebSocket Gateway 改造计划

> 创建时间: 2026-06-12
> 目标: 解决前端写死 notification-service IP 的问题，实现通过 Gateway 的 WebSocket 负载均衡

---

## 背景与问题

### 当前架构问题

1. **前端 WebSocket 连接写死后端 IP**
   - 前端: `ws://localhost:8085/ws/notify?token=xxx`
   - 微服务部署后无法做负载均衡和故障转移

2. **notification-service 已部署 2 个实例**
   - `notification-service`: 主机端口 8085, 容器IP 172.17.0.9
   - `notification-service-2`: 主机端口 8185, 容器IP 172.17.0.10
   - 但 Gateway 没有 WebSocket 路由，无法利用多实例

3. **基线探测发现的阻塞性问题**
   - notification-service **没有引入 `spring-cloud-starter-alibaba-nacos-discovery`**
   - 只通过 Dubbo 注册了 RPC 端口 20885
   - HTTP 端口 8085 没有注册到 Nacos
   - 对比 facade-service `pom.xml` 第36-40行有此依赖

---

## 改造方案（最终版）

### 路由架构

```
Frontend (3000)
    │
    │ ws://localhost:8080/ws/notify?token=xxx
    ▼
Gateway (8080)
    │ uri: lb:ws://notification-service
    │ (通过 Nacos 服务发现 + LoadBalancer)
    │
    ├──▶ notification-service   (172.17.0.9:8085)
    └──▶ notification-service-2 (172.17.0.10:8085)
              │
              │ Redis Pub/Sub (channel: notification:channel:{userId})
              ▼
            Redis (host.docker.internal:6379)
```

### 认证策略

- Gateway 验证 JWT，从 query string 提取 token
- Gateway 注入 `X-User-Id` header 后转发到 notification-service
- notification-service 优先信任 Gateway 注入的 header
- 直连 notification-service（绕过 Gateway）回退使用 query token，便于调试

---

## 实施步骤（每步独立验证）

### ✅ 阶段一: 基线建立（已完成）

**已发现的问题:**
- ❌ notification-service 缺少 nacos-discovery 依赖 → 没注册 HTTP 端点
- ❌ Gateway actuator 端点未暴露 → 无法验证路由
- ✅ Spring Cloud Gateway 4.x 内置 WebsocketRoutingFilter 支持 `lb:ws://`
- ✅ Nacos 中 notification-service 有 2 个实例（但都是 Dubbo 端口 20885）

---

### ⬜ 步骤 1: notification-service 注册 HTTP 端点到 Nacos

**改动文件:**
- `notification-service/pom.xml` 添加依赖:
  ```xml
  <dependency>
      <groupId>com.alibaba.cloud</groupId>
      <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
  </dependency>
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-loadbalancer</artifactId>
  </dependency>
  ```
- `notification-service/src/main/resources/application.yml` 添加配置:
  ```yaml
  spring:
    cloud:
      nacos:
        discovery:
          server-addr: localhost:8848
          namespace: public
          group: DEFAULT_GROUP
  ```
- `NotificationServiceApplication.java` 添加 `@EnableDiscoveryClient`（如需要）

**重新打包:** `mvn clean package -pl notification-service -am -DskipTests`

**重新构建镜像:** 仅 notification-service 镜像

**重启容器:**
```bash
docker rm -f notification-service notification-service-2
# 重新启动两个实例
```

**验证:**
```bash
# 1. Nacos 中看到 HTTP 端点
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=notification-service" | jq '.hosts[].port'
# 期望: 看到 8085 端口（不只是 20885）

# 2. 直连两个实例都能正常工作
wscat -c "ws://localhost:8085/ws/notify?token=<JWT>"
wscat -c "ws://localhost:8185/ws/notify?token=<JWT>"
```

---

### ⬜ 步骤 2: Gateway 添加 WebSocket 路由

**改动文件:** `gateway/src/main/resources/application.yml`

**新增内容:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        # 现有路由保留...
        # 新增 WebSocket 路由
        - id: notification-websocket
          uri: lb:ws://notification-service
          predicates:
            - Path=/ws/notify
          filters:
            - StripPrefix=0

# 暴露 actuator 端点用于验证
management:
  endpoints:
    web:
      exposure:
        include: gateway,health,info
```

**重启:** `docker restart gateway`（无需重新打包，仅配置文件变化但已打入镜像，所以需要重新打包）

实际操作:
```bash
mvn clean package -pl gateway -am -DskipTests
docker build -f docker/Dockerfile.gateway -t .../social-platform-gateway:latest gateway
docker rm -f gateway
# 重新 docker run
```

**验证:**
```bash
# 1. 路由已加载
curl http://localhost:8080/actuator/gateway/routes | jq '.[] | select(.route_id=="notification-websocket")'
# 期望: 看到 uri: "lb:ws://notification-service"

# 2. 通过 Gateway 能连接
wscat -c "ws://localhost:8080/ws/notify?token=<JWT>"

# 3. 负载均衡验证（连续连接，观察分布）
for i in {1..10}; do
  wscat -c "ws://localhost:8080/ws/notify?token=<JWT>" -x "ping" -t 2 2>&1 | head -3
done
docker logs notification-service 2>&1 | grep "WebSocket connected" | wc -l
docker logs notification-service-2 2>&1 | grep "WebSocket connected" | wc -l
# 期望: 两个实例大致 5:5 分布
```

---

### ⬜ 步骤 3: JwtAuthenticationFilter 支持 query token

**改动文件:** `gateway/src/main/java/com/social/gateway/filter/JwtAuthenticationFilter.java`

**改动逻辑:**
```java
// 1. 优先从 Authorization header 读取
// 2. fallback 从 query string 读取 token=xxx
// 3. 验证后注入 X-User-Id header
// 4. /ws/notify 路径必须校验 token 后才能转发（安全要求）
```

**测试用例矩阵:**

| 用例 | 输入 | 期望 |
|------|------|------|
| header 携带 token | `Authorization: Bearer xxx` | ✅ 200, 注入 X-User-Id |
| query 携带 token | `?token=xxx` | ✅ 200, 注入 X-User-Id |
| 都没有 | 无 | ❌ 401 |
| token 过期 | 过期 JWT | ❌ 401 |
| token 篡改 | 修改签名 | ❌ 401 |
| WebSocket 握手 | `Upgrade: websocket` + `?token=xxx` | ✅ 转发，X-User-Id 注入 |

**验证脚本:** 写到 `scripts/verify-jwt.sh`

---

### ⬜ 步骤 4: notification-service 信任 Gateway header

**改动文件:** `notification-service/src/main/java/.../NotificationWebSocketHandler.java`

**改动逻辑:**
```java
private Long getUserIdFromSession(WebSocketSession session) {
    // 1. 优先从 X-User-Id header 读取（Gateway 注入）
    String headerUserId = session.getHandshakeHeaders().getFirst("X-User-Id");
    if (headerUserId != null) {
        return Long.parseLong(headerUserId);
    }
    
    // 2. fallback: 从 query string 解析 token（直连场景）
    String token = extractToken(session);
    if (token == null) return null;
    return parseTokenAndGetUserId(token);
}
```

**安全验证:**
- 不能让攻击者直连 8085 伪造 X-User-Id（需要白名单 IP 限制 或 内部签名）
- 当前简化方案: 直连场景仍走 token 验证，header 仅在 Gateway 路由后生效

**验证:**
```bash
# 攻击场景: 直连 + 伪造 header → 应失败
curl -i -H "X-User-Id: 1" \
  -H "Connection: Upgrade" -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: dGVzdA==" \
  http://localhost:8085/ws/notify
# 期望: 拒绝（无 token）
```

---

### ⬜ 步骤 5: 前端改造

**改动文件:**
- `frontend/src/api/websocket.ts` 或类似的 WebSocket 客户端文件

**改动内容:**
```typescript
class NotificationClient {
    private ws: WebSocket | null = null;
    private reconnectDelay = 1000;
    private readonly maxDelay = 30000;
    private readonly token: string;
    private heartbeatTimer: number | null = null;
    
    connect() {
        // 通过 Gateway，使用相对 URL（vite proxy 已配 /ws）
        const wsUrl = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/notify?token=${this.token}`;
        this.ws = new WebSocket(wsUrl);
        
        this.ws.onopen = () => {
            this.reconnectDelay = 1000;  // 重置退避
            this.startHeartbeat();
        };
        
        this.ws.onclose = () => {
            this.stopHeartbeat();
            setTimeout(() => this.connect(), this.reconnectDelay);
            this.reconnectDelay = Math.min(this.reconnectDelay * 2, this.maxDelay);
        };
        
        this.ws.onmessage = (event) => {
            if (event.data === 'pong') return;
            this.handleNotification(JSON.parse(event.data));
        };
    }
    
    startHeartbeat() {
        this.heartbeatTimer = window.setInterval(() => {
            if (this.ws?.readyState === WebSocket.OPEN) {
                this.ws.send('ping');
            }
        }, 30000);
    }
}
```

**vite.config.ts 已有 `/ws` 代理到 8080，所以前端只需用相对路径**

**验证:**
- 浏览器开发者工具 Network → WS 标签查看连接 URL 和帧
- 触发关注/点赞，验证收到通知
- 手动 docker stop notification-service，观察前端自动重连日志

---

### ⬜ 步骤 6: 故障场景验证

**6.1 实例宕机**
```bash
# 关闭其中一个实例
docker stop notification-service-2

# 验证:
# 1. 连接到 -2 上的 WebSocket 断开
# 2. 前端自动重连，路由到 notification-service
# 3. Gateway 健康检查剔除故障实例
# 4. 通知功能仍正常工作

# 恢复:
docker start notification-service-2
# Gateway 自动恢复负载均衡
```

**6.2 Gateway 宕机**
```bash
docker stop gateway

# 验证:
# 1. 所有 WebSocket 断开
# 2. 前端尝试重连失败但不崩溃
# 3. 业务功能不可用（预期）

docker start gateway
# 前端自动重连成功
```

**6.3 Redis 宕机**
```bash
# Redis 当前在主机，需要 stop redis 服务（Windows 服务）
# 验证:
# 1. 已建立的 WebSocket 不断（仅推送失败）
# 2. notification-service 不崩溃
# 3. Redis 恢复后 Pub/Sub 重新工作
```

---

### ⬜ 步骤 7: 回归验证

```bash
# 跑前端 e2e
cd frontend && npm test

# 手动验证关键 API
- POST /api/users/login
- POST /api/posts
- POST /api/relations/follow → 应推送通知
- POST /api/interactions/like → 应推送通知
- POST /api/interactions/comments → 应推送通知
```

---

## 当前服务状态（改造前）

```
nacos                 8848  ✅ Up
gateway               8080  ✅ Up（无 WS 路由）
user-service          8081  ✅ Up
post-service          8082  ✅ Up
interaction-service   8083  ✅ Up
relation-service      8084  ✅ Up
notification-service  8085  ✅ Up（Nacos 仅注册 Dubbo 20885）
notification-service-2 8185 ✅ Up（Nacos 仅注册 Dubbo 20885）
file-service          8086  ✅ Up
facade-service        8087  ✅ Up
frontend              3000  ✅ Up
```

---

## 关键风险与回滚

### 风险点

| 风险 | 应对 |
|------|------|
| `lb:ws://` 在当前 Gateway 版本不工作 | 步骤2 验证失败立即回滚配置 |
| Sticky session 问题（用户重连可能落到不同实例）| 通过 Redis Pub/Sub 路由解决，无需 sticky |
| query string 中 token 可能进入访问日志 | 步骤3 检查 Gateway 日志脱敏 |
| 容器 IP 变化导致 Nacos 注册脏数据 | 重启后 Nacos 30s 内自动剔除 |

### 回滚方案

每一步改动都通过 git 管理，出问题:
```bash
git diff
git checkout -- <修改的文件>
# 重新打包重启对应服务
```

不修改任何已生效的服务（如 facade-service、user-service），改动范围限定在:
- `notification-service` (pom.xml + application.yml + Java)
- `gateway` (application.yml + Java filter)
- `frontend` (websocket client)

---

## 完成标志

- [ ] 前端连接 `ws://localhost:8080/ws/notify` 成功
- [ ] 多次连接均匀分布到 2 个 notification-service 实例
- [ ] 关闭一个实例后，前端自动重连到健康实例（≤30s）
- [ ] 跨实例消息推送正确（用户A在实例1，用户B在实例2，B给A点赞，A收到通知）
- [ ] 现有业务功能无回归
- [ ] 文档更新（CLAUDE.md 中加入 WebSocket 路由说明）

---

## 改造完成后追加任务（不在本次范围）

- 单 channel 改造（解决 N 用户 N channel 问题）
- 客户端 sticky session 优化
- WebSocket 心跳超时主动断开机制
- KOL 用户的 WebSocket 推送策略复审

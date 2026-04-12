## 上下文

当前社交平台使用传统方式部署微服务，存在环境依赖复杂、部署流程繁琐等问题。需要实现 Docker 容器化部署，使各服务能够独立运行在容器中。

**当前架构：**
- 8个微服务 + 1个 common 模块
- 服务：gateway, user-service, post-service, interaction-service, relation-service, notification-service, file-service, facade-service
- 基础设施：MySQL(:3306), Redis(:6379), Nacos(:8848) 部署在宿主机

**部署约束：**
- JDK 镜像：swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/openjdk:21（已准备）
- MySQL/Redis/Nacos 不容器化，继续使用宿主机部署
- 服务需要通过 `--network host` 访问宿主机基础设施

## 目标 / 非目标

**目标：**
- 为每个微服务创建独立 Docker 镜像
- 标准化构建流程，通过脚本一键构建所有镜像
- 运行时通过 `--network host` 访问宿主机 MySQL/Redis/Nacos
- file-service 通过 volume 挂载持久化文件

**非目标：**
- 不容器化 MySQL/Redis/Nacos 等基础设施
- 不使用 docker-compose（用户要求使用 docker run）
- 不实现 WebSocket gateway 路由（暂缓）
- 不实现 Kubernetes 部署

## 决策

### 决策 1：构建流程

**选择：宿主机 Maven 构建 + Dockerfile 打包**

```
宿主机: mvn clean package -DskipTests
          ↓
各服务 target/*.jar 已就绪
          ↓
Dockerfile: COPY target/*.jar app.jar
          ↓
docker build 构建镜像
```

**理由：**
- 避免在 Docker 内重复执行 Maven 构建，加速镜像构建
- 减少镜像体积（不包含 Maven 运行时）
- 构建上下文只需包含 target 目录和源码

### 决策 2：Dockerfile 模板

**通用服务 Dockerfile（7个服务）：**
```dockerfile
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/openjdk:21
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**file-service 专用 Dockerfile：**
```dockerfile
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/openjdk:21
COPY target/*.jar app.jar
# 文件目录已在运行时通过 -v 挂载，无需 Dockerfile 内创建
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**理由：** file-service 的 Dockerfile 内容与通用模板相同，但运行时需要额外挂载。

### 决策 3：镜像命名规范

```
registry: swr.cn-north-4.myhuaweicloud.com/ddn-k8s
项目前缀: social-platform
服务名: {service-name}
标签: latest 或 1.0.0

完整格式: swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-{service}:{tag}
```

### 决策 4：网络模式

**选择：`--network host`**

```bash
docker run --network host social-platform-gateway:latest
docker run --network host social-platform-user:latest
docker run --network host -v /data/files:/data/files social-platform-file:latest
```

**理由：**
- 容器内服务通过 `localhost:3306` 访问宿主机 MySQL
- 容器内服务通过 `localhost:6379` 访问宿主机 Redis
- 容器内服务通过 `localhost:8848` 访问宿主机 Nacos
- gateway 固定端口 8080，其他服务端口随机（端口已在 application.yml 配置）

### 决策 5：文件持久化

**选择：宿主机目录挂载**

```bash
-v /data/files:/data/files
```

**理由：**
- file-service 数据存储在 `/data/files/{type}/{uuid}.{ext}`
- 通过 volume 挂载确保容器重启后文件不丢失
- 宿主机备份管理更方便

## 风险 / 权衡

| 风险 | 缓解措施 |
|------|---------|
| 宿主机端口与容器端口冲突 | gateway 固定 8080，其他服务端口随机绑定 |
| 文件权限问题 | 确保 /data/files 目录权限为 1000:1000 或 777 |
| 多实例部署时 session/缓存问题 | 依赖 Redis，实现无状态服务 |

## 文件结构

```
docker/
├── Dockerfile.base           # 通用基础镜像配置
├── Dockerfile.file-service   # file-service 专用
├── Dockerfile.user-service
├── Dockerfile.post-service
├── Dockerfile.interaction-service
├── Dockerfile.relation-service
├── Dockerfile.notification-service
├── Dockerfile.facade-service
├── Dockerfile.gateway
├── .dockerignore
└── build.sh                  # 构建脚本

scripts/
└── docker-build.sh          # 一键构建所有镜像脚本
```

## 构建命令

```bash
# 1. 打包所有服务
mvn clean package -DskipTests

# 2. 构建所有镜像
./scripts/docker-build.sh

# 3. 运行示例
docker run --network host --name gateway -p 8080:8080 social-platform-gateway:latest
docker run --network host --name user social-platform-user:latest
docker run --network host -v /data/files:/data/files --name file social-platform-file:latest
```

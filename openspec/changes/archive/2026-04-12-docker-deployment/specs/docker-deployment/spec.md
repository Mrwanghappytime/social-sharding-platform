## 新增需求

### 需求:Docker 镜像构建
系统必须支持为所有微服务构建独立的 Docker 镜像。

#### 场景:构建所有服务镜像
- **当** 执行 `mvn clean package -DskipTests` 后运行 `./scripts/docker-build.sh`
- **那么** 所有 8 个服务（gateway, user, post, interaction, relation, notification, file, facade）的镜像均被构建

### 需求:通用服务 Docker 镜像
除 file-service 外的 7 个服务必须使用通用 Dockerfile 构建镜像。

#### 场景:通用 Dockerfile 结构
- **当** 使用通用 Dockerfile 构建服务镜像
- **那么** 镜像基于 openjdk:21，包含单个 JAR 文件作为 ENTRYPOINT

### 需求:file-service 文件挂载
file-service 必须支持通过 volume 挂载持久化文件存储。

#### 场景:file-service 文件目录挂载
- **当** 运行 file-service 容器时使用 `-v /data/files:/data/files` 参数
- **那么** 容器内 `/data/files` 与宿主机 `/data/files` 同步

### 需求:宿主机网络访问
所有容器必须能够通过 `--network host` 访问宿主机上的基础设施服务。

#### 场景:访问宿主机 MySQL
- **当** 容器内应用连接 `localhost:3306`
- **那么** 连接的是宿主机上的 MySQL 服务

#### 场景:访问宿主机 Redis
- **当** 容器内应用连接 `localhost:6379`
- **那么** 连接的是宿主机上的 Redis 服务

#### 场景:访问宿主机 Nacos
- **当** 容器内应用连接 `localhost:8848`
- **那么** 连接的是宿主机上的 Nacos 服务

### 需求:网关固定端口
gateway 服务必须固定使用 8080 端口。

#### 场景:gateway 端口映射
- **当** 运行 gateway 容器时使用 `-p 8080:8080`
- **那么** gateway 服务通过 8080 端口对外提供 HTTP API

### 需求:构建产物清理
Dockerfile 构建完成后必须删除构建产物以减小镜像体积。

#### 场景:构建后清理
- **当** Dockerfile 执行 COPY 指令复制 JAR 文件后
- **那么** 镜像中不包含源代码、target 目录等构建中间产物

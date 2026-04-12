# Social Platform - Docker 部署

## 快速开始

### 1. 构建镜像

```bash
# 在项目根目录执行
./scripts/docker-build.sh
```

### 2. 启动服务

**Linux 环境（推荐使用 host 网络）：**

```bash
# 启动所有服务
docker run --network host --name gateway swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-gateway:latest
docker run --network host --name user-service swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-user-service:latest
docker run --network host --name post-service swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-post-service:latest
docker run --network host --name interaction-service swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-interaction-service:latest
docker run --network host --name relation-service swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-relation-service:latest
docker run --network host --name notification-service swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-notification-service:latest
docker run --network host --name facade-service swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-facade-service:latest
docker run --network host -v /data/files:/data/files --name file-service swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-file-service:latest
```

**Windows Docker Desktop 环境：**

```bash
./scripts/docker-run.sh start
```

### 3. 查看服务状态

```bash
# Windows
./scripts/docker-run.sh status

# Linux
docker ps
```

### 4. 查看日志

```bash
# Windows
./scripts/docker-run.sh logs

# Linux
docker logs gateway
docker logs user-service
# ...
```

### 5. 停止服务

```bash
# Windows
./scripts/docker-run.sh stop

# Linux
docker stop gateway user-service post-service interaction-service relation-service notification-service facade-service file-service
docker rm gateway user-service post-service interaction-service relation-service notification-service facade-service file-service
```

## 环境要求

- Docker 20.10+
- MySQL/Redis/Nacos 已在宿主机运行
- file-service 需要 `/data/files` 目录存在

## 端口映射

| 服务 | 端口 | 说明 |
|------|------|------|
| gateway | 8080 | HTTP 入口 |
| user-service | 8081 | - |
| post-service | 8082 | - |
| interaction-service | 8083 | - |
| relation-service | 8084 | - |
| notification-service | 8085 | - |
| facade-service | 8087 | - |
| file-service | 8086 | 需挂载 /data/files |

## Windows 环境特殊配置

Windows Docker Desktop 使用 `host.docker.internal` 访问宿主机服务。

启动脚本会自动设置：
- `--add-host=host.docker.internal:host-gateway`
- `-e SPRING_CLOUD_NACOS_DISCOVERY_SERVER-ADDR=host.docker.internal:8848`
- `-e SPRING_DATA_REDIS_HOST=host.docker.internal`
- `-e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/...`

## 镜像列表

```
swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-gateway:latest
swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-user-service:latest
swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-post-service:latest
swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-interaction-service:latest
swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-relation-service:latest
swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-notification-service:latest
swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-facade-service:latest
swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-file-service:latest
```

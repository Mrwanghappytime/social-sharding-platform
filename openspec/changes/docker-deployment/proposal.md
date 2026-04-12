## 为什么

当前社交平台使用传统方式部署微服务，各服务依赖宿主机环境和手动管理。采用 Docker 容器化可以：
- 标准化服务运行环境
- 简化服务部署和扩容流程
- 实现环境一致性（开发/测试/生产）

## 变更内容

1. **新增 Dockerfile 模板**
   - 通用服务 Dockerfile（7个服务共享）
   - file-service 专用 Dockerfile（增加文件目录挂载）
   - `.dockerignore` 减少构建上下文

2. **新增构建脚本**
   - `docker-build.sh`: 构建所有服务镜像
   - 各服务独立镜像命名规范

3. **运行时配置**
   - 使用 `--network host` 共享宿主机网络
   - file-service 挂载 `/data/files` 持久化文件

## 功能 (Capabilities)

### 新增功能
- `docker-deployment`: Docker 镜像构建和容器化部署规范

### 修改功能
（无）

## 影响

- 新增 `docker/` 目录包含所有 Dockerfile
- 新增 `docker-build.sh` 构建脚本
- 各服务 `application.yml` 无需修改（已通过环境变量或 Nacos 配置中心注入配置）

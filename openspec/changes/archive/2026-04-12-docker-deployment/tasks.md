## 1. 基础设施文件

- [x] 1.1 创建 `docker/.dockerignore`（排除 target、node_modules、.git、logs、docs 等）
- [x] 1.2 创建 `docker/Dockerfile.base`（通用服务基础 Dockerfile 模板）

## 2. 各服务 Dockerfile

- [x] 2.1 创建 `docker/Dockerfile.gateway`
- [x] 2.2 创建 `docker/Dockerfile.user-service`
- [x] 2.3 创建 `docker/Dockerfile.post-service`
- [x] 2.4 创建 `docker/Dockerfile.interaction-service`
- [x] 2.5 创建 `docker/Dockerfile.relation-service`
- [x] 2.6 创建 `docker/Dockerfile.notification-service`
- [x] 2.7 创建 `docker/Dockerfile.facade-service`
- [x] 2.8 创建 `docker/Dockerfile.file-service`（特殊：运行时需挂载 /data/files）

## 3. 构建脚本

- [x] 3.1 创建 `scripts/docker-build.sh`（一键构建所有镜像脚本）

## 4. 测试验证

- [x] 4.1 执行 `mvn clean package -DskipTests` 验证构建
- [x] 4.2 执行 `./scripts/docker-build.sh` 验证镜像构建
- [x] 4.3 测试 docker run --network host 启动 gateway 服务（容器启动正常，日志输出正确）
- [x] 4.4 测试 file-service 容器启动并挂载 /data/files（Windows 端口映射模式已验证）

**回归测试基线：** `docs/regression-tests.md`（48 项测试用例）

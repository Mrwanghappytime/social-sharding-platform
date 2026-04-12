#!/bin/bash

# ============================================
# Social Platform - Docker Build Script
# ============================================
# 功能：构建所有微服务的 Docker 镜像
# 使用：./scripts/docker-build.sh
# ============================================

set -e

# 项目根目录
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

# 镜像仓库地址
REGISTRY="swr.cn-north-4.myhuaweicloud.com/ddn-k8s"

# 项目名称前缀
PROJECT_PREFIX="social-platform"

# 服务列表
SERVICES=(
    "gateway"
    "user-service"
    "post-service"
    "interaction-service"
    "relation-service"
    "notification-service"
    "facade-service"
    "file-service"
)

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo_step() {
    echo -e "${YELLOW}==> $1${NC}"
}

echo_success() {
    echo -e "${GREEN}==> $1${NC}"
}

echo_error() {
    echo -e "${RED}==> ERROR: $1${NC}"
}

# 清理旧镜像和构建缓存
cleanup_images() {
    echo_step "清理旧镜像和构建缓存..."

    # 获取构建前的镜像占用空间
    BEFORE_SIZE=$(docker system df --format "{{.Size}}" 2>/dev/null | head -1)

    # 清理悬空镜像（无 tag 的旧镜像）
    echo "  清理悬空镜像..."
    docker image prune -f > /dev/null 2>&1 || true

    # 清理构建缓存
    echo "  清理构建缓存..."
    docker builder prune -f > /dev/null 2>&1 || true

    # 获取清理后的镜像占用空间
    AFTER_SIZE=$(docker system df --format "{{.Size}}" 2>/dev/null | head -1)

    if [ -n "$BEFORE_SIZE" ] && [ -n "$AFTER_SIZE" ]; then
        echo_success "镜像清理完成 (清理前: $BEFORE_SIZE, 清理后: $AFTER_SIZE)"
    else
        echo_success "镜像清理完成"
    fi
}

# 1. Maven 打包
echo_step "Step 1: Maven 打包所有服务..."
mvn clean package -DskipTests -q
echo_success "Maven 打包完成"

# 2. 构建各服务镜像
echo_step "Step 2: 构建 Docker 镜像..."

for SERVICE in "${SERVICES[@]}"; do
    echo_step "Building ${SERVICE}..."

    # 镜像名称
    IMAGE_NAME="${REGISTRY}/${PROJECT_PREFIX}-${SERVICE}:latest"

    # Dockerfile 路径
    DOCKERFILE="docker/Dockerfile.${SERVICE}"

    if [ ! -f "$DOCKERFILE" ]; then
        echo_error "Dockerfile not found: $DOCKERFILE"
        continue
    fi

    # 构建镜像（context 使用 {service}/ 目录，这样 COPY target/*.jar 能正确解析）
    docker build -f "$DOCKERFILE" -t "$IMAGE_NAME" "${SERVICE}"

    if [ $? -eq 0 ]; then
        echo_success "Built: $IMAGE_NAME"
    else
        echo_error "Failed to build: $SERVICE"
        exit 1
    fi
done

echo_success "所有镜像构建完成！"

# 3. 清理旧镜像
echo ""
cleanup_images

echo ""
echo "镜像列表："
for SERVICE in "${SERVICES[@]}"; do
    echo "  - ${REGISTRY}/${PROJECT_PREFIX}-${SERVICE}:latest"
done

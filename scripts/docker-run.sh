#!/bin/bash

# ============================================
# Social Platform - Docker Run Script (Windows)
# ============================================
# 功能：使用端口映射方式启动所有 Docker 容器
# 适用：Windows Docker Desktop 环境
# 重要：需要先确保 /data/files 目录存在
# ============================================

set -e

# 项目根目录
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

# 镜像仓库地址
REGISTRY="swr.cn-north-4.myhuaweicloud.com/ddn-k8s"

# 项目名称前缀
PROJECT_PREFIX="social-platform"

# 服务配置：名称、端口、镜像名
declare -A SERVICES=(
    ["gateway"]="8080:swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-gateway:latest"
    ["user-service"]="8081:swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-user-service:latest"
    ["post-service"]="8082:swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-post-service:latest"
    ["interaction-service"]="8083:swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-interaction-service:latest"
    ["relation-service"]="8084:swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-relation-service:latest"
    ["notification-service"]="8085:swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-notification-service:latest"
    ["facade-service"]="8087:swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-facade-service:latest"
    ["file-service"]="8086:swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-file-service:latest"
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

# 清理已存在的容器
cleanup() {
    echo_step "清理已存在的容器..."
    for SERVICE in "${!SERVICES[@]}"; do
        CONTAINER_NAME="${SERVICE}"
        if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
            docker stop "$CONTAINER_NAME" 2>/dev/null || true
            docker rm "$CONTAINER_NAME" 2>/dev/null || true
            echo "  已清理: $CONTAINER_NAME"
        fi
    done
}

# 启动单个服务
start_service() {
    local SERVICE=$1
    local PORT=$2
    local IMAGE=$3

    echo_step "启动 $SERVICE (端口 $PORT)..."

    # 检查镜像是否存在
    if ! docker image inspect "$IMAGE" > /dev/null 2>&1; then
        echo_error "镜像不存在: $IMAGE"
        echo_error "请先执行: ./scripts/docker-build.sh"
        return 1
    fi

    local DOCKER_RUN_CMD="docker run -d --name $SERVICE"
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD -p $PORT:$PORT"
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD --add-host=host.docker.internal:host-gateway"
    # Java 系统属性覆盖 YAML 配置
    local JAVA_OPTS_VALUE="-Dspring.cloud.nacos.discovery.server-addr=host.docker.internal:8848"
    JAVA_OPTS_VALUE="$JAVA_OPTS_VALUE -Dspring.cloud.nacos.config.server-addr=host.docker.internal:8848"
    JAVA_OPTS_VALUE="$JAVA_OPTS_VALUE -Dspring.datasource.url=jdbc:mysql://host.docker.internal:3306/social_platform"
    JAVA_OPTS_VALUE="$JAVA_OPTS_VALUE -Dspring.data.redis.host=host.docker.internal"
    JAVA_OPTS_VALUE="$JAVA_OPTS_VALUE -Dlogging.file.path=/app/logs"
    JAVA_OPTS_VALUE="$JAVA_OPTS_VALUE -Ddubbo.registry.address=nacos://host.docker.internal:8848"
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD -e JAVA_OPTS=\"$JAVA_OPTS_VALUE\""
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD -e SPRING_CLOUD_NACOS_DISCOVERY_SERVER-ADDR=host.docker.internal:8848"
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD -e SPRING_CLOUD_NACOS_CONFIG_SERVER-ADDR=host.docker.internal:8848"
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/social_platform"
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD -e SPRING_DATA_REDIS_HOST=host.docker.internal"
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD -e LOG_PATH=/app/logs"

    # 挂载日志目录 (G盘)
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD -v g:/logs/${SERVICE}:/app/logs"

    # file-service 特殊处理：挂载文件目录 (G盘)
    if [ "$SERVICE" = "file-service" ]; then
        DOCKER_RUN_CMD="$DOCKER_RUN_CMD -v g:/data/files:/data/files"
    fi

    DOCKER_RUN_CMD="$DOCKER_RUN_CMD $IMAGE"

    eval "$DOCKER_RUN_CMD"

    if [ $? -eq 0 ]; then
        echo_success "已启动: $SERVICE"
    else
        echo_error "启动失败: $SERVICE"
        return 1
    fi
}

# 显示帮助
show_help() {
    echo "Usage: $0 [command] [service]"
    echo ""
    echo "管理所有或单个微服务的 Docker 容器"
    echo ""
    echo "Commands:"
    echo "  start [service]    启动所有服务或单个服务"
    echo "  stop [service]     停止所有服务或单个服务"
    echo "  restart [service]  重启所有服务或单个服务"
    echo "  logs [service]     查看所有服务或单个服务的日志"
    echo "  status [service]   查看服务状态"
    echo "  cleanup [service]  清理所有容器或单个容器"
    echo "  help               显示帮助"
    echo ""
    echo "服务列表:"
    for service in "${!SERVICES[@]}"; do
        local PORT="${SERVICES[$service]%%:*}"
        echo "  $service (端口: $PORT)"
    done
    echo ""
    echo "示例:"
    echo "  $0 start              # 启动所有服务"
    echo "  $0 start gateway      # 仅启动 gateway 服务"
    echo "  $0 logs user-service  # 查看 user-service 的日志"
    echo "  $0 status             # 查看所有服务状态"
}

# 检查服务是否有效
is_valid_service() {
    local service=$1
    for s in "${!SERVICES[@]}"; do
        if [ "$s" = "$service" ]; then
            return 0
        fi
    done
    return 1
}

# 清理单个容器
cleanup_service() {
    local SERVICE=$1
    local CONTAINER_NAME="${SERVICE}"
    if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        docker stop "$CONTAINER_NAME" 2>/dev/null || true
        docker rm "$CONTAINER_NAME" 2>/dev/null || true
        echo "  已清理: $CONTAINER_NAME"
    fi
}

# 查看单个服务日志
show_service_logs() {
    local SERVICE=$1
    echo -e "${YELLOW}==> $SERVICE 日志${NC}"
    docker logs "$SERVICE" 2>&1 | tail -20
    echo ""
}

# 查看单个服务状态
show_service_status() {
    local SERVICE=$1
    local PORT="${SERVICES[$SERVICE]%%:*}"
    local STATUS=$(docker ps --filter "name=$SERVICE" --format "{{.Status}}" 2>/dev/null || echo "未运行")
    printf "%-25s %-15s %s\n" "$SERVICE" "$STATUS" "$PORT"
}

# 停止单个服务
stop_service() {
    local SERVICE=$1
    docker stop "$SERVICE" 2>/dev/null || true
    echo "  已停止: $SERVICE"
}

# 查看日志
show_logs() {
    if [ $# -eq 0 ]; then
        for SERVICE in "${!SERVICES[@]}"; do
            echo -e "${YELLOW}==> $SERVICE 日志${NC}"
            docker logs "$SERVICE" 2>&1 | tail -20
            echo ""
        done
    else
        show_service_logs "$1"
    fi
}

# 查看状态
show_status() {
    if [ $# -eq 0 ]; then
        echo "服务状态："
        printf "%-25s %-15s %s\n" "服务名" "状态" "端口"
        echo "------------------------------------------------"
        for SERVICE in "${!SERVICES[@]}"; do
            local PORT="${SERVICES[$SERVICE]%%:*}"
            local STATUS=$(docker ps --filter "name=$SERVICE" --format "{{.Status}}" 2>/dev/null || echo "未运行")
            printf "%-25s %-15s %s\n" "$SERVICE" "$STATUS" "$PORT"
        done
    else
        show_service_status "$1"
    fi
}

# 停止所有服务
stop_all() {
    echo_step "停止所有服务..."
    for SERVICE in "${!SERVICES[@]}"; do
        docker stop "$SERVICE" 2>/dev/null || true
        echo "  已停止: $SERVICE"
    done
}

# 清理所有容器
cleanup() {
    echo_step "清理已存在的容器..."
    if [ $# -eq 0 ]; then
        for SERVICE in "${!SERVICES[@]}"; do
            cleanup_service "$SERVICE"
        done
    else
        cleanup_service "$1"
    fi
}

# 主逻辑
if [ $# -eq 0 ]; then
    # 默认启动所有服务
    set -- "start"
fi

case "${1}" in
    start)
        if [ $# -eq 1 ]; then
            # 启动所有服务
            cleanup
            echo_step "启动所有服务..."
            for SERVICE in "${!SERVICES[@]}"; do
                PORT="${SERVICES[$SERVICE]%%:*}"
                IMAGE="${SERVICES[$SERVICE]#*:}"
                start_service "$SERVICE" "$PORT" "$IMAGE" || exit 1
            done
            echo ""
            echo_success "所有服务启动完成！"
            echo ""
            show_status
        elif is_valid_service "${2}"; then
            # 启动单个服务
            SERVICE="${2}"
            cleanup_service "$SERVICE"
            echo_step "启动 $SERVICE 服务..."
            PORT="${SERVICES[$SERVICE]%%:*}"
            IMAGE="${SERVICES[$SERVICE]#*:}"
            start_service "$SERVICE" "$PORT" "$IMAGE" || exit 1
            echo ""
            echo_success "$SERVICE 服务启动完成！"
            echo ""
            show_service_status "$SERVICE"
        else
            echo_error "无效的服务名称: ${2}"
            show_help
            exit 1
        fi
        ;;
    stop)
        if [ $# -eq 1 ]; then
            stop_all
        elif is_valid_service "${2}"; then
            echo_step "停止 ${2} 服务..."
            stop_service "${2}"
            echo_success "${2} 服务已停止"
        else
            echo_error "无效的服务名称: ${2}"
            show_help
            exit 1
        fi
        ;;
    restart)
        if [ $# -eq 1 ]; then
            stop_all
            echo ""
            $0 start
        elif is_valid_service "${2}"; then
            echo_step "重启 ${2} 服务..."
            stop_service "${2}"
            cleanup_service "${2}"
            PORT="${SERVICES[${2}]%%:*}"
            IMAGE="${SERVICES[${2}]#*:}"
            start_service "${2}" "$PORT" "$IMAGE" || exit 1
            echo_success "${2} 服务重启完成！"
            show_service_status "${2}"
        else
            echo_error "无效的服务名称: ${2}"
            show_help
            exit 1
        fi
        ;;
    logs)
        if [ $# -eq 1 ]; then
            show_logs
        elif is_valid_service "${2}"; then
            show_service_logs "${2}"
        else
            echo_error "无效的服务名称: ${2}"
            show_help
            exit 1
        fi
        ;;
    status)
        if [ $# -eq 1 ]; then
            show_status
        elif is_valid_service "${2}"; then
            show_service_status "${2}"
        else
            echo_error "无效的服务名称: ${2}"
            show_help
            exit 1
        fi
        ;;
    cleanup)
        if [ $# -eq 1 ]; then
            cleanup
        elif is_valid_service "${2}"; then
            echo_step "清理 ${2} 容器..."
            cleanup_service "${2}"
            echo_success "${2} 容器清理完成"
        else
            echo_error "无效的服务名称: ${2}"
            show_help
            exit 1
        fi
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo_error "未知命令: $1"
        show_help
        exit 1
        ;;
esac

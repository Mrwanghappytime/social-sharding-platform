#!/bin/bash

# Social Platform - Stop, Build and Start All Services
# Usage: ./restart-all.sh

set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

# Services configuration: name, port, module, debug_port
declare -a SERVICES=(
    "gateway:8080:gateway:5005"
    "user-service:8081:user-service:5006"
    "post-service:8082:post-service:5007"
    "interaction-service:8083:interaction-service:5008"
    "relation-service:8084:relation-service:5009"
    "notification-service:8085:notification-service:5010"
    "file-service:8086:file-service:5011"
    "facade-service:8087:facade-service:5012"
)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo_step() {
    echo -e "${YELLOW}==> $1${NC}"
}

echo_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

echo_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Function to kill process on port
kill_port() {
    local port=$1
    echo "  Checking port $port..."
    # Use netstat to find PID using port
    local pid=$(netstat -ano 2>/dev/null | grep ":$port " | grep LISTENING | awk '{print $5}' | head -1)
    if [ -n "$pid" ] && [ "$pid" != "0" ]; then
        echo "  Killing process $pid on port $port..."
        # Use taskkill on Windows, kill on Unix
        if grep -qEi "(Microsoft|MSYS|MINGW)" <<< "$(uname -a)" 2>/dev/null; then
            taskkill //F //PID $pid 2>/dev/null || true
        else
            kill -9 $pid 2>/dev/null || true
        fi
    else
        echo "  No process found on port $port"
    fi
}

# Function to check if port is free
check_port() {
    local port=$1
    if lsof -i:$port > /dev/null 2>&1; then
        return 0  # port is in use
    else
        return 1  # port is free
    fi
}

# Stop all services
echo_step "Stopping all services..."
for svc in "${SERVICES[@]}"; do
    IFS=':' read -r name port module <<< "$svc"
    kill_port $port
done
echo_success "All services stopped"

# Wait a bit for ports to be released
sleep 2

# Clean build all modules
echo_step "Building all modules..."
mvn clean install -DskipTests -q
echo_success "Build complete"

# JVM memory settings
JVM_OPTS="-Xmx300m -Xms100m"

# Start all services in background
echo_step "Starting all services..."
for svc in "${SERVICES[@]}"; do
    IFS=':' read -r name port module debug_port <<< "$svc"
    echo "  Starting $name on port $port (debug: $debug_port)..."
    cd "$PROJECT_DIR/$module"
    export MAVEN_OPTS="$JVM_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$debug_port"
    nohup mvn spring-boot:run > "$PROJECT_DIR/logs/$name.log" 2>&1 &
    echo "  $name started, PID: $!"
done

echo ""
echo_success "All services starting..."
echo "Logs will be written to: $PROJECT_DIR/logs/"
echo ""
echo "Services:"
for svc in "${SERVICES[@]}"; do
    IFS=':' read -r name port module debug_port <<< "$svc"
    echo "  - $name (port $port, debug: $debug_port)"
done

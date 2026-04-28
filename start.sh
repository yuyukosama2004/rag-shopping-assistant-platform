#!/bin/bash
# 手机电商平台 一键启动/停止脚本

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
MVN_IMG="maven:3.9.9-eclipse-temurin-21"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

# 启动中间件
start_infra() {
    log "启动中间件 (MySQL, Redis, RabbitMQ, Nacos)..."
    cd "$PROJECT_DIR" && docker compose -f docker/docker-compose.infrastructure.yml up -d
    log "等待中间件就绪..."
    sleep 10
    docker ps --format "table {{.Names}}\t{{.Status}}" --filter "name=biyesheji-"
    log "中间件启动完成"
}

# 停止并清理中间件
stop_infra() {
    log "停止中间件..."
    cd "$PROJECT_DIR" && docker compose -f docker/docker-compose.infrastructure.yml down
    log "中间件已停止"
}

# 构建项目
build() {
    log "Maven 构建项目..."
    docker run --rm -v "$PROJECT_DIR:/app" -v maven-repo:/root/.m2 -w /app "$MVN_IMG" mvn clean install -DskipTests
}

# 启动单个微服务
start_service() {
    local svc=$1
    local port=$2
    log "启动 $svc (:${port})..."
    cd "$PROJECT_DIR/$svc" || exit 1
    docker run --rm -d --name "biyesheji-$svc" \
        --network host \
        -v "$PROJECT_DIR:/app" \
        -v "$PROJECT_DIR/$svc/target:/app/target" \
        -w /app \
        "$MVN_IMG" mvn -pl "$svc" spring-boot:run &
    sleep 5
    if curl -s "http://localhost:$port/doc.html" > /dev/null 2>&1; then
        log "$svc 启动成功 - http://localhost:$port/doc.html"
    else
        warn "$svc 正在启动中，请稍候..."
    fi
}

case "${1:-help}" in
    infra-start)
        start_infra
        ;;
    infra-stop)
        stop_infra
        ;;
    build)
        build
        ;;
    all)
        start_infra
        build
        ;;
    help|*)
        echo "手机电商平台 管理脚本"
        echo ""
        echo "用法: ./start.sh <command>"
        echo ""
        echo "命令:"
        echo "  infra-start   启动中间件 (MySQL, Redis, RabbitMQ, Nacos)"
        echo "  infra-stop    停止中间件"
        echo "  build         Maven 构建全部微服务"
        echo "  all           启动中间件 + 构建项目"
        echo ""
        echo "启动微服务 (IDE 方式):"
        echo "  cd user-service && mvn spring-boot:run     # :8081"
        echo "  cd product-service && mvn spring-boot:run  # :8082"
        echo "  cd order-service && mvn spring-boot:run    # :8083"
        echo "  cd gateway-service && mvn spring-boot:run  # :8080"
        echo ""
        echo "启动前端:"
        echo "  cd biyesheji-frontend && npm run dev       # :5173"
        echo ""
        echo "访问地址:"
        echo "  前端:        http://localhost:5173"
        echo "  网关:        http://localhost:8080"
        echo "  Nacos控制台: http://localhost:8848/nacos (账号 nacos/nacos)"
        echo "  RabbitMQ:    http://localhost:15672 (账号 admin/admin123)"
        echo "  Knife4j文档: http://localhost:8081/doc.html (用户服务)"
        ;;
esac

#!/bin/bash
# 手机电商平台 启动脚本（低配优化版）
# 适配: AMD A8 4500M + 12GB DDR3 + Ubuntu Server + SSD

set -e
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()   { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()   { echo -e "${RED}[ERROR]${NC} $1"; }

# JVM 低配参数（所有微服务通用）
# SerialGC: 单线程 GC，4 核 CPU 上避免和其他进程争抢
JVM_OPTS="-Xms64m -Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Xss320k -XX:+TieredCompilation -Djava.awt.headless=true"

wait_http() {
    local url=$1 desc=$2 max=${3:-60}
    log "等待 $desc ($url)..."
    for i in $(seq 1 $max); do
        if curl -sf "$url" >/dev/null 2>&1; then
            log "$desc 就绪 (${i}s)"
            return 0
        fi
        sleep 1
    done
    warn "$desc 超时"
    return 1
}

# ========== infra ==========
start_infra() {
    log "启动中间件..."
    cd "$PROJECT_DIR"
    docker compose -f docker/docker-compose.infrastructure.yml up -d

    wait_http "http://localhost:3306" "MySQL"      40 || true  # MySQL 不响应 HTTP，用 docker ps 辅助
    docker exec biyesheji-mysql mysqladmin ping -uroot -proot123 --silent 2>/dev/null \
        && log "MySQL 就绪" || warn "MySQL 可能未就绪"

    wait_http "http://localhost:15672" "RabbitMQ"   40
    wait_http "http://localhost:8848/nacos" "Nacos" 60

    docker ps --format "table {{.Names}}\t{{.Status}}" --filter "name=biyesheji-"
    log "中间件全部就绪"
}

stop_infra() {
    log "停止中间件..."
    cd "$PROJECT_DIR" && docker compose -f docker/docker-compose.infrastructure.yml down
    log "中间件已停止"
}

# ========== build ==========
build() {
    log "Maven 构建项目..."
    docker run --rm -v "$PROJECT_DIR:/app" -v maven-repo:/root/.m2 -w /app \
        maven:3.9.9-eclipse-temurin-21 mvn clean install -DskipTests
    log "构建完成"
}

# ========== 启动微服务 ==========
start_services() {
    log "启动微服务（低配 JVM 参数）..."
    cd "$PROJECT_DIR"

    # 1. user-service :8081
    log "启动 user-service..."
    nohup java $JVM_OPTS -jar user-service/target/user-service-1.0.0.jar \
        --spring.profiles.active=low > logs/user-service.log 2>&1 &
    echo $! > logs/user-service.pid
    wait_http "http://localhost:8081/doc.html" "user-service" 90

    # 2. product-service :8082
    log "启动 product-service..."
    nohup java $JVM_OPTS -jar product-service/target/product-service-1.0.0.jar \
        --spring.profiles.active=low > logs/product-service.log 2>&1 &
    echo $! > logs/product-service.pid
    wait_http "http://localhost:8082/doc.html" "product-service" 60

    # 3. order-service :8083
    log "启动 order-service..."
    nohup java $JVM_OPTS -jar order-service/target/order-service-1.0.0.jar \
        --spring.profiles.active=low > logs/order-service.log 2>&1 &
    echo $! > logs/order-service.pid
    wait_http "http://localhost:8083/doc.html" "order-service" 90

    # 4. gateway-service :8080
    log "启动 gateway-service..."
    nohup java $JVM_OPTS -jar gateway-service/target/gateway-service-1.0.0.jar \
        --spring.profiles.active=low > logs/gateway-service.log 2>&1 &
    echo $! > logs/gateway-service.pid
    wait_http "http://localhost:8080/doc.html" "gateway-service" 60

    log "全部微服务启动完成"
    jps -l 2>/dev/null | grep biyesheji || log "请用 jps 或 ps 检查进程"
}

stop_services() {
    log "停止微服务..."
    for pid_file in logs/*.pid; do
        [ -f "$pid_file" ] && kill "$(cat "$pid_file")" 2>/dev/null && rm "$pid_file"
    done
    log "微服务已停止"
}

# ========== 前端 ==========
start_frontend() {
    log "启动前端开发服务器..."
    cd "$PROJECT_DIR/biyesheji-frontend"
    npm run dev -- --host 0.0.0.0 &
    log "前端启动中 (http://localhost:5173)"
}

# ========== 状态 ==========
status() {
    echo "===== Docker 容器 ====="
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=biyesheji-"
    echo ""
    echo "===== Java 进程 ====="
    jps -lv 2>/dev/null | grep -v jps || echo "无 Java 进程"
}

# ========== 入口 ==========
mkdir -p "$PROJECT_DIR/logs"

case "${1:-help}" in
    infra-start)  start_infra ;;
    infra-stop)   stop_infra ;;
    build)        build ;;
    start)        start_services ;;
    stop)         stop_services ;;
    frontend)     start_frontend ;;
    restart)      stop_services; start_services ;;
    status)       status ;;
    all)
        start_infra
        start_services
        start_frontend
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
        echo "  start         按顺序启动 4 个微服务（低配 JVM 参数）"
        echo "  stop          停止所有微服务"
        echo "  restart       重启微服务"
        echo "  frontend      启动 Vue 前端开发服务器"
        echo "  status        查看容器和进程状态"
        echo "  all           一键启动全部（中间件 + 微服务 + 前端）"
        echo ""
        echo "演示流程:  ./start.sh infra-start && ./start.sh start && ./start.sh frontend"
        ;;
esac

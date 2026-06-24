#!/bin/bash
# ============================================================
# PhoneMall E5 管理脚本
# 用法:
#   bash e5-ctl.sh start         一键启动全部
#   bash e5-ctl.sh stop          停止全部
#   bash e5-ctl.sh restart       重启全部
#   bash e5-ctl.sh status        查看状态
#   bash e5-ctl.sh restart xxx   重启单个服务 (gateway/user/product/order)
#   bash e5-ctl.sh logs xxx      查看某服务日志
#   bash e5-ctl.sh check         全面检查 + 自动修复
# ============================================================

set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[..]${NC} $1"; }
err()   { echo -e "${RED}[!!]${NC} $1"; }

PROJECT_DIR=$(cd "$(dirname "$0")/.." && pwd)
JVM_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:MaxMetaspaceSize=256m"
IMG="eclipse-temurin:17-jre-jammy"

# ========== 启动中间件 ==========
start_infra() {
    log "启动中间件..."
    cd "$PROJECT_DIR/docker"
    docker compose -f docker-compose.e5.yml up -d

    log "等待 MySQL..."
    for i in $(seq 1 60); do
        docker exec biyesheji-mysql mysqladmin ping -uroot -proot123 --silent 2>/dev/null && break
        sleep 1
    done
    docker exec biyesheji-mysql mysqladmin ping -uroot -proot123 --silent 2>/dev/null && log "MySQL OK" || warn "MySQL 未就绪"

    # 确保表结构完整
    docker exec biyesheji-mysql mysql -uroot -proot123 biyesheji -e \
        "ALTER TABLE t_product ADD COLUMN color_options JSON" 2>/dev/null || true
    docker exec biyesheji-mysql mysql -uroot -proot123 biyesheji -e \
        "ALTER TABLE t_product ADD COLUMN storage_options JSON" 2>/dev/null || true
    docker exec biyesheji-mysql mysql -uroot -proot123 biyesheji -e \
        "ALTER TABLE t_shopping_cart ADD COLUMN selected_color VARCHAR(50)" 2>/dev/null || true
    docker exec biyesheji-mysql mysql -uroot -proot123 biyesheji -e \
        "ALTER TABLE t_shopping_cart ADD COLUMN selected_storage VARCHAR(50)" 2>/dev/null || true
}

# ========== 启动单个微服务 ==========
start_svc() {
    local svc=$1
    local jar="$PROJECT_DIR/$svc/target/$svc-1.0.0.jar"
    [ -f "$jar" ] || { err "$jar 不存在, 请先构建"; return 1; }

    docker rm -f biyesheji-$svc 2>/dev/null || true

    local extra=""
    local jvm="$JVM_OPTS"
    [ "$svc" = "order-service" ] && extra="-e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY:-} -e OPENROUTER_API_KEY=${OPENROUTER_API_KEY:-}" && jvm="$JVM_OPTS -Djdk.httpclient.keepalive.timeout=300"

    docker run --rm -d --name biyesheji-$svc --network host \
        -v "$jar:/app.jar" $extra $IMG java $jvm -jar /app.jar
    log "已启动: $svc"
}

# ========== 启动全部 ==========
start_all() {
    start_infra
    for s in user-service product-service order-service gateway-service; do
        start_svc $s
        sleep 4
    done

    log "等待 Gateway 就绪..."
    for i in $(seq 1 30); do
        curl -sf "http://localhost:8080/api/product/page?pageSize=1" >/dev/null 2>&1 && log "Gateway 就绪" && break
        sleep 2
    done

    # frpc
    systemctl restart frpc 2>/dev/null && log "frpc 已启动" || warn "frpc 未配置"

    # nginx
    sudo nginx -s reload 2>/dev/null && log "Nginx 已重载" || warn "Nginx 未运行"

    status
}

# ========== 停止全部 ==========
stop_all() {
    log "停止微服务..."
    docker rm -f biyesheji-user-service biyesheji-product-service biyesheji-order-service biyesheji-gateway-service 2>/dev/null || true
    log "停止中间件..."
    cd "$PROJECT_DIR/docker" && docker compose -f docker-compose.e5.yml down 2>/dev/null || true
    log "全部已停止"
}

# ========== 重启 ==========
restart_svc() {
    local svc=$1
    case $svc in
        gateway) svc="gateway-service" ;;
        user)    svc="user-service" ;;
        product) svc="product-service" ;;
        order)   svc="order-service" ;;
        *)       err "未知服务: $svc (可用: gateway/user/product/order)"; return 1 ;;
    esac
    log "重启 $svc..."
    start_svc $svc
}

# ========== 状态 ==========
status() {
    echo ""
    echo "===== 容器状态 ====="
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=biyesheji-" 2>/dev/null
    echo ""
    echo "===== 服务健康检查 ====="
    curl -sf "http://localhost:8080/api/product/page?pageSize=1" >/dev/null 2>&1 && log "Gateway :8080 OK" || warn "Gateway :8080 不可用"
    curl -sf "http://localhost:8848/nacos" >/dev/null 2>&1 && log "Nacos :8848 OK" || warn "Nacos 不可用"
    curl -sf "http://localhost:15672" >/dev/null 2>&1 && log "RabbitMQ :15672 OK" || warn "RabbitMQ 不可用"
    echo ""
}

# ========== 日志 ==========
logs() {
    local svc=$1
    case $svc in
        gateway) docker logs biyesheji-gateway-service --tail 50 -f ;;
        user)    docker logs biyesheji-user-service --tail 50 -f ;;
        product) docker logs biyesheji-product-service --tail 50 -f ;;
        order)   docker logs biyesheji-order-service --tail 50 -f ;;
        mysql)   docker logs biyesheji-mysql --tail 50 -f ;;
        *)       err "可用: gateway/user/product/order/mysql" ;;
    esac
}

# ========== 全面检查+修复 ==========
check() {
    log "全面检查..."

    # 1. 容器运行状态
    local missing=""
    for c in biyesheji-mysql biyesheji-redis biyesheji-rabbitmq biyesheji-nacos \
             biyesheji-user-service biyesheji-product-service biyesheji-order-service biyesheji-gateway-service; do
        docker ps --format "{{.Names}}" --filter "name=$c" | grep -q $c || missing="$missing $c"
    done

    if [ -n "$missing" ]; then
        warn "缺少容器:$missing"
        log "尝试修复: 重新启动..."
        start_all
        return
    fi
    log "所有容器正在运行"

    # 2. Gateway 可达性
    if ! curl -sf "http://localhost:8080/api/product/page?pageSize=1" >/dev/null 2>&1; then
        warn "Gateway 不可达，重启 Gateway..."
        restart_svc gateway
        sleep 10
    fi

    # 3. 数据库表结构
    log "检查数据库表结构..."
    docker exec biyesheji-mysql mysql -uroot -proot123 biyesheji -e \
        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='t_product' AND COLUMN_NAME='color_options'" 2>/dev/null | grep -q color_options || {
        warn "缺少 color_options 列，修复中..."
        start_infra
    }

    # 4. frpc
    systemctl is-active --quiet frpc 2>/dev/null && log "frpc 运行中" || warn "frpc 未运行，尝试启动: sudo systemctl start frpc"

    # 5. Nginx
    curl -sf "http://localhost/" >/dev/null 2>&1 && log "Nginx 正常" || warn "Nginx 不可达，尝试重载: sudo nginx -s reload"

    echo ""
    log "检查完成"
    status
}

# ========== 入口 ==========
case "${1:-}" in
    start)     start_all ;;
    stop)      stop_all ;;
    restart)
        if [ -n "$2" ]; then
            restart_svc "$2"
        else
            stop_all
            start_all
        fi
        ;;
    status)    status ;;
    logs)      logs "${2:-order}" ;;
    check)     check ;;
    *)
        echo "PhoneMall E5 管理脚本"
        echo ""
        echo "用法: bash e5-ctl.sh <命令> [参数]"
        echo ""
        echo "命令:"
        echo "  start              一键启动全部"
        echo "  stop               停止全部"
        echo "  restart            重启全部"
        echo "  restart gateway     重启 Gateway"
        echo "  restart user        重启 user-service"
        echo "  restart product     重启 product-service"
        echo "  restart order       重启 order-service"
        echo "  status             查看运行状态"
        echo "  logs [服务名]       查看日志 (默认 order)"
        echo "  check              全面检查 + 自动修复"
        echo ""
        echo "示例:"
        echo "  bash e5-ctl.sh start"
        echo "  bash e5-ctl.sh check"
        echo "  bash e5-ctl.sh restart order"
        ;;
esac

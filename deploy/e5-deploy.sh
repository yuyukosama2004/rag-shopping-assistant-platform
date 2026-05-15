#!/bin/bash
# ============================================================
# PhoneMall E5 2680V4 服务器一键部署脚本
# 用法: bash e5-deploy.sh
# 前提: 服务器已安装 Docker
# ============================================================
set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[..]${NC} $1"; }
err()   { echo -e "${RED}[!!]${NC} $1"; exit 1; }

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

echo "============================================"
echo " PhoneMall E5 2680V4 一键部署"
echo " 项目目录: $PROJECT_DIR"
echo "============================================"
echo ""

# ========== 1. 检查依赖 ==========
log "检查依赖..."
command -v docker >/dev/null 2>&1 || err "请先安装 Docker"
docker compose version >/dev/null 2>&1 || err "请安装 docker-compose-plugin"

# ========== 2. 启动中间件 ==========
log "启动中间件 (MySQL, Redis, RabbitMQ, Nacos)..."
cd "$PROJECT_DIR/docker"
docker compose -f docker-compose.e5.yml up -d

log "等待 MySQL 就绪..."
for i in $(seq 1 60); do
    docker exec biyesheji-mysql mysqladmin ping -uroot -proot123 --silent 2>/dev/null && break
    sleep 1
done

for svc in "http://localhost:15672 RabbitMQ" "http://localhost:8848/nacos Nacos"; do
    url=$(echo $svc | awk '{print $1}')
    name=$(echo $svc | awk '{print $2}')
    for i in $(seq 1 40); do
        curl -sf "$url" >/dev/null 2>&1 && break
        sleep 1
    done
    curl -sf "$url" >/dev/null 2>&1 && log "$name 就绪" || warn "$name 可能未就绪"
done

# ========== 3. Maven 构建 ==========
log "Maven 构建全部微服务 (Docker Maven JDK17)..."
docker run --rm -v "$PROJECT_DIR:/app" -v maven-repo:/root/.m2 -w /app \
    maven:3.9-eclipse-temurin-17 mvn package -DskipTests -q
log "构建完成"

# ========== 4. 启动微服务 ==========
log "启动微服务..."
JVM_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:MaxMetaspaceSize=256m"
JAVA_IMAGE="eclipse-temurin:17-jre-jammy"

for svc in user-service product-service order-service gateway-service; do
    jar="$svc/target/$svc-1.0.0.jar"
    [ -f "$jar" ] || err "$jar 不存在, 构建可能失败"

    # 为 order-service 传递 API Keys
    extra_env=""
    if [ "$svc" = "order-service" ]; then
        extra_env="-e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY:-} -e OPENROUTER_API_KEY=${OPENROUTER_API_KEY:-}"
        [ -z "$DEEPSEEK_API_KEY" ] && warn "DEEPSEEK_API_KEY 未设置, AI导购将无法使用"
        [ -z "$OPENROUTER_API_KEY" ] && warn "OPENROUTER_API_KEY 未设置, RAG Embedding 将无法使用"
    fi

    docker rm -f biyesheji-$svc 2>/dev/null || true
    docker run --rm -d --name biyesheji-$svc --network host \
        -v "$PROJECT_DIR/$jar:/app.jar" \
        $extra_env \
        $JAVA_IMAGE java $JVM_OPTS -jar /app.jar
    log "启动 $svc..."
    sleep 5
done

# ========== 5. 等待服务就绪 ==========
log "等待服务就绪..."
wait_http() {
    for i in $(seq 1 60); do curl -sf "$1" >/dev/null 2>&1 && return 0; sleep 2; done
    return 1
}
wait_http "http://localhost:8080/api/product/page?pageSize=1" && log "Gateway 就绪" || warn "Gateway 未就绪"

# ========== 6. 构建前端 ==========
log "构建前端..."
cd "$PROJECT_DIR/biyesheji-frontend"
if ! command -v npm >/dev/null 2>&1; then
    warn "未安装 npm, 跳过前端构建"
else
    npm install --silent 2>/dev/null || true
    npm run build 2>&1 | tail -3
    log "前端构建完成"
fi

# ========== 7. 配置 Nginx ==========
log "配置 Nginx..."
if command -v nginx >/dev/null 2>&1; then
    sudo cp -f "$PROJECT_DIR/deploy/e5-nginx.conf" /etc/nginx/conf.d/biyesheji.conf 2>/dev/null && \
        sudo nginx -t && sudo nginx -s reload && \
        log "Nginx 配置完成" || warn "Nginx 配置失败，请手动检查"
else
    warn "未安装 nginx，请手动安装: apt install nginx -y"
    warn "然后复制配置: cp deploy/e5-nginx.conf /etc/nginx/conf.d/biyesheji.conf"
fi

# ========== 8. 配置 frpc ==========
log "配置 frpc..."
FRP_DIR="$PROJECT_DIR/frp"
mkdir -p "$FRP_DIR"
cp -f "$PROJECT_DIR/deploy/e5-frpc.toml" "$FRP_DIR/frpc.toml"

if [ -f "$HOME/frp_0.54.0_linux_amd64/frpc" ]; then
    # 已有 frpc
    nohup "$HOME/frp_0.54.0_linux_amd64/frpc" -c "$FRP_DIR/frpc.toml" > "$FRP_DIR/frpc.log" 2>&1 &
    log "frpc 已启动"
else
    warn "frpc 未安装，请手动安装并配置: deploy/e5-frpc.toml"
fi

# ========== 9. 验证 ==========
echo ""
echo "============================================"
echo " 部署完成!"
echo "============================================"
echo ""
echo "运行状态:"
docker ps --format "table {{.Names}}\t{{.Status}}" --filter "name=biyesheji-"
echo ""
echo "本地测试:"
echo "  curl http://localhost/api/product/page?pageSize=2"
echo "  curl http://localhost/"
echo ""
echo "如 frpc 已配置, 外网地址: https://execute42.top"
echo ""

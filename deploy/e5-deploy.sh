#!/bin/bash
# ============================================================
# PhoneMall E5 一键部署脚本 v2
# 用法: bash e5-deploy.sh [skip-build]
#   skip-build - 跳过 Maven 编译（jar 已存在时用）
# ============================================================
set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[..]${NC} $1"; }
err()   { echo -e "${RED}[!!]${NC} $1"; exit 1; }

PROJECT_DIR=$(cd "$(dirname "$0")/.." && pwd)
CURRENT_USER=$(whoami)
SKIP_BUILD=false
[ "$1" = "skip-build" ] && SKIP_BUILD=true

echo "============================================"
echo " PhoneMall E5 一键部署 v2"
echo " 项目目录: $PROJECT_DIR"
echo " 当前用户: $CURRENT_USER"
echo "============================================"
echo ""

# ========== 1. 检查依赖 ==========
log "检查依赖..."
command -v docker >/dev/null 2>&1 || err "请先安装 Docker"
docker compose version >/dev/null 2>&1 || err "请安装 docker-compose: apt install docker-compose-v2"

# ========== 2. 启动中间件 ==========
log "启动中间件 (MySQL, Redis, RabbitMQ, Nacos)..."
cd "$PROJECT_DIR/docker"
docker compose -f docker-compose.e5.yml up -d

log "等待 MySQL 就绪..."
for i in $(seq 1 60); do
    docker exec biyesheji-mysql mysqladmin ping -uroot -proot123 --silent 2>/dev/null && break
    sleep 1
done
docker exec biyesheji-mysql mysqladmin ping -uroot -proot123 --silent 2>/dev/null \
    && log "MySQL 就绪" || warn "MySQL 可能未就绪"

for pair in "http://localhost:15672 RabbitMQ" "http://localhost:8848/nacos Nacos"; do
    url=$(echo $pair | awk '{print $1}'); name=$(echo $pair | awk '{print $2}')
    ok=false
    for i in $(seq 1 40); do curl -sf "$url" >/dev/null 2>&1 && { ok=true; break; }; sleep 1; done
    $ok && log "$name 就绪" || warn "$name 可能未就绪"
done

# 确保数据库表结构完整（补充 color_options / storage_options）
log "检查数据库结构..."
docker exec biyesheji-mysql mysql -uroot -proot123 biyesheji -e \
    "ALTER TABLE t_product ADD COLUMN color_options JSON" 2>/dev/null || true
docker exec biyesheji-mysql mysql -uroot -proot123 biyesheji -e \
    "ALTER TABLE t_product ADD COLUMN storage_options JSON" 2>/dev/null || true
docker exec biyesheji-mysql mysql -uroot -proot123 biyesheji -e \
    "ALTER TABLE t_shopping_cart ADD COLUMN selected_color VARCHAR(50)" 2>/dev/null || true
docker exec biyesheji-mysql mysql -uroot -proot123 biyesheji -e \
    "ALTER TABLE t_shopping_cart ADD COLUMN selected_storage VARCHAR(50)" 2>/dev/null || true
log "数据库结构检查完成"

# ========== 3. Maven 构建 ==========
if $SKIP_BUILD; then
    log "跳过 Maven 构建 (skip-build)"
else
    log "Maven 构建全部微服务 (Docker Maven JDK17, 可能需要2-3分钟)..."
    docker run --rm \
        -v "$PROJECT_DIR:/app" \
        -v maven-repo:/root/.m2 \
        -w /app \
        maven:3.9-eclipse-temurin-17 mvn package -DskipTests 2>&1 | grep -E "BUILD|ERROR|WARNING.*Could not transfer" || true

    # 修复 Docker 构建产生的 root 属主问题
    for s in user-service product-service order-service gateway-service; do
        [ -f "$PROJECT_DIR/$s/target/$s-1.0.0.jar" ] && sudo chown "$CURRENT_USER:$CURRENT_USER" "$PROJECT_DIR/$s/target/$s-1.0.0.jar" 2>/dev/null || true
    done
    log "构建完成"
fi

# ========== 4. 验证 jar 存在 ==========
for svc in user-service product-service order-service gateway-service; do
    jar="$PROJECT_DIR/$svc/target/$svc-1.0.0.jar"
    [ -f "$jar" ] || err "$jar 不存在, 构建可能失败了。重试: docker run --rm -v $PROJECT_DIR:/app -v maven-repo:/root/.m2 -w /app maven:3.9-eclipse-temurin-17 mvn package -DskipTests"
done
log "所有 jar 文件就绪"

# ========== 5. 启动微服务 ==========
log "启动微服务..."
JVM_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:MaxMetaspaceSize=256m"
JAVA_IMAGE="eclipse-temurin:17-jre-jammy"

for svc in user-service product-service order-service gateway-service; do
    jar="$PROJECT_DIR/$svc/target/$svc-1.0.0.jar"
    extra_env=""
    if [ "$svc" = "order-service" ]; then
        [ -z "$DEEPSEEK_API_KEY" ] && warn "DEEPSEEK_API_KEY 未设置, AI导购将无法使用"
        [ -z "$OPENROUTER_API_KEY" ] && warn "OPENROUTER_API_KEY 未设置, RAG将无法使用"
        extra_env="-e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY:-} -e OPENROUTER_API_KEY=${OPENROUTER_API_KEY:-}"
    fi
    docker rm -f biyesheji-$svc 2>/dev/null || true
    docker run --rm -d --name biyesheji-$svc --network host \
        -v "$jar:/app.jar" \
        $extra_env \
        $JAVA_IMAGE java $JVM_OPTS -jar /app.jar
    log "启动 $svc..."
    sleep 5
done

# ========== 6. 等待服务就绪 ==========
log "等待服务就绪..."
wait_http() { for i in $(seq 1 60); do curl -sf "$1" >/dev/null 2>&1 && return 0; sleep 2; done; return 1; }
wait_http "http://localhost:8080/api/product/page?pageSize=1" && log "Gateway 就绪" || warn "Gateway 未就绪，检查: docker logs biyesheji-gateway-service"

# ========== 7. 构建前端 ==========
log "构建前端..."
cd "$PROJECT_DIR/biyesheji-frontend"
if ! command -v npm >/dev/null 2>&1; then
    warn "未安装 npm, 安装中..."
    curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash - 2>/dev/null
    sudo apt install nodejs -y
    command -v npm >/dev/null 2>&1 || warn "npm 安装失败, 跳过前端构建"
fi
if command -v npm >/dev/null 2>&1; then
    node_version=$(node -v 2>/dev/null | grep -oP '\d+' | head -1)
    if [ "$node_version" -lt 18 ] 2>/dev/null; then
        warn "Node.js 版本过旧 (v$node_version), 升级中..."
        curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash - 2>/dev/null
        sudo apt install nodejs -y
    fi
    npm install 2>&1 | tail -1
    npm run build 2>&1 | tail -5
    log "前端构建完成"
fi

# ========== 8. 配置 Nginx ==========
log "配置 Nginx..."
HOME_DIR="/home/$CURRENT_USER"
sudo rm /etc/nginx/sites-enabled/default 2>/dev/null || true

# 替换 nginx 配置中的路径占位符
sed "s|PROJECT_DIR|$PROJECT_DIR|g" "$PROJECT_DIR/deploy/e5-nginx.conf" \
    | sudo tee /etc/nginx/conf.d/biyesheji.conf > /dev/null

# 确保 nginx 能访问用户目录
sudo chmod o+x "$HOME_DIR" 2>/dev/null || true
sudo chmod -R o+rX "$PROJECT_DIR/biyesheji-frontend/dist" 2>/dev/null || true

sudo nginx -t 2>&1 && sudo nginx -s reload 2>&1 && log "Nginx 配置完成" || warn "Nginx 配置有问题，请手动检查"

# ========== 9. 配置 frpc ==========
log "配置 frpc..."
if command -v systemctl >/dev/null 2>&1; then
    sudo tee /etc/systemd/system/frpc.service > /dev/null << SYSTEMDEOF
[Unit]
Description=Frp Client Service
After=network.target

[Service]
Type=simple
User=$CURRENT_USER
ExecStart=$HOME_DIR/frp_0.54.0_linux_amd64/frpc -c $PROJECT_DIR/deploy/e5-frpc.toml
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
SYSTEMDEOF
    sudo systemctl daemon-reload
    sudo systemctl enable frpc 2>/dev/null || true
    sudo systemctl restart frpc 2>/dev/null && log "frpc systemd 服务已配置" || warn "frpc 未安装，跳过"
else
    warn "无法配置 frpc systemd 服务"
fi

# ========== 10. 验证 ==========
echo ""
echo "============================================"
echo " 部署完成!"
echo "============================================"
echo ""
docker ps --format "table {{.Names}}\t{{.Status}}" --filter "name=biyesheji-" 2>/dev/null
echo ""
echo "本地测试:"
echo "  curl http://localhost/api/product/page?pageSize=2"
echo "  curl http://localhost/"
echo ""
echo "外网: https://execute42.top"
echo ""

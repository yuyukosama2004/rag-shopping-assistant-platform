#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$PROJECT_DIR/.env"
COMPOSE_FILE="${COMPOSE_FILE:-$PROJECT_DIR/docker/docker-compose.infrastructure.yml}"
NETWORK="biyesheji-internal"
JVM_OPTS="-Xms64m -Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Djava.awt.headless=true"
JAVA_IMAGE="eclipse-temurin:17-jre-jammy"

die() { echo "ERROR: $*" >&2; exit 1; }
info() { echo "INFO: $*"; }

load_env() {
  [ -f "$ENV_FILE" ] || die "Missing $ENV_FILE. Copy .env.example to .env and set every required secret."
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
  for key in MYSQL_ROOT_PASSWORD REDIS_PASSWORD JWT_SECRET; do
    [ -n "${!key:-}" ] || die "$key must be set in .env"
  done
  [ "${#JWT_SECRET}" -ge 32 ] || die "JWT_SECRET must contain at least 32 bytes"
}

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

wait_http() {
  local url="$1" name="$2" attempts="${3:-60}"
  for _ in $(seq 1 "$attempts"); do
    curl --fail --silent --show-error "$url" >/dev/null && { info "$name is ready"; return 0; }
    sleep 1
  done
  die "$name did not become ready: $url"
}

start_infra() {
  load_env
  info "Starting MySQL, Redis and Nacos"
  compose up -d
  for _ in $(seq 1 60); do
    docker exec -e "MYSQL_PWD=$MYSQL_ROOT_PASSWORD" biyesheji-mysql mysqladmin ping -uroot --silent >/dev/null 2>&1 && break
    sleep 1
  done
  docker exec -e "MYSQL_PWD=$MYSQL_ROOT_PASSWORD" biyesheji-mysql mysqladmin ping -uroot --silent >/dev/null 2>&1 \
    || die "MySQL did not become ready"
  wait_http "http://127.0.0.1:8848/nacos" "Nacos" 60
}

stop_infra() {
  load_env
  compose down
}

build() {
  info "Building backend with Java 17 and running tests"
  docker run --rm -v "$PROJECT_DIR:/workspace" -v maven-repo:/root/.m2 -w /workspace \
    maven:3.9.9-eclipse-temurin-17 mvn -B clean verify
}

run_service() {
  local service="$1" jar="$PROJECT_DIR/$1/target/$1-1.0.0.jar"
  [ -f "$jar" ] || die "Missing $jar; run ./start.sh build first"
  docker rm -f "biyesheji-$service" >/dev/null 2>&1 || true
  local -a args=(run -d --name "biyesheji-$service" --restart unless-stopped --network "$NETWORK" --env-file "$ENV_FILE" -v "$jar:/app.jar:ro")
  if [ "$service" = "gateway-service" ]; then
    args+=(-p 127.0.0.1:8080:8080)
  fi
  docker "${args[@]}" "$JAVA_IMAGE" java $JVM_OPTS -jar /app.jar >/dev/null
}

start_services() {
  load_env
  for service in user-service product-service order-service gateway-service; do
    run_service "$service"
  done
  wait_http "http://127.0.0.1:8080/api/product/page?pageNum=1&pageSize=1" "Gateway" 90
}

stop_services() {
  for service in user-service product-service order-service gateway-service; do
    docker rm -f "biyesheji-$service" >/dev/null 2>&1 || true
  done
}

status() {
  docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=biyesheji-"
  curl --fail --silent "http://127.0.0.1:8080/api/product/page?pageNum=1&pageSize=1" >/dev/null \
    && info "Gateway request succeeded" || die "Gateway request failed"
}

case "${1:-help}" in
  infra-start) start_infra ;;
  infra-stop) stop_infra ;;
  build) build ;;
  start) start_services ;;
  stop) stop_services ;;
  restart) stop_services; start_services ;;
  status) status ;;
  all) start_infra; build; start_services ;;
  *)
    cat <<'EOF'
Usage: ./start.sh {infra-start|infra-stop|build|start|stop|restart|status|all}

All commands require a populated .env file. Services are attached to the private
Docker network; only the gateway is bound to 127.0.0.1:8080 for Nginx.
EOF
    ;;
esac

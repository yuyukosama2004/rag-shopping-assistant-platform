#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$PROJECT_DIR/.env"
COMPOSE_FILE="${COMPOSE_FILE:-$PROJECT_DIR/docker/docker-compose.infrastructure.yml}"
APP_COMPOSE_FILE="${APP_COMPOSE_FILE:-$PROJECT_DIR/docker/docker-compose.app.yml}"
OBSERVABILITY_COMPOSE_FILE="${OBSERVABILITY_COMPOSE_FILE:-$PROJECT_DIR/docker/docker-compose.observability.yml}"

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
  GATEWAY_HOST_PORT="${GATEWAY_HOST_PORT:-8080}"
  [[ "$GATEWAY_HOST_PORT" =~ ^[0-9]+$ ]] && [ "$GATEWAY_HOST_PORT" -ge 1 ] && [ "$GATEWAY_HOST_PORT" -le 65535 ] \
    || die "GATEWAY_HOST_PORT must be a valid TCP port"
}

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

app_compose() {
  docker compose --env-file "$ENV_FILE" -f "$APP_COMPOSE_FILE" "$@"
}

observability_compose() {
  docker compose --env-file "$ENV_FILE" -f "$OBSERVABILITY_COMPOSE_FILE" "$@"
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
  wait_healthy biyesheji-mysql 60
  wait_healthy biyesheji-redis 30
  wait_healthy biyesheji-nacos 90
}

stop_infra() {
  load_env
  for service in user-service product-service order-service gateway-service; do
    [ "$(docker inspect --format '{{.State.Running}}' "biyesheji-$service" 2>/dev/null || true)" != "true" ] \
      || die "Stop application services before stopping infrastructure"
  done
  compose down
}

build() {
  info "Building backend with Java 17 and running tests"
  docker run --rm -v "$PROJECT_DIR:/workspace" -v maven-repo:/root/.m2 -w /workspace \
    "${MAVEN_IMAGE:-maven:3.9-eclipse-temurin-17}" mvn -B clean verify
}

build_frontend() {
  info "Building frontend with Node 22"
  docker run --rm -v "$PROJECT_DIR/biyesheji-frontend:/workspace" -w /workspace \
    "${NODE_IMAGE:-node:22-bookworm-slim}" sh -c 'npm ci && npm run build'
}

wait_healthy() {
  local container="$1" attempts="${2:-90}" status
  for _ in $(seq 1 "$attempts"); do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)"
    [ "$status" = "healthy" ] && { info "$container is healthy"; return 0; }
    [ "$status" = "exited" ] || [ "$status" = "dead" ] && break
    sleep 1
  done
  docker logs --tail 100 "$container" >&2 || true
  die "$container did not become healthy"
}

start_services() {
  load_env
  start_infra
  for service in user-service product-service order-service gateway-service; do
    [ -f "$PROJECT_DIR/$service/target/$service-1.0.0.jar" ] \
      || die "Missing $service build artifact; run ./start.sh build first"
  done
  info "Building application runtime images"
  app_compose build
  for service in user-service product-service order-service gateway-service; do
    docker rm -f "biyesheji-$service" >/dev/null 2>&1 || true
  done
  info "Starting application services through Docker Compose"
  app_compose up -d
  for service in user-service product-service order-service gateway-service; do
    wait_healthy "biyesheji-$service"
  done
  wait_http "http://127.0.0.1:$GATEWAY_HOST_PORT/api/product/page?pageNum=1&pageSize=1" "Gateway" 30
  start_observability
}

stop_services() {
  [ -f "$ENV_FILE" ] || die "Missing $ENV_FILE"
  app_compose down
}

status() {
  load_env
  app_compose ps
  curl --fail --silent "http://127.0.0.1:$GATEWAY_HOST_PORT/api/product/page?pageNum=1&pageSize=1" >/dev/null \
    && info "Gateway request succeeded" || die "Gateway request failed"
}

start_observability() {
  load_env
  info "Starting Prometheus, Alertmanager and host/container exporters"
  observability_compose up -d
  wait_http "http://127.0.0.1:${PROMETHEUS_HOST_PORT:-19090}/-/ready" "Prometheus" 60
  wait_http "http://127.0.0.1:${ALERTMANAGER_HOST_PORT:-19093}/-/ready" "Alertmanager" 60
}

stop_observability() {
  [ -f "$ENV_FILE" ] || die "Missing $ENV_FILE"
  observability_compose down
}

observability_status() {
  load_env
  observability_compose ps
  curl --fail --silent "http://127.0.0.1:${PROMETHEUS_HOST_PORT:-19090}/-/ready" >/dev/null \
    && info "Prometheus is ready" || die "Prometheus is not ready"
  curl --fail --silent "http://127.0.0.1:${ALERTMANAGER_HOST_PORT:-19093}/-/ready" >/dev/null \
    && info "Alertmanager is ready" || die "Alertmanager is not ready"
}

# shellcheck source=scripts/data-maintenance.sh
. "$PROJECT_DIR/scripts/data-maintenance.sh"
# shellcheck source=scripts/release-maintenance.sh
. "$PROJECT_DIR/scripts/release-maintenance.sh"

case "${1:-help}" in
  infra-start) start_infra ;;
  infra-stop) stop_infra ;;
  build) build ;;
  frontend-build) build_frontend ;;
  start) start_services ;;
  stop) stop_services ;;
  restart) stop_services; start_services ;;
  status) status; release_status ;;
  backup) backup_data ;;
  restore) restore_data "${2:-}" ;;
  install) install_release "${2:-}" ;;
  upgrade) upgrade_release "${2:-}" ;;
  rollback) rollback_release "${2:-}" ;;
  observability-start) start_observability ;;
  observability-stop) stop_observability ;;
  observability-status) observability_status ;;
  all) start_infra; build; build_frontend; start_services ;;
  *)
    cat <<'EOF'
Usage: ./start.sh {infra-start|infra-stop|build|frontend-build|start|stop|restart|status|backup|restore|install|upgrade|rollback|observability-start|observability-stop|observability-status|all}

All commands require a populated .env file. Services are attached to the private
Docker network; only the gateway is bound to its configured loopback port for Nginx.
The frontend is built in the Node 22 container, so no host Node.js installation is required.
EOF
    ;;
esac

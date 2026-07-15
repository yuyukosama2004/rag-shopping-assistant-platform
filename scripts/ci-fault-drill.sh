#!/usr/bin/env bash
set -euo pipefail

wait_container_healthy() {
  local container="$1" attempts="${2:-90}" status
  for _ in $(seq 1 "$attempts"); do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)"
    [ "$status" = healthy ] && return 0
    sleep 1
  done
  echo "ERROR: $container did not become healthy" >&2
  return 1
}

wait_internal_health() {
  local container="$1" port="$2" attempts="${3:-90}"
  for _ in $(seq 1 "$attempts"); do
    docker exec "$container" curl --fail --silent "http://127.0.0.1:$port/actuator/health" >/dev/null 2>&1 && return 0
    sleep 1
  done
  echo "ERROR: $container health endpoint did not recover" >&2
  return 1
}

wait_internal_unhealthy() {
  local container="$1" port="$2" attempts="${3:-30}"
  for _ in $(seq 1 "$attempts"); do
    if ! docker exec "$container" curl --fail --silent "http://127.0.0.1:$port/actuator/health" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "ERROR: $container did not report its dependency failure" >&2
  return 1
}

recover_dependencies() {
  docker start biyesheji-mysql biyesheji-redis biyesheji-nacos >/dev/null 2>&1 || true
}
trap recover_dependencies EXIT

docker stop biyesheji-redis >/dev/null
wait_internal_unhealthy biyesheji-user-service 8081
docker start biyesheji-redis >/dev/null
wait_container_healthy biyesheji-redis 60
wait_internal_health biyesheji-user-service 8081
echo REDIS_FAILURE_AND_RECOVERY_OK

docker stop biyesheji-mysql >/dev/null
wait_internal_unhealthy biyesheji-product-service 8082
docker start biyesheji-mysql >/dev/null
wait_container_healthy biyesheji-mysql 90
wait_internal_health biyesheji-product-service 8082
echo MYSQL_FAILURE_AND_RECOVERY_OK

docker stop biyesheji-nacos >/dev/null
curl --fail --silent 'http://127.0.0.1:18080/api/product/page?pageNum=1&pageSize=1' >/dev/null
docker start biyesheji-nacos >/dev/null
wait_container_healthy biyesheji-nacos 120
for _ in $(seq 1 60); do
  curl --fail --silent 'http://127.0.0.1:18080/api/product/page?pageNum=1&pageSize=1' >/dev/null && break
  sleep 2
done
curl --fail --silent 'http://127.0.0.1:18080/api/product/page?pageNum=1&pageSize=1' >/dev/null
echo NACOS_SHORT_OUTAGE_RECOVERY_OK

docker restart biyesheji-order-service >/dev/null
wait_container_healthy biyesheji-order-service 120
for _ in $(seq 1 60); do
  curl --fail --silent 'http://127.0.0.1:18080/api/shipping-rules' >/dev/null && break
  sleep 2
done
curl --fail --silent 'http://127.0.0.1:18080/api/shipping-rules' >/dev/null
echo ORDER_SERVICE_RESTART_RECOVERY_OK

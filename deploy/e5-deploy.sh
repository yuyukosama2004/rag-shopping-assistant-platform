#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$PROJECT_DIR/.env"
CURRENT_USER="$(id -un)"

[ -f "$ENV_FILE" ] || { echo "Missing $ENV_FILE; copy .env.example first." >&2; exit 1; }
set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a
for key in MYSQL_ROOT_PASSWORD REDIS_PASSWORD JWT_SECRET; do
  [ -n "${!key:-}" ] || { echo "$key must be set" >&2; exit 1; }
done
[ "${#JWT_SECRET}" -ge 32 ] || { echo "JWT_SECRET must contain at least 32 bytes" >&2; exit 1; }

command -v docker >/dev/null || { echo "Docker is required" >&2; exit 1; }
command -v nginx >/dev/null || { echo "Nginx is required" >&2; exit 1; }

COMPOSE_FILE="$PROJECT_DIR/docker/docker-compose.e5.yml" bash "$PROJECT_DIR/start.sh" infra-start
if [ "${1:-}" != "skip-build" ]; then
  bash "$PROJECT_DIR/start.sh" build
fi
COMPOSE_FILE="$PROJECT_DIR/docker/docker-compose.e5.yml" bash "$PROJECT_DIR/start.sh" restart

sed -e "s|PROJECT_DIR|$PROJECT_DIR|g" -e "s|GATEWAY_HOST_PORT|${GATEWAY_HOST_PORT:-8080}|g" "$PROJECT_DIR/deploy/e5-nginx.conf" \
  | sudo tee /etc/nginx/conf.d/biyesheji.conf >/dev/null
sudo nginx -t
sudo nginx -s reload

echo "Deployment completed for user $CURRENT_USER."

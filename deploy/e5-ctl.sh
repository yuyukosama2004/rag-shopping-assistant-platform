#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
run_platform() { COMPOSE_FILE="$PROJECT_DIR/docker/docker-compose.e5.yml" bash "$PROJECT_DIR/start.sh" "$@"; }

case "${1:-}" in
  start) run_platform all ;;
  stop) run_platform stop; run_platform infra-stop ;;
  restart)
    if [ -n "${2:-}" ]; then
      service="${2%-service}-service"
      case "$service" in user-service|product-service|order-service|gateway-service) ;; *) echo "Unknown service: $2" >&2; exit 1 ;; esac
      docker rm -f "biyesheji-$service" >/dev/null 2>&1 || true
      run_platform start
    else
      run_platform restart
    fi
    ;;
  status|check) run_platform status ;;
  logs) docker logs -f --tail 100 "biyesheji-${2:-order}-service" ;;
  *) echo "Usage: e5-ctl.sh {start|stop|restart [user|product|order|gateway]|status|check|logs [service]}" >&2; exit 2 ;;
esac

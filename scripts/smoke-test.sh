#!/usr/bin/env bash
set -euo pipefail

if [ -z "${API_BASE_URL:-}" ]; then
  gateway_host_port="8080"
  if [ -f .env ]; then
    configured_port="$(sed -n 's/^GATEWAY_HOST_PORT=//p' .env | tail -n 1)"
    [ -z "$configured_port" ] || gateway_host_port="$configured_port"
  fi
  API_BASE_URL="http://127.0.0.1:${gateway_host_port}"
fi
WEB_BASE_URL="${WEB_BASE_URL:-http://127.0.0.1}"

die() { echo "ERROR: $*" >&2; exit 1; }
info() { echo "INFO: $*"; }

request() {
  local url="$1" output status
  output="$(mktemp)"
  status="$(curl --silent --show-error --output "$output" --write-out '%{http_code}' "$url")" || {
    rm -f "$output"
    die "Request failed: $url"
  }
  [ "$status" = "200" ] || {
    cat "$output" >&2
    rm -f "$output"
    die "Expected HTTP 200 from $url, received $status"
  }
  cat "$output"
  rm -f "$output"
}

health="$(request "$API_BASE_URL/actuator/health")"
printf '%s' "$health" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"' \
  || die "Gateway health response is not UP"
info "Gateway health check passed"

products="$(request "$API_BASE_URL/api/product/page?pageNum=1&pageSize=1")"
printf '%s' "$products" | grep -q '"code"[[:space:]]*:[[:space:]]*200' \
  || die "Product API did not return a successful business response"
info "Product API check passed"

homepage="$(request "$WEB_BASE_URL/")"
printf '%s' "$homepage" | grep -q '<div id="app"></div>' \
  || die "Frontend entry document is not the expected SPA shell"
info "Frontend entry check passed"

info "Smoke test passed"

#!/usr/bin/env bash

release_dir() {
  printf '%s/.releases' "$PROJECT_DIR"
}

validate_release_tag() {
  local tag="$1"
  [[ "$tag" =~ ^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$ ]] \
    || die "Release tag must use 1-64 letters, numbers, dots, underscores or dashes"
}

current_release_tag() {
  local state="$(release_dir)/current"
  [ -s "$state" ] && cat "$state" || printf 'local'
}

archive_frontend() {
  local tag="$1" archive="$(release_dir)/frontend-$1.tar.gz"
  [ -f "$PROJECT_DIR/biyesheji-frontend/dist/index.html" ] || die "Frontend dist is missing"
  mkdir -p "$(release_dir)"
  [ -f "$archive" ] || tar -C "$PROJECT_DIR/biyesheji-frontend/dist" -czf "$archive" .
}

restore_frontend() {
  local tag="$1" releases temp old archive
  releases="$(release_dir)"
  archive="$releases/frontend-$tag.tar.gz"
  [ -f "$archive" ] || die "Frontend archive is missing for release: $tag"
  temp="$releases/.frontend-$tag-$$"
  old="$releases/.replaced-frontend-$$"
  mkdir "$temp"
  tar -C "$temp" -xzf "$archive"
  [ -f "$temp/index.html" ] || die "Frontend archive is invalid for release: $tag"
  mv "$PROJECT_DIR/biyesheji-frontend/dist" "$old"
  mv "$temp" "$PROJECT_DIR/biyesheji-frontend/dist"
  rm -rf -- "$old"
}

release_health_ok() {
  local service status
  for service in user-service product-service order-service gateway-service; do
    for _ in $(seq 1 90); do
      status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "biyesheji-$service" 2>/dev/null || true)"
      [ "$status" = "healthy" ] && break
      [ "$status" = "exited" ] || [ "$status" = "dead" ] && return 1
      sleep 1
    done
    [ "$status" = "healthy" ] || return 1
  done
  curl --fail --silent "http://127.0.0.1:$GATEWAY_HOST_PORT/api/product/page?pageNum=1&pageSize=1" >/dev/null
}

activate_release() {
  local tag="$1" service
  validate_release_tag "$tag"
  for service in user-service product-service order-service gateway-service; do
    docker image inspect "biyesheji-$service:$tag" >/dev/null 2>&1 \
      || { echo "ERROR: Missing image biyesheji-$service:$tag" >&2; return 1; }
  done
  restore_frontend "$tag"
  export APP_IMAGE_TAG="$tag"
  app_compose up -d --no-build --force-recreate
  release_health_ok
}

build_release() {
  local tag="$1"
  validate_release_tag "$tag"
  build
  build_frontend
  export APP_IMAGE_TAG="$tag"
  app_compose build
  archive_frontend "$tag"
}

install_release() (
  set -euo pipefail
  load_env
  local tag="${1:-$(git -C "$PROJECT_DIR" describe --tags --always --dirty)}" releases
  validate_release_tag "$tag"
  releases="$(release_dir)"
  mkdir -p "$releases"
  start_infra
  build_release "$tag"
  activate_release "$tag" || die "Release failed health checks: $tag"
  printf '%s\n' "$tag" > "$releases/current"
  info "Installed release: $tag"
)

upgrade_release() (
  set -euo pipefail
  load_env
  local tag="${1:-}" previous releases
  [ -n "$tag" ] || die "Usage: ./start.sh upgrade <release-tag>"
  validate_release_tag "$tag"
  releases="$(release_dir)"
  mkdir -p "$releases"
  previous="$(current_release_tag)"
  [ "$tag" != "$previous" ] || die "Release is already active: $tag"
  archive_frontend "$previous"
  backup_data
  start_infra
  build_release "$tag"
  if activate_release "$tag"; then
    printf '%s\n' "$previous" > "$releases/previous"
    printf '%s\n' "$tag" > "$releases/current"
    info "Upgraded release: $previous -> $tag"
    return
  fi
  echo "ERROR: Release $tag failed health checks; restoring $previous" >&2
  activate_release "$previous" || die "Automatic rollback failed; inspect application logs"
  die "Upgrade failed and previous release was restored"
)

rollback_release() (
  set -euo pipefail
  load_env
  local releases current target
  releases="$(release_dir)"
  current="$(current_release_tag)"
  target="${1:-$(cat "$releases/previous" 2>/dev/null || true)}"
  [ -n "$target" ] || die "No previous release recorded; pass an explicit release tag"
  validate_release_tag "$target"
  [ "$target" != "$current" ] || die "Release is already active: $target"
  if activate_release "$target"; then
    printf '%s\n' "$current" > "$releases/previous"
    printf '%s\n' "$target" > "$releases/current"
    info "Rolled back release: $current -> $target"
    return
  fi
  die "Rollback failed health checks; current data was not rolled back"
)

release_status() {
  info "Active release: $(current_release_tag)"
}

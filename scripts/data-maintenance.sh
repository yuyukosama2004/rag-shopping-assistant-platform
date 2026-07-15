#!/usr/bin/env bash

backup_data() (
  set -euo pipefail
  load_env
  command -v openssl >/dev/null || die "openssl is required for encrypted backups"
  [ -n "${BACKUP_ENCRYPTION_PASSWORD:-}" ] || die "BACKUP_ENCRYPTION_PASSWORD must be set in .env"

  local backup_dir timestamp work_dir plain_archive encrypted_archive media_volume retention_days
  backup_dir="${BACKUP_DIR:-$PROJECT_DIR/backups/data}"
  retention_days="${BACKUP_RETENTION_DAYS:-14}"
  media_volume="${PRODUCT_MEDIA_VOLUME:-biyesheji-app_product-media}"
  [[ "$retention_days" =~ ^[0-9]+$ ]] || die "BACKUP_RETENTION_DAYS must be a non-negative integer"
  mkdir -p "$backup_dir"
  backup_dir="$(cd "$backup_dir" && pwd -P)"
  [ "$backup_dir" != "/" ] || die "BACKUP_DIR must not be the filesystem root"
  docker volume inspect "$media_volume" >/dev/null 2>&1 || die "Media volume not found: $media_volume"

  timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
  work_dir="$backup_dir/.tmp-$timestamp-$$"
  plain_archive="$backup_dir/biyesheji-$timestamp.tar.gz"
  encrypted_archive="$plain_archive.enc"
  mkdir "$work_dir"
  trap 'rm -rf -- "$work_dir"; rm -f -- "$plain_archive"' EXIT

  info "Dumping MySQL database"
  docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" biyesheji-mysql \
    mysqldump -uroot --single-transaction --routines --triggers biyesheji > "$work_dir/database.sql"

  info "Archiving product media"
  docker run --rm \
    -v "$media_volume:/data:ro" \
    -v "$work_dir:/backup" \
    "${BACKUP_TOOL_IMAGE:-alpine:3.20}" \
    tar -C /data -czf /backup/media.tar.gz .

  cat > "$work_dir/metadata.env" <<EOF
BACKUP_FORMAT_VERSION=1
CREATED_AT_UTC=$timestamp
SOURCE_GIT_COMMIT=$(git -C "$PROJECT_DIR" rev-parse HEAD 2>/dev/null || echo unknown)
DATABASE_NAME=biyesheji
MEDIA_VOLUME=$media_volume
EOF

  tar -C "$work_dir" -czf "$plain_archive" database.sql media.tar.gz metadata.env
  openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 \
    -pass env:BACKUP_ENCRYPTION_PASSWORD -in "$plain_archive" -out "$encrypted_archive"
  (cd "$backup_dir" && sha256sum "$(basename "$encrypted_archive")" > "$(basename "$encrypted_archive").sha256")
  chmod 600 "$encrypted_archive" "$encrypted_archive.sha256"

  if [ "$retention_days" -gt 0 ]; then
    while IFS= read -r -d '' expired; do
      rm -f -- "$expired" "$expired.sha256"
    done < <(find "$backup_dir" -maxdepth 1 -type f -name 'biyesheji-*.tar.gz.enc' -mtime "+$retention_days" -print0)
  fi
  info "Encrypted backup created: $encrypted_archive"
)

restore_data() (
  set -euo pipefail
  load_env
  command -v openssl >/dev/null || die "openssl is required for restore"
  [ -n "${BACKUP_ENCRYPTION_PASSWORD:-}" ] || die "BACKUP_ENCRYPTION_PASSWORD must be set in .env"
  [ "${RESTORE_CONFIRM:-}" = "RESTORE_BIYESHEJI_DATA" ] \
    || die "Restore replaces the current database and media. Set RESTORE_CONFIRM=RESTORE_BIYESHEJI_DATA to continue."

  local encrypted_archive="${1:-}" checksum_file archive_dir work_dir plain_archive media_volume
  [ -n "$encrypted_archive" ] || die "Usage: RESTORE_CONFIRM=RESTORE_BIYESHEJI_DATA ./start.sh restore <backup.tar.gz.enc>"
  encrypted_archive="$(cd "$(dirname "$encrypted_archive")" && pwd -P)/$(basename "$encrypted_archive")"
  [ -f "$encrypted_archive" ] || die "Backup file not found: $encrypted_archive"
  checksum_file="$encrypted_archive.sha256"
  [ -f "$checksum_file" ] || die "Checksum file not found: $checksum_file"
  archive_dir="$(dirname "$encrypted_archive")"
  (cd "$archive_dir" && sha256sum -c "$(basename "$checksum_file")")

  work_dir="$archive_dir/.restore-$(date -u +%Y%m%dT%H%M%SZ)-$$"
  plain_archive="$work_dir/backup.tar.gz"
  mkdir "$work_dir"
  trap 'rm -rf -- "$work_dir"' EXIT
  openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 \
    -pass env:BACKUP_ENCRYPTION_PASSWORD -in "$encrypted_archive" -out "$plain_archive"
  tar -C "$work_dir" -xzf "$plain_archive"
  [ -s "$work_dir/database.sql" ] || die "Backup does not contain database.sql"
  [ -f "$work_dir/media.tar.gz" ] || die "Backup does not contain media.tar.gz"
  media_volume="${PRODUCT_MEDIA_VOLUME:-biyesheji-app_product-media}"
  docker volume inspect "$media_volume" >/dev/null 2>&1 || die "Media volume not found: $media_volume"

  info "Stopping application services before restore"
  app_compose down
  start_infra
  info "Replacing database"
  docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" biyesheji-mysql \
    mysql -uroot -e 'DROP DATABASE IF EXISTS biyesheji; CREATE DATABASE biyesheji CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
  docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" biyesheji-mysql \
    mysql -uroot biyesheji < "$work_dir/database.sql"

  info "Replacing product media"
  docker run --rm \
    -v "$media_volume:/data" \
    -v "$work_dir:/backup:ro" \
    "${BACKUP_TOOL_IMAGE:-alpine:3.20}" \
    sh -c 'find /data -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +; tar -C /data -xzf /backup/media.tar.gz'
  start_services
  info "Restore completed and services are healthy"
)

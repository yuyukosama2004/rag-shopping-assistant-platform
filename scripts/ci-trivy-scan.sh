#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 || $# -gt 3 ]]; then
  echo "Usage: $0 <fs|image> <target> [ignore-file]" >&2
  exit 2
fi

scan_type="$1"
target="$2"
ignore_file="${3:-}"
cache_dir="${TRIVY_CACHE_DIR:-${RUNNER_TEMP:-/tmp}/trivy-cache}"

case "$scan_type" in
  fs) scanners="vuln,misconfig" ;;
  image) scanners="vuln" ;;
  *)
    echo "Unsupported Trivy scan type: $scan_type" >&2
    exit 2
    ;;
esac

if [[ -n "$ignore_file" && ! -f "$ignore_file" ]]; then
  echo "Trivy ignore file does not exist: $ignore_file" >&2
  exit 1
fi

mkdir -p "$cache_dir"
args=(
  "$scan_type"
  --cache-dir "$cache_dir"
  --scanners "$scanners"
  --severity HIGH,CRITICAL
  --ignore-unfixed
  --no-progress
  --skip-version-check
  --timeout "${TRIVY_TIMEOUT:-15m}"
  --exit-code 1
)

if [[ -n "$ignore_file" ]]; then
  args+=(--ignorefile "$ignore_file")
fi

exec trivy "${args[@]}" "$target"

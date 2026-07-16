#!/usr/bin/env bash
set -euo pipefail

readonly TRIVY_VERSION="0.69.3"
readonly TRIVY_SHA256="1816b632dfe529869c740c0913e36bd1629cb7688bd5634f4a858c1d57c88b75"
readonly GITLEAKS_VERSION="8.24.3"
readonly GITLEAKS_SHA256="9991e0b2903da4c8f6122b5c3186448b927a5da4deef1fe45271c3793f4ee29c"

if [[ $# -eq 0 ]]; then
  echo "Usage: $0 <trivy|gitleaks> [...]" >&2
  exit 2
fi

if [[ "$(uname -s)" != "Linux" || "$(uname -m)" != "x86_64" ]]; then
  echo "Security tool bootstrap currently supports Linux x86_64 runners only." >&2
  exit 1
fi

install_root="${RUNNER_TEMP:-/tmp}/biyesheji-ci-security-tools"
mkdir -p "$install_root"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

download_and_install() {
  local tool="$1"
  local version="$2"
  local sha256="$3"
  local url="$4"
  local archive="$tmp_dir/${tool}.tar.gz"
  local -a curl_args=(--fail --location --silent --show-error --retry 3 --retry-all-errors)

  if [[ -n "${GH_TOKEN:-}" ]]; then
    curl_args+=(--header "Authorization: Bearer ${GH_TOKEN}")
  fi

  echo "Installing ${tool} ${version} from its pinned GitHub release."
  curl "${curl_args[@]}" --output "$archive" "$url"
  printf '%s  %s\n' "$sha256" "$archive" | sha256sum --check --status
  tar -xzf "$archive" -C "$tmp_dir" "$tool"
  install -m 0755 "$tmp_dir/$tool" "$install_root/$tool"
}

ensure_trivy() {
  if command -v trivy >/dev/null 2>&1 && trivy --version | grep -Fq "Version: ${TRIVY_VERSION}"; then
    echo "Using preinstalled Trivy ${TRIVY_VERSION}."
    return
  fi

  download_and_install \
    trivy \
    "$TRIVY_VERSION" \
    "$TRIVY_SHA256" \
    "https://github.com/aquasecurity/trivy/releases/download/v${TRIVY_VERSION}/trivy_${TRIVY_VERSION}_Linux-64bit.tar.gz"
}

ensure_gitleaks() {
  if command -v gitleaks >/dev/null 2>&1 && [[ "$(gitleaks version)" == "$GITLEAKS_VERSION" ]]; then
    echo "Using preinstalled Gitleaks ${GITLEAKS_VERSION}."
    return
  fi

  download_and_install \
    gitleaks \
    "$GITLEAKS_VERSION" \
    "$GITLEAKS_SHA256" \
    "https://github.com/gitleaks/gitleaks/releases/download/v${GITLEAKS_VERSION}/gitleaks_${GITLEAKS_VERSION}_linux_x64.tar.gz"
}

for tool in "$@"; do
  case "$tool" in
    trivy) ensure_trivy ;;
    gitleaks) ensure_gitleaks ;;
    *)
      echo "Unsupported security tool: $tool" >&2
      exit 2
      ;;
  esac
done

if [[ -n "${GITHUB_PATH:-}" ]]; then
  echo "$install_root" >> "$GITHUB_PATH"
fi

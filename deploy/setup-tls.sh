#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOMAIN="${1:-}"
EMAIL="${2:-}"

[ "$(id -u)" -eq 0 ] || { echo "Run this script as root." >&2; exit 1; }
[[ "$DOMAIN" =~ ^[A-Za-z0-9.-]+\.[A-Za-z]{2,}$ ]] || { echo "Usage: sudo ./deploy/setup-tls.sh shop.example.com owner@example.com" >&2; exit 1; }
[[ "$EMAIL" == *@*.* ]] || { echo "A valid certificate contact email is required." >&2; exit 1; }
command -v nginx >/dev/null || { echo "nginx is required" >&2; exit 1; }
command -v certbot >/dev/null || { echo "certbot is required" >&2; exit 1; }
[ -f "$PROJECT_DIR/.env" ] || { echo "Missing $PROJECT_DIR/.env" >&2; exit 1; }

set -a
# shellcheck disable=SC1091
. "$PROJECT_DIR/.env"
set +a
GATEWAY_HOST_PORT="${GATEWAY_HOST_PORT:-8080}"
mkdir -p /var/www/certbot

bootstrap="/etc/nginx/conf.d/biyesheji-acme.conf"
cat > "$bootstrap" <<EOF
server {
    listen 80;
    server_name $DOMAIN;
    location ^~ /.well-known/acme-challenge/ { root /var/www/certbot; }
    location / { return 404; }
}
EOF
nginx -t
nginx -s reload

mkdir -p /etc/letsencrypt/renewal-hooks/deploy
cat > /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh <<'EOF'
#!/usr/bin/env sh
nginx -t && nginx -s reload
EOF
chmod 0755 /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh

certbot certonly --webroot -w /var/www/certbot -d "$DOMAIN" \
  --email "$EMAIL" --agree-tos --non-interactive --keep-until-expiring

sed -e "s|DOMAIN|$DOMAIN|g" \
    -e "s|PROJECT_DIR|$PROJECT_DIR|g" \
    -e "s|GATEWAY_HOST_PORT|$GATEWAY_HOST_PORT|g" \
    "$PROJECT_DIR/deploy/production-nginx.conf.template" > /etc/nginx/conf.d/biyesheji.conf
install -m 0644 "$PROJECT_DIR/deploy/biyesheji-proxy-headers.conf" /etc/nginx/conf.d/biyesheji-proxy-headers.conf
rm -f "$bootstrap"
nginx -t
nginx -s reload

echo "HTTPS enabled for https://$DOMAIN"
echo "Verify automatic renewal with: certbot renew --dry-run"

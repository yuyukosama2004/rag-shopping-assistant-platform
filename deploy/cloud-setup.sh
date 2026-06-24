#!/bin/bash
# ============================================================
# 阿里云服务器一键部署脚本
# 在云服务器 (8.153.98.251) 上以 root 执行
# ============================================================
set -e

echo "=== [1/5] 检查 frps 运行状态 ==="
if systemctl is-active --quiet frps 2>/dev/null; then
    echo "frps 已运行"
else
    echo "启动 frps..."
    # 假设 frp 安装在 /root/frp_0.54.0_linux_amd64
    cd /root/frp_0.54.0_linux_amd64 2>/dev/null || cd /opt/frp* 2>/dev/null
    if [ -f frps.toml ]; then
        nohup ./frps -c frps.toml > frps.log 2>&1 &
        echo "frps started"
    else
        echo "ERROR: frps 未安装或找不到 frps.toml"
    fi
fi

echo ""
echo "=== [2/5] 部署 frps 配置 ==="
cat > /tmp/frps.toml << 'FRPSEOF'
bindPort = 7000
FRPSEOF
echo "frps.toml 已更新"

echo ""
echo "=== [3/5] 部署 nginx 配置 ==="
cat > /etc/nginx/conf.d/execute42.conf << 'NGXEOF'
server {
    listen 80;
    server_name execute42.top mall.execute42.top api.execute42.top;

    location / {
        proxy_pass http://127.0.0.1:6080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }
}
NGXEOF

nginx -t && nginx -s reload 2>&1 || nginx 2>&1
echo "nginx 配置已部署"

echo ""
echo "=== [4/5] 配置防火墙（阿里云安全组） ==="
echo "请在阿里云控制台 → ECS → 安全组 中开放以下入方向端口:"
echo "  - 80   (HTTP)"
echo "  - 443  (HTTPS)"
echo "  - 7000 (frps)"
echo "  - 6080 (Web tunnel)"
echo ""
echo "或使用命令行 (如果安装了 aliyun CLI):"
echo "  aliyun ecs AuthorizeSecurityGroup --RegionId cn-hangzhou --SecurityGroupId <sg-id> --IpProtocol tcp --PortRange 6080/6080 --SourceCidrIp 0.0.0.0/0"

echo ""
echo "=== [5/5] 验证 ==="
sleep 3
echo "测试公网访问 (通过 frp 隧道):"
curl -s http://127.0.0.1:6080/api/product/page?pageSize=1 2>/dev/null | head -c 100 || echo "隧道未通，检查 frps 是否已重启"
echo ""

echo ""
echo "=== 部署完成 ==="
echo "公网地址: http://execute42.top"
echo "API 地址: http://execute42.top/api/product/page"
echo "Windows 前端 .env.development: VITE_API_BASE_URL=http://192.168.1.123:8080"

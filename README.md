# PhoneMall — RAG Shopping Assistant Platform

<p align="center">
  基于 Spring Cloud、Vue 3 与真实商品数据构建的自托管商城和 RAG 智能导购
</p>

<p align="center">
  <a href="https://www.execute42.top/projects/phonemall/">公开案例</a>
</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-17-orange">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen">
  <img alt="Spring Cloud" src="https://img.shields.io/badge/Spring%20Cloud-2025.0.3-green">
  <img alt="Vue" src="https://img.shields.io/badge/Vue-3.5-42b883">
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-8.0-blue">
  <img alt="Redis" src="https://img.shields.io/badge/Redis-7-red">
</p>

## 项目介绍

PhoneMall 是一个面向手机商品销售场景的前后端分离电商平台。项目采用 Spring Cloud 微服务架构，将用户、商品、订单等业务拆分为独立服务，并通过 API 网关统一提供访问入口。

系统实现了消费者注册登录、商品查询、购物车、收货地址、事务化下单、SKU 库存管理和
`COD/OFFLINE` 履约，也提供店主/店员权限、商品上架、库存调整、订单处理、发货、售后记录和
店铺配置等商家工作台能力。同时接入大语言模型与向量检索，为用户提供基于真实可售商品的
AI 智能导购服务。

当前发布版本为 `v0.8.0-alpha.2`。内部自动化、服务器升级、备份、监控和回滚验收已经通过；
真实商家试点、公网域名 TLS、外部告警送达和真实支付仍未完成，因此继续使用 Alpha 标识。

## 项目状态

- 发布：[`v0.8.0-alpha.2`](https://github.com/yuyukosama2004/rag-shopping-assistant-platform/releases/tag/v0.8.0-alpha.2)
- 阶段：Alpha，已达到单商家非生产数据试点候选
- 部署：单商家、单店铺、单实例自托管
- CI：GitHub 托管 `ubuntu-latest`，13 项必需检查保护 `main`
- 已验证：完整交易 E2E、并发库存、安全扫描、故障恢复、加密备份、监控和版本回滚
- 未完成：真实商家试点、公网 TLS 现场验收、外部告警接收和在线支付
- 公开说明：[项目案例与工程证据](https://www.execute42.top/projects/phonemall/)

## 功能模块

### 用户中心

- 用户注册与登录
- JWT 身份认证
- Access Token 与 Refresh Token
- 用户资料查看和修改
- 收货地址新增、修改、删除
- 默认收货地址设置

### 商品中心

- 商品分页展示
- 商品详情查看
- 商品名称与关键词搜索
- 品牌、分类和价格区间筛选
- 按销量或价格排序
- 热门商品推荐
- Redis 商品详情缓存
- 热门商品与筛选项缓存

### 购物车

- 商品加入购物车
- 商品数量修改
- 商品规格选择
- 单项删除与批量删除
- 商品选中状态切换
- 全选与取消全选
- 购物车数量统计

### 订单中心

- 提交订单
- 同步事务创建订单与订单明细
- Redis Lua 原子预扣库存
- MySQL 库存确认
- 防止重复提交
- 订单详情与分页查询
- 货到付款/线下收款状态记录
- 商家确认、发货与完成履约
- 用户取消订单
- 未支付订单超时处理
- 库存自动恢复

### AI 智能导购

- 根据自然语言描述购机需求
- 商品信息向量化
- 基于余弦相似度检索相关商品
- RAG 检索增强生成
- DeepSeek 生成推荐结果
- SSE 流式输出
- 用户历史对话记录
- AI 自动丰富商品描述

### 网关与服务治理

- Spring Cloud Gateway 统一入口
- 面向单商家固定拓扑的显式上游路由
- JWT 全局鉴权
- Sentinel 网关限流
- 跨域访问配置
- 基于容器 DNS 的私有网络路由

## 系统架构

```mermaid
flowchart LR
    U[浏览器用户] --> F[Vue 3 前端]
    F --> G[Spring Cloud Gateway]

    G --> US[user-service]
    G --> PS[product-service]
    G --> OS[order-service]

    US --> DB[(MySQL)]
    PS --> DB
    OS --> DB

    US --> R[(Redis)]
    PS --> R
    OS --> R

    OS --> AI[DeepSeek API]
    OS --> EMB[OpenRouter Embedding API]

```

## 技术栈

### 后端

| 技术 | 用途 |
| --- | --- |
| Java 17 | 后端开发语言 |
| Spring Boot 3.5.16 | 微服务基础框架 |
| Spring Cloud 2025.0.3 | 微服务治理 |
| Spring Cloud Gateway | API 网关与统一路由 |
| Sentinel | 网关流量控制 |
| MyBatis-Plus | 数据持久层与分页查询 |
| MySQL 8.0 | 业务数据存储 |
| Redis / Redisson | 缓存、幂等和库存预扣 |
| JJWT | JWT 生成与解析 |
| Knife4j | 在线接口文档 |
| Hutool | 常用工具与数据处理 |
| Lombok | 简化 Java 代码 |

### 前端

| 技术 | 用途 |
| --- | --- |
| Vue 3 | 前端框架 |
| TypeScript | 类型支持 |
| Vite | 开发与构建工具 |
| Element Plus | UI 组件库 |
| Pinia | 状态管理 |
| Vue Router | 前端路由 |
| Axios | HTTP 请求 |

### AI 能力

| 技术 | 用途 |
| --- | --- |
| DeepSeek API | 推荐内容生成与商品描述生成 |
| OpenRouter Embedding API | 商品和查询文本向量化 |
| RAG | 根据用户需求检索相关商品 |
| SSE | AI 回答流式传输 |

### 部署与运维

- Docker
- Docker Compose
- Nginx
- FRP
- Shell 脚本

## 项目结构

```text
rag-shopping-assistant-platform
├── biyesheji-common/          # 公共模块：实体、DTO、VO、工具类、异常处理
├── gateway-service/           # API 网关、JWT 鉴权、Sentinel 限流
├── user-service/              # 用户、登录、个人资料和收货地址
├── product-service/           # 商品查询、搜索、筛选和缓存
├── order-service/             # 购物车、订单、库存和 AI 导购
├── biyesheji-frontend/        # Vue 3 前端项目
├── sql/
│   ├── init.sql               # 数据库表结构
│   └── mock_data.sql          # 示例商品与业务数据
├── docker/                    # MySQL、Redis 与应用编排文件
├── deploy/                    # Nginx、FRP 和服务器部署脚本
├── docs/                      # 项目说明与辅助脚本
├── scripts/                   # 冒烟、数据维护、发布和 CI 辅助脚本
├── start.sh                   # 项目管理脚本
└── pom.xml                    # Maven 父工程
```

## 服务端口

| 服务 | 默认端口 | 说明 |
| --- | ---: | --- |
| gateway-service | 8080 | API 统一入口 |
| user-service | 8081 | 用户服务 |
| product-service | 8082 | 商品服务 |
| order-service | 8083 | 订单与 AI 服务 |
| 前端开发服务器 | 5173 | Vue 开发环境 |
| MySQL | 3306 | 关系型数据库 |
| Redis | 6379 | 缓存服务 |

## 环境要求

运行项目前，请准备以下环境：

- Git
- Docker
- Docker Compose
- Java 17 或更高版本
- Maven
- Node.js 与 npm

使用项目自带的 Docker 构建脚本时，本机可以不单独安装 Maven 和 Java 17，但需要能够正常运行 Docker。

## 快速启动

### 1. 克隆项目

```bash
git clone https://github.com/yuyukosama2004/rag-shopping-assistant-platform.git
cd rag-shopping-assistant-platform
```

### 2. 配置运行环境

```bash
cp .env.example .env
# 编辑 .env：必须设置 MYSQL_ROOT_PASSWORD、REDIS_PASSWORD、JWT_SECRET、OWNER_INIT_TOKEN；
# Docker 部署还应保留 MYSQL_HOST=mysql、REDIS_HOST=redis。
```

`.env` 不会提交到 Git。JWT 密钥至少为 32 字节；`OWNER_INIT_TOKEN` 是仅用于首次创建店主的随机密钥，创建成功后接口会永久拒绝再次初始化。不要把真实 AI Key、初始化令牌或数据库口令写回配置文件。

Docker Compose 默认启用 `prod` 配置：关闭 API 文档、禁止 Flyway clean，并输出 ECS JSON 日志。直接从 IDE 或命令行启动服务时默认使用 `dev`；自动化测试可通过 `SPRING_PROFILES_ACTIVE=test` 显式启用测试配置。不要在公网部署中改用 `dev`。

### 3. 启动基础设施

项目提供了 MySQL 和 Redis 的 Docker Compose 配置：

```bash
docker compose -f docker/docker-compose.infrastructure.yml up -d
```

也可以使用项目脚本：

```bash
chmod +x start.sh
./start.sh infra-start
```

首次创建 MySQL 容器时，会自动执行：

```text
sql/init.sql
sql/mock_data.sql
```

### 4. 配置 AI 密钥

AI 导购需要设置 DeepSeek 和 OpenRouter API 密钥。

Linux 或 macOS：

```bash
export DEEPSEEK_API_KEY="你的 DeepSeek API Key"
export OPENROUTER_API_KEY="你的 OpenRouter API Key"
```

Windows PowerShell：

```powershell
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
$env:OPENROUTER_API_KEY="你的 OpenRouter API Key"
```

未配置 AI 密钥时，用户、商品、购物车和订单等基础电商功能仍可独立运行。

### 4.1 配置商品媒体存储（可选）

默认使用 Docker 命名卷持久化商品图片。若商家已有 AWS S3、MinIO、Cloudflare R2
或其他 S3 兼容对象存储，可在 `.env` 中切换：

```dotenv
MEDIA_STORAGE_TYPE=s3
MEDIA_S3_ENDPOINT=https://s3-compatible.example
MEDIA_S3_REGION=us-east-1
MEDIA_S3_BUCKET=shop-media
MEDIA_S3_ACCESS_KEY=replace-me
MEDIA_S3_SECRET_KEY=replace-me
MEDIA_S3_PATH_STYLE=true
MEDIA_S3_PREFIX=product-media
```

AWS S3 可将 `MEDIA_S3_ENDPOINT` 留空，并按供应商要求设置 Region。应用只向消费者返回
`/api/media/<id>`，Bucket 无需公开；访问密钥只授予指定 Bucket/前缀的读取、写入和删除权限。
内置 `backup`/`restore` 会通过固定版本 AWS CLI 镜像同步对象，仍建议在对象存储侧启用版本控制、
生命周期和跨位置备份。切换存储类型前须先迁移现有对象，否则旧图片 URL 虽保持不变但对象不存在。

### 4.2 配置订单 Webhook（可选）

商家可在 `.env` 中启用订单事件通知：

```dotenv
ORDER_WEBHOOK_ENABLED=true
ORDER_WEBHOOK_URL=https://merchant.example/webhooks/orders
ORDER_WEBHOOK_SECRET=请使用至少32字符的独立随机签名密钥
```

系统对订单创建、取消、确认收款、发货和完成事件使用事务 Outbox 持久化，并在失败时指数退避重试。
请求包含 `X-Webhook-Id`、`X-Webhook-Event` 和
`X-Webhook-Signature: sha256=<HMAC-SHA256>`；签名原文是未经修改的 JSON 请求体。
接收方应按 `X-Webhook-Id` 幂等处理。Webhook 负载只含事件、订单号和状态，不包含密码、Token、
收货信息或商家内部备注。正式环境应使用 HTTPS，签名密钥不得与 JWT、数据库或备份密钥复用。

### 5. 构建后端

使用本机 Maven：

```bash
./mvnw verify
```

使用项目脚本和 Docker Maven 镜像：

```bash
./start.sh build
```

### 6. 启动微服务

使用项目脚本：

```bash
./start.sh start
```

脚本会依次启动：

1. user-service
2. product-service
3. order-service
4. gateway-service

也可以在不同终端中手动启动：

```bash
java -jar user-service/target/user-service-1.0.0.jar
java -jar product-service/target/product-service-1.0.0.jar
java -jar order-service/target/order-service-1.0.0.jar
java -jar gateway-service/target/gateway-service-1.0.0.jar
```

### 7. 启动前端

```bash
cd biyesheji-frontend
npm ci
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

后端统一入口：

```text
http://localhost:8080
```

## 项目脚本

```bash
./start.sh <command>
```

| 命令 | 说明 |
| --- | --- |
| `infra-start` | 启动 MySQL 和 Redis |
| `infra-stop` | 停止基础设施 |
| `build` | 构建全部后端模块 |
| `frontend-build` | 使用 Node 22 Docker 镜像构建前端静态资源 |
| `start` | 通过应用 Compose 构建并启动四个微服务 |
| `stop` | 停止并删除应用服务容器，不删除数据卷 |
| `restart` | 重建并重启应用服务容器 |
| `status` | 查看容器和服务状态 |
| `backup` | 创建数据库与商品媒体的 AES-256 加密备份并执行保留策略 |
| `restore <file>` | 校验并恢复加密备份；必须显式设置恢复确认变量 |
| `install [tag]` | 构建前后端、生成版本化镜像并安装指定版本 |
| `upgrade <tag>` | 先加密备份，再构建和切换版本；健康失败自动回滚 |
| `rollback [tag]` | 切换到上一版本或指定的已保留镜像和前端产物 |
| `observability-start` | 启动 Prometheus、Alertmanager 与主机/容器指标采集 |
| `observability-stop` | 停止监控组件，保留指标和告警数据卷 |
| `observability-status` | 检查监控容器及 Prometheus/Alertmanager 就绪状态 |
| `all` | 启动基础设施、构建后端并启动微服务 |

首次运行建议依次执行：

```bash
./start.sh infra-start
./start.sh build
./start.sh frontend-build
./start.sh start
```

`start` 会复用 `docker/docker-compose.app.yml`，并等待四个应用服务的
`/actuator/health` 健康检查通过。网关仅绑定在 `127.0.0.1`；如宿主机的
8080 已被占用，请在 `.env` 设置 `GATEWAY_HOST_PORT`，并让 Nginx 指向该端口。

### 备份与恢复

在 `.env` 中设置独立的强随机 `BACKUP_ENCRYPTION_PASSWORD`。备份默认写入
`backups/data`，同时包含 MySQL 数据和商品媒体，并生成 SHA-256 校验文件；
`BACKUP_RETENTION_DAYS` 默认为 14 天。

```bash
./start.sh backup
```

恢复会替换当前数据库和商品媒体，脚本默认拒绝执行。确认文件和环境后使用：

```bash
RESTORE_CONFIRM=RESTORE_BIYESHEJI_DATA \
  ./start.sh restore backups/data/biyesheji-YYYYMMDDTHHMMSSZ.tar.gz.enc
```

恢复前应先额外保留当前备份。脚本会校验 SHA-256、解密归档、停止应用服务、恢复数据，
随后重新启动并等待全部健康检查通过。不要把备份密码、解密后的归档或 `.env` 提交到 Git。

### 版本化安装、升级与回滚

首次版本化安装可使用 Git Tag 作为版本号：

```bash
./start.sh install v0.8.0-alpha.2
```

升级前先将仓库切换到目标 Tag/提交，再执行：

```bash
./start.sh upgrade v0.8.0-alpha.2
```

升级会先调用加密备份，保留旧版本的四个应用镜像和前端归档，再构建新版本并等待健康检查。
新版本失败时会自动重新激活上一版本。手工回滚使用：

```bash
./start.sh rollback                 # 上一版本
./start.sh rollback v0.8.0-alpha.1  # 指定已保留版本
```

Flyway 数据库迁移只向前执行，`rollback` 不会降级数据库。发布新迁移时必须满足以下兼容规则：

| 变更类型 | 直接升级 | 应用回滚 |
| --- | --- | --- |
| 新增表、可空列、带默认值列 | 支持 | 支持，旧应用忽略新增结构 |
| 列重命名、删除列、收紧非空约束 | 禁止直接发布 | 不支持，必须拆成多阶段迁移 |
| 数据回填 | 先发布兼容结构，再异步回填 | 回填期间需保持旧字段可读 |

执行升级前应确认磁盘空间足够同时保留新旧镜像、前端归档和至少一份加密备份。

### 生产 HTTPS 与入口限流

为域名配置指向服务器的 DNS A/AAAA 记录，并确保公网仅放行 80/443。安装 Nginx 与 Certbot 后执行：

```bash
sudo ./deploy/setup-tls.sh shop.example.com owner@example.com
sudo certbot renew --dry-run
```

脚本使用 HTTP-01 签发 Let's Encrypt 证书，渲染 `production-nginx.conf.template`，启用 TLS 1.2/1.3、
HSTS、CSP、防嵌入/类型嗅探头，并安装续期后的 Nginx 重载钩子。生产模板对登录、AI、上传、
商品搜索和订单接口分别限流，拒绝从公网访问 `/actuator/`。MySQL、Redis 和网关均继续
只绑定 loopback 或 Docker 私有网络，不能直接映射到公网。

### 监控、告警与结构化日志

`start`、`install` 和成功的 `upgrade` 会自动启动监控栈；也可以单独执行：

```bash
./start.sh observability-start
./start.sh observability-status
```

Prometheus 和 Alertmanager 默认只监听宿主机 `127.0.0.1:19090/19093`，不会暴露到公网。
应用日志使用 ECS JSON 格式，Prometheus 采集四个服务的 JVM、HTTP、聚合健康状态和 AI 费用指标。
聚合健康指标包含数据库、Redis 等依赖状态。主机 CPU/磁盘、
容器内存、5xx 比例、AI 预算和失败率均配置了告警规则。

完整的查看方式、告警演练和外部通知接入见
[`docs/监控与告警指南.md`](docs/监控与告警指南.md)，已完成的服务器演练证据见
[`docs/生产运维验收记录.md`](docs/生产运维验收记录.md)。

质量门禁、试点与发布资料：

- [`docs/质量与安全验收记录.md`](docs/质量与安全验收记录.md)
- [`docs/试点验收手册.md`](docs/试点验收手册.md)
- [`docs/发布说明与已知限制.md`](docs/发布说明与已知限制.md)

### GitHub CI 与分支保护

`.github/workflows/ci.yml` 固定使用 GitHub 托管的 `ubuntu-latest` Runner，不要求部署服务器承担
CI。Pull Request 和 `main` 推送会执行 13 项检查，包括后端、前端、配置、完整 Playwright E2E、
源码密钥/依赖扫描、四个业务镜像以及基础设施和监控镜像扫描。

`main` 已启用严格分支保护：必需检查必须全部通过、分支必须保持线性历史、讨论必须解决，并禁止
强制推送和删除。X99 等部署服务器只负责开发构建和版本部署，不注册 GitHub self-hosted Runner。

### 首次初始化店主

首次部署后，在 HTTPS 已配置的环境中，用 `.env` 中的 `OWNER_INIT_TOKEN` 创建唯一
店主和店铺。该接口仅在尚未存在店主时可调用；成功后即使令牌仍留在环境变量中也会被
永久拒绝。请使用密码管理器生成并保存密码与初始化令牌。

```bash
curl -X POST http://127.0.0.1:${GATEWAY_HOST_PORT:-8080}/api/merchant/initialize \
  -H "Content-Type: application/json" \
  -H "X-Owner-Init-Token: $OWNER_INIT_TOKEN" \
  -d '{"username":"owner","password":"change-this-to-a-strong-password","storeName":"我的店铺"}'
```

初始化后的店主使用现有 `/api/user/login` 登录；`GET /api/store/setting` 为消费者公开
读取店铺信息，`GET` 和 `PUT /api/merchant/store/setting` 仅店主可访问。

## API 路由

网关会按照请求路径将流量转发到不同服务：

| 路径 | 目标服务 |
| --- | --- |
| `/api/user/**` | user-service |
| `/api/product/**` | product-service |
| `/api/order/**` | order-service |

### 用户接口示例

```text
POST /api/user/register
POST /api/user/login
POST /api/user/refresh
GET  /api/user/info
PUT  /api/user/info
GET  /api/user/address
POST /api/user/address
```

### 商品接口示例

```text
GET /api/product/page
GET /api/product/{id}
GET /api/product/hot
GET /api/product/filters
```

商品分页查询示例：

```text
GET /api/product/page?pageNum=1&pageSize=12&brand=Apple&sort=price_asc
```

### 购物车接口示例

```text
POST   /api/order/cart
GET    /api/order/cart
PUT    /api/order/cart/{cartId}
DELETE /api/order/cart/{cartId}
PUT    /api/order/cart/check-all
GET    /api/order/cart/count
```

### 订单接口示例

```text
POST /api/order/submit
GET  /api/order/page
GET  /api/order/{orderNo}
POST /api/order/{orderNo}/pay
POST /api/order/{orderNo}/cancel
```

### AI 导购接口

```text
GET /api/order/ai/chat?query=预算三千元，想要续航好并且适合拍照的手机
```

该接口通过 SSE 持续返回生成内容。

## 接口文档

后端服务启动后，可以访问 Knife4j 接口文档：

```text
用户服务：http://localhost:8081/doc.html
商品服务：http://localhost:8082/doc.html
订单服务：http://localhost:8083/doc.html
```

## 核心业务流程

### 用户登录流程

```text
用户提交账号密码
        ↓
用户服务校验 BCrypt 密码
        ↓
生成 Access Token 和 Refresh Token
        ↓
前端保存 Token
        ↓
后续请求携带 Authorization: Bearer <Token>
        ↓
网关统一完成 JWT 校验
```

### 下单与库存流程

```text
用户提交订单
      ↓
校验商品并计算订单金额
      ↓
Redis Lua 原子预扣库存
      ↓
在同一事务中创建订单和订单明细
      ↓
保持库存预留，支付时确认扣减，取消或超时则释放
```

### AI 导购流程

```text
用户输入购机需求
      ↓
将查询文本转换为向量
      ↓
与商品向量计算余弦相似度
      ↓
召回最相关的商品
      ↓
将商品数据与用户需求组合为提示词
      ↓
调用 DeepSeek 生成推荐内容
      ↓
通过 SSE 流式返回前端
```

## 数据库设计

主要数据表如下：

| 表名 | 说明 |
| --- | --- |
| `t_user` | 用户信息 |
| `t_address` | 收货地址 |
| `t_product` | 手机商品 |
| `t_shopping_cart` | 购物车 |
| `t_order` | 订单主表 |
| `t_order_item` | 订单明细 |
| `t_stock` | 商品库存 |
| `t_ai_conversation` | AI 对话记录 |
| `undo_log` | 分布式事务预留表 |

数据库初始化文件位于：

```text
sql/init.sql
```

示例数据文件位于：

```text
sql/mock_data.sql
```

## 前端页面

前端目前包含以下主要页面：

- 首页
- 用户登录
- 用户注册
- 商品列表
- 商品详情
- 购物车
- 订单结算
- 订单列表
- 订单详情
- 个人账户
- AI 智能导购

## Docker 部署

基础设施低配版：

```bash
docker compose -f docker/docker-compose.infrastructure.yml up -d
```

推荐使用项目脚本启动完整后端。它会先构建 JAR，再构建运行时镜像，并通过
`docker-compose.app.yml` 启动应用服务：

```bash
./start.sh all
```

应用服务与 MySQL、Redis 使用私有 Docker 网络；对宿主机仅暴露可配置的
网关回环端口。Nginx 应将 `/api/` 反向代理到 `127.0.0.1:${GATEWAY_HOST_PORT}`。

从使用 Nacos 的旧版本升级时，`infra-start` 会移除已不再需要的 Nacos 容器，但保留旧的
`nacos-data` 数据卷供人工确认。新版本通过 Docker 私有网络中的固定服务名路由，不再运行注册中心。

部署后可执行冒烟测试。若网关使用非默认端口，显式传入对应地址：

```bash
API_BASE_URL=http://127.0.0.1:18080 WEB_BASE_URL=http://127.0.0.1 ./scripts/smoke-test.sh
```

E5 服务器配置：

```bash
docker compose -f docker/docker-compose.e5.yml up -d
```

停止并删除容器：

```bash
docker compose -f docker/docker-compose.infrastructure.yml down
```

部署相关文件位于：

```text
deploy/
```

其中包含：

- Nginx 配置
- FRP 服务端配置
- FRP 客户端配置
- E5 服务器部署脚本
- 云服务器初始化脚本

## 构建前端生产版本

```bash
cd biyesheji-frontend
npm ci
npm run build
```

构建产物默认生成在：

```text
biyesheji-frontend/dist
```

可使用 Nginx 部署该目录，并将 `/api/` 请求反向代理到网关服务的 `8080` 端口。

## 项目特点

- 使用微服务方式拆分用户、商品和订单业务
- 通过网关统一完成路由、鉴权和限流
- 使用 Redis Lua 实现原子库存操作
- 使用同步事务确保订单落库与库存预留的一致性
- 使用 Redis 缓存提高商品查询效率
- 使用定时任务处理超时订单
- 使用 Vue 3 与 Element Plus 构建完整前端页面
- 将 RAG、向量检索、SSE 和大语言模型集成到传统电商业务
- 提供 Docker、Nginx、FRP 和服务器部署脚本

## 使用说明

本项目是持续开发中的个人全栈 AI 应用，当前以 `v0.8.0-alpha.2` 提供自托管部署能力。
自动化与内部技术验收不能替代真实商家试点或公网生产验收。运行 AI 功能时，第三方模型接口可能
产生费用，请根据对应平台的计费规则合理使用。

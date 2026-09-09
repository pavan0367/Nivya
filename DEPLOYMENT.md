# Nivya Platform — Deployment & Operational Runbook

This document is the authoritative guide for configuring, deploying, operating, backing up, and monitoring the Nivya Family Safety platform across development and production environments.

---

## 1. System Architecture & Topology

Nivya utilizes a multi-tier containerized topology connected via an internal Docker bridge network (`nivya-network`).

```
                              [ Internet / Clients ]
                         (Parent Web App, Android Client)
                                       │
                                       ▼ (Port 80 / 443)
                      ┌─────────────────────────────────┐
                      │    Nginx Reverse Proxy Gateway   │
                      │  (SSL/TLS, Security Headers, Gzip)│
                      └────────────────┬────────────────┘
                                       │ (nivya-network internal)
            ┌──────────────────────────┼──────────────────────────┐
            │ /                        │ /api/, /ws/, /actuator/  │ /healthz
            ▼                          ▼                          ▼
 ┌──────────────────────┐   ┌──────────────────────┐   ┌──────────────────┐
 │    nivya-frontend    │   │    nivya-backend     │   │  Gateway Health  │
 │ (React 18 SPA/Nginx) │   │  (Spring Boot 3.3.5) │   │     (200 OK)     │
 └──────────────────────┘   └──────────┬───────────┘   └──────────────────┘
                                       │
                        ┌──────────────┴──────────────┐
                        ▼                             ▼
             ┌─────────────────────┐       ┌─────────────────────┐
             │     nivya-mysql     │       │     nivya-redis     │
             │     (MySQL 8.0)     │       │    (Redis 7.2 AOF)  │
             │ [mysql_data volume] │       │ [redis_data volume] │
             └─────────────────────┘       └─────────────────────┘
```

### Network Isolation & Port Policy
* **Production (`docker-compose.yml`)**:
  * **Public Ingress**: Only Nginx listens on host ports `80` (HTTP) and `443` (HTTPS).
  * **Database Isolation**: MySQL (`3306`) and Redis (`6379`) do **NOT** publish host ports. They are reachable solely by `backend` through `nivya-network`.
  * **Application Isolation**: Backend (`8080`) and Frontend (`80`) do **NOT** publish host ports. They are reachable solely through the Nginx gateway.
* **Development (`docker-compose.dev.yml`)**:
  * MySQL is bound strictly to `127.0.0.1:3306` (loopback only) for IDE and database client inspection.
  * Redis is bound strictly to `127.0.0.1:6379` (loopback only).
  * Backend is bound to `127.0.0.1:8080` for debugger attachment and direct Postman / Swagger access.
  * Frontend is bound to `127.0.0.1:3000`.

---

## 2. Environment Variables & Secret Management

Configuration is decoupled from code using environment variables.

### Environment Variable Matrix

| Variable | Target Service | Default / Example | Required in Prod? | Description |
|---|---|---|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL | *(random 32 hex)* | **YES** | MySQL administrative root password |
| `MYSQL_DATABASE` | MySQL, Backend | `nivya_db` | No | Application database name |
| `MYSQL_USER` | MySQL, Backend | `nivya_user` | No | Dedicated application user |
| `MYSQL_PASSWORD` | MySQL, Backend | *(random 32 hex)* | **YES** | Dedicated application user password |
| `REDIS_PASSWORD` | Redis, Backend | *(random 32 hex)* | **YES** | Redis authentication password |
| `SPRING_PROFILES_ACTIVE`| Backend | `prod` (`dev` in dev)| No | Active Spring Boot profile |
| `JWT_SECRET` | Backend | *(64+ hex characters)*| **YES** | 256-bit secret key for HMAC-SHA256 tokens |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS` | Backend | `900000` (15m) | No | Access token expiration |
| `JWT_REFRESH_TOKEN_EXPIRATION_MS` | Backend | `604800000` (7d) | No | Refresh token expiration |
| `CORS_ALLOWED_ORIGINS` | Backend | `https://nivya.com` | **YES** | Allowed origins for web clients |
| `PAIRING_CODE_TTL_MINUTES` | Backend | `10` | No | Family pairing code validity |
| `CONVOCATION_VIEW_MODE_DURATION_SECONDS` | Backend | `120` | No | Live viewing window duration |
| `FIREBASE_ENABLED` | Backend | `false` | No | Set `true` when FCM credentials provided |
| `FIREBASE_CREDENTIALS_JSON` | Backend | `""` | Optional | Service account JSON string |
| `HTTP_PORT` | Nginx | `80` | No | Host HTTP port |
| `HTTPS_PORT` | Nginx | `443` | No | Host HTTPS port |
| `DOMAIN_NAME` | Nginx | `nivya.yourdomain.com` | **YES** | Fully qualified production domain |

### Generating Cryptographically Secure Secrets
Run the following commands to generate production secrets:
```bash
# Generate 256-bit JWT Secret Key (64 hex characters)
openssl rand -hex 32

# Generate secure passwords for MySQL Root, App User, and Redis
openssl rand -base64 24
```

### Secret Security Rules
1. **Never commit `.env`**: Live `.env` files are ignored in `.gitignore`. Only commit `.env.example`.
2. **File Permissions**: On Linux production servers, restrict access:
   ```bash
   chmod 600 .env
   chown root:root .env
   ```
3. **CI/CD Integration**: In automated pipelines (GitHub Actions, GitLab CI), inject secrets via repository secrets / masked variables.

---

## 3. Database Setup & Persistence

### Character Set and Collation
The Nivya database requires 4-byte UTF-8 encoding (`utf8mb4`) to handle multi-lingual family names, device names, and emoji character sets without truncation.

Database initialization is handled automatically on initial container startup by [docker/mysql/init.sql](file:///d:/Clg/vs/Nivya/docker/mysql/init.sql):
```sql
CREATE DATABASE IF NOT EXISTS `nivya_db`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON `nivya_db`.* TO 'nivya_user'@'%';
FLUSH PRIVILEGES;
```

### Data Persistence
* Database storage is persisted via the Docker named volume `mysql_data`, mounted to `/var/lib/mysql`.
* Redis data is persisted via `redis_data`, mounted to `/data` with Append-Only File (`AOF`) enabled.
* Destroying and recreating containers (`docker compose down && docker compose up -d`) will **NOT** lose database records as long as the volume is preserved.

---

## 4. Database Migrations (Flyway)

Database schema evolution is managed entirely through **Flyway**:

### Migration File Conventions
Migration files reside in `backend/src/main/resources/db/migration/`:
* Format: `V{Version}__{Description}.sql` (e.g., `V1__init_schema.sql`, `V2__add_telemetry_indices.sql`).
* Baseline version: `0` (`baseline-on-migrate: true`).

### Execution
* Migrations run automatically during Spring Boot backend startup before accepting traffic.
* In production (`application-prod.yml`), Hibernate DDL generation is strictly set to `validate` (`ddl-auto: validate`) to prevent accidental schema modifications.

---

## 5. Backend Startup & Runtime Configuration

### Multi-Stage Build
The backend [docker/backend/Dockerfile](file:///d:/Clg/vs/Nivya/docker/backend/Dockerfile) executes:
1. **Builder Stage (`maven:3.9.6-eclipse-temurin-21`)**:
   * Pre-fetches dependencies using `mvn dependency:go-offline`.
   * Packages production artifact via `mvn clean package -DskipTests`.
2. **Runtime Stage (`eclipse-temurin:21-jre-jammy`)**:
   * Runs as unprivileged service user `nivya:nivya` (UID/GID 1000).
   * Installs `curl` for container health monitoring.
   * Runs with container-aware JVM flags:
     ```text
     -XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Duser.timezone=UTC
     ```

### Health Endpoints
* **Liveness & Readiness Endpoint**: `GET /api/v1/health`
* **Fast Ping**: `GET /api/v1/ping`
* **Spring Actuator**: `GET /actuator/health`

---

## 6. Frontend Startup & Static Serving

The React frontend [docker/frontend/Dockerfile](file:///d:/Clg/vs/Nivya/docker/frontend/Dockerfile) builds an optimized production distribution:
1. **Builder Stage (`node:20-alpine`)**:
   * Executes `npm ci` and `npm run build` using Vite.
2. **Runtime Stage (`nginx:1.27-alpine`)**:
   * Serves static bundle via lightweight internal Nginx on port 80.
   * Enforces SPA fallback routing: `try_files $uri $uri/ /index.html`.
   * Implements immutable cache headers (`Cache-Control: public, max-age=31536000, immutable`) for all hashed assets under `/assets/`.

---

## 7. Nginx Reverse Proxy & API Gateway

The gateway Nginx container coordinates routing:

### Routing Table
| Ingress Path | Upstream Destination | Protocol / Special Handling |
|---|---|---|
| `/` | `frontend_upstream:80` | HTTP/1.1 keepalive, SPA routing |
| `/api/` | `backend_upstream:8080` | REST API reverse proxy, client IP forwarding |
| `/ws/` | `backend_upstream:8080` | WebSocket Upgrade (`$http_upgrade`), 3600s timeouts |
| `/actuator/health` | `backend_upstream:8080` | System health probe |
| `/healthz` | Local Nginx response | Returns `200 "healthy\n"` immediately |

### Security Headers Configured
* `X-Frame-Options: SAMEORIGIN`
* `X-Content-Type-Options: nosniff`
* `X-XSS-Protection: 1; mode=block`
* `Referrer-Policy: strict-origin-when-cross-origin`
* `Permissions-Policy: camera=(), microphone=(), geolocation=()`

---

## 8. Android API Configuration

The Android application communicates with the backend for authentication, pairing, telemetry synchronization, and Convocation push alerts.

### 1. Local Development (Android Studio Emulator)
The Android emulator connects to the host machine loopback using IP `10.0.2.2`:
* **REST Base URL**: `http://10.0.2.2:8080/api/v1/` (direct) or `http://10.0.2.2:80/api/v1/` (via Nginx)
* **WebSocket URL**: `ws://10.0.2.2:8080/ws`

### 2. Physical Device (Local Wi-Fi Testing)
When running on a physical Android device on the same local network:
* Determine host LAN IP (e.g., `192.168.1.100` via `ipconfig` or `ip a`).
* **REST Base URL**: `http://192.168.1.100/api/v1/`
* **WebSocket URL**: `ws://192.168.1.100/ws`
* Ensure the host firewall permits inbound traffic on port 80.

### 3. Production Configuration
* **REST Base URL**: `https://api.nivya.com/api/v1/`
* **WebSocket URL**: `wss://api.nivya.com/ws`

### 4. Network Security Configuration (`res/xml/network_security_config.xml`)
In production release builds, cleartext HTTP is strictly forbidden:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Allow cleartext ONLY in debug builds on local dev domains -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
    <!-- Enforce TLS 1.3 / HTTPS for production -->
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```

---

## 9. HTTPS & Domain Configuration

### 1. DNS Records
Configure your DNS provider with:
* `A` Record: `nivya.yourdomain.com` -> `<SERVER_PUBLIC_IPV4>`
* `A` Record: `api.nivya.com` -> `<SERVER_PUBLIC_IPV4>` (if using subdomains)

### 2. Automated TLS with Certbot (Let's Encrypt)
On your production Linux host:
```bash
# 1. Install Certbot
sudo apt update && sudo apt install -y certbot

# 2. Issue certificate in standalone or webroot mode
sudo certbot certonly --standalone -d nivya.yourdomain.com

# 3. Link or copy certificates into docker/nginx/ssl
sudo cp /etc/letsencrypt/live/nivya.yourdomain.com/fullchain.pem docker/nginx/ssl/cert.pem
sudo cp /etc/letsencrypt/live/nivya.yourdomain.com/privkey.pem docker/nginx/ssl/key.pem
sudo chmod 600 docker/nginx/ssl/key.pem
```

### 3. Nginx HTTPS Configuration
Mount `./docker/nginx/ssl:/etc/nginx/ssl:ro` and uncomment the HTTPS server block in `docker/nginx/nginx.conf`:
```nginx
server {
    listen 443 ssl http2;
    server_name nivya.yourdomain.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # HSTS Header (Strict-Transport-Security)
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;

    # (Proxy routes identical to HTTP block)
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name nivya.yourdomain.com;
    return 301 https://$host$request_uri;
}
```

---

## 10. Automated Backups & Disaster Recovery

### Backup Scripts
Automated backup scripts are provided for Linux/macOS ([scripts/backup.sh](file:///d:/Clg/vs/Nivya/scripts/backup.sh)) and Windows ([scripts/backup.ps1](file:///d:/Clg/vs/Nivya/scripts/backup.ps1)).

#### Execution
```bash
# Linux / macOS
chmod +x scripts/backup.sh
./scripts/backup.sh --retention-days 7

# Windows PowerShell
powershell -ExecutionPolicy Bypass -File scripts/backup.ps1 -RetentionDays 7
```

### Features
* **Consistent MySQL Snapshot**: Uses `mysqldump --single-transaction --quick --routines --triggers --hex-blob` to ensure transactional consistency without locking tables.
* **Gzip Compression**: Compresses SQL dumps on the fly (`nivya_nivya_db_YYYYMMDD_HHMMSS.sql.gz`).
* **Redis Snapshot**: Triggers a synchronous `SAVE` and archives `dump.rdb.gz`.
* **Automatic Retention Pruning**: Deletes backups older than `RETENTION_DAYS` (default: 7 days).

### Automated Cron Job (Production Linux)
Add the following line to root crontab (`sudo crontab -e`) for daily backups at 02:00 UTC:
```cron
0 2 * * * /opt/nivya/scripts/backup.sh --retention-days 14 >> /var/log/nivya_backup.log 2>&1
```

### Off-Site Cloud Sync (Recommended)
Sync local backups to AWS S3, Google Cloud Storage, or Azure Blob:
```bash
# Example: AWS S3 sync
aws s3 sync /opt/nivya/backups/ s3://my-nivya-backups-bucket/ --delete
```

---

## 11. Database Restoration Instructions

Restoration is automated and safe via [scripts/restore.sh](file:///d:/Clg/vs/Nivya/scripts/restore.sh) and [scripts/restore.ps1](file:///d:/Clg/vs/Nivya/scripts/restore.ps1).

### Automated Restoration
```bash
# Linux / macOS
chmod +x scripts/restore.sh
./scripts/restore.sh backups/mysql/nivya_nivya_db_20260909_020000.sql.gz

# Windows PowerShell
powershell -ExecutionPolicy Bypass -File scripts/restore.ps1 -BackupFile backups/mysql/nivya_nivya_db_20260909_020000.sql
```

### Step-by-Step Manual CLI Restoration
If executing manually without the scripts:
1. Ensure the `nivya-mysql` container is running:
   ```bash
   docker ps --filter "name=nivya-mysql"
   ```
2. Decompress and pipe the backup file directly into the MySQL container:
   ```bash
   # If backup is gzipped:
   gzip -dc backups/mysql/nivya_backup.sql.gz | docker exec -i nivya-mysql mysql -u root -p"<MYSQL_ROOT_PASSWORD>" nivya_db

   # If backup is plain SQL:
   docker exec -i nivya-mysql mysql -u root -p"<MYSQL_ROOT_PASSWORD>" nivya_db < backups/mysql/nivya_backup.sql
   ```
3. Verify restored tables and row counts:
   ```bash
   docker exec -it nivya-mysql mysql -u root -p"<MYSQL_ROOT_PASSWORD>" -e "USE nivya_db; SHOW TABLES; SELECT count(*) FROM users;"
   ```
4. Restart the backend service to clear any stale in-memory caches:
   ```bash
   docker compose restart backend
   ```

---

## 12. Logging, Observability & Monitoring

### Docker Log Management
All containers in `docker-compose.yml` are configured with the `json-file` log driver with file size bounds to prevent unbounded disk usage:
```yaml
logging:
  driver: "json-file"
  options:
    max-size: "50m"
    max-file: "5"
```

### Viewing Logs
```bash
# Stream all container logs in real time
docker compose logs -f

# Stream specific service logs
docker compose logs -f backend
docker compose logs -f nginx
docker compose logs -f mysql
```

### Nginx Structured Access Logs
Nginx logs are configured with upstream performance metrics:
```text
$remote_addr - $remote_user [$time_local] "$request" $status $body_bytes_sent rt=$request_time urt="$upstream_response_time"
```

### Health & Metrics Monitoring
| Endpoint | Purpose | Tool Integration |
|---|---|---|
| `GET /healthz` | External load balancer & Nginx liveness probe | AWS ALB, Cloudflare, Uptime Kuma |
| `GET /api/v1/health` | Application-level health and uptime status | Synthetic testing, internal monitoring |
| `GET /actuator/health` | Detailed Spring Boot component health (DB, Redis) | Kubernetes Probes, Datadog |
| `GET /actuator/prometheus` | Prometheus metric exposition | Prometheus, Grafana |

### Alerting Thresholds
* **CPU Usage**: Alert if container CPU exceeds 85% for > 5 consecutive minutes.
* **Memory Usage**: Alert if backend JVM memory exceeds 85% of heap limit.
* **HikariCP Connection Pool**: Alert if pending database connections > 5.
* **Disk Space**: Alert if host disk space usage exceeds 80%.

---

## 13. Step-by-Step Deployment Walkthrough

### Rapid Local Development Startup
```bash
# 1. Clone repository & configure environment
cp .env.example .env

# 2. Start services with development overrides (exposes local DB and Redis ports)
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build

# 3. Check health status of all containers
docker compose ps
```

### Production Deployment
```bash
# 1. Prepare environment with production secrets
cp .env.example .env
# Edit .env with unique strong passwords and keys!

# 2. Build and launch all services in detached mode
docker compose up -d --build

# 3. Verify services are healthy
docker compose ps

# 4. Tail gateway and backend logs
docker compose logs -f nginx backend
```

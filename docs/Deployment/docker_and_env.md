# Nivya — Deployment, Docker & Environment Guide

> For the comprehensive production runbook covering migrations, domain configuration, Let's Encrypt TLS setup, Android network security, and monitoring, please consult the authoritative [DEPLOYMENT.md](../../DEPLOYMENT.md).

---

## 1. Multi-Tier Container Architecture

Nivya provides fully isolated, containerized topologies for both development and production environments.

```
                                  [ Edge Ingress: 80 / 443 ]
                                              │
                                              ▼
                             ┌─────────────────────────────────┐
                             │           nivya-nginx           │
                             │         (Reverse Proxy)         │
                             └────────────────┬────────────────┘
                                              │ (nivya-network internal)
            ┌─────────────────────────────────┼─────────────────────────────────┐
            │ /                               │ /api/, /ws/                     │ /healthz
            ▼                                 ▼                                 ▼
 ┌──────────────────────┐          ┌──────────────────────┐          ┌──────────────────┐
 │    nivya-frontend    │          │    nivya-backend     │          │  Gateway Health  │
 │ (React 18 SPA/Nginx) │          │  (Spring Boot 3.3.5) │          │     (200 OK)     │
 └──────────────────────┘          └──────────┬───────────┘          └──────────────────┘
                                              │
                               ┌──────────────┴──────────────┐
                               ▼                             ▼
                    ┌─────────────────────┐       ┌─────────────────────┐
                    │     nivya-mysql     │       │     nivya-redis     │
                    │     (MySQL 8.0)     │       │    (Redis 7.2 AOF)  │
                    │ [mysql_data volume] │       │ [redis_data volume] │
                    └─────────────────────┘       └─────────────────────┘
```

---

## 2. Docker Compose Configurations

### 2.1 Production Topology (`docker-compose.yml`)
- **Port Policy**: Only Nginx publishes ports `80` and `443` to the host. MySQL (`3306`), Redis (`6379`), Backend (`8080`), and Frontend (`80`) are **strictly internal** to `nivya-network`.
- **Health Checks**: Every service has active health checks with startup grace periods and `depends_on: { condition: service_healthy }`.
- **Restarts**: Configured with `restart: unless-stopped`.
- **Secrets**: Exclusively sourced from environment variables; zero hardcoded secrets.

```bash
# Launch full production stack
cp .env.example .env
docker compose up -d --build
```

### 2.2 Development Override (`docker-compose.dev.yml`)
- **Loopback Tooling Ports**: Binds `127.0.0.1:3306` (MySQL) and `127.0.0.1:6379` (Redis) to localhost for IDEs, DBeaver, and RedisInsight.
- **Backend Debugging**: Binds `127.0.0.1:8080` for debugger attachment, Postman, and Swagger UI.
- **Frontend Port**: Binds `127.0.0.1:3000` for direct web browser access.

```bash
# Launch development stack with loopback ports exposed
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

---

## 3. Environment Variables Reference

A documented template is maintained in [.env.example](../../.env.example):

| Variable | Default / Format | Description |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` / `dev` | Active Spring profile. |
| `MYSQL_ROOT_PASSWORD` | *(random 32 hex)* | Administrative root password. |
| `MYSQL_DATABASE` | `nivya_db` | Target database name. |
| `MYSQL_USER` | `nivya_user` | Dedicated application user. |
| `MYSQL_PASSWORD` | *(random 32 hex)* | Dedicated user password. |
| `REDIS_PASSWORD` | *(random 32 hex)* | Redis authentication password. |
| `JWT_SECRET` | *(64+ hex chars)* | HMAC-SHA256 256-bit secret key. |
| `CORS_ALLOWED_ORIGINS` | `https://nivya.com`| Comma-separated allowed web origins. |
| `HTTP_PORT` | `80` | Ingress HTTP port. |
| `HTTPS_PORT` | `443` | Ingress HTTPS port. |
| `DOMAIN_NAME` | `nivya.yourdomain.com`| Fully qualified domain name. |

---

## 4. Automated Backup & Disaster Recovery

Cross-platform backup and restoration utilities:

### 4.1 Creating Backups (MySQL + Redis + Gzip)
```bash
# Linux / macOS (POSIX)
chmod +x scripts/backup.sh
./scripts/backup.sh --retention-days 7

# Windows PowerShell
powershell -ExecutionPolicy Bypass -File scripts/backup.ps1 -RetentionDays 7
```
*Output*: Compressed, consistent dumps in `backups/mysql/nivya_nivya_db_YYYYMMDD_HHMMSS.sql.gz` and Redis snapshots in `backups/redis/dump_YYYYMMDD_HHMMSS.rdb.gz`.

### 4.2 Restoring from Backup
```bash
# Linux / macOS (POSIX)
chmod +x scripts/restore.sh
./scripts/restore.sh backups/mysql/nivya_backup.sql.gz

# Windows PowerShell
powershell -ExecutionPolicy Bypass -File scripts/restore.ps1 -BackupFile backups/mysql/nivya_backup.sql
```

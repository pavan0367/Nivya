# Nivya — Deployment, Docker & Environment Guide

## 1. Environment Configuration

All configurable secrets, ports, and external resource URIs are managed via environment variables. The application ships with `.env.example` as the canonical template.

### Critical Environment Variables

| Variable | Default (Dev) | Description |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile (`dev` or `prod`). |
| `SERVER_PORT` | `8080` | Spring Boot REST API and WebSocket port. |
| `MYSQL_HOST` | `localhost` | MySQL hostname (use `mysql` in Docker). |
| `MYSQL_PORT` | `3306` | MySQL listener port. |
| `MYSQL_DATABASE` | `nivya_db` | Application database name. |
| `MYSQL_USER` | `nivya_user` | Dedicated application database user. |
| `MYSQL_PASSWORD` | `[dev-password]` | Database user password. |
| `REDIS_HOST` | `localhost` | Redis server hostname (use `redis` in Docker). |
| `REDIS_PORT` | `6379` | Redis listener port. |
| `REDIS_PASSWORD` | `[dev-password]` | Redis authentication password. |
| `JWT_SECRET` | `[256-bit-key]` | Secret key used for signing HMAC-SHA256 JWTs. |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS` | `900000` (15m) | Access token validity duration. |
| `JWT_REFRESH_TOKEN_EXPIRATION_MS` | `604800000` (7d)| Refresh token validity duration. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173`| Comma-separated list of allowed web origins. |

---

## 2. Docker Compose Infrastructure

The repository provides a `docker-compose.yml` supporting both modular local service dependencies and full-stack containerization.

### Service Topology

```
┌─────────────────────────────────────────────────────────────┐
│                       nivya-network                         │
│                                                             │
│   ┌───────────────┐                  ┌──────────────────┐   │
│   │  nivya-mysql  │◀─────────────────│  nivya-backend   │   │
│   │  (MySQL 8.0)  │                  │ (Spring Boot 3)  │   │
│   └───────────────┘                  └────────┬─────────┘   │
│                                               │             │
│   ┌───────────────┐                           │             │
│   │  nivya-redis  │◀──────────────────────────┘             │
│   │  (Redis 7.2)  │                           ▲             │
│   └───────────────┘                           │             │
│                                      ┌────────┴─────────┐   │
│                                      │   nivya-nginx    │   │
│                                      │ (Reverse Proxy)  │   │
│                                      └──────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Starting Infrastructure Services for Local Development
To launch MySQL and Redis while running the backend and frontend locally:
```powershell
docker compose up -d mysql redis
```

To verify service health:
```powershell
docker compose ps
```

---

## 3. Database Backup & Disaster Recovery

A standardized backup script is provided in `scripts/backup/db_backup.ps1`.

### Executing a Backup
```powershell
powershell -ExecutionPolicy Bypass -File scripts/backup/db_backup.ps1
```
This produces a compressed timestamped SQL dump in `database/backups/nivya_dump_YYYYMMDD_HHMMSS.sql`.

### Restoring from Backup
```powershell
docker exec -i nivya-mysql mysql -u nivya_user -p[password] nivya_db < database/backups/nivya_dump_example.sql
```

---

## 4. Production Hardening Checklist
1. **Secret Management**: Inject production credentials via environment variables or secret vaults (e.g. HashiCorp Vault, AWS Secrets Manager). Never commit `.env` to Git.
2. **TLS / HTTPS**: Configure SSL certificates in Nginx (`docker/nginx/nginx.conf`) with HTTP/2 and modern cipher suites.
3. **Database Security**:
   - Restrict MySQL user permissions to necessary DDL/DML on `nivya_db`.
   - Never expose port 3306 publicly; bind only to the internal Docker network.
4. **Telemetry Archival**: Schedule periodic pruning or archival cron jobs to keep the active database lean and responsive.

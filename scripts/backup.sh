#!/usr/bin/env bash
# ==============================================================================
# Nivya Production Database & Cache Backup Script (POSIX / Linux)
# ==============================================================================
# Usage:
#   ./scripts/backup.sh [--retention-days 7] [--backup-dir ./backups]
#
# Cron Example (Daily at 02:00 UTC):
#   0 2 * * * /path/to/nivya/scripts/backup.sh >> /var/log/nivya_backup.log 2>&1
# ==============================================================================

set -euo pipefail

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Configuration defaults
BACKUP_DIR="${ROOT_DIR}/backups"
RETENTION_DAYS=7
TIMESTAMP="$(date +"%Y%m%d_%H%M%S")"

# Load environment variables if .env exists
if [ -f "${ROOT_DIR}/.env" ]; then
    # Export non-comment lines
    export $(grep -v '^#' "${ROOT_DIR}/.env" | xargs -0 2>/dev/null || true)
fi

# Parse CLI arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --retention-days)
            RETENTION_DAYS="$2"
            shift 2
            ;;
        --backup-dir)
            BACKUP_DIR="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

MYSQL_CONTAINER="${MYSQL_CONTAINER:-nivya-mysql}"
REDIS_CONTAINER="${REDIS_CONTAINER:-nivya-redis}"
DB_NAME="${MYSQL_DATABASE:-nivya_db}"
DB_USER="${MYSQL_USER:-nivya_user}"
DB_PASS="${MYSQL_PASSWORD:-}"
DB_ROOT_PASS="${MYSQL_ROOT_PASSWORD:-}"
REDIS_PASS="${REDIS_PASSWORD:-}"

MYSQL_BACKUP_DIR="${BACKUP_DIR}/mysql"
REDIS_BACKUP_DIR="${BACKUP_DIR}/redis"
mkdir -p "${MYSQL_BACKUP_DIR}" "${REDIS_BACKUP_DIR}"

echo "======================================================================"
echo " Starting Nivya Backup Process: ${TIMESTAMP}"
echo "======================================================================"

# ------------------------------------------------------------------------------
# 1. MySQL Dump with Consistency and Compression
# ------------------------------------------------------------------------------
echo "[-] Verifying MySQL container '${MYSQL_CONTAINER}'..."
if ! docker ps --format '{{.Names}}' | grep -Eq "^${MYSQL_CONTAINER}\$"; then
    echo "[ERROR] Container '${MYSQL_CONTAINER}' is not running!" >&2
    exit 1
fi

MYSQL_DUMP_FILE="${MYSQL_BACKUP_DIR}/nivya_${DB_NAME}_${TIMESTAMP}.sql.gz"
echo "[-] Performing consistent mysqldump to ${MYSQL_DUMP_FILE}..."

AUTH_FLAG="-p${DB_ROOT_PASS}"
AUTH_USER="root"
if [ -z "${DB_ROOT_PASS}" ]; then
    AUTH_USER="${DB_USER}"
    AUTH_FLAG="-p${DB_PASS}"
fi

# Execute single-transaction consistent dump and stream through gzip
docker exec "${MYSQL_CONTAINER}" mysqldump \
    -u "${AUTH_USER}" \
    "${AUTH_FLAG}" \
    --single-transaction \
    --quick \
    --routines \
    --triggers \
    --hex-blob \
    --default-character-set=utf8mb4 \
    "${DB_NAME}" | gzip -c > "${MYSQL_DUMP_FILE}"

MYSQL_SIZE=$(du -h "${MYSQL_DUMP_FILE}" | cut -f1)
echo "[+] MySQL backup complete: ${MYSQL_DUMP_FILE} (${MYSQL_SIZE})"

# ------------------------------------------------------------------------------
# 2. Redis AOF / RDB Snapshot
# ------------------------------------------------------------------------------
if docker ps --format '{{.Names}}' | grep -Eq "^${REDIS_CONTAINER}\$"; then
    echo "[-] Triggering Redis synchronous snapshot save..."
    REDIS_AUTH_CMD=""
    if [ -n "${REDIS_PASS}" ]; then
        REDIS_AUTH_CMD="-a ${REDIS_PASS}"
    fi
    docker exec "${REDIS_CONTAINER}" redis-cli ${REDIS_AUTH_CMD} SAVE > /dev/null

    REDIS_DUMP_FILE="${REDIS_BACKUP_DIR}/dump_${TIMESTAMP}.rdb"
    docker cp "${REDIS_CONTAINER}:/data/dump.rdb" "${REDIS_DUMP_FILE}" 2>/dev/null || true

    if [ -f "${REDIS_DUMP_FILE}" ]; then
        gzip -f "${REDIS_DUMP_FILE}"
        echo "[+] Redis backup complete: ${REDIS_DUMP_FILE}.gz"
    fi
fi

# ------------------------------------------------------------------------------
# 3. Retention Policy & Cleanup
# ------------------------------------------------------------------------------
echo "[-] Pruning backups older than ${RETENTION_DAYS} days..."
find "${MYSQL_BACKUP_DIR}" -type f -name "*.sql.gz" -mtime +"${RETENTION_DAYS}" -exec rm -f {} \;
find "${REDIS_BACKUP_DIR}" -type f -name "*.rdb.gz" -mtime +"${RETENTION_DAYS}" -exec rm -f {} \;

echo "======================================================================"
echo "[SUCCESS] Backup process finished successfully."
echo "======================================================================"

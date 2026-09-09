#!/usr/bin/env bash
# ==============================================================================
# Nivya Database Restoration Script (POSIX / Linux)
# ==============================================================================
# Usage:
#   ./scripts/restore.sh /path/to/backup.sql.gz [--force]
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Load .env variables
if [ -f "${ROOT_DIR}/.env" ]; then
    export $(grep -v '^#' "${ROOT_DIR}/.env" | xargs -0 2>/dev/null || true)
fi

BACKUP_FILE="${1:-}"
FORCE=false

if [ -z "${BACKUP_FILE}" ]; then
    echo "Usage: $0 <path-to-sql-or-sql.gz-file> [--force]"
    exit 1
fi

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "[ERROR] Backup file '${BACKUP_FILE}' does not exist!" >&2
    exit 1
fi

if [ "${2:-}" = "--force" ] || [ "${2:-}" = "-f" ]; then
    FORCE=true
fi

MYSQL_CONTAINER="${MYSQL_CONTAINER:-nivya-mysql}"
DB_NAME="${MYSQL_DATABASE:-nivya_db}"
DB_ROOT_PASS="${MYSQL_ROOT_PASSWORD:-}"
DB_PASS="${MYSQL_PASSWORD:-}"
DB_USER="${MYSQL_USER:-nivya_user}"

echo "======================================================================"
echo " Nivya Database Restore Utility"
echo "======================================================================"
echo "Target Container: ${MYSQL_CONTAINER}"
echo "Target Database:  ${DB_NAME}"
echo "Source File:      ${BACKUP_FILE}"
echo "======================================================================"

# Verify container is running
if ! docker ps --format '{{.Names}}' | grep -Eq "^${MYSQL_CONTAINER}\$"; then
    echo "[ERROR] Container '${MYSQL_CONTAINER}' is not running!" >&2
    exit 1
fi

# Confirmation prompt unless force flag provided
if [ "${FORCE}" != "true" ]; then
    read -r -p "WARNING: This will overwrite tables in '${DB_NAME}'. Proceed? [y/N] " response
    if [[ ! "${response}" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        echo "Restoration cancelled by user."
        exit 0
    fi
fi

AUTH_FLAG="-p${DB_ROOT_PASS}"
AUTH_USER="root"
if [ -z "${DB_ROOT_PASS}" ]; then
    AUTH_USER="${DB_USER}"
    AUTH_FLAG="-p${DB_PASS}"
fi

echo "[-] Streaming backup into MySQL database '${DB_NAME}'..."
if [[ "${BACKUP_FILE}" == *.gz ]]; then
    gzip -dc "${BACKUP_FILE}" | docker exec -i "${MYSQL_CONTAINER}" mysql -u "${AUTH_USER}" "${AUTH_FLAG}" "${DB_NAME}"
else
    cat "${BACKUP_FILE}" | docker exec -i "${MYSQL_CONTAINER}" mysql -u "${AUTH_USER}" "${AUTH_FLAG}" "${DB_NAME}"
fi

echo "[-] Verifying restored database..."
TABLE_COUNT=$(docker exec "${MYSQL_CONTAINER}" mysql -u "${AUTH_USER}" "${AUTH_FLAG}" -e "SELECT count(*) FROM information_schema.tables WHERE table_schema = '${DB_NAME}';" -s -N)

echo "======================================================================"
echo "[SUCCESS] Database '${DB_NAME}' restored successfully!"
echo "          Total tables in schema: ${TABLE_COUNT}"
echo "======================================================================"

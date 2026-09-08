# ==============================================================================
# db_backup.ps1 - Database Backup Script for Nivya MySQL Service
# ==============================================================================
param (
    [string]$BackupDir = "database/backups"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$dumpFile = Join-Path $BackupDir "nivya_dump_$timestamp.sql"

Write-Host "Backing up Nivya database to $dumpFile..." -ForegroundColor Cyan

# Check if Docker container is running
$containerName = "nivya-mysql"
$containerRunning = docker ps --filter "name=$containerName" --format "{{.Names}}"

if ($containerRunning -eq $containerName) {
    docker exec $containerName mysqldump -u nivya_user -pnivya_secure_dev_password_2026 nivya_db > $dumpFile
    Write-Host "[OK] Backup created successfully via Docker: $dumpFile" -ForegroundColor Green
} else {
    Write-Host "[WARN] $containerName is not running. Start it with 'docker compose up -d mysql' first." -ForegroundColor Yellow
}

# ==============================================================================
# Nivya Production Database & Cache Backup Script (Windows PowerShell)
# ==============================================================================
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/backup.ps1 -RetentionDays 7
# ==============================================================================

[CmdletBinding()]
param (
    [int]$RetentionDays = 7,
    [string]$BackupDir = "backups"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$FullBackupDir = Join-Path $RootDir $BackupDir
$MysqlBackupDir = Join-Path $FullBackupDir "mysql"
$RedisBackupDir = Join-Path $FullBackupDir "redis"

New-Item -ItemType Directory -Path $MysqlBackupDir -Force | Out-Null
New-Item -ItemType Directory -Path $RedisBackupDir -Force | Out-Null

# Load .env file if available
$EnvFile = Join-Path $RootDir ".env"
$EnvVars = @{}
if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $EnvVars[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
}

$ContainerMysql = if ($EnvVars.ContainsKey("MYSQL_CONTAINER")) { $EnvVars["MYSQL_CONTAINER"] } else { "nivya-mysql" }
$ContainerRedis = if ($EnvVars.ContainsKey("REDIS_CONTAINER")) { $EnvVars["REDIS_CONTAINER"] } else { "nivya-redis" }
$DbName = if ($EnvVars.ContainsKey("MYSQL_DATABASE")) { $EnvVars["MYSQL_DATABASE"] } else { "nivya_db" }
$RootPass = if ($EnvVars.ContainsKey("MYSQL_ROOT_PASSWORD")) { $EnvVars["MYSQL_ROOT_PASSWORD"] } else { "" }
$DbPass = if ($EnvVars.ContainsKey("MYSQL_PASSWORD")) { $EnvVars["MYSQL_PASSWORD"] } else { "nivya_secure_dev_password_2026" }
$RedisPass = if ($EnvVars.ContainsKey("REDIS_PASSWORD")) { $EnvVars["REDIS_PASSWORD"] } else { "" }

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Starting Nivya Backup Process: $timestamp" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

# 1. MySQL Dump
Write-Host "[-] Checking MySQL container '$ContainerMysql'..." -ForegroundColor Gray
$running = docker ps --filter "name=$ContainerMysql" --format "{{.Names}}"
if ($running -ne $ContainerMysql) {
    Write-Error "Container '$ContainerMysql' is not running. Start with 'docker compose up -d' first."
}

$authFlag = if ($RootPass) { "-u root -p$RootPass" } else { "-u nivya_user -p$DbPass" }
$sqlFile = Join-Path $MysqlBackupDir "nivya_${DbName}_$timestamp.sql"

Write-Host "[-] Running mysqldump..." -ForegroundColor Gray
$dumpCmd = "docker exec $ContainerMysql mysqldump $authFlag --single-transaction --quick --routines --triggers --hex-blob $DbName > `"$sqlFile`""
cmd.exe /c $dumpCmd

if (Test-Path $sqlFile) {
    $item = Get-Item $sqlFile
    Write-Host "[+] MySQL backup created: $($item.FullName) ($([math]::Round($item.Length/1MB, 2)) MB)" -ForegroundColor Green
} else {
    Write-Error "Failed to generate MySQL backup file."
}

# 2. Redis Snapshot
$redisRunning = docker ps --filter "name=$ContainerRedis" --format "{{.Names}}"
if ($redisRunning -eq $ContainerRedis) {
    Write-Host "[-] Triggering Redis synchronous snapshot save..." -ForegroundColor Gray
    if ($RedisPass) {
        docker exec $ContainerRedis redis-cli -a $RedisPass SAVE | Out-Null
    } else {
        docker exec $ContainerRedis redis-cli SAVE | Out-Null
    }
    $redisDumpFile = Join-Path $RedisBackupDir "dump_$timestamp.rdb"
    docker cp "${ContainerRedis}:/data/dump.rdb" $redisDumpFile
    if (Test-Path $redisDumpFile) {
        Write-Host "[+] Redis snapshot copied: $redisDumpFile" -ForegroundColor Green
    }
}

# 3. Retention Cleanup
Write-Host "[-] Pruning backups older than $RetentionDays days..." -ForegroundColor Gray
$cutoff = (Get-Date).AddDays(-$RetentionDays)
Get-ChildItem -Path $MysqlBackupDir -Filter "*.sql" | Where-Object { $_.LastWriteTime -lt $cutoff } | Remove-Item -Force
Get-ChildItem -Path $RedisBackupDir -Filter "*.rdb" | Where-Object { $_.LastWriteTime -lt $cutoff } | Remove-Item -Force

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "[SUCCESS] Backup process finished successfully." -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Cyan

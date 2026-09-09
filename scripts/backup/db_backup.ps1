# ==============================================================================
# db_backup.ps1 - Backward-Compatible Wrapper for Nivya Database Backup
# ==============================================================================
param (
    [int]$RetentionDays = 7,
    [string]$BackupDir = "backups"
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$TargetScript = Join-Path (Split-Path -Parent $ScriptDir) "backup.ps1"

& $TargetScript -RetentionDays $RetentionDays -BackupDir $BackupDir

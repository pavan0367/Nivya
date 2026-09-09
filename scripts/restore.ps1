# ==============================================================================
# Nivya Database Restoration Script (Windows PowerShell)
# ==============================================================================
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/restore.ps1 -BackupFile backups/mysql/nivya_db_dump.sql [-Force]
# ==============================================================================

[CmdletBinding()]
param (
    [Parameter(Mandatory=$true, Position=0)]
    [string]$BackupFile,

    [Parameter(Mandatory=$false)]
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir

if (-not [System.IO.Path]::IsPathRooted($BackupFile)) {
    $BackupFile = Join-Path $RootDir $BackupFile
}

if (-not (Test-Path $BackupFile)) {
    Write-Error "Backup file '$BackupFile' was not found."
}

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
$DbName = if ($EnvVars.ContainsKey("MYSQL_DATABASE")) { $EnvVars["MYSQL_DATABASE"] } else { "nivya_db" }
$RootPass = if ($EnvVars.ContainsKey("MYSQL_ROOT_PASSWORD")) { $EnvVars["MYSQL_ROOT_PASSWORD"] } else { "" }
$DbPass = if ($EnvVars.ContainsKey("MYSQL_PASSWORD")) { $EnvVars["MYSQL_PASSWORD"] } else { "nivya_secure_dev_password_2026" }

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Nivya Database Restore Utility" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "Target Container: $ContainerMysql"
Write-Host "Target Database:  $DbName"
Write-Host "Source File:      $BackupFile"
Write-Host "======================================================================" -ForegroundColor Cyan

# Verify container
$running = docker ps --filter "name=$ContainerMysql" --format "{{.Names}}"
if ($running -ne $ContainerMysql) {
    Write-Error "Container '$ContainerMysql' is not running. Start with 'docker compose up -d' first."
}

if (-not $Force) {
    $title = "Confirm Database Overwrite"
    $message = "WARNING: Restoring will overwrite existing data in '$DbName'. Are you sure you want to proceed?"
    $yes = New-Object System.Management.Automation.Host.ChoiceDescription "&Yes", "Overwrites current database."
    $no = New-Object System.Management.Automation.Host.ChoiceDescription "&No", "Cancels restoration."
    $options = [System.Management.Automation.Host.ChoiceDescription[]]($yes, $no)
    $result = $host.ui.PromptForChoice($title, $message, $options, 1)
    if ($result -ne 0) {
        Write-Host "Restoration cancelled by user." -ForegroundColor Yellow
        exit 0
    }
}

$authFlag = if ($RootPass) { "-u root -p$RootPass" } else { "-u nivya_user -p$DbPass" }

Write-Host "[-] Streaming backup into database '$DbName'..." -ForegroundColor Gray
$restoreCmd = "type `"$BackupFile`" | docker exec -i $ContainerMysql mysql $authFlag $DbName"
cmd.exe /c $restoreCmd

Write-Host "[-] Verifying restored tables..." -ForegroundColor Gray
$verifyCmd = "docker exec $ContainerMysql mysql $authFlag -e `"SELECT count(*) FROM information_schema.tables WHERE table_schema = '$DbName';`" -s -N"
$tableCount = cmd.exe /c $verifyCmd

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "[SUCCESS] Database '$DbName' restored successfully!" -ForegroundColor Green
Write-Host "          Total tables in schema: $tableCount" -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Cyan

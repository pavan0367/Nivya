# ==============================================================================
# setup_dev_env.ps1 - Development Environment Initializer for Nivya
# ==============================================================================
$ErrorActionPreference = "Stop"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "       NIVYA - Dev Environment Initializer        " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 1. Check .env file
if (-not (Test-Path ".env")) {
    if (Test-Path ".env.example") {
        Copy-Item ".env.example" ".env"
        Write-Host "[OK] Created .env from .env.example" -ForegroundColor Green
        Write-Host "[INFO] Please review and edit .env to provide custom secrets." -ForegroundColor Yellow
    } else {
        Write-Host "[ERROR] .env.example does not exist!" -ForegroundColor Red
    }
} else {
    Write-Host "[OK] .env file already exists." -ForegroundColor Green
}

# 2. Check System Prerequisites
Write-Host "`nChecking System Prerequisites:" -ForegroundColor Yellow

# Java 21 Check
try {
    $javaOutput = java -version 2>&1 | Out-String
    Write-Host "[OK] Java installed: $($javaOutput.Split("`n")[0].Trim())" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Java 21 not detected in PATH." -ForegroundColor Yellow
}

# Maven Check
try {
    $mvnOutput = mvn -v 2>&1 | Out-String
    Write-Host "[OK] Maven installed: $($mvnOutput.Split("`n")[0].Trim())" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Maven not detected in PATH." -ForegroundColor Yellow
}

# Node.js Check
try {
    $nodeVer = node -v
    Write-Host "[OK] Node.js installed: $nodeVer" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Node.js not detected in PATH." -ForegroundColor Yellow
}

# Docker Check
try {
    $dockerVer = docker --version
    Write-Host "[OK] Docker installed: $dockerVer" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Docker not detected in PATH." -ForegroundColor Yellow
}

Write-Host "`nDev environment ready for Phase 1!" -ForegroundColor Cyan

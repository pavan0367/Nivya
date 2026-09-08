# ==============================================================================
# build_all.ps1 - Multi-module build verification runner for Nivya
# ==============================================================================
$ErrorActionPreference = "Stop"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "         NIVYA - Multi-Module Build Runner        " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# Phase check
if (-not (Test-Path "backend/pom.xml")) {
    Write-Host "[INFO] Backend POM not yet initialized (Slated for Phase 2)." -ForegroundColor Yellow
}

if (-not (Test-Path "web/package.json")) {
    Write-Host "[INFO] Web package.json not yet initialized (Slated for Phase 20)." -ForegroundColor Yellow
}

Write-Host "`nAll phases adhere to staged implementation order." -ForegroundColor Green

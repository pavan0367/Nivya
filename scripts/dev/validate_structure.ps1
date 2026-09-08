# ==============================================================================
# validate_structure.ps1 - Phase 1 Validation Script for Nivya Repository
# ==============================================================================
$ErrorActionPreference = "Stop"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "   NIVYA - Phase 1 Structure & Integrity Audit    " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

$missingItems = @()

# 1. Required Root Files
$requiredRootFiles = @(
    ".gitignore",
    ".gitattributes",
    ".env.example",
    "docker-compose.yml",
    "README.md",
    "LICENSE"
)

Write-Host "`n[1/5] Checking Root Configuration & Documentation Files..." -ForegroundColor Yellow
foreach ($file in $requiredRootFiles) {
    if (Test-Path $file) {
        Write-Host "  [OK] $file" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Missing file: $file" -ForegroundColor Red
        $missingItems += $file
    }
}

# 2. Required Core Directories
$requiredDirectories = @(
    # Backend
    "backend/src/main/java/com/nivya/auth",
    "backend/src/main/java/com/nivya/user",
    "backend/src/main/java/com/nivya/role",
    "backend/src/main/java/com/nivya/pairing",
    "backend/src/main/java/com/nivya/family",
    "backend/src/main/java/com/nivya/device",
    "backend/src/main/java/com/nivya/battery",
    "backend/src/main/java/com/nivya/network",
    "backend/src/main/java/com/nivya/location",
    "backend/src/main/java/com/nivya/usage",
    "backend/src/main/java/com/nivya/activity",
    "backend/src/main/java/com/nivya/history",
    "backend/src/main/java/com/nivya/communication",
    "backend/src/main/java/com/nivya/alerts",
    "backend/src/main/java/com/nivya/notification",
    "backend/src/main/java/com/nivya/convocation",
    "backend/src/main/java/com/nivya/consent",
    "backend/src/main/java/com/nivya/privacy",
    "backend/src/main/java/com/nivya/audit",
    "backend/src/main/java/com/nivya/websocket",
    "backend/src/main/java/com/nivya/security",
    "backend/src/main/java/com/nivya/common",
    "backend/src/main/resources/db/migration",

    # Android
    "android/app",
    "android/core/network",
    "android/core/security",
    "android/core/permissions",
    "android/core/database",
    "android/core/utils",
    "android/data/local",
    "android/data/remote",
    "android/data/repository",
    "android/data/model",
    "android/domain/model",
    "android/domain/usecase",
    "android/ui/auth",
    "android/ui/role",
    "android/ui/pairing",
    "android/ui/dashboard",
    "android/ui/battery",
    "android/ui/screen_time",
    "android/ui/network",
    "android/ui/location",
    "android/ui/device_health",
    "android/ui/cleanup",
    "android/ui/alerts",
    "android/ui/convocation",
    "android/ui/settings",
    "android/services/sync",
    "android/services/telemetry",
    "android/services/location",
    "android/services/usage",
    "android/services/notification",
    "android/services/reconnect",
    "android/permissions/location",
    "android/permissions/usage_access",
    "android/permissions/notification_listener",

    # Web
    "web/src/components",
    "web/src/pages/Login",
    "web/src/pages/RoleSelection",
    "web/src/pages/Dashboard",
    "web/src/pages/LiveActivity",
    "web/src/pages/History",
    "web/src/pages/Location",
    "web/src/pages/Usage",
    "web/src/pages/Alerts",
    "web/src/pages/Convocation",
    "web/src/pages/Settings",
    "web/src/layouts",
    "web/src/services",
    "web/src/websocket",
    "web/src/store",
    "web/src/hooks",
    "web/src/routes",
    "web/src/utils",

    # Database
    "database/migrations",
    "database/seeds",
    "database/indexes",
    "database/procedures",

    # Docker
    "docker/backend",
    "docker/mysql",
    "docker/redis",
    "docker/nginx",

    # Documentation
    "docs/SRS",
    "docs/API",
    "docs/Architecture",
    "docs/Database",
    "docs/UI",
    "docs/Deployment",

    # Scripts
    "scripts/dev",
    "scripts/build",
    "scripts/backup"
)

Write-Host "`n[2/5] Checking Required Modular Directory Tree..." -ForegroundColor Yellow
$dirCount = 0
foreach ($dir in $requiredDirectories) {
    if (Test-Path $dir) {
        $dirCount++
    } else {
        Write-Host "  [FAIL] Missing directory: $dir" -ForegroundColor Red
        $missingItems += $dir
    }
}
Write-Host "  [OK] Verified $dirCount/$($requiredDirectories.Count) required module directories." -ForegroundColor Green

# 3. Required Docker Skeleton Files
$requiredDockerFiles = @(
    "docker/mysql/init.sql",
    "docker/redis/redis.conf",
    "docker/nginx/nginx.conf",
    "docker/backend/Dockerfile"
)

Write-Host "`n[3/5] Checking Docker Skeleton Assets..." -ForegroundColor Yellow
foreach ($dockerFile in $requiredDockerFiles) {
    if (Test-Path $dockerFile) {
        Write-Host "  [OK] $dockerFile" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Missing Docker asset: $dockerFile" -ForegroundColor Red
        $missingItems += $dockerFile
    }
}

# 4. Required Technical Documentation Files
$requiredDocs = @(
    "docs/SRS/README.md",
    "docs/Architecture/system_architecture.md",
    "docs/Database/schema_overview.md",
    "docs/API/api_overview.md",
    "docs/UI/navigation_and_roles.md",
    "docs/Deployment/docker_and_env.md"
)

Write-Host "`n[4/5] Checking Technical Documentation Files..." -ForegroundColor Yellow
foreach ($doc in $requiredDocs) {
    if (Test-Path $doc) {
        Write-Host "  [OK] $doc" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Missing documentation: $doc" -ForegroundColor Red
        $missingItems += $doc
    }
}

# 5. Git Status Check
Write-Host "`n[5/5] Checking Git Status..." -ForegroundColor Yellow
if (Test-Path ".git") {
    Write-Host "  [OK] Git repository initialized." -ForegroundColor Green
} else {
    Write-Host "  [FAIL] .git directory missing." -ForegroundColor Red
    $missingItems += ".git"
}

Write-Host "`n==================================================" -ForegroundColor Cyan
if ($missingItems.Count -eq 0) {
    Write-Host " [PASS] Phase 1 Structure & Integrity Validation SUCCESSFUL!" -ForegroundColor Green
    Write-Host "==================================================" -ForegroundColor Cyan
    exit 0
} else {
    Write-Host " [FAIL] Missing items detected: $($missingItems.Count)" -ForegroundColor Red
    foreach ($m in $missingItems) {
        Write-Host "   - $m" -ForegroundColor Red
    }
    Write-Host "==================================================" -ForegroundColor Cyan
    exit 1
}

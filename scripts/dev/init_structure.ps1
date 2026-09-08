# init_structure.ps1 - Creates required directory structure for Nivya Phase 1
$ErrorActionPreference = "Stop"

$directories = @(
    # Backend Java packages
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

    # Android Clean Architecture modules
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

    # Web React modules
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

foreach ($dir in $directories) {
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    $keepFile = Join-Path $dir ".gitkeep"
    if (-not (Test-Path $keepFile) -and (Get-ChildItem $dir).Count -eq 0) {
        Set-Content -Path $keepFile -Value "" -NoNewline
    }
}

Write-Host "All $($directories.Count) directories ensured."

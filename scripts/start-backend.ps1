# Start TestPilot backend (Spring Boot 2.7.18, port 3344).
# Requires: JDK 8+, Maven; local MySQL (127.0.0.1:3306) is auto-started via start-mysql.ps1 if not running.
#
# Two modes in one script:
#   - Launcher (default): ensure MySQL is up, then spawn a visible window
#     (title: "TestPilot Backend (3344)") that runs this script with -Run,
#     wait until port 3344 accepts connections, and exit.
#   - Runner (-Run): run "mvn spring-boot:run" in the foreground of this
#     window. Stop it with Ctrl+C here or via scripts/stop-backend.ps1.
#
# Exit codes (launcher): 0 = ready (already running or just started), 1 = failed
param([switch]$Run)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$port = 3344

function Test-BackendPort {
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $task = $client.ConnectAsync('127.0.0.1', $port)
        $ok = $task.Wait(500) -and $client.Connected
        $client.Dispose()
        return $ok
    } catch {
        return $false
    }
}

if ($Run) {
    # ---- Runner: foreground "mvn spring-boot:run" inside this window ----
    $host.UI.RawUI.WindowTitle = 'TestPilot Backend (3344)'
    Set-Location (Join-Path $root 'backend')
    Write-Output 'Starting backend on port 3344 (Flyway will auto-create database lemon_testpilot on first run)...'
    Write-Output 'Ctrl+C here stops the backend.'
    mvn spring-boot:run
    exit $LASTEXITCODE
}

# ---- Launcher ----

# Middleware first: ensure MySQL (3306) is up; skip if already running
& (Join-Path $PSScriptRoot 'start-mysql.ps1')
if ($LASTEXITCODE -ne 0) {
    Write-Output '[Error] MySQL is not available, aborting backend startup.'
    exit $LASTEXITCODE
}

# First-run bootstrap: create application-local.yml from the example
$localCfg = Join-Path $root 'backend\src\main\resources\application-local.yml'
$example  = Join-Path $root 'backend\src\main\resources\application-local.yml.example'
if (-not (Test-Path $localCfg)) {
    Copy-Item $example $localCfg
    Write-Output '[Hint] application-local.yml created from example.'
    Write-Output '[Hint] Edit backend/src/main/resources/application-local.yml and set your MySQL password, then restart.'
}

if (Test-BackendPort) {
    Write-Output '[Backend] Already running on port 3344, skip.'
    exit 0
}

Write-Output '[Backend] Starting in a visible foreground window (mvn spring-boot:run)...'
Start-Process -FilePath 'powershell.exe' -WorkingDirectory (Join-Path $root 'backend') -ArgumentList @(
    '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $PSCommandPath, '-Run'
)

# Wait until port 3344 accepts connections (max 120s - cold Maven starts can be slow)
$deadline = (Get-Date).AddSeconds(120)
while ((Get-Date) -lt $deadline) {
    if (Test-BackendPort) {
        Write-Output '[Backend] Ready on port 3344 (http://localhost:3344).'
        exit 0
    }
    Start-Sleep -Milliseconds 1000
}

Write-Output '[Backend] FAILED: port 3344 not ready within 120s.'
Write-Output '[Backend] Check the "TestPilot Backend (3344)" window for Maven/Spring logs.'
exit 1

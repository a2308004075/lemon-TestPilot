# Start TestPilot frontend (Vite dev server, port 5173, proxies /api -> 3344).
# Requires: Node.js 16+
#
# Two modes in one script:
#   - Launcher (default): if 5173 is already listening, skip; otherwise spawn
#     a visible window (title: "TestPilot Frontend (5173)") that runs this
#     script with -Run, wait until port 5173 is listening, and exit.
#   - Runner (-Run): install dependencies on first run, then run "npm run dev"
#     in the foreground of this window. Stop it with Ctrl+C here or via
#     scripts/stop-frontend.ps1.
#
# Exit codes (launcher): 0 = ready (already running or just started), 1 = failed
param([switch]$Run)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$port = 5173

function Test-FrontendPort {
    # Vite binds IPv6 ::1 by default, so probe the LISTEN socket instead of
    # connecting to 127.0.0.1 (a plain TCP probe would misreport DOWN).
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

if ($Run) {
    # ---- Runner: foreground "npm run dev" inside this window ----
    $host.UI.RawUI.WindowTitle = 'TestPilot Frontend (5173)'
    Set-Location (Join-Path $root 'frontend')
    if (-not (Test-Path (Join-Path (Get-Location) 'node_modules'))) {
        Write-Output 'Installing frontend dependencies (first run)...'
        npm install
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    Write-Output 'Starting frontend dev server on http://localhost:5173 (backend must be running on 3344)...'
    Write-Output 'Ctrl+C here stops the frontend.'
    npm run dev
    exit $LASTEXITCODE
}

# ---- Launcher ----
if (Test-FrontendPort) {
    Write-Output '[Frontend] Already running on port 5173, skip.'
    exit 0
}

Write-Output '[Frontend] Starting in a visible foreground window (npm run dev)...'
Start-Process -FilePath 'powershell.exe' -WorkingDirectory (Join-Path $root 'frontend') -ArgumentList @(
    '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $PSCommandPath, '-Run'
)

# Wait until port 5173 is listening (60s normally; 300s on a first run where
# the spawned window has to npm install first)
$timeoutSec = 60
if (-not (Test-Path (Join-Path $root 'frontend\node_modules'))) { $timeoutSec = 300 }
$deadline = (Get-Date).AddSeconds($timeoutSec)
while ((Get-Date) -lt $deadline) {
    if (Test-FrontendPort) {
        Write-Output '[Frontend] Ready on port 5173 (http://localhost:5173).'
        exit 0
    }
    Start-Sleep -Milliseconds 1000
}

Write-Output "[Frontend] FAILED: port 5173 not ready within $timeoutSec s."
Write-Output '[Frontend] Check the "TestPilot Frontend (5173)" window (first run runs npm install first).'
exit 1

# Start TestPilot frontend (Vite dev server, port 5173, proxies /api -> 3344)
# Requires: Node.js 16+
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location (Join-Path $root 'frontend')

if (-not (Test-Path (Join-Path (Get-Location) 'node_modules'))) {
    Write-Output 'Installing frontend dependencies (first run)...'
    npm install
}

Write-Output 'Starting frontend dev server on http://localhost:5173 (backend must be running on 3344)...'
npm run dev

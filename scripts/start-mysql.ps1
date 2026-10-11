# Start TestPilot middleware: MySQL (portable install, port 3306).
# mysqld runs in the FOREGROUND inside its own visible console window
# (title: "TestPilot MySQL (3306)"), never as a hidden background process.
#
# Two modes in one script:
#   - Launcher (default, no args): if 3306 is already listening, skip;
#     otherwise spawn a visible window that runs this script with -Run,
#     then wait until port 3306 accepts connections and exit.
#   - Runner (-Run): set the window title and run mysqld in the foreground
#     of this window. Stop it with Ctrl+C here (graceful) or via
#     scripts/stop-mysql.ps1.
#
# Exit codes (launcher): 0 = ready (already running or just started), 1 = failed
param([switch]$Run)
$ErrorActionPreference = 'Stop'

$mysqlDir = 'D:\software\mysql-8.0'
$mysqld   = Join-Path $mysqlDir 'bin\mysqld.exe'
$defaults = Join-Path $mysqlDir 'my.ini'
$port     = 3306

function Test-MySqlPort {
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
    # ---- Runner: foreground mysqld inside this window ----
    $host.UI.RawUI.WindowTitle = 'TestPilot MySQL (3306)'
    Set-Location $mysqlDir
    Write-Output '[MySQL] mysqld starting in foreground. Ctrl+C here = graceful stop.'
    Write-Output "[MySQL] Server log (*.err) lives under $mysqlDir\data\."
    & $mysqld "--defaults-file=$defaults"
    exit $LASTEXITCODE
}

# ---- Launcher ----
if (Test-MySqlPort) {
    Write-Output '[MySQL] Already running on port 3306, skip.'
    exit 0
}

if (-not (Test-Path $mysqld) -or -not (Test-Path $defaults)) {
    Write-Output "[MySQL] Not found at $mysqlDir (mysqld.exe or my.ini missing)."
    Write-Output '[MySQL] Install MySQL there or start it manually, then retry.'
    exit 1
}

Write-Output '[MySQL] Starting mysqld in a visible foreground window...'
Start-Process -FilePath 'powershell.exe' -WorkingDirectory $mysqlDir -ArgumentList @(
    '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $PSCommandPath, '-Run'
)

# Wait until port 3306 accepts connections (max 30s)
$deadline = (Get-Date).AddSeconds(30)
while ((Get-Date) -lt $deadline) {
    if (Test-MySqlPort) {
        Write-Output '[MySQL] Ready on port 3306.'
        exit 0
    }
    Start-Sleep -Milliseconds 500
}

Write-Output '[MySQL] FAILED: port 3306 not ready within 30s.'
Write-Output '[MySQL] Check the "TestPilot MySQL (3306)" window and error log (*.err) under D:\software\mysql-8.0\data\.'
exit 1

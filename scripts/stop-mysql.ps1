# Stop TestPilot middleware: MySQL (portable install, port 3306) GRACEFULLY
# Uses mysqladmin shutdown so InnoDB flushes and closes cleanly.
# Never force-kills mysqld - crash recovery on next start is the last resort.
# Root password is read from backend/src/main/resources/application-local.yml
# (git-ignored), so no credentials are stored in this script.
# Exit codes: 0 = stopped or already stopped, 1 = failed
$ErrorActionPreference = 'Stop'

$mysqlDir   = 'D:\software\mysql-8.0'
$mysqladmin = Join-Path $mysqlDir 'bin\mysqladmin.exe'
$port       = 3306

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

if (-not (Test-MySqlPort)) {
    Write-Output '[MySQL] Not running on port 3306, skip.'
    exit 0
}

if (-not (Test-Path $mysqladmin)) {
    Write-Output "[MySQL] mysqladmin.exe not found at $mysqlDir - cannot shut down gracefully."
    Write-Output '[MySQL] Do NOT taskkill mysqld unless data loss is acceptable.'
    exit 1
}

# Read datasource password from backend local config (git-ignored)
$repoRoot = Split-Path -Parent $PSScriptRoot
$cfg = Join-Path $repoRoot 'backend\src\main\resources\application-local.yml'
$dbPassword = $null
if (Test-Path $cfg) {
    foreach ($line in Get-Content $cfg) {
        if ($line -match '^\s*password:\s*(\S+)\s*$') { $dbPassword = $Matches[1]; break }
    }
}

Write-Output '[MySQL] Sending shutdown via mysqladmin...'
$cmdArgs = @('-h', '127.0.0.1', '-P', "$port", '-uroot')
if ($dbPassword) { $cmdArgs += "-p$dbPassword" }

# Temporarily relax EAP: in PS 5.1, native stderr (mysqladmin's password
# warning) redirected via 2>&1 raises a terminating NativeCommandError under
# $ErrorActionPreference = 'Stop'.
$ErrorActionPreference = 'Continue'
$output = & $mysqladmin @cmdArgs shutdown 2>&1
$ErrorActionPreference = 'Stop'
if ($LASTEXITCODE -ne 0) {
    Write-Output "[MySQL] FAILED: mysqladmin shutdown returned exit code $LASTEXITCODE."
    Write-Output ($output | Out-String).Trim()
    exit 1
}

# Wait until port 3306 stops accepting connections (max 30s)
$deadline = (Get-Date).AddSeconds(30)
while ((Get-Date) -lt $deadline) {
    if (-not (Test-MySqlPort)) {
        Write-Output '[MySQL] Stopped cleanly (port 3306 closed).'
        exit 0
    }
    Start-Sleep -Milliseconds 500
}

Write-Output '[MySQL] WARN: shutdown sent but port 3306 still open after 30s.'
Write-Output "[MySQL] Check error log (*.err) under $mysqlDir\data\."
exit 1

# Stop TestPilot frontend dev server (Vite, port 5173)
# Locates the node process listening on 5173 (vite binds IPv6 ::1, so we look
# up the listener socket instead of probing 127.0.0.1), walks up its parent
# chain (vite node <- npm node <- npm.cmd cmd <- powershell <- host cmd /k
# window) and kills that tree so the detached window closes too.
# Exit codes: 0 = stopped or not running, 1 = failed
$ErrorActionPreference = 'Stop'
$port = 5173

function Get-ListenPid {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($conn) { return [int]$conn.OwningProcess }
    return $null
}

$vitePid = Get-ListenPid
if (-not $vitePid) {
    Write-Output "[Frontend] Nothing listening on port $port, skip."
    exit 0
}

# Only ancestors from our own start scripts are killable; stop the walk at
# anything else (e.g. explorer.exe, a terminal app) to avoid killing
# unrelated processes.
$killable = @('node.exe', 'cmd.exe', 'powershell.exe', 'pwsh.exe')
$top = $vitePid
$cur = Get-CimInstance Win32_Process -Filter "ProcessId = $vitePid"
$depth = 0
while ($cur -and $depth -lt 10) {
    if ($killable -notcontains $cur.Name) { break }
    $top = $cur.ProcessId
    $cur = Get-CimInstance Win32_Process -Filter "ProcessId = $($cur.ParentProcessId)"
    $depth++
}

Write-Output "[Frontend] Vite pid $vitePid, killing tree rooted at pid $top"
taskkill /PID $top /T /F

# Confirm the port is released
$deadline = (Get-Date).AddSeconds(10)
while ((Get-Date) -lt $deadline) {
    if (-not (Get-ListenPid)) {
        Write-Output "[Frontend] Stopped (port $port released)."
        exit 0
    }
    Start-Sleep -Milliseconds 500
}

Write-Output "[Frontend] WARN: port $port still listening after taskkill."
exit 1

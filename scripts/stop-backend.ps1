$ErrorActionPreference = 'SilentlyContinue'
# Stop TestPilot backend java process (classpath contains TestPilot-my)
$procs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'"
foreach ($p in $procs) {
    if ($p.CommandLine -match 'TestPilot-my') {
        Write-Output "Stopping PID $($p.ProcessId)"
        Stop-Process -Id $p.ProcessId -Force
    }
}
Start-Sleep -Seconds 2
Write-Output 'done'

# V13 迁移链验证（字节级直传版）：Start-Process 重定向 stdin，避免 PowerShell 管道编码损坏
$mysql = 'D:\software\mysql-8.0\bin\mysql.exe'
$cnf = 'D:\develop\lemon-TestPilot\backend\scripts\backup\mysql-client.cnf'
$dir = 'D:\develop\lemon-TestPilot\backend\src\main\resources\db\migration'
$errFile = 'D:\develop\lemon-TestPilot\backend\scripts\backup\_mig_err.txt'
$outFile = 'D:\develop\lemon-TestPilot\backend\scripts\backup\_mig_out.txt'

& $mysql "--defaults-extra-file=$cnf" -e 'DROP DATABASE IF EXISTS lemon_testpilot_migcheck; CREATE DATABASE lemon_testpilot_migcheck DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;'
if ($LASTEXITCODE -ne 0) { Write-Output 'CREATE TEMP DB FAILED'; exit 1 }

$fail = 0
foreach ($n in 1..13) {
    $f = Get-ChildItem $dir -Filter "V${n}__*.sql"
    if (-not $f) { Write-Output "MISSING V$n"; $fail++; continue }
    $p = Start-Process -FilePath $mysql `
        -ArgumentList "--defaults-extra-file=$cnf", '-D', 'lemon_testpilot_migcheck', '--default-character-set=utf8mb4' `
        -RedirectStandardInput $f.FullName -RedirectStandardOutput $outFile -RedirectStandardError $errFile `
        -Wait -NoNewWindow -PassThru
    if ($p.ExitCode -eq 0) {
        Write-Output "OK   $($f.Name)"
    } else {
        Write-Output "FAIL $($f.Name)"
        Get-Content $errFile | ForEach-Object { Write-Output "  $_" }
        $fail++
    }
}
Write-Output "RESULT: $fail failure(s)"

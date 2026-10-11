@echo off
rem Stop TestPilot middleware: MySQL (port 3306) GRACEFULLY.
rem Uses mysqladmin shutdown so InnoDB flushes and closes cleanly.
rem Never force-kill mysqld - crash recovery on next start is the last resort.
rem Root password is read from backend\src\main\resources\application-local.yml
rem (git-ignored), so no credentials are stored in this script.
rem Exit codes: 0 = stopped or already stopped, 1 = failed

setlocal
set "mysqlDir=D:\software\mysql-8.0"
set "mysqladmin=%mysqlDir%\bin\mysqladmin.exe"
set "port=3306"

call :check_port
if errorlevel 1 (
    echo [MySQL] Not running on port %port%, skip.
    exit /b 0
)

if not exist "%mysqladmin%" (
    echo [MySQL] mysqladmin.exe not found under %mysqlDir% - cannot shut down gracefully.
    echo [MySQL] Do NOT taskkill mysqld unless data loss is acceptable.
    pause
    exit /b 1
)

rem Read datasource password from backend local config (git-ignored)
set "repoRoot=%~dp0.."
set "cfg=%repoRoot%\backend\src\main\resources\application-local.yml"
set "dbPassword="
if exist "%cfg%" (
    for /f "tokens=2 delims=: " %%p in ('findstr /R /C:"^ *password: *[^ :]" "%cfg%"') do (
        if not defined dbPassword set "dbPassword=%%p"
    )
)

echo [MySQL] Sending shutdown via mysqladmin...
if defined dbPassword goto with_password
"%mysqladmin%" -h 127.0.0.1 -P %port% -uroot shutdown
goto shutdown_sent

:with_password
"%mysqladmin%" -h 127.0.0.1 -P %port% -uroot -p%dbPassword% shutdown

:shutdown_sent
if errorlevel 1 (
    echo [MySQL] FAILED: mysqladmin shutdown returned an error.
    echo [MySQL] Check the password in backend\src\main\resources\application-local.yml.
    pause
    exit /b 1
)

rem Wait until port 3306 stops listening (max 30s)
set /a tries=0
:wait_loop
ping -n 2 127.0.0.1 >nul
call :check_port
if errorlevel 1 goto stopped
set /a tries+=1
if %tries% lss 30 goto wait_loop

echo [MySQL] WARN: shutdown sent but port %port% still open after 30s.
echo [MySQL] Check error log *.err under %mysqlDir%\data\.
pause
exit /b 1

:stopped
echo [MySQL] Stopped cleanly. Port %port% closed.
exit /b 0

:check_port
rem Errorlevel 0 = something LISTENs on %port% in the local address column.
netstat -ano | findstr /C:":%port% " | findstr /C:"LISTENING" >nul 2>&1
exit /b %errorlevel%

@echo off
rem Start TestPilot middleware: MySQL (portable install, port 3306).
rem mysqld runs in the FOREGROUND inside its own visible console window
rem (title: "TestPilot MySQL (3306)"). This launcher is idempotent: if 3306
rem is already listening it skips; otherwise it spawns that window and waits
rem until the port is listening (max 30s), then exits.
rem Stop MySQL with scripts\stop-mysql.bat (graceful) or Ctrl+C in its window.
rem Exit codes: 0 = ready (already running or just started), 1 = failed

setlocal
set "mysqlDir=D:\software\mysql-8.0"
set "mysqld=%mysqlDir%\bin\mysqld.exe"
set "defaults=%mysqlDir%\my.ini"
set "port=3306"

call :check_port
if not errorlevel 1 (
    echo [MySQL] Already running on port %port%, skip.
    exit /b 0
)

if exist "%mysqld%" if exist "%defaults%" goto do_start
echo [MySQL] mysqld.exe or my.ini not found under %mysqlDir%.
echo [MySQL] Install MySQL there or start it manually, then retry.
pause
exit /b 1

:do_start
echo [MySQL] Starting mysqld in a visible foreground window...
start "TestPilot MySQL (%port%)" /D "%mysqlDir%" "%mysqld%" "--defaults-file=%defaults%"

rem Wait until port 3306 is listening (max 30s)
set /a tries=0
:wait_loop
ping -n 2 127.0.0.1 >nul
call :check_port
if not errorlevel 1 (
    echo [MySQL] Ready on port %port%.
    exit /b 0
)
set /a tries+=1
if %tries% lss 30 goto wait_loop

echo [MySQL] FAILED: port %port% not ready within 30s.
echo [MySQL] Check the "TestPilot MySQL (%port%)" window and error log *.err under %mysqlDir%\data\.
pause
exit /b 1

:check_port
rem Errorlevel 0 = something LISTENs on %port% in the local address column.
netstat -ano | findstr /C:":%port% " | findstr /C:"LISTENING" >nul 2>&1
exit /b %errorlevel%

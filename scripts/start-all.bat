@echo off
rem Start TestPilot: MySQL (3306) + backend (3344) + frontend (5173).
rem Each service runs in the FOREGROUND of its own visible window
rem ("TestPilot MySQL (3306)" / "TestPilot Backend (3344)" / "TestPilot Frontend (5173)").
rem Each start-*.ps1 below spawns that window and waits until its port is ready,
rem so when this batch exits all three services are up and visible.
rem
rem Error handling uses goto labels instead of parenthesized blocks on purpose:
rem echo text containing parentheses (e.g. "TestPilot MySQL (3306)") inside an
rem "if errorlevel 1 ( ... )" block breaks cmd's block parser and aborts the
rem whole batch with e.g. "此时不应有 under。".

echo Starting MySQL middleware on port 3306...
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0start-mysql.ps1"
if errorlevel 1 goto mysql_failed

echo.
echo Starting backend on port 3344...
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0start-backend.ps1"
if errorlevel 1 goto backend_failed

echo.
echo Starting frontend on port 5173...
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0start-frontend.ps1"
if errorlevel 1 goto frontend_failed

echo.
echo MySQL   : port 3306 (window "TestPilot MySQL (3306)")
echo Backend : http://localhost:3344 (window "TestPilot Backend (3344)")
echo Frontend: http://localhost:5173 (window "TestPilot Frontend (5173)")
echo.
echo Stop everything with scripts\stop-all.bat (or Ctrl+C in each window).
echo (First run: edit backend/src/main/resources/application-local.yml for MySQL password)
goto :eof

:mysql_failed
echo [Error] MySQL failed to start. Check the "TestPilot MySQL (3306)" window or error log (*.err) under D:\software\mysql-8.0\data\
pause
exit /b 1

:backend_failed
echo [Error] Backend failed to start. Check the "TestPilot Backend (3344)" window.
pause
exit /b 1

:frontend_failed
echo [Error] Frontend failed to start. Check the "TestPilot Frontend (5173)" window.
pause
exit /b 1

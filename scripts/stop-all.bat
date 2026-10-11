@echo off
rem Stop TestPilot: frontend (5173) + backend (3344) + MySQL (3306)
rem Reverse start order: apps first, MySQL last (graceful shutdown)
echo Stopping frontend dev server on port 5173...
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0stop-frontend.ps1"
echo.
echo Stopping backend on port 3344...
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0stop-backend.ps1"
echo.
echo Stopping MySQL on port 3306 (graceful shutdown)...
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0stop-mysql.ps1"
echo.
echo TestPilot stopped.

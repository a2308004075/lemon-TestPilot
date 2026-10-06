@echo off
rem Start TestPilot: backend (3344) + frontend (5173) in separate windows
start "TestPilot Backend (3344)" cmd /k powershell -ExecutionPolicy Bypass -File "%~dp0start-backend.ps1"
timeout /t 5 /nobreak >nul
start "TestPilot Frontend (5173)" cmd /k powershell -ExecutionPolicy Bypass -File "%~dp0start-frontend.ps1"
echo.
echo Backend : http://localhost:3344
echo Frontend: http://localhost:5173
echo (First run: edit backend/src/main/resources/application-local.yml for MySQL password)

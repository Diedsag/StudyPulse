@echo off
setlocal

cd /d "%~dp0"

if not exist "venv\Scripts\python.exe" (
    echo Venv not found. Run setup-backend.cmd first.
    pause
    exit /b 1
)

echo Seeding database...
"venv\Scripts\python.exe" seed.py

pause
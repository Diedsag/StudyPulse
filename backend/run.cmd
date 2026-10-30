@echo off
setlocal

cd /d "%~dp0"

if not exist "venv\Scripts\python.exe" (
    echo Venv not found. Run setup-backend.cmd first.
    pause
    exit /b 1
)

echo Starting StudyPulse Flask API...
"venv\Scripts\python.exe" run.py
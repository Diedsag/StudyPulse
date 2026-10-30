@echo off
setlocal

cd /d "%~dp0"

if not exist "venv\Scripts\python.exe" (
    echo Creating Python virtual environment...
    python -m venv venv
    if errorlevel 1 (
        py -3 -m venv venv
    )
)

echo Upgrading pip...
"venv\Scripts\python.exe" -m pip install --upgrade pip

echo Installing dependencies...
"venv\Scripts\python.exe" -m pip install -r requirements.txt

echo Backend environment is ready.
pause
$ErrorActionPreference = 'Stop'

Set-Location -Path $PSScriptRoot

$venvPython = Join-Path $PSScriptRoot 'venv\Scripts\python.exe'

if (-not (Test-Path $venvPython)) {
    Write-Host 'Creating Python virtual environment...' -ForegroundColor Cyan

    try {
        python -m venv venv
    } catch {
        py -3 -m venv venv
    }
}

Write-Host 'Upgrading pip...' -ForegroundColor Cyan
& $venvPython -m pip install --upgrade pip

Write-Host 'Installing dependencies...' -ForegroundColor Cyan
& $venvPython -m pip install -r requirements.txt

Write-Host 'Backend environment is ready.' -ForegroundColor Green
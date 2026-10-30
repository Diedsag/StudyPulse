$ErrorActionPreference = 'Stop'

Set-Location -Path $PSScriptRoot

$venvPython = Join-Path $PSScriptRoot 'venv\Scripts\python.exe'

if (-not (Test-Path $venvPython)) {
    throw 'Venv not found. Run setup-backend.ps1 first.'
}

Write-Host 'Seeding database...' -ForegroundColor Cyan
& $venvPython seed.py
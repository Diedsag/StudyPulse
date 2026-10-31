$ErrorActionPreference = 'Stop'

Set-Location -Path $PSScriptRoot

Write-Host 'Removing old backend SQLite databases...' -ForegroundColor Cyan

Get-ChildItem -Path . -Recurse -Filter *.db |
    Where-Object { $_.FullName -notmatch '\\venv\\' } |
    Remove-Item -Force

Write-Host 'Running seed...' -ForegroundColor Cyan

$venvPython = Join-Path . 'venv\Scripts\python.exe'

if (-not (Test-Path $venvPython)) {
    throw 'Venv not found. Run setup-backend.ps1 first.'
}

& $venvPython seed.py

Write-Host 'Backend database reset complete.' -ForegroundColor Green
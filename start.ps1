# M-OS — запуск полного стека (PostgreSQL + Backend + Frontend + nginx)
# Использование: .\start.ps1   или   npm start

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker не найден. Установите Docker Desktop и запустите его."
}

if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    Write-Host ""
    Write-Host "Создан .env из .env.example — проверьте POSTGRES_PASSWORD, JWT_SECRET и MOS_CORS_ALLOWED_ORIGINS." -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "Запуск M-OS (docker compose up --build)..." -ForegroundColor Cyan
Write-Host "После старта откройте http://localhost или http://<IP-ноутбука> в локальной сети." -ForegroundColor Cyan
Write-Host "Остановка: Ctrl+C, затем docker compose down" -ForegroundColor DarkGray
Write-Host ""

docker compose up --build

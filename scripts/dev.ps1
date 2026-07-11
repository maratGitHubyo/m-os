# M-OS — локальная разработка: PostgreSQL + Backend + Frontend
# Использование: npm run dev

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker не найден (нужен для PostgreSQL)."
}

Write-Host "1/3 PostgreSQL..." -ForegroundColor Cyan
docker compose -f docker-compose.dev.yml up -d

Write-Host "2/3 Backend (новое окно)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD\backend'; mvn spring-boot:run"

Start-Sleep -Seconds 2

Write-Host "3/3 Frontend (новое окно)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD\frontend'; if (-not (Test-Path node_modules)) { npm install }; npm run dev"

Write-Host ""
Write-Host "Готово:" -ForegroundColor Green
Write-Host "  Frontend: http://localhost:5173"
Write-Host "  Backend:  http://localhost:8080/actuator/health"
Write-Host "  PostgreSQL: localhost:5432"
Write-Host ""
Write-Host "Backend и Frontend работают в отдельных окнах. Закройте их для остановки." -ForegroundColor DarkGray

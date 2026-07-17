# Starts ngrok → local nginx (:80), prints public URL, updates .env CORS/QR, recreates backend.
# Prerequisite: ngrok config add-authtoken <YOUR_TOKEN>
# Usage (from repo root):
#   powershell -ExecutionPolicy Bypass -File .\scripts\start-ngrok.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
  [System.Environment]::GetEnvironmentVariable("Path", "User")

if (-not (Get-Command ngrok -ErrorAction SilentlyContinue)) {
  Write-Error "ngrok not found. Install: winget install --id Ngrok.Ngrok -e --source winget"
}

$configPath = Join-Path $env:LOCALAPPDATA "ngrok\ngrok.yml"
if (-not (Test-Path $configPath)) {
  Write-Host @"
ngrok is not authenticated yet.

1. Sign up: https://dashboard.ngrok.com/signup
2. Copy token: https://dashboard.ngrok.com/get-started/your-authtoken
3. Run:
   ngrok config add-authtoken YOUR_TOKEN_HERE
4. Re-run this script.
"@
  exit 1
}

$envFile = Join-Path $root ".env"
if (-not (Test-Path $envFile)) {
  Write-Error ".env not found. Copy .env.example to .env first."
}

# Kill previous local ngrok if any
Get-Process ngrok -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1

$logOut = Join-Path $env:TEMP "mos-ngrok-out.log"
$logErr = Join-Path $env:TEMP "mos-ngrok-err.log"
Remove-Item $logOut, $logErr -ErrorAction SilentlyContinue

Write-Host "Starting ngrok http 80 ..."
$ngrok = Start-Process -FilePath "ngrok" `
  -ArgumentList @("http", "80", "--log=stdout", "--log-level=info") `
  -PassThru -WindowStyle Minimized `
  -RedirectStandardOutput $logOut `
  -RedirectStandardError $logErr

$publicUrl = $null
for ($i = 0; $i -lt 25; $i++) {
  Start-Sleep -Seconds 1

  if ($ngrok.HasExited) {
    break
  }

  try {
    $tunnels = Invoke-RestMethod -Uri "http://127.0.0.1:4040/api/tunnels" -TimeoutSec 2
    $https = $tunnels.tunnels | Where-Object { $_.public_url -like "https://*" } | Select-Object -First 1
    if ($https) {
      $publicUrl = $https.public_url
      break
    }
  } catch {
    # ngrok API not ready yet
  }
}

if (-not $publicUrl) {
  Stop-Process -Id $ngrok.Id -Force -ErrorAction SilentlyContinue

  $errText = ""
  if (Test-Path $logErr) { $errText += (Get-Content $logErr -Raw) }
  if (Test-Path $logOut) { $errText += (Get-Content $logOut -Raw) }

  Write-Host ""
  Write-Host "ngrok failed to create a public URL." -ForegroundColor Red
  if ($errText -match "ERR_NGROK_9040|do not allow agents to connect.*IP") {
    Write-Host @"

Причина: ngrok блокирует ваш IP (ERR_NGROK_9040).
Это ограничение ngrok, не M-OS и не Docker.

Варианты:
  1) Для вечеринки в одной Wi-Fi — без ngrok:
       http://<IP-ноутбука>   (например http://192.168.1.138)
  2) VPN в другую страну → снова запустить этот скрипт
  3) Cloudflare Tunnel (часто работает там, где ngrok режет IP)

Лог: $logErr
"@
  } elseif ($errText) {
    Write-Host $errText
  } else {
    Write-Host "Is docker nginx listening on :80? Try: docker ps"
    Write-Host "Manual check: ngrok http 80"
  }
  exit 1
}

Write-Host ""
Write-Host "Public URL: $publicUrl"
Write-Host "ngrok UI:    http://127.0.0.1:4040"
Write-Host ""

$lanIp = (
  Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
  Where-Object { $_.IPAddress -like '192.168.*' } |
  Select-Object -ExpandProperty IPAddress -First 1
)
if (-not $lanIp) { $lanIp = "192.168.1.143" }

$content = Get-Content $envFile -Raw
$corsLine = "MOS_CORS_ALLOWED_ORIGINS=$publicUrl,http://$lanIp,http://localhost"
$qrLine = "MOS_QR_BASE_URL=$publicUrl/qr"

if ($content -match "(?m)^MOS_CORS_ALLOWED_ORIGINS=.*$") {
  $content = $content -replace "(?m)^MOS_CORS_ALLOWED_ORIGINS=.*$", $corsLine
} else {
  $content = $content.TrimEnd() + "`r`n$corsLine`r`n"
}

if ($content -match "(?m)^MOS_QR_BASE_URL=.*$") {
  $content = $content -replace "(?m)^MOS_QR_BASE_URL=.*$", $qrLine
} else {
  $content = $content.TrimEnd() + "`r`n$qrLine`r`n"
}

Set-Content -Path $envFile -Value $content -NoNewline
Write-Host "Updated .env:"
Write-Host "  $corsLine"
Write-Host "  $qrLine"
Write-Host ""
Write-Host "Recreating backend to apply env..."
docker compose up -d --force-recreate --no-deps backend

Write-Host ""
Write-Host "Ready. Open on phone (any network):"
Write-Host "  $publicUrl"
Write-Host ""
Write-Host "Keep ngrok running while guests use the app."
Write-Host "Dashboard: http://127.0.0.1:4040"
Write-Host "Stop: Stop-Process -Name ngrok"

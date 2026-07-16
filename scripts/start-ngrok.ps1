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

Write-Host "Starting ngrok http 80 ..."
$ngrok = Start-Process -FilePath "ngrok" -ArgumentList @("http", "80", "--log=stdout") -PassThru -WindowStyle Minimized

$publicUrl = $null
for ($i = 0; $i -lt 30; $i++) {
  Start-Sleep -Seconds 1
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
  Write-Error "Could not get ngrok public URL. Is docker nginx listening on :80? Open http://127.0.0.1:4040"
}

Write-Host ""
Write-Host "Public URL: $publicUrl"
Write-Host "ngrok UI:    http://127.0.0.1:4040"
Write-Host ""

$content = Get-Content $envFile -Raw
$corsLine = "MOS_CORS_ALLOWED_ORIGINS=$publicUrl,http://192.168.1.143,http://localhost"
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
Write-Host "Keep this terminal/session alive while guests use the app."
Write-Host "Dashboard: http://127.0.0.1:4040"
Write-Host "Stop: Stop-Process -Name ngrok"

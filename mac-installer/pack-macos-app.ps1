# pack-macos-app.ps1 - packages the Haval Installer .app + instructions PDF
# into a zip that preserves Unix executable bits (via zip-perms.js).
#
# PowerShell's Compress-Archive and Git Bash tar both strip the +x bits on
# Windows, which would make the .app unopenable on macOS. zip-perms.js writes
# the external file attributes by hand so Archive Utility restores them.
#
# Usage:  .\pack-macos-app.ps1 [-Apk <path-to-haval.apk>] [-OutDir <dir>]
param(
  [string]$Apk = "",
  [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$repo = Split-Path $root -Parent

if (-not $Apk) { $Apk = Join-Path $repo "app\build\outputs\apk\debug\app-debug.apk" }
if (-not $OutDir) { $OutDir = Join-Path $root "dist" }
if (-not (Test-Path $Apk)) { throw "APK not found: $Apk (build it first: .\build.ps1)" }

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$stage = Join-Path $env:TEMP "haval-app-stage"
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }

$app = Join-Path $stage "Haval Installer.app"
New-Item -ItemType Directory -Force -Path (Join-Path $app "Contents\MacOS") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $app "Contents\Resources\payloads") | Out-Null

Write-Host "[*] Assembling app bundle..." -ForegroundColor Cyan

# --- bundle skeleton ---
Copy-Item (Join-Path $root "app-src\Contents\Info.plist") (Join-Path $app "Contents\Info.plist")
Copy-Item (Join-Path $root "app-src\Contents\MacOS\HavalInstaller") (Join-Path $app "Contents\MacOS\HavalInstaller")
Copy-Item (Join-Path $root "app-src\Contents\Resources\menu.sh") (Join-Path $app "Contents\Resources\menu.sh")

# --- installer scripts + hook script + zero-dependency tools (perl) ---
foreach ($f in @("install-macos.sh", "car.py", "car.pl", "serve.pl", "mock-car.pl", "fetch-payloads.sh", "system_server.js")) {
  Copy-Item (Join-Path $root $f) (Join-Path $app "Contents\Resources\$f")
}

# --- payloads (frida binaries, shizuku, legacy hook, manifest) ---
Copy-Item (Join-Path $root "payloads\*") (Join-Path $app "Contents\Resources\payloads\")

# --- the APK (installed by the script; served to the car) ---
Copy-Item $Apk (Join-Path $app "Contents\Resources\haval.apk")
$apkSha = (Get-FileHash $Apk -Algorithm SHA256).Hash.ToLower()
$apkSize = [math]::Round((Get-Item $Apk).Length / 1MB, 1)
Write-Host "[+] APK bundled: $Apk ($apkSize MB, sha256 $apkSha)" -ForegroundColor Green

# --- instructions PDF (Chrome headless) ---
$chrome = @(
  (Join-Path $env:ProgramFiles "Google\Chrome\Application\chrome.exe"),
  (Join-Path ${env:ProgramFiles(x86)} "Google\Chrome\Application\chrome.exe"),
  (Join-Path $env:LOCALAPPDATA "Google\Chrome\Application\chrome.exe")
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $chrome) { throw "Chrome not found - cannot render the instructions PDF" }

$html = Join-Path $root "app-src\instrucoes.html"
$pdf  = Join-Path $stage "Instrucoes-Haval-Installer.pdf"
Write-Host "[*] Rendering instructions PDF (Chrome headless)..." -ForegroundColor Cyan
$ErrorActionPreference = "Continue"   # Chrome's stderr "bytes written" line is not an error
& $chrome --headless=new --disable-gpu --no-pdf-header-footer --print-to-pdf="$pdf" $html 2>$null | Out-Null
$ErrorActionPreference = "Stop"
if (-not (Test-Path $pdf)) { throw "PDF generation failed" }
Write-Host "[+] PDF: $((Get-Item $pdf).Name) ($([math]::Round((Get-Item $pdf).Length / 1KB)) KB)" -ForegroundColor Green

# --- archives with preserved exec bits (zip + tar.gz fallback) ---
$zip = Join-Path $OutDir "Haval-Installer-macOS.zip"
$tgz = Join-Path $OutDir "Haval-Installer-macOS.tar.gz"
foreach ($a in @($zip, $tgz)) { if (Test-Path $a) { Remove-Item $a -Force } }
node (Join-Path $root "zip-perms.js") --out $zip --app $app --pdf $pdf
if ($LASTEXITCODE -ne 0) { throw "zip-perms.js failed (zip)" }
node (Join-Path $root "zip-perms.js") --tgz --out $tgz --app $app --pdf $pdf
if ($LASTEXITCODE -ne 0) { throw "zip-perms.js failed (tar.gz)" }

Write-Host ""
Write-Host "[+] PACKAGED:" -ForegroundColor Green
Write-Host "    $((Split-Path $zip -Leaf))  sha256 $((Get-FileHash $zip -Algorithm SHA256).Hash.ToLower())" -ForegroundColor Green
Write-Host "    $((Split-Path $tgz -Leaf))  sha256 $((Get-FileHash $tgz -Algorithm SHA256).Hash.ToLower())" -ForegroundColor Green
Write-Host ""
Write-Host "Drop one of them in Google Drive, download on the Mac." -ForegroundColor Yellow
Write-Host "On the Mac, verify BEFORE unzipping:" -ForegroundColor Yellow
Write-Host "  shasum -a 256 ~/Downloads/<file>" -ForegroundColor White
Write-Host "If Archive Utility says 'damaged or incomplete', the download was" -ForegroundColor Yellow
Write-Host "truncated - redownload, or use the .tar.gz variant." -ForegroundColor Yellow

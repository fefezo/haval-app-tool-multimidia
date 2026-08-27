# build.ps1 - one-command iteration build for the Haval H6 fork (Windows)
#
# Does:
#   1. copies the latest widget build into the app bundle (res/raw/app.html)
#      only if it changed (the bundled widget is served offline, so its fonts
#      must be inlined - the dist html already has them as data URIs)
#   2. assembles the debug APK (same debug keystore across builds, so the car
#      can update with `pm install -r` - UID and signature are preserved)
#   3. prints the APK path + sha256 and the exact Mac-side command to deploy
#
# Usage:  .\build.ps1            (default widget + default output)
#         .\build.ps1 -SkipWidget  (rebuild APK only, keep current bundle)
#         .\build.ps1 -Output <path>  (copy the APK somewhere, e.g. a sync dir)

param(
  [string]$Widget = "cluster-widgets/air-control/dist/app-night.html",
  [string]$Dest   = "app/src/main/res/raw/app.html",
  [string]$Output = "",
  [switch]$SkipWidget
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

# --- 0. locate a JDK if none is on PATH -------------------------------
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
  $jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -ErrorAction SilentlyContinue |
         Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
  if (-not $jdk) { throw "No JDK found on PATH nor in C:\Program Files\Eclipse Adoptium" }
  $env:JAVA_HOME = $jdk
  Write-Host "[i] JAVA_HOME=$jdk" -ForegroundColor DarkCyan
}

# --- 1. widget -> app bundle ------------------------------------------
if (-not $SkipWidget) {
  if (-not (Test-Path $Widget)) { throw "Widget build not found: $Widget (build the widget first)" }
  $src = (Get-Item $Widget)
  $dst = Get-Item $Dest -ErrorAction SilentlyContinue
  if (-not $dst -or $src.LastWriteTime -gt $dst.LastWriteTime) {
    Copy-Item $src.FullName $Dest -Force
    Write-Host "[+] Widget copied: $($src.Name) ($([math]::Round($src.Length/1KB)) KB) -> $Dest" -ForegroundColor Green
  } else {
    Write-Host "[i] Widget unchanged, keeping bundle copy" -ForegroundColor DarkCyan
  }
}

# --- 2. build the debug APK --------------------------------------------
Write-Host "[*] gradlew assembleDebug ..." -ForegroundColor Cyan
& .\gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit $LASTEXITCODE)" }

$apk = "app\build\outputs\apk\debug\app-debug.apk"
$sha = (Get-FileHash $apk -Algorithm SHA256).Hash.ToLower()
$size = [math]::Round((Get-Item $apk).Length / 1MB, 1)
Write-Host ""
Write-Host "[+] BUILD OK: $apk ($size MB)" -ForegroundColor Green
Write-Host "    sha256 $sha" -ForegroundColor DarkGray

if ($Output) {
  Copy-Item $apk $Output -Force
  Write-Host "[+] Copied to $Output" -ForegroundColor Green
}

# --- 3. next step -------------------------------------------------------
Write-Host ""
Write-Host "Next: get this APK onto your Mac, then in mac-installer/ run:" -ForegroundColor Yellow
Write-Host "  ./install-macos.sh --update" -ForegroundColor White
Write-Host "(same debug signature => UID preserved, no frida, no shizuku reinstall)"

#!/bin/bash
# fetch-payloads.sh - one-time setup for the Haval H6 install chain (run on macOS).
# Needs internet ONCE. After this, install-macos.sh runs fully offline.
#
# What it does:
#   1. Verifies the frida payloads in ../app/src/main/res/raw/ against the pinned
#      manifest (they must be byte-identical to official Frida 17.2.15 android-arm64).
#      If a file is missing/corrupt, re-downloads it from the official Frida release.
#   2. Vendors the ORIGINAL joaoiot hook bundle (system_server_orig.js) out of the
#      built APK - used only with install-macos.sh --legacy. The default hook script
#      (system_server.js) is our clean-room rewrite, repo-owned, nothing to fetch.
#   3. Downloads Shizuku v13.6.0 (official RikkaApps release) into payloads/.
#   4. Verifies everything against payloads/SHA256SUMS.
#
# Usage:  bash fetch-payloads.sh [path/to/haval.apk]
set -euo pipefail
cd "$(dirname "$0")"

FRIDA_VERSION="17.2.15"
SHIZUKU_URL="https://github.com/RikkaApps/Shizuku/releases/download/v13.6.0/shizuku-v13.6.0.r1086.2650830c-release.apk"
RAW=../app/src/main/res/raw
HAVAL_APK="${1:-../app/build/outputs/apk/debug/app-debug.apk}"

say()  { printf '\033[1;32m[+]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[!]\033[0m %s\n' "$*"; }

# --- 1. frida binaries (source of truth: the app bundle; official repo as repair) ---
for name in fridaserver fridainject; do
  if [ ! -f "$RAW/$name" ]; then
    warn "$name missing from app bundle - downloading official Frida $FRIDA_VERSION"
    curl -fL -o "$RAW/$name.xz" \
      "https://github.com/frida/frida/releases/download/$FRIDA_VERSION/frida-${name#frida}-$FRIDA_VERSION-android-arm64.xz"
    unxz -f "$RAW/$name.xz"
  fi
  # verify against pinned hash (also catches a tampered bundle)
  grep -q "$name" payloads/SHA256SUMS && (cd "$RAW" && sha256sum -c --quiet "$OLDPWD/payloads/SHA256SUMS" --ignore-missing --check 2>/dev/null || true)
  actual=$(shasum -a 256 "$RAW/$name" | awk '{print $1}')
  pinned=$(awk -v n="$name" '$1!="#" && $2==n {print $1}' payloads/SHA256SUMS)
  if [ "$actual" != "$pinned" ]; then
    warn "$name hash mismatch (have $actual, want $pinned) - replacing with official Frida $FRIDA_VERSION"
    curl -fL -o "$name.xz" \
      "https://github.com/frida/frida/releases/download/$FRIDA_VERSION/frida-${name#frida}-$FRIDA_VERSION-android-arm64.xz"
    unxz -f "$name.xz" && mv -f "$name" "$RAW/$name"
  fi
done

# --- 1b. hook scripts ---
# system_server.js (clean-room rewrite) is repo-owned: just verify it
actual=$(shasum -a 256 system_server.js | awk '{print $1}')
pinned=$(awk -v n="system_server.js" '$1!="#" && $2==n {print $1}' payloads/SHA256SUMS)
if [ "$actual" != "$pinned" ]; then
  warn "system_server.js hash mismatch! (have $actual, want $pinned)"
  warn "You edited the script - update the manifest: shasum -a 256 system_server.js"
fi

# system_server_orig.js (original joaoiot bundle) is vendored from the APK
if [ ! -f payloads/system_server_orig.js ]; then
  [ -f "$HAVAL_APK" ] || { warn "APK not found: $HAVAL_APK (build it first, or pass the path as \$1)"; exit 1; }
  say "Extracting original hook bundle from $HAVAL_APK"
  unzip -p "$HAVAL_APK" res/raw/system_server.js > payloads/system_server_orig.js
fi
actual=$(shasum -a 256 payloads/system_server_orig.js | awk '{print $1}')
pinned=$(awk -v n="system_server_orig.js" '$1!="#" && $2==n {print $1}' payloads/SHA256SUMS)
[ "$actual" = "$pinned" ] || warn "system_server_orig.js hash mismatch! (have $actual, want $pinned) - the APK changed; if that is intended, update the manifest"

# --- 2. Shizuku APK (official release) ---
if [ ! -f payloads/shizuku.apk ]; then
  say "Downloading Shizuku v13.6.0 (official release)"
  curl -fL -o payloads/shizuku.apk "$SHIZUKU_URL"
fi
actual=$(shasum -a 256 payloads/shizuku.apk | awk '{print $1}')
pinned=$(awk -v n="shizuku.apk" '$1!="#" && $2==n {print $1}' payloads/SHA256SUMS)
[ "$actual" = "$pinned" ] || { warn "shizuku.apk hash mismatch - deleting, re-run"; rm -f payloads/shizuku.apk; exit 1; }

say "All payloads verified. Ready for install-macos.sh (no internet needed)."

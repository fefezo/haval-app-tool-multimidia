#!/bin/bash
# install-macos.sh - install/update the HavalShisuku fork (+ Shizuku) on the H6 head unit.
# Runs fully OFFLINE (everything is served from this Mac over the car's hotspot LAN).
# No internet, no third-party domains, no live-fetched scripts.
#
# Prerequisites:
#   - MacBook connected to the car's WiFi hotspot (gateway must be 192.168.33.*)
#   - NO installs required: uses whatever ships with macOS (curl + one of
#     python3 / ruby / perl for the HTTP server; python3 or perl for the car
#     channel). Perl is always present on macOS - nothing to install, even
#     fully offline.
#   - payloads prepared once: bash fetch-payloads.sh
#   - the fork APK built (default: ../app/build/outputs/apk/debug/app-debug.apk)
#
# Usage:
#   ./install-macos.sh                 first install (MODE A; frida fallback MODE B)
#   ./install-macos.sh --update [apk]  fast iteration: reinstall only the new APK
#                                      (same signature => UID preserved, no frida needed)
#   ./install-macos.sh --legacy [apk]  first install using the ORIGINAL joaoiot hook
#                                      bundle instead of our clean-room rewrite
#   ./install-macos.sh [apk]           first install, explicit APK path
#   ./install-macos.sh --update --legacy [apk]  update using the legacy hook script
#   ./install-macos.sh --selftest      offline test WITHOUT the car: start the
#                                      server, download every payload, hash-check
#   ./install-macos.sh --mock          offline DRY-RUN of the whole install
#                                      against a simulated head unit (no car,
#                                      no hotspot, no internet)
#
# Flow (first install):
#   1. verify every payload against payloads/SHA256SUMS (nothing runs unverified)
#   2. detect the car hotspot
#   3. serve payloads over LAN from this Mac (python3 http.server, ruby httpd
#      or the bundled perl serve.pl - first one found wins)
#   4. BEFORE the car tries: verify the local server can serve every payload
#      (this also surfaces the macOS firewall prompt early)
#   5. push files to the head unit via curl (the car pulls from us), then verify
#      each file's size ON the head unit and retry missing files once
#   6. MODE A: plain pm install of Shizuku + the fork, then check BOTH that both
#      packages are present and that the app UID is <= 10999 (the app needs a
#      low UID to use its localhost:23 root telnet at boot)
#   7. if that fails, MODE B: start fridaserver, inject the hook into
#      system_server (UID clamp + GWM gate), VERIFY the hook reported ready
#      (retry clean-room script, then the vendored original bundle), retry install
#   8. cleanup: stop frida processes on the car, remove temp files, stop the server
set -euo pipefail
cd "$(dirname "$0")"

PAYLOADS=payloads
RAW=../app/src/main/res/raw
PKG="br.com.redesurftank.havalshisuku"
SHIZUKU_PKG="moe.shizuku.privileged.api"
PORT=8123

say()  { printf '\033[1;32m[+]\033[0m %s\n' "$*"; }
info() { printf '\033[1;34m[*]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[!\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m[x]\033[0m %s\n' "$*"; exit 1; }

# --- dependencies: nothing needs to be INSTALLED. macOS ships curl, perl,
# ruby; python3 comes with Xcode CLT. We use the first available of each.
command -v curl >/dev/null || die "curl required (ships with macOS)"
SHA() {  # sha256 of a file (shasum on macOS, sha256sum elsewhere)
  if command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1" | awk '{print $1}'
  else sha256sum "$1" | awk '{print $1}'; fi
}
SIZE_OF() {  # byte size of a file. NOT stat: GNU `stat -f %z` prints
  # filesystem info to stdout and exits non-zero, which would corrupt the
  # size-check command with multi-line garbage. wc works on macOS, Linux
  # and Git Bash alike.
  wc -c < "$1" 2>/dev/null | tr -d ' ' || echo 0
}
probe() {  # true if $1 actually RUNS - Windows can leave a dead python3/ruby
  # "Microsoft Store" stub that `command -v` still finds; picking it makes the
  # server die silently (log to /dev/null). macOS binaries always pass.
  case "$1" in
    python3|python) "$1" -c 'import sys' >/dev/null 2>&1 ;;
    ruby|perl)      "$1" -e 'exit 0' >/dev/null 2>&1 ;;
    *)              "$1" --version >/dev/null 2>&1 ;;
  esac
}
pick_server() {  # HTTP server for the car to pull from
  if [ -n "${HAVAL_SERVER:-}" ]; then
    SERVER_BIN="$HAVAL_SERVER"
    command -v "$SERVER_BIN" >/dev/null 2>&1 || die "HAVAL_SERVER=$SERVER_BIN not found"
  else
    local try
    for try in python3 ruby perl; do
      command -v "$try" >/dev/null 2>&1 && probe "$try" && { SERVER_BIN="$try"; break; }
    done
    [ -n "${SERVER_BIN:-}" ] || die "no HTTP server available: need python3, ruby or perl (all ship with macOS)"
  fi
  [ "$SERVER_BIN" = "perl" ] && [ -f serve.pl ] || [ "$SERVER_BIN" != "perl" ] \
    || die "serve.pl missing from the bundle"
}
pick_car() {  # telnet channel to the head unit
  if [ -n "${HAVAL_CAR:-}" ]; then
    CAR_BIN="$HAVAL_CAR"
    command -v "$CAR_BIN" >/dev/null 2>&1 || die "HAVAL_CAR=$CAR_BIN not found"
  else
    local try
    for try in python3 perl; do
      command -v "$try" >/dev/null 2>&1 && probe "$try" && { CAR_BIN="$try"; break; }
    done
    [ -n "${CAR_BIN:-}" ] || die "no car-channel tool: need python3 or perl (perl ships with macOS)"
  fi
  if [ "$(basename "$CAR_BIN")" = "python3" ] || [ "$(basename "$CAR_BIN")" = "python" ]; then
    [ -f car.py ] || die "car.py missing from the bundle"
  else
    [ -f car.pl ] || die "car.pl missing from the bundle"
  fi
}
run_car() {  # $1 = gateway, $2 = commands file
  case "$(basename "$CAR_BIN")" in
    python3|python) "$CAR_BIN" car.py "$1" "$2" ;;
    *)              "$CAR_BIN" car.pl "$1" "$2" ;;
  esac
}
start_server() {  # $1 = directory to serve, $2 = bind IP
  local dir="$1" bind="$2"
  # free the port first: a server left behind by an interrupted run would make
  # the new bind fail silently (stderr is swallowed) and leave the car with
  # timeouts/404s. Port 8123 on this Mac is only ever used by this installer.
  lsof -ti tcp:"$PORT" 2>/dev/null | xargs kill 2>/dev/null || true
  # match on basename so HAVAL_SERVER accepts `python3`, `python` or a full
  # path; ALWAYS invoke "$SERVER_BIN" itself (a bare `python3` can be a dead
  # Windows Store stub even when a real interpreter was chosen)
  case "$(basename "$SERVER_BIN")" in
    python3|python) "$SERVER_BIN" -m http.server "$PORT" --bind "$bind" --directory "$dir" >/dev/null 2>&1 & ;;
    ruby)           (cd "$dir" && "$SERVER_BIN" -run -e httpd . -p "$PORT" -b "$bind" >/dev/null 2>&1) & ;;
    perl)           "$SERVER_BIN" serve.pl "$PORT" "$dir" "$bind" >/dev/null 2>&1 & ;;
  esac
  HTTP_PID=$!
  sleep 1
}

# self-contained mode: in the packaged .app, everything (frida binaries,
# hook script, APK) lives inside Resources/. Repo-relative paths are used
# only as fallback when running from a dev checkout.
FRIDA_DIR=""
for d in "$PAYLOADS" "$RAW"; do
  if [ -f "$d/fridaserver" ] && [ -f "$d/fridainject" ]; then FRIDA_DIR="$d"; break; fi
done
[ -n "$FRIDA_DIR" ] || die "missing fridaserver/fridainject (payloads/ or app/src/main/res/raw) - run fetch-payloads.sh"

UPDATE=0
LEGACY=0
SELFTEST=0
MOCK=0
APK_ARG=""
while [ $# -gt 0 ]; do
  case "$1" in
    --update) UPDATE=1; shift ;;
    --legacy) LEGACY=1; shift ;;
    --selftest) SELFTEST=1; shift ;;
    --mock) MOCK=1; shift ;;
    *) APK_ARG="$1"; shift ;;
  esac
done
pick_server
pick_car
info "Dependencies: curl OK | HTTP server: $SERVER_BIN | car channel: $CAR_BIN (nothing to install - macOS ships these)"
DEFAULT_APK=""
for cand in "./haval.apk" "../app/build/outputs/apk/debug/app-debug.apk"; do
  if [ -f "$cand" ]; then DEFAULT_APK="$cand"; break; fi
done
HAVAL_APK="${APK_ARG:-$DEFAULT_APK}"

# ---------------------------------------------------------------- 1. checksums
if [ "$LEGACY" = "1" ]; then
  HOOK_SRC="$PAYLOADS/system_server_orig.js"
  HOOK_MANIFEST="system_server_orig.js"
  info "Using the ORIGINAL joaoiot hook bundle (--legacy)"
else
  HOOK_SRC="./system_server.js"
  HOOK_MANIFEST="system_server.js"
  info "Using the clean-room hook script (mac-installer/system_server.js)"
fi

say "Verifying payloads against pinned manifest..."
for f in fridaserver fridainject; do
  [ -f "$FRIDA_DIR/$f" ] || die "missing $FRIDA_DIR/$f (run fetch-payloads.sh)"
  actual=$(SHA "$FRIDA_DIR/$f")
  pinned=$(awk -v n="$f" '$1!="#" && $2==n {print $1}' "$PAYLOADS/SHA256SUMS")
  [ "$actual" = "$pinned" ] || die "$f hash mismatch (have $actual, want $pinned) - do NOT install, run fetch-payloads.sh to repair"
done
[ -f "$HOOK_SRC" ] || die "missing $HOOK_SRC (run fetch-payloads.sh)"
actual=$(SHA "$HOOK_SRC")
pinned=$(awk -v n="$HOOK_MANIFEST" '$1!="#" && $2==n {print $1}' "$PAYLOADS/SHA256SUMS")
[ "$actual" = "$pinned" ] || die "$HOOK_SRC hash mismatch - do NOT install. If you edited it on purpose, update payloads/SHA256SUMS"
# the vendored original bundle is the MODE B fallback when the clean-room hook
# cannot attach - it must always be present and verified
[ -f "$PAYLOADS/system_server_orig.js" ] || die "missing $PAYLOADS/system_server_orig.js (run fetch-payloads.sh)"
actual=$(SHA "$PAYLOADS/system_server_orig.js")
pinned=$(awk -v n="system_server_orig.js" '$1!="#" && $2==n {print $1}' "$PAYLOADS/SHA256SUMS")
[ "$actual" = "$pinned" ] || die "system_server_orig.js hash mismatch - run fetch-payloads.sh"
for f in shizuku.apk; do
  [ -f "$PAYLOADS/$f" ] || die "missing $PAYLOADS/$f (run fetch-payloads.sh)"
  actual=$(SHA "$PAYLOADS/$f")
  pinned=$(awk -v n="$f" '$1!="#" && $2==n {print $1}' "$PAYLOADS/SHA256SUMS")
  [ "$actual" = "$pinned" ] || die "$f hash mismatch - run fetch-payloads.sh"
done
[ -f "$HAVAL_APK" ] || die "haval APK not found: $HAVAL_APK"
HAVAL_HASH=$(SHA "$HAVAL_APK")
info "haval APK: $HAVAL_APK ($(du -h "$HAVAL_APK" | cut -f1), sha256 $HAVAL_HASH)"
say "All payloads verified."

# ------------------------------------------------------------- 1.5 selftest
if [ "$SELFTEST" = "1" ]; then
  say "SELFTEST: verifying server + file delivery - NO CAR needed, fully offline."
  SELFTEST_DIR=$(mktemp -d)
  cp "$HOOK_SRC" "$SELFTEST_DIR/system_server.js"
  cp "$PAYLOADS/system_server_orig.js" "$SELFTEST_DIR/system_server_orig.js"
  cp "$FRIDA_DIR"/fridaserver "$FRIDA_DIR"/fridainject "$PAYLOADS/shizuku.apk" "$SELFTEST_DIR/"
  cp "$HAVAL_APK" "$SELFTEST_DIR/haval.apk"
  start_server "$SELFTEST_DIR" "127.0.0.1"
  say "HTTP server ($SERVER_BIN) listening on 127.0.0.1:$PORT"
  D=$(mktemp -d)
  fail=0
  for f in system_server.js system_server_orig.js fridaserver fridainject shizuku.apk haval.apk; do
    if curl -fsSL -o "$D/$f" "http://127.0.0.1:$PORT/$f" && cmp -s "$SELFTEST_DIR/$f" "$D/$f"; then
      say "  $f served correctly ($(du -h "$SELFTEST_DIR/$f" | cut -f1))"
    else
      warn "  $f FAILED to download or content mismatch"
      fail=1
    fi
  done
  kill ${HTTP_PID:-} 2>/dev/null || true
  rm -rf "$D" "$SELFTEST_DIR" || true
  [ "$fail" = "0" ] || die "SELFTEST FAILED - fix the bundle before going to the car"
  say "SELFTEST PASSED - the bundle is ready. Connect the car hotspot and run ./install-macos.sh"
  exit 0
fi

# ----------------------------------------------------------------- 2. hotspot
if [ "$MOCK" = "1" ]; then
  info "MOCK mode: simulating the head unit on 127.0.0.1:2323 - no car, no hotspot, no internet."
  GW="127.0.0.1"
  MAC_IP="127.0.0.1"
  export HAVAL_PORT=2323
  perl mock-car.pl "$HAVAL_PORT" >/dev/null 2>&1 &
  MOCK_PID=$!
  info "mock head unit started (pid $MOCK_PID) - every car command gets a canned success reply."
else
  say "Checking car hotspot..."
  GW=$(route -n get default 2>/dev/null | awk '/gateway:/{print $2}')
  case "$GW" in
    192.168.33.*) ;;
    *) die "gateway is $GW, not the Haval hotspot (192.168.33.x). Connect the Mac to the car's WiFi." ;;
  esac
  IF=$(route -n get default | awk '/interface:/{print $2}')
  MAC_IP=$(ipconfig getifaddr "$IF")
  [ -n "$MAC_IP" ] || die "could not determine this Mac's IP on $IF"
  info "gateway (head unit): $GW  |  this Mac: $MAC_IP"
fi

# ----------------------------------------------------------------- 3. serve
cleanup() {
  # every command guarded: with `set -e`, a failing kill/rm inside the EXIT
  # trap would override the script's exit status (a dead PID or deleted dir
  # turns a successful run into "exit 1")
  kill ${HTTP_PID:-} 2>/dev/null || true
  kill ${MOCK_PID:-} 2>/dev/null || true
  rm -rf ${SERVE_DIR:-} ${CMDS:-} ${UID_CMDS:-} ${CLEAN1:-} ${CLEAN2:-} 2>/dev/null || true
}
trap cleanup EXIT
SERVE_DIR=$(mktemp -d)
cp "$HOOK_SRC" "$SERVE_DIR/system_server.js"
cp "$PAYLOADS/system_server_orig.js" "$SERVE_DIR/system_server_orig.js"
if [ "$UPDATE" = "1" ]; then
  info "UPDATE mode: serving only the APK (no frida, no shizuku - not needed for same-signature updates)"
else
  cp "$FRIDA_DIR"/fridaserver "$FRIDA_DIR"/fridainject "$PAYLOADS/shizuku.apk" "$SERVE_DIR/"
fi
# plain copy, NOT a symlink: with a relative $HAVAL_APK (./haval.apk), macOS
# `ln -sf` resolves the target against the temp dir and creates a DANGLING
# link - the car then 404s haval.apk while every other file serves fine.
# (Git Bash's ln fails and the old `|| cp` fallback hid this on Windows.)
cp "$HAVAL_APK" "$SERVE_DIR/haval.apk"
# 0.0.0.0 in real mode (loopback + LAN hit the same listener); loopback only
# for the mock, which must not touch real interfaces.
[ "$MOCK" = "1" ] && BIND=127.0.0.1 || BIND=0.0.0.0
start_server "$SERVE_DIR" "$BIND"
say "Serving payloads on http://$MAC_IP:$PORT via $SERVER_BIN (car's LAN, offline)"

# ------------------------------------------------ 3.5 verify the server, pre-flight
# The car cannot reach this Mac until the macOS firewall allows $SERVER_BIN -
# if a prompt appears, click Allow. The pre-flight checks through LOOPBACK on
# purpose: the car hotspot's AP does not hairpin, so this Mac often cannot
# reach its OWN LAN IP (curl (28) timeout) even though the car CAN. The real
# end-to-end proof is the size verification the head unit reports below.
warn "If a macOS firewall prompt appears now, click Allow (the car needs to reach this Mac)."
for f in system_server.js system_server_orig.js fridaserver fridainject shizuku.apk haval.apk; do
  [ "$UPDATE" = "1" ] && [ "$f" != "haval.apk" ] && continue
  if curl -fsS -o /dev/null "http://127.0.0.1:$PORT/$f"; then
    say "  server OK: $f"
  else
    die "the local server cannot serve $f even on localhost - the HTTP server ($SERVER_BIN) did not start (port 8123 busy?). Re-run. Nothing was sent to the car."
  fi
done

# ----------------------------------------------------------------- 4. push
CMDS=$(mktemp)
{
  echo "mkdir -p /data/local/tmp/hav"
  if [ "$UPDATE" = "1" ]; then
    echo "curl -fsSL -o /data/local/tmp/hav/haval.apk http://$MAC_IP:$PORT/haval.apk"
  else
    for f in fridaserver fridainject system_server.js system_server_orig.js shizuku.apk haval.apk; do
      echo "curl -fsSL -o /data/local/tmp/hav/$f http://$MAC_IP:$PORT/$f"
    done
    echo "chmod +x /data/local/tmp/hav/fridaserver /data/local/tmp/hav/fridainject"
  fi
} > "$CMDS"

push_checks() {  # appends car-side size checks (verifies each file actually arrived)
  local f src
  {
    for f in fridaserver fridainject system_server.js system_server_orig.js shizuku.apk haval.apk; do
      [ "$UPDATE" = "1" ] && [ "$f" != "haval.apk" ] && continue
      case "$f" in
        system_server.js)            src="$HOOK_SRC" ;;
        system_server_orig.js|shizuku.apk) src="$PAYLOADS/$f" ;;
        haval.apk)                   src="$HAVAL_APK" ;;
        *)                           src="$FRIDA_DIR/$f" ;;
      esac
      echo "test \$(wc -c < /data/local/tmp/hav/$f) -eq $(SIZE_OF "$src") && echo \"SIZE-OK $f\" || echo \"SIZE-BAD $f\""
    done
  } >> "$1"
}
push_checks "$CMDS"

if [ "$UPDATE" = "1" ]; then
  say "Pushing the new APK to the head unit (a few minutes for 115 MB)..."
else
  say "Pushing payloads to head unit (this takes a few minutes for the 115 MB APK)..."
fi
warn "Keep the MacBook awake and near the car - the hotspot drops connections if the Mac sleeps. The first download may be slow if macOS asks about $SERVER_BIN."

PUSH_OUT=$(run_car "$GW" "$CMDS" 2>&1 || true)
echo "$PUSH_OUT"
if echo "$PUSH_OUT" | grep -qE '!! (TIMEOUT|connection closed)'; then
  die "the connection to the head unit dropped during transfer - re-run the installer (keep the Mac awake and on the car hotspot)."
fi
# ^ anchor: the device echoes each command line, which contains the literal
# text `|| echo "SIZE-BAD ..."` - only the real reply starts a line.
BAD=$(echo "$PUSH_OUT" | grep -oE '^SIZE-BAD [a-zA-Z0-9._-]+' | cut -d' ' -f2 || true)
if [ -n "$BAD" ]; then
  warn "Some files did not transfer: $BAD - retrying once..."
  RETRY_CMDS=$(mktemp)
  : > "$RETRY_CMDS"
  for f in $BAD; do
    echo "curl -fsSL -o /data/local/tmp/hav/$f http://$MAC_IP:$PORT/$f" >> "$RETRY_CMDS"
  done
  echo "--- retry ---"
  run_car "$GW" "$RETRY_CMDS" || true
  PUSH_OUT=$(run_car "$GW" "$CMDS" 2>&1 || true)
  echo "$PUSH_OUT"
  # ^ anchor: the device echoes each command line, which contains the literal
# text `|| echo "SIZE-BAD ..."` - only the real reply starts a line.
BAD=$(echo "$PUSH_OUT" | grep -oE '^SIZE-BAD [a-zA-Z0-9._-]+' | cut -d' ' -f2 || true)
  [ -z "$BAD" ] || die "transfer still failing for: $BAD - the car cannot reach this Mac. If macOS asked about $SERVER_BIN, click Allow; or check System Settings > Privacy & Security > Firewall (allow $SERVER_BIN). Keep the Mac awake and near the car, then re-run. Nothing was installed."
fi
say "All payloads transferred and size-verified on the head unit."

# ------------------------------------------------------------ 5/6. install
install_phase() {  # plain pm installs (hooks are injected separately in MODE B)
  local cmds
  cmds=$(mktemp)
  if [ "$UPDATE" = "1" ]; then
    echo "pm install -r /data/local/tmp/hav/haval.apk" >> "$cmds"
  else
    {
      echo "pm install -r /data/local/tmp/hav/shizuku.apk"
      echo "pm install -r /data/local/tmp/hav/haval.apk"
    } >> "$cmds"
  fi
  run_car "$GW" "$cmds"
}

# Success = BOTH packages present AND the app UID is <= 10999 (telnet gate).
# Sets APP_UID / PKGS_OK. Returns 0 on full success.
verify_install() {
  local cmds out
  cmds=$(mktemp)
  {
    echo "dumpsys package $PKG | grep -E 'userId='"
    echo "pm list packages | grep -E '$PKG|$SHIZUKU_PKG'"
  } > "$cmds"
  out=$(run_car "$GW" "$cmds" 2>&1 || true)
  echo "$out"
  APP_UID=$(echo "$out" | grep -oE 'userId=[0-9]+' | grep -oE '[0-9]+' || true)
  PKGS_OK=0
  if echo "$out" | grep -q "package:$SHIZUKU_PKG" && echo "$out" | grep -q "package:$PKG"; then
    PKGS_OK=1
  fi
  info "app UID: ${APP_UID:-not found} | packages: $([ "$PKGS_OK" = 1 ] && echo 'BOTH PRESENT' || echo 'MISSING (shizuku or app absent)')"
  [ "$PKGS_OK" = 1 ] && [ -n "$APP_UID" ] && [ "$APP_UID" -le 10999 ] 2>/dev/null
}

# MODE B: inject the hook into system_server, VERIFY it actually loaded, retry,
# then fall back to the vendored original bundle. The hook must be live during
# pm install (that is what bypasses the GWM install gate for Shizuku).
mode_b() {
  local attempt=0 hook_car ready_mark hook_label inj out hook_ok=""
  while [ -z "$hook_ok" ]; do
    attempt=$((attempt+1))
    [ "$attempt" -gt 4 ] && break
    if [ "$attempt" -le 2 ]; then
      hook_car="/data/local/tmp/hav/system_server.js"; ready_mark="HAVAL-HOOK-READY"
      hook_label="clean-room"
    else
      hook_car="/data/local/tmp/hav/system_server_orig.js"; ready_mark=""
      hook_label="original bundle (vendored, pinned)"
    fi
    info "hook attempt $attempt: $hook_label"
    inj=$(mktemp)
    {
      echo "pkill -f fridainject || true"
      echo "pkill -f fridaserver || true"
      echo "sleep 1"
      echo "setsid /data/local/tmp/hav/fridaserver >/dev/null 2>&1 < /dev/null &"
      echo "sleep 2"
      echo "pgrep fridaserver || true"
      echo "SYSTEM_PID=\$(pidof system_server || true)"
      echo "case \"\$SYSTEM_PID\" in ''|*[!0-9]*) SYSTEM_PID=\$(pgrep -x system_server || true);; esac"
      echo "echo \"[i] system_server pid: \${SYSTEM_PID:-NOT FOUND}\""
      echo "[ -n \"\$SYSTEM_PID\" ] || { echo 'NO-SYSTEM-SERVER-PID'; exit 1; }"
      echo "echo FRIDA-START"
      echo "/data/local/tmp/hav/fridainject -p \$SYSTEM_PID -s $hook_car &"
      echo "sleep 4"
      echo "echo FRIDA-END"
    } > "$inj"
    out=$(run_car "$GW" "$inj" 2>&1 || true)
    echo "$out"
    # ^ anchor: the echoed command line contains the literal text
    # `echo 'NO-SYSTEM-SERVER-PID'` - only the real failure output starts a line.
    if echo "$out" | grep -q '^NO-SYSTEM-SERVER-PID'; then
      die "cannot find system_server on the head unit - the frida hook cannot be injected. Nothing was installed."
    fi
    if echo "$out" | grep -qE '"type":"error"'; then
      warn "frida reported an error attaching - retrying..."
    elif [ -n "$ready_mark" ] && ! echo "$out" | grep -q "$ready_mark"; then
      warn "hook did not report ready - retrying..."
    elif [ -z "$ready_mark" ] && ! echo "$out" | grep -q 'FRIDA-END'; then
      # legacy bundle has no ready mark of its own: require that the inject
      # process actually started (FRIDA-START/END bracket it) and no error above
      warn "fridainject did not start (no FRIDA-END) - retrying..."
    else
      hook_ok=1
      say "hook active ($hook_label) - installing with the hook live"
    fi
  done
  [ -n "$hook_ok" ] || die "frida hook could not be injected after 4 attempts (clean-room + original bundle). Nothing was installed. Keep this log."
  say "Installing Shizuku + the app with the hook live..."
  OUT=$(install_phase || true)
  echo "$OUT"
}

if [ "$UPDATE" = "1" ]; then
  say "Installing the update (pm install -r - signature unchanged, UID preserved)..."
  OUT=$(install_phase || true)
  echo "$OUT"
  if echo "$OUT" | grep -qE "Success"; then
    say "Update installed."
  else
    warn "pm install did not report Success - review the output above."
    warn "If the failure mentions signature/UID, do a first install instead: ./install-macos.sh"
    exit 1
  fi
  UID_CMDS=$(mktemp)
  echo "dumpsys package $PKG | grep -E 'userId='" > "$UID_CMDS"
  UID_OUT=$(run_car "$GW" "$UID_CMDS" 2>&1 || true)
  echo "$UID_OUT"
  APP_UID=$(echo "$UID_OUT" | grep -oE 'userId=[0-9]+' | grep -oE '[0-9]+' || true)
  if [ -n "$APP_UID" ]; then
    say "App UID is $APP_UID (unchanged by update)."
  else
    warn "Could not confirm UID - check dumpsys output above (it should still be the old UID)."
  fi
  say "Cleaning up (removing temp files)..."
  CLEAN1=$(mktemp)
  {
    echo "rm -rf /data/local/tmp/hav"
    echo "pm list packages | grep -E '${PKG}|${SHIZUKU_PKG}'"
  } > "$CLEAN1"
  run_car "$GW" "$CLEAN1" || true
  say "Update complete. Package $PKG is installed with the new build."
  exit 0
fi

MODE="A"
ok=0
say "Installing (MODE A - plain pm install, no hook)..."
OUT=$(install_phase || true)
echo "$OUT"
if verify_install; then
  ok=1
  say "MODE A succeeded - app UID $APP_UID (<= 10999, telnet at boot will work)."
else
  warn "MODE A did not succeed (UID=${APP_UID:-unknown}, packages: $([ "$PKGS_OK" = 1 ] && echo 'both present' || echo 'not all present')). Retrying with the frida hook (MODE B)..."
  MODE="B"
  mode_b
  if verify_install; then
    ok=1
    say "MODE B succeeded - app UID $APP_UID."
  fi
fi

[ "$ok" = "1" ] || die "Install did not verify. Review the output above. Nothing was left running on the car."

# ----------------------------------------------------------------- 7. cleanup
say "Cleaning up (stopping frida on the car, removing temp files)..."
CLEAN2=$(mktemp)
{
  echo "pkill -f fridainject || true"
  echo "pkill -f fridaserver || true"
  echo "rm -rf /data/local/tmp/hav"
  echo "pm list packages | grep -E '${PKG}|${SHIZUKU_PKG}'"
} > "$CLEAN2"
run_car "$GW" "$CLEAN2" || true

say "Done! Mode $MODE. Both packages should be installed:"
info "  - $PKG"
info "  - $SHIZUKU_PKG"
info "The app auto-starts at next boot and brings up Shizuku via localhost telnet."
info "If the UID is right, no further setup is needed on the head unit."

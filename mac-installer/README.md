# mac-installer — self-contained Haval H6 install chain

Installs your fork (HavalShisuku) + Shizuku on the head unit **without any third-party
domain and without internet at install time**. Everything is served from your MacBook
over the car's hotspot LAN, and every payload is verified against a pinned manifest
before anything runs.

## What replaces what (vs the tontonhaval tool)

| tontonhaval tool                         | this chain                                             |
|------------------------------------------|--------------------------------------------------------|
| downloads `install.sh` live from GitHub main at run time | no remote script — commands are in `install-macos.sh` |
| frida binaries from `haval.joaoiot.com.br` (unverified)   | official Frida 17.2.15 android-arm64, SHA-256 pinned   |
| `system_server.js` from `haval.joaoiot.com.br`            | **clean-room rewrite**, repo-owned, SHA-256 pinned (original bundle kept as `--legacy`) |
| Shizuku "latest release" lookup (unpinned)                | Shizuku v13.6.0 (official release), SHA-256 pinned     |
| runs on Windows/Linux                                     | runs on macOS                                          |

The frida binaries in the app bundle were verified **byte-identical** to official
Frida 17.2.15 (`frida-server`/`frida-inject`, android-arm64) on 2026-08-25 — the
joaoiot domain was only mirroring stock builds.

## The hook script (`system_server.js`) — now ours

`system_server.js` is a **clean-room rewrite** of the hook logic that shipped inside
the original app bundle. It implements the same three hooks, readable and documented:

1. **UID clamp** — `Settings.newUserIdLPwForThirdApp` zeroes `mFirstAvailableUid`
   before the real allocation, so the next third-party install lands with a UID near 0
   (the app needs **UID ≤ 10999** to use its root telnet at boot).
2. **GWM install gate** — `PackageManagerService.APK_install_finish = false` keeps the
   firmware's install-restriction path from kicking in.
3. **FakeGApps (optional, toggle `FAKEGAPPS`)** — signature spoof so Play Services /
   Play Store queries report the AOSP test certificate. Irrelevant to our own app;
   only needed if you install GMS on the unit.

The original joaoiot bundle is vendored as `payloads/system_server_orig.js`
(SHA-256 pinned) and used only with `--legacy`. **No code from the original bundle is
used in the default path.** Plain frida script — no frida-compile build step.

## How it works

```
MacBook ──WiFi──> car hotspot (192.168.33.x)
   │                  │
   │ http.server on 0.0.0.0:8123        head unit telnet root (port 23)
   └─────────────< curl pulls files <──────────┘
        fridaserver, fridainject, system_server.js, shizuku.apk, haval.apk
```

Install modes:
- **First install (MODE A):** plain `pm install` of Shizuku + the fork, then VERIFIES
  both packages are present in `pm list packages` AND that the app UID is ≤ 10999
  (telnet gate). A fresh head unit often passes without any hook.
- **MODE B (fallback):** starts `fridaserver`, injects the hook script into
  `system_server` (UID clamp + GWM gate), then retries the installs. The installer
  only trusts the hook after it prints `[HAVAL-HOOK-READY]` — attach failures are
  retried up to 4× (clean-room script twice, then the vendored original bundle).
- After success it stops the frida processes and removes temp files — the car is left
  clean; the app runs its own bundled frida when needed.

Transfer is verified end-to-end:
- Before the car downloads anything, the installer checks the local server can serve
  every payload — through **loopback**, on purpose: the car hotspot's AP does not
  hairpin, so the Mac often cannot reach its own LAN IP (`curl (28)` timeout) even
  though the car CAN. A loopback failure means the server itself is broken.
- After the push, the car itself reports `wc -c` for every file; mismatches are
  re-transferred once, and the installer refuses to install anything if a file still
  fails. If the car cannot reach the Mac at all, the die message points at the macOS
  firewall (popup → Allow; or System Settings → Privacy & Security → Firewall).
  No more silent 404 → blind-install chains.

Windows dev-notes: a leftover `python3` "Microsoft Store" stub passes `command -v` but
never runs — the installer probes interpreters before picking one (and always invokes
the chosen binary, never a bare name), and a stale server left listening on port 8123
from an interrupted run is killed before the new one binds (via `lsof`, which macOS
ships).

## Usage

```bash
# 1. one-time prep (needs internet once; repairs the app-bundle payloads if needed)
bash fetch-payloads.sh

# 2. connect the MacBook to the car's WiFi hotspot (the car is the gateway, 192.168.33.x)

# 3. first install (fully offline)
bash install-macos.sh                       # uses ../app/build/outputs/apk/debug/app-debug.apk
bash install-macos.sh /path/to/haval.apk    # or any build you want
bash install-macos.sh --legacy              # (only if you ever need the original hook bundle)

# 4. after it finishes: restart the head unit (or wait for the app's BootReceiver)
```

### Fast iteration — update a build in ~2 minutes

The debug APK is always signed with the same keystore, so updating over the existing
install preserves both the signature and the app UID — **no frida, no Shizuku
reinstall, no UID dance**. The whole loop:

```bash
# on Windows (idea -> APK):
.\build.ps1                     # copies the latest widget into the bundle + builds the APK
# (get the APK to the Mac: scp / AirDrop / sync folder — it's 115 MB)

# on the Mac, parked at the car:
bash install-macos.sh --update  # pushes only the APK, pm install -r, done
```

Iterate: change the widget (`cluster-widgets/...`) or the hook script
(`mac-installer/system_server.js`), rebuild, update. If you edit the hook script,
re-pin it first: `shasum -a 256 system_server.js` → update `payloads/SHA256SUMS`.

Dry-run without the car (needs no hotspot, no internet):
- `bash install-macos.sh --selftest` — serves every payload and downloads it back,
  byte-verified.
- `bash install-macos.sh --mock` — full install against a simulated head unit that
  behaves like this one: echoes command lines, blocks Shizuku by name until the hook
  loads (forcing MODE B), fails the first hook attach and the first size check so the
  retry paths actually run.

## Files

- `system_server.js` — our clean-room hook script (default, SHA-256 pinned)
- `payloads/SHA256SUMS` — the pinned manifest (do not edit without re-verifying)
- `payloads/system_server_orig.js` — original joaoiot bundle, `--legacy` only
- `fetch-payloads.sh` — one-time setup: verify/repair payloads, vendor legacy script,
  download Shizuku
- `install-macos.sh` — the offline installer (`--update` for fast re-installs)
- `car.py` — raw-telnet driver used by the installer (commands file + echo markers)

## Security notes

- Nothing is fetched at install time — every byte comes from this Mac's local disk.
- Every payload is checked against `payloads/SHA256SUMS` before it runs; a hash
  mismatch aborts the install.
- The head-unit telnet (port 23) is a root shell on the car's LAN — anyone on the
  hotspot can reach it. The hotspot password is your exposure boundary; change it if
  you ever share it.
- The hook script hooks `PackageManagerService` in `system_server` to force low UIDs
  and bypass a GWM install gate — that is what makes the app's root telnet usable at
  boot. The current script is our clean-room rewrite; the vendored original is kept
  only for comparison/fallback.

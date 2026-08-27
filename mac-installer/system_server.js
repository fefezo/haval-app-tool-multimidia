/*
 * system_server.js - clean-room hook script for the Haval H6 install flow
 * =========================================================================
 *
 * What this is
 * ------------
 * A from-scratch, readable rewrite of the hook script that ships inside the
 * original joaoiot/haval-tool APK (there it was a minified frida-compile
 * bundle). The hooks below are behaviorally identical to the original; the
 * code is our own. No code from the original bundle is included.
 *
 * How it is loaded
 * ----------------
 *   fridainject -p $(pidof system_server) -s system_server.js
 *
 * Requires stock frida-inject for android-arm64 (the Java bridge is part of
 * the stock runtime - no build step, no frida-compile, nothing to fetch).
 * All three hooks live in Java.perform and only touch the Java layer, so a
 * plain script is sufficient.
 *
 * Why system_server?
 * ------------------
 * The head unit refuses third-party apps a root telnet session at boot when
 * their UID is above 10999. system_server assigns those UIDs, so hooking it
 * is the clean way to make installs land in the allowed range.
 *
 * The hooks
 * ---------
 * 1. UID clamp (REQUIRED for the whole flow)
 *    com.android.server.pm.Settings.newUserIdLPwForThirdApp
 *    Zeroes mFirstAvailableUid before the real allocation runs, so the next
 *    third-party app gets a UID near 0 - far below the 10999 gate. Only the
 *    app installed while the hook is live is affected; nothing is persisted.
 *
 * 2. GWM install gate (REQUIRED)
 *    com.android.server.pm.PackageManagerService.APK_install_finish
 *    A GWM-customized static field on PMS. The firmware's installer logic
 *    checks it after finishing an install; setting it false keeps GWM's
 *    install-restriction path from kicking in.
 *
 * 3. FakeGApps signature spoof (OPTIONAL - only needed if you install
 *    Google Play Services / Play Store on the unit; irrelevant to our app)
 *    com.android.server.pm.PackageManagerService.generatePackageInfo
 *    When something queries the signing info of com.google.android.gms or
 *    com.android.vending, reports the AOSP test certificate instead of the
 *    real signature, so signature-verifying clients accept the GMS build
 *    used on these units. Toggle with FAKEGAPPS below.
 *
 * Differences from the original bundle
 * ------------------------------------
 * - Readable, documented, no minification, no frida-gum C payload.
 * - Logs only actual signature spoofs (the original logged every
 *   generatePackageInfo call, which fires constantly in system_server).
 * - Behavior is otherwise identical.
 */

'use strict';

// ---------------------------------------------------------------------------
// Settings (toggle here; defaults match the original behavior)
// ---------------------------------------------------------------------------
var FAKEGAPPS = true;

// ---------------------------------------------------------------------------
// Hooks 1 + 2: UID clamp and GWM install gate.
// Kept outside the FakeGApps toggle: the install flow depends on these.
// ---------------------------------------------------------------------------
Java.perform(function () {
    // The installer (install-macos.sh) greps for this line to confirm the Java
    // bridge actually came up before trusting the hooks. If it never prints,
    // the attach failed somewhere in the frida runtime and the installer
    // retries with the vendored original bundle.
    console.log("[HAVAL-HOOK-READY] Java bridge up - hooks installing");

    // --- Hook 1: force the next third-party install into the low-UID range
    var Settings = Java.use("com.android.server.pm.Settings");
    Settings.newUserIdLPwForThirdApp.implementation = function (n) {
        this.mFirstAvailableUid.value = 0;   // next allocation starts at ~0
        return this.newUserIdLPw(n);         // original allocation, same args
    };

    // --- Hook 2: disable GWM's install-finished restriction flag
    var PMS = Java.use("com.android.server.pm.PackageManagerService");
    PMS.APK_install_finish.value = false;
});

// ---------------------------------------------------------------------------
// Hook 3 (optional): FakeGApps signature spoof.
// Only relevant when GMS / Play Store are installed on the head unit.
// ---------------------------------------------------------------------------
if (FAKEGAPPS) {
    Java.perform(function () {
        try {
            var PMS = Java.use("com.android.server.pm.PackageManagerService");
            var Base64 = Java.use("android.util.Base64");
            var Signature = Java.use("android.content.pm.Signature");
            var SigningDetails = Java.use("android.content.pm.PackageParser$SigningDetails");
            var Integer = Java.use("java.lang.Integer");
            var SigningInfo = Java.use("android.content.pm.SigningInfo");

            // AOSP test certificate (CN=Android, O=Google Inc., the key the
            // GMS builds on these units are signed with). Decoded at load.
            var CERT_B64 =
                "MIIEQzCCAyugAwIBAgIJAMLgh0ZkSjCNMA0GCSqGSIb3DQEBBAUAMHQxCzAJBgNVBAYTAlVTMRMw" +
                "EQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29n" +
                "bGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDAeFw0wODA4MjEyMzEz" +
                "MzRaFw0zNjAxMDcyMzEzMzRaMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYw" +
                "FAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5k" +
                "cm9pZDEQMA4GA1UEAxMHQW5kcm9pZDCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBAKtW" +
                "LgDYO6IIrgqWbxJOKdoR8qtW0I9Y4sypEwPpt1TTcvZApxsdyxMJZ2JORland2qSGT2y5b+3JKke" +
                "dxiLDmpHpDsz2WCbdxgxRczfey5YZnTJ4VZbH0xqWVW/8lGmPav5xVwnIiJS6HXk+BVKZF+JcWjA" +
                "sb/GEuq/eFdpuzSqeYTcfi6idkyugwfYwXFU1+5fZKUaRKYCwkkFQVfcAs1fXA5V+++FGfvjJ/Cx" +
                "URaSxaBvGdGDhfXE28LWuT9ozCl5xw4Yq5OGazvV24mZVSoOO0yZ31j7kYvtwYK6NeADwbSxDdJE" +
                "qO4k//0zOHKrUiGYXtqw/A0LFFtqoZKFjnkCAQOjgdkwgdYwHQYDVR0OBBYEFMd9jMIhF1Ylmn/T" +
                "gt9r45jk14alMIGmBgNVHSMEgZ4wgZuAFMd9jMIhF1Ylmn/Tgt9r45jk14aloXikdjB0MQswCQYD" +
                "VQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEUMBIG" +
                "A1UEChMLR29vZ2xlIEluYy4xEDAOBgNVBAsTB0FuZHJvaWQxEDAOBgNVBAMTB0FuZHJvaWSCCQDC" +
                "4IdGZEowjTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBAUAA4IBAQBt0lLO74UwLDYKqs6Tm8/y" +
                "zKkEu116FmH4rkaymUIE0P9KaMftGlMexFlaYjzmB2OxZyl6euNXEsQH8gjwyxCUKRJNexBiGcCE" +
                "yj6z+a1fuHHvkiaai+KL8W1EyNmgjmyy8AW7P+LLlkR+ho5zEHatRbM/YAnqGcFh5iZBqpknHf1S" +
                "KMXFh4dd239FJ1jWYfbMDMy3NS5CTMQ2XFI1MvcyUTdZPErjQfTbQe3aDQsQcafEQPD+nqActifK" +
                "Z0Np0IS9L9kR/wbNvyz6ENwPiTrjV2KRkEjH78ZMcUQXg0L3BYHJ3lc69Vs5Ddf9uUGGMYldX3Wf" +
                "MBEmh/9iFBDAaTCK";
            var fakeSignature = Signature.$new(Base64.decode(CERT_B64, Base64.DEFAULT.value));

            PMS.generatePackageInfo.implementation = function (a, b, c) {
                var result = this.generatePackageInfo(a, b, c); // call the original
                try {
                    if (result != null && result.packageName != null) {
                        var name = result.packageName.value;
                        if (name === "com.google.android.gms" || name === "com.android.vending") {
                            console.log("FakeGApps: spoofing signing info for " + name);
                            var sigs = Java.array("android.content.pm.Signature", [fakeSignature]);
                            result.signatures.value = sigs;
                            var flags = Integer.valueOf(3).intValue(); // same value the original used
                            var details = SigningDetails.$new(sigs, flags);
                            var info = SigningInfo.$new(details);
                            result.signingInfo.value = info;
                            console.log("FakeGApps: signing info set for " + name);
                        }
                    }
                } catch (err) {
                    console.log("FakeGApps: error generating signing info: " + err);
                }
                return result;
            };
        } catch (err) {
            console.log("Error in PackageManagerService hook: " + err);
        }
    });
}

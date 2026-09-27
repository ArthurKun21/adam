---
title:  "Companion service"
---

## Overview

The companion service is an on-device accessibility service (ported from
[google/artemis](https://github.com/google/artemis), Apache 2.0) that turns adam into a full
device-automation client **on real devices, without root**:

- real gesture injection (tap, double tap, long press, swipe) via
  `AccessibilityService.dispatchGesture`
- text entry via `ACTION_SET_TEXT` and clipboard injection - full Unicode support
- multi-window UI hierarchy dumps (app, dialogs, IME keyboard, split screen) in standard
  uiautomator XML format
- atomic screenshot + hierarchy snapshots on API 30+

The helper answers on loopback port 18888 with a small token-authenticated HTTP API. The host
reaches it through an adb port forward, so the device only ever talks to the computer that has
adb access.

The helper APK is built by the `:companion` module:

```bash
./gradlew :companion:assembleDebug
# companion/build/outputs/apk/debug/companion-debug.apk
```

## Deploy

```kotlin
val manager = CompanionManager(adb, supportedFeatures)

val deployment: CompanionDeployment = manager.deploy(
    serial = "emulator-5554",
    apk = File("companion/build/outputs/apk/debug/companion-debug.apk"),
)
```

`deploy` pushes and installs the APK (`pm install -r -g`), enables the accessibility service via
secure settings (no user interaction required), pushes a fresh 48-character hex session token
over an `am broadcast` that only the adb shell user may deliver, allocates a random local port
with `adb forward tcp:0 tcp:18888` and polls `/ping` until the helper reports a compatible
protocol version.

## Use

```kotlin
val client = deployment.client

val ping: CompanionPing = client.ping()

val xml: String = client.dumpXml()

val snapshot: CompanionSnapshot = client.snapshot()
snapshot.xml                      // uiautomator XML
snapshot.screenshotBase64         // JPEG, captured atomically with the hierarchy (API 30+)

client.tap(500, 1200)
client.doubleTap(500, 1200)
client.longPress(500, 1200, durationMs = 1000)
client.swipe(200, 2000, 200, 800, durationMs = 300)
client.type("héllo world")        // via ACTION_SET_TEXT on the focused input
client.clearText()
client.setClipboard("some text")
client.globalAction("back")       // back, home, recents, notifications, quick_settings,
                                  // power_dialog, toggle_split_screen, lock_screen,
                                  // take_screenshot
```

## Teardown

```kotlin
deployment.teardown()                                  // remove the port forward only
deployment.teardown(disableService = true)             // also disable the accessibility service
deployment.teardown(disableService = true, uninstall = true) // also remove the APK
```

Security notes: the loopback server is reachable by every app on the device, so every endpoint
except `/ping` requires the session token (constant-time comparison). The token receiver is
guarded by `WRITE_SECURE_SETTINGS` in the manifest, so only the adb shell user or the system can
deliver a token - a local app cannot mint its own and talk to the helper.

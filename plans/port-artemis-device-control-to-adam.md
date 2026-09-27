# Plan: Port Artemis device-control capabilities to adam

Branch: `docs/research-for-artemis-upgrade` · Date: 2026-09-27

## 1. Goal

Research https://github.com/google/artemis and port the parts of its device-control layer that
enhance the adam ADB client library. All new code lives under the `com.arthurkun21` namespace.
Scope (approved): Phase 1 (pure-ADB device-control requests in adam core) + Phase 2 (companion
accessibility service ported from artemis, helper APK built in-repo). No MCP server module.

## 2. Research findings

### 2.1 What artemis actually is

**artemis is not an ADB implementation.** It never speaks the ADB wire protocol. It is a Python
AI-agent framework (~10.4k stars, Apache-2.0, includes code from Minitap's `mobile-use`) that
converts natural-language instructions into Android device automation. Its device layer is:

- **adbutils** (Python ADB client library speaking the client↔adb-server protocol on 5037) for
  hot paths — `artemis/drivers/android/adb_driver.py`
- **adb binary** via subprocess for install/forward/settings/broadcast — `artemis/runtime/helper_manager.py`,
  `artemis/clients/ui_automator_client.py`, `artemis/clients/screen_client_factory.py`
- **On-device "Artemis Accessibility Helper"** — Java Android project in-repo
  (`packages/artemis-accessibility-helper/`, package `com.artemis.helper`, minSdk 24): an exported
  accessibility service (special-use foreground service) serving HTTP/1.1 + line-delimited JSON on
  loopback `127.0.0.1:18888`, reached via `adb forward --no-rebind tcp:0 tcp:18888` (random local
  port), token-authenticated (`X-Artemis-Token`, 48-hex token pushed via `am broadcast` gated on
  WRITE_SECURE_SETTINGS) — `artemis/clients/accessibility_client.py`
- **uiautomator2** Python library as fallback backend (`artemis/clients/ui_automator_client.py`)
- **scrcpy + ffmpeg** subprocesses for screen recording — `artemis/controllers/unified_controller.py`

Nothing ported at the wire-protocol level: adam's protocol surface (shell v1/v2, sync v1/v2,
multi-session install, track-devices, mDNS, pairing, framebuffer, emulator gRPC, abb) already
exceeds anything artemis touches.

### 2.2 adam gaps filled by artemis-proven patterns

Verified by grep over `adam/src/main/kotlin` — none of the following exist today:

| Capability | artemis reference | adam today |
|---|---|---|
| Input injection | `input tap/swipe/keyevent/text` + keycode map + text escaping (`adb_driver.py`) | none (emulator gRPC only) |
| Raw PNG screencap | `exec-out screencap -p` (`ui_automator_client.py`) | framebuffer service only (RGB frames) |
| UI hierarchy over pure ADB | u2 `dump_hierarchy`; adb-native equivalent is `uiautomator dump` | none |
| App control | `monkey -p … LAUNCHER`, `am force-stop`, `dumpsys window … mCurrentFocus`, `am start -a VIEW -d` | none |
| Device state probe | `adb get-state` (`screen_client_factory.py`) | none |
| Wait for boot | ping/state polling (`helper_manager.py`) | none |
| Settings I/O | `settings get/put secure …` incl. accessibility enable/rebind (`helper_manager.py`) | none |
| Broadcast | `am broadcast -n … -a … --es` (token push, ADBKeyboard b64 text) | none |
| Screen recording | `screenrecord` + SIGINT + sync.pull (`recorder.py`) | none |
| Gesture/hierarchy/screenshot on real devices w/o root | companion helper (`GestureController.java`, `HierarchyDumper.java`, `CommandServer.java`) | none |

Also verified: `LocalTcpPortSpec(port = 0)` already implements artemis' `tcp:0` random-port
forward trick (`request/forwarding/LocalPortSpec.kt:40`).

### 2.3 Not ported (out of library scope)

OCR/VLM agent stack, Flash/Pro agent loop, MCP server (user-declined), cloud WebSocket adb tunnel,
device-pool locking, uiautomator2 backend (Python; the helper is the primary backend anyway).

## 3. Implementation

### Phase 1 — ADB-level device-control requests (adam core)

New code under `adam/src/main/kotlin/com/arthurkun21/adam/request/`; shell-based requests follow
the existing `SyncShellCommandRequest` v2-first/v1-fallback pattern (mirror `PmListRequest` /
`UninstallRemotePackageRequest`). Unit tests against `server-stub` mirror existing request tests.

- `request/control/` — `InputTapRequest(x, y, durationMs?)`, `InputSwipeRequest`, 
  `InputKeyEventRequest` + `AndroidKeyCode`, `InputTextRequest` (escape shell specials,
  space→`%s`, newline→keyevent 66)
- `request/control/` — `ScreencapRequest` (raw PNG via `exec:` service, raw mode — no CRLF
  mangling), `UiAutomatorHierarchyRequest` (`uiautomator dump` + pull → XML string)
- `request/app/` — `LaunchAppRequest`, `ForceStopAppRequest`, `ForegroundAppRequest`,
  `OpenUrlRequest`, `ClearAppDataRequest`
- `request/misc/` — `GetStateRequest` (`host-serial:<serial>:get-state`),
  `WaitForDeviceRequest(state, timeout)`, `WaitForBootCompleteRequest` (`sys.boot_completed` poll)
- `request/settings/` — `SettingNamespace`, `PutSettingRequest`, `GetSettingRequest`,
  `EnableAccessibilityServiceRequest`, `DisableAccessibilityServiceRequest`, `AmBroadcastRequest`
- `request/screenrecord/` — `ScreenRecordRequest` (supervised `screenrecord --time-limit` + stop
  + pull; documented 3-min per-segment cap)

Integration tests under `adam/src/integrationTest/kotlin/com/arthurkun21/adam/` using
`rule/AdbDeviceRule` where a device is required.

### Phase 2 — Companion accessibility service

**New module `:companion`** (Android app, minSdk 24 / compileSdk 35, package
`com.arthurkun21.adam.companion`, APK built in-repo). Kotlin port of artemis'
`com.artemis.helper` (Apache-2.0; attribution added to NOTICE):

- `CompanionAccessibilityService` — exported accessibility + special-use foreground service
- `GestureController` — `dispatchGesture` + `GestureDescription.StrokeDescription` (tap 60ms,
  long-press 500–5000ms clamp, swipe 50–5000ms clamp), `ACTION_SET_TEXT` typing via input-node DFS
- `HierarchyDumper` — multi-window dump (windows by layer, API 33 `PREFETCH_DESCENDANTS_HYBRID`,
  guards MAX_DEPTH=75 / MAX_NODES=8000, bounds clipping) → uiautomator-format XML; atomic
  screenshot via `takeScreenshot` (API 30+, ~333ms rate-limit retry)
- `CommandServer` — loopback TCP `127.0.0.1:18888`, minimal HTTP/1.1 + JSON; endpoints `/ping`
  (unauthenticated, protocol_version), `/dump` (JSON), `/dump_xml`, `/snapshot`, `/action`
  (tap/double_tap/long_press/swipe/type/clear/clipboard/global); token header `X-Adam-Token`
- `TokenStore` + `TokenReceiver` — exported receiver gated on WRITE_SECURE_SETTINGS, action
  `com.arthurkun21.adam.companion.SET_TOKEN`

**Host side in adam core** (`com.arthurkun21.adam.companion`):

- `CompanionClient` — minimal HTTP/1.1 client over adam's `Socket` (no new dependency) +
  kotlinx-serialization models; `ping()`, `dumpHierarchy()`, `atomicSnapshot()`, input actions
- `CompanionManager` — deployment lifecycle reusing Phase-1 requests: installed-version check →
  install caller-supplied APK → `EnableAccessibilityServiceRequest` → token push via
  `AmBroadcastRequest` → `PortForwardRequest(LocalTcpPortSpec(0), tcp:18888)` → `/ping` poll;
  teardown (unforward, optionally disable/uninstall)

**Tests**: JVM unit tests for helper pure logic + `CompanionClient` against a fake loopback
CommandServer; `CompanionManager` against `server-stub` sessions; `CompanionE2ETest` on emulator.

## 4. Final steps

- Docs: new Zensical pages under `docs/docs/` (device-control, companion), nav in `zensical.toml`
- Update AGENTS.md (module list, key types, test commands)
- `./gradlew spotlessApply` · `./gradlew build` · `./gradlew test`; integration tests with device

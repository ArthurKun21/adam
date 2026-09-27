---
title:  "Device control"
---

## Input injection

Inject taps, swipes and key events over the shell service. All requests return a
`ShellCommandResult` so the exit code can be inspected.

```kotlin
adb.execute(InputTapRequest(500, 1200), serial)

// press-and-hold: expressed as a same-point swipe
adb.execute(InputTapRequest(500, 1200, durationMs = 1000), serial)

adb.execute(InputSwipeRequest(200, 2000, 200, 800, 300), serial)
adb.execute(InputSwipeRequest.longPress(500, 1200), serial)

adb.execute(InputKeyEventRequest(AndroidKeyCode.BACK), serial)
adb.execute(InputKeyEventRequest(AndroidKeyCode.ENTER, metastate = 1), serial)

// shell specials are escaped, spaces become %s, newlines become ENTER events
adb.execute(InputTextRequest("hello world"), serial)

// clear the focused text field
adb.execute(ClearTextFieldRequest(), serial)
```

`input text` does not support Unicode reliably. For full Unicode input deploy
[ADBKeyboard](https://github.com/senzhk/ADBKeyBoard) on the device and broadcast to it:

```kotlin
val text = Base64.getEncoder().encodeToString("héllo".toByteArray())
adb.execute(
    AmBroadcastRequest(action = "ADB_INPUT_B64", stringExtras = mapOf("msg" to text)),
    serial,
)
adb.execute(InputKeyEventRequest(AndroidKeyCode.PASTE), serial)
```

## Screenshot as PNG

`ScreencapRequest` returns a PNG image via the raw `exec` service (this is the same wire-level
service `adb exec-out` uses, so the binary output is not mangled):

```kotlin
val png: ByteArray = adb.execute(ScreencapRequest(), serial)
File("/tmp/screen.png").writeBytes(png)
```

For raw framebuffer frames use the `ScreenCaptureRequest` from the screen-capture guide.

## UI hierarchy (uiautomator)

Dumps the active window hierarchy with the on-device `uiautomator` command and returns
uiautomator-format XML:

```kotlin
val xml: String = adb.execute(UiAutomatorHierarchyRequest(), serial)
```

## App control

```kotlin
adb.execute(LaunchAppRequest("com.example"), serial)      // via monkey launcher intent
adb.execute(ForceStopAppRequest("com.example"), serial)   // am force-stop
adb.execute(ClearAppDataRequest("com.example"), serial)   // pm clear
adb.execute(OpenUrlRequest("https://example.com"), serial)

val foreground: ForegroundApp? = adb.execute(ForegroundAppRequest(), serial)
println(foreground?.packageName)
```

## Device state

```kotlin
val state: DeviceState = adb.execute(GetStateRequest(serial))

// wait until a device is connected and fully booted
adb.execute(WaitForDeviceRequest(serial, DeviceState.DEVICE, timeoutMs = 60_000), serial = null)
adb.execute(WaitForBootCompleteRequest(timeoutMs = 60_000), serial)
```

`GetStateRequest` carries the serial in the request itself: execute it with `serial = null`.

## Settings and broadcasts

```kotlin
adb.execute(PutSettingRequest(SettingNamespace.SECURE, "my_key", "value"), serial)
val value: String = adb.execute(GetSettingRequest(SettingNamespace.SECURE, "my_key"), serial)

adb.execute(
    AmBroadcastRequest(
        action = "com.example.ACTION",
        component = "com.example/.Receiver",
        stringExtras = mapOf("extra" to "value"),
    ),
    serial,
)
```

Accessibility services can be enabled and disabled without user interaction - this is how the
[companion](../companion/companion.md) service is deployed:

```kotlin
adb.execute(EnableAccessibilityServiceRequest("com.pkg/.MyAccessibilityService"), serial)
adb.execute(DisableAccessibilityServiceRequest("com.pkg/.MyAccessibilityService"), serial)
```

## Screen recording

Records the screen with the on-device `screenrecord` command and pulls the mp4. A single
recording is capped at 180 seconds by the device. Cancelling the coroutine that executes the
request stops the recording early and still pulls the finalized file:

```kotlin
val file: File = adb.execute(
    ScreenRecordRequest(
        destination = File("recording.mp4"),
        timeLimitSeconds = 180,
        supportedFeatures = features,
    ),
    serial,
)
```

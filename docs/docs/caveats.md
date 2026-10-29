---
title: "Caveats"
---

## Response types

You execute every adam request through an instance of `AndroidDebugBridgeClient`. The requests produce either a **single response** (e.g. `ListDevicesRequest`):

```kotlin
val devices: List<Device> = adb.execute(request = ListDevicesRequest())
```

or a **stream of responses** (e.g. the progress of a file pull):

```kotlin
val testFile = File.createTempFile("testfile", ".tmp")
val channel = adb.execute(
    request = PullFileRequest("/data/local/tmp/testfile", testFile),
    scope = GlobalScope,
    serial = "emulator-5554"
)

for (progressDouble in channel) {
    println(progressDouble)
}
println("done!")
```

## Exception handling

You can expect the following for any request:

1. `ClosedWriteChannelException` when the device connection is not available anymore
2. `RequestRejectedException` when the ADB server does not respond properly
3. `RequestValidationException` when the request's `#validate()` returns false before execution

There are additional exceptions:

* `PullFailedException`, `PushFailedException` and `UnsupportedSyncProtocolException` for file requests
* `UnsupportedForwardingSpecException` for port forwarding requests
* `UnsupportedImageProtocolException` for screenshot requests

## Request target

When the client executes a request, it sends the target for that particular request to the ADB server.

Possible targets:

1. `HostTarget`. The host target can also mean 'any single device or emulator connected to or running on the host'. This applies when you ask for information related to a device.
2. `SerialTarget`. A query with the `host-serial:<serial-number>:` prefix. The client asks the ADB server for information related to a specific device.
3. `UsbTarget`. A variant of host-serial that targets the single USB device connected to the host. It fails when there is none or more than one.
4. `LocalTarget`. A variant of host-serial that targets the single emulator instance running on the host. It fails when there is none or more than one.
5. `NonSpecifiedTarget`

In most cases you can specify any of the targets. There are sensible defaults. For example, the default target of `KillAdbRequest` is `HostTarget`, because this request does not make sense for an Android device itself.

For the requests that target a particular device, e.g. `ScreenCaptureRequest`, specify the `serial` parameter during execution:

```kotlin
adb.execute(
    request = ScreenCaptureRequest(),
    serial = "emulator-5554"
)
```

Retrieve the serial of a device with `ListDevicesRequest` or `AsyncDeviceMonitorRequest`.

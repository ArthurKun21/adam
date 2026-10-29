---
title: "Device management"
---

## List devices

This request captures a snapshot of the device states at the moment of execution:

```kotlin
val devices: List<Device> = adb.execute(request = ListDevicesRequest())
```

## Monitor device changes

If listing the devices once is not enough and you want to monitor state changes continuously (connect, disconnect, etc), use the following request:

```kotlin
val deviceEventsChannel: ReceiveChannel<List<Device>> = adb.execute(
    request = AsyncDeviceMonitorRequest(),
    scope = GlobalScope
)

for (currentDeviceList in deviceEventsChannel) {
    //...
}
```

The channel reports **all devices**, even the ones that did not change.

## Fetch device features

This request fetches the list of features that a particular device supports:

```kotlin
val features: List<Feature> = adb.execute(request = FetchDeviceFeaturesRequest("emulator-5554"))
```

adam knows the following features:

* `Feature.SHELL_V2`: support for separate stdout, stderr and exit code
* `Feature.CMD`: the `cmd` command is available, Android 24+
* `Feature.STAT_V2`: the device supports the extended `FileEntryV2` format for the stat operation
* `Feature.LS_V2`: the device supports the extended `FileEntryV2` format for the list operation
* `Feature.APEX`: adbd supports installing .apex packages
* `Feature.ABB`: adbd supports the Android Binder Bridge (abb) in interactive mode using the shell protocol
* `Feature.ABB_EXEC`: adbd supports abb using a raw pipe
* `Feature.SENDRECV_V2`: adbd supports version 2 of send/recv

The ADB server reports more features. adam does not use them at the moment.

When you see a badge like
Requires Feature.ABB
{: .label .label-yellow }
in this documentation, the request fails unless the device supports the feature. Check the support beforehand with `FetchDeviceFeaturesRequest`, or catch the `RequestValidationException`.

Some requests use a feature only when the device provides a fallback. See the documentation of the particular request.

## Connect device

The following request connects a remote Android device to a local ADB server:

```kotlin
val output = adb.execute(ConnectDeviceRequest("10.0.0.2", 5555))
```

## Disconnect device

The following request disconnects a previously connected Android device:

```kotlin
val output = adb.execute(DisconnectDeviceRequest("10.0.0.2", 5555))
```

## Reconnect device

The behavior of this request depends on the reconnection target.

Without a `reconnectTarget`, adam reconnects the first available device:

```kotlin
val output = adb.execute(ReconnectRequest())
```

With the `Device` target, provide the target either in the request or through the `serial` parameter of `execute`:

```kotlin
val output = adb.execute(ReconnectRequest(reconnectTarget = Device, target = SerialTarget("10.0.0.2:5555")))
```

With the `Offline` target, use the host target:

```kotlin
val output = adb.execute(ReconnectRequest(reconnectTarget = Offline, target = HostTarget))
```

## Pair device

The following request pairs the ADB server with a device over WiFi. See the [official documentation](https://developer.android.com/studio/command-line/adb#connect-to-a-device-over-wi-fi-android-11+) for the workflow.

```kotlin
val output = adb.execute(PairDeviceRequest("10.0.0.2:39567", "123456"))
```

The target device has the form `host[:port]`. The port is optional.

## Reboot device

Reboot a device, for example when it stopped executing requests properly:

```kotlin
adb.execute(request = RebootRequest(), serial = "emulator-5554")
```

Reboot to recovery:

```kotlin
adb.execute(request = RebootRequest(mode = RECOVERY), serial = "emulator-5554")
```

Reboot to the bootloader:

```kotlin
adb.execute(request = RebootRequest(mode = BOOTLOADER), serial = "emulator-5554")
```

---
title: "3. Install package"
---

## 3. Install package

Start with the boilerplate from [part 1](1-shell.md) to set up the communication with the device:

```kotlin
StartAdbInteractor().execute()
val adb = AndroidDebugBridgeClientFactory().build()
val devices: List<Device> = adb.execute(ListDevicesRequest())
val device = devices.firstOrNull { it.state == DeviceState.DEVICE } ?: throw RuntimeException("no devices available")
```

The `InstallRemotePackageRequest` installs a package from a file that already exists on the device. First transfer your package to the device:

```kotlin
val apkFile = File("/my/precious/application/app-debug.apk")
val fileName = apkFile.name
val channel = adb.execute(
    PushFileRequest(apkFile, "/data/local/tmp/$fileName"),
    GlobalScope,
    serial = device.serial
)

for (progress in channel) {
    println(progress)
}
```

`PushFileRequest` returns a channel of progress values. Iterate over the channel until the transfer finishes and the channel closes.

Now install the file:

```kotlin
val output: ShellCommandResult = adb.execute(
    InstallRemotePackageRequest("/data/local/tmp/$fileName", true),
    serial = device.serial
)
if (!output.output.startsWith("Success")) throw RuntimeException("Unable to install the apk")
```

If the installation succeeds, the output should start with `Success`.

Next verify that the package is installed:

```kotlin
val packages: List<Package> = adb.execute(PmListRequest(), serial = device.serial)
val pkg: Package? = packages.find { it.name == "com.example" }
```

See [Package management](../docs/package-management/package-management.md) for the other installation modes.

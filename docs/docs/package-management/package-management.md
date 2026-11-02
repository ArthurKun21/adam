---
title: "Package management"
---

## Install a package

### Default installation mode

First push the apk to the device with `PushFileRequest`. Push your apks to `/data/local/tmp`. That path is user-writable on all versions of Android (_so far_).

Then install the package from the remote file:

```kotlin
val output: ShellCommandResult = adb.execute(
    request = InstallRemotePackageRequest(
        absoluteRemoteFilePath = "/data/local/tmp/$apkFileName",
        reinstall = true,
        extraArgs = emptyList()
    ),
    serial = "emulator-5554"
)
```

### Streaming installation

Requires Feature.CMD or Feature.ABB_EXEC
{: .label .label-yellow }

Optionally uses Feature.APEX
{: .label .label-blue }

This mode streams the package file. You do not need to push the file to the device beforehand. You also save two requests: push file at the start and delete file at the end.

```kotlin
val result: StreamingPackageInstallResult = adb.execute(
    StreamingPackageInstallRequest(
        pkg = apkFile,
        supportedFeatures,
        reinstall = false,
        extraArgs = emptyList()
    ),
    serial = "emulator-5554"
)
println(result.success)
```

### Split apk

Optionally uses Feature.CMD or Feature.ABB_EXEC
{: .label .label-blue }

Install an apk split as follows:

```kotlin
val output: String = adb.execute(
    InstallSplitPackageRequest(
        pkg = ApkSplitInstallationPackage(appFile1, appFile2),
        supportedFeatures,
        reinstall = false,
        extraArgs = emptyList()
    ),
    serial = "emulator-5554"
)
```

If both `CMD` and `ABB_EXEC` features are missing, adam falls back to `exec:`. In this case, the `pm` binary might not support the installation of split packages at all.

### Atomic multi-package install

Requires Feature.CMD or Feature.ABB_EXEC
{: .label .label-yellow }

Optionally uses Feature.APEX
{: .label .label-blue }

This request installs multiple packages as a single atomic operation. If one package fails, all packages fail.

```kotlin
val output: String = adb.execute(
    AtomicInstallPackageRequest(
        pkgList = listOf(
            SingleFileInstallationPackage(appFile),
            ApkSplitInstallationPackage(appFile1, appFile2)
        ),
        supportedFeatures,
        reinstall = false,
        extraArgs = emptyList()
    ),
    serial = "emulator-5554"
)
```

## List installed packages

The following request lists all installed packages:

```kotlin
val packages: List<Package> = adb.execute(
    request = PmListRequest(
        includePath = false
    ),
    serial = "emulator-5554"
)
```

## Uninstall package

```kotlin
val output: ShellCommandResult = adb.execute(
    request = UninstallRemotePackageRequest(
        packageName = "com.example",
        keepData = false
    ),
    serial = "emulator-5554"
)
```

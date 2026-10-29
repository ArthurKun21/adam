---
title: Miscellaneous
---

## Get ADB server version

This request returns the version of the ADB server as defined in `adb/adb.h` ([source](https://android.googlesource.com/platform/system/core/+/dd7bc3319deb2b77c5d07a51b7d6cd7e11b5beb0/adb/adb.h#36)). Use it to debug incompatible adb versions and to check that the ADB server supports your requests.

```kotlin
val version: Int = adb.execute(request = GetAdbServerVersionRequest())
```

## Kill ADB server

This request is equivalent to executing `adb kill-server`:

```kotlin
adb.execute(request = KillAdbRequest())
```

## Remount partitions

This request remounts the partitions read-write. If the device requires a reboot, pass `autoReboot = true` and adam reboots it automatically.

```kotlin
val output: String = adb.execute(request = RemountPartitionsRequest(autoReboot = false), "emulator-5554")
```

## Enable/disable dm-verity checking on userdebug builds

```kotlin
val output: String = adb.execute(request = SetDmVerityCheckingRequest(false), "emulator-5554")
```

## Fetch host features

```kotlin
val features: List<Feature> = adb.execute(request = FetchHostFeaturesRequest())
```

## Check mDNS discovery availability

mDNS supports automatic discovery and connection of remote devices (for example, ADB over WiFi on Android 11):

```kotlin
val status: MdnsStatus = adb.execute(MdnsCheckRequest())
```

## List all mDNS discovered services

```kotlin
val services: List<MdnsService> = adb.execute(ListMdnsServicesRequest())
```

```kotlin
data class MdnsService(
    val name: String,
    val serviceType: String,
    val url: String
)
```

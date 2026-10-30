---
title: "adbd (on-device)"
---

## Root permissions

Restart adbd with root permissions:

```kotlin
val stdout = adb.execute(
    request = RestartAdbdRequest(RootAdbdMode),
    serial = "emulator-5554"
)

println(stdout)
```

Restart adbd without root permissions:

```kotlin
val stdout = adb.execute(
    request = RestartAdbdRequest(UnrootAdbdMode),
    serial = "emulator-5554"
)

println(stdout)
```

## Switch transport

Switch the device to listen on a TCP port instead of USB:

```kotlin
val stdout = adb.execute(
    request = RestartAdbdRequest(TcpIpAdbdMode(8080)),
    serial = "emulator-5554"
)

println(stdout)
```

Switch back to USB:

```kotlin
val stdout = adb.execute(
    request = RestartAdbdRequest(UsbAdbdMode),
    serial = "emulator-5554"
)

println(stdout)
```

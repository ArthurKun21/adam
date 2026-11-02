---
title: "1. Execute shell request"
---

## 1. Execute shell request

All request execution in adam happens in a coroutine scope. The examples below omit the scope. When you try out the examples, wrap them in `runBlocking { }`. When you write production code, you should understand coroutines and their scoping.

First, make sure that the ADB server is running:

```kotlin
StartAdbInteractor().execute()
```

Next, create an instance of `AndroidDebugBridgeClient` with the factory:

```kotlin
val adb = AndroidDebugBridgeClientFactory().build()
```

The `adb` client has an `execute()` method for requests. You do not yet know which devices are connected to the ADB server. List the devices and pick one:

```kotlin
val devices: List<Device> = adb.execute(ListDevicesRequest())
val device = devices.firstOrNull { it.state == DeviceState.DEVICE } ?: throw RuntimeException("no devices available")
```

Now execute a request for this device:

```kotlin
val response: ShellCommandResult = adb.execute(ShellCommandRequest("echo hello"), device.serial)
```

adam establishes the transport connection and waits for the response in the background. You do not need to wait for anything. This process does not allocate new threads.

---
title: "2. Stream logcat"
---

## 2. Stream logcat

Some requests in adam stream their output. Logcat is one of them. The device produces logcat output continuously. The stream ends only when you stop reading or the device stops producing output.

Start with the boilerplate from [part 1](1-shell.md) to set up the communication with the device:

```kotlin
StartAdbInteractor().execute()
val adb = AndroidDebugBridgeClientFactory().build()
val devices: List<Device> = adb.execute(ListDevicesRequest())
val device = devices.firstOrNull { it.state == DeviceState.DEVICE } ?: throw RuntimeException("no devices available")
```

Now execute the request:

```kotlin
launch {
    val channel: ReceiveChannel<String> = adb.execute(
        request = ChanneledLogcatRequest(),
        scope = this,
        serial = device.serial
    )

    do {
        val line: String? = channel.receiveCatching().getOrNull()

        if (line != null) {
            println(line)
        }

        if (stopStreaming) { //your stop condition, e.g. a user action
            channel.cancel()
            break
        }
    } while (line != null)
}
```

Note the return type `ReceiveChannel<String>`. The channel can deliver more `String` instances over time. One iteration of the loop handles one value:

1. `receiveCatching()` waits for the next chunk. When a chunk arrives, the code prints it.
2. When the channel closes without a chunk, `getOrNull()` returns `null`. The channel closes when the device stops producing output.
3. `stopStreaming` is your external stop condition, for example a user action. Cancel the channel to release the transport resources. Then exit the loop.

The loop repeats until the device stops producing output or you stop it.

There are more options for [`ChanneledLogcatRequest`](../docs/logcat/logcat.md). They change the format of the output, the filtering, and more.

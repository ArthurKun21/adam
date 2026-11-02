---
title: "Logcat"
---

## Retrieve logcat log

The following request reads the logcat once:

```kotlin
val log = adb.execute(
    request = SyncLogcatRequest(
        since = LogcatSinceFormat.DateStringYear(
            Clock.System.now() - 60.seconds,
            TimeZone.currentSystemDefault().id,
        ),
        filters = listOf(LogcatFilterSpec("TAG", LogcatVerbosityLevel.E))
    ),
    serial = "emulator-5554"
)
```

`SyncLogcatRequest` maps most of the options of the underlying `logcat` command:

```kotlin
class SyncLogcatRequest(
    since: LogcatSinceFormat? = null,
    modes: List<LogcatReadMode> = listOf(LogcatReadMode.long),
    buffers: List<LogcatBuffer> = listOf(LogcatBuffer.default),
    pid: Long? = null,
    lastReboot: Boolean? = null,
    filters: List<LogcatFilterSpec> = emptyList()
)
```

## Stream logcat output

The following request streams the logcat output, for example when you write it to a file:

```kotlin
launch {
    val channel = adb.execute(
        request = ChanneledLogcatRequest(),
        scope = this,
        serial = "emulator-5554"
    )

    val logcatChunk = channel.receive()
    //logcatChunk == "I/ActivityManager(  585): Starting activity: Intent { action=android.intent.action...}\nI/MyActivity( 1557): MyClass"
    //write to a file or append to a buffer

    //Cancel the channel to release the resources
    channel.cancel()
}
```

Logcat chunks might not end with `\n`. If you need to parse the logcat output line-by-line, accumulate the chunks in a buffer first.

`ChanneledLogcatRequest` maps most of the options of the underlying `logcat` command:

```kotlin
class ChanneledLogcatRequest(
    since: LogcatSinceFormat? = null,
    modes: List<LogcatReadMode> = listOf(LogcatReadMode.long),
    buffers: List<LogcatBuffer> = emptyList(),
    pid: Long? = null,
    lastReboot: Boolean? = null,
    filters: List<LogcatFilterSpec> = emptyList()
)
```

See the [official docs](https://developer.android.com/studio/command-line/logcat) for the meaning of these options.

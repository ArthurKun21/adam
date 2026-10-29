---
title: Shell
---

This section describes the requests in `com.malinskiy.adam.request.shell.v1`.

## Execute shell command

You can execute arbitrary commands (`ls`, `date`, etc) on the device with `ShellCommandRequest`:

```kotlin
val response: ShellCommandResult = adb.execute(
    request = ShellCommandRequest("echo hello"),
    serial = "emulator-5554"
)
```

The response contains stdout mixed with stderr. This protocol does not support separate streams. Shell v1 also does not return an exit code natively. To get the exit code, adam appends `;echo $?` to every shell v1 command and parses the result.

```kotlin
data class ShellCommandResult(
    val stdout: ByteArray,
    val exitCode: Int
)
```

If the output is UTF-8 encoded, use the lazy property `output` to convert the bytes into a `String`, e.g. `result.output`.

Use this request when the command finishes on its own and you want the complete result. If you need the output while the command runs, use the `ChanneledShellCommandRequest`.

## Streaming shell request

You can execute arbitrary commands (`cat`, `tail -f`, etc) on the device with `ChanneledShellCommandRequest`:

```kotlin
launch {
    val updates = adb.execute(
        request = ChanneledShellCommandRequest("logcat -v"),
        scope = this,
        serial = "emulator-5554"
    )

    for (lines in updates) {
        println(lines)
    }
}
```

## Execute shell command with pipe input

This request executes the command and provides the channel as input to the command. It does not return a result.

```kotlin
val blockSizeChannel = Channel<Int>(capacity = 1)
//Provide a channel that emits the input data in blocks.
//The test code in adam has an example that reads from a file.
val inputChannel: ReceiveChannel<ByteArray> = readBlocksAsByteArray(blockSizeChannel)

adb.execute(
    request = ExecInRequest(
        cmd = "cmd package install -S ${apkFile.length()}",
        channel = inputChannel,
        sizeChannel = blockSizeChannel
    ),
    serial = "emulator-5554"
)
```

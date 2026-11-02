---
title: "Shell v2"
---

This section describes the requests in `com.malinskiy.adam.request.shell.v2`.

## Execute shell command

Requires Feature.SHELL_V2
{: .label .label-yellow }

You can execute arbitrary commands (`ls`, `date`, etc) on the device with `ShellCommandRequest`:

```kotlin
val response: ShellCommandResult = adb.execute(
    request = ShellCommandRequest("echo hello"),
    serial = "emulator-5554"
)
```

The response contains separate stdout and stderr as well as an exit code.

```kotlin
data class ShellCommandResult(
    val stdout: ByteArray,
    val stderr: ByteArray,
    val exitCode: Int
)
```

If the output is UTF-8 encoded, use the lazy properties `output` and `errorOutput` to convert the bytes into a `String`, e.g. `result.output`.

Use this request when the command finishes on its own and you want the complete result. If you need the output while the command runs, use the `ChanneledShellCommandRequest`.

## Streaming shell request

Requires Feature.SHELL_V2
{: .label .label-yellow }

You can execute arbitrary commands (`cat`, `tail -f`, etc) on the device with `ChanneledShellCommandRequest`. Shell v2 supports stdin through a separate channel.

```kotlin
launch {
    val stdio = Channel<ShellCommandInputChunk>()
    val receiveChannel = adb.execute(ChanneledShellCommandRequest("cat", stdio), this, "emulator-5554")
    //Sending input requires an additional coroutine, otherwise the execution might deadlock
    val stdioJob = launch(Dispatchers.IO) {
        stdio.send(
            ShellCommandInputChunk(
                stdin = "cafebabe".toByteArray(Charsets.UTF_8)
            )
        )

        stdio.send(
            ShellCommandInputChunk(
                close = true
            )
        )
    }

    val stdoutBuilder = StringBuilder()
    val stderrBuilder = StringBuilder()
    var exitCode = 1
    for (i in receiveChannel) {
        i.stdout?.let { stdoutBuilder.append(String(it, Charsets.UTF_8)) }
        i.stderr?.let { stderrBuilder.append(String(it, Charsets.UTF_8)) }
        i.exitCode?.let { exitCode = it }
    }
    stdioJob.join()

    println(stdoutBuilder.toString())
}
```

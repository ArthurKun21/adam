---
title: "sync v1"
---

## Stat file

The following request returns the file stats:

```kotlin
val stats = adb.execute(StatFileRequest("/data/local/tmp/app-debug.apk"), "emulator-5554")
```

The stats model is `FileEntryV1`:

```kotlin
data class FileEntryV1(
    val name: String? = null,
    val mode: UInt,
    val size: UInt,
    val mtime: Instant
) {
    fun exists(): Boolean
    fun isDirectory(): Boolean
    fun isRegularFile(): Boolean
    fun isBlockDevice(): Boolean
    fun isCharDevice(): Boolean
    fun isLink(): Boolean
}
```

`name` is optional. List requests fill it. Stat requests do not.

## List files

The following request returns the list of files for a particular path:

```kotlin
val list: List<FileEntryV1> = adb.execute(ListFileRequest("/sdcard/"), "emulator-5554")
```

## Pull file

The following request pulls a file with a known path on the device (not a folder):

```kotlin
launch {
    val channel = adb.execute(
        PullFileRequest("/data/local/tmp/testfile", File.createTempFile("testfile", ".tmp")),
        scope = this,
        "emulator-5554"
    )

    for (percentageDouble in channel) {
        println((percentageDouble * 100).roundToInt())
    }
}
```

## Push file

The following request pushes a local file to the device (`remotePath` is the full path with the name of the target file):

```kotlin
launch {
    val file = File("some-file")
    val channel = adb.execute(
        PushFileRequest(
            local = file,
            remotePath = "/data/local/tmp/some-file",
            mode = "0644"
        ),
        scope = this,
        serial = "emulator-5554"
    )

    for (percentageDouble in channel) {
        println((percentageDouble * 100).roundToInt())
    }
}
```

`mode` contains the access rights in octal as a `String`.

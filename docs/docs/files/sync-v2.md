---
title: "sync v2"
---

## Stat file

Requires Feature.STAT_V2
{: .label .label-yellow }

The following request returns the file stats:

```kotlin
val stats = adb.execute(StatFileRequest("/data/local/tmp/app-debug.apk", supportedFeaturesList), "emulator-5554")
```

The stats model is `FileEntryV2`:

```kotlin
data class FileEntryV2(
    val error: UInt,
    val dev: ULong,
    val ino: ULong,
    val mode: UInt,
    val nlink: UInt,
    val uid: UInt,
    val gid: UInt,
    val size: ULong,
    val atime: Instant,
    val mtime: Instant,
    val ctime: Instant,
    val name: String? = null
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

Requires Feature.LS_V2
{: .label .label-yellow }

The following request returns the list of files for a particular path:

```kotlin
val list: List<FileEntryV2> = adb.execute(ListFileRequest("/sdcard/", supportedFeaturesList), "emulator-5554")
```

## Pull file

Requires Feature.SENDRECV_V2
{: .label .label-yellow }

The following request pulls a file with a known path on the device (not a folder):

```kotlin
launch {
    val channel = adb.execute(
        PullFileRequest("/data/local/tmp/testfile", File.createTempFile("testfile", ".tmp"), supportedFeaturesList),
        scope = this,
        "emulator-5554"
    )

    for (percentageDouble in channel) {
        println((percentageDouble * 100).roundToInt())
    }
}
```

## Push file

Requires Feature.SENDRECV_V2
{: .label .label-yellow }

The following request pushes a local file to the device (`remotePath` is the full path with the name of the target file):

```kotlin
launch {
    val file = File("some-file")
    val channel = adb.execute(
        PushFileRequest(
            local = file,
            remotePath = "/data/local/tmp/some-file",
            supportedFeaturesList,
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

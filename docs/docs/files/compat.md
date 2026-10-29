---
title: "Compatibility mode"
---

The compatibility requests select the sync v1 or sync v2 protocol automatically. They check the features that the device supports for each operation.

## Stat file

Optionally uses Feature.STAT_V2
{: .label .label-blue }

The following request returns the file stats:

```kotlin
val stats = adb.execute(CompatStatFileRequest("/data/local/tmp/app-debug.apk", supportedFeaturesList), "emulator-5554")
```

The stats model is `FileEntry`. adam returns an instance of `FileEntryV1` or `FileEntryV2`, depending on the available features:

```kotlin
sealed class FileEntry {
    abstract val mode: UInt
    abstract val name: String?
    abstract val mtime: Instant

    fun isDirectory()
    fun isRegularFile()
    fun isBlockDevice()
    fun isCharDevice()
    fun isLink()

    fun size()

    abstract fun exists(): Boolean
}
```

`name` is optional. List requests fill it. Stat requests do not.

## List files

Optionally uses Feature.LS_V2
{: .label .label-blue }

The following request returns the list of files for a particular path:

```kotlin
val list: List<FileEntry> = adb.execute(CompatListFileRequest("/sdcard/", supportedFeaturesList), "emulator-5554")
```

## Pull file

Optionally uses Feature.SENDRECV_V2
{: .label .label-blue }

The following request pulls a file with a known path on the device (not a folder):

```kotlin
launch {
    val channel = adb.execute(
        CompatPullFileRequest(
            "/data/local/tmp/testfile",
            File.createTempFile("testfile", ".tmp"),
            supportedFeaturesList,
            coroutineScope = this
        ),
        serial = "emulator-5554"
    )

    for (percentageDouble in channel) {
        println((percentageDouble * 100).roundToInt())
    }
}
```

The channel reports the progress of the transfer.

## Push file

Optionally uses Feature.SENDRECV_V2
{: .label .label-blue }

The following request pushes a local file to the device (`destination` is the full path with the name of the target file):

```kotlin
launch {
    val file = File("some-file")
    val channel = adb.execute(
        CompatPushFileRequest(
            source = file,
            destination = "/data/local/tmp/some-file",
            supportedFeatures = supportedFeaturesList,
            coroutineScope = this,
            mode = "0644"
        ),
        serial = "emulator-5554"
    )

    for (percentageDouble in channel) {
        println((percentageDouble * 100).roundToInt())
    }
}
```

`mode` contains the access rights in octal as a `String`.

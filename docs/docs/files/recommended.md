---
title: "Recommended mode"
---

## Pull file(s)/folder(s)

Optionally uses Feature.STAT_V2, Feature.LS_V2, Feature.SENDRECV_V2
{: .label .label-blue }

The following request pulls remote file(s) or folder(s):

```kotlin
val success: Boolean = adb.execute(PullRequest("/data/local/tmp/testdir", destination, supportedFeatures), "emulator-5554")
```

This request does not handle file links. When the source is a directory and the destination is an existing directory, adam creates a subdirectory.

## Push file(s)/folder(s)

Optionally uses Feature.STAT_V2, Feature.LS_V2, Feature.SENDRECV_V2
{: .label .label-blue }

The following request pushes local file(s) or folder(s) to the remote device:

```kotlin
val success: Boolean = adb.execute(PushRequest(source, "/data/local/tmp/testdir", supportedFeatures), "emulator-5554")
```

This request does not handle file links. When the source is a directory and the destination is an existing directory, adam creates a subdirectory.

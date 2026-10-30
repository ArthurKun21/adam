---
title: Sideload
---

## Sideload

The following request installs packages in sideload mode:

```kotlin
val success = adb.execute(
    request = SideloadRequest(
        pkg = file
    ),
    serial = "emulator-5554"
)
```

### Sideload for legacy devices (pre-KitKat)

For devices older than Android 4.4, use `LegacySideloadRequest`:

```kotlin
val success = adb.execute(
    request = LegacySideloadRequest(
        pkg = file
    ),
    serial = "emulator-5554"
)
```

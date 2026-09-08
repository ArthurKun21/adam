---
title: Sideload
---

## Sideload

If you need to use sideload to install packages use the following:

```kotlin
val success = client.execute(
    request = SideloadRequest(
        pkg = file
    ),
    serial = "emulator-5554"
)
```

### Sideload for legacy devices (pre KitKat)

```kotlin
val success = client.execute(
    request = LegacySideloadRequest(
        pkg = file
    ),
    serial = "emulator-5554"
)
```

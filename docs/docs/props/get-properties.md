---
title: "Device properties (getprop)"
---

These requests read the device properties. They are equivalent to executing `getprop` on the device.

## Get all device properties

The following request returns all properties as a `Map<String, String>`:

```kotlin
val properties: Map<String, String> = adb.execute(
    request = GetPropRequest(),
    serial = "emulator-5554"
)
```

You can access a property by name: `properties["sys.boot_completed"]`.

## Get single device property

If you need only a single property, use the shorter request:

```kotlin
val value: String = adb.execute(
    request = GetSinglePropRequest(name = "sys.boot_completed"),
    serial = "emulator-5554"
)
```

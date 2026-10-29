---
title: "Android Binder Bridge"
---

The Android Binder Bridge (abb) communicates directly with the services on the device. One service is `package`, which handles package management. To see the full list of services, run the abb request with the `-l` argument.

## Execute with stdout, stderr and exit code

Requires Feature.ABB
{: .label .label-yellow }

Executing a command on a service is equivalent to executing an arbitrary `cmd` sub-command (`cmd package list`, `cmd statusbar expand-notifications`, etc) on the device. The following example lists the available services:

```kotlin
val result = adb.execute(
    request = AbbRequest(listOf("-l"), supportedFeatures),
    serial = "emulator-5554"
)

println(result.stdout)
```

The result of the execution includes stdout, stderr and exit code.

## Executing with stdout only

Requires Feature.ABB_EXEC
{: .label .label-yellow }

Some devices do not support the abb requests. They support abb_exec instead, which returns stdout only:

```kotlin
val stdout = adb.execute(
    request = AbbExecRequest(listOf("-l"), supportedFeatures),
    serial = "emulator-5554"
)

println(stdout)
```

---
title: "Emulator"
---

You can control an emulator in three ways:

1. With regular adb requests
2. Through the console port
3. Through the gRPC bridge port

At boot time, each emulator allocates two ports on the loopback interface of the host: a _console_ port and a _gRPC_ bridge port. You can calculate the console port from the serial number of the emulator. For example, the console port of `emulator-5554` is 5554 and the console port of `emulator-5556` is 5556. To use a particular console port, start the emulator with a parameter:

```bash
$ emulator @Nexus_5X_API_23 -port <port>
```

The gRPC bridge port is usually the console port + 3000. The default gRPC bridge port of `emulator-5554` is 8554. To use a particular gRPC bridge port, start the emulator with a parameter:

```bash
$ emulator @Nexus_5X_API_23 -grpc <port>
```

## Run emulator console command

The following request executes an emulator console command:

```kotlin
val output: String = adb.execute(
    request =
    EmulatorCommandRequest(
        "help",
        InetSocketAddress("localhost", 5556)
    )
)
```

This request is different from the other requests: it connects directly to the emulator instead of the ADB server. It works like the ADB server requests and shares the socket creation logic with them.

## Run gRPC bridge commands

adam bundles a gRPC client for the emulator gRPC bridge, built with [kotlinx-rpc](https://kotlin.github.io/kotlinx-rpc/). The build generates the client from [emulator_controller.proto][1].

adam provides a small helper to create a plaintext client for the bridge and to obtain the service proxy (`com.arthurkun21.adam.emulator`):

```kotlin
val client = emulatorGrpcClient(grpcHost, grpcPort)
val emulatorController = client.emulatorController()
val state = emulatorController.getVmState(Empty { })

// when done
client.shutdownAndAwaitTermination()
```

Unary methods are `suspend` functions. Server-streaming methods (e.g. `streamScreenshot`, `streamLogcat`) return a cold `Flow`. Collect the `Flow` in your own coroutine scope.

See [emulator_controller.proto][1] for the supported functionality.

[1]: https://android.googlesource.com/platform/prebuilts/android-emulator/+/master/linux-x86_64/lib/emulator_controller.proto "emulator_controller.proto"

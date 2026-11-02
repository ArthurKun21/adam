---
title: "Android JUnit 4 rules"
---

adam supports tests that interact with a device over adb, the emulator console, or gRPC. A test can execute a request, set the location of the device, or emulate a sensor input.

This makes end-to-end test environments possible for complex applications. Examples: a fitness tracker app that recognizes a particular movement pattern, or an application that handles phone calls/SMS.

The [emulator_controller.proto][1] supports the following sensors. The list shows the range of the possibilities:

```
  enum SensorType {
    // Measures the acceleration force in m/s2 that is applied to a device
    // on all three physical axes (x, y, and z), including the force of
    // gravity.
    ACCELERATION = 0;
    // Measures a device's rate of rotation in rad/s around each of the
    // three physical axes (x, y, and z).
    GYROSCOPE = 1;
    // Measures the ambient geomagnetic field for all three physical axes
    // (x, y, z) in μT.
    MAGNETIC_FIELD = 2;
    // Measures degrees of rotation that a device makes around all three
    // physical axes (x, y, z)
    ORIENTATION = 3;
    // Measures the temperature of the device in degrees Celsius (°C).
    TEMPERATURE = 4;
    // Measures the proximity of an object in cm relative to the view screen
    // of a device. This sensor is typically used to determine whether a
    // handset is being held up to a person's ear.
    PROXIMITY = 5;
    // Measures the ambient light level (illumination) in lx.
    LIGHT = 6;
    // Measures the ambient air pressure in hPa or mbar.
    PRESSURE = 7;
    // Measures the relative ambient humidity in percent (%).
    HUMIDITY = 8;
    MAGNETIC_FIELD_UNCALIBRATED = 9;
  }
```

The rules require support from your test runner. [Marathon](https://github.com/MarathonLabs/marathon) is a reference implementation of this support.

A custom JUnit 4 rule injects a working implementation of adb, console, or gRPC into the test. All rules can fail via assumption, e.g. `EmulatorGrpcRule(mode = Mode.ASSUME)`. Use this mode in mixed device environments where not every run has access to an emulator or adb.

## AdbRule

The adb rule provides the usual adam requests. The requests target only the current device through the explicitly specified serial number.

```kotlin
class AdbActivityTest {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val adbRule = AdbRule(mode = Mode.ASSERT)

    @Test
    fun testVmState() {
        runBlocking {
            val result = adbRule.adb.execute(ShellCommandRequest("echo \"hello world\""))
            assert(result.exitCode == 0)
            assert(result.output.startsWith("hello world"))
        }
    }
}
```

## EmulatorGrpcRule

The gRPC rule provides the supported gRPC requests. It is valid only for an emulator. See [emulator_controller.proto][1] for the list of supported requests.

gRPC TLS and auth are not implemented.

```kotlin
class GrpcActivityTest {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val emulator = EmulatorGrpcRule(mode = Mode.ASSERT)

    @Test
    fun testVmState() {
        runBlocking {
            val vmState = emulator.grpc.getVmState(Empty { })
            assert(vmState.state == VmRunState.RunState.RUNNING)
        }
    }
}
```

The rule exposes the kotlinx-rpc generated `EmulatorController` service proxy. The proxy is a plain Kotlin interface: unary methods are `suspend` functions, server-streaming methods return a `Flow`. The generated builder DSL constructs the messages.

## EmulatorConsoleRule

The emulator console requires an auth token by default. The emulator stores the token on its host OS in the `$HOME/.emulator_console_auth_token` file. To disable the auth check, leave an empty file at the same location. The emulator then accepts requests without an auth token.

```kotlin
class ConsoleActivityTest {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val console = EmulatorConsoleRule(mode = Mode.ASSERT)

    @Test
    fun testVmState() {
        runBlocking {
            val result = console.execute("avd status")
            Allure.description("VM state is $result")
            assert(result.contains("running"))
        }
    }
}
```

## Notes for developers of test runners

Real devices expose only adb access. The test runner sets this up through reverse port forwarding. This works for both transport modes: direct USB connection or TCP via adb connect. The test communicates with adb on the device through localhost. The default port is 5037, but it can differ. Only the test runner knows how to establish the adb port, because the test runner communicates with this adb server. The test runner must therefore provide the port number to the test through the instrumentation arguments.

Emulators have two cases.

A local emulator can reach the loopback interface of the host (with all the necessary services) through the special `10.0.2.2` IP. For adb, the test runner must provide the port number for the same reasons as for real devices. For the telnet console, only the test runner can calculate the port number (`emulator-5554` -> 5554). Accessing this port requires an auth token. The test runner must supply the token, because the test cannot read the host's `$HOME/.emulator_console_auth_token` file. The same applies to the gRPC bridge: the test runner must supply everything.

For a remote emulator connected via adb connect, the telnet and gRPC ports are not currently reachable. Technically this would be possible. In practice, the adb port for such a case would most likely be port-forwarded already. There is no way to guarantee that the adb connect port is actually equal to the real adb port. Solving this would require the emulator to expose its `emulator-5554` serial somehow. Android properties would break, for example on the load of a snapshot.

The test runner contract defines passing the host and port of a provided adb/console/gRPC connection. Use it as a shared dependency:

```kotlin
dependencies {
  implementation("com.github.ArthurKun21:android-testrunner-contract:X.X.X")
}
```

The contract looks roughly like this (use the maven dependency, do not copy this code):

```java
public class TestRunnerContract {
    public static String grpcPortArgumentName = "com.malinskiy.adam.android.GRPC_PORT";
    public static String grpcHostArgumentName = "com.malinskiy.adam.android.GRPC_HOST";
    public static String adbPortArgumentName = "com.malinskiy.adam.android.ADB_PORT";
    public static String adbHostArgumentName = "com.malinskiy.adam.android.ADB_HOST";
    public static String consolePortArgumentName = "com.malinskiy.adam.android.CONSOLE_PORT";
    public static String consoleHostArgumentName = "com.malinskiy.adam.android.CONSOLE_HOST";
    public static String emulatorAuthTokenArgumentName = "com.malinskiy.adam.android.AUTH_TOKEN";
    public static String deviceSerialArgumentName = "com.malinskiy.adam.android.ADB_SERIAL";
}
```

It is the test runner's responsibility to set these up when executing tests, e.g.:

```bash
$ am instrument -w -r --no-window-animation -e class com.example.AdbActivityTest#testUnsafeAccess -e debug false -e com.malinskiy.adam.android.ADB_PORT 5037 -e com.malinskiy.adam.android.ADB_HOST 10.0.2.2 -e com.malinskiy.adam.android.ADB_SERIAL emulator-5554 -e com.malinskiy.adam.android.GRPC_PORT 8554 -e com.malinskiy.adam.android.GRPC_HOST 10.0.2.2 com.example.test/androidx.test.runner.AndroidJUnitRunner
```

[1]: https://android.googlesource.com/platform/prebuilts/android-emulator/+/master/linux-x86_64/lib/emulator_controller.proto "emulator_controller.proto"

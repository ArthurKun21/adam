---
title: "Test runner (am instrument)"
---

## Test runner request

Optionally uses Feature.SHELL_V2
{: .label .label-blue }

Execute tests with the `TestRunnerRequest`:

```kotlin
val channel: ReceiveChannel<List<TestEvent>> = adb.execute(
    request = TestRunnerRequest(
        testPackage = "com.example.test",
        instrumentOptions = InstrumentOptions(
            clazz = listOf("com.example.MyTest")
        ),
        supportedFeatures = emptyList(),
        coroutineScope = GlobalScope,
    ),
    serial = "emulator-5554"
)
```

The result is a channel of type `ReceiveChannel<List<TestEvent>>`. It contains the parsed and converted output of the `am instrument` command.

### Required parameters

Provide the `testPackage`, the `InstrumentOptions`, the `coroutineScope`, and the `supportedFeatures` of the target device.

Caution: provide the `supportedFeatures` of the target device. Newer Android devices write information into the stderr of `am instrument`. The textual instrumentation parser reads only stdout. adam runs the command over the shell v2 protocol only when the device reports `Feature.SHELL_V2`. Shell v2 keeps stderr as a separate stream, so the stderr output does not pollute the parsed stdout.

### Runner class

The default `runnerClass` is `android.support.test.runner.AndroidJUnitRunner`. Set the `runnerClass` option to change it.

For all options, check the [source](https://android.googlesource.com/platform/frameworks/base/+/master/cmds/am/src/com/android/commands/am/Am.java#155) of the `am` command.

### No hidden api checks

This option disables the restrictions on the use of hidden APIs.

### No window animation

This option turns off window animations while the tests run.

### No isolated storage

This option skips the isolated-storage sandbox and mounts the full external storage.

### User ID

This option selects the user that the instrumentation runs in. Without it, the instrumentation runs in the current user.

### ABI

This option launches the instrumented process with the selected ABI. It assumes that the process supports the selected ABI.

### Profiling output

This option writes profiling data to the specified path on the device.

### Output log path

This option writes the test log to the specified path on the device.

### Protobuf output

This option writes the output as protobuf. The protobuf output is machine readable. Requires API 26+.

## Instrument options

### pkg

The fully-qualified Java package name of one of the packages in the test application. adam executes any test case class that uses this package name. This is not an Android package name. A test package has a single Android package name. It may contain several Java packages.

### clazz

The fully-qualified Java class name of one of the test case classes. adam executes only this test case class.

Alternatively, use `<class_name>#method`. This value selects a fully-qualified test case class and one of its methods. adam executes only this method. Note the hash mark (#) between the class name and the method name.

### functional

This option runs all test classes that extend `InstrumentationTestCase`.

### unit

This option runs all test classes that do not extend `InstrumentationTestCase` or `PerformanceTestCase`.

### filterSize

This option runs test methods with a size annotation. The annotations are `@SmallTest`, `@MediumTest`, and `@LargeTest`.

### performance

This option runs all test classes that implement `PerformanceTestCase`.

### debug

This option runs the tests in debug mode.

### log

This option loads and logs the specified tests without running them. The test information appears in stdout. Use it to check combinations of other filters and test specifications.

### emma

This option runs an EMMA code coverage analysis. EMMA is a legacy feature of `am instrument`. The output goes to `/data/<app_package>/coverage.ec` on the device.

To override the file location, use the `coverageFile` option described below.

### coverageFile

This option overrides the default location of the EMMA coverage file on the device. Specify a path and filename in UNIX format. The default filename is described in the `emma` option.

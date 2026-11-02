---
title: "Android test metric producers"
---

adam provides producers of test statuses. They inform the test runner about the test execution. See [Android JUnit 4 rules](1-android-junit.md) for the test runner integration.

## TestAnnotationProducer

Artifact: `com.github.ArthurKun21:android-junit4-test-annotation-producer:X.X.X`

This producer emits the annotations of the current test as a metric, e.g.:

```
INSTRUMENTATION_STATUS_CODE: 0
INSTRUMENTATION_STATUS: class=com.example.FailedAssumptionTest
INSTRUMENTATION_STATUS: current=4
INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
INSTRUMENTATION_STATUS: numtests=39
INSTRUMENTATION_STATUS: stream=
com.example.FailedAssumptionTest:
INSTRUMENTATION_STATUS: test=ignoreTest
INSTRUMENTATION_STATUS_CODE: 1
INSTRUMENTATION_STATUS: com.malinskiy.adam.junit4.android.listener.TestAnnotationProducer.v4=[64Lorg.junit.Test(34Lexpected=class org.junit.Test$None9Ltimeout=0), 46Lio.qameta.allure.kotlin.Epic(13Lvalue=General), 93Lorg.junit.runner.RunWith(64Lvalue=class io.qameta.allure.android.runners.AllureAndroidJUnit4), 65Lio.qameta.allure.kotlin.Feature(29Lvalue=Graphics on main screen), 45Lio.qameta.allure.kotlin.Owner(11Lvalue=user2), 44Lio.qameta.allure.kotlin.Story(10Lvalue=Slow), 51Lio.qameta.allure.kotlin.Severity(14Lvalue=critical), 197Lkotlin.Metadata(26LbytecodeVersion=[I@199c8a631Ldata1=[Ljava.lang.String;@132e733Ldata2=[Ljava.lang.String;@3af3e9411LextraInt=4812LextraString=6Lkind=126LmetadataVersion=[I@126ed3d12LpackageName=)]
INSTRUMENTATION_STATUS_CODE: 2
INSTRUMENTATION_STATUS: class=com.example.FailedAssumptionTest
INSTRUMENTATION_STATUS: current=4
INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
INSTRUMENTATION_STATUS: numtests=39
INSTRUMENTATION_STATUS: stream=.
INSTRUMENTATION_STATUS: test=ignoreTest
```

This is useful when bytecode analysis is too complex and it is easier to let the Android OS report the annotations of each test.

## AdamScreenCaptureProcessor

Artifact: `com.github.ArthurKun21:androidx-screencapture:X.X.X`

This producer emits test screen captures. It uses `androidx.test.runner.screenshot`, e.g.:

```
INSTRUMENTATION_STATUS_CODE: 0
INSTRUMENTATION_STATUS: class=com.example.FailedAssumptionTest
INSTRUMENTATION_STATUS: current=4
INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
INSTRUMENTATION_STATUS: numtests=39
INSTRUMENTATION_STATUS: stream=
com.example.FailedAssumptionTest:
INSTRUMENTATION_STATUS: test=ignoreTest
INSTRUMENTATION_STATUS_CODE: 1
INSTRUMENTATION_STATUS: com.malinskiy.adam.junit4.android.screencapture.AdamScreenCaptureProcessor.v1=/sdcard/images/screenshot/screenshot-1.png
INSTRUMENTATION_STATUS_CODE: 2
INSTRUMENTATION_STATUS: class=com.example.FailedAssumptionTest
INSTRUMENTATION_STATUS: current=4
INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
INSTRUMENTATION_STATUS: numtests=39
INSTRUMENTATION_STATUS: stream=.
INSTRUMENTATION_STATUS: test=ignoreTest
```

This is useful to inform the test runner about the test screenshots and to optionally attach them to the test report.

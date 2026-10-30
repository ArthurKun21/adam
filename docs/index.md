---
title: Home
search:
  exclude: true
hide:
  - navigation
---

Android Debug Bridge client library written in Kotlin.

[Get started now](#getting-started){: .md-button .md-button--primary } [View it on GitHub](https://github.com/ArthurKun21/adam){: .md-button }

## Motivation

The Java world has one way to access adb programmatically: the ddmlib library. ddmlib has several limitations:

1. Sub-optimal resource usage
2. Insufficient test coverage
3. The limitations of the ADB server propagate to the ddmlib user

adam uses coroutines instead of blocking threads to reduce the resource usage. This reduces the load dramatically in scenarios where the host communicates with dozens of connected devices. To guarantee stability, the project also runs full end-to-end tests with at least one Android emulator.

## Supported functionality

* Shell
  * Basic `shell:` support (with stdout and a patched exit code)
  * `shell_v2` support (with separate stdout, stderr and exit code, plus stdin)
  * Exec shell with stdin on legacy devices without `shell_v2` support
* Package install, uninstall, list
  * Streaming installation
  * Atomic multi-package installation
  * Apk split installation
  * APEX support
  * Sideload (with pre-KitKat support)
  * Install sessions support
* Device management
  * List connected devices
  * Monitor connected devices continuously
  * Fetch device features
  * Connect/disconnect/reconnect device
  * adb over WiFi pairing
  * Reboot device
* Files
  * List files using `ls`
  * Push/pull files and folders (recursive)
  * Stat, list, pull and push using `sync:`
  * Support for stat_v2, sendrecv_v2, ls_v2
* Emulator commands (`gsm call`, `rotate`, etc)
* Props
  * Get single prop
  * Get all props
* Instrumented tests
  * Raw output parsing
  * Proto output parsing
* Screen capture
  * Dynamic adapters with raw buffer and fast BufferedImage conversion
  * Support for legacy devices as well as new sRGB and DCI-P3 devices
* Logcat
  * Read the logcat log
  * Monitor logcat continuously
* Port-forwarding (including reverse port-forwarding)
  * List ports
  * Add rule
  * Remove rule
  * Remove all rules
* Android Binder Bridge: `abb` and `abb_exec`
* Restart adbd on device: `root:`, `unroot:`, plus transport switch `usb:`, `tcpip:`
* Miscellaneous
  * Get the ADB server version
  * Kill the ADB server
  * Remount partitions
  * Enable/disable dm-verity checking on userdebug builds
  * Fetch host features
  * Check mDNS discovery availability
  * List all mDNS discovered services

Plus any shell command on the device.

## Getting started

[![Release](https://jitpack.io/v/ArthurKun21/adam.svg)](https://jitpack.io/#ArthurKun21/adam)

Add the JitPack repository and the adam dependency.

Maven:

```xml
<dependency>
  <groupId>com.github.ArthurKun21</groupId>
  <artifactId>adam</artifactId>
  <version>X.X.X</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.ArthurKun21:adam:X.X.X")
}
```

## Basic usage example

```kotlin
//Start the ADB server
StartAdbInteractor().execute()

//Create the adb client
val adb = AndroidDebugBridgeClientFactory().build()

//Execute a request
val output = adb.execute(ShellCommandRequest("echo hello"), "emulator-5554")
println(output.output) // hello
```

## About the project

adam is &copy; 2019-2026 by [Anton Malinskiy](http://github.com/Malinskiy).

### License

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

### Contributing

Before you change this repository, discuss the change with the maintainers. Use an issue, an email, or [Slack #adam](https://bit.ly/2LLghaW).

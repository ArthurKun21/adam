# AGENTS.md

## Build & Test

- Build: `./gradlew build`
- Lint: `./gradlew spotlessCheck` (fix: `./gradlew spotlessApply`)
- Run all tests: `./gradlew test`
- Run Ktor transport regression tests: `./gradlew :transport-ktor:test --tests "com.malinskiy.adam.transport.ktor.KtorSocketRegressionTest"`
- Purpose: guards Ktor byte-array offset/limit API semantics so Adam `Socket` methods keep length-based behavior during Ktor upgrades.
- Run single test: `./gradlew :adam:test --tests "com.malinskiy.adam.ClassName.testMethod"`
- Integration tests: `./gradlew :adam:integrationTest`

## Architecture

Kotlin coroutine-based ADB (Android Debug Bridge) client library. Modules:

- **adam** — Core ADB client (requests, transport, protobuf/gRPC). Main package: `com.malinskiy.adam`
- **transport-ktor** — Ktor-based transport implementation
- **android-junit4**, **android-testrunner-contract**, **androidx-screencapture** — Android test instrumentation helpers
- **server/** — Server stubs for testing (JUnit 4 & 5 variants)
- **buildSrc** — Convention plugins (`adam.jvm`, `adam.code.lint`, `adam.android.library`)

## Code Style

- Kotlin, JVM target 17. No star imports (star import threshold 999). Max line length 120.
- Formatting: ktlint (IntelliJ IDEA style) via Spotless. Trailing commas allowed.
- Tests use JUnit 4 + assertk + kotlinx.coroutines (`runBlocking`). Server stubs from `:server:server-stub-junit4`.
- Apache 2.0 license header required on all source files.
- 4-space indent, UTF-8, trailing whitespace trimmed, final newline required.

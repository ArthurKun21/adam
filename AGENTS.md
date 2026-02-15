# AGENTS.md

## Build & Test

- Build: `./gradlew build`
- Assemble only: `./gradlew assemble`
- Lint: `./gradlew spotlessApply`
- Run all tests: `./gradlew test`
- Run Ktor transport regression tests: `./gradlew :transport-ktor:test --tests "com.malinskiy.adam.transport.ktor.KtorSocketRegressionTest"`
  - Purpose: guards Ktor byte-array offset/limit API semantics so Adam `Socket` methods keep length-based behavior during Ktor upgrades.
- Run single test: `./gradlew :adam:test --tests "com.malinskiy.adam.ClassName.testMethod"`
- Integration tests: `./gradlew :adam:integrationTest`
- Coverage report: `./gradlew jacocoTestReport` (unit), `./gradlew :adam:jacocoIntegrationTestReport` (integration)
- Generate docs: `./gradlew :adam:dokkaGeneratePublicationHtml` (output: `docs/api/`)

## Architecture

Kotlin coroutine-based ADB (Android Debug Bridge) client library. Modules:

- **adam** — Core ADB client (requests, transport, protobuf/gRPC). Main package: `com.malinskiy.adam`
- **transport-ktor** — Ktor-based transport implementation
- **android-junit4** — Android JUnit4 test rules for adam
- **android-junit4-test-annotation-producer** — Android test annotation producer
- **android-testrunner-contract** — Android test runner contract interfaces
- **androidx-screencapture** — AndroidX screen capture helpers
- **server/** — Server stubs for testing (`server-stub`, `server-stub-junit4`, `server-stub-junit5`)
- **buildSrc** — Convention plugins (`adam.jvm`, `adam.code.lint`, `adam.android.library`) and build logic (`ProjectConfig`, `IntegrationTestConfig`, `MavenPomConfig`, `ProjectExtensions`)

## Key Dependencies

- Kotlin 2.3.10, Coroutines 1.10.2
- Ktor 3.4.0 (network/transport)
- Vert.x 5.0.7 (core, kotlin, coroutines)
- Protobuf 4.33.5 (javalite) + gRPC 1.79.0
- Android Gradle Plugin 9.0.0 (compileSdk 36, minSdk 24, targetSdk 36)

## Code Style

- Kotlin, JVM target 17. Explicit API mode for library modules. No star imports (star import threshold 999). Max line length 120.
- Formatting: ktlint (IntelliJ IDEA style) via Spotless (`com.diffplug.spotless`). Trailing commas allowed.
- Tests use JUnit 4 + assertk + kotlinx.coroutines (`runBlocking`). Server stubs from `:server:server-stub-junit4`.
- Apache 2.0 license header required on all source files.
- 4-space indent, UTF-8, trailing whitespace trimmed, final newline required.
- Publishing: Vanniktech Maven Publish plugin to GitHub Packages (`com.github.ArthurKun21` group).

## CI

- GitHub Actions (`ci.yaml`): runs on push to `dev` and PRs.
- Java 17 (Temurin distribution).
- Unit tests + Ktor transport tests + JaCoCo coverage.
- Integration tests on Android emulator (API levels 24, 34, 35) with AVD caching.

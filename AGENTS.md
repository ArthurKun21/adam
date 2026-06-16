# AGENTS.md

## Build & Test

- Build: `./gradlew build`
- Assemble only: `./gradlew assemble`
- Lint: `./gradlew spotlessApply`
- Run all tests: `./gradlew test`
- Run single test: `./gradlew :adam:test --tests "com.malinskiy.adam.ClassName.testMethod"`
- Run single test in other modules: `./gradlew :transport-ktor:test --tests "com.malinskiy.adam.transport.ktor.KtorSocketRegressionTest"`
- Ktor transport regression tests: `./gradlew :transport-ktor:test --tests "com.malinskiy.adam.transport.ktor.KtorSocketRegressionTest"`
  - Guards Ktor byte-array offset/limit API semantics
- Integration tests: `./gradlew :adam:integrationTest`
- Coverage reports: `./gradlew jacocoTestReport` (unit), `./gradlew :adam:jacocoIntegrationTestReport` (integration)
- Combined coverage: `./gradlew :adam:jacocoCombinedTestReport`
- Generate docs: `./gradlew :adam:dokkaGeneratePublicationHtml` (output: `docs/api/`)

## Architecture

Kotlin coroutine-based ADB (Android Debug Bridge) client library.

### Modules

- **adam** — Core ADB client (requests, transport, protobuf/gRPC). Main package: `com.malinskiy.adam`
- **transport-ktor** — Ktor-based transport implementation
- **android-junit4** — Android JUnit4 test rules for adam
- **android-junit4-test-annotation-producer** — Android test annotation producer
- **android-testrunner-contract** — Android test runner contract interfaces
- **androidx-screencapture** — AndroidX screen capture helpers
- **server/** — Server stubs for testing (`server-stub`, `server-stub-junit4`, `server-stub-junit5`)
- **build-logic** — Convention plugins and build logic

### Key Types

- `AndroidDebugBridgeClient` — Main entry point for ADB communication
- `Request` — Base class for all ADB requests; subclasses: `ComplexRequest<T>`, `AsyncChannelRequest<T, I>`, `MultiRequest<T>`
- `Socket` — Interface for transport layer (implemented by `KtorSocket`)
- `Target` — Request targeting (HostTarget, DeviceTarget, etc.)

## Code Style

### Formatting

- Kotlin, JVM target 17
- ktlint (IntelliJ IDEA style) via Spotless (`com.diffplug.spotless`)
- 4-space indent, UTF-8, max line length 120
- Trailing commas allowed
- Trailing whitespace trimmed, final newline required
- Explicit API mode enabled for library modules (`kotlin.explicitApi()`)

### Imports

- No star imports (threshold set to 999 to force explicit imports)
- Import aliases for disambiguation: `import io.ktor.network.sockets.Socket as RealKtorSocket`

### Naming Conventions

- Classes: PascalCase (e.g., `AndroidDebugBridgeClient`, `ListDevicesRequest`)
- Functions/properties: camelCase (e.g., `execute`, `readElement`, `socketIdleTimeout`)
- Constants: `UPPER_SNAKE_CASE` in companion objects or objects
- Extension functions: placed in `extension/` package (e.g., `Socket.kt` extensions)
- Exception classes: suffix `Exception` (e.g., `RequestRejectedException`, `PushFailedException`)

### Types

- Use `public` modifier explicitly (explicit API mode)
- Prefer `suspend` functions for async operations
- Use `Result` or throw exceptions for error handling
- Prefer `ByteBuffer` and `ByteArray` for binary data
- Use Kotlin nullable types (`?`) for optional values

### Error Handling

- Custom exceptions extend `RuntimeException` (e.g., `RequestRejectedException`, `RequestValidationException`)
- Request validation uses `ValidationResponse` data class
- Socket operations catch exceptions during cleanup and log them

### Logging

- Use `AdamLogging.logger {}` to create a logger
- Log levels: `debug`, `info`, `warn`, `error`, `trace`
- Pattern: `log.debug(e) { "message" }`

## Testing

- JUnit 4 with `@Test`, `@Rule`
- assertk for assertions: `assertThat(value).isEqualTo(expected)`
- `runBlocking { }` for coroutine tests
- Server stubs from `:server:server-stub-junit4` for mocking ADB server
- Test naming: `test<Description>` (e.g., `testReturnsProperContent`)
- Integration tests in `src/integrationTest/kotlin/`

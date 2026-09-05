# Plan: Replace Vert.x with Ktor as adam's default transport

Branch: `feat/switch-vertx` · Date: 2026-09-05

## 1. Goal

Replace `io.vertx` (vertx-core, vertx-lang-kotlin, vertx-lang-kotlin-coroutines 5.1.7) with
`io.ktor:ktor-network-jvm` 3.5.2 as the transport implementation of the `adam` core library,
while maintaining identical functionality: the full ADB protocol surface (handshake, shell v1/v2,
sync push/pull, framebuffer, logcat, test runner, emulator console) must keep working through the
existing `Socket`/`SocketFactory` abstraction with no API change for consumers.

Approved decisions:

- **Clean break**: delete the Vert.x implementation and the deprecated `transport-ktor` module.
- **New package name**: `com.arthurkun21.adam.transport.ktor` (fork namespace, matches
  `com.arthurkun21.adam.samples.desktop`).

## 2. Research findings

### 2.1 Current state

- Vert.x is used **only** in `adam` core, in 3 files under `com.malinskiy.adam.transport.vertx`:
  - `VertxSocketFactory.kt` — default `SocketFactory`; lazily creates a `Vertx` instance, deploys
    a `VertxSocket` verticle per TCP connection, `close()` only if initialized.
  - `VertxSocket.kt` — `CoroutineVerticle : Socket`; connects via `NetClient`, writes via
    `NetSocket.write(Buffer)`, reads via `VariableSizeRecordParser` bridged to a
    `ReceiveChannel` (`ReadStream.toChannel`), TCP-like state machine, drain-on-close.
  - `VariableSizeRecordParser.kt` — pull-based framing parser (custom fork of Vert.x `RecordParser`).
- The only reference outside that package is the default in
  `AndroidDebugBridgeClientFactory.kt:38` (`socketFactory ?: VertxSocketFactory(...)`).
- The `Socket` interface (`adam/.../transport/Socket.kt`) is transport-agnostic (pure
  `java.nio`/`java.net` types). Read contract: return value `> 0` = bytes read, `-1` = EOF, `0` =
  tolerated by consumers (`Socket.copyTo` yields and retries).
- Per-request sockets: `AndroidDebugBridgeClient.execute` opens a fresh socket per request via
  `socketFactory.tcp(socketAddress, connectTimeout?, idleTimeout = request.socketIdleTimeout)`,
  so per-connect options are sufficient (no shared-connection edge cases).
- `server/server-stub` (the ADB-server simulator used by the whole test suite) is **already
  Ktor-network based**; ktor 3.5.2 is already pinned in `gradle/libs.versions.toml`.
- gRPC uses `grpc-okhttp` (not grpc-netty), so removing Vert.x removes **Netty entirely** from the
  dependency tree.
- History: Ktor was the original transport; Vert.x replaced it in 2022 (PR #35 "Faster IO") due to
  Ktor 2.x-era "stability and performance issues"; `transport-ktor` was extracted and deprecated
  (PR #76) "in the hopes that eventually ktor may become fast".

### 2.2 Ktor 3.5.2 API facts (verified from source)

| Need | Ktor 3.5.2 answer |
|---|---|
| Connect with timeout | No `connectTimeout` in `SocketOptions` — use `withTimeout(n) { connect() }`; connect suspends via non-blocking NIO + `SelectorManager` and is cancellable; close the socket defensively on failure |
| Idle/read timeout per request | `socketTimeout` (read **and** write) in `TCPClientSocketOptions`; implemented as an idle deadline that closes the channel with `SocketTimeoutException` — equivalent to Vert.x `idleTimeout` |
| `readAvailable(ByteArray, offset, limit): Int` | `ByteReadChannel.readAvailable(sink, offset, length)` — suspends until ≥1 byte via `awaitContent()`, returns `-1` on EOF; `length` is a byte count. Matches Vert.x semantics |
| `readFully(ByteArray, offset, limit)` | `ByteReadChannel.readFully(sink, start, end)` — `end` is **exclusive** → pass `offset + limit` |
| `readFully(ByteBuffer)` / `writeFully(ByteBuffer)` | Overloads exist (proven by `transport-ktor` compiling against 3.5.2) |
| Endianness | `readInt`/`writeInt` are big-endian → wrap with `Integer.reverseBytes` for ADB's little-endian |
| TCP_NODELAY | `noDelay` defaults to `true` (matches Vert.x) |

The old `KtorSocket` carries an `@InternalAPI` workaround in `readAvailable`
(`if (!isClosedForRead && readBuffer.buffer.size == 0L) return 0`) carried forward from the Ktor 2
era during the 2026-02 dependency update. Current Ktor 3.5.2 `readAvailable` handles exhaustion
correctly, so the workaround is expected to be removable — verify with tests; keep only if needed.

### 2.3 Verdict

Feasible with functional parity and materially lower risk than in 2022. Ktor is coroutine-native
(no verticle-per-connection deployment, no custom record parser — the implementation gets simpler),
officially supports Android, and consolidates the stack on one network library.

## 3. Implementation steps

### Step 1 — Add the Ktor transport to adam core

Create `adam/src/main/kotlin/com/arthurkun21/adam/transport/ktor/` with Apache-2.0 headers,
explicit `public` (explicit API mode), 4-space indent, no star imports:

**`KtorSocketFactory.kt`**

```kotlin
public class KtorSocketFactory(
    private val connectTimeout: Long = 10_000,
    private val idleTimeout: Long = 30_000,
) : SocketFactory {
    private val selectorManager by lazy { SelectorManager(Dispatchers.IO).also { initialized.set(true) } }
    private val initialized = AtomicBoolean(false)

    override suspend fun tcp(socketAddress: InetSocketAddress, connectTimeout: Long?, idleTimeout: Long?): Socket {
        val address = InetSocketAddress(socketAddress.hostName, socketAddress.port) // ktor alias
        val socket = try {
            withTimeout(connectTimeout ?: this@KtorSocketFactory.connectTimeout) {
                aSocket(selectorManager).tcp().connect(address) {
                    socketTimeout = idleTimeout ?: this@KtorSocketFactory.idleTimeout
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw SocketTimeoutException("Timed out connecting to $socketAddress")
        }
        return KtorSocket(socket)
    }

    override fun close() {
        if (initialized.get()) selectorManager.close()
    }
}
```

Details:
- Mirror `VertxSocketFactory`'s `initialized` guard so `close()` before any connection is a no-op.
- Keep the `withTimeout` scope tight; on any connect failure ensure the underlying channel is
  closed (Ktor's `buildOrClose` does this for connect failures; the defensive close covers
  cancellation races).
- Preserve the factory-level defaults + per-call override semantics of the current default.

**`KtorSocket.kt`** — port of `transport-ktor/.../KtorSocket.kt` without `@Deprecated`:

- `readAvailable`: plain `readChannel.readAvailable(buffer, offset, limit)` — **no `@InternalAPI`
  workaround** (see §2.2).
- `readFully(ByteArray, offset, limit)` → `readChannel.readFully(buffer, offset, offset + limit)`.
- `readFully(ByteBuffer): Int` → delegate to `readChannel.readFully(buffer)`, return count
  (Ktor throws `EOFException` on short reads where Vert.x relied on `assert` — strictly better;
  acceptable behavior change, documented).
- `writeFully(ByteBuffer)` / `writeFully(ByteArray, offset, limit)` → delegate
  (`writeFully(source, start, end)` with `end = offset + limit`).
- `readByte`/`writeByte` → delegate.
- `readIntLittleEndian`/`writeIntLittleEndian` → `Integer.reverseBytes(...)` wrapping.
- `isClosedForRead`/`isClosedForWrite` → delegate to `readChannel`/`writeChannel`.
- `close()` → `flushAndClose()` + `readChannel.cancel()` + `socket.close()`, exceptions caught and
  logged via `AdamLogging.logger {}` (same as existing implementation).

### Step 2 — Wire as default

- `adam/src/main/kotlin/com/malinskiy/adam/AndroidDebugBridgeClientFactory.kt`: replace the
  `VertxSocketFactory` default with `com.arthurkun21.adam.transport.ktor.KtorSocketFactory(...)`.
  The factory's public API is otherwise unchanged.

### Step 3 — Remove Vert.x

- Delete `adam/src/main/kotlin/com/malinskiy/adam/transport/vertx/` (all 3 files;
  `VariableSizeRecordParser` is not needed — Ktor reads are natively pull-based).
- `adam/build.gradle.kts`: remove `libs.vertx.core`, `libs.vertx.kotlin`, `libs.vertx.coroutines`;
  add `implementation(libs.ktor.network)`.
- `gradle/libs.versions.toml`: remove the `vertx = "5.1.7"` version and the 3 `vertx-*` library
  entries. Keep `ktor-network` (used by `server-stub` and now `adam`).

### Step 4 — Delete the transport-ktor module (clean break)

- Delete the `transport-ktor/` directory.
- Remove `include(":transport-ktor")` from `settings.gradle.kts`.
- Update `AGENTS.md`: drop the `transport-ktor` module entry and the
  `KtorSocketRegressionTest` invocation notes; document that the default transport is the
  Ktor-based implementation inside `adam` (`com.arthurkun21.adam.transport.ktor`).
- Check `renovate.json` for transport-ktor-specific rules (none expected beyond version-catalog
  automation).

### Step 5 — Tests & validation

- **Port** `transport-ktor/src/test/.../KtorSocketRegressionTest.kt` into
  `adam/src/test/kotlin/com/arthurkun21/adam/transport/ktor/` against the new core `KtorSocket`
  (real TCP loopback socket pair; guards the ByteArray offset/limit semantics for both
  `readFully` and `writeFully`).
- Run:
  - `./gradlew spotlessApply`
  - `./gradlew :adam:test` — the existing suite exercises the full ADB protocol end-to-end
    (server-stub = Ktor server, adam client = Ktor client now); this is the main parity gate.
  - `./gradlew build`
  - `./gradlew :adam:integrationTest` if a local adb server is available.
- Manual sanity check (optional): push/pull a file and run shell v2 against a real device/emulator.

## 4. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Historical "stability/performance issues" resurface | Those were Ktor 2.x-era; semantics verified from 3.5.2 source; offset/limit regression tests + full protocol suite as the gate |
| Empty-buffer read edge case (old `@InternalAPI` hack) | Start without the hack; if tests show a hang, restore the documented workaround and note it |
| Connect cancellation leaves a half-open channel | Tight `withTimeout` scope + defensive socket close in the catch path |
| Behavior differences (EOFException on short `readFully` instead of silent partial reads) | Strictly better than Vert.x under non-`-ea` JVMs; documented in the plan |
| Published-artifact impact | `com.malinskiy.adam` API unchanged (factory still accepts any `SocketFactory`); Vert.x-specific public classes and the deprecated `transport-ktor` artifact are removed — clean break per approved decision |

## 5. Out of scope / follow-ups

- No Kotlin Multiplatform work (the `Socket` interface itself still uses JVM types).
- No throughput benchmark in this change; a follow-up push/pull throughput check against a real
  device can validate the "Faster IO" claim if desired.

## 6. Addendum (found during implementation)

- **`AdbDeviceRule.waitForDevice` hot-spin defect** (pre-existing, exposed by the swap): with no
  device attached, the rule's device-poll loop retried `ListDevicesRequest` in a tight loop with
  no delay (~49k TCP connects per test with the faster Ktor connect cycle vs. a few thousand with
  Vert.x's per-connection verticle overhead). At that rate one connect eventually stalls past the
  10s connect timeout, and the resulting `SocketTimeoutException` escaped the rule (it only caught
  `ConnectException`), turning designed "skip" outcomes into failures. Fixed by adding a
  `delay(1_000)` poll interval to the loop.
- **Forward/reverse-list parsing broken by serials containing spaces** (pre-existing, fixed):
  modern adb mDNS/TLS transports use serials like
  `adb-126354051R007897-k3TQnV (2)._adb-tls-connect._tcp` (contains a space). `adb list forwards`
  / `reverse:list-forward` responses are `<serial> <spec> <spec>` lines, and both
  `ListPortForwardsRequest` and `ListReversePortForwardsRequest` split lines by space, which
  misaligned the specs whenever a forward existed for such a serial
  (`UnsupportedForwardingSpecException: Unknown type (2)._adb-tls-connect._tcp`). Fixed by
  locating the two trailing spec fields from the end of the line (`lastIndexOf(' ')`), keeping the
  serial as everything before. Regression tests added to both request test classes.
  Transport-independent (would fail identically on Vert.x when the device's only/first transport
  is the mDNS one).

# Plan: Replace grpc-java/protobuf-javalite with kotlinx-rpc (gRPC) + kotlinx-serialization (instrumentation proto)

Branch: `feat/kotlin-rpc` · Date: 2026-09-08

## 1. Goal

Replace the gRPC/protobuf stack of the `adam` core library:

- **gRPC layer** (`EmulatorController` bridge): `io.grpc:grpc-stub` + `grpc-kotlin-stub` +
  `grpc-protobuf-lite` + `protobuf-javalite` + `protoc`/`protoc-gen-grpc-java`/`protoc-gen-grpc-kotlin`
  codegen (protobuf-gradle-plugin 0.10.0) → **kotlinx-rpc gRPC** (`0.11.0-grpc-189`, Kotlin-first
  codegen, `GrpcClient`).
- **Instrumentation proto** (`instrumentation-data.proto`, proto2, parsed from the raw ADB socket):
  `protobuf-javalite` → **kotlinx-serialization protobuf** (stable, Maven Central).

Note on `protobuf-javalite`: it leaves adam's direct dependencies and the compile classpath, but it
**remains on the runtime classpath** as kotlinx-rpc's own transitive dependency — the generated
marshallers require it (see §6, "protobuf-javalite is load-bearing").

Functionality must be preserved: plaintext unary + server-streaming calls against the emulator's
gRPC bridge (`EmulatorGrpcRule`, `EmulatorGrpcE2ETest`) and the fragmented protobuf-stream parsing of
`am instrument` output (`ProtoInstrumentationResponseTransformer`, public signature unchanged).

Approved decisions (user-confirmed):

- **Migrate now** to kotlinx-rpc `0.11.0-grpc-189` (pre-release), accepting the extra Maven repo and
  API breaks.
- **Both protos leave the protoc pipeline**: `emulator_controller.proto` → kotlinx-rpc codegen;
  `instrumentation-data.proto` → hand-written `@Serializable` Kotlin model.
- **Transport spike order**: `grpc-okhttp` first (keeps Netty out, same rationale as the
  Vert.x→Ktor migration); fall back to `grpc-netty` (the kotlinx-rpc docs default) if OkHttp's
  provider is not picked up.
- **Clean break** in one change, new hand-written code under `com/arthurkun21/adam/`, plan doc
  committed with the implementation.

## 2. Research findings

### 2.1 Current state

- 5 `api` deps in `adam/build.gradle.kts` (L135–139): `protobuf-javalite` 4.36.1,
  `grpc-protobuf-lite`/`grpc-stub`/`grpc-okhttp` 1.84.0, `grpc-kotlin-stub` 1.5.0 — every consumer
  of `adam` gets them on the compile classpath. Codegen via protobuf-gradle-plugin 0.10.0
  (protoc 4.36.1 + grpc-java + grpc-kotlin generators, all with `option("lite")`).
- `emulator_controller.proto`: proto3, `package android.emulation.control`,
  `java_package com.android.emulator.control`; service `EmulatorController` with 27 RPCs — only
  unary + 6 server-streaming; heavy use of `google.protobuf.Empty`; no client/bidi streaming.
- `instrumentation-data.proto`: proto2, `package android.am`, messages only (all fields
  `optional`/`repeated`, `sint32`/`sint64` zigzag fields, `bytes`, recursive message), no service.
- Hand-written gRPC consumers (only 2): `EmulatorGrpcRule.kt` (android-junit4; public
  `lateinit var grpc: EmulatorControllerCoroutineStub`; `ManagedChannelBuilder` plaintext +
  executor; `shutdownNow()` + `awaitTermination`) and `EmulatorGrpcE2ETest.kt`
  (`getStatus(Empty.getDefaultInstance())`). No interceptors/TLS/metadata/deadlines anywhere.
- Non-gRPC protobuf consumer: `ProtoInstrumentationResponseTransformer` (`Session.parseFrom` over a
  memory-mapped buffer, fragmentation-tolerant, compacts by `session.serializedSize`). Public API
  does not expose protobuf types (`Const.MAX_PROTOBUF_PACKET_LENGTH` aside).
- The ADB wire protocol itself is plain length-prefixed bytes over the Ktor `Socket` — untouched.

### 2.2 kotlinx-rpc gRPC facts (verified from 0.11.0-grpc-189 docs/samples/source)

| Need | kotlinx-rpc answer |
|---|---|
| Availability | Dev pre-releases only (`0.11.0-grpc-189`), published at `https://redirector.kotlinlang.org/maven/kxrpc-grpc`; stable 0.10.3 has no gRPC; "no stability guarantees, DSL may change between dev builds" |
| JVM implementation | Built **on top of gRPC-Java**: `kotlinx-rpc-grpc-core` api-exposes `io.grpc:grpc-api`, `grpc-util`, `grpc-stub` (bundled version 1.79.0); a gRPC-Java transport must be added explicitly (docs: `grpc-netty` 1.79.0; OkHttp provider is the spike candidate) |
| Gradle setup | Plugin `org.jetbrains.kotlinx.rpc.plugin`; `rpc { protoc() }` (or `kotlinx.rpc.protoc=true`); Buf CLI auto-provisioned; do **not** also apply `com.google.protobuf` to the same module; Gradle 9+ emits a known eager-configuration warning (cosmetic) |
| Artifacts | `kotlinx-rpc-protobuf` (runtime), `kotlinx-rpc-grpc-core` (full runtime incl. bundled WKT), `kotlinx-rpc-grpc-client`; server modules not needed |
| Generated code | Kotlin message interfaces + builder DSL (compiler plugin), `@Grpc` service interfaces; unary = `suspend fun`, server-streaming = `Flow<T>`; one `.proto` → one file; proto comments → KDoc |
| Packages | Package comes from the proto `package` declaration — `java_package` is **ignored** (no `kotlin_package` option): `android.emulation.control` |
| WKT | `google.protobuf.Empty` etc. generated as plain messages (bundled in full runtime) |
| proto2 | Supported (presence via `has<Field>()` + defaults; oneofs as sealed interfaces); `optionalFieldOrNullGetters` option exists |
| Client | `GrpcClient(host, port) { credentials = plaintext() }`; `client.withService<Service>()`; lifecycle mirrors ManagedChannel (`shutdown`/`awaitTermination`/`shutdownNow`) |
| Limitations | No per-method interceptors/call options; no descriptor reflection; no JSON; unknown fields preserved on round-trip but not readable; field-presence API still in flux; incompatible with protobuf-java types |
| Codegen options | `explicitApiModeEnabled`, `camelCaseNames` (default true), `generateComments`, `optionalFieldOrNullGetters`, ... |
| IDE | Kotlin External FIR Support (KEFS) plugin required for compiler-generated declarations; KT-84711 on IDE 2026.1 |

### 2.3 kotlinx-serialization protobuf facts (for instrumentation-data)

- Stable artifact `org.jetbrains.kotlinx:kotlinx-serialization-protobuf` on Maven Central;
  compiler plugin `org.jetbrains.kotlin.plugin.serialization` (version = Kotlin version).
- Zigzag: `@ProtoType(ProtoIntegerType.SIGNED)` = `sint32`/`sint64`. `bytes` → `ByteArray`.
  Field numbers via `@ProtoNumber(n)`.
- proto2 presence → nullable properties with `null` defaults (absent = null, present-with-default
  = non-null). Collections need explicit `emptyList()` defaults.
- Enums serialize **by ordinal** → model `status_code` as `Int?` (lossless, no unknown-value crash).
- Unknown fields are dropped on decode (opt-in preservation exists) — same effective behavior as
  protobuf-lite skipping them; adam never re-encodes.
- No "bytes consumed" API and no Source-based partial decode: a top-level protobuf message is not
  self-delimited, so no parser can stop at "message end" vs "next message start". protobuf-java's
  top-level `parseFrom(ByteBuffer)` has the identical ambiguity (it reads until EOF, merging any
  following bytes). The transformer already relies on the one-message-per-packet invariant (see its
  own comment about `serializedSize` mismatch), so "consumed = all buffered bytes on successful
  decode" is equivalent to today's behavior under that invariant — and the fragmentation edge cases
  (truncation mid-field → exception → wait for more input) behave identically.

### 2.4 Verdict

Feasible. io.grpc does not fully disappear (it is the JVM transport underneath kotlinx-rpc gRPC),
but the stub/codegen/protobuf-runtime layers are replaced by Kotlin-first code; the protoc pipeline
and protobuf-javalite as a *direct* dependency are removed entirely (protobuf-java remains
transitively at runtime — see §6, "protobuf-javalite is load-bearing"). Main trade-offs: pre-release
pin + extra repo for consumers, generated-package rename, `EmulatorGrpcRule` API break.

## 3. Implementation steps

1. Repos & catalog: add the `kxrpc-grpc` repo to `settings.gradle.kts` (pluginManagement +
   dependencyResolutionManagement, `FAIL_ON_PROJECT_REPOS` forbids project repos); catalog gains
   `kotlinxRpc` version, `kotlinx-rpc` plugin, `kotlinx-rpc-protobuf`/`kotlinx-rpc-grpc-core`/
   `kotlinx-rpc-grpc-client` libraries, `kotlin-serialization` plugin + `kotlinx-serialization-protobuf`,
   `grpc-netty` fallback entry; loses `protobufGradle`/`protobuf`/`grpcKotlin`,
   `protobuf-lite`/`grpc-protobuf-lite`/`grpc-kotlin-stub`, `javax-annotations`.
2. `adam/build.gradle.kts`: swap the protobuf plugin for `org.jetbrains.kotlinx.rpc.plugin` +
   serialization plugin; `rpc { protoc() }`; deps `api(kotlinx-rpc-protobuf, grpc-core, grpc-client)`,
   `implementation(kotlinx-serialization-protobuf, grpc-okhttp)`.
3. New glue `adam/src/main/kotlin/com/arthurkun21/adam/emulator/EmulatorGrpcClientFactory.kt`
   (client construction + `withService<EmulatorController>()` + shutdown idiom).
4. Port `EmulatorGrpcRule` (type of `grpc` → generated `EmulatorController`) and
   `EmulatorGrpcE2ETest` (`Empty { }`).
5. `@Serializable` instrumentation model in
   `adam/src/main/kotlin/com/arthurkun21/adam/instrumentation/InstrumentationData.kt`; rework
   `ProtoInstrumentationResponseTransformer` onto it; move `instrumentation-data.proto` out of
   `src/main/proto` (kept as wire-format reference under `docs/reference/`).
6. Docs: `AGENTS.md`, `docs/docs/emulator/emulator.md`, `docs/extensions/1-android-junit.md`
   (kotlinx-rpc snippets; extra-repo note for consumers).
7. Validate: `spotlessApply`, `:adam:test` (transformer fixture = parity gate; full ADB suite via
   server-stub), `build`, `:adam:integrationTest` (assumption-gated, needs an emulator).

## 4. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Pre-release artifacts from a separate repo required by consumers | Pin exact version; document in docs/; clean-break single commit = easy revert |
| OkHttp transport not picked up | Spike first; fallback `grpc-netty` (documented Netty re-entry) |
| Kotlin 2.4.10 ↔ compiler-plugin pairing | Build fails fast; align Kotlin version if required (fork precedent) |
| Explicit-API strict mode vs generated code | `explicitApiModeEnabled` codegen option; isolate/adjust if FIR-generated builders still violate |
| Hand-maintained field numbers in the `@Serializable` model | Real fragmented fixture test (`ProtoInstrumentationResponseTransformerTest`) is the parity gate; `.proto` kept as reference |
| Framing edge: packet coalescing lands mid-message at a field boundary | Behavior class unchanged vs today (both parsers merge/swallow); fixture test decides; fallback = keep protobuf-javalite for this proto only |
| `google.protobuf.Empty` becomes a generated plain message | Supported (WKT bundled); mechanical usage change |

## 5. Out of scope / follow-ups

- KMP targets (gRPC KMP is preview-only), server-side gRPC, gRPC-ktor-server.
- Renovate automation for kotlinx-rpc dev releases (custom registry datasource).
- Re-expose a non-generated facade type from `EmulatorGrpcRule` (beyond the required type swap).

## 6. Addendum (found during implementation)

- **`java_package` is respected** by the protoc-gen (contrary to the kotlinx-rpc limitations page):
  generated code lands in `com.android.emulator.control` / `com.android.commands.am`, same packages
  as the old grpc-java codegen, so message-type imports did not change.
- **Kotlin 2.4.10 pairs cleanly** with the kotlinx-rpc `0.11.0-grpc-189` Gradle + compiler plugins;
  no version bump was needed. Generated code carries explicit `public` modifiers, so strict
  explicit-API mode needed no workaround.
- **`grpc-okhttp` works as the gRPC-Java transport** under kotlinx-rpc (channel built via
  `ManagedChannelBuilder.forAddress` + service-loader provider discovery). Netty stays out of the
  tree; `EmulatorGrpcClientFactoryTest` guards the discovery (`No functional channel service
  provider found` if the transport goes missing).
- **API details that differ from the docs/samples**: `GrpcClient.awaitTermination` takes a
  `kotlin.time.Duration` (not `(Long, TimeUnit)`); the `Empty { }` builder is an extension
  (`import com.google.protobuf.kotlin.invoke`); `GrpcClientConfiguration` exposes no executor
  option, so `EmulatorGrpcRule`'s `coroutineDispatcher` constructor parameter was removed
  (breaking, accepted).
- **settings.gradle.kts repo content filter**: `includeGroup("org.jetbrains.kotlinx")` does not
  match the plugin-marker group `org.jetbrains.kotlinx.rpc.plugin` — use
  `includeGroupByRegex("org\\.jetbrains\\.kotlinx.*")` in both pluginManagement and
  dependencyResolutionManagement.
- **proto2 scalar defaults preserved**: `SessionStatus.statusCode`/`resultCode` and
  `TestStatus.resultCode` are modeled as non-null `Int = 0` (proto2 default) because the old
  protobuf-java getters returned 0 for absent fields and the transformer's branching relies on
  that; all other fields are nullable for presence.
- **Transformer framing**: `ProtoBuf.decodeFromByteArray` consumes the entire buffered payload, so
  the old `serializedSize` compact was replaced by resetting the buffer to empty on successful
  decode; a failed decode (`SerializationException`) keeps the bytes and waits for more input.
  One latent quirk improved: the old `testCount == 0` early-return left the buffer position at 0,
  so the next packet overwrote buffered bytes; the port appends instead.
- `instrumentation-data.proto` moved to `docs/reference/` as the wire-format reference; buf now
  generates only the emulator controller code.
- **`protobuf-javalite` is load-bearing at runtime — CI regression found & fixed** (correction of
  an interim attempt to exclude it, see the history below):
  - **Symptom**: `EmulatorGrpcE2ETest > testProto` failed on all CI emulator matrix jobs
    (API 24/34/35) with
    `java.lang.NoClassDefFoundError: com/google/protobuf/InvalidProtocolBufferException` on the
    first gRPC call; everything was green locally (compile + full unit suite).
  - **Root cause**: the kotlinx-rpc helper `checkForPlatformDecodeException` is an *inline*
    function whose body catches `com.google.protobuf.InvalidProtocolBufferException` (translating
    it into `ProtobufDecodingException`). Kotlin materializes the inline body into every generated
    `*Internal$MARSHALLER.decode` — all 27 marshallers in adam's jar carry the reference (54
    constant-pool hits). Verifying/executing those `decode` methods therefore requires protobuf-java
    on the runtime classpath.
  - **Why it slipped through every local gate**: the dependency is declared `runtime`-scope by
    `kotlinx-rpc-protobuf-lite`, so it never appears on the compile classpath — the generated code
    compiles without it, and no source file imports it. And no unit test executes a gRPC call:
    `GrpcClient` builds eagerly but connects/decodes lazily, so `EmulatorGrpcClientFactoryTest`
    (construct client + obtain proxy + shutdown) passes even with the class missing. Only a real
    RPC round-trip hits `decode`. Lesson: a "failed" code path behind a `catch` is still a
    classpath requirement; validate a dependency's load-bearing-ness by executing the code path
    that references it, not by absence of compile errors.
  - **Why the interim exclusion looked safe**: an early constant-pool scan concluded no
    kotlinx-rpc jar references protobuf-java. Two scan mistakes produced that false negative:
    (a) a whole-cache scan was a silent no-op because Gradle cache paths use the literal group
    name (`files-2.1/io.grpc/...`), not slash-separated groups; (b) kotlinx-rpc's own WKT runtime
    lives in `com.google.protobuf.kotlin`, which naive `com/google/protobuf` string matching
    reports as protobuf-java references — and dismissing those noisy hits led to over-trusting the
    scan. An exact, per-entry scan (python `zipfile`) found the 54 references in
    `*Internal$MARSHALLER.class` immediately.
  - **Fix**: the `exclude(group = "com.google.protobuf", module = "protobuf-javalite")` block was
    removed from `adam/build.gradle.kts`. Final dependency statement: protobuf-java is gone from
    the compile classpath and from adam's direct `api`/`implementation` deps; it remains only as
    kotlinx-rpc's transitive **runtime** dependency (`protobuf-javalite` 4.35.1 via
    `kotlinx-rpc-protobuf-lite`).
  - **Regression guard**: `GeneratedMessageMarshallerTest` round-trips a `VmRunState` through a
    generated marshaller (encode → decode), forcing the decode path to verify. Proven effective by
    temporarily re-applying the exclusion: the test fails with the exact CI error
    (`NoClassDefFoundError`) — without needing a live emulator.
  - Do **not** exclude `com.google.protobuf` artifacts from the kotlinx-rpc dependency tree.
- **sources jar collision**: kotlinx-rpc generates the messages file and the service file with
  identical relative paths (`com/android/emulator/control/EmulatorController.kt`) in the
  `kotlin-multiplatform` and `grpc-kotlin-multiplatform` source roots, which fails
  `:adam:sourcesJar` with a duplicate-entry error. Fixed by excluding the reproducible generated
  sources from the published sources jar (hand-written sources only); the classes jar ships all
  generated classes.
- Published sources jar note for consumers: generated `com.android.emulator.control` sources are
  not in the `-sources.jar` (regenerate locally via `rpc { protoc() }` if needed).

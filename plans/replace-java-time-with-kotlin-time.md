# Plan: Replace java.time with Kotlin stdlib time (+ kotlinx-datetime helpers)

Branch: `feat/kotlin-time` · Date: 2026-09-10

## 1. Goal

Replace every `java.time` type in the codebase with the native Kotlin equivalents:

- `java.time.Duration` → `kotlin.time.Duration` (stdlib, stable)
- `java.time.Instant` → `kotlin.time.Instant` (stdlib, stable since Kotlin 2.3.0; repo is on
  Kotlin 2.4.20, so no opt-in is required)
- `java.time.Clock` → `kotlin.time.Clock`
- `java.time.DateTimeFormatter` / `ZoneId` / `ZonedDateTime` → `kotlinx-datetime` 0.8.0
  (`LocalDateTime.Format { byUnicodePattern(...) }`, `TimeZone`, `Instant.toLocalDateTime`)

Approved decisions:

- **Clean break, no deprecated java.time overloads** ("convert all"). This changes the public API
  of the published `adam` artifact (explicit API mode): `AndroidDebugBridgeClientFactory`
  timeout vars, `FileEntry` timestamp fields, and the `LogcatSinceFormat` constructor params all
  change type — a semver-major change.
- The public API exposes **only stdlib types** (`kotlin.time.Instant`, `kotlin.time.Duration`);
  `kotlinx-datetime` is an `implementation`-scope helper used where the stdlib has no equivalent
  (date-time formatting, `TimeZone`), so consumers do not inherit the dependency.
- Non-`java.time` time APIs stay: `java.nio.file.attribute.FileTime` in `PullRequest` (JVM
  filesystem API, converted via `fromMillis`), `java.util.TimeZone`/`Calendar` in
  `LogcatE2ETest`'s log-line model (only the java.time bridging around them changes).

## 2. Research findings

### 2.1 Current state (all java.time usage)

Main sources (`adam`, public API surface):

| File | Usage |
|---|---|
| `AndroidDebugBridgeClientFactory.kt` | `idleTimeout`/`connectTimeout: java.time.Duration?` public vars (L31–32); converted with `.toMillis()` when building the default `KtorSocketFactory` (L39–40; Long-millis, defaults 30_000/10_000) |
| `request/sync/model/FileEntry.kt` | `FileEntry.mtime`, `FileEntryV1.mtime`, `FileEntryV2.atime/mtime/ctime: java.time.Instant` (L25, L45, L59–61); `exists()` checks `mtime.epochSecond == 0L` (L47, L64) |
| `request/logcat/LogcatSinceFormat.kt` | `Instant` ctor params on `DateString`/`DateStringYear`/`TimeStamp`; `DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")` / `("yyyy-MM-dd HH:mm:ss.SSS")` with `withZone(java.util.TimeZone.getTimeZone(tz).toZoneId())`; `instant.toEpochMilli()` |
| `request/logcat/SyncLogcatRequest.kt` | Vestigial `import java.time.Instant` (L21), no actual usage |
| `request/sync/v1/StatFileRequest.kt:50`, `v1/ListFileRequest.kt:54` | `Instant.ofEpochSecond(...)` from 4-byte ints |
| `request/sync/v2/{StatFileRequest,ListFileRequest}.kt` | `Instant.ofEpochSecond(...)` for atime/mtime/ctime from 8-byte longs |
| `request/sync/PullRequest.kt:184` | `Files.setLastModifiedTime(path, FileTime.from(Instant.ofEpochSecond(...)))` — java.nio `FileTime` bridging |

Test sources: `Instant.parse("...")`, `Instant.ofEpochSecond(n)`, `Instant.ofEpochMilli(n)` in
`AsyncLogcatRequestTest`, `SyncLogcatRequestTest`, sync v1/v2 `StatFileRequestTest`/
`ListFileRequestTest`, and the two `compat/` tests.

Integration tests: `LogcatE2ETest` (`Instant.now()`, `DateTimeFormatter` +
`ZoneId.systemDefault()` formatter at L49–50, `ZonedDateTime.ofInstant(date.toInstant(), ...)` at
L154; the `persist.sys.timezone` device property is an IANA zone ID, e.g. `America/New_York`);
`AdbDeviceRule` (`initTimeout: java.time.Duration = Duration.ofSeconds(30)`, L52).

Samples: `samples/desktopApp/.../ui/MainViewModel.kt` — converts `kotlin.time.Duration` →
`java.time.Duration` (`JavaDuration.ofMillis`, L56–57) just to feed the factory vars;
`Instant.now().minusSeconds(LOGCAT_LOOKBACK_SECONDS)` (L593); `ZoneId.systemDefault().id` (L594).

Docs: `docs/docs/logcat/logcat.md` example snippet uses `Instant.now().minusSeconds(60)`.

No `java.time` usage in `android-junit4*`, `android-testrunner-contract`, `androidx-screencapture`,
or `server/*`. No `java.util.Date` / `TimeUnit` usage anywhere. No `kotlinx-datetime` dependency
exists yet; `kotlin.time.Duration` is already used by `EmulatorGrpcClientFactory.kt`.

### 2.2 Target API facts (verified)

| Need | Stdlib / kotlinx-datetime 0.8.0 answer |
|---|---|
| Timestamps | `kotlin.time.Instant` — stable in Kotlin 2.3.0 (KT-80778; introduced experimental in 2.1.20). Companion: `fromEpochSeconds`, `fromEpochMilliseconds`, `parse`; members: `epochSeconds`, `toEpochMilliseconds()` |
| "Now" | `kotlin.time.Clock.System.now()` |
| Timeouts/durations | `kotlin.time.Duration` — `30.seconds`, `inWholeMilliseconds` |
| Zone from ID | `kotlinx.datetime.TimeZone.of(id)` (IANA IDs, `UTC`, `±hh:mm` offsets); `TimeZone.currentSystemDefault()`. Note: unlike `java.util.TimeZone`, no `GMT+08:00`-prefix or legacy 3-letter IDs → small normalization helper (see Step 4) |
| Instant → local wall clock | `instant.toLocalDateTime(timeZone)` (kotlinx-datetime extension on `kotlin.time.Instant`) — equivalent of `DateTimeFormatter.withZone(zone).format(instant)` for local-field patterns |
| Pattern formatting | `LocalDateTime.Format { byUnicodePattern("MM-dd HH:mm:ss.SSS") }` — `byUnicodePattern` added in kotlinx-datetime 0.7.0 |
| kotlinx-datetime version | 0.8.0 (May 2026); since 0.7.x its `Instant`/`Clock` are the stdlib types, so everything composes |

Fallback: if `byUnicodePattern` maps `SSS` (millis-of-second) in a way that breaks the exact
formatted output asserted by the existing tests, replace the pattern with the explicit
kotlinx-datetime field DSL (`monthNumber()`, `char(' ')`, `secondFraction(3)`, ...). The unit
tests assert full formatted strings, so any mismatch is caught.

### 2.3 Verdict

A like-for-like migration is feasible with output parity: durations and timestamps map 1:1 onto
stdlib types, and the only nontrivial part (pattern formatting with a zone) is covered by
kotlinx-datetime's `byUnicodePattern` + `toLocalDateTime`. Bonus: the desktop sample's
`kotlin.time.Duration → java.time.Duration` bridge disappears, and the
`java.util.TimeZone → ZoneId` bridge in `LogcatSinceFormat` is replaced by one stdlib-ecosystem
`TimeZone`.

## 3. Implementation steps

### Step 1 — Dependency wiring

- `gradle/libs.versions.toml`: add `kotlinxDatetime = "0.8.0"` and
  `kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }`.
- `adam/build.gradle.kts`: `implementation(libs.kotlinx.datetime)`.
- Sample desktop app build file: same `implementation` dependency (the sample formats time).
- If kotlinx-datetime 0.8.0's `TimeZone` typealias surfaces an experimental-stdlib opt-in error
  on Kotlin 2.4.20, add the required `-opt-in=...` to the compiler args in
  `build-logic/convention/.../JvmConventionPlugin.kt` (verify at compile time).

### Step 2 — Public API: factory + FileEntry

- `AndroidDebugBridgeClientFactory.kt`: `idleTimeout`/`connectTimeout` become
  `kotlin.time.Duration?`; `build()` passes `idleTimeout?.inWholeMilliseconds ?: 30_000` /
  `connectTimeout?.inWholeMilliseconds ?: 10_000`. `KtorSocketFactory`'s Long-millis plumbing
  stays as is.
- `FileEntry.kt`: all timestamp fields become `kotlin.time.Instant`; `exists()` uses
  `mtime.epochSeconds == 0L`.
- Sync v1/v2 requests (`StatFileRequest.kt`, `ListFileRequest.kt` ×2):
  `Instant.ofEpochSecond(...)` → `Instant.fromEpochSeconds(...)` (`kotlin.time.Instant`).

### Step 3 — PullRequest

- `PullRequest.kt:184`: `Files.setLastModifiedTime(path, FileTime.fromMillis(file.mtime.toEpochMilliseconds()))`
  (keep `java.nio.file.attribute.FileTime`).

### Step 4 — LogcatSinceFormat

```kotlin
private val sinceFormatter = LocalDateTime.Format { byUnicodePattern("MM-dd HH:mm:ss.SSS") }
private val sinceYearFormatter = LocalDateTime.Format { byUnicodePattern("yyyy-MM-dd HH:mm:ss.SSS") }

public sealed class LogcatSinceFormat(public val text: String) {
    public class DateString(instant: Instant, timezone: String) :
        LogcatSinceFormat("'${sinceFormatter.format(instant.toLocalDateTime(timeZone(timezone)))}'")
    // DateStringYear: same with sinceYearFormatter
    // TimeStamp: "${instant.toEpochMilliseconds()}.0"
}
```

- `Instant` = `kotlin.time.Instant`.
- Replace `java.util.TimeZone.getTimeZone(...).toZoneId()` with
  `kotlinx.datetime.TimeZone.of(...)` plus a small normalization helper: if the ID starts with
  `GMT`/`UTC` followed by an offset (e.g. `GMT+08:00`), strip the prefix before `TimeZone.of` so
  previously-accepted java.util-style IDs keep working; IANA IDs (what Android's
  `persist.sys.timezone` provides, and what the tests use) pass through unchanged.
- Remove the vestigial `java.time.Instant` import from `SyncLogcatRequest.kt`.

### Step 5 — Tests

- Unit tests (logcat ×2, sync v1/v2 ×4, compat ×2) — mechanical swaps:
  `Instant.ofEpochSecond(n)` → `Instant.fromEpochSeconds(n)`,
  `Instant.ofEpochMilli(n)` → `Instant.fromEpochMilliseconds(n)`,
  `Instant.parse("...")` → `kotlin.time.Instant.parse(...)`. Exact-string assertions on the
  formatted logcat output stay — they are the parity gate for the formatter swap.
- `LogcatE2ETest.kt`: `Instant.now()` → `Clock.System.now()`; the
  `DateTimeFormatter.ofPattern(...).withZone(ZoneId.systemDefault())` formatter →
  `LocalDateTime.Format { byUnicodePattern("MM-dd HH:mm:ss.SSS") }` +
  `TimeZone.currentSystemDefault()`; the `ZonedDateTime.ofInstant(date.toInstant(), ...)` (L154)
  conversion → `kotlin.time.Instant.fromEpochMilliseconds(date.time).toLocalDateTime(zone)`.
- `AdbDeviceRule.kt`: `initTimeout: Duration = 30.seconds` (kotlin.time); adjust its consumption
  site accordingly.

### Step 6 — Samples & docs

- `samples/desktopApp/.../ui/MainViewModel.kt`: delete the `JavaDuration` bridging (factory now
  takes kotlin.time directly); `Instant.now().minusSeconds(n)` →
  `Clock.System.now() - n.seconds`; `ZoneId.systemDefault().id` →
  `TimeZone.currentSystemDefault().id`.
- `docs/docs/logcat/logcat.md`: update the example
  (`since = Instant.now().minusSeconds(60)` → `Clock.System.now() - 60.seconds`); sweep the other
  doc pages for time API examples.

### Step 7 — Validation

- `./gradlew spotlessApply`
- `./gradlew :adam:test` — main parity gate (protocol suite + exact logcat format assertions)
- `./gradlew build`
- `./gradlew :adam:integrationTest` if a device/emulator is attached (requires local adb server).

## 4. Risks & mitigations

| Risk | Mitigation |
|---|---|
| `byUnicodePattern` output differs from `DateTimeFormatter` (e.g. `SSS` mapping, `yyyy` year-of-era vs proleptic) | Existing tests assert exact formatted strings; fallback to the explicit kotlinx-datetime field DSL for the offending field |
| `TimeZone.of` rejects java.util-style IDs (`GMT+08:00`, `PST`) that `LogcatSinceFormat` used to accept | Normalization helper strips `GMT`/`UTC` prefix; legacy 3-letter IDs were nonstandard in java.util too and are accepted as a behavior change only where java.util mapped them (out of scope) |
| Binary/API break for consumers of the published artifact | Intentional, approved clean break; call out in the PR description (semver-major) |
| Experimental-stdlib opt-in errors for `TimeZone` on Kotlin 2.4.20 | Add `-opt-in=...` to the JVM convention plugin compiler args if the compiler demands it |
| Integration-test-only formatting paths (E2E) can't be verified without a device | Keep the same pattern strings and logic shape; unit tests cover `LogcatSinceFormat` formatting parity |

## 5. Out of scope / follow-ups

- No Kotlin Multiplatform work (the codebase remains JVM-only; the types chosen are
  multiplatform-ready if that ever changes).
- No serializer for `kotlin.time.Instant` needed — nothing in the repo serializes these types
  (the `am instrument` protobuf models are unrelated).
- `AGENTS.md` unchanged (no java.time-specific guidance to update).

## 6. Addendum (found during implementation)

- **`UtcOffset` did not move to the stdlib**: `kotlin.time.UtcOffset` does not exist on Kotlin
  2.4.20 — in kotlinx-datetime 0.8.0 it is still `kotlinx.datetime.UtcOffset` (with the
  `asTimeZone()` extension). Only `Clock`/`Instant` were promoted to `kotlin.time`. No opt-in
  flags were needed anywhere (`TimeZone.of`, `byUnicodePattern`, `Clock.System.now()` all
  compile clean under explicit API mode).
- **`PullRequest` had two java.time touchpoints, not one** (compiler caught the second):
  the internal `SyncFile.mtime` is a `Long` of epoch-seconds, so the conversion chain is
  `FileEntry.mtime.epochSeconds` when building `SyncFile`, and
  `FileTime.fromMillis(file.mtime * 1000)` when restoring the local mtime after a pull
  (a first attempt used `.toEpochMilliseconds()` on the Long — nonsense caught at compile time).
- **`import kotlin.time.seconds` is wrong** — the extension lives at
  `kotlin.time.Duration.Companion.seconds`.
- **`docs/docs/logcat/logcat.md` was already stale**: it showed `since: Instant?` constructor
  params, but the actual API takes `LogcatSinceFormat?`. Corrected the signatures while updating
  the snippet to `Clock.System.now() - 60.seconds` + `LogcatSinceFormat.DateStringYear`.
- **Validation**: `:adam:test` green (exact-string logcat formatter assertions confirm
  `byUnicodePattern` output parity with the old `DateTimeFormatter`); `./gradlew build` green;
  integration tests run against a real wireless device — `LogcatE2ETest` (real `persist.sys.timezone`
  → `TimeZone.of` → `since` formatting → live logcat filtering → log-line re-parsing), `PullE2ETest`,
  `FileE2ETest`, `feature.StatV2E2ETest`, `feature.LsV2E2ETest` all pass.

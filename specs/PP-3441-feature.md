# PP-3441: Separate connect and read/write timeouts for a fast IPv6 to IPv4 fallback

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-3441
Related: PP-3504 (OkHttp 5 upgrade / Happy Eyeballs — the long-term fix, out of scope here)

## Problem

On a network where IPv6 routes are advertised but not functional (C24 saw it on
an emulator and on end-user devices), requests to `pay-api.gini.net` and
`user.gini.net` hang for a full **60 seconds** and then surface to the user as an
"unexpected error" (`EHOSTUNREACH` / connect timeout). Forcing IPv4 through a
custom `GiniHttpClientProvider` works around it.

Expected: a failed IPv6 connect attempt should give up quickly (10–15 s) so
OkHttp's route retry reaches the IPv4 address and the request succeeds, while
read/write of large document uploads keep their long timeout.

## Reproducing the problem

Cheapest faithful reproduction — a Robolectric unit test that inspects the
timeouts of the client the SDK builds by default:

`core-api-library/library/src/test/java/net/gini/android/core/api/http/DefaultGiniHttpClientProviderTest.kt`

```
./gradlew core-api-library:library:testDebugUnitTest \
  --tests "net.gini.android.core.api.http.DefaultGiniHttpClientProviderTest"
```

Before the fix, `the default client bounds the connect timeout separately from
the read and write timeouts` fails with:

```
value of: connectTimeoutMillis()
expected: 15000
but was : 60000
```

`an explicitly configured connect timeout applies to connect only` does not
compile before the change, because `setConnectTimeoutInMs` is new. The
deprecated-setter tests and the negative-timeout test pin pre-existing
behaviour.

The real-network manifestation (60 s stall on a black-holed IPv6 route) follows
mechanically from that configuration plus OkHttp 4's sequential route attempts
(see below); it is not reproduced in CI because it needs a broken network.

## Background

Three facts combine; the first two are ours, the third is OkHttp 4 behaviour.

1. **One value drives all three OkHttp timeouts.**
   `core-api-library/library/src/main/java/net/gini/android/core/api/http/DefaultGiniHttpClientProvider.kt:105-107`
   passes the single `connectionTimeoutInMs` to `connectTimeout`, `readTimeout`
   and `writeTimeout`. Its default is `DEFAULT_TIMEOUT_MS = 60_000` (line 271).
   OkHttp's own connect default is 10 s; 60 s is justified for read/write
   (multi-page document uploads on slow links) but not for a TCP connect.

2. **`GiniCoreAPIBuilder` always forwards its own 60 s default.**
   `core-api-library/library/src/main/java/net/gini/android/core/api/internal/GiniCoreAPIBuilder.kt:63`
   holds `mTimeoutInMs = 60_000` (raised from 2 500 ms in commit `bc6212d3d`,
   Dec 2022, PIA-3382 — for uploads) and line 516 calls
   `setConnectionTimeoutInMs(mTimeoutInMs)` unconditionally in
   `createDefaultOkHttpClient`. Every SDK reaches OkHttp through this path
   (`GiniBankAPIBuilder`, `GiniHealthAPIBuilder`,
   `GiniCaptureDefaultNetworkService.Builder` → bank-sdk, health-sdk,
   internal-payment-sdk, capture-sdk). So lowering the provider default alone
   would change nothing for integrators — the builder must stop overriding it
   when the integrator did not configure a timeout.

3. **OkHttp 4.12.0 tries resolved addresses one at a time** (no
   `fastFallback` / Happy Eyeballs — that arrived in OkHttp 5;
   `gradle/libs.versions.toml:70`). Android's resolver returns the IPv6 address
   first on a dual-stack host. The first attempt blocks for the full
   `connectTimeout`, i.e. 60 s, before `retryOnConnectionFailure` moves to the
   IPv4 route — by then the user-facing operation has long failed.

Symptom site vs. cause: the error surfaces in the SDK screens (bank-sdk /
health-sdk "unexpected error"), but the defect is the timeout configuration in
`core-api-library`. Fixing it there fixes all SDKs at once.

## Solution

Change in two modules: the timeout split itself in `core-api-library:library`,
plus new public setters in `capture-sdk:default-network` so capture-sdk and
bank-sdk integrators can reach both timeouts (item 3). Both modules gain public
API and both API dumps change; every other SDK picks the new defaults up
transitively without code changes.

Decision history for the existing connection-timeout setters:

- 2026-09-18: make `setConnectionTimeoutInMs` connect-only ("the name finally
  means what it says; no third setter").
- **2026-09-29, supersedes the above:** keep `setConnectionTimeoutInMs` (and the
  capture-sdk `setConnectionTimeout` / `setConnectionTimeoutUnit` pair) doing
  exactly what it always did — one value for connect, read and write — and
  deprecate it. Review of the migration note showed that a connect-only
  reinterpretation silently changes read/write for every integrator who passes a
  value other than 60 s, and `0` (no timeout anywhere) would have turned into a
  60 s read/write limit that aborts long uploads without any code change on the
  integrator's side. A new connect-only setter carries the fix instead. The cost
  is that integrators who already call the old setter keep their old connect
  timeout until they migrate; the deprecation message tells them how.

1. `DefaultGiniHttpClientProvider`
   - Hold two values instead of one: `connectTimeoutInMs` (new default
     **15 000 ms**) and `readWriteTimeoutInMs` (default 60 000 ms, unchanged).
   - `connectTimeout(connectTimeoutInMs)`, `readTimeout/writeTimeout(readWriteTimeoutInMs)`.
   - New public setter `Builder.setConnectTimeoutInMs(x)` for the connect
     timeout and `Builder.setReadWriteTimeoutInMs(x)` for read and write.
   - `Builder.setConnectionTimeoutInMs(x)` is `@Deprecated` and still sets all
     three. The two dedicated setters take precedence over it regardless of call
     order (resolved in `build()`: `connect ?: connection ?: default`, same for
     read/write), so the result never depends on ordering. No `ReplaceWith`,
     because a mechanical replacement with `setConnectTimeoutInMs` would drop
     read/write to 60 s.
   - KDoc (class header, example, setters) states the split defaults and the
     precedence rule.
   - The `private constructor` signature changes and two public methods are
     added → `./gradlew core-api-library:library:apiDump`. Nothing is removed;
     `setConnectionTimeoutInMs` keeps its signature and its behaviour.

2. `GiniCoreAPIBuilder`
   - `mTimeoutInMs` is replaced by `mConnectTimeoutInMs`, `mReadWriteTimeoutInMs`
     and `mConnectionTimeoutInMs` (all `Int?`, default `null`);
     `createDefaultOkHttpClient` forwards `connect ?: connection` and
     `readWrite ?: connection` to the provider's dedicated setters only when the
     integrator set something, so the provider's defaults reach every SDK.
   - New `open` setters `setConnectTimeoutInMs` and `setReadWriteTimeoutInMs`;
     `setConnectionTimeoutInMs` is `@Deprecated`, still `open`, still all three.
   - KDoc updated: when nothing is set, the defaults are 15 s connect / 60 s
     read & write.

3. `GiniCaptureDefaultNetworkService.Builder` (capture-sdk:default-network)
   - New `setConnectTimeout(timeout, unit)` and `setReadWriteTimeout(timeout,
     unit)`, single-call with the unit as second parameter (the read/write
     setter was first added on 2026-09-18 as a value/unit pair and reshaped on
     2026-09-29 before release). Both reject negative values up front and
     forward to `setConnectTimeoutInMs` / `setReadWriteTimeoutInMs`. The
     millisecond conversion saturates at `Int.MAX_VALUE` (~24.9 days) instead
     of wrapping to a negative value that the API builder would reject later in
     `build()`, far from the setter that caused it.
   - `setConnectionTimeout` / `setConnectionTimeoutUnit` are `@Deprecated`,
     still apply one value to all three, and lose to the dedicated setters
     regardless of call order. Their KDoc (which still described a read timeout
     with a backoff multiplier) is corrected.
   - Two `internal` resolvers, `connectTimeoutInMs()` and
     `readWriteTimeoutInMs()`, compute the forwarded values with the precedence
     applied; `build()` reads them and the unit test asserts them, since the
     built `OkHttpClient` is not reachable from this module.
   - `capture-sdk:default-network` API dump updated.

Why 15 s and not 10 s: OkHttp's connect timeout covers the TCP handshake only
(TLS runs under the read timeout), so 10 s would be enough on healthy networks;
15 s adds margin for congested mobile links while still turning the 60 s stall
into a ~15 s blip before the IPv4 retry. Either value satisfies the ticket
("~10–15 s"); see Open questions.

Public API impact: binary compatible — `setConnectTimeoutInMs` and
`setReadWriteTimeoutInMs` are added on `DefaultGiniHttpClientProvider.Builder`
and `GiniCoreAPIBuilder`, and `setConnectTimeout(Long, TimeUnit)` /
`setReadWriteTimeout(Long, TimeUnit)` on `GiniCaptureDefaultNetworkService.Builder`.
`setConnectionTimeoutInMs`, `setConnectionTimeout` and `setConnectionTimeoutUnit`
are deprecated but keep signature and behaviour. No public constructor or
method signature changed or was removed. The one changed line in the
`core-api-library:library` API dump is the synthetic bridge of
`DefaultGiniHttpClientProvider`'s `private constructor`, which gained an `I`
for the new read/write field; it is generated for the default-argument
constructor and only reachable from the `Builder`, so no signature an
integrator can call moved.

One **behavioural change**, needing a release-notes entry for every SDK that
ships the bumped `core-api-library`: integrators who never set a timeout get a
15 s connect timeout instead of 60 s (read and write stay 60 s). Integrators who
call the deprecated setters keep exactly their previous connect, read and write
timeouts — including the IPv6 stall, until they move to the dedicated setters.

Release-note bullets (draft). The API-library packages and the SDKs built
directly on `GiniCoreAPIBuilder` (`core-api-library`, `bank-api-library`,
`health-api-library`, `health-sdk`, `internal-payment-sdk`) expose the
`...InMs` setters; `capture-sdk`, `capture-sdk:default-network` and `bank-sdk`
integrators configure the network through
`GiniCaptureDefaultNetworkService.Builder`. Use the matching variant per
package:

For `core-api-library`, `bank-api-library`, `health-api-library`, `health-sdk`,
`internal-payment-sdk`:

> The default connect timeout is now 15 s (was 60 s) so that a failed IPv6
> connect falls back to IPv4 quickly; read and write timeouts stay at 60 s. The
> two are configured separately with the new `setConnectTimeoutInMs` and
> `setReadWriteTimeoutInMs`. `setConnectionTimeoutInMs` is deprecated: it keeps
> applying one value to connect, read and write, so if you call it nothing
> changes for you, but you also keep the slow IPv6 fallback until you switch to
> the new setters. The new setters take precedence over the deprecated one.

For `capture-sdk`, `capture-sdk:default-network`, `bank-sdk`:

> The default connect timeout is now 15 s (was 60 s) so that a failed IPv6
> connect falls back to IPv4 quickly; read and write timeouts stay at 60 s. The
> two are configured separately with the new
> `GiniCaptureDefaultNetworkService.Builder.setConnectTimeout(timeout, unit)`
> and `setReadWriteTimeout(timeout, unit)`. `setConnectionTimeout` /
> `setConnectionTimeoutUnit` are deprecated: they keep applying one value to
> connect, read and write, so if you call them nothing changes for you, but you
> also keep the slow IPv6 fallback until you switch to the new setters. The new
> setters take precedence over the deprecated pair.

## Test plan

Stack: JUnit4 + Robolectric (`AndroidJUnit4` runner) + Truth, matching
`GiniCoreAPIBuilderTest.kt` next door. No network needed.

- `core-api-library/library/src/test/java/net/gini/android/core/api/http/DefaultGiniHttpClientProviderTest.kt` (new, written as the reproduction)
  - default client: connect 15 000 / read 60 000 / write 60 000 — **fails before, passes after**
  - `setConnectTimeoutInMs(10 000)` only: connect 10 000, read & write stay 60 000
  - `setReadWriteTimeoutInMs(90 000)` only: connect stays 15 000, read & write 90 000
  - both together: connect 10 000, read & write 90 000
  - deprecated `setConnectionTimeoutInMs(10 000)`: all three 10 000 (pins the old behaviour)
  - precedence: dedicated setters win over the deprecated one even when it is called last; the deprecated value fills in whichever dedicated setter was not called
  - `0` for connect and read/write reaches OkHttp as `0` (OkHttp: no timeout)
  - negative timeout rejected by all three setters
- `core-api-library/library/src/test/java/net/gini/android/core/api/internal/GiniCoreAPIBuilderTest.kt` (extend)
  - default API client (no timeout configured) has connect 15 000 / read & write 60 000 — **fails before** (builder forwards 60 000), passes after
  - the same matrix as above through the client the API builder creates, proving the builder forwards each value and the precedence to the provider
- `capture-sdk/default-network/src/test/java/net/gini/android/capture/network/GiniCaptureDefaultNetworkServiceBuilderTest.kt` (extend)
  - `setConnectTimeout(30, SECONDS)` + `setReadWriteTimeout(2, MINUTES)` resolve to 30 000 / 120 000 (different units so a swapped field or dropped conversion fails)
  - both resolvers `null` until set; the deprecated pair resolves to both timeouts and is ignored until its unit is set
  - precedence in both directions; negative values rejected by both new setters
  - `0` resolves to `0`; `Long.MAX_VALUE` seconds and 30 days both saturate to `Int.MAX_VALUE` ms instead of turning negative

Verification: `/gini-check` for `core-api-library:library` expanded through
the dependency chain (health-api-library, bank-api-library, capture-sdk:default-network,
bank-sdk, health-sdk, internal-payment-sdk compile against it).

## Out of scope

- **OkHttp 5 / Happy Eyeballs** (`fastFallback`) — the real fix for dual-stack
  fallback (~250 ms instead of a connect timeout). Tracked in PP-3504.
- An IPv4-preferring `okhttp3.Dns` in the default provider — penalises healthy
  IPv6-first networks; superseded by PP-3504.
- The iOS counterpart of the new connect and read/write setters.
- Removing the AAAA record — backend/ops decision, not Android.
- The shared integration tests and three androidTests called
  `setConnectionTimeoutInMs(60000)`; they now call `setConnectTimeoutInMs(60000)`
  and get connect 60 s / read & write 60 s (the default), i.e. the same three
  effective values as before.

## Open questions

- Connect default: decided 2026-09-16 — **15 s**.
- Version bumps / release: `core-api-library` 3.6.0 → 3.6.1 plus dependent
  modules — handled with `/gini-release` after merge, not part of this fix.

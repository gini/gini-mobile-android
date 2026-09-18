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

`an explicitly configured connection timeout applies to connect only` also
fails before the change, because `setConnectionTimeoutInMs` used to set all
three timeouts. Only the negative-timeout test pins pre-existing behaviour.

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

Minimal, behind existing public API, in `core-api-library:library` only:

1. `DefaultGiniHttpClientProvider`
   - Hold two values instead of one: `connectTimeoutInMs` (new default
     **15 000 ms**) and `readWriteTimeoutInMs` (default 60 000 ms, unchanged).
   - `connectTimeout(connectTimeoutInMs)`, `readTimeout/writeTimeout(readWriteTimeoutInMs)`.
   - `Builder.setConnectionTimeoutInMs(x)` now sets the **connect timeout only**
     (decided 2026-09-18: the name finally means what it says; no third setter).
   - New public setter `Builder.setReadWriteTimeoutInMs(x)` for the read and
     write timeouts.
   - Update the KDoc (class header, `@param`, setters) to state the split defaults
     and that `setConnectionTimeoutInMs` no longer covers read/write.
   - The `private constructor` signature changes and one public method is added
     per builder → run `./gradlew core-api-library:library:apiDump`. Nothing is
     removed; `setConnectionTimeoutInMs` keeps its signature.

2. `GiniCoreAPIBuilder`
   - `mTimeoutInMs` is replaced by `mConnectTimeoutInMs: Int?` and
     `mReadWriteTimeoutInMs: Int?`, both defaulting to `null`;
     `createDefaultOkHttpClient` forwards each only when the integrator set it.
   - `setConnectionTimeoutInMs` becomes connect-only; new `open` setter
     `setReadWriteTimeoutInMs`.
   - KDoc updated: when nothing is set, the defaults are 15 s connect / 60 s
     read & write.

3. `GiniCaptureDefaultNetworkService.Builder.setConnectionTimeout` (capture-sdk:
   default-network) forwards to `setConnectionTimeoutInMs` and therefore becomes
   connect-only as well. Its KDoc (which still described a read timeout with a
   backoff multiplier) is corrected. New `setReadWriteTimeout` /
   `setReadWriteTimeoutUnit` mirror the connect timeout pair and forward to
   `setReadWriteTimeoutInMs`, so capture-sdk and bank-sdk integrators keep a
   timeout knob for slow uploads without a custom `GiniHttpClientProvider`
   (added after review on 2026-09-18; `capture-sdk:default-network` API dump
   updated).

Why 15 s and not 10 s: OkHttp's connect timeout covers the TCP handshake only
(TLS runs under the read timeout), so 10 s would be enough on healthy networks;
15 s adds margin for congested mobile links while still turning the 60 s stall
into a ~15 s blip before the IPv4 retry. Either value satisfies the ticket
("~10–15 s"); see Open questions.

Public API impact: binary compatible — `setReadWriteTimeoutInMs` is added on
`DefaultGiniHttpClientProvider.Builder` and `GiniCoreAPIBuilder`, and
`setReadWriteTimeout` / `setReadWriteTimeoutUnit` on
`GiniCaptureDefaultNetworkService.Builder`; no signature removed or changed. Two **behavioural changes**, both needing a release-notes
entry for every SDK that ships the bumped `core-api-library`:

- Integrators who never set a timeout: connect attempts now fail after 15 s
  instead of 60 s.
- Integrators who call `setConnectionTimeoutInMs` (or the capture-sdk
  `setConnectionTimeout`): it now bounds only the connect phase. Read and write
  stay at the 60 s default unless `setReadWriteTimeoutInMs` is called. Only
  integrators who had raised the value above 60 s for slow uploads are
  affected; they must now call `setReadWriteTimeoutInMs` with that value.

Release-note bullet (draft, for each affected package):

> `setConnectionTimeoutInMs` now sets only the connect timeout (default 15 s, was
> 60 s) so that a failed IPv6 connect falls back to IPv4 quickly. Read and write
> timeouts are configured separately with the new `setReadWriteTimeoutInMs`
> (default 60 s, unchanged). If you raised the connection timeout above 60 s for
> large uploads, set `setReadWriteTimeoutInMs` to that value as well.

## Test plan

Stack: JUnit4 + Robolectric (`AndroidJUnit4` runner) + Truth, matching
`GiniCoreAPIBuilderTest.kt` next door. No network needed.

- `core-api-library/library/src/test/java/net/gini/android/core/api/http/DefaultGiniHttpClientProviderTest.kt` (new, written as the reproduction)
  - default client: connect 15 000 / read 60 000 / write 60 000 — **fails before, passes after**
  - `setConnectionTimeoutInMs(10 000)` only: connect 10 000, read & write stay 60 000 (**fails before**: used to set all three)
  - `setReadWriteTimeoutInMs(90 000)` only: connect stays 15 000, read & write 90 000
  - both together: connect 10 000, read & write 90 000
  - negative timeout rejected by both setters
- `core-api-library/library/src/test/java/net/gini/android/core/api/internal/GiniCoreAPIBuilderTest.kt` (extend)
  - default API client (no timeout configured) has connect 15 000 / read & write 60 000 — **fails before** (builder forwards 60 000), passes after
  - `setConnectionTimeoutInMs(10 000)` / `setReadWriteTimeoutInMs(90 000)` on the builder each reach only their OkHttp timeout(s), and both together reach the default client
  - negative timeout rejected by both setters

Verification: `/gini-check` for `core-api-library:library` expanded through
the dependency chain (health-api-library, bank-api-library, capture-sdk:default-network,
bank-sdk, health-sdk, internal-payment-sdk compile against it).

## Out of scope

- **OkHttp 5 / Happy Eyeballs** (`fastFallback`) — the real fix for dual-stack
  fallback (~250 ms instead of a connect timeout). Tracked in PP-3504.
- An IPv4-preferring `okhttp3.Dns` in the default provider — penalises healthy
  IPv6-first networks; superseded by PP-3504.
- The iOS counterpart of the new read/write setters.
- Removing the AAAA record — backend/ops decision, not Android.
- The shared integration tests call `setConnectionTimeoutInMs(60000)` explicitly;
  they now get connect 60 s / read & write 60 s (the default), i.e. the same
  three values as before, and are left as is.

## Open questions

- Connect default: decided 2026-09-16 — **15 s**.
- Version bumps / release: `core-api-library` 3.6.0 → 3.6.1 plus dependent
  modules — handled with `/gini-release` after merge, not part of this fix.

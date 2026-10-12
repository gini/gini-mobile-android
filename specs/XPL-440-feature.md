# XPL-440 — `concurrency-specialist` agent

Status: implemented
Ticket: https://ginis.atlassian.net/browse/XPL-440 (parent XPL-402 — [Android] Add 7 specialist agents)

- Source: Confluence "AI Planning for 2026" (PLMO), September 2026 section:
  *"Checks coroutines and Flow for dispatcher, cancellation and main-thread mistakes."*
- Rule from that page: **every agent must be valid for our repo. Generic Kotlin advice is not enough.**
- iOS twin: `concurrency-specialist` in `gini-mobile-ios` (XPL-407, PR gini/gini-mobile-ios#1274).

## 1. Goal

Add one agent file, `.claude/agents/concurrency-specialist.md`. It reviews coroutines, Flow and
the legacy Java threading in capture-sdk. It looks for **real** problems:

- the main thread blocked
- data races
- scopes that leak or never get cancelled
- cancellation that is lost or swallowed
- callback bridges that hang
- Flow collected outside the lifecycle

It also checks that coroutine tests are deterministic.

The agent **owns** `.claude/rules/coroutines-flow.md`, the same way the iOS agent owns the
Concurrency section of `mandatory-rules.md`. This PR also fixes that file.

Then wire the agent into `gini-orchestrator`, `gini-task` and the prompt hook, so it is picked
for the right tasks.

## 2. Decisions (from the clarifying questions, 2026-10-12)

| Question | Decision |
|---|---|
| File name | `concurrency-specialist` — same as the wiki and iOS. The Jira title keeps the `android-` prefix only to tell the tickets apart. |
| `coroutines-flow.md` | The agent owns it. This PR fixes the rules that are wrong and adds the missing ones. |
| Branch | `XPL-440-concurrency-specialist`, **stacked on `XPL-437-android-security-specialist`** (changed 2026-10-12 to avoid conflicts in the orchestrator, `gini-task.md` and the hook). Still one PR for this agent: its base is the XPL-437 branch until that PR merges, then `main`. |
| `InjectDispatcher` and the other detekt coroutine rules | All need type resolution (detekt docs). CI and `/gini-check` run plain `detekt`, so these rules never fire. The agent must not say "detekt will catch it". |
| Main thread blocked | `concurrency-specialist` owns blocking calls on main. `performance-specialist` (XPL-442) will own rendering, bitmaps, memory and recomposition. |
| Bugs found in the survey | Listed in section 9. No code fixes in this PR. The user decides on tickets. |
| Tools | `Read, Edit, Write, Glob, Grep` — same as the iOS twin and the security agent (XPL-402 decision). |

## 3. What we took from each source

| Source | Took | Rejected (and why) |
|---|---|---|
| iOS `concurrency-specialist` | Section order. "A race claim must name the two execution contexts." "Do not flag the old style just for being old." Labelled findings with severity. Owns the shared rules file. | Swift-only content. |
| Review of iOS PR 1274 | (1) Never recommend an API that does not compile or does not exist in our versions. (2) A weak reference is not teardown — long work needs an explicit cancel. (3) Value-type safety claims need evidence. | — |
| new-silvermoon `kotlin-concurrency-expert` | Triage first, then smallest fix. `launchWhenX` → `repeatOnLifecycle`. `ensureActive()` in loops. | "Inject an application scope" — an SDK does not own the `Application`. |
| new-silvermoon `android-coroutines` | Main-safe data layer. `callbackFlow` must end in `awaitClose`. `runCatching` must rethrow cancellation. | "Collect only in `lifecycleScope`" — Fragments need `viewLifecycleOwner`. |
| Google `android/skills` | No skill covers coroutines. `camerax`: camera callbacks are on background executors → hop to main. `android-profiler`: measure jank, don't guess. | `testing-setup` assumes Hilt and puts UI tests in `test/` — conflicts with our Koin + `androidTest` + Orchestrator. |
| piyushverma0 `kotlin-patterns` | `MutableStateFlow.update {}` for read-modify-write. `CoroutineExceptionHandler` does nothing on `async`. No `runBlocking` in production. | Several wrong claims (see 3a). Hilt qualifiers. |
| AlexGladkov `diagnostics-kotlin` | "SharedFlow with replay 0 drops events when nobody collects." | KMP/Decompose content. Its sample catches `Exception` and swallows cancellation. |
| 0xfurai, VoltAgent | Nothing concrete. | Generic; 0xfurai suggests LiveData. |
| developer.android.com + kotlinlang | The rule wording to cite (URLs in section 6). | — |

### 3a. Wrong claims we must NOT copy into the agent

From the community skills. Each one is false:

- `viewModelScope` is cancelled on process death, or leaks on rotation.
- `combine` emits before every input has emitted once.
- `Flow.flatMap` exists.
- CPU work belongs on `Dispatchers.IO`.
- `Dispatchers.IO` has a fixed 64 threads. (It is max(64, cores).)
- A try/catch around `await()` inside `coroutineScope` gives a fallback. (The scope is still cancelled.)
- `TestCoroutineDispatcher` / `runBlockingTest`. (Deprecated.)

## 4. What the code really does (verified on main, 2026-10-12)

Paths are real. The agent must not hard-code counts — it cites the `grep` instead (see the memory
note on skill docs and counts).

### Versions (`gradle/libs.versions.toml`)
kotlinx-coroutines 1.9.0, lifecycle 2.8.7, Orbit 9.0.0, Koin 4.0.2, Turbine 0.12.3. No
`lifecycle-runtime-compose`, so **`collectAsStateWithLifecycle()` is not available** in the SDKs
today. The current rules file asks for it anyway — that is wrong (iOS lesson 1).

### Dispatchers
- Hardcoded `Dispatchers.IO`/`Main` in many places, for example:
  - the API builders: `UserRemoteSource(Dispatchers.IO, …)`
  - capture-sdk extensions
  - `GiniHealth`
  - `PaymentComponentView`
- `CoroutineDispatcher` is injected in only a few classes, for example `LastAnalyzedDocumentProvider` and `AttachedToTransactionDocumentProviderImpl`. Other classes take a `CoroutineContext` default parameter (`GiniCaptureDefaultNetworkService`).
- **There are no named Koin dispatcher bindings.** Koin modules pass `Dispatchers.IO`/`Main` as plain values (`capture-sdk` `di/ProviderModule.kt`, `bank-sdk` `CaptureSdkDiBridge.kt`, `SkontoScreenModule.kt`).

### Scopes
- No `GlobalScope`. No `MainScope()`.
- Ad-hoc `CoroutineScope(...)` in capture-sdk, bank-sdk, health-sdk and internal-payment-sdk. Some are cancelled (`AnalysisScreenPresenterExtension`, `GiniCaptureDefaultNetworkService.cleanup()`). Some never are (`LastAnalyzedDocumentProvider`, `CameraFragmentExtension`, `AmplitudeUserAnalyticsEventTracker`, `GiniHealth` constructor).

### `runBlocking` in SDK `src/main` (all of them)
| File | Verdict for the agent |
|---|---|
| `core-api-library` `http/GiniSessionInterceptor.kt` | **Deliberate.** OkHttp interceptors are synchronous and run on OkHttp threads. Do not flag. |
| `capture-sdk` `analysis/AnalysisScreenPresenterExtension.kt` | Main thread + DataStore + Mutex. Hotspot. |
| `capture-sdk` `camera/CameraFragmentExtension.kt` | Main thread + DataStore + Mutex unlocked in a UI callback. Hotspot. |
| `capture-sdk` `util/SAFHelper.kt` | Blocking file copy, called from `AnalysisFragmentImpl` (UI). Hotspot. |
| `bank-sdk` `capture/CaptureFlowFragment.kt`, `capture/digitalinvoice/DigitalInvoiceFragment.kt` | Main thread DataStore read. Hotspot. |

### Cancellation
- `core-api-library` `Resource.kt` `wrapInResource` catches `java.util.concurrent.CancellationException` (the supertype of the coroutines one) and returns `Resource.Cancelled`. This is **public behaviour** — `Resource.Cancelled` is part of the API. The agent must treat it as a known design: do not flag existing callers, do not copy the pattern into new code, and never "fix" it without a ticket (breaking change).
- Many `catch (e: Exception|Throwable)` and `runCatching` around suspend calls (internal-payment-sdk `FlowBottomSheetsManager`, health-sdk `PaymentFlowViewModel`, capture-sdk extensions).
- `CoroutineExceptionHandler`: none.

### Flow and state
- `StateFlow` + `MutableSharedFlow` in capture/health/internal-payment. Orbit-MVI containers in bank-sdk (`SkontoFragmentViewModel`, `DigitalInvoiceSkontoViewModel`, `InvoicePreviewViewModel`, `TransactionDocInvoicePreviewViewModel`).
- No `Channel`, `callbackFlow`, `stateIn`, `shareIn`, `flowOn` in the SDKs.
- `repeatOnLifecycle` is used in health/internal-payment Fragments and in capture-sdk `util/compose/FlowExt.kt` (`collectFlow`). Orbit's own `collectAsState` is lifecycle-aware. One plain `androidx.compose.runtime.collectAsState` in bank-sdk `TransactionDocs.kt`.

### Callback bridges
- `suspendCancellableCoroutine` in bank-sdk `invoice/network/*NetworkService.kt`.
- Non-cancellable `suspendCoroutine` in bank-sdk `invoice/image/InvoicePreviewPageImageProcessor.kt`.
- ML Kit QR is bridged with the **blocking** `Tasks.await` on a `HandlerThread` (`QRCodeDetectorTaskMLKit.java`). ML Kit text recognition uses listeners with no executor (main thread) in `MLKitTextRecognizer.kt`.
- Java-facing wrappers: bank-sdk `util/CoroutineContinuationHelper.kt` (`Dispatchers.Unconfined` + `Handler`), capture-sdk default-network `launchCancellable` + `CancellationToken`.

### Legacy Java threading (capture-sdk)
- `AsyncTask` in many files.
- `HandlerThread` in `QRCodeDetectorImpl`.
- `UIExecutor`.
- `synchronized` on photo and document classes.
- `AtomicInteger` frame ref-count in `CameraFragmentImpl`.
- `ParcelableMemoryCache` (static main-looper `Handler` + `ConcurrentHashMap`).

`CompletableFuture` is the **jersey jsr166e backport**, not `java.util.concurrent` — it is not a
minSdk 23 problem. CameraX `ImageAnalysis.setAnalyzer` uses `ContextCompat.getMainExecutor`, so
frame analysis runs **on the main thread** today.

### Tooling
- Detekt `coroutines` rule set:
  - on: `InjectDispatcher`, `RedundantSuspendModifier`, `SleepInsteadOfDelay`, `SuspendFunWithFlowReturnType`
  - off: `GlobalCoroutineUsage`, `SuspendFunSwallowedCancellation`, `SuspendFunWithCoroutineScopeReceiver`
- `CodeAnalysisPlugin` sets `ignoreFailures` for most modules. Detekt only gates `capture-sdk:sdk`, `bank-sdk:sdk` and `bank-sdk:example-app`.
- **Unverified:** whether `InjectDispatcher` needs type resolution (CI runs plain `detekt`). Check before the agent claims "detekt will catch it".
- No StrictMode. Almost no `@MainThread`/`@WorkerThread` annotations.

### Tests
- `runTest` is common. Main-dispatcher rules:
  - bank-sdk `MainDispatcherRule.kt`
  - health-sdk and internal-payment-sdk `ViewModelTestCoroutineRule.kt` — these call `resetMain` without `try/finally`
  - capture-sdk calls `setMain` inline
- `Thread.sleep` / `CountDownLatch` / `runBlocking` appear in some tests (`GiniSessionInterceptorTest`, `AnalysisScreenPresenterTest`, the URI import tests).
- Turbine is used only in health-sdk and internal-payment-sdk.

## 5. Agent design — `.claude/agents/concurrency-specialist.md`

Same section order as the iOS twin and `android-security-specialist`. No line numbers anywhere.

1. **Frontmatter** — `name`, a `description` that names coroutines, Flow, dispatchers, cancellation, main-thread, legacy Java threading and test determinism; `tools: Read, Edit, Write, Glob, Grep`.
2. **Knowledge Source — REQUIRED FIRST.** Read `.claude/rules/coroutines-flow.md`. You own it. When a rule is wrong or missing, propose the edit to that file, not to your own instructions. `AGENTS.md` wins on conflict.
3. **The most important fact — we ship SDKs, not an app.**
   - No `Application` scope to inject.
   - Long-lived work needs an SDK-owned scope that the facade cancels (`cleanup()` / `release()`).
   - Integrator callbacks must arrive on the thread the KDoc promises (main, unless stated otherwise).
   - Integrators may call us from Java — a public `suspend` fun needs a Java path or a stated reason.
4. **Repo Context.** The facts in section 4, written as patterns plus the `grep` to re-check them, not counts. Include the list of "deliberate, do not flag" items:
   - `GiniSessionInterceptor`'s `runBlocking`
   - `Resource.Cancelled`
   - Orbit `intent/reduce/postSideEffect`
   - `MutableSharedFlow` events
   - the legacy Java threading style
5. **What You Review** (each item says what is a finding and what is not):
   1. **Main thread blocked** — `runBlocking`, `Tasks.await`, DataStore `first()`, file/bitmap/PDF work, `Thread.sleep` on main. The most common real finding.
   2. **Data races** — shared mutable state touched from two threads with no confinement, lock, atomic, `Mutex` or `update {}`. **The claim must name both threads.**
   3. **Scope ownership and leaks** — every `CoroutineScope(...)` has an owner that cancels it. No fire-and-forget scope in a singleton, a View or a Koin `single {}`. Long view-bound work is cancelled on teardown, not just weakly referenced.
   4. **Dispatchers** — injected for new code (constructor parameter, defaulted is fine; Koin: plain value today, a named binding only if the module adds one). Main-safe suspend funs. CPU on `Default`, blocking I/O on `IO`. Hardcoded dispatchers in existing code are not a finding.
   5. **Cancellation** — never swallow `CancellationException`: `catch (Exception|Throwable)` and `runCatching` around suspend calls must rethrow it. `ensureActive()`/`yield()` in CPU loops. Cleanup in `finally` + `withContext(NonCancellable)`.
   6. **Structured concurrency and errors** — `coroutineScope` vs `supervisorScope`. Every `async` is awaited. `CoroutineExceptionHandler` does nothing on `async`/children.
   7. **Callback bridges** — `suspendCancellableCoroutine` with `invokeOnCancellation`, resumed exactly once on every path. `callbackFlow` + `trySend` + `awaitClose`. Prefer these over `suspendCoroutine` and the blocking `Tasks.await` in new code.
   8. **Lifecycle-aware collection** — Fragments: `viewLifecycleOwner.lifecycleScope` + `repeatOnLifecycle(STARTED)`. Compose: Orbit `collectAsState`, the capture-sdk `collectFlow` helper, or `collectAsStateWithLifecycle` **only if** the module adds `lifecycle-runtime-compose`.
   9. **Flow semantics** — private mutable / public read-only. `update {}` for read-modify-write. `SharedFlow` replay/buffer chosen on purpose (replay 0 drops events with no collector). `flowOn` not `withContext` inside `flow {}`. `combine` vs `zip`, `flatMapLatest`.
   10. **Orbit-MVI (bank-sdk)** — state read before a suspension inside `intent {}` can be stale in `reduce {}`; reduce from `state` inside `reduce`.
   11. **Legacy Java threading (capture-sdk)** — review, never convert: `AsyncTask` cancellation, `HandlerThread` message drops, `volatile` on fields shared with a worker thread, check-then-act on atomics, `synchronized` scope.
   12. **Camera and ML Kit threads** — which executor a callback runs on, frame release on every path (including errors), scanner closed while a detection is in flight.
   13. **Public API threading** — callback thread documented and honoured; Java callers can cancel; no `Unconfined` surprises.
   14. **Test determinism** — `runTest`, one shared scheduler, Main rule with `resetMain` in `finally`, `backgroundScope` for endless collectors, no `Thread.sleep`/`runBlocking`/`CountDownLatch` for coroutine timing. Defer the full testing review to `testing-specialist`.
6. **What NOT to Flag** — the "deliberate" list, plus:
   - hardcoded dispatchers in untouched code
   - missing `Channel`/`stateIn` in existing code
   - asking to convert Java to coroutines
   - asking for `collectAsStateWithLifecycle` without the dependency
   - asking for StrictMode in an SDK (the host app owns it)
   - the wrong claims in section 3a
   - pre-existing issues on lines the diff did not touch, unless a full audit was asked for
7. **Review Checklist** — one line per review item.
8. **Review Process** — triage → smallest safe fix → verify. **Ask the user first** when the fix:
   - changes public API or the callback thread
   - changes `Resource` / cancellation semantics
   - touches `GiniSessionInterceptor`/session code
   - touches the camera pipeline
   - fixes something the diff did not introduce

   A full audit with no diff is report-only. Never commit. Cannot run Gradle → tell the user to run `/gini-check` (and `/gini-connected-check` for core-api `http/` or camera changes).
9. **Output Format** — group by file, `file:line`, rule quoted from `coroutines-flow.md`, `before` → `after`. Closing summary ranked, labelled by type (Main-Thread, Data Race, Scope Leak, Dispatcher, Cancellation, Bridge, Lifecycle, Flow, Orbit, Java Threading, Public API, Test) with severity (blocker / warning / nit).

## 6. Rules file changes — `.claude/rules/coroutines-flow.md`

- Header: owned by `concurrency-specialist`; the other agents read it.
- **Fix:** "bind named dispatchers in Koin" → "inject the dispatcher (constructor parameter with a default is fine). The Koin modules pass plain `Dispatchers.*` values today; add a named binding only together with its first consumer."
- **Fix:** "repos/use-cases never launch" — keep the rule, but name the exceptions that exist (`LastAnalyzedDocumentProvider`, `AttachedToTransactionDocumentProviderImpl`) as "existing, do not copy".
- **Fix:** `collectAsStateWithLifecycle()` is only valid once a module adds `lifecycle-runtime-compose`. Until then: Orbit `collectAsState`, or capture-sdk `collectFlow`.
- **Add:** no `runBlocking` in SDK code except a synchronous third-party contract (OkHttp interceptor).
- **Add:** `Resource.Cancelled` is the public cancellation outcome; new code still rethrows `CancellationException`.
- **Add:** SDK scope rule (no application scope; the facade cancels its own scope).
- **Add:** integrator callbacks on the documented thread.
- **Add:** legacy Java threading is reviewed in place, never converted.
- **Add:** a short "Sources" line with the official URLs:
  - https://developer.android.com/kotlin/coroutines/coroutines-best-practices
  - https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
  - https://developer.android.com/kotlin/coroutines/test
  - https://kotlinlang.org/docs/exception-handling.html
  - https://kotlinlang.org/docs/cancellation-and-timeouts.html

Keep it short. The compose, views and testing agents already point at this file, so they need no edit
unless their own text contradicts the new rules. Check `compose-specialist`'s "never `collectAsState()`" —
it must allow Orbit's `collectAsState`.

## 7. Wiring

| File | Change |
|---|---|
| `.claude/agents/gini-orchestrator.md` | Add `concurrency` to the description. Add a team-table row. Add a delegation rule: invoke for diffs that touch `CoroutineScope`, `launch`/`async`, `runBlocking`, `withContext`, `Dispatchers`, `Flow`/`StateFlow`/`SharedFlow`, `suspendCancellableCoroutine`, `Mutex`, `synchronized`, `AsyncTask`, `Handler`/`HandlerThread`, `CompletableFuture`, camera/ML Kit callbacks, Orbit intents, or coroutine tests. Remove "coroutines/concurrency" from the "enforce inline" rule. Update the line about `coroutines-flow.md` ownership. |
| `.claude/commands/gini-task.md` | Add `concurrency` to the specialist list. |
| `.claude/settings.json` | Add `concurrency` to the agent list in the `UserPromptSubmit` hook text. |

The XPL-437 PR edits the same three lines. Expect a merge conflict; keep both entries.

## 8. Test plan (agent files have no unit tests)

1. Frontmatter parses: `name`, `description`, `tools` present; file name matches `name`.
2. `grep -n ':[0-9]' .claude/agents/concurrency-specialist.md` finds no `file:line` citations.
3. Every class and path named in the agent exists on `main` (`grep -rl`).
4. **Dry run on known hotspots.** The agent should flag each of these:
   - `CameraFragmentExtension.kt`: main-thread `runBlocking`, and the integrator callback on IO
   - `AmplitudeUserAnalyticsEventTracker.kt`: race on `events`
   - `LastAnalyzedDocumentProvider.kt`: `clear()`/`update()` can be reordered
5. **False-positive check.** The agent must NOT flag `GiniSessionInterceptor.kt`, `Resource.kt`, or an Orbit container's `postSideEffect`.
6. Run the orchestrator with a coroutine-only diff and confirm it picks `concurrency-specialist` (and `testing-specialist` when tests change).
7. Optional: `/code-review` on the branch before the PR.

### Not tested
How the agent behaves across many sessions. Nothing measures agent quality yet (wiki Q4 item 28).

## 9. Hotspots found (for tickets — not fixed here)

Ranked. Each one needs a ticket decision by the user.

1. `runBlocking` on the main thread with DataStore reads and a `Mutex` locked/unlocked across UI callbacks — capture-sdk `AnalysisScreenPresenterExtension.kt`, `CameraFragmentExtension.kt`. ANR risk; double unlock throws.
2. Public `onExtractionsAvailable` called on `Dispatchers.IO` from an unowned scope — `CameraFragmentExtension.kt`.
3. `AmplitudeUserAnalyticsEventTracker`: `events` list touched from main and IO (data race). Each `setPlatformTokens` starts a new endless loop that is never cancelled.
4. `runBlocking` DataStore read on main — bank-sdk `CaptureFlowFragment.kt`, `DigitalInvoiceFragment.kt`.
5. `SAFHelper.saveFilesToFolder` blocks the UI thread with file copies.
6. `LastAnalyzedDocumentProvider` / `AttachedToTransactionDocumentProviderImpl`: `update`/`clear` launched separately on IO can be reordered; scopes are never cancelled.
7. Camera frame ref-count check-then-act in `CameraFragmentImpl`; QR error path never releases the frame (`QRCodeDetectorHandler.java`).
8. Session code in `core-api-library` `authorization/`: the threading needs a closer look together with `android-security-specialist`. Not confirmed; no details here until it is investigated.
9. `GiniHealth` constructor launches two unowned scopes.
10. Tests: `ViewModelTestCoroutineRule` `resetMain` without `finally` (health-sdk, internal-payment-sdk).

Items 1–7 and 9 were checked in this session by reading the code. Item 8 is a suspicion: it needs a
reproduction.

Confirmed by the agent's dry run (2026-10-12), with more detail:
- Item 2 is worse than listed. `onExtractionsAvailable` runs on IO while holding the `Mutex`, and it forwards to the public `GiniCaptureFragmentListener.onFinishedWithResult`. In bank-sdk, `CaptureFlowFragment` then touches `requestedOrientation` and `composeView.setContent {}` on that IO thread. **Blocker.**
- The education `Mutex` is unlocked from a Compose `LaunchedEffect`. If that effect is cancelled or throws, the `Mutex` never unlocks, and every QR result waits forever.
- Item 3: `setPlatformTokens` runs on every `GiniCaptureFragment.onCreate`, so each recreation leaks one more 5-second loop. `BufferedUserAnalyticsEventTracker` queues are also touched from main and a jsr166e worker.
- bank-sdk `ProceedClickedIntent` calls `reduce` from a state snapshot taken before a DataStore suspension. Hard to hit. Warning.

## 10. Steps and commits

On branch `XPL-440-concurrency-specialist`, stacked on the XPL-437 branch. Commit format `ai: <subject>` + `XPL-440`.

1. `ai: Add concurrency-specialist agent` — the agent file.
2. `ai: Make concurrency-specialist own the coroutines-flow rules` — rules file fixes (+ compose-specialist line if needed).
3. `ai: Route concurrency work to concurrency-specialist` — orchestrator, `gini-task.md`, hook.

No commit without the user's OK (memory rule). PR uses `.github/pull_request_template.md`.

## Out of scope

- Fixing any hotspot in section 9.
- Turning on more detekt coroutine rules or StrictMode.
- Adding `lifecycle-runtime-compose`.
- Converting Java to Kotlin.
- Changing `Resource.Cancelled`.
- The other XPL-402 agents (performance, architecture, debugger…).

## Open questions

None. Both earlier questions are answered in section 2.

## Implementation plan
- [x] 1. Add `.claude/agents/concurrency-specialist.md` (sections 1, 3, 5)
- [x] 2. Make it own `.claude/rules/coroutines-flow.md`: fix the three wrong rules, add the missing ones; align `compose-specialist`'s "never `collectAsState()`" with Orbit and the missing `lifecycle-runtime-compose` (section 6)
- [x] 3. Route to it: `gini-orchestrator.md`, `.claude/commands/gini-task.md`, the hook in `.claude/settings.json` (section 7)
- [x] 4. Verify: frontmatter, no `file:line`, every named path exists, dry run on hotspots and on false-positive files (section 8)

## Reflection (2026-10-12) — compared with the iOS twin

Compared against the latest iOS file on `origin/XPL-407-408-agents` (newer than the copy used for planning).

**Taken from iOS, made Android-specific:**
- Section order now matches iOS and `android-security-specialist`: fact/threat model → Repo Context → Knowledge Source → Core → Review → What NOT → Checklist → Process → Output.
- Boundaries with other specialists. iOS hands `.task {}` to swiftui-specialist; Android hands effect APIs and keys to compose-specialist, lifecycle wiring to views-specialist, test structure to testing-specialist, and security to android-security-specialist.
- "Grep first" hotspot patterns, moved into Triage, using Android patterns.
- "No new concurrency frameworks without asking" → no RxJava/LiveData, no new catalog artifact.
- "A flaky test is a race signal."

**Removed or rewritten because it was iOS thinking:**
- "A weak reference is not teardown." The repo has a single `WeakReference`. Rewritten as the Android leak: work outliving `onDestroyView` (a Fragment `lifecycleScope` vs `viewLifecycleOwner`, `postDelayed`).
- "`Sendable`-style advice" → Kotlin wording.

**Added from the repo, which iOS has no equivalent for:**
- `by lazy` vs `LazyThreadSafetyMode.NONE`
- facade singleton creation paths
- the Orbit event-loop threads
- detekt's coroutine rules not running in CI
- the jsr166e backport
- following an internal listener to its public end

**Kept different on purpose:**
- iOS relies on an external skill (`swift-concurrency-expert`). Android has no official coroutine skill, so the rules file is the reference.
- iOS puts its essentials inline; Android puts them in `coroutines-flow.md`, which three other agents share.

**Known trade-off:** the agent's review items repeat some rules from `coroutines-flow.md`. That keeps the agent readable on its own, but the two can drift. When a rule changes, change the rules file first, then check the matching review item.

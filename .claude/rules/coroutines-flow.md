# Coroutines & Flow — shared concurrency contract

Canonical concurrency rules for the Gini Android SDKs. Owned by **`concurrency-specialist`**;
the compose, views, and testing specialists read it too — edit rules here, nowhere else.

## Repo reality (do not flag existing code over this)

In a diff review these are never findings. In a full audit, report one only with a concrete failure, as a nit marked "known existing pattern".

- **bank-sdk** ViewModels use **Orbit-MVI**: state mutations inside `intent {}`/`reduce {}`, one-shot effects via `postSideEffect`. Orbit runs intents from an event loop on `Dispatchers.Default`, launched with `Unconfined`: intent bodies start off the main thread and interleave only at suspension points. Never `reduce` from a state snapshot taken before a suspension.
- **capture-sdk, health-sdk, internal-payment-sdk** use plain MVVM: `StateFlow` for UI state (set via `.value =`) and **`MutableSharedFlow`** for one-shot events.
- `Channel(...).receiveAsFlow()` and `stateIn(SharingStarted.WhileSubscribed(5_000))` do **not** occur anywhere in this repo today. They are acceptable target patterns for **new** plain-MVVM code, but never demand refactoring working SharedFlow/Orbit code to them.
- **Dispatchers are mostly hardcoded today**, and the Koin modules pass `Dispatchers.*` as plain values — there are no named dispatcher bindings. Don't flag untouched code for it.
- **`lifecycle-runtime-compose` is not in the version catalog**, so `collectAsStateWithLifecycle()` does not compile in the SDKs.
- `capture-sdk` `analysis/LastAnalyzedDocumentProvider` and `bank-sdk` `AttachedToTransactionDocumentProviderImpl` launch inside a provider. Existing — don't copy the pattern.
- `core-api-library` `Resource.wrapInResource` turns cancellation into the public `Resource.Cancelled`. That is API, not a bug to fix in passing.
- `core-api-library` `http/GiniSessionInterceptor` uses `runBlocking`: OkHttp interceptors are synchronous, so this is the right bridge.
- capture-sdk's Java threading (`AsyncTask`, `HandlerThread`, `synchronized`, the jsr166e `CompletableFuture` backport) is reviewed in place, never converted.

## Rules

- **SDK scopes:** there is no `Application` scope to inject. Work that outlives a screen uses an SDK-owned scope that the SDK cancels (facade cleanup or end of flow). `viewModelScope` for UI work (don't wrap it in a `SupervisorJob` — it has one); never `GlobalScope`; never a fire-and-forget `CoroutineScope(...)`.
- **No `runBlocking` in SDK code** — except to satisfy a synchronous third-party contract on a background thread (the OkHttp interceptor). Never on the main thread.
- **Inject `CoroutineDispatcher`** in new classes — a constructor parameter with a default is fine; add a named Koin binding only together with its first consumer. Don't add new hardcoded `Dispatchers.IO`/`Default` in logic. CPU work on `Default`, blocking I/O on `IO`.
- **Integrator callbacks** run on the thread their KDoc promises (main if unstated), and never while holding a lock or `Mutex`.
- **State/event type:** `StateFlow` for UI state, set with `.value =` (or `update {}` for read-modify-write), not `.emit()`. One-shot commands (navigate/snackbar): Orbit `postSideEffect` in bank-sdk; `MutableSharedFlow` in the other SDKs (or `Channel(BUFFERED).receiveAsFlow()` for new code) — with `replay`/`extraBufferCapacity` chosen on purpose, since replay 0 drops events nobody is collecting. If a new hot flow uses `stateIn`, prefer `SharingStarted.WhileSubscribed(5_000)`.
- **Collect** in Fragments with `viewLifecycleOwner.lifecycleScope` + `repeatOnLifecycle(STARTED)`. In Compose: Orbit's `collectAsState` (bank-sdk), capture-sdk's `collectFlow` helper (`util/compose/FlowExt.kt`), or `collectAsStateWithLifecycle()` once the module adds `lifecycle-runtime-compose`. Never plain `androidx.compose.runtime.collectAsState` on a ViewModel flow.
- Compose sources with `combine`; `flatMapLatest` for user-driven switching (cancels stale work). ViewModels launch + expose triggers; repos/use-cases expose `suspend`/`Flow` only and don't launch (a `Flow`-returning function must not be `suspend`).
- Dispatcher-safe: `withContext(dispatcher)` in suspend fns, `flowOn` upstream (never `withContext` inside `flow {}`); switch at data-source boundaries only.
- **Never swallow `CancellationException`** — `catch (Exception|Throwable)` and `runCatching` around suspend calls must rethrow it; catch expected types, not `Throwable`. `coroutineScope` (atomic) vs `supervisorScope` (independent, `await()` every `async`). `CoroutineExceptionHandler` does nothing on `async`/children.
- **Don't use a `Mutex` as a signal between callbacks** (lock in one, unlock in another). Use `CompletableDeferred<Unit>` completed in `finally` — a skipped `unlock` hangs every waiter, and a double `unlock` throws.
- Don't launch/emit inside `combine`/`map` transforms — use `onEach`. Backpressure: `buffer`/`conflate`/`debounce`/`sample`.
- Bridge callbacks with `callbackFlow` (`trySend`, `close(e)` on setup failure, unregister in `awaitClose`) or `suspendCancellableCoroutine` (resume once on every path, `invokeOnCancellation`). Prefer these over `suspendCoroutine` and blocking `Tasks.await` in new code.
- Release hardware in `finally { withContext(NonCancellable) { … } }`; make CPU loops cancellable (`ensureActive()`/`yield()`).
- **Tests:** `runTest`; every `TestDispatcher` shares `testScheduler`; swap Main via a rule that calls `resetMain` in `finally`; `backgroundScope` for endless collectors; virtual time, never `Thread.sleep`/`runBlocking`/`CountDownLatch` for coroutine timing.
- **Detekt won't catch these.** Its coroutine rules need type resolution, and CI runs plain `detekt`.

Sources: [coroutines best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices) ·
[StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow) ·
[testing coroutines](https://developer.android.com/kotlin/coroutines/test) ·
[exception handling](https://kotlinlang.org/docs/exception-handling.html) ·
[cancellation](https://kotlinlang.org/docs/cancellation-and-timeouts.html)

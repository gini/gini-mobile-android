# Coroutines & Flow — shared ViewModel-layer contract

Canonical concurrency rules for the Gini Android SDKs. The compose, views, and
testing specialists all defer to this file — edit rules here, nowhere else.

## Repo reality (do not flag existing code over this)

- **bank-sdk** ViewModels use **Orbit-MVI**: state mutations inside `intent {}`/`reduce {}`, one-shot effects via `postSideEffect`.
- **capture-sdk, health-sdk, internal-payment-sdk** use plain MVVM: `StateFlow` for UI state (set via `.value =`) and **`MutableSharedFlow`** for one-shot events.
- `Channel(...).receiveAsFlow()` and `stateIn(SharingStarted.WhileSubscribed(5_000))` do **not** occur anywhere in this repo today. They are acceptable target patterns for **new** plain-MVVM code, but never demand refactoring working SharedFlow/Orbit code to them.

## Rules

- **Inject `CoroutineDispatcher`** (bind named dispatchers in Koin where the module uses it); never hardcode `Dispatchers.IO`/`Default`. `viewModelScope` for UI work (don't wrap it in a `SupervisorJob` — it has one); never `GlobalScope`.
- **State/event type:** `StateFlow` for UI state, set with `.value =`, not `.emit()`. One-shot commands (navigate/snackbar): Orbit `postSideEffect` in bank-sdk; `MutableSharedFlow` in the other SDKs (or `Channel(BUFFERED).receiveAsFlow()` for new code); `SharedFlow` for multi-collector. If a new hot flow uses `stateIn`, prefer `SharingStarted.WhileSubscribed(5_000)`.
- Collect in Fragments with `viewLifecycleOwner.lifecycleScope` + `repeatOnLifecycle(STARTED)`; in Compose with `collectAsStateWithLifecycle()`.
- Compose sources with `combine`; `flatMapLatest` for user-driven switching (cancels stale work). ViewModels launch + expose triggers; repos/use-cases expose `suspend`/`Flow` only and never launch (a `Flow`-returning function must not be `suspend`).
- Dispatcher-safe: `withContext(dispatcher)` in suspend fns, `flowOn` upstream; switch at data-source boundaries only.
- Never catch `CancellationException`; catch expected types, not `Throwable`; `coroutineScope` (atomic) vs `supervisorScope` (independent, `await()` every `async`).
- Don't launch/emit inside `combine`/`map` transforms — use `onEach`. Backpressure: `buffer`/`conflate`/`debounce`/`sample`.
- Bridge callbacks with `callbackFlow` (`awaitClose`, `trySend`) or `suspendCancellableCoroutine` (resume once, `invokeOnCancellation`); `.await()` for `Task<T>`.
- Release hardware in `finally { withContext(NonCancellable) { … } }`; make CPU loops cancellable (`ensureActive()`/`yield()`).

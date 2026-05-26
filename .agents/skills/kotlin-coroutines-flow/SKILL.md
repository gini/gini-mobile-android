---
name: kotlin-coroutines-flow
description: Guide for Kotlin coroutines and Flow patterns in Android. Use this when writing async code, working with StateFlow/SharedFlow, testing coroutines with Turbine, or debugging coroutine lifecycle and cancellation issues.
---

This repo uses Kotlin coroutines and Flow throughout all SDK modules. ViewModels use `viewModelScope`; repositories use `suspend` functions and `Flow` for reactive streams. Tests use `kotlinx-coroutines-test` and `Turbine`.

## Coroutine scopes — which to use

| Scope | Where | When |
|---|---|---|
| `viewModelScope` | ViewModel | UI-driven work; auto-cancelled when ViewModel clears |
| `lifecycleScope` | Activity/Fragment | UI observation; use `repeatOnLifecycle` to avoid leaks |
| `coroutineScope` / `supervisorScope` | Suspend functions | Structured concurrency in business logic |
| Custom `CoroutineScope` | Repositories, Services | Inject via constructor for testability |

**Never** use `GlobalScope` — it leaks and is untestable.

## StateFlow vs SharedFlow

```kotlin
// StateFlow — single current value, replay 1, for UI state
private val _state = MutableStateFlow(initialValue)
val state: StateFlow<UiState> = _state.asStateFlow()

// SharedFlow — events/side effects, no replay by default
private val _events = MutableSharedFlow<Event>()
val events: SharedFlow<Event> = _events.asSharedFlow()

// Emit a side effect (from ViewModel)
viewModelScope.launch { _events.emit(Event.NavigateBack) }
```

## Collecting Flow safely in UI

```kotlin
// In a Fragment — use repeatOnLifecycle to avoid collecting in background
viewLifecycleOwner.lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect { state -> render(state) }
    }
}

// In Jetpack Compose
val state by viewModel.state.collectAsStateWithLifecycle()
```

**Never** use `.collect {}` directly in `lifecycleScope.launch {}` without `repeatOnLifecycle` — it leaks collection in the background.

## Common Flow operators

```kotlin
flow
    .map { it.transform() }
    .filter { it.isValid() }
    .distinctUntilChanged()          // skip identical consecutive emissions
    .debounce(300)                   // for search inputs
    .catch { e -> emit(fallback) }   // handle errors inline
    .onEach { log(it) }              // side effects without consuming
    .stateIn(                        // convert to StateFlow
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading
    )
```

## Error handling

```kotlin
// In a Flow
flow.catch { e -> _state.update { it.copy(error = e.message) } }

// In a coroutine
viewModelScope.launch {
    runCatching { repository.fetchData() }
        .onSuccess { _state.update { s -> s.copy(data = it) } }
        .onFailure { _state.update { s -> s.copy(error = it.message) } }
}
```

## Testing coroutines — `kotlinx-coroutines-test`

```kotlin
class MyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule() // replaces Dispatchers.Main with TestDispatcher

    @Test
    fun `loading state is emitted then data`() = runTest {
        val viewModel = MyViewModel(fakeRepository)

        viewModel.state.test {            // Turbine
            assertEquals(UiState.Loading, awaitItem())
            viewModel.load()
            assertEquals(UiState.Success(data), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

// MainDispatcherRule helper (standard pattern):
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(d: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(d: Description) = Dispatchers.resetMain()
}
```

## Testing Flow with Turbine

```kotlin
repository.dataFlow.test {
    assertEquals(expected1, awaitItem())
    assertEquals(expected2, awaitItem())
    awaitComplete()                     // assert the flow completed
}

// For SharedFlow / side effects:
viewModel.events.test {
    viewModel.onButtonClick()
    assertEquals(Event.NavigateBack, awaitItem())
    cancelAndIgnoreRemainingEvents()
}
```

## Common pitfalls

- **Forgetting `distinctUntilChanged()`** on StateFlow collectors causing redundant recompositions
- **Using `Dispatchers.IO` directly** in production code — inject a `CoroutineDispatcher` instead for testability
- **Collecting a cold Flow multiple times** — use `shareIn` or `stateIn` to make it hot
- **`launch` inside `collect`** — use `mapLatest`/`flatMapLatest` to automatically cancel previous work
- **Not cancelling jobs** — always use structured concurrency; avoid storing `Job` references unless necessary

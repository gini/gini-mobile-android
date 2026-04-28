---
name: jetpack-compose-best-practices
description: Guide for Jetpack Compose best practices in Android. Use this when writing or reviewing Compose UI code, migrating from ViewBinding to Compose, working with Material 3 theming, optimizing recomposition, or writing Compose previews and tests.
---

This repo uses Jetpack Compose with Material 3 for newer screens and is actively migrating from ViewBinding. DI is via Koin; architecture is Orbit MVI. ViewModels expose `StateFlow<UiState>` consumed with `collectAsStateWithLifecycle()`.

## State hoisting — the core principle

Move state up to the lowest common ancestor that needs it. Keep Composables stateless (dumb) where possible.

```kotlin
// Stateless (preferred — testable, reusable, previewable)
@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) { ... }

// Stateful (only at screen/ViewModel boundary)
@Composable
fun AmountScreen(viewModel: AmountViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AmountInput(value = state.amount, onValueChange = viewModel::onAmountChanged)
}
```

## Connecting to Orbit MVI ViewModels

```kotlin
@Composable
fun PaymentScreen(viewModel: PaymentViewModel = koinViewModel()) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    // Handle side effects
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel) {
        viewModel.container.sideEffectFlow
            .flowWithLifecycle(lifecycleOwner.lifecycle)
            .collect { effect ->
                when (effect) {
                    is PaymentSideEffect.NavigateToConfirmation -> { /* navigate */ }
                }
            }
    }

    PaymentContent(state = state, onIntent = viewModel::dispatch)
}
```

## Avoiding unnecessary recomposition

```kotlin
// Use stable data classes for state — Compose compiler tracks them
@Immutable
data class PaymentUiState(val amount: String = "", val isLoading: Boolean = false)

// Use `key` in lists to preserve identity
LazyColumn {
    items(items, key = { it.id }) { item -> ItemRow(item) }
}

// Use `derivedStateOf` when a value is derived from other state
val isSubmitEnabled by remember {
    derivedStateOf { state.amount.isNotBlank() && !state.isLoading }
}

// Use `lambda references` instead of inline lambdas in recomposing contexts
Button(onClick = viewModel::onSubmit)   // stable
Button(onClick = { viewModel.onSubmit() })  // creates new lambda each recomposition
```

## Material 3 theming

```kotlin
// Access theme tokens
Surface(color = MaterialTheme.colorScheme.surface) { ... }
Text(style = MaterialTheme.typography.bodyLarge)

// Use MaterialTheme.colorScheme, never hardcoded colors
// Use MaterialTheme.typography, never hardcoded TextStyles
// Use MaterialTheme.shapes for consistent corner radii
```

## Modifier best practices

```kotlin
// Always expose a modifier parameter with default
@Composable
fun MyComponent(modifier: Modifier = Modifier) {
    Box(modifier = modifier) { ... }
}

// Chain modifiers in a logical order: size → padding → background → clickable
Box(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .background(color, RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
)
```

## Previews

```kotlin
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Preview(showBackground = true, name = "Light")
@Composable
private fun PaymentScreenPreview() {
    AppTheme {
        PaymentContent(
            state = PaymentUiState(amount = "12.50"),
            onIntent = {}
        )
    }
}
```

Always preview the stateless `*Content` composable, not the screen-level one wired to a ViewModel.

## Migrating from ViewBinding to Compose

1. Create a stateless `@Composable` equivalent of the View layout
2. Wire it to the ViewModel's `StateFlow` at the Fragment/Activity level using `setContent { ... }` or `ComposeView`
3. Migrate one screen at a time — use `AndroidView` for Views not yet migrated
4. Remove the corresponding XML layout file once migration is complete

```kotlin
// Interop: embed Compose in an existing Fragment
override fun onCreateView(...): View = ComposeView(requireContext()).apply {
    setContent {
        AppTheme {
            MyComposableScreen()
        }
    }
}
```

## Compose testing

```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun `submit button is disabled when amount is empty`() {
    composeTestRule.setContent {
        AppTheme {
            PaymentContent(state = PaymentUiState(amount = ""), onIntent = {})
        }
    }
    composeTestRule.onNodeWithTag("submit_button").assertIsNotEnabled()
}
```

Use `testTag` to mark nodes; avoid text-based selectors for non-display strings.

## Common pitfalls

- **Reading State outside composition** — only read `State` inside `@Composable` functions or `remember` blocks
- **Creating objects in composition** without `remember` — they recreate every recomposition
- **Side effects outside `LaunchedEffect`/`SideEffect`** — never launch coroutines or run effects directly in the Composable body
- **Deeply nested Composables** — extract to named functions; improves readability and recomposition scope

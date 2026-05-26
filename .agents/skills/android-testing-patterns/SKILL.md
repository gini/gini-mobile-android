---
name: android-testing-patterns
description: Guide for Android testing patterns in this repo. Use this when writing unit tests, instrumented tests, ViewModel tests, repository tests, or when debugging flaky tests. Covers Robolectric, MockK, Turbine, Espresso, and Orbit MVI testing.
---

This repo uses a three-tier testing strategy. All test dependencies are centralized in `gradle/libs.versions.toml`.

## Test stack summary

| Layer | Framework | Location |
|---|---|---|
| Unit tests | JUnit 4, Robolectric, MockK, Turbine, Truth | `src/test/` |
| Instrumented tests | Espresso, UIAutomator, AndroidX Test | `src/androidTest/` |
| MVI ViewModel tests | Orbit MVI test, kotlinx-coroutines-test | `src/test/` |

## Unit tests with Robolectric

Use Robolectric to run Android-dependent code (Context, Resources, etc.) on the JVM without an emulator.

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MyRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `reads shared preferences correctly`() {
        val prefs = context.getSharedPreferences("test", Context.MODE_PRIVATE)
        prefs.edit().putString("key", "value").apply()
        assertThat(prefs.getString("key", null)).isEqualTo("value")
    }
}
```

Enable Android resources in `build.gradle.kts` if needed:
```kotlin
testOptions {
    unitTests { isIncludeAndroidResources = true }
}
```

## Mocking with MockK

```kotlin
// Create mocks
val mockRepository = mockk<PaymentRepository>()
val spyViewModel = spyk(MyViewModel(mockRepository))

// Stub
coEvery { mockRepository.fetchPayment(any()) } returns Result.success(payment)
every { mockRepository.paymentFlow } returns flowOf(payment)

// Verify
coVerify(exactly = 1) { mockRepository.fetchPayment("id-123") }
verify { mockRepository.paymentFlow }

// Relaxed mock (returns defaults for all calls)
val relaxedMock = mockk<MyClass>(relaxed = true)

// Capture arguments
val slot = slot<String>()
coEvery { mockRepository.save(capture(slot)) } just Runs
// After call: slot.captured == the argument passed
```

## Testing Orbit MVI ViewModels

```kotlin
class PaymentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockRepository = mockk<PaymentRepository>()
    private lateinit var viewModel: PaymentViewModel

    @Before
    fun setUp() {
        viewModel = PaymentViewModel(mockRepository)
    }

    @Test
    fun `loading then success state on fetch`() = runTest {
        val payment = Payment("12.50", "IBAN123")
        coEvery { mockRepository.fetchPayment(any()) } returns payment

        viewModel.test {
            expectInitialState()         // assert initial state
            viewModel.onFetch("id-1")
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false, payment = payment) }
        }
    }

    @Test
    fun `navigation side effect is emitted on submit`() = runTest {
        viewModel.test {
            expectInitialState()
            viewModel.onSubmit()
            expectSideEffect(PaymentSideEffect.NavigateToConfirmation)
        }
    }
}
```

## Testing Flow with Turbine

```kotlin
@Test
fun `emits updated state on each payment change`() = runTest {
    viewModel.container.stateFlow.test {
        assertThat(awaitItem()).isEqualTo(PaymentUiState())   // initial
        viewModel.updateAmount("10.00")
        assertThat(awaitItem().amount).isEqualTo("10.00")
        cancelAndIgnoreRemainingEvents()
    }
}
```

## Google Truth assertions

Prefer Truth over JUnit `assertEquals` for readable failure messages:

```kotlin
// Instead of: assertEquals(expected, actual)
assertThat(actual).isEqualTo(expected)
assertThat(list).containsExactly(a, b, c).inOrder()
assertThat(result).isInstanceOf(Result.Success::class.java)
assertThat(string).startsWith("Bearer ")
assertThat(map).containsKey("Authorization")
```

## Instrumented tests (on-device / emulator)

Located in `src/androidTest/`. These test against real Gini API endpoints.

### Injecting test credentials

Credentials are injected at build time via the `injectTestProperties` Gradle task into `src/androidTest/assets/test.properties`. Load them in tests:

```kotlin
@Before
fun setUp() {
    val props = Properties()
    InstrumentationRegistry.getInstrumentation().context.assets
        .open("test.properties").use { props.load(it) }
    clientId = props.getProperty("clientId")
    clientSecret = props.getProperty("clientSecret")
}
```

### Running instrumented tests locally

```bash
./gradlew :<module>:connectedDebugAndroidTest
```

Requires a connected device or running emulator (API 33 recommended to match CI).

### Espresso patterns

```kotlin
// Click
onView(withId(R.id.submit_button)).perform(click())

// Type text
onView(withId(R.id.amount_input)).perform(typeText("12.50"), closeSoftKeyboard())

// Assert visibility
onView(withId(R.id.error_message)).check(matches(isDisplayed()))
onView(withText("Success")).check(matches(isDisplayed()))

// Wait for async operations — use IdlingResource or:
onView(isRoot()).perform(waitFor(2000))   // avoid — prefer IdlingResource
```

## MainDispatcherRule (standard helper)

Include this in every test module that tests coroutines:

```kotlin
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(testDispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

## Common test pitfalls

- **Not using `runTest` for coroutine tests** — always wrap in `runTest {}`, not `runBlocking {}`
- **Forgetting `cancelAndIgnoreRemainingEvents()`** in Turbine — causes test to hang waiting for more emissions
- **Sharing mocks between tests** without resetting — use `@Before` to recreate mocks
- **Flaky instrumented tests** — often caused by animation; disable animations on the emulator or use `AnimationMode.Disabled` in Compose tests
- **Hardcoded delays** (`Thread.sleep`) — replace with `IdlingResource`, `awaitItem()`, or `advanceUntilIdle()`

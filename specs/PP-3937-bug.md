# PP-3937: Camera QR waiters leak the fragment when the education carousel is interrupted

Status: fixed
Ticket: https://ginis.atlassian.net/browse/PP-3937

## Symptom

Observed: when the QR-code education carousel on the Camera screen is interrupted — the user
presses back during the 4.5 s animation, or the host pops the fragment — the `CameraFragmentImpl`
(and with it the `CameraFragment`, its views and the host `Activity`) stays reachable from a
suspended coroutine for the rest of the process. No crash; one leaked camera screen per
interruption. Raised in the PR #1017 review
(https://github.com/gini/gini-mobile-android/pull/1017#discussion_r4360130909).

Expected: tearing down the camera fragment releases every coroutine that was waiting for the
carousel; nothing outlives the fragment.

A second, worse consequence of the same defect, found while diagnosing: the Camera screen's
empty-extractions and analysis-error branches navigate away *without* waiting for the carousel
(`CameraFragmentImpl.analyzeQRCode`, the `mappedExtractions.isEmpty()` branch and
`handleAnalysisError`). That destroys the camera view while the fragment stays on the back stack.
Coming back and scanning a QR code that again triggers education blocks the main thread forever
in `runBlocking { … educationMutex.lock() }` (an ANR), because the first step's lock was never
released and the fragment instance — and its mutex — survived the round trip.

## Reproduction

Cheapest faithful reproduction, a JVM unit test in
`capture-sdk/sdk/src/test/java/net/gini/android/capture/camera/CameraFragmentExtensionTest.kt`:

- `a second education step is not blocked by a carousel that never completed` — runs the
  education half twice against a stubbed `QRCodeEducationPopup` whose `show` never invokes
  `onComplete` (exactly what a disposed `ComposeView` does). Before the fix the second
  `showQrCodePopup` parks the test thread in `educationMutex.lock()` and the test fails on its
  JUnit timeout.

Manual reproduction (from the ticket): enable QR education, scan a payment QR code, press back
while the carousel plays, take a heap dump — `CameraFragmentImpl` is retained through a coroutine
suspended in `Mutex.lock`.

## Root cause

`capture-sdk/sdk/src/main/java/net/gini/android/capture/camera/CameraFragmentExtension.kt` uses a
`Mutex` as a one-shot signal between callbacks and waits on it from scopes nothing owns:

- `educationMutex` (line 53) is locked on the main thread inside `runBlocking` in
  `showQrCodePopup` (line 213) right after the carousel is shown, and the **only** unlock is the
  carousel's completion callback (line 210), which `QrCodeEducationPopupContent` fires from a
  `LaunchedEffect` in `AnimatedEducationMessageWithIntro.kt` lines 57–60 after the 4.5 s
  animation.
- Two waiters park on that lock from fire-and-forget scopes:
  `onQrCodeRecognized` launches `CoroutineScope(Dispatchers.IO)` (lines 235–239) and
  `onQrCodeFallbackToUnsupported` launches `CoroutineScope(fallbackIoDispatcher)` (lines 247–250).
  Neither scope has a `Job` anyone cancels; `CameraFragmentImpl.onDestroy` (line 983) only cancels
  the import `AsyncTask`.

Mechanism, cause to symptom: the ComposeView hosting the carousel (`gc_qr_code_education_compose_view`,
default `ViewCompositionStrategy`) disposes its composition when the fragment view is detached.
That cancels the `LaunchedEffect`, so `onComplete` — and therefore `educationMutex.unlock()` —
never runs. A waiter already suspended in `Mutex.lock` stays suspended for the process lifetime.
Its continuation captures the lambda, which captures `this` (`CameraFragmentExtension` →
`CameraFragmentImpl` → `mFragment` → views → host `Activity`), plus the extractions map. That is
the leak. The same never-released lock is what blocks the next `showQrCodePopup` on a fragment
whose view was recreated (`createPopups` builds a fresh popup per view, but the extension — and
its mutex — is per fragment instance, created in `CameraFragment.onCreate`).

Both halves of `.claude/rules/coroutines-flow.md` are violated: "never a fire-and-forget
`CoroutineScope(...)`" and "don't use a `Mutex` as a signal between callbacks … use
`CompletableDeferred<Unit>`".

Where the symptom surfaces (heap retention of the fragment, main-thread hang on re-entry) is
downstream of the cause (the signal and the scopes in `CameraFragmentExtension`); the fix belongs
at the cause.

## Proposed fix

All in `capture-sdk:sdk`, no public API impact (`CameraFragmentExtension` is `internal`,
`CameraFragmentImpl` is package-private; nothing in `capture-sdk/sdk/api/sdk.api` changes).

`CameraFragmentExtension.kt`:

- Replace `educationMutex` with a per-step `CompletableDeferred<Unit>`: the education branch of
  `showQrCodePopup` creates a fresh one before `qrCodeEducationPopup.show(...)`, and the
  carousel callback completes it (after the existing counter increment). No `lock()` on the main
  thread any more. A step interrupted before completion leaves a Deferred nobody waits on, and
  the next step gets its own — which removes the re-entry hang.
- Give the extension one SDK-owned scope, `CoroutineScope(SupervisorJob() + ioDispatcher)`, and
  launch both waiters on it. Waiters `await()` the current step's Deferred (if any) instead of
  `withLock`. The listener call in `onQrCodeRecognized` keeps running on the IO dispatcher as
  today; the Wero fallback keeps hopping to the main dispatcher.
- Add `resetQrEducationStep()`, which drops the current step's Deferred and running flag.
  `CameraFragmentImpl.createPopups` calls it on every view creation next to the fresh
  `QRCodeEducationPopup`: `handleEPSPaymentQRCode` reaches `onQrCodeRecognized` without passing
  through `showQrCodePopup`, so on a fragment that survived an interrupted carousel with a
  recreated view an EPS scan would otherwise await the dead carousel until `onDestroy`.
- Add `cancelQrStepWaiters()`, which cancels that scope's job.
  `CameraFragmentImpl.onDestroy` calls it, so waiters end with `CancellationException` when the
  fragment is destroyed — nothing is delivered to a dead screen.
- Inject the two dispatchers through a constructor with defaults and drop the mutable
  `fallbackIoDispatcher` / `fallbackMainDispatcher` test hooks. `CameraFragmentImpl`'s existing
  constructor passes `Dispatchers.IO`/`Main` explicitly and gains a package-private
  `@VisibleForTesting` overload that forwards test dispatchers.

`CameraFragmentImpl.java`: call the new teardown from `onDestroy()` and the reset from
`createPopups(View)`; add the dispatcher-forwarding
constructor overload; update the two comments that mention "the education mutex".

Tests: `CameraFragmentExtensionTest` loses the reflective `educationMutex()` accessor — the
Deferred is completed through the captured `show` callback instead;
`CameraFragmentImplTest` constructs its impls with `UnconfinedTestDispatcher` via the new
constructor instead of setting the removed properties.

## Regression test plan

`capture-sdk/sdk/src/test/java/net/gini/android/capture/camera/CameraFragmentExtensionTest.kt`
(JUnit4 + MockK + Truth + `kotlinx-coroutines-test`, matching the file):

1. `a second education step is not blocked by a carousel that never completed` — fails before
   the fix (JUnit timeout in `Mutex.lock`), passes after. Already added as the reproduction.
2. `destroying the fragment releases waiters parked on an interrupted carousel` — education half
   with a `show` stub that never completes, then `onQrCodeRecognized` and
   `onQrCodeFallbackToUnsupported` on a `StandardTestDispatcher(testScheduler)`; after
   `advanceUntilIdle` neither callback fired; after the teardown call and `advanceUntilIdle` the
   scope has no active child and still neither callback fired. Cannot compile before the fix
   (the teardown method does not exist); it is the acceptance-criteria test.
3. Happy-path parity: `onQrCodeRecognized` and `onQrCodeFallbackToUnsupported` wait until the
   captured `show` callback runs, then fire exactly once — rewrite of the two existing mutex
   tests without reflection. Plus `a retrieval step after an interrupted education step does not
   wait for the dead carousel`, which guards the reset of the signal for retrieval steps.
4. `CameraFragmentImplTest`: `onDestroy releases a waiter parked on an interrupted education
   carousel` — pins the production wiring `CameraFragmentImpl.onDestroy` →
   `cancelQrStepWaiters()` through the new test constructor.

Run: `./gradlew capture-sdk:sdk:testDebugUnitTest --tests "net.gini.android.capture.camera.CameraFragmentExtensionTest"`
and `… --tests "net.gini.android.capture.camera.CameraFragmentImplTest"`.

## Out of scope

- `AnalysisScreenPresenterExtension.kt` (lines 76, 397–420) uses the same mutex-as-signal shape
  for the invoice education, mitigated by a `releaseMutex()` guard. Same rule violation, separate
  screen — file a ticket rather than widen this fix.
- `showQrCodePopup` and the carousel callback keep their `runBlocking` around the suspend use
  cases; the rule forbids it in SDK code but it predates this ticket and is unrelated to the leak.
- `fragmentListener.onExtractionsAvailable` is invoked on the IO dispatcher today (the Kotlin
  implementation in `GiniCaptureFragment` tolerates it). Preserved as-is; a threading change is
  a separate decision.
- The empty-extractions and error branches of `analyzeQRCode` navigate away mid-carousel. With a
  per-step Deferred that is harmless; whether they should wait for the carousel like the success
  branch does is a product question.

## Open questions

- Teardown hook: `CameraFragmentImpl.onDestroy` (fragment instance gone) as the ticket proposes.
  Cancelling on `onDestroyView` instead would also stop a waiter when the view is recreated, but
  no waiter can be pending across a view recreation today (the branches that navigate forward
  do not launch one), so `onDestroy` is sufficient and minimal.

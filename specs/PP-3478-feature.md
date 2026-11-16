# PP-3478: [Android] Ingredient Brand - Add UI Automation tests - BrowserStack

Status: implemented (R21 — 3 BrowserStack runs of both groups — pending, run by the user)
Ticket: https://ginis.atlassian.net/browse/PP-3478
Epic: https://ginis.atlassian.net/browse/PP-2568
Manual test basis: PP-3476 (Xray Test Set PP-3740)

## Problem

The ingredient brand feature (epic PP-2568) is built on Android: the "Powered by Gini" badge
(PP-2571), the backend flag `ingredientBrandScreens` (PP-3507) and the animated Gini loading
indicator (PP-3512). Today it is verified only by unit tests and by manual Xray runs. The PM
wants the main behaviour covered by automated UI tests that run on real devices in BrowserStack,
so every release can be checked without a manual regression run.

The hard part is the switch itself: `ingredientBrandScreens` comes from the backend
`/configurations` response, and a device cannot change it. The example app already has a
test-only mock backend (`UiTestMockBackend`, built for the credit note flags in
`CreditNoteMockBackendTests`) that serves `/configurations` from the test body. This spec extends
it with the ingredient brand flag, the education flag, and an analysis delay, so the Analysis
screen stays up long enough to be checked.

Decisions from the clarifying questions:
- **Flag source: mock backend only.** Every new test arms `UiTestMockBackend`. No new test reads
  the real server configuration.
- **Density: real BrowserStack devices only.** The ticket asks for mdpi–xxxhdpi. BrowserStack real
  devices are xxhdpi/xxxhdpi; the badge is a single vector drawable and its layout is already
  covered by `IngredientBrandLayoutTest`/`PoweredByGiniLayoutTest`. mdpi–hdpi is left to a manual
  or emulator check (see "Not tested").
- **Flows:** image upload analysis, PDF upload analysis, "custom loading indicator is ignored", and
  the QR code camera overlay.
- **QR overlay: local-only emulator test.** BrowserStack rejects camera image injection for
  Espresso (see the comment above `TEST_IMAGE` in `bs_build_and_upload.sh`), so a QR scan cannot
  happen there. The QR test uses the Android emulator's virtual camera scene and is skipped on any
  other device.
- **BrowserStack wiring:** a new group script, added to the run-all script.
- **Education messages are in scope.** The Gini mark only *replaces* the old loading indicator
  and follows its rules. While an education message is shown there is no loading indicator, and
  after the message ends the screen behaves exactly as it does with the brand off. Only the badge
  and the kind of indicator differ between brand on and brand off.
  - The invoice education (Analysis screen) needs a photo taken with the camera. An upload does
    not trigger it (`GetInvoiceEducationTypeUseCase` needs flow type Photo or QrCode). A camera
    photo works on BrowserStack: the camera sees the device rack, and the mock ignores the bytes.
  - One long test also checks the education limit: education on analyses 1 and 2, none on
    analysis 3.
  - The QR education (camera screen) goes into the local emulator QR test.

## Requirements

Definitions used below:
- *Badge*: the view with id `gc_powered_by_gini` (capture-sdk).
- *Gini mark*: the loading indicator `ImageView` whose content description is the capture-sdk
  string `gc_gini_loading_indicator_content_description` ("Wird geladen" / "Loading").
- *Default indicator*: the `ProgressBar` that `DefaultLoadingIndicatorAdapter` puts into
  `gc_injected_loading_indicator_container`.
- *Custom indicator*: the example app's Lottie view `animationView`
  (`animation_onboarding_lottie.xml`), used when "Screen custom loading indicator" is on.
- *Tip*: the capture suggestion container `gc_analysis_hint_container`.
- *Mock armed with X*: `UiTestMockBackend` armed with scenario `INVOICE` and
  `ingredientBrandScreens = X`, before the photo payment button is clicked.
- *Education on*: the mock also serves `isQrCodeEducationEnabled = true`. It switches both the
  invoice education (`GetEducationFeatureEnabledUseCase`) and the QR education
  (`CameraFragmentExtension.kt:176`).
- *Invoice education shown*: the text `gc_qr_education_intro_message` ("Falls Sie es nicht
  wussten...") or `gc_invoice_education_message` is on screen (`AnimatedEducationMessageWithIntro`,
  1.5 s intro + 3 s message).

### Mock backend (test support)

- **R1 (MUST, entry):** Given a test arms `UiTestMockBackend` with `ingredientBrandScreens = X`,
  when the SDK fetches `/configurations`, then `UiTestMockNetworkService.getConfiguration` returns
  a `Configuration` whose `ingredientBrandScreens` equals X.
- **R2 (MUST, entry):** Given a test does not set `ingredientBrandScreens`, when the mock is armed,
  then `ingredientBrandScreens` is empty, so the existing `CreditNoteMockBackendTests` keep running
  with the brand off, unchanged.
- **R3 (MUST, async):** Given a test arms the mock with `analysisDelayMillis = D > 0`, when the SDK
  calls `analyze`, then the success callback is delivered on the main thread no earlier than D ms
  later. While it is pending, the Analysis screen (or the QR overlay) stays on screen.
- **R4 (MUST, async):** Given an analysis callback is pending, when the SDK cancels the returned
  `CancellationToken`, then the callback is never delivered.
- **R5 (MUST, entry):** Given `analysisDelayMillis = 0` (the default), when the SDK calls
  `analyze`, then the callback is delivered synchronously, exactly as today.
- **R6 (MUST, entry):** Given any test finishes, when `UiTestMockBackend.disarm()` runs, then
  `ingredientBrandScreens` is empty again, `analysisDelayMillis` is 0, and
  `qrCodeEducationEnabled` is `false` again.
- **R6a (MUST, entry):** Given a test arms the mock with `qrCodeEducationEnabled = true`, when the
  SDK fetches `/configurations`, then `Configuration.isQrCodeEducationEnabled` is `true`. When a
  test does not set it, it stays `false`, as today.

### Analysis screen, PDF upload (no tips on this path)

- **R7 (MUST, happy):** Given the mock is armed with `["Analysis"]` and a delay of 8 s, when a PDF is
  imported through the file picker, then while the Analysis screen is shown:
  - the badge is displayed;
  - the badge is horizontally centred within 2 dp of the screen centre;
  - the badge's bottom edge is 16 dp (±2 dp) above the bottom edge of the Analysis root layout;
  - the badge does not overlap the analysis message `gc_analysis_message`;
  - the Gini mark is displayed and the default indicator is not.
- **R8 (MUST, happy):** Given the same setup as R7, when the Gini mark is displayed, then its
  content description equals the string `gc_gini_loading_indicator_content_description` for the
  device locale (TalkBack announcement).
- **R9 (MUST, happy):** Given the mock is armed with `[]`, when a PDF is imported, then during the
  analysis the badge is not displayed, the default indicator is displayed, and the Gini mark is
  not.
- **R10 (MUST, error):** Given the mock is armed with `["Foo"]`, when a PDF is imported, then the
  screen behaves exactly as in R9, and analysis ends on the extraction screen (no crash, no error
  screen).
- **R11 (SHOULD, happy):** Given the mock is armed with `["analysis"]` (lower case), when a PDF is
  imported, then the result is the same as R7 (badge and Gini mark shown).

### Analysis screen, image upload (tips on this path)

- **R12 (MUST, happy):** Given the mock is armed with `["Analysis"]` and a delay of 12 s, when an
  image is uploaded and processed, then:
  - the badge is displayed before the first tip appears;
  - once the first tip is displayed, the badge is not displayed;
  - the badge is still not displayed when the next tip has replaced the first one.
- **R13 (MUST, happy):** Given the mock is armed with `[]` and a delay of 12 s, when an image is
  uploaded and processed, then a tip is displayed and the badge is never displayed during the
  analysis.

### Client customization is ignored while the brand is on

- **R14 (MUST, happy):** Given "Screen custom loading indicator" is on (set via
  `ConfigurationViewModel`, not the settings screen) and the mock is armed with `["Analysis"]`, when
  a PDF is imported, then during the analysis the Gini mark is displayed and the custom indicator is
  not.
- **R15 (MUST, happy):** Given "Screen custom loading indicator" is on and the mock is armed with
  `[]`, when a PDF is imported, then during the analysis the custom indicator is displayed, and
  neither the Gini mark nor the badge is.

### Invoice education on the Analysis screen (camera photo)

- **R22 (MUST, happy):** Given the mock is armed with `["Analysis"]`, education on and a delay of
  12 s, and the app data is fresh, when a photo is taken with the camera and processed, then while
  the invoice education is shown:
  - the badge is displayed;
  - neither the Gini mark nor the default indicator is displayed;
  - no tip is displayed.
- **R23 (MUST, happy):** Given the same setup as R22 but the mock is armed with `[]`, when a photo is
  taken and processed, then while the invoice education is shown, the badge, the Gini mark and the
  default indicator are all not displayed, and no tip is displayed.
- **R24 (MUST, happy):** Given the setup of R22, when the education message has disappeared and the
  analysis is still pending, then the loading-indicator area shows what the same moment shows with
  the brand off: neither the Gini mark nor the default indicator. The badge is still displayed and
  no tip appears. This proves the Gini mark follows the old indicator's rules.
- **R25 (MUST, happy):** Given the setup of R22, when one test runs 3 camera-photo analyses in a
  row (back to the main screen after each extraction screen), then analyses 1 and 2 show the
  invoice education with the badge, and analysis 3 shows no education, the Gini mark, and then a
  tip, after which the badge is not displayed.

### QR code camera overlay (local emulator only)

- **R16 (SHOULD, happy):** Given the test runs on an Android emulator whose back camera shows the
  virtual scene with a SEPA payment QR code poster, and the mock is armed with `["Analysis"]` and a
  delay of 8 s, when the camera recognizes the QR code, then while the invoice is retrieved the
  camera screen shows the badge and the Gini mark.
- **R17 (SHOULD, happy):** Given the same emulator setup and the mock armed with `[]`, when the
  camera recognizes the QR code, then neither the badge nor the Gini mark is shown.
- **R17a (SHOULD, happy):** Given the emulator setup of R16, fresh app data and the mock armed with
  `["Analysis"]` and education on, when the camera recognizes the QR code for the first time, then
  while the QR education is shown, the badge is displayed and the Gini mark is not.
- **R18 (MUST, error):** Given the QR tests run on a device that is not an emulator (every
  BrowserStack device), when the test starts, then it is skipped with an
  `AssumptionViolatedException` whose message says the virtual camera scene is required. It is
  never reported as a failure.

### BrowserStack

- **R19 (MUST, entry):** Given `bs_run_group_ingredientbrand.sh` is run, when it triggers the build,
  then the BrowserStack build contains exactly the classes `IngredientBrandTests` and
  `IngredientBrandQrOverlayTests`.
- **R19a (MUST, entry):** Given `bs_run_group_ingredientbrand_education.sh` is run, when it
  triggers the build, then the build contains exactly `IngredientBrandEducationTests` and sends
  `"disableAnimations": "false"`. The education is a Compose animation, and with animations off it
  may end at once and never be seen. The other groups keep `"true"`.
- **R20 (MUST, entry):** Given `bs_run_all_groups.sh` is run, when it triggers its groups, then
  `ingredientbrand` and `ingredientbrand_education` are among them.
- **R21 (MUST, happy):** Given both groups have been run 3 times in a row on the default
  BrowserStack device pair, then all `IngredientBrandTests` and `IngredientBrandEducationTests`
  tests pass in all 3 runs, with no test passing only on a retry. The QR tests show as skipped. This is the "no flaky tests left enabled" acceptance
  criterion; a test that fails it is fixed or removed, not left enabled.

## Changes decided during the build (2026-09-28)

These were approved by the user while building and override the text below where they differ.

1. **QR tests run on a real device, not the emulator.** `IngredientBrandQrOverlayTests` needs a
   real SEPA QR code in front of the back camera (`src/androidTest/assets/sepa_payment_qr_poster.png`,
   for example opened full screen on a laptop). R16/R17/R17a now read "on a device whose camera
   sees the QR code". R18 now reads: when no QR code is detected within 20 s, the test is skipped
   with an `AssumptionViolatedException`; on BrowserStack this is always the case. There is no
   emulator check. Verified on a Samsung S22: 3/3 skipped without a QR code, 4/4 passed with one.
2. **capture-sdk fix: no loading indicator during the QR code education (R26).** The QR test
   found that the Gini mark (and, with the brand off, the default spinner) ran behind the QR
   education message and showed through its dim. iOS never shows an indicator there
   (`QRCodeOverlay.showAnimation` shows the education view or the indicator, on
   `release/GiniBankSDK_4.6.0` and on PP-3511). Fixed in `CameraFragmentExtension`
   (`shouldShowQrStepLoadingIndicator`) and `CameraFragmentImpl.analyzeQRCode`. This adds
   `capture-sdk:sdk` to the affected modules; no public API changes.
   - **R26 (MUST, happy):** Given the QR code education is shown on the camera screen, when the
     invoice is retrieved behind it, then no loading indicator is shown, with or without the
     brand. The retrieval half without education keeps its indicator.
3. **New QR test** `test4_qr_brandOff_educationShowsNoIndicator` (R26, brand off).
4. **Pre-existing SDK freeze (not fixed here).** Resuming the Analysis screen while the invoice
   education is showing freezes the main thread (see Open question 3). The education test classes
   wait for the education to end in `@After`. It needs its own bug ticket.

## Added after the first build: gaps from the PP-3476 test-case review (2026-09-28)

The review of the manual Xray cases listed acceptance criteria no case covered. These are the
ones a UI test can check; the visual ones (smooth loop without flicker, dark mode, 200% font) stay
manual.

- **R27 (MUST, happy):** Given the mock is armed with `["Analysis", "Foo"]`, when a PDF is imported,
  then the badge and the Gini mark are shown.
- **R28 (MUST, happy):** Given one test with the same app data, when the SDK is started with `[]`
  and then started again with `["Analysis"]`, then the first analysis shows no badge and the
  second one shows it. The reverse order (`["Analysis"]` then `[]`) hides it on the second start.
  This is "turning it on needs no SDK release".
- **R29 (MUST, error):** Given a first SDK start received `["Analysis"]`, when the next start's
  `/configurations` request fails, then the badge and the Gini mark are still shown (the saved
  value is kept).
- **R30 (MUST, error):** Given fresh app data, when `/configurations` fails, then no badge and the
  default indicator are shown.
- **R31 (MUST, happy):** Given the mock is armed with `["Analysis"]`, then the badge is not shown on
  the live camera screen, on the Review screen, on the No Results screen (analysis without
  extractions) or on the Error screen (analysis failure).
- **R32 (MUST, happy):** Given the badge is shown, then its content description is exactly
  "Powered by Gini", whatever the device language.

Mock additions: `UiTestMockBackend.configurationFails` (the `getConfiguration` callback fails) and
two scenarios, `NO_RESULTS` (no extractions) and `ANALYSIS_ERROR` (`analyze` fails). New test
class `IngredientBrandConfigurationTests` (R27–R31), `IngredientBrandTests.test11` (R32); both run in
`bs_run_group_ingredientbrand.sh`.

## Added: landscape + dark mode (2026-09-28)

The Xray cases' generic "check in landscape/portrait and dark/light mode" step means every case
in both modes. The portrait UI tests run in light mode; these cover the most important flows in
landscape + dark mode. Decided with the user: the SDK is opened in portrait (the example app's
main screen hides "Photo payment" in landscape) and the device is turned to landscape **on the
camera screen**, so the Analysis screen opens in landscape and is never rotated.
`LandscapeDarkModeRule` switches dark mode on before the app starts, turns the device on request,
and restores both afterwards. A device where dark mode cannot be switched on skips these tests.

- **R33 (MUST, happy):** Landscape + dark, photo without education, analysis past 5 s: badge in its
  designed place, Gini mark, then the first tip replaces the badge.
- **R34 (MUST, happy):** Landscape + dark, photo without education, analysis ending before the first
  tip: badge in its designed place and the Gini mark until the result, no tip.
- **R35 (MUST, happy):** Landscape + dark, photo with the invoice education: badge, no indicator, no
  tip (`IngredientBrandLandscapeEducationTests`, animations-on group).
- **R36 (SHOULD, happy):** Landscape + dark, QR code without education: badge and Gini mark on the
  overlay. **R37 (SHOULD, happy):** with the QR education: badge, no indicator. Both need a real QR
  code; a scan before the device turns skips the test (the turn would rebuild the camera under the
  QR step), and they always skip on BrowserStack.

Verified on a Samsung S22: R33–R35 pass with no retries; R36/R37 pass with a QR code in view.
Found while building: the QR-education check looked only for the 1.5 s intro text and could miss
it; it now also matches the two 3 s messages.

## Affected modules

- `bank-sdk:example-app` only.
  - `src/main` gets additive changes to the test-support mock (`uitestsupport` package).
  - `src/androidTest` gets the new tests, page object, configurator and script.
- No SDK module changes. `capture-sdk:sdk` is only read: its resource ids and strings are
  referenced from the tests.

## Public API impact

None. The feature has no integrator entry point: it is switched only by the backend
`/configurations` field `ingredientBrandScreens`, which these tests serve from the mock.
The example app is not a published artifact, and no SDK module is touched, so no `.api`
dump changes.

## Technical conventions

1. **Language.** All new code is Kotlin. No legacy Java file is touched.
   - `UiTestMockBackend`, `UiTestMockNetworkService` and `UiTestMockClientConfiguration` keep their
     current visibility (the network service is `internal`, the rest are used from androidTest so
     they stay public in the app module).
   - New androidTest classes follow the existing `ui/testcases`, `ui/screens`, `ui/resources`
     packages.
2. **UI.** No production UI is added or changed, and no XML layout is added or removed.
3. **Architecture.**
   - Tests use the existing page-object pattern: test class → screen object → UiAutomator /
     Espresso.
   - The new screen object is `AnalysisScreen` (precedent: `ExtractionScreen.waitForExtractionScreen`,
     which uses UiAutomator `waitForExists`).
   - The new configurator is `ScreenLoadingIndicatorConfigurator` (precedent:
     `PaymentHintConfigurator`, which calls `ConfigurationViewModel.setConfiguration` with a copy of
     `configurationFlow.value`).
4. **Wiring and async.**
   - No DI: the mock is a process-wide object, armed from the test body.
   - The delayed callback uses `Handler(Looper.getMainLooper()).postDelayed`, because
     `GiniCaptureDefaultNetworkService` delivers callbacks on `Dispatchers.Main`.
   - The mock's `CancellationToken.cancel()` removes the pending runnable.
   - No coroutines are needed.
   - This is test support in the example app, not SDK code. It follows the existing precedent
     (`UiTestMockBackend` is inert until a test arms it), so it does not break the rule against
     mocks in shipped SDK code.
5. **Strings and resources.**
   - No new strings.
   - Tests read capture-sdk resources through `net.gini.android.capture.R`, as
     `NoResultsTests` already does with `R.string.gc_supported_format_qr_code`.
   - UiAutomator resource ids go through `AppResources.resId(...)`.
   - Assertions on the content description read the string through the target context, so they
     work in the German default and the English locale.
6. **Quality gates.**
   - `./gradlew bank-sdk:example-app:ktlintCheck bank-sdk:example-app:detekt` must pass.
   - The build of `assembleDevExampleAppDebugAndroidTest` must pass.
   - Jacoco/Sonar do not measure androidTest UI suites here, so there is no coverage target for
     this ticket.

## Design

### Mock backend changes (`bank-sdk/example-app/src/main/java/net/gini/android/bank/sdk/exampleapp/uitestsupport/`)

- **`UiTestMockBackend.kt`**
  - `UiTestMockClientConfiguration` gets `val ingredientBrandScreens: Set<String> = emptySet()` and
    `val qrCodeEducationEnabled: Boolean = false`. Both are backend flags, so they belong with the
    other server flags.
  - `UiTestMockBackend` gets `@Volatile var analysisDelayMillis: Long = 0`. It is a transport
    behaviour, not a flag, so it stays out of the client configuration.
  - `arm(...)` gets an `analysisDelayMillis: Long = 0` parameter.
  - `disarm()` resets it to 0.
  - `networkService()` passes it to the service.
- **`UiTestMockNetworkService.kt`**
  - `getConfiguration` passes `ingredientBrandScreens = clientConfiguration.ingredientBrandScreens`.
    The field exists in `capture-sdk/sdk/src/main/java/net/gini/android/capture/internal/network/Configuration.kt:26`.
  - `analyze` delivers synchronously when the delay is 0. Otherwise it posts the same
    `callback.success(...)` with `postDelayed` and returns a token whose `cancel()` calls
    `removeCallbacks`.
  - `isQrCodeEducationEnabled` comes from `clientConfiguration.qrCodeEducationEnabled`. It is still
    `false` by default, so every existing mock test and every non-education test sees no education,
    and tips (images) and the QR retrieval state (QR) are what those tests see.

### Test base (`bank-sdk/example-app/src/androidTest/.../ui/testcases/WarningBottomSheetTestBase.kt`)

- `armMockBackend(...)` gets three optional parameters: `ingredientBrandScreens: Set<String> =
  emptySet()`, `qrCodeEducationEnabled: Boolean = false` and `analysisDelayMillis: Long = 0`. Both are passed through. The change is additive,
  so the existing callers are unchanged.
- The new test class extends this base, the same way `CreditNoteMockBackendTests` does. That gives
  it `RetryRule` (order `Int.MIN_VALUE`), the permission rules, `uploadFixtureInvoiceAndProcess`,
  `uploadFixturePdfAndProcess`, and `disarm()` in `tearDown`.

### New page object (`.../ui/screens/AnalysisScreen.kt`)

UiAutomator based, with explicit timeouts. It never uses a blind sleep.

| Method | What it does |
|---|---|
| `waitForAnalysisScreen(timeoutMs)` | Waits for `gc_analysis_overlay` |
| `isPoweredByGiniDisplayed()` | Checks the badge |
| `waitForPoweredByGiniGone(timeoutMs)` | Waits for the badge to disappear |
| `poweredByGiniBounds()` | Returns the badge bounds |
| `rootBounds()` | Returns the bounds of `gc_layout_root` |
| `analysisMessageBounds()` | Returns the bounds of `gc_analysis_message` |
| `isGiniLoadingIndicatorDisplayed()` | Looks it up by content description, `By.desc(<string>)` |
| `isDefaultLoadingIndicatorDisplayed()` | Looks for a `ProgressBar` under `gc_injected_loading_indicator_container` |
| `isCustomLoadingIndicatorDisplayed()` | Looks for `animationView` |
| `waitForTip(timeoutMs)` | Waits for `gc_analysis_hint_container` to be visible |
| `currentTipHeadline()` | Returns the text of `gc_analysis_hint_headline` |
| `waitForInvoiceEducation(timeoutMs)` | Waits for the intro or message text (`By.text`, Compose text is visible to UiAutomator) |
| `waitForInvoiceEducationGone(timeoutMs)` | Waits until neither education text is on screen |

- dp to px uses `targetContext.resources.displayMetrics.density`.
- The same badge id is used on the camera screen (`gc_fragment_camera.xml` includes
  `gc_powered_by_gini`), so the QR test reuses the badge and Gini-mark lookups.

### New configurator (`.../ui/resources/ScreenLoadingIndicatorConfigurator.kt`)

It sets `isScreenCustomLoadingIndicatorEnabled` through `ConfigurationViewModel`, exactly like
`PaymentHintConfigurator`. `ConfigurationViewModel.kt:234-235` then passes
`CustomLottiLoadingIndicatorAdapter(R.raw.custom_loading)` to the SDK. It is called before the
photo payment button is clicked.

### New test classes (`.../ui/testcases/`)

- **`IngredientBrandTests`** (extends `WarningBottomSheetTestBase`) covers R7–R15.
  - PDF cases use `uploadFixturePdfAndProcess("sample.pdf")`, which BrowserStack pre-loads as
    media.
  - Image cases use `uploadFixtureInvoiceAndProcess(CreditNoteFixtures.PLAIN_INVOICE_ASSET)`.
  - The mock ignores the bytes, so the document never affects the result.
- **`IngredientBrandEducationTests`** (extends `WarningBottomSheetTestBase`) covers R22–R25.
  - It takes the photo with the existing `captureScreen.clickCameraButton()` (used by
    `CaptureScreenTests` and `ErrorScreenTests`), then `reviewScreen.clickProcessButton()`.
  - The long test (R25) goes back from the extraction screen to the main screen between analyses
    (confidence: LOW — confirm which page-object call does this; `ExtractionScreen` may need a new
    `clickBack()`; see Open questions).
- **`IngredientBrandQrOverlayTests`** covers R16–R18 and R17a.
  - In `@Before`, it calls `Assume.assumeTrue(isEmulator)`, where `Build.HARDWARE` is `ranchu` or
    `goldfish`. It then opens the camera and waits up to 30 s for the QR detected popup.
  - Emulator setup: the AVD runs with `-camera-back virtualscene`, and a SEPA QR poster image is
    set as the virtual-scene wall poster (confidence: LOW — confirm the emulator lets a poster be
    set and the camera aimed at it without manual keyboard navigation; see Open questions).
  - A QR fixture image is added to `src/androidTest/assets/` (for example
    `sepa_payment_qr_poster.png`), with the setup steps in a KDoc on the class.

### BrowserStack (`bank-sdk/example-app/src/androidTest/scripts/`)

- A new `bs_run_group_ingredientbrand.sh`, copied from `bs_run_group_creditnote.sh`, calls
  `bs_build_and_upload.sh IngredientBrandTests IngredientBrandQrOverlayTests`.
- `bs_run_all_groups.sh` gets `run_group ingredientbrand IngredientBrandTests
  IngredientBrandQrOverlayTests` and `run_group ingredientbrand_education
  IngredientBrandEducationTests`.
- A new `bs_run_group_ingredientbrand_education.sh` calls `bs_build_and_upload.sh
  IngredientBrandEducationTests` with `BS_DISABLE_ANIMATIONS=false`.
- `bs_build_and_upload.sh` gets `BS_DISABLE_ANIMATIONS` (default `"true"`), used for the
  `disableAnimations` value in the build payload (today hard-coded at line 240). The default keeps
  every other group unchanged.
- `bs_build_and_upload.md` lists the new group.
- The device pair and the rest of the payload stay unchanged: orchestrator, `clearPackageData`.
- `clearPackageData` also wipes the persisted `ingredient_brand_screens` value between tests, so no
  test can see the previous test's flag.

## Test plan

The stack is JUnit4, AndroidX Test (ActivityScenario, orchestrator), Espresso and UiAutomator,
matching the neighbouring classes in `bank-sdk/example-app/src/androidTest`.

| Test class | New/extends | Tests | Covers |
|---|---|---|---|
| `IngredientBrandTests` | New, extends `WarningBottomSheetTestBase` | 10 | R1, R3, R4, R7–R15 |
| `IngredientBrandEducationTests` | New, extends `WarningBottomSheetTestBase` | 4 | R6a, R22–R25 |
| `IngredientBrandQrOverlayTests` | New, extends `WarningBottomSheetTestBase` | 3 | R16–R18, R17a |
| `CreditNoteMockBackendTests` | Existing, unchanged | 6 (re-run) | R2, R5, R6 |

`IngredientBrandTests`:
1. `test1_pdf_brandOn_showsBadgeInPositionAndGiniMark` — R1, R3, R7
2. `test2_pdf_brandOn_giniMarkHasTalkBackDescription` — R8
3. `test3_pdf_brandOff_showsDefaultIndicatorAndNoBadge` — R9
4. `test4_pdf_unknownValue_behavesLikeBrandOff` — R10
5. `test5_pdf_lowercaseAnalysis_showsBrand` — R11
6. `test6_image_brandOn_badgeHidesWhenFirstTipAppears` — R12
7. `test7_image_brandOff_tipsWithoutBadge` — R13
8. `test8_customIndicator_brandOn_giniMarkWins` — R14
9. `test9_customIndicator_brandOff_customIndicatorShown` — R15
10. `test10_closeDuringDelayedAnalysis_callbackNeverArrives` — R4: arm with `["Analysis"]` and 8 s,
    close the Analysis screen with its top-bar close button while the analysis is pending, wait
    10 s, then assert the main screen is still shown and the extraction screen never appeared.

The count is 10: 5 PDF cases, 2 image cases, 2 customization cases, and 1 cancellation case. Each test asserts one row of the manual Test Set PP-3740.

`IngredientBrandEducationTests`:
1. `test1_photo_brandOn_educationShowsBadgeWithoutIndicator` — R6a, R22
2. `test2_photo_brandOff_educationShowsNoBadgeNoIndicator` — R23
3. `test3_photo_brandOn_afterEducationMatchesBrandOff` — R24
4. `test4_photo_brandOn_educationLimitThenTipsHideBadge` — R25 (long test, 3 analyses, about
   60–90 s)

`IngredientBrandQrOverlayTests`:
1. `test1_qr_brandOn_overlayShowsBadgeAndGiniMark` — R16
2. `test2_qr_brandOff_overlayShowsNeither` — R17
3. `test3_qr_brandOn_educationShowsBadgeWithoutGiniMark` — R17a

R18 is proven by both tests showing as skipped on BrowserStack.

R19–R21 are process checks run by `/gini-build`: run the group script, record the build ids of 3
green runs in the PR description.

Every new Kotlin class gets a test. `AnalysisScreen` and `ScreenLoadingIndicatorConfigurator` are
exercised by `IngredientBrandTests`. The example app has no unit-test setup (only
`testImplementation(libs.junit)`), so the mock changes are proven through these instrumented tests,
as they are for the credit note mock.

### Not tested

- **The QR overlay on BrowserStack.** Camera image injection is not supported for Espresso there.
  It is covered by the local emulator test, `CameraFragmentExtensionTest` (unit), and the manual
  Xray cases.
- **mdpi–hdpi density buckets.** They are not available on BrowserStack real devices. The badge is
  one vector drawable, and `IngredientBrandLayoutTest`/`PoweredByGiniLayoutTest` cover the layouts.
  A manual or emulator check at 160/240 dpi remains.
- **Dark mode, 200% font, and the TalkBack reading order.** These are visual or assistive-tech
  checks that UiAutomator cannot judge. They stay in the manual Test Set PP-3740.
- **The animation itself** (smooth loop, no flicker). Animations are disabled on BrowserStack
  (`disableAnimations`), so only the mark's presence is asserted.
- **The QR education on BrowserStack.** It needs a real QR scan, so it is only covered by the
  local emulator test and the manual Xray cases.
- **The real backend configuration.** The decision was mock only. The existing real-backend suites
  run against `gini-mobile-test`, which PP-3476 states is configured with `["Analysis"]`, so they
  already cover the ticket's "existing tests still pass with the flag on" criterion when they run in
  the same release.
- **The payment hint bottom sheet with the brand on.** It is out of scope for PP-3478.

## Out of scope

- Any change to capture-sdk, bank-sdk or other SDK production code.
- iOS UI tests (PP-3477).
- Re-enabling the commented-out GitHub Actions UI-test workflow (`bank-sdk.check.ui-tests.yml`).
- Changing the BrowserStack device pair or adding low-density devices.
- Refactoring `WarningBottomSheetTestBase` into a more generally named base class.

## Open questions

1. **Emulator virtual scene for the QR test (R16, R17, R17a, confidence LOW).** Can the virtual-scene wall
   poster be set to our QR image, and the camera made to face it, from a script (for example
   `emulator -camera-back virtualscene` plus the poster setting) without manual keyboard navigation
   in the emulator window?
   - `/gini-build` must prove this first.
   - If it cannot be automated, the three QR tests are not written. Instead, the class KDoc and the
     spec record the manual emulator steps, and R16, R17 and R17a move to "Not tested". The tests must never
     be written to pass without a real QR detection.
2. **Tip timing on slow devices (R12).** The first tip appears 5 s after the Analysis screen starts
   (`AnalysisHintsAnimator.HINT_START_DELAY`). The "badge visible before the first tip" check must
   run inside that window. If BrowserStack devices are too slow to reach the Analysis screen and
   assert within ~4 s, R12's first check falls back to asserting that the badge was visible at the
   first poll, and the result is recorded in the PR.
3. **Education with animations on (R19a, confidence LOW).** Is the Compose education really
   skipped when BrowserStack disables animations, and does the education group run stably with
   `disableAnimations: false`?
   - `/gini-build` must check this first with one run of `IngredientBrandEducationTests` in each
     mode.
   - If the education is visible even with animations off, the separate group and the
     `BS_DISABLE_ANIMATIONS` variable are not added, and the class joins `bs_run_group_ingredientbrand.sh`.
   - **Answered 2026-09-28 on a Samsung S22 (Android 16):** with all three animation scales at 0,
     `test1` failed on every attempt with "Invoice education did not appear"; with scale 1 all 4
     education tests pass. So the separate group with `disableAnimations: false` is required.
   - Also found while building: the SDK freezes its main thread when the Analysis screen is
     resumed while the invoice education is showing. `showLoadingIndicator` waits on
     `educationMutex` inside `runBlocking`, and `releaseMutexForEducation()` is never called.
     The activity rule's teardown triggers it (it resumes the screen behind an empty test
     activity), so `IngredientBrandEducationTests` waits for the education to end in `@After`.
     This is a pre-existing capture-sdk bug on `main`, out of scope here; it needs its own ticket.
4. **Returning to the main screen in the long test (R25, confidence LOW).** Which existing
   page-object call goes back from the extraction screen to the main screen, or whether
   `ExtractionScreen` needs a new `clickBack()`.

## Implementation plan
- [x] 1. Mock backend: `ingredientBrandScreens`, `qrCodeEducationEnabled`, `analysisDelayMillis` with main-thread delayed, cancellable `analyze` (`bank-sdk:example-app` `uitestsupport/UiTestMockBackend.kt`, `UiTestMockNetworkService.kt`) (R1–R6, R6a)
- [x] 2. Test base: pass the new mock options through `armMockBackend(...)` (`WarningBottomSheetTestBase.kt`) (R1, R3, R6a)
- [x] 3. Page object `AnalysisScreen.kt` and `ScreenLoadingIndicatorConfigurator.kt` (R7–R15, R22–R25)
- [x] 4. `IngredientBrandTests` (10 tests), green on the local device (R4, R7–R15)
- [x] 5. Check open question 3 (education with animations off), then `IngredientBrandEducationTests` (4 tests), green on the local device (R22–R25)
- [x] 6. Check open question 1 (emulator virtual scene); write `IngredientBrandQrOverlayTests` (3 tests) or apply the documented fallback (R16–R18, R17a)
- [x] 7. BrowserStack scripts: `bs_run_group_ingredientbrand.sh`, `bs_run_group_ingredientbrand_education.sh`, `BS_DISABLE_ANIMATIONS` in `bs_build_and_upload.sh`, `bs_run_all_groups.sh`, `bs_build_and_upload.md` (R19, R19a, R20)
- [x] 8. Verification: ktlint, detekt, `assembleDevExampleAppDebugAndroidTest`; R21 (3 BrowserStack runs) is handed to the user (R21)
- [x] 9. Mock: `configurationFails`, scenarios `NO_RESULTS` and `ANALYSIS_ERROR` (R29–R31)
- [x] 10. `IngredientBrandConfigurationTests` (R27–R31) and `IngredientBrandTests.test11` (R32), green on the local device
- [x] 11. Add the new class to `bs_run_group_ingredientbrand.sh` and `bs_run_all_groups.sh`; re-run ktlint and the build (R19, R20)
- [x] 12. `LandscapeDarkModeRule`, `IngredientBrandLandscapeTests` (4) and `IngredientBrandLandscapeEducationTests` (1), green on the local device; both added to their BrowserStack groups (R33–R37)

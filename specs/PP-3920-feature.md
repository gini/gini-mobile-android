# PP-3920: [Android] UI Automation for Xray No Result Screen Test Suite

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-3920
Related: PP-3921 (iOS twin, Backlog) · PP-3869 (UI automation plan, "stays manual" rule) ·
Xray folder `/Banking Project/QAlity imported test cases/No Result screen` (12 tests, PP-3189..PP-3200)

## Problem

The Xray "No Result screen" folder holds 12 manual test cases for the capture-sdk No Results screen
(`NoResultsFragment`). Today only two of them are automated, both through a gallery/file import against
the real Gini API (`NoResultsTests.test1`/`test2`). Every release the remaining cases are executed by hand.
The PM wants the whole folder covered by Espresso tests in `bank-sdk:example-app` that run on
BrowserStack's agreed device matrix (Google Pixel 9-16.0 and Google Pixel 10 Pro-16.0, as fixed in
`bs_build_and_upload.sh`), the Android and iOS coverage aligned case by case, and a written proof for
every case that stays manual that it really cannot be automated.

Three facts shape the work:

1. The **Retake images** button exists only for camera-sourced documents
   (`NoResultsFragmentImpl.shouldAllowRetakeImages`), so every "retake" and "back to camera" case needs a
   camera photo, not an import.
2. BrowserStack **rejects camera image injection for Espresso** (documented in `bs_build_and_upload.sh`);
   the camera photographs the device rack. A rack photo against the real API is not guaranteed to produce
   "no results" (`ErrorScreenTests.test1` shows a blank image yields an *upload error* instead), so the
   camera-sourced cases are driven by the existing `UiTestMockBackend` scenario `NO_RESULTS`, which ignores
   the uploaded bytes and returns empty extractions. That is literally what PP-3195 asks for.
3. **QR codes cannot be injected either.** The repo precedent (`IngredientBrandQrOverlayTests`) runs such
   tests locally with a printed/on-screen SEPA QR code and *skips* them on BrowserStack via `Assume`.

## Xray case mapping (the deliverable table)

| Xray | Case | Decision | Android test (class.method) | iOS (PP-3921) |
| --- | --- | --- | --- | --- |
| PP-3189 | Invalid picture shows no result screen | automated, BrowserStack | `NoResultsMockBackendTests.test1_cameraPhoto_noResults_showsTipsRetakeAndEnterManually` | gap (iOS only has CX import variants) |
| PP-3190 | Image – Enter manually button | automated, BrowserStack (existing) | `NoResultsTests.test1_uploadInvalidImageAndClickEnterManuallyButton_NavigatesToMainScreen` (+ `NoResultsMockBackendTests.test3` for the camera variant) | gap |
| PP-3191 | Image – Retake images button | automated, BrowserStack | `NoResultsMockBackendTests.test2_cameraPhoto_retakeImages_returnsToCamera` | `GiniCXNoResultsUITests.testCXNoResultsScreenRetryAction` (CX, import) |
| PP-3192 | Invalid PDF shows no result screen | automated, BrowserStack (existing) | `NoResultsTests.test2_uploadInvalidPdfAndClickEnterManuallyButton_NavigatesToMainScreen` | `GiniCXNoResultsUITests.testCXNoResultsScreenIsDisplayedWhenNoExtractions` (CX) |
| PP-3193 | PDF – Enter manually button | automated, BrowserStack (existing) | `NoResultsTests.test2_…` (same method, second half) | gap |
| PP-3194 | Invalid QR code detected | automated, **local device only** (skips on BrowserStack) | `NoResultsMockBackendTests.test6_qr_noResults_showsQrHeaderFormatsRetakeAndEnterManually` | gap |
| PP-3195 | Testing via API (empty extractions) | automated, BrowserStack | `NoResultsMockBackendTests.test1_…` (mock `NO_RESULTS` = empty `extractions`/`candidates`) | gap |
| PP-3196 | QR code – Enter manually button | automated, **local device only** | `NoResultsMockBackendTests.test7_qr_enterManually_returnsToMainScreen` | gap |
| PP-3197 | QR code – Retake images button | automated, **local device only** | `NoResultsMockBackendTests.test8_qr_retakeImages_returnsToCamera` | gap |
| PP-3198 | Disable QR code scanning | **stays manual** | — | — |
| PP-3199 | Scan Gini QR code | **stays manual** | — | — |
| PP-3200 | Back to Camera button | automated, BrowserStack | `NoResultsMockBackendTests.test4_cameraPhoto_navigationBack_returnsToCamera` (+ `test5` system back) | `GiniCXNoResultsUITests.testCXNoResultsScreenRetryAction` (nav back fallback) |

**Why PP-3198 and PP-3199 stay manual** (PP-3869 rule: real camera needed, no screen assertion proves them):

- PP-3198 expects "every other payment QR code will not be scannable, Gini QR code will be scannable" with
  QR scanning disabled. Proving a *negative* needs a non-Gini payment QR held in front of a real camera for a
  timeout and a Gini QR for the positive half; neither can be injected on BrowserStack, and the case is about
  the camera screen's scanning behaviour, not the No Results screen.
- PP-3199 expects a `https://pay.gini.net/*` QR to be parsed (`GiniPaymentParser`) **and** "exclude the
  paymentdata field from the request JSON" – an assertion on the outgoing API request body. A UI test
  cannot observe the request JSON, and the mock backend bypasses it entirely.

**Why PP-3194/3196/3197 are local-only:** they need a SEPA payment QR in front of the camera. On
BrowserStack the camera sees the device rack, the "QR code detected" popup never appears, and the tests
end as *skipped* (never passed, never failed), exactly like `IngredientBrandQrOverlayTests`.

## Requirements

All strings are asserted by capture-sdk resource id (`net.gini.android.capture.R.string.*`), never by
literal text, so the tests pass in every device locale. English values are quoted for readability only.

- **R1 (MUST, entry):** Given the test APK built by `bs_build_and_upload.sh`, when
  `bs_run_group_extraction.sh` (or `bs_run_all_groups.sh`) is run, then both `NoResultsTests` and
  `NoResultsMockBackendTests` are in the "extraction" shard's class list and execute on
  `Google Pixel 9-16.0` and `Google Pixel 10 Pro-16.0`.
- **R2 (MUST, entry):** Given a local device/emulator, when
  `./gradlew bank-sdk:example-app:connectedDevExampleAppDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.gini.android.bank.sdk.exampleapp.ui.testcases.NoResultsMockBackendTests`
  is run, then the class runs under the Test Orchestrator with no other test class in the filter
  (AGENTS.md gotcha: two comma-separated classes start 0 tests).
- **R3 (MUST, happy – PP-3189, PP-3195):** Given `UiTestMockBackend` armed with `NO_RESULTS`, when the
  user takes a camera photo and taps Process on the Review screen, then within 30 s the No Results screen
  shows the title `gc_title_no_results`, the header `gc_noresults_header` ("Could not retrieve any
  information from your documents."), the tips header `gc_useful_tips`, the five photo-tip titles
  (`gc_photo_tip_good_lighting_title`, `gc_photo_tip_flatten_the_page_title`,
  `gc_photo_tip_parallel_title`, `gc_photo_tip_align_title`, `gc_photo_tip_multiple_pages_title`),
  the **Retake images** button `gc_noresults_retake_images` and the **Enter manually** button
  `gc_noresults_enter_manually`.
- **R4 (MUST, happy – PP-3191):** Given the No Results screen of R3, when the user taps Retake images,
  then the camera screen is shown (`CaptureScreen.checkScanTextDisplayed()` returns `true`, i.e. the
  shutter button `gc_button_camera_trigger` exists within 5 s).
- **R5 (MUST, happy – PP-3190 camera variant):** Given the No Results screen of R3, when the user taps
  Enter manually, then the example app's main screen is shown
  (`MainScreen.assertDescriptionTitle()` returns `true`).
- **R6 (MUST, happy – PP-3200):** Given the No Results screen of R3, when the user taps the top
  navigation bar's back button (content description `gc_back_button_description`), then the camera screen
  is shown (as in R4).
- **R7 (SHOULD, happy – PP-3200):** Given the No Results screen of R3, when the user presses the system
  back button, then the camera screen is shown (`NoResultsFragmentImpl.handleOnBackPressed` routes it to
  the camera).
- **R8 (MUST, happy – PP-3190, PP-3192, PP-3193; existing):** Given the real Gini API, when
  `image-no-result.png` is imported from Photos (test1) or `test-pdf-no-results.pdf` from Files (test2),
  then the No Results screen shows the header and list already asserted today, and Enter manually
  returns to the main screen. These two tests keep passing unchanged in behaviour.
- **R9 (MUST, happy – documents the Retake rule):** Given the No Results screen reached by an import
  (R8, either test), then the Retake images button is **not** displayed
  (`NoResultScreen.checkElementWithTextIsDisplayed(gc_noresults_retake_images)` returns `false`).
- **R10 (MUST, happy – PP-3194, local only):** Given the mock armed with `NO_RESULTS` and
  `qrCodeEducationEnabled = false`, when the camera detects a SEPA payment QR code
  (`AnalysisScreen.waitForQrCodeDetected(20_000)` is `true`), then within 30 s the No Results screen shows
  the QR header `gc_noresults_header_qr` ("The QR code does not contain payment information."), the
  supported-formats section header `gc_supported_format_section_header`, the Gini payment QR row
  `gc_supported_format_qr_type_gini_payment`, the Retake images button and the Enter manually button.
- **R11 (MUST, happy – PP-3196, local only):** Given the QR No Results screen of R10, when the user taps
  Enter manually, then the main screen is shown (as in R5).
- **R12 (MUST, happy – PP-3197, local only):** Given the QR No Results screen of R10, when the user taps
  Retake images, then the camera screen is shown (as in R4).
- **R13 (MUST, error – skip, not fail):** Given no QR code is detected within 20 s (every BrowserStack
  run), when any of the R10–R12 tests runs, then it ends with `AssumptionViolatedException` (reported as
  *skipped*, not retried by `RetryRule`, never passed).
- **R14 (MUST, error – isolation):** Given any `NoResultsMockBackendTests` test ends (pass, fail or skip),
  when the next test starts, then `UiTestMockBackend.isArmed` is `false` (the base class's `tearDown`
  disarms it) so no canned response leaks into a real-backend test.
- **R15 (MUST, async):** Given the analysis is still pending, when a test asserts on the No Results screen,
  then it first waits for `gc_title_no_results` with `NoResultScreen.waitForNoResultScreen` (30 s
  UiAutomator wait) instead of relying on the fixed `SimpleIdlingResource` sleep.
- **R16 (MUST, entry – shard bookkeeping):** Given `bs_run_all_groups.sh`'s header rule ("every test class
  appears in exactly one shard"), when the new class is added, then it is listed in the `extraction`
  group of `bs_run_all_groups.sh` and in `bs_run_group_extraction.sh`, and `bs_build_and_upload.md` gains a
  "No Result screen (PP-3920)" shard row.
- **R17 (MUST, entry – Jira):** Given the mapping table above, when the implementation is done, then
  (a) the table is posted as a comment on PP-3920 and PP-3921, and the PP-3920 comment explicitly asks the
  reporter (Qazi Naveed) and the test author (Valentina Iancu) to confirm the two stays-manual reasons;
  (b) each of the 12 Xray tests gets labels: `android-automated` on PP-3189–3197 and PP-3200,
  additionally `android-local-only` on PP-3194/3196/3197, and `android-stays-manual` on PP-3198/3199.
  The field "Test case can be automated" (`customfield_10574`, Yes/No select) is **not** used: it exists
  on the Story screen only, not on the Test issue type in PP (verified via create/edit metadata on
  2026-10-27). Comment and labels are drafted first and **posted only after the user confirms**.
  Implementation does not wait for QA's answer.
- **R18 (MUST, entry – release sign-off):** Given `bs_run_release.sh`, when the change lands, then its
  RELEASE SCOPE block contains `run_group "noresults" NoResultsTests NoResultsMockBackendTests` and the
  header "Scope" list names it, with a comment that the three QR tests auto-skip on BrowserStack (same
  wording pattern as the existing `ErrorScreenTests#test2` note). `RELEASE_VERSION` is not touched.

## Affected modules

- `bank-sdk:example-app` — `src/androidTest` only: one new test class, one existing test class extended,
  one page object extended, three BrowserStack scripts/docs updated. **No change to `src/main`**: the mock
  backend (`uitestsupport/UiTestMockBackend.kt`, `UiTestMockNetworkService.kt`) already has the
  `NO_RESULTS` scenario.
- `capture-sdk:sdk`, `bank-sdk:sdk` — read only (the screen under test). No change.

## Public API impact

None. No production source set changes; no `api/*.api` dump is touched.

## Technical conventions

1. **Language:** Kotlin, test source set `bank-sdk/example-app/src/androidTest/java/...`. Test classes
   are plain public classes like their neighbours (Espresso needs them public). No legacy Java is touched
   (`NoResultsFragment.java`/`NoResultsFragmentImpl.java` stay as they are; the tests reach them through
   the example app only).
2. **UI:** no UI is added or changed; no XML layouts added/removed; no Compose.
3. **Architecture (test code):** page-object pattern as in `ui/screens/*` + test classes in
   `ui/testcases/*`. The mock-driven class **extends `WarningBottomSheetTestBase`** (the precedent for
   mock-backend classes: `CreditNoteMockBackendTests`, `IngredientBrandQrOverlayTests`) and uses its
   `armMockBackend(...)`, `takePhotoAndProcess()`, rules and page objects. The existing `NoResultsTests`
   keeps its own rules (it does not extend the base, by decision in step 3).
4. **DI / async:** none. `UiTestMockBackend` is armed from the test body before the photo-payment button is
   tapped (its documented contract) and disarmed in the base `tearDown`. Waits use UiAutomator
   `waitForExists`/polling helpers that already exist (`NoResultScreen.waitForNoResultScreen`,
   `AnalysisScreen.waitForQrCodeDetected`), never `Thread.sleep` loops of our own.
5. **Strings/resources:** no new strings. Assertions use `net.gini.android.capture.R.string.*` ids
   (locale-independent). The nav-bar back button is matched by
   `withContentDescription(R.string.gc_back_button_description)` (precedent `HelpScreen.clickBackButton`).
   No new assets: `sepa_payment_qr_poster.png` (QR), `image-no-result.png`, `test-pdf-no-results.pdf`
   already exist under `src/androidTest/assets`.
6. **Quality gates:** CI runs ktlint/detekt/lint for `bank-sdk:sdk` only and assembles the example app
   (`bank-sdk.check.yml`), so the gate for this change is `bank-sdk:example-app:assembleDevExampleAppDebug`
   + `assembleDevExampleAppDebugAndroidTest` compiling, plus the instrumented run itself. Jacoco/Sonar do not
   cover `androidTest`; no coverage target applies. Keep the ktlint style anyway (the test files are
   formatted like their neighbours).
7. **Comments in test code** follow the suite's convention: a KDoc on the class explaining the
   mock/skip strategy, and a one-line `/** PP-xxxx: … */` on each test naming the Xray case.

## Design

### `NoResultScreen` page object (extend) — `ui/screens/NoResultScreen.kt`

Existing: `waitForNoResultScreen(resId)`, `checkElementWithTextIsDisplayed(resId)` (null-safe, returns
`false` for a GONE/missing view), `clickEnterManuallyButton()`. Add:

- `clickRetakeImagesButton()` — `onView(withText(R.string.gc_noresults_retake_images)).perform(click())`.
- `clickNavigationBackButton()` — `onView(withContentDescription(R.string.gc_back_button_description)).perform(click())`
  (the No Results top bar is the SDK's `NavigationBarTopAdapter` with `NavButtonType.BACK`, see
  `NoResultsFragmentImpl.setupTopBarNavigation` and `NavigationBarTopAdapter.kt:108`).
- `pressSystemBack()` — `UiDevice.pressBack()`.

### `NoResultsMockBackendTests` (new) — `ui/testcases/NoResultsMockBackendTests.kt`

`class NoResultsMockBackendTests : WarningBottomSheetTestBase()`; page objects come from the base
(`mainScreen`, `onboardingScreen`, `captureScreen`, `reviewScreen`, `idlingResource`) plus a private
`NoResultScreen()` and `AnalysisScreen()`.

Flow for the camera tests (R3–R7): `armMockBackend(UiTestMockScenario.NO_RESULTS)` →
`takePhotoAndProcess()` (base: photo-payment button, skip onboarding, shutter, Review, Process) →
`noResultScreen.waitForNoResultScreen(gc_title_no_results)` → assertions / button tap → target-screen
assertion. Path through the SDK: `AnalysisScreenPresenterExtension` → `onProceedToNoExtractionsScreen` →
`GiniCaptureFragment.kt:325` → `AnalysisFragmentDirections.toNoResultsFragment(document)`; the document is
camera-sourced so `shouldAllowRetakeImages()` is `true`.

Flow for the QR tests (R10–R13): `armMockBackend(NO_RESULTS, qrCodeEducationEnabled = false)` →
photo-payment button → skip onboarding →
`Assume.assumeTrue(msg, analysisScreen.waitForQrCodeDetected(QR_DETECTION_TIMEOUT_MS = 20_000))` →
wait for No Results → assertions. SDK path: `CameraFragmentImpl.java:1452-1462` navigates to
`CameraFragmentDirections.toNoResultsFragment(qrCodeDocument)` when the analysis result's extractions
are empty; `QRCodeDocument` is created with `Source.newCameraSource()` so Retake is shown, and
`NoResultsFragmentImpl.onCreateView` swaps the header to `gc_noresults_header_qr` and the list to
`SupportedFormatsAdapter(true)` (QR formats, incl. `QR_GINI_PAYMENT`).

Test methods (8): `test1_cameraPhoto_noResults_showsTipsRetakeAndEnterManually` (R3),
`test2_cameraPhoto_retakeImages_returnsToCamera` (R4), `test3_cameraPhoto_enterManually_returnsToMainScreen`
(R5), `test4_cameraPhoto_navigationBack_returnsToCamera` (R6), `test5_cameraPhoto_systemBack_returnsToCamera`
(R7), `test6_qr_noResults_showsQrHeaderFormatsRetakeAndEnterManually` (R10),
`test7_qr_enterManually_returnsToMainScreen` (R11), `test8_qr_retakeImages_returnsToCamera` (R12).
A private `openCameraAndScanQrCode()` with the `Assume` mirrors `IngredientBrandQrOverlayTests`.

### `NoResultsTests` (extend) — `ui/testcases/NoResultsTests.kt`

Keep both tests; add to each the R9 assertion
`assertEquals(false, noResultScreen.checkElementWithTextIsDisplayed(gc_noresults_retake_images))` right
after the Enter manually visibility check, and make `test2` call `waitForNoResultScreen` before its first
assertion (R15; `test1` already does).

### BrowserStack scripts/docs

- `scripts/bs_run_group_extraction.sh`: append `NoResultsMockBackendTests`.
- `scripts/bs_run_all_groups.sh`: append `NoResultsMockBackendTests` to `run_group "extraction"`.
- `scripts/bs_build_and_upload.md`: add a "No Result screen shard (PP-3920)" table row under the
  ingredient-brand section, noting that the three QR tests always show as **skipped** on BrowserStack.
- `scripts/bs_run_release.sh`: add the `noresults` run_group to the RELEASE SCOPE block and the header
  scope list (R18). Nothing else in that file changes.

### Jira (R17, after user confirmation)

Comment with the mapping table on PP-3920 (plus the QA confirmation request) and PP-3921
(`addCommentToJiraIssue`); add the labels of R17(b) to PP-3189..PP-3200 with `editJiraIssue`
(`labels` is editable on the Test issue type; merge with existing labels, never replace).

## Test plan

The deliverable *is* the instrumented tests. No production class is added, so no unit test is added
(there is no `src/main` change to cover). Stack: JUnit4 + Espresso + UiAutomator + `ActivityScenarioRule`
+ `RetryRule` + `UiTestMockBackend`, matching every neighbour in `ui/testcases`.

| Test class | New / extends | Tests | Requirements |
| --- | --- | --- | --- |
| `NoResultsMockBackendTests` (new, extends `WarningBottomSheetTestBase`) | new | 8 | R3–R7, R10–R15 |
| `NoResultsTests` (existing) | extended | 2 (unchanged count, 2 extra assertions each) | R8, R9, R15 |

Eight tests for a multi-path screen (camera / QR × four exits) is within the "6–12" band.

Verification steps for /gini-build (in this order):

1. `./gradlew bank-sdk:example-app:assembleDevExampleAppDebug bank-sdk:example-app:assembleDevExampleAppDebugAndroidTest`
   compiles.
2. Local run, one class per invocation (R2):
   `NoResultsMockBackendTests` then `NoResultsTests`. With a SEPA QR (`sepa_payment_qr_poster.png` opened
   full screen on a laptop) in front of the camera, test6–8 pass; without it they are reported skipped.
3. BrowserStack: `BS_USER`/`BS_KEY` are present in the user's shell environment (verified in this session),
   so run `scripts/bs_run_group_extraction.sh` and check the build in project `gini-mobile-android`: the
   camera tests pass on both devices, the three QR tests show as skipped (R1, R13).
4. Jira updates (R17) only after showing the drafts to the user.

Verification risk to check in step 3, not a decision: the two real-backend fixtures
(`image-no-result.png`, `test-pdf-no-results.pdf`) are not in the script's `uploadMedia` list; the tests
copy them onto the device through MediaStore themselves. If `NoResultsTests.test2` cannot find the PDF in
the Files picker on BrowserStack, add the PDF to the upload list in `bs_build_and_upload.sh` the same way
`sample.pdf` is handled, and report it in the PR.

### Not tested

- The SDK's own No Results logic (`shouldAllowRetakeImages`, QR header swap) is covered here only through
  the UI; no capture-sdk unit test is added (out of scope, no production change).
- PP-3198 and PP-3199 stay manual (reasons above).
- The QR tests never run on BrowserStack (skipped); they need a local device with a QR in view.
- Landscape/dark-mode variants of the No Results screen are not part of this folder (they live in the WCAG
  folder) and are not added.
- Analytics events (`UserAnalyticsEvent.RETAKE_IMAGES_TAPPED`, `ENTER_MANUALLY_TAPPED`, `CLOSE_TAPPED`)
  are not asserted (analytics backend = stays manual per PP-3869).

## Out of scope

- Any change to `capture-sdk`, `bank-sdk:sdk` or the example app's `src/main`.
- Camera image injection or a QR-injection workaround for BrowserStack.
- Automating PP-3198 / PP-3199.
- iOS implementation (PP-3921): only the gap list is produced.
- Changing `RELEASE_VERSION` or any shard other than the new `noresults` group in `bs_run_release.sh`.
- Moving the suite to Appium for camera/QR injection (evaluated 2026-10-27: injection is unavailable for
  Espresso on BrowserStack; an Appium port would be a full rewrite and is a separate decision).
- Re-enabling the commented-out emulator job in `bank-sdk.check.ui-tests.yml`.

## Open questions

None. (The Jira field question was resolved on 2026-10-27: the field is Story-only, labels are used
instead — see R17.)

## Implementation plan
- [x] 1. Extend `NoResultScreen` page object (`bank-sdk/example-app/src/androidTest/java/.../ui/screens/NoResultScreen.kt`): `clickRetakeImagesButton()`, `clickNavigationBackButton()`, `pressSystemBack()` (requirements 4, 6, 7, 12)
- [x] 2. New `NoResultsMockBackendTests` extending `WarningBottomSheetTestBase` (`.../ui/testcases/NoResultsMockBackendTests.kt`): 5 camera tests + 3 QR tests with `Assume` skip (requirements 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15)
- [x] 3. Extend `NoResultsTests` (`.../ui/testcases/NoResultsTests.kt`): Retake-not-shown assertion in both tests, `waitForNoResultScreen` in test2 (requirements 8, 9, 15)
- [x] 4. Shard bookkeeping (`src/androidTest/scripts/`): add `NoResultsMockBackendTests` to `bs_run_group_extraction.sh` and `bs_run_all_groups.sh`; "No Result screen shard (PP-3920)" row in `bs_build_and_upload.md` (requirements 1, 16)
- [x] 5. `bs_run_release.sh`: `run_group "noresults" NoResultsTests NoResultsMockBackendTests` in RELEASE SCOPE + header Scope entry + QR auto-skip note (requirement 18)
- [x] 6. Verify: `assembleDevExampleAppDebug` + `assembleDevExampleAppDebugAndroidTest` compile; local instrumented run of `NoResultsMockBackendTests` then `NoResultsTests` if a device is attached; BrowserStack extraction shard (requirements 1, 2, 13)
- [x] 7. Jira drafts (comment with mapping table for PP-3920/PP-3921 + labels for PP-3189..PP-3200) shown to the user; posted only after confirmation (requirement 17)

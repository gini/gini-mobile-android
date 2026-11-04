# PP-3918: [Android] UI Automation for Xray Error Screen Test Suite

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-3918
Epic: https://ginis.atlassian.net/browse/PP-2671
iOS twin: https://ginis.atlassian.net/browse/PP-3919 (Backlog)
Manual test basis: Xray folder `/Banking Project/QAlity imported test cases/Error & No result screens`
(22 Test issues, PP-2895 – PP-2917, PP-2905 does not exist)

Scope note: this spec covers both the Error screens and the No Results screens. The Jira title says
only "Error Screen" because the team asked for that name; the ticket description says so.

## Problem

The PM wants the 22 manual cases in the Xray folder "Error & No result screens" covered by UI tests
that run on BrowserStack, so error-screen behaviour is checked on every release without a manual
run.

Some of the work is already done. Seven of the 22 cases already have a test in
`bank-sdk/example-app/src/androidTest`, partly through the smoke suite, which has its own Xray
copies of some of these checks (PP-3434 – PP-3440, folder "Smoke Test Suite 2026"). But three of
those seven only run locally: they turn the internet off with `svc wifi disable`, which BrowserStack
cloud devices block, so the test skips itself there (`ErrorScreenTests.skipIfNetworkCouldNotBeDisabled`).
The other 15 cases have no test.

Most uncovered cases need a specific backend failure (401, 503, …) that a device cannot produce on
demand. The example app already has a test-only mock backend (`UiTestMockBackend`) with an
`ANALYSIS_ERROR` scenario, but it always fails with a plain message, which the SDK maps to the
generic error. This spec lets the mock fail with a chosen HTTP status code or a "no connection"
cause. Each error screen then becomes a deterministic test that also runs on BrowserStack.

Decisions from the clarifying questions:
- **Trigger: extend the mock.** The mock's analysis call fails with a chosen status code or an
  `UnknownHostException`. The real offline tests in `ErrorScreenTests` stay for local runs.
- **"Login Error Screens" (PP-2911 – PP-2917, done with Charles) map to the same tests** as the
  matching error type. The SDK uses one mapping (`ErrorType.typeFromError`) for every request, so a
  device test cannot tell a login failure from an analysis failure. The login request itself is not
  exercised (see "Not tested").
- **PP-2901 (device on a table):** take a real camera photo, with the mock answering 400.
- **PP-2907 ("not an invoice" → dialog)** maps to the existing `NoResultsTests#test2`: on Android, a
  valid non-invoice PDF opens No Results, not a dialog.
- **PP-2910 (non-PDF file):** the SDK's Files picker only offers PDF/XML
  (`FileChooserFragment.kt:443`), so an unsupported file can only arrive through "Open with". There
  the SDK rejects it, and the example app shows a Toast and finishes `MainActivity` — through
  `onCaptureResult` with the default Intent-based API, or `abortOpenWith` with the Uri-based one.
  No SDK dialog exists on that path. The test checks that the capture flow never starts. It does not
  read the Toast.
- **PP-2908 (> 10 MB):** the test generates a padded PDF just over `FILE_SIZE_LIMIT` (10 485 760
  bytes) at runtime. No large asset is committed.
- **BrowserStack:** a new group script `bs_run_group_errorscreens.sh`, added to
  `bs_run_all_groups.sh`.
- **iOS alignment:** this spec carries the case → test mapping table below. iOS can reuse it for
  PP-3919. No iOS work here.
- **Reuse first:** cases that already have a test get no new test. Only their KDoc gets the Xray key.

## Case → test mapping

| Xray | Case | Test | Status |
|---|---|---|---|
| PP-2895 | Network connection error | `ErrorAndNoResultScreenMockBackendTests#test1` (BS) + existing `ErrorScreenTests#test2` (local) | new + existing |
| PP-2896 | Upload error | existing `ErrorScreenTests#test1` (real backend) + `ErrorAndNoResultScreenMockBackendTests#test2` | existing + new |
| PP-2897 | Auth error | `ErrorAndNoResultScreenMockBackendTests#test3` | new |
| PP-2898 | Generic error | `ErrorAndNoResultScreenMockBackendTests#test4` | new |
| PP-2899 | Server error | `ErrorAndNoResultScreenMockBackendTests#test5` | new |
| PP-2900 | Maintenance | `ErrorAndNoResultScreenMockBackendTests#test6` | new |
| PP-2901 | Upload error (device on table) | `ErrorAndNoResultScreenMockBackendTests#test2` (camera photo + 400) | new |
| PP-2902 | Back to Camera button | `ErrorAndNoResultScreenMockBackendTests#test8` (BS) + existing `ErrorScreenTests#test4` (local) | new + existing |
| PP-2903 | Error screen without internet | `ErrorAndNoResultScreenMockBackendTests#test1` (BS) + existing `ErrorScreenTests#test2` (local) | new + existing |
| PP-2904 | No Results for non-invoice picture | `ErrorAndNoResultScreenMockBackendTests#test9` (camera) + existing `NoResultsTests#test1` (upload) | new + existing |
| PP-2906 | Password-protected file | existing `FileImportErrorDialogTests#test1` (= smoke PP-3439) | existing |
| PP-2907 | Invalid file (not an invoice) | existing `NoResultsTests#test2` (= smoke PP-3436) | existing, adapted |
| PP-2908 | File > 10 MB | `FileImportErrorDialogTests#test4` | new |
| PP-2909 | Too many pages | existing `FileImportErrorDialogTests#test2` | existing |
| PP-2910 | Unsupported file type | `OpenWithTest#opening_unsupported_file_with_MainActivity_does_not_start_Bank_SDK` | new, adapted |
| PP-2911 | Login: upload problem | `ErrorAndNoResultScreenMockBackendTests#test2` | new (shared) |
| PP-2912 | Login: authentication | `ErrorAndNoResultScreenMockBackendTests#test3` | new (shared) |
| PP-2913 | Login: service unavailable | `ErrorAndNoResultScreenMockBackendTests#test7` | new |
| PP-2914 | Login: upload problem (second flow) | `ErrorAndNoResultScreenMockBackendTests#test2` | new (shared) |
| PP-2915 | Login: maintenance | `ErrorAndNoResultScreenMockBackendTests#test6` | new (shared) |
| PP-2916 | Login: unexpected response | `ErrorAndNoResultScreenMockBackendTests#test5` | new (shared) |
| PP-2917 | Login: unexpected (generic) | `ErrorAndNoResultScreenMockBackendTests#test4` | new (shared) |

No case stays fully manual. The parts that stay manual are listed under "Not tested".

## Requirements

Definitions:
- *Error screen of type T*: the SDK `ErrorFragment` showing the title text "Error", header
  `gc_error_header` = the English string of `T.titleTextResource`, body `gc_error_textview` = the
  English string of `T.descriptionTextResource` (`capture-sdk/sdk/src/main/res/values-en/strings.xml`),
  and the buttons "Enter manually" (`gc_noresults_enter_manually`) and "Back To Camera"
  (`gc_error_back_to_camera`).
- *Mock armed with failure F*: `UiTestMockBackend` armed with scenario `ANALYSIS_ERROR` and
  `analysisFailure = F`, before the photo payment button is clicked.
- *Camera photo processed*: `WarningBottomSheetTestBase.takePhotoAndProcess()`.

### Mock backend (test support)

- **R1 (MUST, entry):** Given a test arms the mock with `ANALYSIS_ERROR` and failure `HTTP(code)`,
  when the SDK calls `analyze`, then the callback's `Error` has `statusCode == code` and
  `ErrorType.typeFromError` maps it to the expected type: 400 → `UPLOAD`, 401 → `AUTH`, 500 →
  `OUTAGE`, 502 → `SERVER`, 503 → `MAINTENANCE`.
- **R2 (MUST, entry):** Given failure `NO_CONNECTION`, when `analyze` fails, then the `Error`'s
  cause is an `UnknownHostException` and has no status code, so it maps to `NO_CONNECTION`.
- **R3 (MUST, entry):** Given failure `GENERIC`, or a test that arms `ANALYSIS_ERROR` without naming
  a failure, when `analyze` fails, then the `Error` has no status code and no cause, so it maps to
  `GENERAL`. This is today's behaviour, so existing users of `ANALYSIS_ERROR` stay unchanged.
- **R4 (MUST, entry):** Given a test that never arms the mock, then the app uses the real network
  service (unchanged `ConfigurationViewModel` wiring), and `disarm()` resets the failure to the
  default.

### Error screens (mock, run on BrowserStack)

- **R5 (MUST, happy):** Given the mock is armed with `NO_CONNECTION`, when a camera photo is
  processed, then the error screen of type `NO_CONNECTION` is shown ("There was a problem connecting
  to the internet" / "Please check your internet connection and try again later on."). Covers
  PP-2895, PP-2903.
- **R6 (MUST, happy):** Given `HTTP(400)`, when a camera photo is processed, then the error screen
  of type `UPLOAD` is shown. Covers PP-2896, PP-2901, PP-2911, PP-2914.
- **R7 (MUST, happy):** Given `HTTP(401)`, then type `AUTH` ("There has been an authentication
  error" / "Upload failed due to authentication error. Please try again later."). Covers PP-2897,
  PP-2912.
- **R8 (MUST, happy):** Given `GENERIC`, then type `GENERAL` ("An unexpected error occurred." /
  "Something went wrong. Please retry shortly. …"). Covers PP-2898, PP-2917.
- **R9 (MUST, happy):** Given `HTTP(502)`, then type `SERVER` (same title as R8, body "… There was
  an unexpected response from the service. …"). The test fails if the body equals the R8 body.
  Covers PP-2899, PP-2916.
- **R10 (MUST, happy):** Given `HTTP(503)`, then type `MAINTENANCE` ("Maintenance is in progress." /
  "We are maintaining our system. …"). Covers PP-2900, PP-2915.
- **R11 (MUST, happy):** Given `HTTP(500)`, then type `OUTAGE` ("Service is unavailable." / "The
  service is unavailable. Please try again later. …"). Covers PP-2913.
- **R12 (MUST, happy):** Given the error screen of R6 is shown, when "Back To Camera" is clicked,
  then the camera screen is shown (`CaptureScreen.checkScanTextDisplayed()` is true). Covers PP-2902.
- **R13 (MUST, happy):** Given the mock is armed with scenario `NO_RESULTS`, when a camera photo is
  processed, then the No Results screen is shown with title `gc_title_no_results`, header
  `gc_noresults_header` and the "Enter manually" button. Covers PP-2904 (camera half).

### File import

- **R14 (MUST, error):** Given a PDF file of `FILE_SIZE_LIMIT + 1 MB` bytes in Downloads, when it is
  picked through Files on the camera screen, then the file import dialog shows
  `gc_error_file_import_size_title` ("This file is too large."), and the Analysis screen never
  opens. Covers PP-2908.
- **R15 (MUST, error):** Given the example app is launched through `ACTION_VIEW` on `MainActivity`
  with a `text/plain` file, when the SDK validates it, then the capture flow never starts: within
  10 s the activity is finished (scenario state `DESTROYED`) and `recyclerview_extractions` was
  never displayed. Covers PP-2910 (adapted).

### Existing tests (reuse)

- **R16 (MUST, entry):** The KDoc of each existing test that covers a case names the Xray key(s):
  `ErrorScreenTests#test1` (PP-2896), `#test2` (PP-2895, PP-2903), `#test4` (PP-2902);
  `NoResultsTests#test1` (PP-2904, upload half), `#test2` (PP-2907); `FileImportErrorDialogTests#test1`
  (PP-2906), `#test2` (PP-2909). Test logic is not changed.

### BrowserStack

- **R17 (MUST, entry):** `bs_run_group_errorscreens.sh` runs every test in the case → test table:
  `ErrorAndNoResultScreenMockBackendTests`, `ErrorScreenTests`, `NoResultsTests`,
  `FileImportErrorDialogTests` and `OpenWithTest#opening_unsupported_file_with_MainActivity_does_not_start_Bank_SDK`
  (whole classes, because BrowserStack rejects a `class` filter of 1000+ characters).
  `bs_run_all_groups.sh` has `run_group "errorscreens" ErrorAndNoResultScreenMockBackendTests`. The
  other classes also stay in their own groups (`import`, `extraction`, `smoke`).
- **R18 (MUST, async):** On BrowserStack, one run of `bs_run_group_errorscreens.sh` passes with no
  failures. The script runs every test in the case → test table, so that one build covers all 22
  cases; the only skips are `ErrorScreenTests#test2`–`#test4`, which switch the network off and skip
  themselves on BrowserStack by design. The build id is recorded in the PR description. (Changed
  with the user from "3 runs of the `errorscreens` and `import` groups": the ticket only asks for the
  tests to run on BrowserStack.)
- **R19 (SHOULD, async):** Each R5–R11 test waits up to 30 s for the error header, like
  `NoResultScreen.waitForNoResultScreen`, because remote devices are slow.

## Affected modules

- `bank-sdk:example-app` only.
  - `src/main/java/.../exampleapp/uitestsupport/` (test-support code that ships in the example app,
    not in any SDK).
  - `src/androidTest/` (tests, page objects, assets, scripts).
- No SDK or library module changes. `capture-sdk:sdk` is read, not changed.

## Public API impact

None. `bank-sdk:example-app` is not published, and no `api/*.api` dump is touched.
`UiTestMockBackend` and its new types live in the example app's main source set, like the existing
ones.

## Technical conventions

1. **Language:** Kotlin. New declarations in `uitestsupport/` follow the existing file: the new enum
   is public, like `UiTestMockScenario`, because the androidTest source set uses it. No Java is
   touched. No `capture-sdk` file is touched.
2. **UI:** no production UI. No Compose, no XML layouts added or removed.
3. **Architecture:** no ViewModel/state changes. Tests use the existing page-object pattern
   (`ui/screens/*`) and the base class `WarningBottomSheetTestBase` (rules, `RetryRule`,
   `armMockBackend`, `disarm` in `@After`, `cancelTestIfRunOnCi`).
4. **DI / async:** no DI change. The mock is wired through the existing `UiTestMockBackend.isArmed`
   check in `ConfigurationViewModel.kt:128`. The mock keeps calling back on the main thread
   (`Handler(Looper.getMainLooper())`), as today.
5. **Strings:** no new strings. Tests read expected text from capture-sdk resources through
   `targetContext.getString(net.gini.android.capture.R.string.…)`, never as hard-coded English.
   Existing tests that hard-code English stay as they are. New asset: `src/androidTest/assets/unsupported.txt`
   (a few bytes of plain text).
6. **Quality gates:** ktlint + detekt clean on the touched files
   (`./gradlew bank-sdk:example-app:ktlintCheck bank-sdk:example-app:detekt`, if configured for the
   module). The example app has no Jacoco/Sonar coverage target. androidTest code compiles with
   `./gradlew bank-sdk:example-app:assembleDevExampleAppDebugAndroidTest`.

## Design

### Mock backend

`bank-sdk/example-app/src/main/java/net/gini/android/bank/sdk/exampleapp/uitestsupport/UiTestMockBackend.kt`:

- New enum, in the same file:

  ```kotlin
  enum class UiTestMockAnalysisFailure(val statusCode: Int?) {
      GENERIC(null), NO_CONNECTION(null),
      UPLOAD(400), AUTH(401), OUTAGE(500), SERVER(502), MAINTENANCE(503)
  }
  ```

  Each entry gets a one-line KDoc naming the `ErrorType` it produces.
- New `@Volatile var analysisFailure = UiTestMockAnalysisFailure.GENERIC`. `arm(...)` gets the
  parameter `analysisFailure: UiTestMockAnalysisFailure = GENERIC`. `disarm()` resets it, and
  `networkService()` passes it on.

`UiTestMockNetworkService.kt`, inside `analyze` → `deliver`, when `scenario == ANALYSIS_ERROR`:

- `GENERIC` → `Error(ANALYSIS_ERROR_MESSAGE)` (today's line).
- `NO_CONNECTION` → `Error(ANALYSIS_ERROR_MESSAGE, UnknownHostException(...))`. It uses the
  `Error(String, Throwable)` constructor, `capture-sdk/.../network/Error.java:48`.
- Every HTTP entry → `Error(statusCode, emptyMap(), RuntimeException(ANALYSIS_ERROR_MESSAGE))`. It
  uses the `Error(Integer, Map, Throwable)` constructor, `Error.java:59`.

The mapping these feed is `ErrorType.typeFromError`
(`capture-sdk/sdk/src/main/java/net/gini/android/capture/error/ErrorType.kt`). The error reaches the
screen through `NetworkRequestsManager` → `FailureException(errorType)` →
`AnalysisScreenPresenter.handleAnalysisError` (`analysis/AnalysisScreenPresenter.java:546`).
Upload succeeds in the mock, so the failure always comes from the analysis step. This matters
because an upload failure on the review path is always shown as `UPLOAD`
(`MultiPageReviewFragment.java:781`).

`WarningBottomSheetTestBase.armMockBackend` gets the same `analysisFailure` parameter with the same
default, so existing callers compile unchanged.

### `ErrorAndNoResultScreenMockBackendTests` (new)

`src/androidTest/java/.../ui/testcases/ErrorAndNoResultScreenMockBackendTests.kt` extends
`WarningBottomSheetTestBase`. Every test:
1. calls `armMockBackend(UiTestMockScenario.ANALYSIS_ERROR, analysisFailure = …)` (or `NO_RESULTS`
   for test9);
2. calls `takePhotoAndProcess()`. The camera works on BrowserStack: it sees the device rack, and the
   mock ignores the bytes (precedent: `IngredientBrandEducationTests`);
3. calls one private helper, `assertErrorScreen(ErrorType)`. The helper resolves both strings from
   the `ErrorType`, waits up to 30 s for `gc_error_header`, then uses the existing `ErrorScreen`
   methods `checkErrorTextDisplayed`, `checkErrorHeaderTextDisplayed`,
   `checkErrorTextViewDisplayed`, `checkEnterManuallyButtonIsDisplayed` and
   `checkBackToCameraButtonIsDisplayed`.

"Back To Camera" is visible here because the document comes from the camera.
`ErrorFragmentImpl.shouldAllowRetakeImages` hides it for imported documents, so every test uses the
camera, not an upload.

`ErrorScreen` (page object) gets `waitForErrorHeader(timeoutMs)`, a UiAutomator wait on
`net.gini.android.bank.sdk.exampleapp:id/gc_error_header`.

### `FileImportErrorDialogTests` (extend)

- `PdfUploader` gets `writeLargePdfToDownloads(context, fileName, sizeBytes)`. It writes a minimal
  valid PDF header, pads it with a comment block to `sizeBytes`, and inserts it into Downloads
  through MediaStore, like `copyPdfToDownloads`. The size is checked before the PDF is parsed
  (`FileImportValidator.java:106`), so the padding never has to be a valid page.
- `test4_importLargerThanTenMbPdfAndVerifyErrorDialogIsDisplayed` uses the file name
  `too-large-generated.pdf`, then follows the same picker steps as `test2`. The size dialog has an
  empty body text (`gc_error_file_import_size_text` is `""`), so the test checks only the title.

### `OpenWithTest` (extend)

- New asset `unsupported.txt`. New test
  `opening_unsupported_file_with_MainActivity_does_not_start_Bank_SDK` launches `MainActivity` with
  `ACTION_VIEW` + `text/plain` through the existing `getAssetFileStorageUri`. It then polls
  `scenario.state` for up to 10 s until `DESTROYED`, and asserts that `recyclerview_extractions`
  does not exist. It does not use `launchActivityForOpenWith`, because that helper asserts success.
- Idling: on the default Intent-based path nothing decrements the open-with idling resource after
  an error, so the test uses no Espresso `onView` (it would wait for the 90 s idling timeout). It
  checks the extractions view with UiAutomator. The next test's `resetIdlingResourceForOpenWith`
  drains the leftover count.

### Scripts (`src/androidTest/scripts/`)

- `bs_run_group_errorscreens.sh`: copy the header style of `bs_run_group_creditnote.sh`, then call
  `bs_build_and_upload.sh ErrorAndNoResultScreenMockBackendTests`.
- `bs_run_all_groups.sh`: `run_group "errorscreens" ErrorAndNoResultScreenMockBackendTests`, with a comment
  noting that the mock removes any network dependency.
- `bs_build_and_upload.md`: list the new group.
- `bs_run_group_smoke.sh` and `bs_run_release.sh` stay unchanged.

## Test plan

Stack: JUnit4, AndroidX Test (ActivityScenario, orchestrator), Espresso and UiAutomator, matching
the neighbouring classes in `bank-sdk/example-app/src/androidTest`.

| Test class | New / extends | New tests | Covers |
|---|---|---|---|
| `ErrorAndNoResultScreenMockBackendTests` | New, extends `WarningBottomSheetTestBase` | 9 | R1–R3, R5–R13, R19 |
| `FileImportErrorDialogTests` | Existing, extended | 1 | R14 |
| `OpenWithTest` | Existing, extended | 1 | R15 |
| `ErrorScreenTests`, `NoResultsTests`, `FileImportErrorDialogTests` | Existing, KDoc only | 0 | R16 |
| `CreditNoteMockBackendTests`, `IngredientBrand*Tests` | Existing, unchanged (re-run) | 0 | R3, R4 |

`ErrorAndNoResultScreenMockBackendTests`:
1. `test1_noConnection_showsConnectionErrorScreen` — R2, R5
2. `test2_status400_showsUploadErrorScreen` — R1, R6
3. `test3_status401_showsAuthErrorScreen` — R1, R7
4. `test4_genericFailure_showsUnexpectedErrorScreen` — R3, R8
5. `test5_status502_showsServerErrorScreen` — R1, R9
6. `test6_status503_showsMaintenanceErrorScreen` — R1, R10
7. `test7_status500_showsServiceUnavailableErrorScreen` — R1, R11
8. `test8_backToCamera_returnsToCameraScreen` — R12
9. `test9_cameraPhotoNoResults_showsNoResultScreen` — R13

There are 9 tests: one per `ErrorType` that the mock can produce (7), one navigation test, and one
No Results case. R1–R3 are checked through the visible screen, because each status code produces
different text. A mock that ignored the status would fail at least 6 of the 7 type tests.

`FileImportErrorDialogTests`:
4. `test4_importLargerThanTenMbPdfAndVerifyErrorDialogIsDisplayed` — R14

`OpenWithTest`:
- `opening_unsupported_file_with_MainActivity_does_not_start_Bank_SDK` — R15

R4 is shown by re-running `CreditNoteMockBackendTests` and `IngredientBrandTests` unchanged. R17 and
R18 are process checks for `/gini-build`: run `bs_run_group_errorscreens.sh` once on BrowserStack
and record the build id.

The mock enum and the `UiTestMockNetworkService` branch have no unit test. The example app has no
unit-test setup for `uitestsupport/` (the same gap PP-3478 recorded), and every branch is exercised
by `ErrorAndNoResultScreenMockBackendTests`.

### Not tested

- **The real login / session request (PP-2911 – PP-2917).** The mock fails the analysis call, not
  `/oauth/token`. The screen and text are identical because the SDK uses one mapping for every
  request. Proving that a real login failure reaches that mapping needs Charles, so that stays
  manual.
- **The backend's real decision for a table photo (PP-2901).** The mock decides it's a 400. Whether
  the real API rejects such a photo is backend behaviour.
- **A real offline device on BrowserStack.** BrowserStack blocks `svc wifi`. Real offline stays
  covered locally by `ErrorScreenTests#test2/#test4`.
- **The Toast text on the PP-2910 path.** It is example-app UI, and Toasts are unreliable to read on
  API 30+.
- **Landscape and dark mode of the error screens.** These belong to other Xray folders
  ("Landscape No Results & Error screens", WCAG landscape).
- **iOS.** This is PP-3919. The mapping table above is the hand-off.

## Out of scope

- Any change to `capture-sdk` error handling, `ErrorType` or strings.
- Rewriting existing tests (`ErrorScreenTests`, `NoResultsTests`, `FileImportErrorDialogTests`)
  beyond KDoc.
- Updating the Xray case texts. Flag to the PM:
  - PP-2909 says "more than 3 pages", but the Android limit is 10.
  - PP-2907 and PP-2910 expect a dialog that Android does not show.
  - The smoke cases PP-3434 – PP-3440 duplicate several of these cases.
- The other similar Xray folders listed under "Not tested".
- Posting the mapping to PP-3919.

## Open questions

- (confidence: LOW — run R14 once on BrowserStack) Can the test insert an 11 MB file into
  MediaStore Downloads on a BrowserStack device, and does the Files picker show it?
  `copyPdfToDownloads` is wrapped in `runCatching` because BrowserStack sometimes refuses
  delete/insert of a pre-loaded file. A new, unique file name should avoid that case. If it still
  fails, the fallback is to upload the generated file as BrowserStack media in
  `bs_build_and_upload.sh`.
- (confidence: LOW — run R15 once) Does a `text/plain` URI reach `FileImportValidator` and end in
  `CreateDocumentFromImportedFileResult.Error`, or does it fail earlier? The R15 assertion (flow not
  started, activity finished) holds either way. Only the claim that it is the "unsupported type"
  rejection is unverified.

## Implementation plan
- [x] 1. `bank-sdk:example-app` main `uitestsupport/`: add `UiTestMockAnalysisFailure`, wire it through `UiTestMockBackend.arm/disarm/networkService` and `UiTestMockNetworkService.analyze` (R1–R4)
- [x] 2. androidTest support: `analysisFailure` param on `WarningBottomSheetTestBase.armMockBackend`; `ErrorScreen.waitForErrorHeader` (R19)
- [x] 3. New `ErrorAndNoResultScreenMockBackendTests` with 9 tests (R5–R13)
- [x] 4. `PdfUploader.writeLargePdfToDownloads` + `FileImportErrorDialogTests#test4` (R14)
- [x] 5. Asset `unsupported.txt` + `OpenWithTest#opening_unsupported_file_with_MainActivity_does_not_start_Bank_SDK` (R15)
- [x] 6. Xray keys in the KDoc of the existing tests (R16)
- [x] 7. `bs_run_group_errorscreens.sh`, `bs_run_all_groups.sh`, `bs_build_and_upload.md` (R17)
- [x] 8. Verify: compile androidTest, ktlint/detekt, run the new and touched classes on an emulator; BrowserStack runs (R18) by the user
  - Local (Samsung S22, Android 16): ErrorAndNoResultScreenMockBackendTests 9/9, OpenWithTest unsupported-file test, CreditNoteMockBackendTests 6/6 and IngredientBrandTests 11/11 pass.
  - FileImportErrorDialogTests#test4 passed 4 of 7 runs on that phone. Cause: its Downloads folder is crowded, and the picker sometimes scrolls the new file under the toolbar, so the tap misses (the existing test1 fails there the same way). PdfUploader is left unchanged; stability is judged on BrowserStack, where Downloads holds only the uploaded files.
  - BrowserStack run 1 (Pixel 9 + Pixel 10 Pro, Android 16): import group `693b654e9cbb414a42ece3ae8e7cfa8227074dde` 44 passed / 6 skipped (the 3 offline ErrorScreenTests × 2 devices, expected) / 0 failed, test4 green on both; errorscreens group `57eb2e7c23d74826f4a70970fbb6e23b3bdf1d2d` 18/18 passed.
  - BrowserStack, errorscreens group with every mapped class (all 22 cases in one build): `72dd47fabc86a1aa46a83292c75fb4eaef3c954e` 34 passed / 6 skipped (ErrorScreenTests #test2–#test4 × 2 devices, expected) / 0 failed.
  - R18 met by build `72dd47fabc86a1aa46a83292c75fb4eaef3c954e`; the user agreed one green run is enough.

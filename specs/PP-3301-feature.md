# PP-3301: [Android] Due Date Hint — Add UI Automation tests (BrowserStack)

Status: draft
Ticket: https://ginis.atlassian.net/browse/PP-3301
Source test cases: XRay CSV exports (PP-3300 / PP-3261 / PP-3263), provided as
`PP-3261-due-date-hint-edited.csv`, `PP-3261-due-date-hint-accessibility-edited.csv`,
`PP-3263-schedule-payment-edited.csv`, `PP-3263-schedule-payment-accessibility-edited.csv`.
Feature under test: PP-3262 (`specs/PP-3262-feature.md`) + PP-3264
(`specs/PP-3264-feature.md`) — both merged.

## Problem

The redesigned due-date bottom sheet (`WarningBottomSheet` in capture-sdk) has
two states — Due Date Hint (PP-3262) and Schedule Payment (PP-3264) — gated on
the `paymentDueHintEnabled` / `paymentScheduleHintEnabled` flag pairs. Today it
is only covered by unit tests and manual QA; every release needs a manual
regression run of ~90 XRay cases. PP-3301 automates the automatable core of
those cases as Espresso/UiAutomator tests in the existing
`bank-sdk:example-app` BrowserStack suite, so the feature is verified on real
devices on every run without manual effort.

The manual cases use Charles to rewrite the server `/configurations` flags and
to control `paymentDueDate`. Neither is possible on BrowserStack, so the
automation replaces them with (a) the example app's client-side config levers
and (b) a fixed far-future test invoice plus the configurable
`paymentDueHintThresholdDays` (decisions confirmed with the user, see below).

## Decisions taken (confirmed with the user)

1. **Test data: far-future invoice + threshold lever.** A new test invoice
   document with a due date far in the future (the exact date is chosen during
   fixture validation — see Design → Test document) that the Gini API
   extracts as `paymentState = "ToBePaid"`. Show/not-show cases are driven
   by setting `paymentDueHintThresholdDays` in the example-app configuration
   (default 5 → shown; `remainingDays + 1` → not shown). No
   extraction-injection infrastructure is built. Real end-to-end, consistent
   with the rest of the suite.
2. **Server flags assumed enabled.** PP-3260 enables `paymentDueHintEnabled`
   and `paymentScheduleHintEnabled` in `/configurations` for the
   `gini-mobile-test` client. Tests assume both server flags are `true` and
   toggle only the client-side flags (`GiniCapture.isPaymentDueHintEnabled()` /
   `isPaymentScheduleHintEnabled()` via the example app's configuration). Each
   CSV Charles flag combination maps to the matching client switch combination.
   This is a hard precondition — if the server flags are off, every
   sheet-shown test fails.
3. **Scope: display logic + flag combinations, and dismissal + button
   actions.** Input-option variants (scan / PDF upload / open-with), the
   date-format variants, and the two accessibility CSVs stay manual (see
   "Not tested").
4. **Locale: resource-based assertions.** Texts are asserted via the
   capture-sdk string resources resolved at runtime
   (`net.gini.android.capture.R.string.gc_due_date_hint_title` etc.), so the
   same test passes on any device locale. Explicit EN/DE copy verification
   stays with unit tests and manual QA.
5. **Minimal example-app observable for the schedule result** *(chosen while
   resolving spec-review findings — veto at review if unwanted)*. The example
   app's `Success` and `SchedulePayment` handlers are identical except for a
   toast (`CaptureResultListener.kt:46-76`): both open `ExtractionsActivity`
   with the same extras. Toast assertions are unreliable on API 30+, so
   without a further observable, the R12 test could not distinguish
   "Schedule Payment" from "Proceed Anyway" — a mis-wired primary button
   would pass. Fix: a small main-source change in `bank-sdk:example-app`
   (never published) that surfaces the schedule path visibly on
   `ExtractionsActivity` (see Design). This also makes the example more
   faithful — the code comment there already says a real bank app would open
   its scheduled-transfer flow at this point.

## Flag-combination mapping (CSV → automation)

The presenter checks the schedule state before the due-date state and the
schedule flags are independent of the due flags
(`AnalysisScreenPresenter.java` ~348–382, 612–689):

| CSV (server flags via Charles) | Client switches in test | Expected sheet |
|---|---|---|
| due=true, schedule=false | due ON, schedule OFF | Due Date Hint |
| due=true, schedule=true | due ON, schedule ON | Schedule Payment (priority) |
| due=false, schedule=true | due OFF, schedule ON | Schedule Payment |
| due=false, schedule=false | due OFF, schedule OFF | none |

## Requirements

The two states are distinguished by description + button labels — the title
(`gc_due_date_hint_title`) is shared by both states, so no requirement may
assert on the title alone.

**Entry**

- R1 (MUST, entry): Given the example app with client flags due=ON,
  schedule=OFF and the far-future test invoice, when the invoice is uploaded
  via the photo/file picker and processed to the Analysis screen, then the Due
  Date Hint bottom sheet appears: title = `gc_due_date_hint_title` formatted
  with the invoice's due date as `dd.MM.yyyy`, description =
  `gc_due_date_hint_desc`, primary button = `gc_proceed_anyway`, secondary
  button = `gc_cancel_transfer` (resource-resolved texts, view IDs
  `warningTitle` / `warningDescription` / `primary_button` /
  `secondary_button`). *(CSV PP-3261 TC-001..003, TC-016)*
- R2 (MUST, entry): Given client flags due=ON, schedule=ON (and separately
  due=OFF, schedule=ON) and the same invoice, when processed, then the
  Schedule Payment sheet appears instead: description =
  `gc_schedule_payment_hint_desc`, primary button = `gc_schedule_payment`,
  secondary button = `gc_proceed_anyway` — proving schedule priority over the
  due hint and its independence from the due flag. *(CSV PP-3263 TC-001..003,
  TC-026..027)*

**Happy path — display logic**

- R3 (MUST, happy): Given `paymentDueHintThresholdDays` set at runtime to
  exactly `ChronoUnit.DAYS.between(today, invoiceDueDate)`, when the invoice
  is processed, then the sheet still appears (boundary: remaining days ==
  threshold → shown). Midnight guard: `remainingDays` computed at setup
  shrinks by 1 if local midnight passes before the presenter recomputes it,
  flipping the boundary to not-shown — the test skips via
  `Assume.assumeTrue` when local time is within 30 minutes of midnight, so
  the boundary case never flakes on a night run. *(TC-003 "exactly threshold
  days")*
- R4 (MUST, error): Given `paymentDueHintThresholdDays` set to
  `remainingDays + 1`, when the invoice is processed, then no sheet appears
  and the flow continues directly to the extraction screen
  (`transfer_summary` visible). *(TC-004 "less than threshold")*
- R5 (MUST, error): Given client flags due=OFF, schedule=OFF, when the
  qualifying invoice is processed, then neither sheet appears and the flow
  continues to the extraction screen. *(PP-3261 TC-026..029)*
- R6 (SHOULD, error): Given a document whose extractions contain no
  `paymentDueDate` (candidate: existing `test_image.jpeg` — must be verified,
  see Open questions), when processed with both hint flags ON, then no sheet
  appears. *(TC-005)*
- R7 (SHOULD, error): Given the existing Return Assistant invoice
  `Testrechnung-RA-1.pdf` and both hint flags ON, when processed, then no
  due-date/schedule sheet appears (RA extractions suppress it; the digital
  invoice flow starts instead). Risk accepted with the SHOULD: this imports
  the digital-invoice flow, which the suite deliberately shards separately
  because it is unreliable in mixed runs — if the test flakes, drop it to
  manual QA rather than retry-padding it. *(TC-009)*

**Happy path — dismissal & button actions**

- R8 (MUST, happy): Given the Due Date Hint sheet is displayed, when the user
  taps outside the sheet (UiAutomator click on the dimmed area above it), then
  the sheet remains displayed (`isCancelable = false`). *(TC-020)*
- R9 (SHOULD, happy): Given the Due Date Hint sheet is displayed, when 6+
  seconds pass, then — asserted *while the sheet is still displayed*, so the
  absence is coupled to the sheet's presence — the rotating capture
  suggestions (`gc_analysis_hint_container`) are not displayed. Note the
  CSV's "4 seconds" is the hint *cycle* interval
  (`AnalysisHintsAnimator.HINT_CYCLE_INTERVAL = 4000`); the first hint would
  appear after `HINT_START_DELAY = 5000` ms, so the test waits ≥ 6 s. Hints
  only run for image documents — satisfied because the fixture is an image.
  Suppression is `AnalysisFragmentImpl.stopAndHideHints()`, which sets the
  container GONE for the rest of the screen's life. SHOULD, not MUST: the
  assertion can pass vacuously when hints would not have started anyway
  (e.g. the education screen ran instead); proving the counterfactual would
  need a second, sheet-free round trip and is not worth the cost. *(TC-021)*
- R10 (MUST, happy): Given the Due Date Hint sheet, when `primary_button`
  ("Proceed Anyway") is tapped, then the sheet is dismissed and the extraction
  screen (`transfer_summary`) is displayed. *(TC-022/023)*
- R11 (MUST, happy): Given the Due Date Hint sheet, when `secondary_button`
  ("Cancel Transfer") is tapped, then the sheet is dismissed and the app
  returns to the example-app landing page (`button_startScanner` visible).
  *(TC-024/025)*
- R12 (MUST, happy): Given the Schedule Payment sheet, when `primary_button`
  ("Schedule Payment") is tapped, then the sheet is dismissed, the example app
  receives `CaptureResult.SchedulePayment`, and `ExtractionsActivity` opens
  with the schedule-payment indicator visible (new example-app view
  `text_scheduled_payment_indicator`, shown only on the SchedulePayment path
  — decision 5, Design). Landing on `ExtractionsActivity` alone is NOT a
  sufficient assertion: the Success ("Proceed Anyway") path lands on the same
  screen with identical extras (`CaptureResultListener.kt:46-76`), so without
  the indicator a primary button mis-wired to `onProceed` would pass. The
  "Schedule payment requested" toast stays in the app but is not asserted
  (toast matching is unreliable on API 30+). *(PP-3263 TC-022/023)*
- R13 (MUST, happy): Given the Schedule Payment sheet, when `secondary_button`
  ("Proceed Anyway") is tapped, then the sheet is dismissed and the extraction
  screen is displayed with the schedule-payment indicator NOT visible —
  differentiating the two CTAs in both directions. *(PP-3263 TC-024/025)*

**Async**

- R14 (MUST, async): The sheet appears only after upload + analysis + the
  education animation complete (`doWhenEducationFinished` mutex). Every
  sheet-shown assertion first waits with UiAutomator
  `waitForExists(30_000)` on `AppResources.resId("warningTitle")`; every
  sheet-not-shown assertion first waits for the *alternative* outcome
  (extraction screen / digital invoice screen) and then asserts the sheet's
  absence — never a bare sleep followed by `doesNotExist`.

**Stability (ticket AC 2)**

- R15 (MUST): The new test classes are registered in the BrowserStack shard
  scripts (see Design) and pass 3 consecutive full BrowserStack runs before
  the PR is merged. Any test that cannot be stabilized is removed or
  `@Ignore`d with a comment, not left flaky. Verification is manual and owned
  by the developer: /gini-build can at most run the suite locally
  (`connectedDevExampleAppDebugAndroidTest` on an attached device with API
  credentials); the BrowserStack runs need `BS_USER`/`BS_KEY` and their
  results are recorded in the PR description.

## Affected modules

- `bank-sdk:example-app` — androidTest sources (new test classes, new page
  object, extended page object, new test document asset), the BrowserStack
  shell scripts under `bank-sdk/example-app/src/androidTest/scripts/`, and
  one minimal main-source change (decision 5): `CaptureResultListener.kt`,
  `ExtractionsActivity.kt`, its layout, and one example-app string, to make
  the SchedulePayment result observable.

No changes to `capture-sdk`, `bank-sdk:sdk`, or the API libraries — no SDK
production code is touched.

## Public API impact

None. All changes are test code and scripts inside `bank-sdk:example-app`'s
`androidTest` source set, which is never published.

## Technical conventions

1. **Language:** Kotlin for all new test files. No legacy Java is touched.
2. **UI:** no SDK UI changes; no Compose. The single UI addition is the
   example-app schedule indicator (decision 5): a plain `TextView`
   (`@+id/text_scheduled_payment_indicator`, GONE by default) added to
   `ExtractionsActivity`'s existing XML layout, matching that layout's style,
   with its label in the example app's `values/strings.xml` (the example app
   is English-only) — a visible `TextView` is read by TalkBack by default, no
   extra a11y wiring needed. Tests match existing view IDs: sheet IDs from
   capture-sdk (`warningTitle`, `warningDescription`, `primary_button`,
   `secondary_button` in `gc_warning_bottom_sheet.xml`), analysis hints
   container `gc_analysis_hint_container`, example-app IDs
   (`button_startScanner`, `transfer_summary`). No test tags exist and none
   are added — the sheet is a Views `BottomSheetDialogFragment`, not Compose.
3. **Architecture:** the suite's page-object pattern —
   test classes in `net.gini.android.bank.sdk.exampleapp.ui.testcases`
   (mandatory: the BrowserStack scripts auto-prefix class names with exactly
   this package), page objects in `.ui.screens`, helpers in `.ui.resources`.
   Rule order per `RetryRule` kdoc:
   `@get:Rule(order = Int.MIN_VALUE) RetryRule()`, then
   `activityScenarioRule<MainActivity>()`, then `GrantPermissionRule`.
   Method naming `test<N>_<camelCaseDescription>` so shards can target
   `Class#method`.
4. **Flag control:** the direct-ViewModel hook proven in
   `ProductTagConfigurationTests.kt:142-147` —
   `activityRule.scenario.onActivity { ViewModelProvider(it)[ConfigurationViewModel::class.java].setConfiguration(vm.configurationFlow.value.copy(paymentDueHintEnabled = …, paymentScheduleHintEnabled = …, paymentDueHintThresholdDays = …)) }`
   — preferred over clicking `switch_paymentDueHint` /
   `switch_paymentScheduleHint` / typing into
   `editText_paymentDueHintThresholdDays` (the text field calls `toInt()` on
   every change and is a flake source). A defensive `@After` resets the
   configuration to defaults (due=ON, schedule=ON, threshold=5) with
   `runCatching { }`. Under the Orchestrator with `clearPackageData = "true"`
   every test already runs in a fresh process with wiped app data, so state
   cannot leak between tests on BrowserStack — the reset only matters for
   local runs that bypass the Orchestrator (e.g. an Android Studio run
   configuration).
5. **Strings/resources:** no new string resources. Assertions resolve existing
   capture-sdk strings at runtime via
   `InstrumentationRegistry.getInstrumentation().targetContext.getString(...)`
   with the formatted date argument where needed (`DueDateFormatter` renders
   `dd.MM.yyyy`).
6. **Quality gates:** `bank-sdk:example-app` must still assemble
   (`assembleDevExampleAppDebug` + `assembleDevExampleAppDebugAndroidTest`);
   `lint`, `detekt`, `ktlintCheck` clean for the module (androidTest sources
   are covered). No Jacoco/Sonar expectations — instrumented example-app tests
   are outside coverage collection.

## Design

### New/changed files

```
bank-sdk/example-app/src/androidTest/
├── java/net/gini/android/bank/sdk/exampleapp/ui/testcases/
│   ├── DueDateHintBottomSheetTests.kt        (new)
│   └── SchedulePaymentBottomSheetTests.kt    (new)
├── java/net/gini/android/bank/sdk/exampleapp/ui/screens/
│   ├── WarningBottomSheetScreen.kt           (new page object)
│   └── ConfigurationScreen.kt                (extended, only if UI toggling
│                                              is needed as fallback)
├── assets/
│   └── invoice_future_due.jpeg (or .png)     (new test document)
└── scripts/
    ├── bs_run_group_duedate.sh               (new shard)
    └── bs_run_all_groups.sh                  (wire the new shard; update the
                                               class-count comments)

bank-sdk/example-app/src/main/                (decision 5 — schedule observable)
├── java/.../ui/util/CaptureResultListener.kt (pass isSchedulePayment extra)
├── java/.../ui/ExtractionsActivity.kt        (read extra, show indicator)
├── res/layout/<ExtractionsActivity layout>   (add hidden indicator TextView)
└── res/values/strings.xml                    (indicator label)
```

### Test document

A rendered invoice image with a far-future due date, amount, IBAN, and no
Skonto/Return-Assistant line items, placed in `androidTest/assets/` and
injected exactly like `test_image.jpeg`:
`ImageUploader.copyImageToDownloads(context, "invoice_future_due.jpeg")` →
photo-picker flow (`ExtractionScreenTests.chooseAndUploadImageFromPhotos()`
pattern). Using an image via `ImageUploader` avoids touching the
BrowserStack `upload-media` calls in `bs_build_and_upload.sh` (those are only
needed for PDFs picked through DocumentsUI).

**The due date is chosen during fixture validation, not fixed here.** A
decades-out date (2099) risks being down-ranked or rejected as implausible by
the extraction model; a near date goes stale. Start ~2 years out and move
further only if validation shows the backend extracts it reliably. The chosen
date lives in a single `FIXTURE_DUE_DATE: LocalDate` constant next to the
tests (the document and the constant must agree), with a comment stating the
refresh deadline if a near-term date ends up being necessary. The document
must be validated against the real API before the tests are written:
extractions must contain `paymentDueDate = <FIXTURE_DUE_DATE as yyyy-MM-dd>`
and `paymentState = ToBePaid` (confidence: LOW — verified only by a manual
upload with the `gini-mobile-test` client; see Open questions).

### Threshold arithmetic

`AnalysisScreenPresenter.calculateRemainingDays` uses
`ChronoUnit.DAYS.between(LocalDate.now(), dueDate)`. Tests compute
`remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), FIXTURE_DUE_DATE)`
at runtime and set the threshold via the ViewModel hook:

- shown (default): threshold 5 (≪ remainingDays)
- boundary shown: threshold = remainingDays (R3, with the midnight guard)
- not shown: threshold = remainingDays + 1 (R4)

This keeps the fixture date valid for its whole lifetime and exercises the
exact boundary the CSVs describe. Boundary-sensitive tests (R3, R4) apply the
R3 midnight guard: `Assume.assumeTrue(LocalTime.now().isBefore(23:30) &&
LocalTime.now().isAfter(00:30))`-style skip, so a date rollover between test
setup and the presenter's own computation cannot flip the expected outcome.

### Schedule-result observable (example-app main-source change, decision 5)

`CaptureResultListener.kt` (`SchedulePayment` branch, line 60) passes
`isSchedulePayment = true` into `ExtractionsActivity.getStartIntent(...)` —
new parameter defaulting to `false`, carried as
`EXTRA_IN_IS_SCHEDULE_PAYMENT` next to the existing
`EXTRA_IN_IS_CX_EXTRACTIONS` (`ExtractionsActivity.kt:320`).
`ExtractionsActivity` reads the extra and toggles a `TextView`
(`@+id/text_scheduled_payment_indicator`, GONE by default) to VISIBLE. The
`Success` branch is untouched, so the indicator is a faithful, real behavior
of the example app — not a test-only stub — and gives R12/R13 their
distinguishing observable. The toast stays as-is.

### Known stability assumptions (document in the test class kdoc)

1. **Server-config race:** `clearPackageData = "true"` wipes the persisted
   `/configurations` DataStore before every test, so each test re-fetches the
   server flags when the capture flow starts. `GiniBankConfigurationProvider`
   defaults every flag to `false` — if the analysis round trip ever completed
   before the config response arrived, the sheet would legitimately not show
   and the test would go red. In practice analysis takes 10–30 s against ~1 s
   for the fetch, so the race is essentially always won; but an intermittent
   "sheet never appeared" failure should be diagnosed as a config-fetch
   problem before suspecting the sheet logic. `RetryRule` masks single
   occurrences.
2. **Server flags enabled** for `gini-mobile-test` (decision 2): an all-red
   shard almost always means PP-3260 regressed, not the tests.

### Page object `WarningBottomSheetScreen`

Follows `ExtractionScreen.kt`: UiAutomator `waitForExists` first, Espresso
second, methods return `this`.

- `waitForSheet(): Boolean` — `device.findObject(UiSelector().resourceId(AppResources.resId("warningTitle"))).waitForExists(30_000)`
- `assertDueDateHintState(expectedDate: String)` / `assertSchedulePaymentState(expectedDate: String)`
  — `onView(withId(...warningTitle))` matches
  `getString(gc_due_date_hint_title, expectedDate)`; description and both
  button texts matched against the state's resources (never title-only — the
  two states share the title)
- `clickPrimaryButton()` / `clickSecondaryButton()`
- `tapOutsideSheet()` — reads the sheet's top edge via UiAutomator bounds
  (`UiObject.getBounds()` on `warningTitle` or the Material sheet root
  `design_bottom_sheet`) and clicks horizontally centered, vertically halfway
  between the status-bar bottom and the sheet's top edge. Never at the top
  edge of the screen — a tap there can grab the status bar and pull down the
  notification shade, breaking this test and its retry
- `assertSheetNotDisplayed()` — used only after waiting for the alternative
  screen (R14)
- `assertCaptureSuggestionsNotDisplayed()` — `gc_analysis_hint_container` not
  shown

### Test classes

Both follow the `ExtractionScreenTests` skeleton (rules, page-object fields,
`SimpleIdlingResource`, `cancelTestIfRunOnCi()`, `@After` unregister +
config reset). Common setup helper: set flags/threshold via ViewModel, upload
`invoice_future_due.jpeg`, click through review → process → analysis.

`DueDateHintBottomSheetTests` (due=ON, schedule=OFF), ~8 tests:

1. `test1_dueDateHintSheetIsDisplayedWithCorrectContent` (R1, R14)
2. `test2_sheetIsDisplayedWhenRemainingDaysEqualThreshold` (R3)
3. `test3_sheetIsNotDisplayedWhenRemainingDaysBelowThreshold` (R4)
4. `test4_noSheetIsDisplayedWhenBothFlagsAreOff` (R5)
5. `test5_tapOutsideDoesNotDismissSheet` (R8)
6. `test6_captureSuggestionsAreSuppressedWhileSheetIsShown` (R9)
7. `test7_proceedAnywayDismissesSheetAndShowsExtractions` (R10)
8. `test8_cancelTransferDismissesSheetAndReturnsToMainScreen` (R11)

Plus (SHOULD, only if the fixtures verify — otherwise dropped to manual):
`test9_noSheetWhenDueDateExtractionIsEmpty` (R6, `test_image.jpeg`),
`test10_noSheetForReturnAssistantInvoice` (R7, `Testrechnung-RA-1.pdf` via
`PdfUploader`).

`SchedulePaymentBottomSheetTests`, ~5 tests:

1. `test1_schedulePaymentSheetIsDisplayedWhenBothFlagsOn` (R2 — priority)
2. `test2_schedulePaymentSheetIsDisplayedWhenOnlyScheduleFlagOn` (R2)
3. `test3_sheetIsNotDisplayedWhenRemainingDaysBelowThreshold` (R4 analog,
   PP-3263 TC-028)
4. `test4_schedulePaymentButtonShowsScheduledPaymentIndicator` (R12 —
   asserts the indicator VISIBLE on `ExtractionsActivity`)
5. `test5_proceedAnywayDismissesSheetAndShowsExtractions` (R13 — asserts the
   indicator NOT visible)

~13–15 tests total. Each is a full live-API round trip (~1–2 min on
BrowserStack), which justifies the dedicated shard and forbids padding the
matrix further — the remaining CSV permutations are redundant with these or
manual-only.

### BrowserStack wiring

New shard `bs_run_group_duedate.sh` targeting the two new classes, plus a
`run_group "duedate"` entry in `bs_run_all_groups.sh`; update the hard-coded
"cover all N classes" comments. The default whole-package run picks the
classes up automatically. Devices stay `Google Pixel 9-16.0` /
`Google Pixel 10 Pro-16.0` (both phones → BottomSheetDialog branch, not the
tablet AlertDialog). No CI workflow change: `bank-sdk.check.ui-tests.yml`
stays disabled; the BrowserStack run remains script-driven with
`BS_USER`/`BS_KEY`, per the existing suite.

## Test plan

The deliverable of this ticket *is* tests, so the plan and the design
coincide. Stack: JUnit4 + Espresso (core/intents) + UiAutomator + AndroidX
test rules + Orchestrator, matching every neighbor in
`bank-sdk/example-app/src/androidTest/.../ui/testcases/` (no MockK/Robolectric
here — that stack belongs to unit tests). Both classes are new (no existing
class covers this screen); `ConfigurationScreen`/`ExtractionScreen`/
`MainScreen` page objects are reused, `WarningBottomSheetScreen` is new.

Requirement → test mapping is inline in the Design section: R1–R15 each map to
a named test above; R14 is a property of every test's wait strategy; R15 is
verified by 3 green consecutive BrowserStack runs of the new shard, recorded
in the PR description.

No unit tests are added: the only production change is the example-app
indicator (decision 5) — trivial intent-extra delegation with no new class,
exercised directly and in both directions by the R12/R13 instrumented tests.
The "every new Kotlin class gets a unit test" rule is not triggered (no new
production class); page objects and test classes are exercised by being run.

### Not tested (deliberate)

- **Accessibility CSVs** (200% font, VoiceOver/TalkBack, external keyboard —
  both `*-accessibility-edited.csv` files): not expressible in
  Espresso/UiAutomator on BrowserStack; stays manual QA (as PP-3262/PP-3264
  specs already assign it).
- **Input-option variants** (scan via camera, PDF upload, open-with,
  PP-3261/3263 TC-011..015): the sheet logic is channel-independent; one
  channel (photo-picker image upload) is exercised. Camera scanning of a
  physical invoice is not automatable; open-with and PDF channels are already
  covered generically by `OpenWithTest`/`ImportPdfImageTests`.
- **Date-format variants** (TC-016..019 beyond the DD.MM.YYYY case in R1):
  backend normalization concern needing 4 distinct invoice documents; weakest
  automation value; manual.
- **German copy** (all "(German)" cases): resource-based assertions make the
  tests locale-agnostic; literal EN/DE copy is covered by
  `WarningBottomSheetTest.kt` unit tests and manual QA.
- **Paid-state and Skonto suppression** (TC-006..008, TC-010): no fixture
  document is known to extract `paymentState = Paid` or Skonto terms; manual
  QA unless such fixtures are produced later (R7 covers the Return Assistant
  sibling with the existing fixture).
- **Server-flag combinations** (the actual Charles cases): the server gates
  are assumed `true` (decision 2) and their read-through is covered by
  existing unit tests (`GetPaymentScheduleHintEnabledUseCaseTest`,
  `ClientConfigurationStorageTest`); client-side flags are the automation
  proxy.
- **Tablet AlertDialog variant, orientation/dark-mode matrix**: the
  BrowserStack devices are phones in portrait; the orientation/theme sweeps in
  the CSVs stay manual.

## Out of scope

- Any production code change in the SDK modules (capture-sdk, bank-sdk:sdk,
  API libraries) — including extraction-injection seams, test hooks, or new
  test tags. The single example-app observable from decision 5 is the only
  production-source change permitted. If the far-future-invoice approach
  proves unstable, adding an injection seam is a new ticket, not scope creep
  here.
- Re-enabling the `bank-sdk.check.ui-tests.yml` GitHub Actions workflow or
  adding BrowserStack calls to CI/fastlane.
- Changing the BrowserStack device matrix (ticket open question 1 — reuse the
  existing Pixel matrix).
- iOS counterpart.
- Automating the manual-only groups listed under "Not tested".

## Open questions

1. **Fixture validation (blocking, first step of /gini-build):** upload the
   candidate `invoice_future_due` document manually with the
   `gini-mobile-test` client and confirm the API returns
   `paymentDueDate = <FIXTURE_DUE_DATE>` and `paymentState = ToBePaid`; this
   validation also fixes the actual due date (see Design → Test document). If
   `ToBePaid` is not returned for a synthetic invoice, the
   far-future-invoice strategy fails and the user must be consulted before
   falling back to an injection seam. (confidence: LOW — backend behavior not
   verifiable from this repo)
2. **Empty-due-date fixture:** verify whether `test_image.jpeg` extractions
   really lack `paymentDueDate` (R6 is SHOULD until confirmed). (confidence:
   LOW)
3. **PP-3260 status:** confirm the two server flags are actually enabled for
   `gini-mobile-test` before the first BrowserStack run — the most likely
   cause of a mysteriously all-red first run.
4. **Decision 5 veto:** the example-app schedule indicator was chosen while
   resolving spec-review findings (the toast alternative is unreliable on
   API 30+ and dropping it would make R12 vacuous). If the example-app
   main-source change is unwanted, say so before /gini-build — the fallback
   is a mandatory toast assertion with its flakiness risk accepted.

## Observations found while planning (not in scope)

- The binary-compatibility api dumps (`capture-sdk/sdk/api/sdk.api`,
  `bank-sdk/sdk/api/sdk.api`, `bank-api-library/library/api/library.api`)
  appear to predate PP-3264's public API additions
  (`CaptureResult.SchedulePayment`, `setPaymentScheduleHintEnabled`, …) — no
  `schedule` symbols are in the dumps while the code has them. If `apiCheck`
  runs on this branch it will likely fail for reasons unrelated to this
  ticket. Raise separately; do not fix here.

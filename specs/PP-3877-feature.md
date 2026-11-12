# PP-3877: [Android] UI Automation for Xray Credit Note Feature Test Suite

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-3877
Epic: https://ginis.atlassian.net/browse/PP-2671
iOS twin: https://ginis.atlassian.net/browse/PP-3878 (Backlog)
Manual test basis: Xray folder `/Banking Project/Credit Note Feature` (18 Test issues)

## Problem

The PM wants the 18 manual cases in the Xray folder "Credit Note Feature" covered by UI tests that
run on BrowserStack, so the credit note warning is checked on every release without a manual run.

Ten cases already have a test in `bank-sdk/example-app/src/androidTest` (`CreditNoteWarningTests`,
real backend, and `CreditNoteMockBackendTests`, canned backend). Eight have none. The ticket counted
one of those as automatable and seven as "stays manual", and asks to check that the seven really
cannot be automated. The check found that four of the seven can be:

- **German (PP-2711):** the example app declares `de-DE` in `res/xml/locale_configs.xml`, so a test
  can switch the app's own language without touching the device language.
- **200 % font (PP-2729, PP-2730):** the font scale is one system setting. The structural half of
  "not distorted, no overlap" (nothing cut off, buttons not overlapping) can be asserted.
- **External keyboard (PP-2734):** key events (TAB, ENTER) reach the app the same way a hardware
  keyboard's do.

Only the three TalkBack cases stay manual: a device test cannot hear what TalkBack announces.

Decisions from the clarifying questions:
- **Automate** PP-2712, PP-2711, PP-2729, PP-2730 and PP-2734.
- **Real backend for the new tests.** PP-2711, PP-2729, PP-2730, PP-2734 and the dark + landscape
  test upload the real `credit_note.png` and go through the Gini API, like `CreditNoteWarningTests`.
- **PP-2712 is the one exception: camera + mock.** It is a *scan* case. On BrowserStack the camera
  sees the device rack and no image is injected (`bs_build_and_upload.sh:82`), so the real backend
  would return nothing. The mock's `INVOICE` scenario answers instead.
- **Dark / landscape:** PP-2696, PP-2711, PP-2729 and PP-2730 say "perform for light/dark and
  portrait/landscape". One extra dark + landscape "Proceed anyway" test is added. No full matrix.
- **Steps:** mapped from the Xray CSV exports of PP-2711, PP-2712, PP-2729, PP-2730 and PP-2734.
  The other 13 cases are mapped from their titles and the existing tests.
- **Jira:** `/gini-build` sets labels on the 18 Xray tests and posts one summary comment on PP-3877,
  after asking the user. The case → test table below is the hand-off for iOS (PP-3878).
- **Reuse first:** a case that already has a test gets no new test. Only its KDoc gets the Xray key.

Decisions made in this spec, for the user to confirm on review:
- **PP-2712 "with RA / Skonto screens"** is not automated. The plain-invoice path is. See
  "Not tested".
- **`bs_run_release.sh`** gets all credit note classes, because the ticket's goal is "on every
  release". Today it runs only `CreditNoteWarningTests`.

## Case → test mapping

| Xray | Case | Test | Backend | Status |
|---|---|---|---|---|
| PP-2690 | SDK flag ON by default | `CreditNoteWarningTests#test1` | — | existing |
| PP-2693 | Warning dialog | `CreditNoteWarningTests#test3` | real | existing |
| PP-2694 | Tap outside does not dismiss | `CreditNoteWarningTests#test4` | real | existing |
| PP-2695 | Cancel transfer | `CreditNoteWarningTests#test5` | real | existing |
| PP-2696 | Proceed anyway, English | `CreditNoteWarningTests#test6` (light, portrait) + `CreditNoteLandscapeDarkTests#test1` (dark, landscape) | real | existing + new |
| PP-2711 | Proceed anyway, German | `CreditNoteLocaleFontScaleTests#test1` | real | new |
| PP-2712 | Flag off: valid invoice scanning | `CreditNoteMockBackendTests#test7` | mock | new |
| PP-2713 | Flag off: valid invoice uploading | `CreditNoteWarningTests#test2` | real | existing |
| PP-2729 | 200 % font, English | `CreditNoteLocaleFontScaleTests#test2` | real | new, structural half |
| PP-2730 | 200 % font, German | `CreditNoteLocaleFontScaleTests#test3` | real | new, structural half |
| PP-2731 | TalkBack, English | — | — | stays manual |
| PP-2733 | TalkBack, German | — | — | stays manual |
| PP-2734 | External keyboard | `CreditNoteWarningTests#test11` | real | new, key events |
| PP-3446 | Frontend ON + SDK ON | `CreditNoteWarningTests#test7`, `CreditNoteMockBackendTests#test1` | real + mock | existing |
| PP-3447 | Frontend ON + SDK OFF | `CreditNoteWarningTests#test8`, `CreditNoteMockBackendTests#test2` | real + mock | existing |
| PP-3451 | Frontend OFF + SDK ON | `CreditNoteMockBackendTests#test3` | mock | existing |
| PP-3452 | Frontend OFF + SDK OFF | `CreditNoteMockBackendTests#test4` | mock | existing |
| PP-3482 | TalkBack focus trapped in dialog | — | — | stays manual |

Result: 15 automated (10 existing, 5 new), 3 stay manual.

## Requirements

Definitions:
- *Real credit note uploaded*: `uploadFixtureInvoiceAndProcess(CreditNoteFixtures.CREDIT_NOTE_ASSET)`
  with the mock not armed and the client flag at its default (on). Preconditions, as for
  `CreditNoteWarningTests`: the server `/configurations` flag `creditNoteHintEnabled` is on for the
  `gini-mobile-test` client, and the backend classifies `credit_note.png` as
  `businessDocType = CreditNote`.
- *Proceed result*: the extraction screen is shown, the warning sheet is gone, and
  `isExtractionFieldFilled("amountToPay")` is false. This is what
  `CreditNoteWarningTests#test6` asserts today.
- *German strings*: the four strings `gc_document_marked_credit_note_title`,
  `gc_document_marked_credit_note_desc`, `gc_cancel_transfer`, `gc_proceed_anyway` resolved from a
  `de-DE` configuration context. *English strings*: the same from an `en-GB` context.

### Scanning with the flag off (PP-2712, mock)

- **R1 (MUST, happy):** Given the mock is armed with scenario `INVOICE` and the SDK credit note flag
  is set to off, when a camera photo is taken and processed, then the extraction screen is shown, the
  warning sheet is not shown, and `iban`, `paymentRecipient`, `paymentPurpose` and `amountToPay` are
  each non-blank.

### German (PP-2711, real backend)

- **R2 (MUST, entry):** Given a test annotated `@AppLocale("de-DE")` on a device with API 33+, when
  the test starts, then the example app's application locale is `de-DE` before `MainActivity` is
  launched, and it is reset to the empty locale list after the test, also when the test fails.
- **R3 (MUST, happy):** Given `@AppLocale("de-DE")`, when a real credit note is uploaded, then the
  warning sheet shows the German strings in `warningTitle`, `warningDescription`, `primary_button`
  (Überweisung abbrechen) and `secondary_button` (Trotzdem fortfahren), and each German string
  differs from its English string. When "Trotzdem fortfahren" is clicked, then the proceed result
  holds.
- **R4 (MUST, error):** Given a device below API 33, when a test annotated `@AppLocale` starts, then
  it is skipped with the assumption message "App locale needs API 33", not failed.

### 200 % font (PP-2729, PP-2730, real backend)

- **R5 (MUST, entry):** Given a test annotated `@FontScale(2.0f)`, when the test starts, then
  `settings get system font_scale` returns `2.0` before `MainActivity` is launched, and the previous
  value is restored after the test, also when the test fails.
- **R6 (MUST, error):** Given the device refuses the setting (the value read back is not `2.0`),
  when the test starts, then it is skipped with the assumption message "Font scale could not be
  changed on this device", not failed.
- **R7 (MUST, happy):** Given `@FontScale(2.0f)` and `@AppLocale("en-GB")`, when a real credit note
  is uploaded, then the sheet shows the English strings and the *layout checks* pass:
  1. neither button's text is ellipsized or cut (every line has ellipsis count 0, and the text
     layout height fits inside the button);
  2. both buttons are completely displayed on screen;
  3. the two buttons' screen rectangles do not intersect each other;
  4. neither button's rectangle intersects the visible rectangle of the text scroll container;
  5. the title and the description are each either completely displayed, or their scroll container
     can scroll vertically (so the hidden part is reachable).

  When "Proceed anyway" is clicked, then the proceed result holds. Covers PP-2729.
- **R8 (MUST, happy):** The same as R7 with `@AppLocale("de-DE")` and the German strings. Covers
  PP-2730. (The Xray case text names the English labels in red; the German strings are what the
  SDK shows in German.)

### External keyboard (PP-2734, real backend)

- **R9 (MUST, happy):** Given a real credit note is uploaded and the warning sheet is shown, when
  `KEYCODE_TAB` is sent at most 6 times, then `secondary_button` has focus after one of them, and
  `primary_button` had focus after an earlier one. When `KEYCODE_ENTER` is then sent, then the
  proceed result holds.
- **R10 (MUST, error):** Given the sheet is shown, when 6 TAB key events have been sent and
  `secondary_button` never had focus, then the test fails with "Proceed anyway was not reachable
  with the keyboard".

### Dark + landscape (PP-2696, real backend)

- **R11 (MUST, happy):** Given dark mode is on, when the real credit note is picked in portrait, the
  device is turned to landscape on the review screen, and "Process" is clicked, then the sheet shows
  the credit note state (`assertCreditNoteState`) while the device is in landscape. When "Proceed
  anyway" is clicked, then the proceed result holds.
- **R12 (MUST, error):** Given the device does not let the test switch dark mode on, then the test is
  skipped (same `Assume` as `IngredientBrandLandscapeTests.checkDarkMode`).

### Existing tests (reuse)

- **R13 (MUST, entry):** The KDoc of each existing test in the mapping table names its Xray key(s).
  Test logic is not changed.

### BrowserStack

- **R14 (MUST, entry):** `bs_run_group_creditnote.sh` and the `creditnote` group in
  `bs_run_all_groups.sh` and `bs_run_release.sh` run `CreditNoteWarningTests`,
  `CreditNoteMockBackendTests`, `CreditNoteLocaleFontScaleTests` and `CreditNoteLandscapeDarkTests`.
- **R15 (MUST, async):** One run of `bs_run_group_creditnote.sh` on BrowserStack finishes with no
  failures. Skips are allowed only for R4, R6 and R12. The build id and the list of skipped tests are
  recorded in the PR description.

### Jira

- **R16 (SHOULD, async):** After R15 and after the user says yes, each of the 18 Xray tests gets one
  label: `android-automated` (ran on BrowserStack), `android-local-only` (automated but skipped on
  BrowserStack) or `android-stays-manual`. Existing labels stay. One comment on PP-3877 carries the
  mapping table and the stays-manual reasons.

## Affected modules

- `bank-sdk:example-app` only, and only its `src/androidTest/` source set (tests, rules, page
  objects, scripts). The example app's main source set, including `uitestsupport/`, is not changed.
- No SDK or library module changes. `capture-sdk:sdk` is read, not changed.

## Public API impact

None. `bank-sdk:example-app` is not published and no `api/*.api` dump is touched. This feature has
no integrator-visible entry point.

## Technical conventions

1. **Language:** Kotlin. No Java file is touched. No `capture-sdk` file is touched.
2. **UI:** no production UI. No Compose, no XML layouts added or removed.
3. **Architecture:** no ViewModel or state changes. Tests use the page-object pattern
   (`ui/screens/*`) and extend `WarningBottomSheetTestBase`. Device settings are changed through
   JUnit `TestRule`s that wrap the activity rule, like `LandscapeDarkModeRule`.
4. **DI / async:** no DI change. No coroutine code.
5. **Strings:** no new strings. Expected texts come from capture-sdk resources
   (`net.gini.android.capture.R.string.…`), never hard-coded. The German tests resolve them from a
   `createConfigurationContext` with locale `de-DE`. Locale folders in `capture-sdk:sdk`: `values`
   (German, the default) and `values-en`.
6. **Quality gates:** ktlint + detekt clean on the touched files
   (`./gradlew bank-sdk:example-app:ktlintCheck bank-sdk:example-app:detekt`). The example app has
   no Jacoco/Sonar coverage target. androidTest compiles with
   `./gradlew bank-sdk:example-app:assembleDevExampleAppDebugAndroidTest`.

## Design

`ui/` below means
`bank-sdk/example-app/src/androidTest/java/net/gini/android/bank/sdk/exampleapp/ui/`.

Why the proceed result holds: `AnalysisScreenPresenter.removeAmountToPay`
(`capture-sdk/sdk/src/main/java/net/gini/android/capture/analysis/AnalysisScreenPresenter.java:755`)
removes `amountToPay` from the extractions. The example app re-adds it as an empty row, so the tests
read the value, not the row (`ui/screens/ExtractionScreen.kt`, `isExtractionFieldFilled`).

### Rules (`ui/resources/`)

- `AppLocaleRule.kt`: annotation `@AppLocale(val tag: String)` and `class AppLocaleRule : TestRule`.
  In `apply`, it reads the annotation from `Description`. No annotation: run the test unchanged.
  With one: `Assume` API 33+, then set
  `targetContext.getSystemService(LocaleManager::class.java).applicationLocales =
  LocaleList.forLanguageTags(tag)`, run the test, and reset to `LocaleList.getEmptyLocaleList()` in
  `finally`. The instrumentation runs in the app's process, so this needs no shell command and no
  permission. `de-DE` and `en-GB` are the two locales in
  `bank-sdk/example-app/src/main/res/xml/locale_configs.xml`.
- `FontScaleRule.kt`: annotation `@FontScale(val scale: Float)` and `class FontScaleRule : TestRule`.
  With the annotation: read the old value with `settings get system font_scale`, run
  `settings put system font_scale <scale>` through `UiDevice.executeShellCommand`, read it back,
  `Assume` it matches, run the test, restore the old value in `finally` (`1.0` when the old value
  was `null`).
- Both rules are declared with `@get:Rule(order = Int.MIN_VALUE + 1)`, so they sit inside
  `RetryRule` (`Int.MIN_VALUE`) and outside the activity rule: the setting is applied before
  `MainActivity` starts and again on every retry. Precedent: `IngredientBrandLandscapeTests.kt:41`.

### `WarningBottomSheetTestBase` (extend, `ui/testcases/`)

- `uploadFixtureInvoiceToReview(assetName)`: the body of `uploadFixtureInvoiceAndProcess` up to and
  including `reviewScreen.assertReviewTitleIsDisplayed()`. `uploadFixtureInvoiceAndProcess` calls it
  and then clicks "Process", so existing callers are unchanged.
- `assertProceedResult()` (protected): the three assertions of the proceed result.

### `CreditNoteWarningTests` (extend, real backend)

- `test11_proceedAnywayIsReachableWithKeyboard` (R9, R10): upload the real credit note, wait for the
  sheet, `warningBottomSheet.focusSecondaryButtonWithTab(maxPresses = 6)`,
  `warningBottomSheet.pressEnter()`, `assertProceedResult()`.

### `CreditNoteMockBackendTests` (extend, mock)

- `test7_noWarningWhenScanningInvoiceWithSdkFlagOff` (R1): `armMockBackend(UiTestMockScenario.INVOICE)`,
  `configureCreditNoteHint(enabled = false)`, `takePhotoAndProcess()`, then the R1 assertions. The
  mock is used as it is today; no new scenario.

### `CreditNoteLocaleFontScaleTests` (new, real backend)

Extends `WarningBottomSheetTestBase`, declares both rules. The class KDoc repeats the two
preconditions of `CreditNoteWarningTests` (server flag, classification).
- `test1_german_proceedAnywayShowsExtractionsWithoutAmount` — `@AppLocale("de-DE")` (R2–R4)
- `test2_fontScale200_english_sheetIsNotCutOffAndProceedWorks` — `@FontScale(2.0f)`,
  `@AppLocale("en-GB")` (R5–R7)
- `test3_fontScale200_german_sheetIsNotCutOffAndProceedWorks` — `@FontScale(2.0f)`,
  `@AppLocale("de-DE")` (R8)

### `CreditNoteLandscapeDarkTests` (new, real backend)

Extends `WarningBottomSheetTestBase`, declares `LandscapeDarkModeRule` and the same
`checkDarkMode` assumption as `IngredientBrandLandscapeTests`. One test,
`test1_landscapeDark_proceedAnywayShowsExtractionsWithoutAmount` (R11, R12):
`uploadFixtureInvoiceToReview(CREDIT_NOTE_ASSET)` in portrait, `landscapeDarkMode.rotateToLandscape()`
on the review screen, `reviewScreen.clickProcessButton()`. The picker stays in portrait and the
Analysis screen opens already in landscape, which is what `LandscapeDarkModeRule` asks for. Precedent
for a credit note sheet in landscape: `IngredientBrandLandscapeTests#test7_landscapeDark_photo_warningOpensAfterTips`.

### `WarningBottomSheetScreen` (extend, `ui/screens/`)

- `assertCreditNoteStateIn(localeTag: String)`: resolves the four strings from a
  `createConfigurationContext` for that locale and checks them like `assertCreditNoteState`. For
  `de-DE` it also asserts each string differs from the `en-GB` one.
- `assertSheetIsNotCutOff()`: the five layout checks of R7, as Espresso `check { view, _ -> … }`
  blocks on `primary_button`, `secondary_button`, `warningTitle` and `warningDescription`
  (`MaterialButton` is a `TextView`, so `layout.getEllipsisCount(line)` works on it). The scroll
  container is the nearest scrollable ancestor of `warningTitle`: `textScroll` in
  `capture-sdk/sdk/src/main/res/layout/gc_warning_bottom_sheet.xml`, `contentScroll` in
  `layout-land/`.
- `focusSecondaryButtonWithTab(maxPresses: Int)`: sends `KeyEvent.KEYCODE_TAB` with
  `UiDevice.pressKeyCode`, checks focus on both buttons after each press with Espresso
  `hasFocus()`, records that `primary_button` was focused, and fails with the R10 message when the
  limit is reached.
- `pressEnter()`: `UiDevice.pressKeyCode(KeyEvent.KEYCODE_ENTER)`.

### `ReviewScreen` and `CaptureScreen` (amended during `/gini-build`, 2026-11-12)

Found when the spec was checked against the code: the upload path matched literal English texts,
which no test noticed because no test ran with the app in German before.

- `ReviewScreen.assertReviewTitleIsDisplayed` and `clickProcessButton` matched `"Review"` and
  `"Process"`. They now match the capture-sdk strings `gc_title_review` and `gc_process_documents`
  by resource id, so they follow the app's language. Unchanged in English.
- `CaptureScreen.clickPhotos` matched `"Photos"`. On API 33+ (where the German tests run) the
  entry is the SDK's own string `gc_file_chooser_fotos_section_header` (`FileChooserFragment`), so
  it follows the app's language. It is now matched by that resource; the `"Photos"` literal stays
  for older devices, where the entry is the gallery app's label.
- `assertSheetIsNotCutOff` takes the expected font scale and first asserts that the sheet's own
  configuration has it (found in the self-review): R5 only proves the setting was written, and the
  layout checks would pass at 100 % if the app did not apply it.

### Scripts (`src/androidTest/scripts/`)

- `bs_run_group_creditnote.sh`: add the two new classes and update the header comment (it still
  says "Three classes").
- `bs_run_all_groups.sh` and `bs_run_release.sh`: `creditnote` group lists all four classes.
- `bs_build_and_upload.md`: update the group description.

## Test plan

Stack: JUnit4, AndroidX Test (ActivityScenario, orchestrator), Espresso and UiAutomator, matching
the neighbouring classes in `bank-sdk/example-app/src/androidTest`.

| Test class | New / extends | New tests | Covers |
|---|---|---|---|
| `CreditNoteWarningTests` | Existing, extended | 1 | R9, R10 |
| `CreditNoteMockBackendTests` | Existing, extended | 1 | R1 |
| `CreditNoteLocaleFontScaleTests` | New, extends `WarningBottomSheetTestBase` | 3 | R2–R8 |
| `CreditNoteLandscapeDarkTests` | New, extends `WarningBottomSheetTestBase` | 1 | R11, R12 |
| `CreditNoteWarningTests`, `CreditNoteMockBackendTests` | Existing, KDoc only | 0 | R13 |

- R1 → `CreditNoteMockBackendTests#test7_noWarningWhenScanningInvoiceWithSdkFlagOff`
- R2, R3, R4 → `CreditNoteLocaleFontScaleTests#test1_german_proceedAnywayShowsExtractionsWithoutAmount`
- R5, R6, R7 → `#test2_fontScale200_english_sheetIsNotCutOffAndProceedWorks`
- R8 → `#test3_fontScale200_german_sheetIsNotCutOffAndProceedWorks`
- R9, R10 → `CreditNoteWarningTests#test11_proceedAnywayIsReachableWithKeyboard`
- R11, R12 → `CreditNoteLandscapeDarkTests#test1_landscapeDark_proceedAnywayShowsExtractionsWithoutAmount`
- R13 is checked by review. R14–R16 are process checks for `/gini-build`.

Six new tests: one per new Xray case (five) plus the dark + landscape variant.

The two rules and the page-object additions have no unit test of their own. The example app has no
unit-test setup for androidTest helpers (the gap PP-3478 and PP-3918 recorded), and each is
exercised by the tests above.

Reading a red run: the five real-backend tests fail together when the server flag or the
classification of `credit_note.png` regresses. Check `CreditNoteWarningTests#test3` first; if it is
red too, the cause is the backend, not the new tests.

### Not tested

- **TalkBack (PP-2731, PP-2733, PP-3482).** What TalkBack announces, in which order, and whether
  focus stays inside the dialog can only be judged with the screen reader running. BrowserStack does
  not offer it for Espresso runs, and the accessibility tree alone does not prove the spoken result.
  Stays manual.
- **"IBAN, Recipient and Reference are filled in correctly"** (PP-2711, PP-2729, PP-2730, PP-2734).
  The real `credit_note.png` prints a 10-character IBAN and has no validated extraction set
  (`CreditNoteFixtures`), so only the empty `amountToPay` is asserted, as in the existing `test6`.
  Stays manual.
- **The visual half of the 200 % cases.** "Not distorted" as a human sees it, and the Figma
  comparison. The tests check only that nothing is cut off or overlapping. The extraction screen at
  200 % is example-app UI and is not checked.
- **A real hardware keyboard (PP-2734).** The test sends the same key events. Pairing or plugging in
  a keyboard stays manual.
- **PP-2712 against the real backend, and with a Return Assistant or Skonto screen in between.** The
  scan runs against the mock, which has no invoice scenario with line items. The routing of a real
  RA invoice with the feature on is already covered by `CreditNoteWarningTests#test10`.
- **PP-2712's Settings switch tap.** The flag is set through `CreditNoteHintConfigurator`; see that
  class for why the switch is not tapped.
- **The device language.** The German tests switch the app's language, not the device's.
- **Button colours** (blue / white). The tests check which button is primary and secondary by id.
- **iOS.** That is PP-3878. The mapping table is the hand-off.

## Out of scope

- Any change to `capture-sdk` (`WarningBottomSheet`, layouts, strings).
- Any change to the example app's main source set, including the mock backend.
- Rewriting the existing credit note tests beyond KDoc.
- A full light/dark × portrait/landscape matrix.
- A validated extraction set for `credit_note.png`.
- Correcting the Xray case texts. Flag to QA: PP-2730 names the English labels although the case is
  German.
- Comparing with the iOS test code.

## Open questions

None open. All four were answered by the BrowserStack run of 2026-11-12, build
`a3abbf91e01a246dc2598490ea66ce7a09e97ea4` (`group-creditnote-20261112-162116`, project
`gini-mobile-android`): 22 of 22 tests passed on Google Pixel 9 and Google Pixel 10 Pro
(Android 16), no failures, no skips. An earlier run of the same day, before the self-review fixes
(`85ece033397016c5c996f8b884bcaae4ed9ee08f`), gave the same result.

- BrowserStack allows `settings put system font_scale`: the two 200 % tests ran, and the sheet's
  own configuration was at font scale 2.0.
- `secondary_button` is reached within 6 TAB presses, after `primary_button`.
- Both buttons stay completely on screen at 200 % in portrait, in English and German, on these two
  phones. A smaller phone was not tried.
- The review screen keeps the picked image and its "Process" button after the turn to landscape.

Not checked: whether any test needed a `RetryRule` retry to pass (the device logs were not read).

## Implementation plan
- [x] 1. `bank-sdk:example-app` androidTest: `ReviewScreen` matches string resources, `CaptureScreen.clickPhotos` accepts "Fotos"; `WarningBottomSheetTestBase` gets `uploadFixtureInvoiceToReview` and `assertProceedResult` (supports R3, R7–R9, R11)
- [x] 2. `ui/resources/AppLocaleRule.kt` and `FontScaleRule.kt` (R2, R4, R5, R6)
- [x] 3. `WarningBottomSheetScreen`: `assertCreditNoteStateIn`, `assertSheetIsNotCutOff`, `focusSecondaryButtonWithTab`, `pressEnter` (R3, R7, R8, R9, R10)
- [x] 4. `CreditNoteWarningTests#test11` and Xray keys in the KDoc of test1–test8 (R9, R10, R13)
- [x] 5. `CreditNoteMockBackendTests#test7` and Xray keys in the KDoc of test1–test4 (R1, R13)
- [x] 6. `CreditNoteLocaleFontScaleTests` (R2–R8)
- [x] 7. `CreditNoteLandscapeDarkTests` (R11, R12)
- [x] 8. Scripts: `bs_run_group_creditnote.sh`, `bs_run_all_groups.sh`, `bs_run_release.sh`, `bs_build_and_upload.md` (R14)
- [x] 9. One BrowserStack run of `bs_run_group_creditnote.sh`; record build id and skips (R15)
- [x] 10. After the user says yes: Jira labels on the 18 Xray tests and one comment on PP-3877 (R16)

# PP-3941: [Android] Spike: Show the IBAN selection sheet on top of the IBAN mismatch screen

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-3941
Epic: https://ginis.atlassian.net/browse/PP-3518 ([Q4] Fraud prevention warnings – phase 1)
Timebox: 1 day (reporter's comment on the ticket)

## Problem

The fraud-prevention epic will add an **IBAN mismatch screen**: when the IBAN in a payment QR
code differs from the IBAN printed on the invoice, the SDK warns the user before the payment
data is handed to the bank app. From that screen the user can open an **IBAN selection
sheet** and pick which IBAN to continue with. On Android this is a modal stacked on a modal
("dialog on dialog"), and the SDK does not control the host: a bank either launches the
flow as the SDK's own `CaptureFlowActivity` or embeds `CaptureFlowFragment` in its own
screen.

Before the feature is built, the team needs to know which native Compose construction
survives both integration modes, phones and tablets, rotation, process death, and the three
dismissal gestures (back button, back gesture, swipe down) without closing the wrong layer,
and how "Continue" closes both layers and moves on.

This is a **spike**. The deliverable is a written recommendation on the Jira ticket. The
prototype is **throwaway code on the `PP-3941-…` branch and is never merged** (decision from
the clarifying questions). Nothing in this spec changes a released module.

The data side does not exist yet: the `fraudSignals` compound extraction (PP-3952) and the
server/client feature flags (PP-3950, PP-3951) are in the backlog, and the production design
(PP-3749) is still in review with no Figma link attached. The spike therefore uses a debug
toggle and hardcoded sample IBANs, and a spike-grade layout.

## Requirements

Decisions from the clarifying questions, folded in:

- Spike code is throwaway on the branch; only the Jira write-up ships.
- The mismatch screen is a **modal over the Analysis screen**, like the existing warning
  sheets (already paid, credit note, due date), not a nav-graph destination.
- Trigger: a **debug toggle in `bank-sdk:example-app`** that forces the mismatch modal after
  any successful analysis, with hardcoded sample IBANs.
- Verification: **manual matrix only**, no automated tests for the throwaway code.

### Entry

- **R1 (MUST, entry):** Given the example app's configuration screen has the new switch
  "IBAN mismatch spike" turned **on** and the bank SDK is started through
  `GiniBank.startCaptureFlow` (Activity integration) or `GiniBank.createCaptureFlowFragment`
  (Fragment integration), when a document analysis finishes with extractions, then the IBAN
  mismatch modal appears over the Analysis screen before any other warning sheet and before
  the extractions are handed to the host.
- **R2 (MUST, entry):** Given the switch is **off** (the default), when a document analysis
  finishes with extractions, then the flow behaves exactly as on `main`: no mismatch modal,
  and the existing already-paid / credit-note / due-date / schedule-payment decision runs
  unchanged.

### Happy path

- **R3 (MUST, happy):** Given the mismatch modal is shown, then it displays the QR IBAN
  `DE02120300000000202051` and the printed IBAN `DE02500105170137075030` (sample values from
  the toggle), a "Cancel transfer" action, a "Continue" action, and a "Choose IBAN" action.
- **R4 (MUST, happy):** Given the mismatch modal is shown, when the user taps "Choose IBAN",
  then a Material 3 `ModalBottomSheet` opens **on top of** the mismatch modal, listing both
  IBANs as selectable rows with the QR IBAN preselected, and the mismatch modal stays
  visible and dimmed behind it.
- **R5 (MUST, happy):** Given the selection sheet is open, when the user taps the printed
  IBAN row, then the sheet closes and the mismatch modal shows the printed IBAN as the
  selected IBAN; the selection is reflected in the modal, not only inside the sheet.
- **R6 (MUST, happy):** Given the mismatch modal is shown (with or without the sheet open),
  when the user taps "Continue", then the sheet (if open) and the modal close together in one
  step, and the flow continues exactly as the "Proceed anyway" CTA of
  `WarningType.DOCUMENT_MARKED_AS_PAID` does today, i.e. the Analysis presenter's
  `proceedWithExtractions(resultHolder)` runs and the next screen (Return Assistant, Skonto
  or the host's result callback) is reached. The selected IBAN is logged at `DEBUG` level
  with the tag `IbanMismatchSpike`; it is **not** written into the extractions (out of scope).
- **R7 (MUST, happy):** Given the mismatch modal is shown, when the user taps "Cancel
  transfer", then the modal closes and the flow cancels exactly as the "Cancel transfer" CTA
  of the existing warning sheets does (`AnalysisFragment.cancelTransaction`: image store
  cleared, `CancelListener.onCancelFlow()`), ending in `CaptureResult.Cancel` for both
  integration modes.

### Dismissal (the layering questions)

- **R8 (MUST, happy):** Given the selection sheet is open, when the user presses the system
  back button, performs the back gesture, or swipes the sheet down, then **only the sheet**
  closes; the mismatch modal stays, the Analysis screen stays, and the bank's host screen is
  not popped or finished. The three gestures must be tested separately.
- **R9 (MUST, happy):** Given the mismatch modal is shown and the sheet is closed, when the
  user presses the back button or performs the back gesture, then the modal does **not**
  dismiss and nothing behind it reacts (same policy as `WarningBottomSheet`, which sets
  `isCancelable = false`). Tapping the scrim does not dismiss the modal either.
- **R10 (MUST, happy):** Given the mismatch modal is shown with the sheet open, when the
  device rotates, then after rotation the modal is still shown **and** the sheet is still
  open with the same selected IBAN, in both integration modes and on phone and tablet.
- **R11 (MUST, happy):** Given the mismatch modal is shown with the sheet open and an IBAN
  selected, when the process is killed in the background and the app is restored (developer
  option "Don't keep activities", or `adb shell am kill <package>` from Recents), then the
  modal and the sheet are restored with the same selected IBAN, in both integration modes.
  If restoration cannot be achieved for one construction, the write-up records it as a
  limitation together with the fallback construction (see Design).

### Error path

- **R12 (MUST, error):** Given the mismatch modal is shown, when the hosting Fragment's view
  is destroyed because the flow is cancelled by the host (`GiniBank` termination callback,
  Activity finished by the bank), then neither the modal nor the sheet leaks a window: no
  `WindowLeaked` log line and no crash on the next launch of the flow.
- **R13 (SHOULD, error):** Given the switch is on but the analysis returns **no**
  extractions, then the mismatch modal is **not** shown and the existing no-results path
  runs; the spike hook only sits in the `SUCCESS_WITH_EXTRACTIONS` branch.

### Deliverable

- **R14 (MUST, entry):** The spike's answer is written to
  `specs/PP-3941-spike-findings.md` in the structure of the ticket: one section per question
  (7), each with the observed result per integration mode (Activity / Fragment) and device
  class (phone / tablet), then "Recommended approach", "Limitations", and "Setup the banks
  need" (expected to be "none", but verified, not assumed). The user posts it as the Jira
  comment; `/gini-build` does not post to Jira.
- **R15 (SHOULD, entry):** The findings file names the fallback construction and what would
  trigger choosing it, so the real feature ticket can start without re-running the spike.

## Affected modules

| Module | Change | Fate |
|---|---|---|
| `capture-sdk:sdk` | Spike hook in the Analysis screen (legacy MVP) + new Compose modal/sheet files + a public spike toggle object + two string resources | Throwaway, branch only |
| `bank-sdk:example-app` | New switch on the configuration screen, wired to the toggle | Throwaway, branch only |
| `bank-sdk:sdk` | **No change.** The toggle is set directly on the capture-sdk object; `CaptureConfiguration` is not extended | — |

The example app already depends on `capture-sdk:sdk` directly
(`bank-sdk/example-app/build.gradle.kts` line 204), so it can set a capture-sdk toggle
without routing through `bank-sdk:sdk`.

## Public API impact

- `capture-sdk:sdk`: **one additive public declaration, spike only**:
  `net.gini.android.capture.analysis.fraud.IbanMismatchSpike` (a Kotlin `object` with
  `@JvmStatic var enabled: Boolean`), annotated `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)`.
  It must be public because the example app is a separate Gradle module and Kotlin
  `internal` does not cross module boundaries. Consequence: `capture-sdk:sdk:apiCheck`
  fails on the branch. This is accepted **because the branch is never merged**; do **not**
  run `apiDump` to re-pin the dump. If the branch were ever considered for merging, the
  object must be deleted first.
- Everything else added in `capture-sdk:sdk` is `internal` or `private`; the Java additions
  to the Analysis MVP classes are package-private (`AnalysisScreenContract.View` methods are
  already package-private abstract methods).
- `bank-sdk:sdk`: none. `bank-sdk:example-app`: not a published module.

## Technical conventions

1. **Language.** New files are Kotlin, `internal` by default (only `IbanMismatchSpike` is
   public, see above). Legacy Java files that **may** be touched, each for one reason:
   - `capture-sdk/sdk/src/main/java/net/gini/android/capture/analysis/AnalysisScreenPresenter.java`:
     one new `else if` branch in the `SUCCESS_WITH_EXTRACTIONS` case, placed **before**
     `shouldShowAlreadyPaidInvoiceWarning`, guarded by `IbanMismatchSpike.enabled`, that
     stores `successResultHolder = resultHolder`, sets `shouldClearImageCaches = false`
     (same bookkeeping as the sibling branches) and calls the Kotlin extension.
   - `capture-sdk/sdk/src/main/java/net/gini/android/capture/analysis/AnalysisScreenContract.java`:
     one new package-private abstract `View` method
     `showIbanMismatch(IbanMismatchArgs args, Consumer<String> onContinue)` (Java-callable;
     `Consumer` from `androidx.core.util`, on the classpath through
     `libs.androidx.core.ktx` in `capture-sdk/sdk/build.gradle.kts` line 139; `java.util.function`
     is avoided because minSdk is 23). Cancel is
     wired inside `AnalysisFragment` to the existing `cancelTransaction()` like the other
     warnings.
   - `capture-sdk/sdk/src/main/java/net/gini/android/capture/analysis/AnalysisFragmentImpl.java`:
     delegate to `mFragment`, calling `stopAndHideHints()` first exactly as `showWarning`
     does (TalkBack focus).
   - `capture-sdk/sdk/src/main/java/net/gini/android/capture/analysis/AnalysisFragment.java`:
     implement the new view method by delegating to `AnalysisFragmentExtension`.
   No other Java is converted or restructured. Kotlin files touched:
   `analysis/AnalysisScreenPresenterExtension.kt` (new `showIbanMismatchWarning`) and
   `analysis/AnalysisFragmentExtension.kt` (new `showIbanMismatch`/`hideIbanMismatch`,
   mirroring `showEducation`/`hideEducation`).
2. **UI.** Compose, Material 3 (`androidx.compose.material3` 1.4.0 through
   `compose-bom = 2026.02.00`), wrapped in `GiniTheme`
   (`capture-sdk/sdk/src/main/java/net/gini/android/capture/ui/theme/GiniTheme.kt`), typography
   through `GiniTheme.typography`, colours through `GiniTheme.colors` as in
   `bank-sdk/.../transactiondocs/ui/dialog/attachdoc/AttachDocumentToTransactionDialog.kt`.
   Light and dark `@Preview`s for the modal content and the sheet content
   (`@Preview` + `@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)`, the pattern in
   `capture-sdk/.../ui/components/topbar/GiniTopBar.kt` line 66). **XML:** one new
   `ComposeView` with id `gc_iban_mismatch_container` is added to
   `capture-sdk/sdk/src/main/res/layout/gc_fragment_analysis.xml` next to the existing
   `gc_education_container` (line 82); no XML layouts are removed. Vector-drawable handling
   stays as-is; the spike adds no drawables.
   Tablet: unlike `WarningBottomSheet`, the mismatch modal does not switch on
   `R.bool.gc_is_tablet`. It is the same full-width, full-height non-dismissable
   `ModalBottomSheet` on phones and tablets, and the selection sheet stays a `ModalBottomSheet`
   on both device classes; the write-up records how it looks on a tablet.
3. **Architecture.** The Analysis screen is **legacy Java MVP** (`AnalysisScreenContract`,
   `AnalysisScreenPresenter`, `AnalysisFragmentImpl` + the Kotlin extension files). Per
   `platform.md` the spike integrates **at the contract boundary** and does not add a
   ViewModel. UI state for the two layers is one Compose-side holder:
   - `internal data class IbanMismatchUiState(val qrIban: String, val printedIban: String, val selectedIban: String, val isSheetVisible: Boolean)` — `@Parcelize`, kept with
     `rememberSaveable` so it survives rotation and process death through the Fragment
     view's `SavedStateRegistry`.
   - `internal data class IbanMismatchArgs(val qrIban: String, val printedIban: String)`
     (`@Parcelize`), the input from the presenter.
   - No intent/effect classes: the two outcomes are plain callbacks `onContinue(selectedIban)`
     and `onCancel()` that the Fragment maps onto the presenter, like
     `WarningBottomSheet.Listener`. Orbit-MVI is bank-sdk only and is not used here.
4. **DI and async.** No new Koin bindings: `IbanMismatchSpike` is a static toggle read in the
   presenter; nothing else needs injection. No coroutines are introduced: the sheet's
   `SheetState.hide()` is a suspend call driven by `rememberCoroutineScope()` inside the
   composition (standard Material 3 usage), which is the only async in the spike. No
   LiveData, no RxJava. No `Context` is captured in any static.
5. **Strings and resources.** `capture-sdk:sdk` keeps its default locale in `values/`
   (German) with English in `values-en/` (verified: `gc_proceed_anyway` exists in both).
   New keys, prefixed `gc_iban_mismatch_` and added to **both** folders: title, description,
   `gc_iban_mismatch_qr_iban_label`, `gc_iban_mismatch_printed_iban_label`,
   `gc_iban_mismatch_choose_iban`, `gc_iban_mismatch_sheet_title`, `gc_iban_mismatch_continue`.
   Reuse `gc_cancel_transfer` for the cancel CTA. No placeholders are needed because the
   IBANs are rendered as separate `Text`s. `values-night`, `values-land`, `values-sw600dp*`
   get no new entries. The example app's switch label goes into
   `bank-sdk/example-app/src/main/res/values/strings.xml` **and** `values-en/strings.xml`
   (the example app has `values`, `values-en`, `values-night`, `values-w820dp`; only the
   first two carry strings).
6. **Quality gates.** Run `./gradlew capture-sdk:sdk:ktlintCheck capture-sdk:sdk:detekt` and
   keep them clean for the Kotlin files (the `LongMethod` rule ignores `@Composable`;
   `FunctionNaming` is satisfied by the existing Compose convention in the module). `lint`
   must pass `MissingTranslation` (hence both locale folders). `apiCheck` is **expected to
   fail** on the branch (see Public API impact). No Jacoco/Sonar expectation: the code is
   throwaway and has no unit tests by decision.

## Design

### Construction under test (primary) and fallback

**Primary — pure Compose, two windows:**

1. The mismatch modal is a Compose `androidx.compose.ui.window.Dialog` with
   `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false, decorFitsSystemWindows = false)`.
   Precedent for a non-dismissable Compose dialog in this repo:
   `AttachDocumentToTransactionDialog.kt` (bank-sdk). Its content is
   `IbanMismatchModalContent(state, onChooseIban, onContinue, onCancel)`.
2. The selection sheet is `androidx.compose.material3.ModalBottomSheet` composed **inside the
   Dialog's content** when `state.isSheetVisible` is true, with
   `rememberModalBottomSheetState(skipPartiallyExpanded = true)`. `ModalBottomSheet` opens its
   own window on top of the dialog window; its `onDismissRequest` handles swipe-down and
   back (Material 3 routes predictive back and the back button of the sheet window to
   `onDismissRequest`). That gives the layering the ticket asks about: back/gesture/swipe
   are consumed by the topmost window first.
   Nothing in the repo uses `ModalBottomSheet` yet (verified by grep); this is its first use
   and the write-up records whether `@OptIn(ExperimentalMaterial3Api::class)` is still
   required in 1.4.0 (confidence: LOW — the compiler tells us on the first build; it is
   not load-bearing).
3. Both layers are driven by one `rememberSaveable { mutableStateOf(IbanMismatchUiState(...)) }`
   in the composition hosted by `gc_iban_mismatch_container`. "Continue" calls
   `onContinue(state.selectedIban)`; the Fragment then calls
   `AnalysisFragmentExtension.hideIbanMismatch()` which sets the container's content to
   empty and `GONE`, so modal and sheet leave composition together and both windows are
   torn down in the same frame — that is the answer to the ticket's question 7.

**Fallback — Fragment-managed sheet:** if R8, R10 or R11 fails for the pure-Compose
construction, the second candidate is a `BottomSheetDialogFragment` hosting a `ComposeView`
(the structure of `capture-sdk/.../analysis/warning/WarningBottomSheet.kt`, shown through
the **activity** `FragmentManager` exactly as `AnalysisFragment.showWarning` does at line
60/247), while the mismatch modal stays a Compose `Dialog`. The write-up must state which
candidate is recommended and why; building the fallback is **only** done if the primary
fails one of R8, R10, R11 — otherwise it is described, not built (timebox).

### Data flow (spike)

```
example app ConfigurationActivity switch
  → ExampleAppBankConfiguration.isIbanMismatchSpikeEnabled
  → ConfigurationViewModel (where CaptureConfiguration is built, ~line 130)
      sets IbanMismatchSpike.enabled = configuration.isIbanMismatchSpikeEnabled
AnalysisScreenPresenter (Java, SUCCESS_WITH_EXTRACTIONS)
  if (IbanMismatchSpike.enabled) → extension.showIbanMismatchWarning(resultHolder, activity)
AnalysisScreenPresenterExtension.showIbanMismatchWarning
  → view.showIbanMismatch(IbanMismatchArgs(sampleQrIban, samplePrintedIban), onContinue = { iban -> log; proceedWithExtractions(resultHolder) })
AnalysisFragmentImpl.showIbanMismatch → stopAndHideHints(); mFragment.showIbanMismatch(...)
AnalysisFragment.showIbanMismatch → analysisFragmentExtension.showIbanMismatch(args, onContinue, onCancel = this::cancelTransaction)
AnalysisFragmentExtension.showIbanMismatch → composeView.setContent { GiniTheme { IbanMismatchModal(...) } }
```

The sample IBANs live as constants in `IbanMismatchSpike` (capture-sdk), next to the toggle,
so the hardcoded data is in one clearly-spike file.

### Files

New, `capture-sdk/sdk/src/main/java/net/gini/android/capture/analysis/fraud/`:

- `IbanMismatchSpike.kt` — the public `@RestrictTo` toggle object + sample IBAN constants.
- `IbanMismatchArgs.kt`, `IbanMismatchUiState.kt` — `@Parcelize` data classes (internal).
- `IbanMismatchModal.kt` — `internal fun IbanMismatchModal(args, onContinue, onCancel)`:
  the `Dialog`, the `rememberSaveable` state, the tablet/phone switch, and the
  `ModalBottomSheet` wiring; plus `IbanMismatchModalContent` and its two previews.
- `IbanSelectionSheet.kt` — `internal fun IbanSelectionSheet(state, onSelect, onDismiss)`:
  the `ModalBottomSheet` with two selectable rows and its two previews.

Changed (listed under Technical conventions 1):
`AnalysisScreenPresenter.java`, `AnalysisScreenContract.java`, `AnalysisFragmentImpl.java`,
`AnalysisFragment.java`, `AnalysisScreenPresenterExtension.kt`, `AnalysisFragmentExtension.kt`,
`res/layout/gc_fragment_analysis.xml`, `res/values/strings.xml`, `res/values-en/strings.xml`.

Example app: `ui/data/ExampleAppBankConfiguration.kt` (new Boolean, default `false`),
`ui/ConfigurationActivity.kt` (switch, following the `switchFlashOnByDefault` pattern at
lines 142–146 and 364–382), `ui/ConfigurationViewModel.kt` (set the toggle), the layout that
holds the existing toggles, and `res/values/strings.xml` + `res/values-en/strings.xml`.

### Integration points that the spike exercises

- **Activity integration:** `GiniBank.startCaptureFlow(launcher)` →
  `bank-sdk/.../capture/CaptureFlowActivity.kt` → `CaptureFlowFragment` → `GiniCaptureFragment`
  → child nav graph → `AnalysisFragment`. The example app's `MainActivity` does this.
- **Fragment integration:** `GiniBank.createCaptureFlowFragment()` placed by the host into
  its own `supportFragmentManager` with a back-stack entry
  (`bank-sdk/example-app/.../ui/ClientBankSDKFragment.kt` lines 114–121). Here the bank's
  back stack sits directly under the SDK, which is why R8/R9 must prove the modal windows
  consume back events before `CaptureFlowFragment`'s primary-navigation handling
  (`CaptureFlowFragment.onResume`, `setPrimaryNavigationFragment`) ever sees them.
- Both hosts use `GiniCaptureTheme` through `onGetLayoutInflater`; `GiniTheme` resolves the
  Compose colours from the same resources, so the modal inherits the integrator's theming.

### Write-up

`specs/PP-3941-spike-findings.md` is written by `/gini-build` from the manual matrix results
the user records (the build cannot run the device matrix itself; it prepares the file with
the seven question headings, the matrix table and the recommendation skeleton, and the user
fills the observed cells or dictates them). The final text is the Jira comment.

## Test plan

By decision there are **no automated tests**: the code is throwaway and never merged.
Every MUST requirement maps to a manual case below. The matrix is run on one phone
emulator/device and one tablet (sw600dp emulator is enough) × Activity and Fragment
integration of the example app (`assembleDevExampleAppDebug`, QA flavor not needed).

| Case | Requirement | Steps | Expected |
|---|---|---|---|
| M1 | R1 | Toggle on → start flow (Activity, then Fragment) → analyse any invoice | Mismatch modal over Analysis in both modes |
| M2 | R2 | Toggle off → same | No modal; existing warning logic unchanged |
| M3 | R3 | Inspect modal | Both sample IBANs, Cancel transfer, Continue, Choose IBAN |
| M4 | R4 | Tap Choose IBAN | Sheet on top, modal dimmed behind, QR IBAN preselected |
| M5 | R5 | Tap printed IBAN row | Sheet closes, modal shows printed IBAN selected |
| M6 | R6 | Continue with sheet open, and again with sheet closed | Both layers gone in one step; next screen / result reached; `IbanMismatchSpike` debug log shows IBAN |
| M7 | R7 | Cancel transfer | Flow ends with `CaptureResult.Cancel` in both modes |
| M8a/b/c | R8 | Sheet open → back button / back gesture / swipe down (three runs) | Only sheet closes; modal, Analysis and host remain |
| M9 | R9 | Sheet closed → back button, back gesture, tap scrim | Nothing dismisses |
| M10 | R10 | Sheet open, IBAN selected → rotate | Modal + sheet + selection restored (phone, tablet, both modes) |
| M11 | R11 | Sheet open, IBAN selected → "Don't keep activities" / `am kill` → restore | Modal + sheet + selection restored, or limitation recorded |
| M12 | R12 | Modal shown → host finishes the Activity / pops the Fragment | No `WindowLeaked`, no crash on next start |
| M13 | R13 | Toggle on, document with no extractions | No-results path, no modal |
| M14 | R14, R15 | Read `specs/PP-3941-spike-findings.md` | Seven answered questions, recommendation, limitations, bank setup, fallback named |

Roughly 14 cases × 2 integration modes × 2 device classes; R10 and R11 are the ones most
likely to need the fallback and should be run first after M1–M6 pass once.

### Not tested

- **No unit or Compose UI tests** (`createComposeRule` under Robolectric as in
  `bank-sdk/.../capture/skonto/SkontoScreenContentTest.kt` would be the stack if the
  decision changes). Reason: throwaway code, 1-day timebox, user decision.
- **Material 3 internals** (`ModalBottomSheet` window creation, predictive-back handling,
  `SheetState` animation) are framework behaviour and are observed, not tested.
- **Real extraction data**: `fraudSignals` does not exist yet; the IBANs are constants.
- **Production design, accessibility and 200 % font scaling**: belong to the real feature
  ticket once PP-3749 delivers the design. The spike layout only has to be legible.
- **iOS parity**: PP-3938 is the iOS sibling and is independent.

## Out of scope

- Writing the selected IBAN back into the extractions or into the transfer summary.
- Reading `fraudSignals` (PP-3952), `fraudSignalsEnabled` from `/configurations` (PP-3951)
  or any feature-flag plumbing in `capture-sdk:default-network` / `bank-api-library`.
- Changes to `bank-sdk:sdk` (`CaptureConfiguration`, `GiniBank`) or to `GiniCapture.Builder`.
- Any public API meant to stay: nothing from this branch is merged, so no deprecation or
  `apiDump` work.
- Production visuals from PP-3749, strings review, analytics events, Xray test cases
  (PP-3943).
- Converting the Analysis screen's Java MVP to Kotlin or MVVM.
- Posting the Jira comment: the user posts the finished findings file.

## Open questions

- `@OptIn(ExperimentalMaterial3Api::class)` for `ModalBottomSheet` in material3 1.4.0:
  unknown until the first compile; trivial either way, recorded in the write-up.
- Whether the real feature will later need the IBAN mismatch modal **outside** the Analysis
  screen (e.g. after Return Assistant / Skonto in `bank-sdk:sdk`). If so, the recommended
  construction must also be checked inside a Compose-hosted bank-sdk Fragment; this is a
  product question for the epic, not for the spike, and is flagged in the write-up under
  "Limitations".

## Implementation plan
- [x] 1. `capture-sdk:sdk` — `analysis/fraud/IbanMismatchSpike.kt` (public `@RestrictTo` toggle + sample IBANs), `IbanMismatchArgs.kt`, `IbanMismatchUiState.kt` (`@Parcelize`, internal) (requirements 1, 3)
- [x] 2. `capture-sdk:sdk` — `gc_iban_mismatch_*` strings in `res/values/strings.xml` and `res/values-en/strings.xml`; example-app switch label in both of its string folders (requirement 3)
- [x] 3. `capture-sdk:sdk` — `analysis/fraud/IbanSelectionSheet.kt` (Material 3 `ModalBottomSheet`, two selectable rows, previews) (requirements 4, 5, 8)
- [x] 4. `capture-sdk:sdk` — `analysis/fraud/IbanMismatchModal.kt` (non-dismissable Compose `Dialog`, `rememberSaveable` state, phone/tablet switch, sheet wiring, previews) (requirements 3, 4, 5, 6, 9, 10, 11)
- [x] 5. `capture-sdk:sdk` — `gc_iban_mismatch_container` ComposeView in `gc_fragment_analysis.xml`; `AnalysisFragmentExtension.showIbanMismatch/hideIbanMismatch`; `AnalysisScreenContract.View.showIbanMismatch`; `AnalysisFragmentImpl.showIbanMismatch` (stopAndHideHints + cancel wired like `cancelTransaction`) (requirements 6, 7, 12)
- [x] 6. `capture-sdk:sdk` — `AnalysisScreenPresenterExtension.showIbanMismatchWarning` + guarded `else if` in `AnalysisScreenPresenter` `SUCCESS_WITH_EXTRACTIONS` (requirements 1, 2, 6, 13)
- [x] 7. `bank-sdk:example-app` — `isIbanMismatchSpikeEnabled` in `ExampleAppBankConfiguration`, switch in `layout_debug_development_toggles.xml`, wiring in `ConfigurationActivity`, `IbanMismatchSpike.enabled` set in `ConfigurationViewModel.configureGiniBank` (requirements 1, 2)
- [x] 8. `specs/PP-3941-spike-findings.md` skeleton: seven question sections with the Activity/Fragment × phone/tablet matrix, recommendation, limitations, fallback, bank setup (requirements 14, 15)

### Implementation notes (deviations found while building, same behaviour)

- The view method lives in `AnalysisFragmentImpl` (which owns `AnalysisFragmentExtension`, like
  `showEducation`), not in `AnalysisFragment.java`; `FragmentImplCallback` is untouched. Cancel runs
  the same two steps as `AnalysisFragment.cancelTransaction()` (image store clear +
  `CancelListener.onCancelFlow()`).
- The Java boundary (`AnalysisScreenContract.View.showIbanMismatch`,
  `AnalysisFragmentExtension.showIbanMismatch`) carries the two IBAN `String`s, not
  `IbanMismatchArgs`: a public Kotlin function cannot expose an `internal` type, and the extension
  class is public for its Java caller. `IbanMismatchArgs` is built inside the extension.
- Public API additions on the branch are therefore **three**, not one: `IbanMismatchSpike` plus
  `AnalysisFragmentExtension.showIbanMismatch/hideIbanMismatch` (the class was already public).
  `apiCheck` also lists the generated Parcelize creators, `ComposableSingletons` and the ViewBinding
  field. All leave with the branch.
- The selection sheet carries its own "Continue" CTA: the `ModalBottomSheet` scrim covers the
  modal's buttons while the sheet is up, so M6 "Continue with the sheet open" is only reachable from
  the sheet.
- `@OptIn(ExperimentalMaterial3Api::class)` is still required for `ModalBottomSheet` in material3
  1.4.0 (verified by compiling without it).
- R11: "Don't keep activities" keeps the process (static toggle and `GiniCapture` instance survive)
  and is testable. `adb shell am kill` is not answerable by the construction: the example app's
  configuration is in-memory, so `IbanMismatchSpike.enabled` resets, and `GiniCapture` has no
  instance until the host re-configures it, so the Analysis screen shows the general error before
  the spike hook runs. Recorded as a limitation in the findings file.
- `AnalysisScreenPresenterExtension` got a class-level `@Suppress("TooManyFunctions")` (repo
  precedent: `CaptureFlowFragment`), because the one new method reached detekt's threshold.

# PP-3966: [Android] IBAN selection sheet — use the chosen IBAN

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-3966
Parent epic: PP-3518 "[Q4] Fraud prevention warnings – phase 1"
Branch: `PP-3961-wire-fraud-signals` (planned here at the user's request; the selection sheet
and the real fraud signals are already wired on this branch)
Builds on: PP-3961 phase 1 (the sheet `analysis/fraud/IBANSelectionSheet.kt`, built by a
colleague) and phase 2 (`IBANMismatchArgsMapper`, presenter wiring) — see `specs/PP-3961-feature.md`.

## Problem

After "Proceed anyway" on the QR-vs-printed or combined warning, the user picks one IBAN on the
selection sheet. Today the choice is thrown away: `AnalysisScreenPresenterExtension.showIbanMismatchWarning`
receives it as `_: String?` and hands the backend's extractions on unchanged. So the bank's
transfer form is filled with `extractions.iban` — which per the API is always the **QR code**
IBAN — even when the user chose the printed one. The ticket's AC "the next screen shows the
chosen IBAN" is not met.

Verified on a Samsung S22 against prod with the Musterfirma Girocode invoice: backend
`iban` = `DE89370400440532013000` (QR), printed `DE02120300000000202051`; choosing `DE02…` still
delivered `DE89…`.

## Requirements

Common setup: `fraudSignalsEnabled = true`, the analysis returns a `QR_PRINTED_IBAN_MISMATCH`
signal (alone or with `IBAN_COUNTRY_MISMATCH`), so the warning and the selection sheet appear.
"Backend IBAN" = `extractions["iban"].value` as the analysis delivered it. IBANs are compared
after removing whitespace and upper-casing ("normalised"), as in `IBANMismatchArgsMapper`.

- **R1 (MUST, entry):** Given the backend IBAN `<Q>` and a printed IBAN `<P>`, when the user picks
  `<P>` on the selection sheet and taps "Next", then `AnalysisFragmentListener.onExtractionsAvailable`
  receives specific extractions whose `"iban"` value is `<P>` (the raw value shown on the sheet),
  and the bank-sdk `CaptureResult.Success.specificExtractions["iban"]` the integrator gets is `<P>`.
- **R2 (MUST, happy):** In R1, the new `"iban"` extraction has `name = "iban"`, `entity = "iban"`,
  `box = null` (the old box pointed at the QR IBAN), the original `candidates` list unchanged, and
  `isDirty() == true`.
- **R3 (MUST, happy):** In R1, the `bic` extraction is removed (it belongs to the backend IBAN,
  and the response does not say which BIC belongs to the chosen one; banks derive the BIC from the
  IBAN — confirmed with the backend team, 2026-11-10). Every other specific extraction and every
  compound extraction is handed on exactly as before PP-3966 (compound extractions still without
  `fraudSignals`). In R4 (QR IBAN kept) `bic` stays.
- **R4 (MUST, happy):** Given the user picks the QR IBAN (normalised equal to the backend IBAN),
  then the extractions are handed on unchanged — same `"iban"` instance, `isDirty()` stays as the
  backend delivered it.
- **R5 (MUST, happy):** Given the country-only warning (no sheet, `onProceed(null)`), then the
  extractions are handed on unchanged.
- **R6 (MUST, async):** Given local invoice saving is enabled and the user picks `<P>`, when the
  Storage Access Framework folder picker opens and the Analysis screen is recreated meanwhile
  (rotation, or the process is killed and restored), then after saving finishes
  `onExtractionsAvailable` still receives `"iban"` = `<P>` (the choice is persisted with the
  existing pending-saving state, not re-asked).
- **R7 (MUST, async):** Same as R6 without a recreation (saving finishes while the presenter is
  alive, `resumeInterruptedFlow`): `"iban"` = `<P>`.
- **R8 (MUST, error):** Given the backend delivered no `"iban"` extraction but the user chose
  `<P>` (should not happen per the API; defensive), then `"iban"` is added with value `<P>` and the
  R2 shape (empty candidates). Given the choice is blank, the extractions are handed on unchanged.
  Applying the choice never throws.
- **R9 (MUST, happy):** The choice of one analysis never leaks into another: after the extractions
  are handed on, or a new analysis starts, the stored choice is cleared, so the next document's
  warnings, hints and plain proceed see the backend IBAN.

### Device verification of the remaining ticket ACs (manual, see Test plan)

The sheet itself was built in PP-3961. These ticket ACs are verified on device as part of
PP-3966 and fixed here only if they fail (a fix to the sheet is a design question for its
author first):

- **R10 (MUST, manual):** "Next" closes the sheet and the warning together; the next screen (Return
  Assistant or the bank's screen) receives `<P>`.
- **R11 (MUST, manual):** Back button, back gesture and swipe-down on the sheet return to the warning
  and never close the SDK or the bank's screen.
- **R12 (MUST, manual):** Works when the bank starts the SDK as an Activity (`CaptureFlowActivity`) and
  when it embeds it as a Fragment (`CaptureFlowFragment`), on a phone and on a tablet.
- **R13 (MUST, manual):** The sheet keeps its selection after rotation and after "Don't keep
  activities" / process kill and restore.
- **R14 (MUST, manual):** 200% font size, long IBANs wrap, TalkBack reads each row (label, IBAN
  spelled, hint, selected state), an external keyboard can move focus, select a row and press "Next".

## Affected modules

| Module | Change |
|---|---|
| `capture-sdk:sdk` | New internal helper in `analysis/fraud/`; `AnalysisScreenPresenterExtension` stores and applies the choice; the saving-state contract carries the choice; `AnalysisFragmentImpl`/`AnalysisFragment` persist it. |
| `bank-sdk:sdk` | No code change. It receives the changed extractions through `CaptureFlowFragment`'s `onExtractionsAvailable`. Run its unit tests (depends on `capture-sdk:sdk`). |

`bank-api-library:library`, `capture-sdk:default-network`: unchanged.

## Public API impact

None. Every touched declaration is `internal` Kotlin or package-private Java
(`AnalysisScreenContract`, `AnalysisFragmentImpl`, the new helper). `GiniCaptureSpecificExtraction`
is only constructed and `setIsDirty` called — both already public. `./gradlew capture-sdk:sdk:apiCheck`
must pass with **no** `apiDump`; a dump diff means something leaked.

Integrator-visible behaviour change: `CaptureResult.Success.specificExtractions["iban"]` can now be
the printed IBAN with `isDirty() == true` when the user chose it. Worth one line in the PP-3968 docs.

## Technical conventions

1. **Language.** New code is Kotlin, `internal`, under `capture-sdk/sdk/src/main/java/`. Legacy Java
   touched, only at the saving-state boundary:
   - `analysis/AnalysisScreenContract.java` — `processInvoiceSaving` and `updateInvoiceSavingState`
     gain a `@Nullable String chosenIban` parameter.
   - `analysis/AnalysisFragmentImpl.java` — field, getter, `onCreate` restore, new
     `CHOSEN_IBAN_KEY = "chosenIban"`, pass-through in `processInvoiceSaving` and the
     `updateInvoiceSavingState` call (line ~565).
   - `analysis/AnalysisFragment.java` — `onSaveInstanceState` puts `CHOSEN_IBAN_KEY`.
   - `analysis/AnalysisScreenPresenter.java` — `updateInvoiceSavingState` forwards it to the
     extension.
   No other Java changes; no conversion.
2. **UI.** No UI change. `IBANSelectionSheet.kt` and `IBANMismatchWarningScreen.kt` stay as they are
   (they already report the chosen raw IBAN through `onProceed`). No XML layout change.
3. **Architecture.** Legacy Java MVP stays: the extension (presenter side) owns the choice; the view
   only persists it, exactly like `PendingSavingAction`. The new logic is a pure, stateless Kotlin
   object (`ChosenIbanExtractions`) — no ViewModel, no state/intent/effect classes.
4. **DI / async.** No Koin binding (object). Everything runs synchronously inside the existing
   callbacks; no new coroutines, no dispatcher change.
5. **Strings/resources.** None.
6. **Quality gates.** `ktlintCheck`, `detekt`, `lint`, `apiCheck` clean on `capture-sdk:sdk`;
   `bank-sdk:sdk:testDebugUnitTest` green. The new object gets full line coverage in Jacoco.

## Design

### Apply the choice (`analysis/fraud/ChosenIbanExtractions.kt`, new)

```kotlin
internal object ChosenIbanExtractions {
    /** [extractions] with "iban" replaced by [chosenIban]; the same map when there is nothing to change. */
    fun apply(
        extractions: Map<String, GiniCaptureSpecificExtraction>,
        chosenIban: String?,
    ): Map<String, GiniCaptureSpecificExtraction>
}
```

- `chosenIban` null/blank → return `extractions` (R5, R8).
- Existing `"iban"` normalised equal to `chosenIban` → return `extractions` (R4).
- Otherwise copy the map and put `GiniCaptureSpecificExtraction("iban", chosenIban, "iban", null,
  existing?.candidates ?: emptyList())` with `setIsDirty(true)` (R1, R2, R8). Same idea as
  `AnalysisScreenPresenter.removeAmountToPay` (line ~788), but compound extractions are kept.
- Normalisation is the mapper's rule (strip whitespace, upper-case); reuse it by moving
  `IBANMismatchArgsMapper`'s private `String.normalized()` to an `internal` top-level function in
  `analysis/fraud/` rather than copying it.

### Store and use it (`analysis/AnalysisScreenPresenterExtension.kt`)

- New `private var chosenIban: String? = null` next to `pendingSavingAction`.
- `showIbanMismatchWarning`: the `onProceed` lambda becomes `{ iban: String? -> chosenIban = iban; handleSaveInvoicesLocally(...) }`.
- `handleSaveInvoicesLocally`: `view.processInvoiceSaving(pendingAction.name, chosenIban)`.
- `runPendingSavingAction(action, resultHolder, activity)`: before dispatching, replace
  `resultHolder` with one whose extractions are `ChosenIbanExtractions.apply(..., chosenIban)` and
  whose compound extractions, document id and file name are unchanged; then clear `chosenIban`
  (R9). Every exit — direct proceed, `resumeAfterInvoiceSaving` (R7), the recreated-flow path where
  `showIbanMismatchWarning` runs with `isSavingInvoicesInProgress = true` (R6) — goes through here,
  so the presenter's `successResultHolder` can stay the unmodified holder.
- `restorePendingSavingAction(actionName)` becomes `restoreInvoiceSavingState(actionName, chosenIban)`
  (or gains the parameter) and restores `chosenIban`.
- R9 needs no extra reset point: the extension is created per presenter
  (`AnalysisScreenPresenter.java` line 125, `new AnalysisScreenPresenterExtension(view)`), and the
  presenter per Analysis fragment (`AnalysisFragmentImpl.java` line 125), so a new analysis starts
  with `chosenIban = null`. Clearing it in `runPendingSavingAction` covers the same presenter
  being reused, e.g. a retry. `pendingSavingAction` has the same lifecycle today.

### Persist it (view)

Mirror `PENDING_SAVING_ACTION_KEY` (`AnalysisFragmentImpl.java` line 80, `onCreate` line ~432,
getter line ~290; `AnalysisFragment.java` `onSaveInstanceState` line ~66). The value is a plain
IBAN string; it is already shown on screen, so the saved-state bundle adds no new data class.

### Unchanged

`AnalysisScreenContract.View.showIbanMismatch(args, Consumer<String>)`,
`AnalysisFragmentImpl.showIbanMismatch`, `AnalysisFragmentExtension.showIbanMismatch`, the sheet and
the warning screen. The already-paid, credit note, payment due and schedule payment flows pass a
null `chosenIban` and behave exactly as today.

## Test plan

Stacks match the neighbours: JUnit4 + Google Truth; the presenter test keeps its Robolectric +
Mockito-Kotlin setup and helpers (`startPresenterForDueHint`, `idleMainLooperUntil`,
`ibanWithCandidates`, `qrMismatchCompounds`, `QR_IBAN`, `PRINTED_IBAN`).

| # | Test class | New / extend | Tests | Covers |
|---|---|---|---|---|
| 1 | `capture-sdk/sdk/src/test/java/net/gini/android/capture/analysis/fraud/ChosenIbanExtractionsTest.kt` | **new**, plain JUnit | 6 | R1, R2, R4, R5, R8 |
| 2 | `capture-sdk/sdk/src/test/java/net/gini/android/capture/analysis/AnalysisScreenPresenterTest.kt` | extend the "IBAN mismatch warning" region | 5 | R1, R3, R6, R7, R9 |

`ChosenIbanExtractionsTest` (6):
1. printed IBAN chosen → `"iban"` value = chosen, entity `iban`, box null, candidates kept, dirty (R1, R2)
2. QR IBAN chosen with different spacing/case → same map instance returned (R4)
3. `null` choice → same map (R5)
4. blank choice → same map (R8)
5. no `"iban"` in the map → added with empty candidates, dirty (R8)
6. after a change `bic` is removed and other extractions (e.g. `amountToPay`) are the same instances (R3)
7. `bic` stays when the QR IBAN is kept (R3, R4)

`AnalysisScreenPresenterTest` (5):
1. Proceed with `PRINTED_IBAN` → `onExtractionsAvailable` gets `"iban"` = `PRINTED_IBAN`, dirty,
   compounds without `fraudSignals` (R1, R3). Replaces the expectation of the current
   "proceed … with an IBAN chosen on the selection sheet" test, which asserts unchanged extractions.
2. Proceed with `QR_IBAN` → `"iban"` unchanged (R4).
3. Invoice saving enabled: proceed with `PRINTED_IBAN` → `processInvoiceSaving` captured with
   `chosenIban = PRINTED_IBAN`; then `resumeInterruptedFlow()` → `"iban"` = `PRINTED_IBAN` (R7).
4. Recreation: `updateInvoiceSavingState(true, "PROCEED", PRINTED_IBAN)` before `start()` → the
   flow continues without showing the warning and delivers `"iban"` = `PRINTED_IBAN` (R6).
5. After a proceed with `PRINTED_IBAN`, a second `runPendingSavingAction` on the same extension
   (e.g. a retry) delivers the backend IBAN (R9).

Commands: `./gradlew capture-sdk:sdk:testDebugUnitTest bank-sdk:sdk:testDebugUnitTest`, then
`/gini-check` (lint, detekt, ktlint) and `./gradlew capture-sdk:sdk:apiCheck`.

### Not tested

- R10–R14 are manual device checks (no Compose UI test setup in `capture-sdk:sdk` unit tests; the
  Activity/Fragment hosting and process death are integration behaviour). Checklist, on the S22
  plus one tablet, `bank-sdk:example-app` prod flavor, the Musterfirma Girocode invoice (real
  `QR_PRINTED_IBAN_MISMATCH` from prod):
  1. Pick the printed IBAN → the example app's extractions screen shows `DE02120300000000202051` (R1, R10).
  2. Same with "Save invoices locally" on; rotate while the folder picker is open (R6).
  3. Same with Developer options → "Don't keep activities" on (R6, R13).
  4. Back / back gesture / swipe-down on the sheet (R11).
  5. Activity entry vs. Fragment entry ("Single Activity" in the example app) (R12).
  6. 200% font + TalkBack + external keyboard (R14).
- `bank-sdk:sdk` hand-over is plain map forwarding; covered by its existing tests, not extended.

## Out of scope

- Pairing BIC and IBAN in the API (would need a compound extraction; not planned for v1).
- Changing the sheet's layout, copy or the multi-IBAN label (PP-3961 / design).
- `IBAN_COUNTRY_MISMATCH` end-to-end from prod (backend has not delivered it yet).
- Writing the choice back to the backend as feedback — the integrator's existing
  `GiniBank.sendTransferSummary` already sends the final IBAN.
- CX mode `crossBorderPayment` compound values — untouched.
- Public docs (PP-3968).

## Open questions

- None. BIC resolved 2026-11-10: the backend cannot pair BICs with IBANs without an API change
  (compound extraction), and banks derive the BIC from the IBAN, so the SDK removes `bic` when a
  different IBAN is chosen (R3). Confirmed by the backend team that the bank may receive the chosen
  IBAN without a `bic`.

## Implementation plan
- [x] 1. `capture-sdk:sdk` — `analysis/fraud/ChosenIbanExtractions.kt` + `ChosenIbanExtractionsTest`; move the mapper's normalisation to a shared `internal fun normalizeIban` in `analysis/fraud/` (R1, R2, R4, R5, R8)
- [x] 2. `capture-sdk:sdk` — `AnalysisScreenPresenterExtension`: store `chosenIban` from the warning's `onProceed`, apply it in `runPendingSavingAction`, then clear it; presenter tests 1, 2, 5 (R1, R3, R4, R9)
- [x] 3. `capture-sdk:sdk` — saving-state contract carries `chosenIban`: `AnalysisScreenContract`, `AnalysisFragmentImpl` (`CHOSEN_IBAN_KEY`), `AnalysisFragment.onSaveInstanceState`, `AnalysisScreenPresenter.updateInvoiceSavingState`, extension restore; presenter tests 3, 4; adapt the existing saving tests to the new parameter (R6, R7)
- [x] 4. Verify: `/gini-check` (capture-sdk:sdk, bank-sdk:sdk), `capture-sdk:sdk:apiCheck` without `apiDump`, `code-reviewer` self-review
- [x] 5. Device: checklist R10–R14 with the Musterfirma Girocode invoice on the S22 (what can be done without a tablet / external keyboard; the rest is reported as not done)

## Build notes

- Verification: `capture-sdk:sdk` and `bank-sdk:sdk` `testDebugUnitTest`, `lint`, `detekt`,
  `ktlintCheck` pass; `capture-sdk:sdk:apiCheck` passes with no `apiDump`. The `code-reviewer`
  self-review found no blockers or warnings; two nits applied (stale KDoc on
  `showIbanMismatchWarning`, simpler condition in `ChosenIbanExtractions.apply`). Not applied: its
  suggestion to clear `chosenIban` in `AnalysisFragmentImpl.processSafFolderSelection` — after the
  folder picker the flow resumes through `onResume` → `start()`, which needs the persisted choice
  (R6); the extension clears its own copy after use (R9).
- Device (Samsung S22, prod, Musterfirma Girocode invoice, real `QR_PRINTED_IBAN_MISMATCH`):
  - R1/R10: chose the printed `DE02120300000000202051` → the example app's extractions screen
    shows `iban` = `DE02120300000000202051` (before PP-3966: `DE89…`). That run predates the BIC decision; `bic` was still `COBADEFFXXX` then
    and is removed since (R3, unit-tested; not re-checked on device).
  - R11: back button on the sheet returns to the warning; the SDK stays open.
  - Not done on device: R6 with rotation or "Don't keep activities" while the folder picker is open
    (needs system settings changed on the phone), R12 Fragment entry and tablet, R13, R14 (TalkBack,
    200% font, external keyboard). R6/R7 are covered by presenter unit tests.

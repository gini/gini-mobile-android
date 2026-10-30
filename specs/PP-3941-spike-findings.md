# PP-3941 spike findings: IBAN selection sheet on top of the IBAN mismatch screen

Ticket: https://ginis.atlassian.net/browse/PP-3941
Branch: `PP-3941-Show-the-IBAN-selection-sheet-on-top-of-the-IBAN-mismatch-screen` (throwaway, never merged)
Status: **done for the spike** — manual run on 2026-10-30 in all four configurations, no failures
reported. The matrix below records which rows that run covered and which are deferred to the
implementation ticket, where the full matrix is re-run row by row.

## What was built

- The mismatch modal is a Material 3 `ModalBottomSheet` (material3 1.4.0 via Compose BOM
  2026.02.00, still `@ExperimentalMaterial3Api`, file-level opt-in) that cannot be dismissed,
  hosted in a `ComposeView` (`gc_iban_mismatch_container`) inside the Analysis Fragment's view.
  Same presentation on phones and tablets: full height below the status bar, full width
  (Material 3's 640 dp `sheetMaxWidth` lifted with `Dp.Unspecified`), default rounded top corners.
  The status bar strip shows the scrim. There is no tablet card variant and no
  `R.bool.gc_is_tablet` switch, unlike `WarningBottomSheet`.
- Non-dismissal is layered: `confirmValueChange` refuses `SheetValue.Hidden` (safety net for
  every path), `ModalBottomSheetProperties(shouldDismissOnBackPress = false,
  shouldDismissOnClickOutside = false)` turns back press, back gesture and scrim taps into no-ops
  **before** `hide()` is ever called (back is the one path where material3 calls `hide()`
  unguarded, so disabling it matters), `sheetGesturesEnabled = false` and `dragHandle = null`
  stop the sheet from moving with the finger or advertising a gesture it rejects. Disabling the
  scrim click also removes the scrim's "Close sheet" TalkBack action, which would otherwise be a
  dead control.
- Full height below the status bar: `statusBarsPadding()` on the sheet's own `modifier` (material3
  applies the caller modifier at the head of the sheet surface's chain, before it measures the drag
  anchors, so the surface itself is inset) plus `Box(Modifier.fillMaxHeight())` around the content
  slot. The status-bar inset is consumed there, so material3's own content insets pad only the
  navigation bar — no double padding.
- The IBAN selection sheet is a second `ModalBottomSheet` composed **inside the mismatch sheet's
  content**. It opens its own window on top of the mismatch sheet's window; its scrim covers the
  mismatch sheet's CTAs while it is up.
- Both layers are driven by one `rememberSaveable` `IbanMismatchUiState` (`@Parcelize`), so the
  Fragment view's `SavedStateRegistry` carries modal + sheet + selected IBAN across rotation and
  Activity recreation. After a rotation the Analysis presenter re-delivers the in-process cached
  analysis result and calls `showIbanMismatch` again, which re-composes the modal against the
  restored state. `SheetState.Saver` restores both sheets as `Expanded`, so they reappear without
  the show animation. See question 6 for why a real process kill cannot reach that point.
- The sheet has its own "Continue" CTA, because the selection sheet's scrim covers the modal's
  buttons while the sheet is up; that is the only way to tap Continue "with the sheet open".
- "Continue" and "Cancel transfer" first drop the composition (`setContent {}` + `GONE`), which
  disposes both sheet windows in the same frame (`DisposableEffect.onDispose` → `dialog.dismiss()`
  for each; no `hide()` involved, so refusing `Hidden` does not block teardown), then run the
  presenter's action: the same `handleSaveInvoicesLocally → clearSavedImagesAndProceed →
  proceedWithExtractions` path as the already-paid warning's "Proceed anyway", or the same
  image-store clear + `CancelListener.onCancelFlow()` as `AnalysisFragment.cancelTransaction`.
- Accessibility: both the mismatch sheet and (via the same mechanism) the selection sheet announce
  their title as the accessibility pane title (`Modifier.semantics { paneTitle }` on the sheet
  modifier, which overrides material3's generic "Bottom Sheet"), the mismatch title carries
  `heading()` semantics. Everything else is spike-grade — see Limitations.
- Trigger: example app → Configuration → "Debug / development" → "IBAN mismatch spike", **on by
  default on the branch** (`ExampleAppBankConfiguration.isIbanMismatchSpikeEnabled = true`,
  `IbanMismatchSpike.enabled = true`). The presenter checks the flag first in the
  `SUCCESS_WITH_EXTRACTIONS` branch, ahead of the already-paid / credit-note / due-date checks.
  The selected IBAN is logged with tag `IbanMismatchSpike` and not written into the extractions.

## Matrix legend

Columns: **A-P** = Activity integration (`GiniBank.startCaptureFlow`, example app "Photopayment")
on a phone; **A-T** = same on a tablet (sw600dp); **F-P** / **F-T** = Fragment integration
(`GiniBank.createCaptureFlowFragment`, example app "Single Activity") on phone / tablet.
Cells: ✅ covered by the 2026-10-30 manual run with no failure reported (the run was reported as a
whole, not ticked row by row — flip any cell that was not actually exercised); ❌ failed (describe
below the table); ⏭ deferred to the implementation ticket.

## 1. Does a native modal-on-modal construction work in both integration modes?

| Case | A-P | A-T | F-P | F-T |
|---|---|---|---|---|
| M1 Modal appears over Analysis after analysis (R1) | ✅ | ✅ | ✅ | ✅ |
| M2 Toggle off: no modal, existing warnings unchanged (R2) | ⏭ | ⏭ | ⏭ | ⏭ |
| M3 Modal shows both IBANs + three CTAs (R3) | ✅ | ✅ | ✅ | ✅ |
| M4 "Choose IBAN" opens the sheet on top, modal dimmed behind, QR IBAN preselected (R4) | ✅ | ✅ | ✅ | ✅ |
| M5 Tapping the printed IBAN row closes the sheet and updates the modal (R5) | ✅ | ✅ | ✅ | ✅ |

Observations:

- Window stacking confirmed against the material3 1.4.0 source as well: the inner
  `ModalBottomSheet` is composed in the outer dialog's sub-composition, which only exists once the
  outer window is attached, so the inner window is always created above it.
- Tablet: the first tablet build showed the sheet capped at material3's default 640 dp width
  ("partially" full screen); lifting `sheetMaxWidth` fixed it. Full width on tablets is a
  deliberate deviation from Material guidance — see Limitations.

## 2. Back button with the sheet open

| Case | A-P | A-T | F-P | F-T |
|---|---|---|---|---|
| M8a Only the sheet closes; modal, Analysis and host remain (R8) | ✅ | ✅ | ✅ | ✅ |

Observations:

- The selection sheet keeps material3's default back handling (dismissable); the mismatch sheet's
  window consumes back through its own `OnBackPressedCallback` and never forwards it, so nothing
  reaches the Analysis screen or the host's back stack in either integration mode.

## 3. Back gesture (predictive back) with the sheet open

| Case | A-P | A-T | F-P | F-T |
|---|---|---|---|---|
| M8b Only the sheet closes; modal, Analysis and host remain (R8) | ✅ | ✅ | ✅ | ✅ |

Observations:

- material3 registers its predictive-back callback (API 34+) only when
  `shouldDismissOnBackPress` is true, so the mismatch sheet shows no predictive-back preview.

## 4. Swipe-down with the sheet open

| Case | A-P | A-T | F-P | F-T |
|---|---|---|---|---|
| M8c Only the sheet closes; modal, Analysis and host remain (R8) | ✅ | ✅ | ✅ | ✅ |
| M9 Sheet closed: back button, back gesture, scrim tap do not dismiss the modal (R9) | ✅ | ✅ | ✅ | ✅ |

Observations:

- With `sheetGesturesEnabled = false` the mismatch sheet does not rubber-band on drag; an earlier
  build with only `dragHandle = null` still moved with the finger and dropped the scrim mid-drag.

## 5. Rotation with the sheet open

| Case | A-P | A-T | F-P | F-T |
|---|---|---|---|---|
| M10 Modal + sheet + selected IBAN restored after rotation (R10) | ✅ | ✅ | ✅ | ✅ |

Observations:

- Both sheets come back already `Expanded` (no re-animation), with the chosen IBAN preserved.

## 6. Process death with the sheet open

Known before running: M11b (`am kill`) is decided at the SDK level, not by the construction. After
a real process kill the example app's in-memory configuration is gone, and `GiniCapture` has no
instance until the host re-configures it: the Analysis presenter shows the general error before the
spike hook runs. The construction's own contribution (the saved `IbanMismatchUiState` in the
Fragment view's saved state) can therefore only be observed with "Don't keep activities" (M11a),
which recreates the Activity but keeps the process. For the real feature the host must re-configure
the SDK before the restored Activity resumes — a general SDK limitation, independent of this ticket.

| Case | A-P | A-T | F-P | F-T |
|---|---|---|---|---|
| M11a "Don't keep activities" (process survives): modal + sheet + selection restored (R11) | ⏭ | ⏭ | ⏭ | ⏭ |
| M11b `adb shell am kill` from Recents: see limitation above — not answerable by the construction | n/a | n/a | n/a | n/a |
| M12 Host finishes the Activity / pops the Fragment while the modal is shown: no `WindowLeaked`, no crash on next start (R12) | ⏭ | ⏭ | ⏭ | ⏭ |
| M13 Toggle on, document without extractions: no-results path, no modal (R13) | ⏭ | ⏭ | ⏭ | ⏭ |

Observations:

- M11a is expected to pass by construction (same `rememberSaveable` path as rotation) but was not
  explicitly exercised in the spike run.
- M12: `ComposeView` uses the default `ViewCompositionStrategy` (dispose on detach), which runs
  during Fragment view destruction, before the Activity's window cleanup, so no `WindowLeaked` is
  expected; `DisposeOnViewTreeLifecycleDestroyed` would make that explicit in the implementation.

## 7. How does "Continue" close both layers and move on?

| Case | A-P | A-T | F-P | F-T |
|---|---|---|---|---|
| M6 Continue with the sheet open: both layers gone in one step, next screen / result reached, IBAN logged (R6) | ✅ | ✅ | ✅ | ✅ |
| M6 Continue with the sheet closed: same (R6) | ✅ | ✅ | ✅ | ✅ |
| M7 Cancel transfer: flow ends with `CaptureResult.Cancel` (R7) | ✅ | ✅ | ✅ | ✅ |

Mechanism: the host removes the composition that owns both windows, so both sheet windows are
disposed in the same frame; the presenter action runs afterwards.

Observations:

- No intermediate frame with only one layer visible was observed.

## Recommended approach

**Pure Compose, no fallback needed.** A non-dismissable Material 3 `ModalBottomSheet` for the
mismatch screen (full height below the status bar, full width, gestures and drag handle off, back
and scrim dismissal disabled) with the selection `ModalBottomSheet` nested in its content and a
single `rememberSaveable` UI state. The `BottomSheetDialogFragment` fallback considered in the
spec was not needed: none of M8a/b/c, M9, M10 failed in either integration mode.

Why this fits Material guidance (verified against the published pages on 2026-10-30):

- Material 3 Dialogs, "Full-screen dialog": full-screen dialogs are the only dialogs over which
  other dialogs can appear, and "components within the dialog open additional dialogs" is a listed
  reason to use one. The mismatch sheet behaves as a full-screen modal, so a sheet on top of it is
  within guidance. https://m3.material.io/components/dialogs/guidelines
- Material 3 Bottom sheets, "Modal bottom sheets": a modal sheet is the alternative to inline menus
  or simple dialogs, "especially when offering a long list of action items". For two IBANs an inline
  radio group would remove the second layer altogether — a product question for the feature.
  https://m3.material.io/components/bottom-sheets/guidelines
- Neither page forbids stacking modal sheets outright; an earlier draft of this document claimed
  that and was wrong.

## Limitations

- **Deliberate deviations from Material guidance**, chosen so tablets look exactly like phones:
  Material 2 advises against full-width bottom sheets on large screens and against opening tall
  sheets at full height ("Scaling and adaptation", "Visibility":
  https://m2.material.io/components/sheets-bottom); Material 3 keeps a default max width on medium
  and expanded breakpoints ("Responsive layout") and says full-screen dialogs are for compact
  breakpoints only. Revisit with design in the implementation.
- **Readability on tablets:** with full width, the description and the buttons span ~1200 dp on a
  10" tablet in landscape. Usual fix if full width stays: cap the content column at 480–640 dp and
  centre it inside the full-width sheet.
- **Status-bar inset relies on material3's modifier order** (caller modifier applied before the
  drag anchors are measured). True in 1.4.0; re-check visually after Compose BOM bumps, and once on
  an API 23–29 device, where dialog-window inset delivery differs from API 30+.
- **Status bar icons sit on the scrim** while the sheet is up; a host with dark status-bar icons
  loses some contrast there. `ModalBottomSheetProperties.isAppearanceLightStatusBars` can force
  light icons; the spike inherits the host's setting.
- The spike only covers the Analysis screen. If the real feature needs the mismatch modal after
  Return Assistant / Skonto (inside a Compose-hosted `bank-sdk` Fragment), the construction must be
  re-checked there — a product question for the epic (PP-3518).
- `ModalBottomSheet` is still `@ExperimentalMaterial3Api` in material3 1.4.0; the opt-in is
  file-level, as for the module's existing date picker / tooltip / switch components.
- Spike-grade layout and strings; production visuals come with PP-3749.
- **Accessibility items for the implementation** (a11y review of the spike, not fixed here): the
  selected IBAN changes silently after the sheet closes (polite live region on the selected-IBAN
  block); the selection sheet content is not scrollable, so "Continue" can clip in phone landscape
  at large font sizes; IBANs are unformatted (group in fours like `IBANRecognizerFilter` so they
  wrap and read in blocks); label/value pairs are two TalkBack stops (merge them); dark-mode
  `text.secondary` on the dialog container is ~3:1, a theme-token issue beyond this screen.
- Process-death restore depends on the host re-configuring the SDK before resume (question 6).
- The selected IBAN is logged, not written into the extractions.
- **Not to carry over into the real feature:** the unconditional `Log.d(IbanMismatchSpike.LOG_TAG, …)`
  of the IBAN in `AnalysisScreenPresenterExtension.showIbanMismatchWarning` (sample constants here,
  live extraction data there), the public `IbanMismatchSpike` toggle and its on-by-default value,
  the hardcoded sample IBANs, and the two public
  `AnalysisFragmentExtension.showIbanMismatch/hideIbanMismatch` methods (they need the same
  `internal`-vs-Java-caller treatment as `showEducation`).
- **Reusable from the spike:** `IbanMismatchModal` / `IbanMismatchSheet` (the non-dismissable sheet
  configuration), `IbanSelectionSheet`, `IbanMismatchUiState` + `IbanMismatchArgs`, and the
  compose-and-remove teardown in `AnalysisFragmentExtension`.

## Setup the banks need

None. Both layers are hosted inside the SDK's own Fragment view and open their own windows; the
host registers nothing and never sees the back events. Confirmed by the Fragment-integration runs
(F-P / F-T), where the bank's back stack sits directly under the SDK: back button, back gesture and
swipe never reached the host screen.

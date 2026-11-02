# PP-3961: [Android] Fraud warning screen: IBAN mismatch — phase 1, UI components

Status: phase 1 implemented (UI only); development switch to be removed before merge, see "Before merge"
Ticket: https://ginis.atlassian.net/browse/PP-3961
Parent epic: PP-3518 "[Q4] Fraud prevention warnings – phase 1"
Design: Figma "Gini Photo Payment (iOS & Android)", section "Android", node `38468-42451`
Spike: PP-3941 (branch `PP-3941-Show-the-IBAN-selection-sheet-on-top-of-the-IBAN-mismatch-screen`,
throwaway) — proved that a non-dismissable Material 3 `ModalBottomSheet` hosted in a `ComposeView`
inside the Analysis Fragment works in both integration modes, on phones and tablets, and that a
second `ModalBottomSheet` composed inside it stacks on top.
Branch: `PP-3961-Fraud-warning-screen-IBAN-mismatch`

## Scope of this phase

In:

- Reusable Jetpack Compose components in `capture-sdk:sdk`, package
  `net.gini.android.capture.ui.components`, each public with KDoc, a `*Colors` data class with a
  `@Composable colors()` factory, and light/dark + font-scale previews.
- One screen, `analysis/fraud/IbanMismatchWarningScreen`, composed only from those components.
  Its content is `IbanMismatchArgs`; the same screen renders the QR-vs-printed, country-only and
  combined cases.
- The Analysis-screen hooks taken over from the spike: `IbanMismatchDebug` (restricted-visibility
  development switch, **off by default**), the presenter check ahead of the existing warning
  checks, `showIbanMismatch`/`hideIbanMismatch` in `AnalysisFragmentExtension`, the
  `gc_iban_mismatch_container` `ComposeView` in the four `gc_fragment_analysis` layouts, and a
  switch in the bank-sdk example app's "Debug / development" configuration section.
- "Cancel transfer" ends the flow like the warning sheets' cancel CTA; "Proceed anyway" hands the
  extractions on unchanged (same path as the already-paid warning's "Proceed anyway").
- DE and EN strings in capture-sdk resources (`gc_fraud_*`, `gc_a11y_expanded/collapsed`), so banks
  can override them.

Out (later phases):

- Reading `QR_PRINTED_IBAN_MISMATCH` / `IBAN_COUNTRY_MISMATCH` from the analysis result
  (PP-3965) and building `IbanMismatchArgs` from it — the hints ("From Poland, not the sender's
  country") and the sender line are display strings resolved by that phase.
- The IBAN selection sheet as a screen (the spike's `IbanSelectionSheet`); `GiniRadioButton` is
  already here for it.
- Writing a chosen IBAN anywhere.

## Design → component mapping

| Design element | Component | Notes |
|---|---|---|
| Scrim + 28 dp-top-cornered sheet below the status bar, full width on all devices, no grabber, no dismissal | `sheet/GiniBottomSheetScaffold` | Wraps `ModalBottomSheet` with the spike's non-dismissal configuration (`confirmValueChange` refuses `Hidden`, back/scrim disabled, gestures off, `sheetMaxWidth = Dp.Unspecified`). `dismissable = true` turns it into a regular sheet with a drag handle for the selection sheet. Content column capped with `tabletMaxWidth()`; footer pinned to the bottom while content fits, scrolls with it otherwise (`heightIn(min = maxHeight)` + weighted spacer in a scrolling column). |
| Red alert icon on an error-tinted rounded square, centred headline + description | `header/GiniHeaderBlock` | Title has `heading()` semantics; icon decorative. |
| "How do I protect myself?" row with shield, chevron, revealed body | `expandable/GiniExpandableInfoRow` | State hoisted (`expanded`/`onExpandedChange`), `Role.Button`, `stateDescription` expanded/collapsed, chevron rotates. |
| "Sender" on a grey card (combined case) | `card/GiniInfoCard` | Placeholder background, 8 dp corners. |
| Label-over-value rows with optional red hint | `labeledvalue/GiniLabeledValue` | Merged semantics (one TalkBack stop), optional leading slot for a radio. |
| Rows separated by hairlines, optional "The following IBANs were detected" title | `labeledvalue/GiniLabeledValueList` | Data-driven via `GiniLabeledValueItem`. |
| Radio with error ring (selection sheet, later) | `radio/GiniRadioButton` | Wraps Material 3 `RadioButton` like `GiniCheckbox` wraps `Checkbox`; `isError`. |
| Filled "Cancel transfer" + outlined "Proceed anyway", stacked in portrait, side by side in landscape | `footer/GiniActionFooter` | Reuses `GiniButton`/`GiniOutlinedButton`; single-button variant. |

Per-case layout (from the Figma frames 1.2.1 / 1.3.1 / 1.2.1-Mix):

- **QR vs printed**: header, protect row, titled list [QR IBAN, printed IBAN].
- **Country only**: header, protect row, untitled list [Sender, "On the document" IBAN + hint].
- **Combined**: header, protect row, sender card, titled list [QR IBAN + hint, printed IBAN (+ hint)].

IBANs are grouped in blocks of four for display (`internal/iban/IbanDisplayFormat.kt`) so they wrap
at block boundaries and screen readers read them in chunks.

## Theme additions

- `GiniColorScheme.errorHint` (`container = error04`, `content = error02`) — the design's
  error-tinted icon square and red hint text; `card.contentError` is the dark error red and does
  not match. Adding a defaulted constructor parameter to the public `GiniColorScheme` data class
  is source-compatible but changes `copy`/constructor signatures in `api/sdk.api`.
- Drawables from the Figma SVGs: `gc_fraud_alert_icon`, `gc_shield_icon`, `gc_chevron_right_icon`.

## Accessibility (spike review + a11y-specialist review of this phase)

- Pane title on the sheet, heading on the title and on the list title, merged label/value/hint
  rows, IBANs grouped in fours for display and spelled character by character for TalkBack
  (`formatIbanForSpeech` → `valueContentDescription`, set on the merged node so label and hint
  are still read), expandable row with button role, expanded/collapsed state and
  expand/collapse click label, scrollable content so the footer never clips at 200 %.
- Material 3's scrim adds its "Close sheet" semantics only when `shouldDismissOnClickOutside`
  is true (verified in material3 1.4.0 `ModalBottomSheet.kt`), so the non-dismissable sheet
  exposes no dead control.
- Dark-mode hint red: the `error02` primitive is resolved from resources and `values-night`
  overrides `gc_error_02` to `#FF3B30`, about 5:1 on `dialogs.container` (`#161616`); the
  `#DD0000` default in `GiniColorPrimitives` is only the fallback. `errorHint.container` keeps
  `error04` in both modes like `card.containerError`; whether dark mode wants a darker container
  is a design question.
- Still open (theme-level, not this screen): dark-mode `text.secondary` (`#626262`) on
  `dialogs.container` is roughly 3:1. Track as a token ticket.

## Review decisions (architecture / Compose specialists)

- `IbanMismatchDebug` is public only because the example-app switch has to reach into
  capture-sdk's presenter and Kotlin `internal` stops at the module boundary; `@RestrictTo` does
  not hide it from `apiCheck` (no `nonPublicMarkers`). It is branch scaffolding, not a feature:
  see "Before merge" below.
- `IbanMismatchArgs` is a plain `internal` data class, not `@Parcelize`: nothing persists it, and
  Parcelize would publish a `$Creator` class.
- `hideIbanMismatch` is `internal` with `@JvmName`, like `showIbanMismatch`.
- `GiniHeaderBlock` defaults are neutral (placeholder background, accent tint); the warning passes
  the error tint. `GiniExpandableInfoRow` requires its icon.
- `GiniRadioButton` and the `leadingContent` slot of `GiniLabeledValueList` have no consumer in
  this phase; they exist for the IBAN selection sheet (next phase) and were agreed up front.
- `GiniColorScheme.errorHint`: `logo` and `contextMenu` were appended the same way and shipped
  in minor releases (`capture-sdk;3.14.0`), so this follows precedent; call it out in the PR.
- `GiniActionFooter` splits on orientation, not width class, because the design stacks the
  buttons on tablets in portrait and puts them side by side on phones in landscape.
- Rotation with the sheet open: the composition dies with the Fragment view and the presenter
  re-shows the sheet from the cached analysis result (as in the spike); `rememberSaveable`
  restores the expanded row. Verify on device.

## Before merge: remove the development switch

The switch exists only so the sheet can be opened on a device before the fraud signals are read
from the analysis result. It must not ship: it would put a debug toggle and a fake Polish IBAN
into a public class on Maven Central. Remove it in one final commit on this branch, after the
manual device checks below are done and before the PR is opened:

- [ ] `capture-sdk/sdk/src/main/java/net/gini/android/capture/analysis/fraud/IbanMismatchDebug.kt`
      (the object and `sampleArgs()`; move the preview data into the previews in
      `IbanMismatchWarningScreen.kt`)
- [ ] the `else if (IbanMismatchDebug.getEnabled())` branch in `AnalysisScreenPresenter.java` and
      `showIbanMismatchWarning` in `AnalysisScreenPresenterExtension.kt`
- [ ] the "IBAN mismatch warning" region of `AnalysisScreenPresenterTest.kt` (three tests; phase 2
      brings them back driven by extractions)
- [ ] bank-sdk example app: `isIbanMismatchDebugEnabled` in `ExampleAppBankConfiguration`,
      `switchIbanMismatchDebug` in `ConfigurationActivity` and
      `layout_debug_development_toggles.xml`, `applyIbanMismatchDebugSwitch` in
      `ConfigurationViewModel`, the `iban_mismatch_debug_switch_label` string
- [ ] `./gradlew capture-sdk:sdk:apiDump` so `IbanMismatchDebug` leaves `sdk.api`
- [ ] this section and the "Manual" step under Verification

What stays: the `ComposeView` container in the four `gc_fragment_analysis` layouts,
`showIbanMismatch`/`hideIbanMismatch` in `AnalysisFragmentExtension.kt` and the
`AnalysisScreenContract` method, the components, the screen and the strings. Phase 2 (PP-3965)
calls `showIbanMismatch` from the extraction check that replaces the removed presenter branch.

To make CI enforce the removal once the device checks are done, add a `// STOPSHIP:` comment to
the object; Detekt's `ForbiddenComment` rule fails `capture-sdk:sdk:detekt` until the file is gone.

## Verification

- `./gradlew capture-sdk:sdk:testDebugUnitTest` — `AnalysisScreenPresenterTest` (switch on → warning
  ahead of other warnings; proceed → extractions unchanged; switch off → no warning),
  `IbanDisplayFormatTest`.
- `/gini-check` on `capture-sdk:sdk` and `bank-sdk:example-app`; `apiDump` for `capture-sdk:sdk`.
- Manual: example app → Configuration → Debug / development → "IBAN mismatch warning
  (development)" → scan any invoice → the combined warning appears over the Analysis screen;
  back/scrim/swipe do nothing; rotation keeps the sheet and the expanded protect row; "Cancel
  transfer" ends the flow; "Proceed anyway" continues to the extractions.

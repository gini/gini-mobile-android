# PP-3961: [Android] Fraud warning screen: IBAN mismatch — screens without business logic

Status: warning screen, components, the case selector (step 1), the IBAN selection sheet
(step 2) and its stacking over the warning (step 3) implemented; device verification (step 4)
and tests (step 5) pending — see "Build plan". Development switch to be removed before merge,
see "Before merge".
Ticket: https://ginis.atlassian.net/browse/PP-3961
Parent epic: PP-3518 "[Q4] Fraud prevention warnings – phase 1"
Design: Figma "Gini Photo Payment (iOS & Android)", section "Android", node `38468-42451`
Spike: PP-3941 (branch `PP-3941-Show-the-IBAN-selection-sheet-on-top-of-the-IBAN-mismatch-screen`,
throwaway) — proved that a non-dismissable Material 3 `ModalBottomSheet` hosted in a `ComposeView`
inside the Analysis Fragment works in both integration modes, on phones and tablets, and that a
second `ModalBottomSheet` composed inside it stacks on top.
Branch: `PP-3961-Fraud-warning-screen-IBAN-mismatch`
Related tickets: PP-3965 (read fraud signals from the extractions, In Review), PP-3966 (IBAN
selection sheet, To Do — its screen is built here, see "Scope"), PP-3968 (public docs),
PP-3970 (UI automation).

## Scope of this branch

Every screen of the design, built without business logic. The trigger stays the development
switch; the Analysis presenter's existing warning logic is untouched and is rewired by PP-3965.

In:

- Reusable Jetpack Compose components in `capture-sdk:sdk`, package
  `net.gini.android.capture.ui.components`, each public with KDoc, a `*Colors` data class with a
  `@Composable colors()` factory, and light/dark + font-scale previews.
- The warning screen `analysis/fraud/IBANMismatchWarningScreen`, composed only from those
  components. Its content is `IBANMismatchArgs`; the same screen renders the QR-vs-printed,
  country-only and combined cases.
- The IBAN selection sheet `analysis/fraud/IBANSelectionSheet` (design frames 1.2.2–1.2.4 and
  the `Mix-Sheet-*` frames), stacked over the warning when "Proceed anyway" is tapped in the
  QR-vs-printed and combined cases. Its three states: initial, error (no choice when "Next" is
  tapped), selected. The chosen IBAN is handed to the caller and not used yet.
- The Analysis-screen hooks taken over from the spike: `IBANMismatchDebug` (restricted-visibility
  development switch, **off by default**, extended with a case selector), the presenter check
  ahead of the existing warning checks, `showIbanMismatch`/`hideIbanMismatch` in
  `AnalysisFragmentExtension`, the `gc_iban_mismatch_container` `ComposeView` in the four
  `gc_fragment_analysis` layouts, and the "Debug / development" controls in the bank-sdk
  example app.
- "Cancel transfer" ends the flow like the warning sheets' cancel CTA; "Proceed anyway" (country
  case) and the selection sheet's "Next" (other cases) hand the extractions on unchanged (same
  path as the already-paid warning's "Proceed anyway").
- DE and EN strings in capture-sdk resources (`gc_fraud_*`, `gc_a11y_expanded/collapsed`), so banks
  can override them.

Out (later tickets):

- Reading `QR_PRINTED_IBAN_MISMATCH` / `IBAN_COUNTRY_MISMATCH` from the analysis result
  (PP-3965) and building `IBANMismatchArgs` from it — the hints ("From Poland, not the sender's
  country") and the sender line are display strings resolved by that ticket.
- Writing the chosen IBAN into the extractions (PP-3966 keeps the behaviour; the screen is here).
- Public documentation (PP-3968) and UI automation (PP-3970).

## Build plan

Each step is one or more commits, reviewed (compose, a11y, architecture, testing specialists)
before the next one starts. The user starts each step explicitly.

1. **Case selector on the development switch.** Replace the boolean example-app switch with a
   choice "Off / QR vs printed / Country / Combined" (`IBANMismatchDebug.case`, a public enum
   inside the object, `null` = off). `sampleArgs(case)` returns the matching `IBANMismatchArgs`
   with the design's sample data. Presenter branch unchanged apart from reading `case != null`.
   Lets every screen be opened on a device.
2. **IBAN selection sheet screen.** `analysis/fraud/IBANSelectionSheet.kt` built from
   `GiniBottomSheetScaffold(dismissable = true)`, `GiniLabeledValue` rows with a
   `GiniRadioButton` in `leadingContent` inside a `selectableGroup`, the red error line, the
   footnote and a single-button `GiniActionFooter`. Content: `IBANSelectionArgs` (QR IBAN,
   printed IBAN, optional hints — derived from `IBANMismatchArgs`). Owns `selectedIban` and
   `showError` in `rememberSaveable`. Strings DE/EN: `gc_fraud_select_iban_title`,
   `gc_fraud_select_iban_error`, `gc_fraud_select_iban_footnote`; "Next" reuses `gc_next`.
   Previews: initial/error/selected × QR-vs-printed/combined, light/dark, font scale.
   a11y review before it is committed.
3. **Stack the sheet over the warning.** In `IBANMismatchWarningScreen`, "Proceed anyway"
   opens the sheet for `QR_VS_PRINTED` and `COMBINED` and calls `onProceed(null)` directly for
   `COUNTRY`. Sheet dismissal (back, scrim, swipe) returns to the warning. "Next" without a
   choice sets the error state; with a choice it hides the sheet and calls
   `onProceed(selectedIban)`. `onProceed` becomes `(String?) -> Unit` through
   `AnalysisFragmentExtension.showIbanMismatch`; the presenter ignores the value for now (PP-3966
   consumes it). `sheetOpen` in `rememberSaveable` so rotation restores the stacked sheet.
4. **Device verification.** Example app, each case of the selector: all three warning layouts,
   the three sheet states, light/dark, portrait/landscape, phone/tablet, 200 % font size
   (the design's `200%` and `1.4.1 Scroll` frames), rotation with the warning and with the
   sheet open, TalkBack (pane titles, headings, radio group with "1 of 2", the error announced
   after "Next" with no choice and read again on the rows), external keyboard (Tab order rows →
   Next, focus stays inside the open sheet, focus visible). Fix what the pass finds.
5. **Tests and checks.** `AnalysisScreenPresenterTest` updated for the case selector. Compose
   tests under Robolectric for the selection sheet's state transitions (initial → error on
   "Next" → selected clears the error → `onProceed` with the chosen IBAN), asserting the
   semantics too (`Selected`, `Role.RadioButton`, `Error` present on both rows after "Next" with
   no choice and absent after a selection, row height at least 48 dp), modelled on bank-sdk's
   `SkontoScreenContentTest`. capture-sdk has Robolectric but not the Compose test rule yet:
   add `libs.compose.ui.test.junit4` and `libs.compose.ui.test.manifest` as `testImplementation`
   to `capture-sdk/sdk/build.gradle.kts`, exactly as `bank-sdk/sdk/build.gradle.kts` does. Then `/gini-check` for `capture-sdk:sdk` and `bank-sdk:example-app`,
   `apiDump`, update this spec.
6. **Pre-PR housekeeping.** Merge `origin/main` (the branch is behind by the Wero and camera
   merges), then the "Before merge" removal below — or, if the user decides the switch stays
   until PP-3965 replaces it, record that decision here and in the PR description.

## Design → component mapping

| Design element | Component | Notes |
|---|---|---|
| Scrim + 28 dp-top-cornered sheet below the status bar, full width on all devices, no grabber, no dismissal | `sheet/GiniBottomSheetScaffold` | Wraps `ModalBottomSheet` with the spike's non-dismissal configuration (`confirmValueChange` refuses `Hidden`, back/scrim disabled, gestures off, no drag handle, `sheetMaxWidth = Dp.Unspecified`). Content column capped with `tabletMaxWidth()`; footer pinned to the bottom while content fits, scrolls with it otherwise (`heightIn(min = maxHeight)` + weighted spacer in a scrolling column). Only for full-height screens the user must answer. |
| Content-height dismissable sheet with grabber (selection sheet) | Material `ModalBottomSheet` directly, inside `IBANSelectionSheet` | Gini container and grabber colors, `tabletMaxWidth()`, the same 24 dp gutters and pane title. Not the scaffold: its footer-pinning logic and non-dismissal are the opposite of what this sheet needs, and flags for both shapes made the scaffold a config bag. |
| Red alert icon on an error-tinted rounded square, centred headline + description | `header/GiniHeaderBlock` | Title has `heading()` semantics; icon decorative. |
| "How do I protect myself?" row with shield, chevron, revealed body | `expandable/GiniExpandableInfoRow` | State hoisted (`expanded`/`onExpandedChange`), `Role.Button`, `stateDescription` expanded/collapsed, chevron rotates. |
| "Sender" on a grey card (combined case) | `card/GiniInfoCard` | Placeholder background, 8 dp corners. |
| Label-over-value rows with optional red hint | `labeledvalue/GiniLabeledValue` | Merged semantics (one TalkBack stop), optional leading slot for a radio. |
| Rows separated by hairlines, optional "The following IBANs were detected" title | private `IBANMismatchRows` in the warning screen | Data-driven via `GiniLabeledValueItem`; the former public `GiniLabeledValueList` was inlined. |
| Radio with error ring (selection sheet) | `radio/GiniRadioButton` | Wraps Material 3 `RadioButton` like `GiniCheckbox` wraps `Checkbox`; `isError`. |
| Filled "Cancel transfer" + outlined "Proceed anyway", stacked in portrait, side by side in landscape; single "Next" on the selection sheet | `footer/GiniActionFooter` | Reuses `GiniButton`/`GiniOutlinedButton`; single-button variant. |

Per-case layout (from the Figma frames 1.2.1 / 1.3.1 / 1.2.1-Mix):

- **QR vs printed**: header, protect row, titled list [QR IBAN, printed IBAN].
- **Country only**: header, protect row, untitled list [Sender, "On the document" IBAN + hint].
- **Combined**: header, protect row, sender card, titled list [QR IBAN + hint, printed IBAN (+ hint)].

Selection sheet (frames 1.2.2 / 1.2.3 / 1.2.4 and `Mix-Sheet-*`): drag handle, title "Choose an
IBAN to continue" (heading), radio rows [QR IBAN (+ hint), printed IBAN (+ hint)] separated by a
hairline, red error line "Choose an IBAN to continue" under the rows in the error state, centred
grey footnote "You can still check the IBAN in the next step.", filled "Next". In the error state
both radios draw the error ring. Opens over the warning; the warning stays visible behind the
scrim.

IBANs are grouped in blocks of four for display (`internal/iban/IBANDisplayFormat.kt`) so they wrap
at block boundaries and screen readers read them in chunks.

## Theme additions

- `GiniColorScheme.errorHint` (`container = error04`, `content = error02`) — the design's
  error-tinted icon square and red hint text; `card.contentError` is the dark error red and does
  not match. Adding a defaulted constructor parameter to the public `GiniColorScheme` data class
  is source-compatible but changes `copy`/constructor signatures in `api/sdk.api`.
- Drawables from the Figma SVGs: `gc_fraud_alert_icon`, `gc_shield_icon`, `gc_chevron_right_icon`.

## Accessibility (spike review + a11y-specialist review of the warning screen)

- Pane title on the sheet, heading on the title and on the list title, merged label/value/hint
  rows, IBANs grouped in fours for display and spelled character by character for TalkBack
  (`formatIbanForSpeech` → `valueContentDescription`, set on the merged node so label and hint
  are still read), expandable row with button role, expanded/collapsed state and
  expand/collapse click label, scrollable content so the footer never clips at 200 %.
- Material 3's scrim adds its "Close sheet" semantics only when `shouldDismissOnClickOutside`
  is true (verified in material3 1.4.0 `ModalBottomSheet.kt`), so the non-dismissable sheet
  exposes no dead control.
- Selection sheet (a11y-specialist review of step 2): `selectableGroup` on the rows,
  `Role.RadioButton` on each row with the radio itself non-clickable (`onClick = null`), so
  TalkBack reads one stop per row with "1 of 2"; `error(message)` semantics on each row while
  the error is shown, so the invalid state is not conveyed by the red ring alone; the error is
  announced with `announceForAccessibility` from a `LaunchedEffect` on every "Next" without a
  choice (a `liveRegion` on a node that enters the composition is not announced by Compose; the
  education messages use the same precedent) and not re-announced after rotation; pane title on
  the sheet; the dismissable sheet keeps Material's "Close sheet" scrim action and the drag
  handle's "Dismiss" action. Error text and ring use `errorHint.content`, about 5:1 in both
  modes.
- Keyboard focus on custom `selectable`/`clickable` rows is indicated only by Material's 10 %
  focused state layer, below WCAG 2.4.11's 3:1. Theme-wide, not this sheet: judge in the step-4
  keyboard pass and, if confirmed, add a reusable focus-outline modifier under
  `ui/theme/modifier` for all custom targets in a follow-up.
- Dark-mode hint red: the `error02` primitive is resolved from resources and `values-night`
  overrides `gc_error_02` to `#FF3B30`, about 5:1 on `dialogs.container` (`#161616`); the
  `#DD0000` default in `GiniColorPrimitives` is only the fallback. `errorHint.container` keeps
  `error04` in both modes like `card.containerError`; whether dark mode wants a darker container
  is a design question.
- Dark-mode `text.secondary`: `values-night` overrides `gc_dark_05` to `#CCCCCC`, about 10:1 on
  `dialogs.container`, so the earlier 3:1 concern looks resolved; re-verify on device in step 4.

## Review decisions (architecture / Compose specialists)

- `IBANMismatchDebug` is public only because the example-app switch has to reach into
  capture-sdk's presenter and Kotlin `internal` stops at the module boundary; `@RestrictTo` does
  not hide it from `apiCheck` (no `nonPublicMarkers`). It is branch scaffolding, not a feature:
  see "Before merge" below.
- `IBANMismatchArgs` is a plain `internal` data class, not `@Parcelize`: nothing persists it, and
  Parcelize would publish a `$Creator` class. The same applies to `IBANSelectionArgs`.
- `hideIbanMismatch` is `internal` with `@JvmName`, like `showIbanMismatch`.
- `GiniHeaderBlock` defaults are neutral (placeholder background, accent tint); the warning passes
  the error tint. `GiniExpandableInfoRow` requires its icon.
- `GiniRadioButton` and the `leadingContent` slot of `GiniLabeledValue` were added ahead of
  their consumer, the selection sheet (step 2), which now uses both.
- `GiniBottomSheetScaffold` lost its `dismissable`/`showDragHandle` flags and
  `GiniBottomSheetScaffoldColors.dragHandleColor`: the selection sheet, their only intended
  consumer, uses `ModalBottomSheet` directly (see the design mapping), so the scaffold is the
  full-height non-dismissable sheet and nothing else. Safe because it has not shipped yet.
- The selection sheet derives its rows once per `args` outside the scaffold's content lambda,
  so a tap only recomposes the rows, not the string lookups and IBAN formatting.
- `GiniColorScheme.errorHint`: `logo` and `contextMenu` were appended the same way and shipped
  in minor releases (`capture-sdk;3.14.0`), so this follows precedent; call it out in the PR.
- `GiniActionFooter` splits on orientation, not width class, because the design stacks the
  buttons on tablets in portrait and puts them side by side on phones in landscape.
- Rotation with the sheet open: the composition dies with the Fragment view and the presenter
  re-shows the sheet from the cached analysis result (as in the spike); `rememberSaveable`
  restores the expanded row, and from step 3 the open selection sheet and its choice. Verify on
  device (step 4).
- The selection sheet is composed inside the warning screen (second `ModalBottomSheet` in the
  same composition), not through a second `ComposeView`: the spike proved the stacking and it
  keeps `AnalysisFragmentExtension` at one show/hide pair. The warning decides with
  `IBANMismatchArgs.toSelectionArgs()`: `null` for the country case (or a QR case without a QR
  IBAN) proceeds directly, otherwise the sheet opens.
- The chosen IBAN travels as `androidx.core.util.Consumer<String>` through
  `AnalysisScreenContract.View.showIbanMismatch` (Java-friendly, `null` when the warning offered
  no choice) and is ignored by the presenter until PP-3966.

## Before merge: remove the development switch

The switch exists only so the screens can be opened on a device before the fraud signals are read
from the analysis result. It must not ship: it would put a debug toggle and fake IBANs into a
public class on Maven Central. Remove it in one final commit on this branch, after the device
checks (step 4) are done and before the PR is opened — unless the user decides it stays until
PP-3965 replaces it (then record that here):

- [ ] `capture-sdk/sdk/src/main/java/net/gini/android/capture/analysis/fraud/IBANMismatchDebug.kt`
      (the object, the case enum and `sampleArgs()`; move the preview data into the previews in
      `IBANMismatchWarningScreen.kt` and `IBANSelectionSheet.kt`)
- [ ] the `else if (IBANMismatchDebug.getCase() != null)` branch in `AnalysisScreenPresenter.java`
      and `showIbanMismatchWarning` in `AnalysisScreenPresenterExtension.kt`
- [ ] the "IBAN mismatch warning" region of `AnalysisScreenPresenterTest.kt` (PP-3965 brings
      the tests back driven by extractions)
- [ ] bank-sdk example app: `ibanMismatchDebugCase` in `ExampleAppBankConfiguration`, the case
      selector in `ConfigurationActivity` and `layout_debug_development_toggles.xml`,
      `applyIbanMismatchDebugCase` in `ConfigurationViewModel`, the related strings
- [ ] `./gradlew capture-sdk:sdk:apiDump` so `IBANMismatchDebug` leaves `sdk.api`
- [ ] this section and the "Manual" step under Verification

What stays: the `ComposeView` container in the four `gc_fragment_analysis` layouts,
`showIbanMismatch`/`hideIbanMismatch` in `AnalysisFragmentExtension.kt` and the
`AnalysisScreenContract` method, the components, both screens and the strings. PP-3965 calls
`showIbanMismatch` from the extraction check that replaces the removed presenter branch.

To make CI enforce the removal once the device checks are done, add a `// STOPSHIP:` comment to
the object; Detekt's `ForbiddenComment` rule fails `capture-sdk:sdk:detekt` until the file is gone.

## Verification

- `./gradlew capture-sdk:sdk:testDebugUnitTest` — `AnalysisScreenPresenterTest` (case set →
  warning ahead of other warnings; proceed → extractions unchanged; case off → no warning),
  `IBANDisplayFormatTest`, the selection sheet Compose tests (step 5).
- `/gini-check` on `capture-sdk:sdk` and `bank-sdk:example-app`; `apiDump` for `capture-sdk:sdk`.
- Manual (step 4): example app → Configuration → Debug / development → "IBAN mismatch warning
  (development)" → pick a case → scan any invoice → the warning appears over the Analysis
  screen; back/scrim/swipe do nothing; rotation keeps the sheet and the expanded protect row;
  "Cancel transfer" ends the flow; "Proceed anyway" continues to the extractions (country case)
  or opens the selection sheet (other cases); the sheet closes on back/scrim/swipe back to the
  warning; "Next" without a choice shows the error, with a choice continues to the extractions.

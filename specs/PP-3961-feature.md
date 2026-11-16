# PP-3961: [Android] Fraud warning screen: IBAN mismatch — screens without business logic

Status: warning screen, components, the case selector (step 1), the IBAN selection sheet
(step 2), its stacking over the warning (step 3) and the phone device pass (step 4, see
"Device verification") done; tablet/landscape and TalkBack still open; tests (step 5) pending. Development switch to be removed before merge,
see "Before merge".
Phase 2 (wiring, end of this file): implemented on `PP-3961-wire-fraud-signals`.
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
  QR-vs-printed and combined cases. Its three states: initial, error (no choice when "Continue" is
  tapped), selected. The chosen IBAN is handed to the caller and not used yet.
- The Analysis-screen hooks taken over from the spike: `IBANMismatchDebug` (restricted-visibility
  development switch, **off by default**, extended with a case selector), the presenter check
  ahead of the existing warning checks, `showIbanMismatch`/`hideIbanMismatch` in
  `AnalysisFragmentExtension`, the `gc_iban_mismatch_container` `ComposeView` in the four
  `gc_fragment_analysis` layouts, and the "Debug / development" controls in the bank-sdk
  example app.
- "Cancel transfer" ends the flow like the warning sheets' cancel CTA; "Proceed anyway" (country
  case) and the selection sheet's "Continue" (other cases) hand the extractions on unchanged (same
  path as the already-paid warning's "Proceed anyway").
- DE and EN strings in capture-sdk resources (`gc_fraud_*`, `gc_a11y_expanded/collapsed`), so banks
  can override them.

Out (later tickets):

- Reading `QR_PRINTED_IBAN_MISMATCH` / `IBAN_COUNTRY_MISMATCH` from the analysis result
  (PP-3965) and building `IBANMismatchArgs` from it. The sentences are resolved here from
  placeholder strings (`gc_fraud_iban_country_hint`, `gc_fraud_sender_with_country`); that
  ticket supplies only the values — sender name, and the countries as localised display names.
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
   `gc_fraud_select_iban_error`, `gc_fraud_select_iban_footnote`; "Continue" uses `gc_fraud_select_iban_continue` (`gc_continue` is "Verarbeiten" in German).
   Previews: initial/error/selected × QR-vs-printed/combined, light/dark, font scale.
   a11y review before it is committed.
3. **Stack the sheet over the warning.** In `IBANMismatchWarningScreen`, "Proceed anyway"
   opens the sheet for `QR_VS_PRINTED` and `COMBINED` and calls `onProceed(null)` directly for
   `COUNTRY`. Sheet dismissal (back, scrim, swipe) returns to the warning. "Continue" without a
   choice sets the error state; with a choice it hides the sheet and calls
   `onProceed(selectedIban)`. `onProceed` becomes `(String?) -> Unit` through
   `AnalysisFragmentExtension.showIbanMismatch`; the presenter ignores the value for now (PP-3966
   consumes it). `sheetOpen` in `rememberSaveable` so rotation restores the stacked sheet.
4. **Device verification.** Example app, each case of the selector: all three warning layouts,
   the three sheet states, light/dark, portrait/landscape, phone/tablet, 200 % font size
   (the design's `200%` and `1.4.1 Scroll` frames), rotation with the warning and with the
   sheet open, TalkBack (pane titles, headings, radio group with "1 of 2", the error announced
   after "Continue" with no choice and read again on the rows), external keyboard (Tab order rows →
   Next, focus stays inside the open sheet, focus visible). Fix what the pass finds.
5. **Tests and checks.** `AnalysisScreenPresenterTest` updated for the case selector. Compose
   tests under Robolectric for the selection sheet's state transitions (initial → error on
   "Continue" → selected clears the error → `onProceed` with the chosen IBAN), asserting the
   semantics too (`Selected`, `Role.RadioButton`, `Error` present on both rows after "Continue" with
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
| Content-height dismissable sheet with grabber on phones, centred dialog on tablets in both orientations (selection sheet) | Material `ModalBottomSheet` on phones and a Compose `Dialog` (extra-large corners, 32 dp window margin, capped at `gc_tablet_width`, scrolls when taller than the window) on tablets, chosen by `isTablet()` inside `IBANSelectionSheet`; the body is shared | Gini container and grabber colors, `tabletMaxWidth()`, the same 24 dp gutters and pane title. Not the scaffold: its footer-pinning logic and non-dismissal are the opposite of what this sheet needs, and flags for both shapes made the scaffold a config bag. |
| Red alert icon on an error-tinted rounded square, centred headline + description | `header/GiniHeaderBlock` | Title has `heading()` semantics; icon decorative. |
| "How do I protect myself?" row with shield, chevron, revealed body | `expandable/GiniExpandableInfoRow` | State hoisted (`expanded`/`onExpandedChange`), `Role.Button`, `stateDescription` expanded/collapsed, chevron rotates. |
| "Sender" on a grey card (combined case) | `card/GiniInfoCard` | Placeholder background, 8 dp corners. |
| Label-over-value rows with optional red hint | `labeledvalue/GiniLabeledValue` | Merged semantics (one TalkBack stop), optional leading slot for a radio. |
| Rows separated by hairlines, optional "The following IBANs were detected" title | private `IBANMismatchRows` in the warning screen | Data-driven via `GiniLabeledValueItem`; the former public `GiniLabeledValueList` was inlined. |
| Radio with error ring (selection sheet) | `radio/GiniRadioButton` | Wraps Material 3 `RadioButton` like `GiniCheckbox` wraps `Checkbox`; `isError`. |
| Filled "Cancel transfer" + outlined "Proceed anyway", stacked in portrait, side by side in landscape; single "Continue" on the selection sheet | `footer/GiniActionFooter` | Reuses `GiniButton`/`GiniOutlinedButton`; single-button variant. |

Per-case layout (from the Figma frames 1.2.1 / 1.3.1 / 1.2.1-Mix):

- **QR vs printed**: header, protect row, titled list [QR IBAN, printed IBAN].
- **Country only**: header, protect row, untitled list [Sender, "On the document" IBAN + hint].
- **Combined**: header, protect row, sender card, titled list [QR IBAN + hint, printed IBAN (+ hint)].

Selection sheet (frames 1.2.2 / 1.2.3 / 1.2.4 and `Mix-Sheet-*`): drag handle, title "Choose an
IBAN to continue" (heading), radio rows [QR IBAN (+ hint), printed IBAN (+ hint)] separated by a
hairline, red error line "Choose an IBAN to continue" under the rows in the error state, centred
grey footnote "You can still check the IBAN in the next step.", filled "Continue". In the error state
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
  announced with `announceForAccessibility` from a `LaunchedEffect` on every "Continue" without a
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
- The args carry values, not sentences: `senderName`, `senderCountry`, `qrIbanCountry`,
  `printedIbanCountry`. The screens build "From Poland, not the sender's country" and
  "Elektro Brandt GmbH · Germany" from `gc_fraud_iban_country_hint` and
  `gc_fraud_sender_with_country` (the latter `translatable="false"`, overridable all the same),
  so banks can reword them and the business logic never formats UI text. The development
  switch derives its sample countries with `Locale.getDisplayCountry`, the way PP-3965 should.
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

## Device verification (step 4, 2026-11-06, Samsung SM-S928B, Android 17, phone)

Driven through the example app's development case selector with an invoice photo imported from
the gallery. Passed:

- All three warning layouts match the Figma frames 1.2.1 / 1.3.1 / 1.2.1-Mix in light mode.
- Warning: back, scrim tap and swipe do nothing; the protect row expands; "Cancel transfer"
  ends the flow and returns to the host app; "Proceed anyway" continues to the extractions in
  the country case and opens the selection sheet in the other two.
- Selection sheet: initial, error ("Continue" without a choice: red rings + error line) and selected
  states match frames 1.2.2–1.2.4; back, scrim and swipe close it back to the warning; a
  reopened sheet starts clean; "Continue" with a choice continues to the extractions.
- Recreation: a font-scale change with the warning open and a dark-mode change with the sheet
  open both restored the expanded protect row, the open sheet and its selection. Rotation itself
  cannot be tested on a phone: `ActivityHelper` forces portrait there.
- 200 % font: IBANs wrap at block boundaries, the warning scrolls with the footer, the sheet
  scrolls, nothing clips.
- Accessibility tree: one merged stop per IBAN row with the IBAN spelled digit by digit, pane
  titles on both sheets, the sheet's scrim exposes Material's "Close sheet".
- External keyboard (adb key events): Tab order drag handle → QR row → printed row → Next,
  Space activates, focus stays inside the open sheet.

Open:

- **Dark mode: the warning's alert icon sits on a flat light square.** `errorHint.container`
  is `error04` = `gc_error_04` `#F6EEEE`, which has no `values-night` override, so the pale pink
  stays pale on the dark sheet. `card.containerError` has the same token, so existing error cards
  behave the same; a night value for `gc_error_04` (a dark red tint) fixes both — design
  decision.
- Tablet and landscape layouts (`GiniActionFooter` side by side, `tabletMaxWidth`) need a
  tablet; not covered.
- TalkBack speech (the error announcement after "Continue", the error read on the rows) was not
  verified with TalkBack running; the semantics are in the tree, the step-5 Robolectric tests
  assert them, a manual TalkBack pass is still owed.
- Keyboard focus is visible only as Material's faint state layer on the rows (theme-wide, see
  Accessibility).
- Example-app only: the four case buttons truncated on a phone; labels shortened to
  "Off / QR / Country / Both".

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
  warning; "Continue" without a choice shows the error, with a choice continues to the extractions.

---

# PP-3961 phase 2: Show the IBAN mismatch warning from the fraud signals

Status: draft
Ticket: https://ginis.atlassian.net/browse/PP-3961
Branch: `PP-3961-wire-fraud-signals` (from `PP-3961-Fraud-warning-screen-IBAN-mismatch`; PR base is that
branch, which is stacked on `PP-3965-read-fraud-signals`, PR #1028)
Builds on: PP-3965 (`FraudSignalsParser`, `FraudSignal`, `FraudSignalsExtractions`, the strip in
`AnalysisScreenPresenter`) and phase 1 above (components, `IBANMismatchWarningScreen`,
`showIbanMismatch`).
API shape: [Fraud signals in the Pay API](https://ginis.atlassian.net/wiki/spaces/bank/pages/2086897236) §4–§6.
Copy: [QR vs printed](https://ginis.atlassian.net/wiki/spaces/bank/pages/2065203277),
[country](https://ginis.atlassian.net/wiki/spaces/bank/pages/2065006657),
[combined](https://ginis.atlassian.net/wiki/spaces/bank/pages/2067169281).

**Build prerequisite:** this branch does not contain the PP-3965 code yet. Before `/gini-build`, merge
`origin/PP-3961-Fraud-warning-screen-IBAN-mismatch` again once PP-3965 has been merged into it (a
colleague resolves the one conflict in `AnalysisScreenPresenterTest.kt`). Check with
`git merge-base --is-ancestor origin/PP-3965-read-fraud-signals HEAD`.

**Phase 1 is still in progress** (as of 2026-11-05). This spec was written against
`origin/PP-3961-Fraud-warning-screen-IBAN-mismatch` at `08e0df987`. After merging its latest state,
re-check the names this spec relies on before building: `IBANMismatchArgs` fields,
`ibanMismatchContent` in `IBANMismatchWarningScreen.kt`, `showIbanMismatch`/`showIbanMismatchWarning`,
the `IBANMismatchDebug` branch and tests, and the example-app switch. Update the Design section if
they moved.

## Problem

Phase 1 shows the warning only behind a development switch, with fake data. PP-3965 reads the real
signals but nothing uses them. Phase 2 connects the two: after a successful analysis, when the
backend sent `QR_PRINTED_IBAN_MISMATCH` and/or `IBAN_COUNTRY_MISMATCH` and the client's
`fraudSignalsEnabled` flag is on, the warning shows the real IBANs, sender and country. The
development switch goes away.

Gating is two flags by design: the server-side profile flag `feature-fraud-signals` (PP-3950, wiki
§5) decides which signals the backend sends; the client flag `fraudSignalsEnabled` in
`/configurations` (PP-3951) decides whether the SDK reads them. The SDK keeps no per-client list of
codes: it shows the codes that arrive and ignores unknown ones (already done by the parser).

## Requirements

Common setup for R1–R13: `fraudSignalsEnabled = true` in `/configurations`, the analysis returns
`SUCCESS_WITH_EXTRACTIONS` with non-empty specific extractions. "QR IBAN" = `fraudSignals[].iban`.
"Candidates" = `extractions["iban"].candidates` values. IBANs are compared after removing spaces and
upper-casing ("normalised").

### Choosing the screen

- **R1 (MUST, entry):** Given `fraudSignals` = [`QR_PRINTED_IBAN_MISMATCH`, iban `<Q>`] and
  candidates [`<Q>`, `<P1>`], when the analysis finishes, then `AnalysisScreenContract.View.showIbanMismatch`
  is called once with `type = QR_VS_PRINTED`, `qrIban.iban = <Q>`, printed IBANs [`<P1>`], `sender = null`,
  and no hint on any row; `AnalysisFragmentListener.onExtractionsAvailable` is not called yet.
- **R2 (MUST, happy):** Given only `IBAN_COUNTRY_MISMATCH` with iban `<I>` (country prefix `PL`),
  `creditorCountry = DE` and `paymentRecipient = <N>`, when the analysis finishes, then the args are
  `type = COUNTRY`, `qrIban = null`, printed IBANs [`<I>` with hint country `PL`], sender name `<N>`,
  sender country `DE`.
- **R3 (MUST, happy):** Given both signals (iban `<Q>` = `PL…`, `creditorCountry = DE`) and candidates
  [`<P1>` = `DE…`, `<P2>` = `LT…`], when the analysis finishes, then one call with `type = COMBINED`:
  `qrIban = <Q>` with hint country `PL`; printed IBANs [`<P1>` without hint, `<P2>` with hint country
  `LT`]; sender `<N>` / `DE`. Never two screens in a row.
- **R4 (MUST, happy):** Given the QR signal and candidates [`<P1>`, `<Q>`, `<P2>`, `<P1>` with spaces],
  when the args are built, then the printed IBANs are [`<P1>`, `<P2>`]: candidate order kept, the QR
  IBAN and duplicates removed by normalised value. Each printed IBAN is one row on the screen.
- **R5 (MUST, happy):** A row's hint is set only when the country signal is present and that IBAN's
  first two letters (upper-cased) differ from `creditorCountry` (upper-cased). In the QR-only case no
  row has a hint. The hint text is `gc_fraud_iban_country_hint` with the country name of that IBAN.
- **R6 (MUST, happy):** The sender line is shown only when the country signal is present. Its text is
  `gc_fraud_sender_with_country` = "<name> · <country name>". If `paymentRecipient` is missing or blank, the
  line is the country name alone.

### Missing data and no warning

- **R7 (MUST, error):** Given the QR signal but no candidate that differs from the QR IBAN (no
  `iban` extraction, no candidates, or only the QR IBAN), when the args are built, then the QR part is
  dropped: with the country signal present the screen is `COUNTRY` (R2 shape, using the QR IBAN as the
  document IBAN); without it there is no warning and the flow is the same as R8.
- **R8 (MUST, error):** Given the flag is off, or `fraudSignals` is missing, or it holds only unknown or
  malformed elements, when the analysis finishes, then `showIbanMismatch` is never called and the flow
  is exactly as today (same warning or hint, or `onExtractionsAvailable` with the same extractions).
- **R9 (MUST, error):** Building the args never throws for any extraction map (missing members,
  blank values, IBANs shorter than two characters → treated as "country unknown": no hint).

### Flow

- **R10 (MUST, happy):** The fraud warning is checked after the two "no extractions" checks and before
  the already-paid, credit note, schedule payment and payment due checks, replacing the phase 1
  `IBANMismatchDebug.getEnabled()` branch. When it shows, none of the other warnings show.
- **R11 (MUST, happy):** Given the warning is shown, when the user taps "Proceed anyway", then
  `onExtractionsAvailable` receives the same specific extractions as the analysis returned and compound
  extractions without `fraudSignals` (the PP-3965 stripped holder). Same for all three types (the IBAN
  selection sheet for QR / combined is PP-3966).
- **R12 (MUST, happy):** "Cancel transfer" ends the flow through `AnalysisFragmentImpl.showIbanMismatch`'s
  cancel action (phase 1, unchanged).
- **R13 (MUST, happy):** The warning is shown in every `ProductTag`, CX mode included, as long as the CX
  "no extractions" check (`isCxEmptyExtractions`) did not already end the flow.
- **R14 (MUST, happy):** Country names are shown in the language of the SDK's strings: German with the
  default `values` strings ("Aus Polen, nicht aus dem Land des Absenders"), English with `values-en`
  ("From Poland, not the sender’s country"). An unknown country code is shown as the code itself.

### Cleanup

- **R15 (MUST, entry):** `IBANMismatchDebug` no longer exists; `capture-sdk/sdk/api/sdk.api` has no
  `IBANMismatchDebug` entry; the bank-sdk example app has no "IBAN mismatch warning (development)" switch.

## Affected modules

| Module | Change |
|---|---|
| `capture-sdk:sdk` | New args mapper and country-name helper in `analysis/fraud/`; `IBANMismatchArgs` reshaped; screen rows built from the list; presenter branch; extension injects `FraudSignalsParser`; new strings; debug switch deleted; dump re-pinned. |
| `bank-sdk:example-app` | Remove the development switch (phase 1 "Before merge" list). |
| `bank-sdk:sdk` | No code change. Rebuild and run its tests (depends on `capture-sdk:sdk`). |

`bank-api-library:library` and `capture-sdk:default-network` are unchanged here (PP-3965 did the flag).

## Public API impact

- `capture-sdk:sdk`: `net.gini.android.capture.analysis.fraud.IBANMismatchDebug` is removed from
  `api/sdk.api`. It was added on this unreleased branch stack, so nothing released breaks. Re-pin with
  `./gradlew capture-sdk:sdk:apiDump`; the diff must only remove `IBANMismatchDebug`.
- Everything new is `internal` and must not appear in the dump: `IBANMismatchArgs`,
  `IBANMismatchIban`, `IBANMismatchArgsMapper`, the country-name helper. `AnalysisScreenContract` is
  package-private Java.
- Integrator-visible behaviour: a new screen can appear after analysis for clients with both flags on.
  The result given to the bank is unchanged (PP-3965 strip). No new integrator toggle.
- New string resources (`gc_fraud_iban_country_hint`, `gc_fraud_sender_with_country`,
  `gc_fraud_country_name_locale`) are overridable by banks like the phase 1 strings.

## Technical conventions

1. **Language.** New code is Kotlin, `internal`, under `capture-sdk/sdk/src/main/java/`. Legacy Java
   touched: `analysis/AnalysisScreenPresenter.java` only — replace the `IBANMismatchDebug` branch
   (currently lines ~359–368) and nothing else. `AnalysisScreenContract.java` and
   `AnalysisFragmentImpl.java` need no change (the `showIbanMismatch(IBANMismatchArgs, Runnable)`
   contract stays). Java reads the Kotlin `internal` classes the same way phase 1 does.
2. **UI.** Compose (Material 3) in `GiniTheme`, existing light/dark + size previews updated to the new
   args shape (add one preview with two printed IBANs). No XML layout change. The rows keep phase 1's
   accessibility (merged rows, IBAN spelled for TalkBack, heading on the list title).
3. **Architecture.** Legacy Java MVP stays: the presenter decides, the view shows. New logic is a pure,
   stateless Kotlin mapper (`IBANMismatchArgsMapper`) — no ViewModel, no state/intent/effect classes
   (same decision as PP-3965). Texts are resolved in the Composable with `stringResource`, so the
   mapper stays JVM-testable and the args carry country **codes**, not display strings.
4. **DI / async.** `FraudSignalsParser` is injected into `AnalysisScreenPresenterExtension` from the
   isolated Koin context (`getGiniCaptureKoin().inject()`, like the hint use cases). The mapper is an
   `internal object` (no binding needed). Everything runs synchronously in the existing analysis
   callback; no new coroutines.
5. **Strings.** `capture-sdk/sdk/src/main/res/values/strings.xml` (German, default) and
   `values-en/strings.xml`:
   - `gc_fraud_iban_country_hint` — DE `Aus %1$s, nicht aus dem Land des Absenders`, EN
     `From %1$s, not the sender’s country` (`%1$s` = country name).
   - `gc_fraud_sender_with_country` — `%1$s · %2$s` in both (`%1$s` name, `%2$s` country name).
   - `gc_fraud_country_name_locale` — `translatable="false"`, `de` in `values`, `en` in `values-en`.
     The country name is built with `Locale("", code).getDisplayCountry(Locale.forLanguageTag(<this>))`,
     so it matches the language of the hint even on a device in a third language. KDoc on the
     resource tells banks that add a translation to override it too.
6. **Quality gates.** `ktlintCheck`, `detekt`, `lint`, `apiCheck` clean on `capture-sdk:sdk` and
   `bank-sdk:example-app`. Every new Kotlin class has a unit test; the mapper and the country-name
   helper should reach full line coverage in Jacoco (pure logic).

## Design

### Args shape (`analysis/fraud/IBANMismatchArgs.kt`)

```kotlin
internal data class IBANMismatchIban(
    val iban: String,
    val hintCountryCode: String? = null,   // non-null → show gc_fraud_iban_country_hint
)

internal data class IBANMismatchArgs(
    val type: IBANMismatchType,
    val qrIban: IBANMismatchIban?,          // null for COUNTRY
    val printedIbans: List<IBANMismatchIban>, // COUNTRY: exactly the document IBAN
    val senderName: String? = null,
    val senderCountryCode: String? = null,  // non-null → sender line shown
)
```

`printedIban`, `printedIbanHint`, `qrIbanHint` and `sender` from phase 1 are replaced. KDoc is updated
(texts are resolved by the screen, not by the caller).

### Mapper (`analysis/fraud/IBANMismatchArgsMapper.kt`, new)

```kotlin
internal object IBANMismatchArgsMapper {
    fun map(
        signals: List<FraudSignal>,
        extractions: Map<String, GiniCaptureSpecificExtraction>,
    ): IBANMismatchArgs?
}
```

1. `qr = signals.firstOrNull { code == QR_PRINTED_IBAN_MISMATCH }`,
   `country = signals.firstOrNull { code == IBAN_COUNTRY_MISMATCH }`.
2. `printed = extractions["iban"]?.candidates` values → drop blank → `distinctBy(normalise)` →
   drop those equal to `normalise(qr.iban)` (R4).
3. `qrUsable = qr != null && printed.isNotEmpty()` (R7).
4. Type: `qrUsable && country != null` → COMBINED; `qrUsable` → QR_VS_PRINTED; `country != null` →
   COUNTRY; else `null`.
5. Hints per R5 using `country.creditorCountry`; sender per R6 from `extractions["paymentRecipient"]`.
6. COUNTRY: `printedIbans = [country.iban]` (falls back to `qr.iban` per R7 — same value per the API).

Wrap nothing in try/catch: every step is null-safe by construction (R9), proven by tests.

### Country name helper (`analysis/fraud/CountryName.kt`, new)

`internal fun countryDisplayName(code: String, locale: Locale): String` — returns
`Locale("", code.uppercase()).getDisplayCountry(locale)`, or `code` when that is blank or equal to the
code (unknown). The screen calls it with `Locale.forLanguageTag(stringResource(R.string.gc_fraud_country_name_locale))`.

### Screen (`analysis/fraud/IBANMismatchWarningScreen.kt`)

`ibanMismatchContent(args)` builds: QR row (hint from `hintCountryCode`), one row per `printedIbans`
entry (label `gc_fraud_document_iban_label` for COUNTRY, `gc_fraud_printed_iban_label` otherwise), the
sender item from `senderName`/`senderCountryCode` (card in COMBINED, first row in COUNTRY, absent in
QR_VS_PRINTED — as phase 1). The layout still follows `type` alone.

### Presenter (`analysis/AnalysisScreenPresenter.java`) and extension

- Extension gets `val fraudSignalsParser: FraudSignalsParser by getGiniCaptureKoin().inject()` and
  ```kotlin
  fun ibanMismatchArgs(analysisResult: ResultHolder): IBANMismatchArgs? =
      IBANMismatchArgsMapper.map(
          fraudSignalsParser.parse(getMapOrEmpty(analysisResult.compoundExtractions)),
          getMapOrEmpty(analysisResult.extractions),
      )
  ```
  It reads the **original** `analysisResult` (PP-3965 renamed the callback parameter; `resultHolder`
  is the stripped one).
- In `SUCCESS_WITH_EXTRACTIONS`, compute `final IBANMismatchArgs ibanMismatchArgs =
  extension.ibanMismatchArgs(analysisResult);` before the if-chain, and replace the debug branch with
  `else if (ibanMismatchArgs != null)` → `successResultHolder = resultHolder; shouldClearImageCaches =
  false; extension.showIbanMismatchWarning(ibanMismatchArgs, …, successResultHolder, getActivity())`.
- `showIbanMismatchWarning` takes the args as its first parameter instead of calling
  `IBANMismatchDebug.sampleArgs()`. The invoice-saving and education handling stay as they are.

### Delete the development switch

Everything in phase 1's "Before merge" list: `IBANMismatchDebug.kt` (sample data moves into the
previews), the presenter branch, the three debug tests, the example-app switch
(`ExampleAppBankConfiguration.isIbanMismatchDebugEnabled`, `ConfigurationActivity.switchIbanMismatchDebug`,
`layout_debug_development_toggles.xml`, `ConfigurationViewModel.applyIbanMismatchDebugSwitch`,
`iban_mismatch_debug_switch_label` in the example app's `values` and `values-en`), then `apiDump`.

## Test plan

Stacks match the neighbours: JUnit4 + Google Truth; `AnalysisScreenPresenterTest` keeps its
Robolectric + Mockito-Kotlin/MockK mix and helpers (`startPresenterForDueHint`, `idleMainLooperUntil`,
`configurationProvider.update`).

| # | Test class | New / extend | Tests | Covers |
|---|---|---|---|---|
| 1 | `capture-sdk/sdk/src/test/java/net/gini/android/capture/analysis/fraud/IBANMismatchArgsMapperTest.kt` | **new**, plain JUnit | 10 (below) | R1–R7, R9 |
| 2 | `capture-sdk/sdk/src/test/java/net/gini/android/capture/analysis/fraud/CountryNameTest.kt` | **new**, plain JUnit | 3: `PL` in `de` → "Polen"; `PL` in `en` → "Poland"; unknown `XX` → "XX" | R14 |
| 3 | `capture-sdk/sdk/src/test/java/net/gini/android/capture/analysis/AnalysisScreenPresenterTest.kt` | extend; replace the 3 debug tests in "IBAN mismatch warning" | 5 (below) | R1, R8, R10, R11, R13 |

`IBANMismatchArgsMapperTest` (10 — one per screen case and per fallback; all pure and fast):
1. QR only → QR_VS_PRINTED, QR row and one printed row, no hints, no sender (R1, R5)
2. Country only → COUNTRY with document IBAN, hint country, sender name + country (R2, R6)
3. Both → COMBINED, hint only on IBANs whose country differs (R3, R5)
4. Several candidates with the QR IBAN and a spaced/lower-case duplicate → order kept, QR and duplicate removed (R4)
5. QR signal, no `iban` extraction → `null` (R7)
6. QR signal, candidates only the QR IBAN, plus country signal → COUNTRY (R7)
7. Country signal, no `paymentRecipient` → sender name `null`, country kept (R6)
8. No signals → `null` (R8 support)
9. Lower-case `creditorCountry` and IBAN prefix → compared case-insensitively (R5)
10. One-character / blank candidate and IBAN → no crash, no hint for it (R9)

Use different IBANs and countries in every fixture so a hardcoded result cannot pass.

`AnalysisScreenPresenterTest` "IBAN mismatch warning" region (5):
1. Flag on + QR signal + candidates → `showIbanMismatch` with captured args (`type`, QR and printed
   IBANs from the fixture), and no `showWarning` although the credit-note hint would apply (R1, R10)
2. "Proceed anyway" → `onExtractionsAvailable` with the fixture's specific extraction keys, and compound
   extractions without `fraudSignals` (R11)
3. Flag off with valid signals → no `showIbanMismatch`, `onExtractionsAvailable` as today (R8)
4. Flag on, no `fraudSignals` → no `showIbanMismatch` (R8)
5. CX mode (`ProductTag.CxExtractions`) with `crossBorderPayment` + signals → `showIbanMismatch` called (R13)

Commands: `./gradlew capture-sdk:sdk:testDebugUnitTest bank-sdk:sdk:testDebugUnitTest`, then
`/gini-check` (lint, detekt, ktlint) and `./gradlew capture-sdk:sdk:apiCheck`.

### Not tested

- The Compose screen rendering per args (no Compose UI test setup in `capture-sdk:sdk` unit tests):
  covered by the updated previews and manual QA. R6's string format and R12's cancel are UI wiring
  from phase 1.
- R15 (switch removed) is checked by `apiCheck` and by the build, not by a unit test.
- Rotation with the sheet open: same path as the other analysis warnings; manual check on device.
- End-to-end with the real backend: PP-3952 is In Review. Manual QA with the IBAN-mismatch documents
  from the epic's Drive folder once it ships; country-mismatch documents are still TBA.

## Out of scope

- The IBAN selection sheet after "Proceed anyway" for QR / combined (PP-3966).
- Using `severity` (wiki §4: v1 does not use it).
- Analytics for the warnings (no ticket yet).
- Public docs (PP-3968), Figma guide (PP-3967), UI automation (PP-3970).
- Any change to the PP-3965 parser, the flag, or the strip.
- Copy differences between phase 1 strings and the copy pages (e.g. combined list title "The following
  IBANs were detected" vs "Found on the document"; "Proceed anyway" vs "Continue anyway"): report to
  the phase 1 author, do not change here.

## Open questions

- **Label of several printed IBANs (layout only):** showing every printed IBAN is decided (R4). Only
  the label is open: the design for this case is "TBA" in PP-3518/PP-3966, so each row repeats
  "Printed on the document" for now. If design asks for one shared label or numbered labels, only
  `ibanMismatchContent` in `IBANMismatchWarningScreen.kt` changes; the mapper and R4 stay.
- **CX mode data (confidence: LOW — confirm with backend / a CX test document):** decided to show the
  warning in CX mode. Whether CX responses carry `fraudSignals` and `extractions.iban` candidates is
  unverified; without candidates the QR part is dropped by R7.

## Build notes (phase 2)

- PP-3965 is merged into this branch locally (merge commit `ef5de3ca0`, not pushed); the one conflict
  in `AnalysisScreenPresenterTest.kt` was solved by keeping both regions.
- The development switch is kept for now, but it no longer shows fixed args: while
  `IBANMismatchDebug.enabled` it adds a sample `fraudSignals` compound (both signals, QR IBAN
  `PL12…`, sender country `DE`) and sample printed IBANs (`DE89…`, the QR IBAN, `AT61…`), and the
  **real** `FraudSignalsParser` (flag forced on) and `IBANMismatchArgsMapper` build the warning. The
  scanned document's `paymentRecipient` is kept. The example-app switch is unchanged. Remove it with
  the rest of "Before merge" once the backend delivers fraud signals.
- The parser for the real path reads `fraudSignalsEnabled`; the debug path ignores it on purpose.

## Temporary UI changes to reconcile with phase 1

Phase 1 was still in progress when phase 2 was built (base `08e0df987`). These phase 1 files were
changed here and must be checked against the colleague's final version when PP-3961 is merged in
again:

| File | What phase 2 changed |
|---|---|
| `analysis/fraud/IBANMismatchArgs.kt` | Reshaped: `qrIban: IBANMismatchIban?`, `printedIbans: List<IBANMismatchIban>`, `senderName`, `senderCountryCode`; new `IBANMismatchIban(iban, hintCountryCode)`. Phase 1's `printedIban`, `printedIbanHint`, `qrIbanHint`, `sender` are gone. |
| `analysis/fraud/IBANMismatchWarningScreen.kt` | `ibanMismatchContent` resolves sender and hint texts, one row per printed IBAN; new `ibanRow` helper; previews use the new args (combined preview with two printed IBANs). Layout composables untouched. |
| `analysis/fraud/IBANMismatchDebug.kt` | `sampleArgs()` replaced by `parser`, `withSampleSignals`, `withSamplePrintedIbans`. |
| `analysis/AnalysisScreenPresenterExtension.kt` | New `ibanMismatchArgs(analysisResult)`; `showIbanMismatchWarning` takes the args first. |
| `analysis/AnalysisScreenPresenter.java` | Debug branch replaced by `ibanMismatchArgs != null`, args computed before the if-chain. |
| `res/values{,-en}/strings.xml` | Added `gc_fraud_iban_country_hint`, `gc_fraud_sender_with_country`, `gc_fraud_country_name_locale` after `gc_fraud_document_iban_label`. |

Reconcile rule: keep the colleague's layout, components and copy; keep phase 2's args shape,
mapper and text resolution. If phase 1 renamed or moved a piece listed above, move the phase 2
change with it, then re-run `IBANMismatchArgsMapperTest`, `CountryNameTest` and the "IBAN mismatch
warning" region of `AnalysisScreenPresenterTest`.

### Reconciled with phase 1 (2026-11-09)

Merged `origin/PP-3961-Fraud-warning-screen-IBAN-mismatch` at `ba4ff435c` (selection sheet, case
selector, string copy). What was kept from each side:

- **Phase 1 kept:** layout, components, copy, `gc_fraud_iban_country_hint` and
  `gc_fraud_sender_with_country` (phase 2's duplicate `gc_fraud_sender_value` dropped), the
  `Consumer<String>` proceed callback, the IBAN selection sheet and the example-app case selector
  (`IBANMismatchDebug.Case`).
- **Phase 2 kept:** the args shape (`printedIbans` list, country **codes**), the mapper, and the
  country-name resolution in Compose via `gc_fraud_country_name_locale`.
- **Changed in phase 1 code:** `IBANSelectionArgs` now holds `qrIban: IBANMismatchIban` and
  `printedIbans: List<IBANMismatchIban>`, so the selection sheet offers the QR IBAN plus every
  printed IBAN, like the warning. Row building is shared: `ibanItem` and `countryNameLocale` in
  `IBANMismatchWarningScreen.kt` replace phase 1's `countryHint(country: String?)`.
- **Debug switch:** each `Case` adds its own sample signals (QR only / country only / both) and the
  real parser and mapper build the args; phase 1's `sampleArgs(case)` is gone.
- The chosen IBAN from the selection sheet is still ignored by the presenter (PP-3966).

### Development switch removed (2026-11-09)

The backend delivers `fraudSignals` on prod (checked on a Samsung S22 with a Girocode invoice
whose QR IBAN differs from the printed one: the real `QR_PRINTED_IBAN_MISMATCH` opened the warning
and the selection sheet). So the "Before merge" cleanup is done on this branch:

- `IBANMismatchDebug.kt` deleted; `ibanMismatchArgs` reads only the real parser and mapper.
- The example app's "IBAN mismatch warning (development)" selector removed (its six files are back
  to `main`).
- The two development-switch tests in `AnalysisScreenPresenterTest` removed; the mapper tests cover
  the three screen types.
- `capture-sdk/sdk/api/sdk.api` re-pinned with `apiDump`; the only change is the removed
  `IBANMismatchDebug` and `IBANMismatchDebug$Case`.

Not yet seen from prod: `IBAN_COUNTRY_MISMATCH` (three foreign-IBAN invoices returned no signal).

---

# PP-3961 phase 3: Continue into the normal hints after the fraud warning

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-3961
Branch: `PP-3961-wire-fraud-signals`, on top of the PP-3966 work (`specs/PP-3966-feature.md`,
uncommitted at the time of writing — commit it first or build on the same working tree).
Source: [Fraud prevention v1: UI/UX flow and testing conditions](https://ginis.atlassian.net/wiki/spaces/bank/pages/2100920349),
§2 step 8: "Country-only warning: 'Continue anyway' goes to bank app flow (e.g. Already paid,
Credit note)".

## Problem

The fraud warning is the first branch of the Analysis presenter's hint chain, and "Proceed anyway"
ends the flow: the extractions go straight to the bank app. So a document that is also already
paid, a credit note, schedulable or nearly due never shows that hint. Example: a credit note with
a foreign IBAN keeps its `amountToPay`, because the credit-note branch (which removes it) is never
reached. The UI/UX page says the flow must continue into the bank app flow after the warning.

## Decisions (from the clarifying questions)

- All three warning types (QR vs printed, country, combined) continue into the hint chain after
  "Proceed anyway" (after the selection sheet where it opens). One rule for all.
- The page's step 8 names only the country-only warning, and step 5 is marked "(to be
  confirmed)". Confirmed 2026-11-10: all three fraud warning types continue into the normal hints.
- Order: fraud warning first, then the first matching hint, then the bank app. Invoice saving runs
  once, at the end, through the hint's own CTA.

## Requirements

Setup: `fraudSignalsEnabled = true`, the result has a fraud signal, so the warning shows. "The
chain" = the existing branches after the fraud branch in `AnalysisScreenPresenter`:
already paid → credit note → schedule payment → payment due → plain proceed.

- **R1 (MUST, entry):** Given a fraud signal and `paymentState = "Paid"` with the already-paid hint
  enabled, when the user taps "Proceed anyway" (country-only) or picks an IBAN and taps "Continue" (QR
  or combined), then `View.showWarning(WarningType.DOCUMENT_MARKED_AS_PAID, …)` is called, and
  `onExtractionsAvailable` is not called yet.
- **R2 (MUST, happy):** Given a fraud signal and a credit note (`businessDocType = "CreditNote"`,
  credit-note hint enabled), after "Proceed anyway" the credit-note warning shows, and when its
  "Proceed" runs, `onExtractionsAvailable` receives extractions **without** `amountToPay`.
- **R3 (MUST, happy):** Given a fraud signal and a due date that triggers the schedule-payment hint,
  after "Proceed anyway" the schedule-payment sheet shows; its "Schedule" CTA ends in
  `onSchedulePayment`, its "Proceed" in `onExtractionsAvailable`.
- **R4 (MUST, happy):** Given a fraud signal and the payment-due hint conditions, after "Proceed
  anyway" `showWarning(WarningType.PAYMENT_DUE_DATE, …)` is called.
- **R5 (MUST, happy):** Given a fraud signal and no hint condition, "Proceed anyway" hands the
  extractions on exactly as today (no extra screen).
- **R6 (MUST, happy):** The IBAN chosen on the selection sheet (PP-3966) is still the `"iban"` value
  the bank app receives at the end of every path in R1–R5, including after the credit-note
  `removeAmountToPay` copy and on the `onSchedulePayment` path.
- **R7 (MUST, async):** Invoice saving (Storage Access Framework) is requested exactly once per
  flow — by the hint's CTA, or by the plain proceed when no hint applies — never by the fraud
  warning itself. With saving enabled, `View.processInvoiceSaving` is called once.
- **R8 (MUST, async):** Given the screen is recreated while the folder picker is open after a hint
  CTA that followed the fraud warning, when the flow restarts, then the fraud warning and the hint
  are not shown again and the flow continues with the restored pending action and the restored
  chosen IBAN (e.g. "Schedule" after recreation still ends in `onSchedulePayment`).
- **R9 (MUST, error):** "Cancel transfer" on the fraud warning still ends the flow
  (`AnalysisFragmentImpl.showIbanMismatch`'s cancel), and no hint is shown afterwards.
- **R10 (MUST, happy):** Documents without a fraud signal go through the chain exactly as before
  (all existing hint tests stay green unchanged).

## Affected modules

| Module | Change |
|---|---|
| `capture-sdk:sdk` | `AnalysisScreenPresenter.java`: the chain after the fraud branch moves into a private method that both the normal path and the fraud warning's proceed call. `AnalysisScreenPresenterExtension.kt`: `showIbanMismatchWarning` takes a continuation instead of starting invoice saving. |
| `bank-sdk:sdk` | No code change; run its tests. |

## Public API impact

None. `AnalysisScreenPresenter` and `AnalysisScreenContract` are package-private Java; the
extension is `internal`. No `api/sdk.api` change; `apiCheck` must pass without `apiDump`.

## Technical conventions

1. **Language.** Kotlin for the extension change. Legacy Java touched: `AnalysisScreenPresenter.java`
   only — an extract-method refactor of the existing branches (`shouldShowAlreadyPaidInvoiceWarning`
   … final `proceedWithExtractions`, lines ~376–414), no behaviour change for documents without a
   fraud signal. No conversion.
2. **UI.** None. The warning, the sheet and the hint sheets are unchanged.
3. **Architecture.** Legacy Java MVP stays: the presenter owns the decision order; the extension
   owns showing the warning. No ViewModel.
4. **DI / async.** None new. The continuation runs on the main thread (the Compose callback); every
   chain branch already schedules its UI through `doWhenEducationFinished`
   (`AnalysisScreenPresenterExtension.kt`, `scope.launch` + `withContext(Dispatchers.Main)`).
5. **Strings.** None.
6. **Quality gates.** `ktlintCheck`, `detekt`, `lint`, `apiCheck` clean on `capture-sdk:sdk`;
   `bank-sdk:sdk:testDebugUnitTest` green.

## Design

### Presenter (`analysis/AnalysisScreenPresenter.java`)

- New `private void showHintOrProceed(@NonNull AnalysisInteractor.ResultHolder resultHolder)`
  containing the branches that follow the fraud branch today, unchanged: already paid
  (`successResultHolder = resultHolder; shouldClearImageCaches = false; showDocumentMarkedWarning`),
  credit note (`removeAmountToPay`), schedule payment, payment due, else `proceedWithExtractions`.
- The `SUCCESS_WITH_EXTRACTIONS` case keeps the two "no extractions" checks, then:
  `else if (ibanMismatchArgs != null) { successResultHolder = resultHolder; shouldClearImageCaches = false; extension.showIbanMismatchWarning(ibanMismatchArgs, isSavingInvoicesInProgress, () -> showHintOrProceed(resultHolder)); } else { showHintOrProceed(resultHolder); }`.
  `shouldClearImageCaches = false` is set once in the outer branch (the continuation runs later,
  so it cannot set the local), which gives the same value after the switch as today.

### Extension (`analysis/AnalysisScreenPresenterExtension.kt`)

- `showIbanMismatchWarning(args, isSavingInvoicesInProgress, onContinue: () -> Unit)`:
  - `isSavingInvoicesInProgress` → call `onContinue()` at once, without showing the warning (R8):
    the chain's matching branch then sees the same flag and resumes saving with the restored
    `pendingSavingAction` (the schedule-payment branch already passes it) and the restored
    `chosenIban` (PP-3966).
  - otherwise `doWhenEducationFinished { view.showIbanMismatch(args) { iban -> chosenIban = iban; onContinue() } }`.
  - The `mIsInvoiceSavingEnabled`, `resultHolder` and `activity` parameters and the
    `handleSaveInvoicesLocally` call are removed from this method (R7).
- `chosenIban` stays stored until `runPendingSavingAction` applies and clears it (PP-3966), so it
  survives the hint step and the `removeAmountToPay` copy (R6).

### Unchanged

`AnalysisScreenContract.View.showIbanMismatch`, `AnalysisFragmentImpl`, `AnalysisFragmentExtension`,
the warning and sheet composables, the hint sheets, `ChosenIbanExtractions`.

## Test plan

Extend `capture-sdk/sdk/src/test/java/net/gini/android/capture/analysis/AnalysisScreenPresenterTest.kt`,
"IBAN mismatch warning" region — Robolectric + Mockito-Kotlin + Truth, existing helpers
(`startPresenterForDueHint`, `creditNoteExtractions`, `dueHintExtractions`, `qrMismatchCompounds`,
`ibanWithCandidates`, `idleMainLooperUntil`). About 7 tests; the chain has five exits and two async
paths, each a distinct regression:

1. Fraud + already paid → after proceed, `showWarning(DOCUMENT_MARKED_AS_PAID, …)`; no
   `onExtractionsAvailable` yet (R1). The existing "…ahead of other warnings" test stays: it checks
   that no hint shows while the warning is open, which is still true.
2. Fraud + credit note → after proceed and the credit-note "Proceed", extractions without
   `amountToPay` and with the chosen `PRINTED_IBAN` (R2, R6).
3. Fraud + schedule-payment hint → "Schedule" → `onSchedulePayment` with the chosen IBAN (R3, R6).
4. Fraud + payment-due hint → `showWarning(PAYMENT_DUE_DATE, …)` (R4).
5. Fraud, no hint → as today (existing proceed tests cover it; keep them) (R5).
6. Fraud + already paid with invoice saving enabled → `processInvoiceSaving` called once, after
   the already-paid "Proceed", not after the fraud "Proceed anyway" (R7).
7. Recreation: `updateInvoiceSavingState(true, "SCHEDULE_PAYMENT", PRINTED_IBAN)` then `start()`
   with fraud + schedule conditions → no `showIbanMismatch`, `onSchedulePayment` with
   `PRINTED_IBAN` (R8).

R9 is the existing cancel wiring (unchanged view code). R10: the whole existing presenter suite
must stay green without edits to non-fraud tests.

### Not tested

- The visual sequence warning → sheet → hint sheet on device (manual: Musterfirma Girocode invoice
  plus a document that is also marked paid, if one is available in the epic's test folder).
- Compose rendering of the sheets (no Compose UI test setup in `capture-sdk:sdk` unit tests).

## Out of scope

- Changing the order or content of the existing hints.
- The BIC question (PP-3966 open question).
- iOS behaviour (PP-3960/PP-3962).

## Open questions

None. The step 5 / step 8 conflict on the UI/UX page is settled: all three fraud warning types
continue into the normal hints (confirmed 2026-11-10). The iOS team should build the same, and the
UI/UX page still says "(to be confirmed)" on step 5.

## Implementation plan
- [x] 1. Tests first in `AnalysisScreenPresenterTest.kt`, "IBAN mismatch warning" region: fraud +
  already paid, credit note, schedule, payment due, saving once, recreation (R1–R4, R6–R8); the
  existing saving test waits for the now-async saving request (R5, R10)
- [x] 2. `AnalysisScreenPresenter.java`: extract `showHintOrProceed`; the fraud branch passes it
  as the continuation. `AnalysisScreenPresenterExtension.kt`: `showIbanMismatchWarning(args,
  isSavingInvoicesInProgress, onContinue)` (R1–R9)
- [x] 3. Verify: `/gini-check capture-sdk:sdk bank-sdk:sdk`, `apiCheck`, code review (R10)

## Build notes

- Verification: `testDebugUnitTest`, `lint`, `detekt`, `ktlintCheck` and `apiCheck` pass on
  `capture-sdk:sdk` and `bank-sdk:sdk` (no `api/sdk.api` change).
- Tests: 7 new presenter tests (already paid, credit note, schedule "Schedule" and "Proceed",
  payment due, saving once, recreation during saving); the existing saving test waits for the
  saving request, which now goes through `doWhenEducationFinished`.
- Device (S22, prod, Musterfirma Girocode invoice, already-paid hint forced by a temporary local
  change that was removed afterwards): fraud warning → printed IBAN → "Document marked as paid" →
  "Proceed anyway" → `iban` = `DE02120300000000202051`.
- Recreation while a hint is open after the fraud warning (dark mode switched on device): the fraud
  warning shows again on top, the user picks the IBAN again, then the hint shows with working
  buttons and the chosen IBAN is handed on. The user is not stuck and no IBAN is lost. The repeat
  of the fraud warning is accepted as is (decision 2026-11-10); persisting "fraud warning passed"
  is not built.


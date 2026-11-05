# PP-3961: [Android] Fraud warning screen: IBAN mismatch — phase 1, UI components

Status: phase 1 implemented (UI only); phase 2 (wiring, end of this file) draft
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
- One screen, `analysis/fraud/IBANMismatchWarningScreen`, composed only from those components.
  Its content is `IBANMismatchArgs`; the same screen renders the QR-vs-printed, country-only and
  combined cases.
- The Analysis-screen hooks taken over from the spike: `IBANMismatchDebug` (restricted-visibility
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
  (PP-3965) and building `IBANMismatchArgs` from it — the hints ("From Poland, not the sender's
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

IBANs are grouped in blocks of four for display (`internal/iban/IBANDisplayFormat.kt`) so they wrap
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

- `IBANMismatchDebug` is public only because the example-app switch has to reach into
  capture-sdk's presenter and Kotlin `internal` stops at the module boundary; `@RestrictTo` does
  not hide it from `apiCheck` (no `nonPublicMarkers`). It is branch scaffolding, not a feature:
  see "Before merge" below.
- `IBANMismatchArgs` is a plain `internal` data class, not `@Parcelize`: nothing persists it, and
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

- [ ] `capture-sdk/sdk/src/main/java/net/gini/android/capture/analysis/fraud/IBANMismatchDebug.kt`
      (the object and `sampleArgs()`; move the preview data into the previews in
      `IBANMismatchWarningScreen.kt`)
- [ ] the `else if (IBANMismatchDebug.getEnabled())` branch in `AnalysisScreenPresenter.java` and
      `showIbanMismatchWarning` in `AnalysisScreenPresenterExtension.kt`
- [ ] the "IBAN mismatch warning" region of `AnalysisScreenPresenterTest.kt` (three tests; phase 2
      brings them back driven by extractions)
- [ ] bank-sdk example app: `isIbanMismatchDebugEnabled` in `ExampleAppBankConfiguration`,
      `switchIbanMismatchDebug` in `ConfigurationActivity` and
      `layout_debug_development_toggles.xml`, `applyIbanMismatchDebugSwitch` in
      `ConfigurationViewModel`, the `iban_mismatch_debug_switch_label` string
- [ ] `./gradlew capture-sdk:sdk:apiDump` so `IBANMismatchDebug` leaves `sdk.api`
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
  `IBANDisplayFormatTest`.
- `/gini-check` on `capture-sdk:sdk` and `bank-sdk:example-app`; `apiDump` for `capture-sdk:sdk`.
- Manual: example app → Configuration → Debug / development → "IBAN mismatch warning
  (development)" → scan any invoice → the combined warning appears over the Analysis screen;
  back/scrim/swipe do nothing; rotation keeps the sheet and the expanded protect row; "Cancel
  transfer" ends the flow; "Proceed anyway" continues to the extractions.

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
  `gc_fraud_sender_value` = "<name> · <country name>". If `paymentRecipient` is missing or blank, the
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
- New string resources (`gc_fraud_iban_country_hint`, `gc_fraud_sender_value`,
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
   - `gc_fraud_sender_value` — `%1$s · %2$s` in both (`%1$s` name, `%2$s` country name).
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
| `res/values{,-en}/strings.xml` | Added `gc_fraud_iban_country_hint`, `gc_fraud_sender_value`, `gc_fraud_country_name_locale` after `gc_fraud_document_iban_label`. |

Reconcile rule: keep the colleague's layout, components and copy; keep phase 2's args shape,
mapper and text resolution. If phase 1 renamed or moved a piece listed above, move the phase 2
change with it, then re-run `IBANMismatchArgsMapperTest`, `CountryNameTest` and the "IBAN mismatch
warning" region of `AnalysisScreenPresenterTest`.

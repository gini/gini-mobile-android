# PP-3965: [Android] Read fraud signals from the extractions

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-3965
Epic: [PP-3518](https://ginis.atlassian.net/browse/PP-3518) (Fraud prevention warnings – phase 1) ·
Depends on: [PP-3951](https://ginis.atlassian.net/browse/PP-3951) (`fraudSignalsEnabled` in `/configurations`),
[PP-3952](https://ginis.atlassian.net/browse/PP-3952) (`fraudSignals` compound extraction) ·
Consumer: [PP-3961](https://ginis.atlassian.net/browse/PP-3961) (Android fraud warning screen) ·
iOS twin: [PP-3959](https://ginis.atlassian.net/browse/PP-3959) ·
API shape: [Fraud signals in the Pay API – response proposal](https://ginis.atlassian.net/wiki/spaces/bank/pages/2086897236)

## Problem

The backend will start to return a new compound extraction, `fraudSignals`. Each element is one
fraud signal: the IBAN in the QR code does not match the IBAN printed on the invoice
(`QR_PRINTED_IBAN_MISMATCH`), or the IBAN country does not match the payment recipient's country
(`IBAN_COUNTRY_MISMATCH`). The backend sends it only to clients whose `/configurations` response has
`fraudSignalsEnabled = true`.

The Android SDK must:

1. Read the new `fraudSignalsEnabled` flag from `/configurations`.
2. Turn the raw `fraudSignals` compound extraction into a typed list that the warning screen
   (PP-3961) can use — only when the flag is on.
3. Make sure `fraudSignals` never leaves the SDK: not in the analysis result given to the bank,
   and not in the feedback (transfer summary).

This ticket is the **data layer only**. It shows no UI. It follows the same pattern as the credit
note hint: a flag use case plus a parser. PP-3961 adds the `shouldShowFraudWarning` branch in the
Analysis presenter and passes the parsed list straight to the screen.

Example payload (from the API proposal):

```json
"compoundExtractions": {
  "fraudSignals": [
    { "signalCode": { "value": "QR_PRINTED_IBAN_MISMATCH", "entity": "text" },
      "severity":   { "value": "STOP", "entity": "text" },
      "iban":       { "value": "LT601010012345678901", "entity": "iban", "candidates": "ibans" } },
    { "signalCode":      { "value": "IBAN_COUNTRY_MISMATCH", "entity": "text" },
      "severity":        { "value": "CHECK", "entity": "text" },
      "iban":            { "value": "LT601010012345678901", "entity": "iban", "candidates": "ibans" },
      "creditorCountry": { "value": "DE", "entity": "text" } }
  ]
}
```

On Android this arrives as `compoundExtractions["fraudSignals"].specificExtractionMaps`, a
`List<Map<String, GiniCaptureSpecificExtraction>>`. The compound mapper is generic, so no
API-library parsing change is needed for the extraction itself.

## Requirements

### Configuration flag

- **R1 (MUST, entry):** Given `/configurations` returns `"fraudSignalsEnabled": true`, when the
  configuration is mapped, then `net.gini.android.bank.api.models.Configuration.isFraudSignalsEnabled`
  is `true`, and after `GiniCaptureDefaultNetworkService.getConfiguration` it is `true` in the
  capture-sdk `Configuration` held by `GiniBankConfigurationProvider`.
- **R2 (MUST, error):** Given `/configurations` has no `fraudSignalsEnabled` field (older backend)
  or `null`, when it is mapped, then `isFraudSignalsEnabled` is `false`. No exception is thrown.
- **R3 (MUST, happy):** Given the flag was `true` in the last `/configurations` response, when
  `ClientConfigurationStorage.saveConfiguration` and then `getConfiguration` run, then the stored
  configuration has `isFraudSignalsEnabled = true` (same caching as every other flag).
- **R4 (MUST, entry):** Given `GiniBankConfigurationProvider` holds `isFraudSignalsEnabled = X`,
  when `GetFraudSignalsEnabledUseCase()` is invoked, then it returns `X` (default `false`).

### Parsing (`FraudSignalsParser`)

- **R5 (MUST, happy):** Given the flag is on and `fraudSignals` has one element with
  `signalCode = QR_PRINTED_IBAN_MISMATCH`, `severity = STOP`, `iban = <I>`, when `parse` runs, then
  it returns exactly one `FraudSignal(code = QR_PRINTED_IBAN_MISMATCH, severity = STOP, iban = <I>,
  creditorCountry = null)`.
- **R6 (MUST, happy):** Given the flag is on and one element with `signalCode =
  IBAN_COUNTRY_MISMATCH`, `severity = CHECK`, `iban = <I>`, `creditorCountry = <C>`, when `parse`
  runs, then it returns exactly one `FraudSignal(IBAN_COUNTRY_MISMATCH, CHECK, <I>, <C>)`.
- **R7 (MUST, happy):** Given the flag is on and both elements are present, when `parse` runs, then
  it returns both signals, in the order of the response, with the values from each element.
- **R8 (MUST, error):** Given the flag is on and an element has an unknown `signalCode`
  (e.g. `SOME_FUTURE_SIGNAL`), when `parse` runs, then that element is skipped, every valid element
  is still returned, and no exception is thrown.
- **R9 (MUST, error):** Given the flag is on and `compoundExtractions` has no `fraudSignals` key, or
  the key has an empty `specificExtractionMaps`, when `parse` runs, then it returns an empty list.
- **R10 (MUST, error):** Given the flag is **off** and `fraudSignals` contains valid elements, when
  `parse` runs, then it returns an empty list.
- **R11 (MUST, error):** Given the flag is on and an element with a known `signalCode` is malformed,
  when `parse` runs, then only that element is dropped and the others are still returned.
  Malformed means any of:
  - `severity` member missing, blank, or not one of `STOP`, `CHECK`, `ESCALATOR`, `SILENT`;
  - `iban` member missing or blank;
  - `signalCode = IBAN_COUNTRY_MISMATCH` and `creditorCountry` member missing or blank.
- **R12 (SHOULD, error):** Given the flag is on and two elements have the same known `signalCode`,
  when `parse` runs, then only the first valid one is returned (the API promises each code at most
  once).
- **R13 (MUST, happy):** Given a `QR_PRINTED_IBAN_MISMATCH` element that also has a
  `creditorCountry` member, when `parse` runs, then the signal is returned and `creditorCountry` is
  set from that member (it is not required for this code, but not thrown away).

### Never leaves the SDK

- **R14 (MUST, happy):** Given an analysis returns `SUCCESS_WITH_EXTRACTIONS` with compound
  extractions `{fraudSignals, lineItems}` (any flag state, any `ProductTag`), when
  `AnalysisScreenPresenter` forwards the result, then the `compoundExtractions` passed on through
  `AnalysisScreenPresenterExtension.proceedWithExtractionsWhenEducationFinished` (and therefore to
  `AnalysisFragmentListener`, `CaptureSDKResult.Success`, bank-sdk's `CaptureFlowFragment`, Return
  Assistant, Skonto and `CameraActivity`) contain `lineItems` and do **not** contain
  `fraudSignals`. Specific extractions are unchanged.
- **R15 (MUST, happy):** Same as R14 for every other branch that stores the result holder:
  already-paid warning, credit note warning, schedule payment hint, payment due hint — the stored
  `successResultHolder` has no `fraudSignals`.
- **R16 (MUST, error):** Given the analysis returns specific extractions that are empty and compound
  extractions that hold only `fraudSignals` (non-CX mode), when the presenter handles the result,
  then the no-extractions path (`proceedSuccessNoExtractions`) is taken — the same as today when
  there are no extractions.
- **R17 (MUST, happy):** Given `GiniCapture.getInstance().internal().getCompoundExtractions()`
  contains `fraudSignals` (defensive: e.g. set by a future caller), when
  `GiniCapture.sendTransferSummary(paymentRecipient, …, instantPayment)` **or**
  `GiniCapture.sendTransferSummary(Map<String, String>)` (SEPA branch) runs, then the
  `compoundExtractions` passed to `GiniCaptureNetworkService.sendFeedback` do not contain
  `fraudSignals`.
- **R18 (MUST, happy):** Given the flag is off, or `fraudSignals` is missing, when an analysis
  completes, then the flow is exactly as today: same branch taken, same specific extractions, same
  other compound extractions, no warning.

## Affected modules

| Module | Change |
|---|---|
| `bank-api-library:library` | `ConfigurationResponse` + `Configuration` get the `fraudSignalsEnabled` flag. |
| `capture-sdk:default-network` | Maps the new flag into the capture-sdk `Configuration`. |
| `capture-sdk:sdk` | Internal `Configuration` field, storage key, use case, Koin binding, new `fraudSignals` package (model + parser + strip helper), strip in `AnalysisScreenPresenter`, safety strip in `GiniCapture.sendTransferSummary`. |
| `bank-sdk:sdk` | **No code change.** Gets the stripped result from capture-sdk. Rebuild + run its tests because it depends on capture-sdk. |

Dependency direction: `bank-api-library` ← `capture-sdk:default-network` ← `bank-sdk:sdk`;
`capture-sdk:sdk` ← `bank-sdk:sdk`. `health-sdk` / `internal-payment-sdk` are not affected (they
do not use `bank-api-library` or `capture-sdk`).

## Public API impact

- **`bank-api-library:library`** — additive in source, needs a dump re-pin:
  - `net.gini.android.bank.api.response.ConfigurationResponse`: new property
    `fraudSignalsEnabled: Boolean? = null` (`@Json(name = "fraudSignalsEnabled")`), added at the
    **end**, after `ingredientBrandScreens`, so existing positional callers are not shifted.
  - `net.gini.android.bank.api.models.Configuration`: new property
    `isFraudSignalsEnabled: Boolean = false`, added at the **end**.
  - Both are public `data class`es. Adding a constructor parameter with a default keeps Kotlin source
    compatibility, but changes the JVM constructor / `copy` / `componentN` signatures. This is the
    same kind of change the credit note flag made (commit `ae80e6805`, dump re-pin `e6c1eb68a`).
    Re-pin `bank-api-library/library/api/library.api` (`./gradlew bank-api-library:library:apiDump`).
- **`capture-sdk:sdk`** — `net.gini.android.capture.internal.network.Configuration` is listed in
  `capture-sdk/sdk/api/sdk.api` (it is a public class in an `internal` package). It gets
  `isFraudSignalsEnabled: Boolean = false` at the **end** (after `ingredientBrandScreens`). Re-pin
  `capture-sdk/sdk/api/sdk.api` (`./gradlew capture-sdk:sdk:apiDump`). All new classes are `internal`
  and must not appear in the dump.
- **`capture-sdk:default-network`**, **`bank-sdk:sdk`** — none.
- **No new integrator toggle.** There is no `fraudSignalsEnabled` in bank-sdk `CaptureConfiguration`
  or `GiniCapture.Builder` (decided: only the `/configurations` flag gates the feature).
- **Integrator-visible behaviour:** `CaptureResult.Success.compoundExtractions` (bank-sdk) and
  `CaptureSDKResult.Success.compoundExtractions` / `CameraActivity.EXTRA_OUT_COMPOUND_EXTRACTIONS`
  (capture-sdk) never contain a `"fraudSignals"` key. Today they would pass it through if the
  backend sent it, so this is a deliberate filter, not a removal of something integrators get today.

## Technical conventions

1. **Language.** All new code is Kotlin, `internal`, under `src/main/java/`. Legacy Java files that
   may be touched, and why:
   - `capture-sdk/sdk/src/main/java/net/gini/android/capture/analysis/AnalysisScreenPresenter.java`
     — the strip must happen at the one point where the analysis result enters the success branch.
     Touch only the `SUCCESS_WITH_EXTRACTIONS` case; do not convert the file.
   - `capture-sdk/sdk/src/main/java/net/gini/android/capture/GiniCapture.java` — add
     `remove("fraudSignals")` next to the existing `remove("skontoDiscounts")` in both
     `sendTransferSummary` overloads (lines ~242 and ~349). Nothing else.
   - Java calls into the new Kotlin code: put the helpers in an `internal object` whose **members are
     not marked `internal`** (an `internal` member gets a mangled JVM name such as `foo$sdk`, which
     Java cannot call cleanly). Use `@JvmStatic` and a `const val` for the key, so Java calls
     `FraudSignalsExtractions.withoutFraudSignals(map)` and reads `FraudSignalsExtractions.FRAUD_SIGNALS_KEY`.
2. **UI.** None. No Compose, no XML, no previews in this ticket.
3. **Architecture.** capture-sdk Analysis screen is legacy Java MVP — integrate at its boundary, do
   not rewrite. New code is plain Kotlin: a use case (same shape as `GetCreditNoteHintEnabledUseCase`),
   a stateless parser, and immutable model types. No ViewModel, no state/intent/effect classes, no
   holder/store (decided: credit-note pattern; PP-3961 passes the parsed list to its screen).
4. **DI / async.** capture-sdk uses its isolated Koin context (`CaptureSdkIsolatedKoinContext`).
   Register `GetFraudSignalsEnabledUseCase` and `FraudSignalsParser` as `factory {}` in a new
   `internal val fraudSignalsModule` in `capture-sdk/sdk/src/main/java/net/gini/android/capture/di/fraudSignalsModule.kt`,
   and add it to the module list in `CaptureSdkIsolatedKoinContext.kt` next to `paymentHintsModule`.
   No Hilt, no global Koin. Parsing is synchronous and pure (no coroutines needed). No LiveData/RxJava.
5. **Strings/resources.** None. The API sends codes only; copy and localisation belong to PP-3961.
6. **Quality gates.** `ktlintCheck` and `detekt` clean on all touched modules; `apiCheck` passes after
   the two dump re-pins. Every new Kotlin class has a unit test; the new `fraudSignals` package
   should reach full line coverage in Jacoco (it is pure logic).

## Design

### 1. Flag plumbing (copy the `creditNoteHintEnabled` path, commit `ae80e6805`)

| File | Change |
|---|---|
| `bank-api-library/library/src/main/java/net/gini/android/bank/api/response/ConfigurationResponse.kt` | `@Json(name = "fraudSignalsEnabled") val fraudSignalsEnabled: Boolean? = null` (last param); `toConfiguration()` maps `isFraudSignalsEnabled = fraudSignalsEnabled ?: false`. |
| `bank-api-library/library/src/main/java/net/gini/android/bank/api/models/Configuration.kt` | `val isFraudSignalsEnabled: Boolean = false` (last param). |
| `capture-sdk/sdk/src/main/java/net/gini/android/capture/internal/network/Configuration.kt` | `val isFraudSignalsEnabled: Boolean = false` (last param). |
| `capture-sdk/default-network/src/main/java/net/gini/android/capture/network/GiniCaptureDefaultNetworkService.kt` (~line 215) | `isFraudSignalsEnabled = configuration.isFraudSignalsEnabled`. |
| `capture-sdk/sdk/src/main/java/net/gini/android/capture/internal/provider/GiniBankConfigurationProvider.kt` | Default config: `isFraudSignalsEnabled = false`. |
| `capture-sdk/sdk/src/main/java/net/gini/android/capture/internal/storage/ClientConfigurationStorage.kt` | New key `booleanPreferencesKey("is_fraud_signals_enabled")`; read with `prefs.flag(...)`, write in `saveConfiguration`. |

### 2. New package `net.gini.android.capture.fraudSignals` (`capture-sdk/sdk/src/main/java/net/gini/android/capture/fraudSignals/`)

```kotlin
internal class GetFraudSignalsEnabledUseCase(
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
) {
    operator fun invoke() = giniBankConfigurationProvider.provide().isFraudSignalsEnabled
}

internal enum class FraudSignalCode(val apiValue: String) {
    QR_PRINTED_IBAN_MISMATCH("QR_PRINTED_IBAN_MISMATCH"),
    IBAN_COUNTRY_MISMATCH("IBAN_COUNTRY_MISMATCH");
    companion object { fun fromApiValue(value: String?): FraudSignalCode? }   // null for unknown
}

internal enum class FraudSignalSeverity { STOP, CHECK, ESCALATOR, SILENT;
    companion object { fun fromApiValue(value: String?): FraudSignalSeverity? } // null for unknown
}

internal data class FraudSignal(
    val code: FraudSignalCode,
    val severity: FraudSignalSeverity,
    val iban: String,
    val creditorCountry: String?,   // required (non-blank) for IBAN_COUNTRY_MISMATCH
)

internal class FraudSignalsParser(
    private val getFraudSignalsEnabled: GetFraudSignalsEnabledUseCase,
) {
    fun parse(compoundExtractions: Map<String, GiniCaptureCompoundExtraction>): List<FraudSignal>
}

internal object FraudSignalsExtractions {
    const val FRAUD_SIGNALS_KEY = "fraudSignals"
    @JvmStatic
    fun withoutFraudSignals(
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
    ): Map<String, GiniCaptureCompoundExtraction>   // new map; input not mutated
}
```

Member labels read from each `Map<String, GiniCaptureSpecificExtraction>`: `signalCode`, `severity`,
`iban`, `creditorCountry` (use `.value`, trimmed). `parse` logic:

1. Flag off → `emptyList()` (R10).
2. `compoundExtractions[FRAUD_SIGNALS_KEY]?.specificExtractionMaps` null/empty → `emptyList()` (R9).
3. For each element: code = `FraudSignalCode.fromApiValue` → null ⇒ skip (R8); severity null ⇒ skip;
   iban blank ⇒ skip; `IBAN_COUNTRY_MISMATCH` with blank `creditorCountry` ⇒ skip (R11).
4. Keep the first element per code (`distinctBy { it.code }`, R12).

Names/labels are kept as constants in the parser; the parser must never throw for any input map.

Visibility note: `GetCreditNoteHintEnabledUseCase` is public today (it is in `sdk.api`), but the newer
`GetPaymentScheduleHintEnabledUseCase` was made `internal` (commit `c208dd969`). Follow the newer one.

### 3. Strip in `AnalysisScreenPresenter.java`

In the `SUCCESS_WITH_EXTRACTIONS` case (currently `AnalysisScreenPresenter.java:342-397`), before the
if/else chain, build a stripped holder once:

```java
final AnalysisInteractor.ResultHolder resultHolderWithoutFraudSignals =
        withoutFraudSignals(resultHolder);   // private helper: new ResultHolder(result, extractions,
                                             // FraudSignalsExtractions.withoutFraudSignals(compounds),
                                             // documentId, documentFileName)
```

and use it in **every** branch below (empty check, `isCxEmptyExtractions`, the four warning/hint
branches, `successResultHolder =`, `proceedWithExtractions`). The original `resultHolder` is kept
only for PP-3961, which will read signals from it with `FraudSignalsParser`. In this ticket the
original is not read after the stripped one is built.

The existing `removeAmountToPay(...)` (`AnalysisScreenPresenter.java:756`) already builds a holder
with empty compound extractions; it now receives the stripped holder, which does not change its
behaviour.

`AnalysisInteractor` (`AnalysisInteractor.java:79-91`) is **not** changed: it must still deliver
`fraudSignals` to the presenter so PP-3961 can parse it.

### 4. Safety strip in feedback

`GiniCapture.java` — in both `sendTransferSummary` overloads, next to
`getCompoundExtractions().remove("skontoDiscounts")`, add
`.remove(FraudSignalsExtractions.FRAUD_SIGNALS_KEY)`. With step 3 in place, `fraudSignals` can no
longer reach `internal().getCompoundExtractions()` through Return Assistant
(`DigitalInvoiceScreenPresenter.kt:187`), but this keeps feedback safe whatever the source.
The CX branch of `sendTransferSummary(Map)` builds its own compound map and needs no change.

## Test plan

Test stack per module, matching neighbours: JUnit4 + Google Truth everywhere; MockK in new capture-sdk
tests and `GiniCaptureSendTransferSummaryTest`; `AnalysisScreenPresenterTest` mixes Mockito-Kotlin and
MockK under Robolectric (`AndroidJUnit4`) — follow its existing helpers. Every new class gets a test.

| # | Test class | New / extend | Tests | Covers |
|---|---|---|---|---|
| 1 | `bank-api-library/library/src/test/java/net/gini/android/bank/api/response/ConfigurationResponseTest.kt` | extend | 2: `fraudSignalsEnabled true is mapped`, `missing fraudSignalsEnabled maps to false` | R1, R2 |
| 2 | `bank-api-library/library/src/test/java/net/gini/android/bank/api/BankApiWireTest.kt` | extend | 0 new; add `"fraudSignalsEnabled": true` to the full-config JSON fixture and assert `isFraudSignalsEnabled` | R1 (wire level) |
| 3 | `capture-sdk/default-network/src/test/java/net/gini/android/capture/network/GiniCaptureDefaultNetworkServiceTest.kt` | extend | 0 new; extend `getConfiguration returns success and maps configuration` with `isFraudSignalsEnabled = true` | R1 |
| 4 | `capture-sdk/sdk/src/test/java/net/gini/android/capture/internal/storage/ClientConfigurationStorageTest.kt` | **new** (Robolectric) | 2: `fraud signals flag round-trips through storage`, `missing key reads as false` | R3 |
| 5 | `capture-sdk/sdk/src/test/java/net/gini/android/capture/fraudSignals/GetFraudSignalsEnabledUseCaseTest.kt` | **new** | 2: default false, returns provider flag | R4 |
| 6 | `capture-sdk/sdk/src/test/java/net/gini/android/capture/fraudSignals/FraudSignalsParserTest.kt` | **new** | 11 (see below) | R5–R13 |
| 7 | `capture-sdk/sdk/src/test/java/net/gini/android/capture/fraudSignals/FraudSignalsExtractionsTest.kt` | **new** | 3: removes key and keeps others, no key → equal map, input not mutated | R14 support |
| 8 | `capture-sdk/sdk/src/test/java/net/gini/android/capture/analysis/AnalysisScreenPresenterTest.kt` | extend | 4: forwarded compounds lack `fraudSignals` but keep `lineItems` (R14); flag off → still stripped, same branch (R14, R18); credit note branch stores holder without `fraudSignals` (R15); only `fraudSignals` + empty specific extractions → no-extractions (R16) | R14–R16, R18 |
| 9 | `capture-sdk/sdk/src/test/java/net/gini/android/capture/GiniCaptureSendTransferSummaryTest.kt` | extend | 2: positional overload drops `fraudSignals`; SEPA `Map` overload drops `fraudSignals` (capture `sendFeedback` compound slot) | R17 |

`FraudSignalsParserTest` (11 — justified: one test per ticket AC case plus each malformed shape;
all are pure, fast tests):

1. QR mismatch only → one signal with the given IBAN and `creditorCountry = null` (R5)
2. Country mismatch only → one signal with the given IBAN and country (R6)
3. Both → two signals, response order (R7)
4. Unknown `signalCode` next to a valid one → only the valid one (R8)
5. No `fraudSignals` key → empty (R9)
6. Empty `specificExtractionMaps` → empty (R9)
7. Flag off with valid data → empty (R10)
8. Missing `iban` → that element dropped, other kept (R11)
9. Unknown / missing `severity` → dropped (R11)
10. Country mismatch without `creditorCountry` → dropped (R11)
11. Duplicate code → first one kept (R12); QR element with `creditorCountry` keeps it (R13) — two
    assertions on one fixture

Values in every fixture must differ per test (different IBANs/countries) so a hardcoded return value
cannot pass.

Commands: `./gradlew bank-api-library:library:testDebugUnitTest capture-sdk:sdk:testDebugUnitTest
capture-sdk:default-network:testDebugUnitTest bank-sdk:sdk:testDebugUnitTest`, then lint / detekt /
ktlintCheck / apiCheck on the same modules (or `/gini-check`).

### Not tested

- Moshi codegen itself and the generic `CompoundExtractionsMapper` (third-party / already covered);
  R1 is checked at the mapping and wire level instead.
- Koin module wiring of `fraudSignalsModule` — trivial `factory {}` declarations; it is exercised
  when PP-3961 injects the parser.
- End-to-end with a real backend: PP-3951/PP-3952 are not live yet. Manual QA with the IBAN-mismatch
  test documents from the epic once the backend ships (country-mismatch documents are still TBA).
- bank-sdk: no new tests; its existing `CaptureFlowFragment` / Return Assistant tests must stay green.

## Out of scope

- Any UI: warning screen, combined screen, IBAN selection sheet (PP-3961, PP-3966).
- The `shouldShowFraudWarning` branch in `AnalysisScreenPresenter` and passing signals to a screen (PP-3961).
- Reading `candidates.ibans` / "printed IBANs" and `paymentRecipient` for the screen (PP-3961).
- Analytics events for warnings (later ticket).
- An integrator-side `fraudSignalsEnabled` toggle in bank-sdk `CaptureConfiguration` / `GiniCapture.Builder`.
- Example-app toggles, public docs (PP-3968), UI automation (PP-3970).
- iOS (PP-3959).
- Changes to `AnalysisInteractor`, `CompoundExtractionsMapper`, or `CxExtractionsFilter`.

## Open questions

- **CX mode:** this spec strips `fraudSignals` in every `ProductTag` but says nothing about showing a
  warning in CX mode. Whether PP-3961 skips the warning in CX mode (like the already-paid and due-date
  hints do) is PP-3961's decision. Not blocking for this ticket.
- **Backend dependencies:** PP-3951 is In Review and PP-3952 is in Backlog. The field names here
  (`fraudSignalsEnabled`, `fraudSignals`, `signalCode`, `severity`, `iban`, `creditorCountry`) come
  from the ticket ACs and the API proposal page (status DRAFT). If the backend changes a name, update
  the constants and fixtures.

## Implementation plan
- [x] 1. `bank-api-library:library`: `fraudSignalsEnabled` in `ConfigurationResponse` + `Configuration`; extend `ConfigurationResponseTest`, `BankApiWireTest` (R1, R2)
- [x] 2. `capture-sdk:sdk` + `capture-sdk:default-network`: `isFraudSignalsEnabled` in internal `Configuration`, `GiniBankConfigurationProvider`, `ClientConfigurationStorage`, network mapping; extend `GiniCaptureDefaultNetworkServiceTest`, new `ClientConfigurationStorageTest` (R1, R3)
- [x] 3. `capture-sdk:sdk` new `fraudSignals` package: `GetFraudSignalsEnabledUseCase`, `FraudSignalCode`, `FraudSignalSeverity`, `FraudSignal`, `FraudSignalsParser`, `FraudSignalsExtractions`, `fraudSignalsModule`; new tests for each (R4–R13)
- [x] 4. `AnalysisScreenPresenter.java`: strip `fraudSignals` before the success branches; extend `AnalysisScreenPresenterTest` (R14–R16, R18)
- [x] 5. `GiniCapture.java`: remove `fraudSignals` in both `sendTransferSummary` overloads; extend `GiniCaptureSendTransferSummaryTest` (R17)
- [x] 6. Re-pin API dumps (`bank-api-library:library`, `capture-sdk:sdk`) and run `/gini-check`

## Build notes
- R15 is tested on the already-paid, schedule-payment and payment-due-hint branches, not the credit
  note branch: `removeAmountToPay` already drops all compound extractions, so a credit-note test
  would pass without the strip.
- `FraudSignalsExtractions.withoutFraudSignals` returns a mutable `HashMap` (not Kotlin's `minus`),
  so integrators still get a mutable map in `AnalysisFragmentListener.onExtractionsAvailable`.
- `ClientConfigurationStorage`'s file-level `dataStore` is now `internal` + `@VisibleForTesting`, so
  `ClientConfigurationStorageTest` can simulate a cache written by an older SDK (missing key).
- `AnalysisScreenPresenter.java` (Design §3) is done with the smallest diff: the analysis
  callback parameter is renamed to `analysisResult` and its first line shadows it with
  `resultHolder = withoutFraudSignals(analysisResult)`, so no branch line changes. PP-3961 reads
  the signals from `analysisResult`.
- Confirmed after build (2026-11-02): a signal with an unknown `severity` stays dropped (R11), even
  though v1 does not use `severity`. Trade-off accepted: a new backend severity hides that warning
  until the SDK knows the value.

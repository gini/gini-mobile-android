# PP-3916: [Android] UI Automation for Xray Skonto + RA Test Suite

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-3916
Epic: https://ginis.atlassian.net/browse/PP-2671
iOS twin: https://ginis.atlassian.net/browse/PP-3917 (Backlog)
Base branch: `release/bank-sdk-4.10.0`
Manual test basis: Xray folder `/Banking Project/QAlity imported test cases/Skonto + RA`
(9 Test issues, PP-2807 – PP-2815; read with the Xray GraphQL API on 2026-11-25, folder filter,
no descendants)

## Problem

The PM wants the 9 manual cases in the Xray folder "Skonto + RA" covered by UI tests that run on
BrowserStack. These cases check the Return Assistant (RA) screen when the invoice also has a Skonto
discount: the Skonto row, its edge-case texts, the Skonto screen opened from RA, and the two
configuration switches.

None of the 9 cases had a running test. `DigitalInvoiceSkontoTests` (smoke PP-3428) was parked
because no repository document extracted `lineItems` and `skontoDiscounts` together.

Measured during the build (2026-11-25, Galaxy S22, real API): **the API leaves out a Skonto whose
deadline has passed.** The OTTO invoice `Testrechnung-RA-Skonto.pdf` (invoice date 15.08.2024)
returns line items only; the same page with its invoice date moved to 15.08.2028 returns line
items **and** Skonto. That copy is the real-backend fixture `ra_skonto_valid_2028.pdf`.

Two clean generated invoices the user supplied (RA-SKO-202611-001, 3 and 5 positions) returned
Skonto only — no line items, and the "Rechnungsbetrag nach Skonto" line read as the full amount —
both as the original file and as a 2028 copy, so they were not used.

Decisions:

- **Hybrid.** Real backend (user's request) with the OTTO 2028 fixture for PP-2807, 2808, 2809,
  2814, 2815. Mock backend (`UiTestMockBackend`) for the date edge cases PP-2810 – 2813: "expires
  today" and "cash today" need a due date that is today on every run, which no committed document
  can print.
- **Android vs. Xray mismatches: tests assert what Android does today** (table below). No SDK
  change. Not done, outside this ticket: a bug for the missing `PayByCashToday` text, and product's
  answer on the cash-only switch default.
- **PP-2809:** `DigitalInvoiceSkontoFragment` has no Compose test tags; the page object matches
  text / content descriptions resolved from the SDK's `R.string`. No SDK change.
- **BrowserStack:** new group `bs_run_group_skontora.sh` (both classes), added to
  `bs_run_all_groups.sh`.
- **Not in scope:** un-parking `DigitalInvoiceSkontoTests` (PP-3428), an iOS mapping table.

## Android vs. Xray mismatches

Read from code: `GetSkontoEdgeCaseUseCase`, `GetSkontoDefaultSelectionStateUseCase`,
`SkontoInfoBannerTextFactory`, `LineItemsAdapter.SkontoViewHolder`.

| Xray | Xray expects | Android today | Test asserts |
|---|---|---|---|
| PP-2810 cash only | switch enabled, Edit enabled | switch starts **off** (`PayByCashOnly` → default off), Edit **disabled**, text `…_pay_cash_message` | Android behaviour |
| PP-2811 expires today | switch enabled, Edit enabled, "Pay today…" | switch on, Edit enabled, `…_pay_today_message` | matches Xray |
| PP-2812 expired | switch disabled | switch **off but still tappable**, `…_date_expired_message`, Edit disabled | Android behaviour |
| PP-2813 cash today | "Pay in cash today…", switch/Edit enabled | switch **off**, Edit **disabled**, row shows the **normal** text: `SkontoInfoBannerTextFactory` has no `PayByCashToday` branch (`…_pay_cash_today_message` is unused) | Android behaviour |

## Case → test mapping

| Xray | Case | Backend | Test |
|---|---|---|---|
| PP-2807 | RA screen, active Skonto | real | `SkontoReturnAssistantTests#test1_activeSkontoRowOnReturnAssistantScreen` |
| PP-2808 | RA screen, Skonto switched off | real | `SkontoReturnAssistantTests#test2_disabledSkontoRowAfterSwitchingOff` |
| PP-2809 | Skonto screen opened from RA | real | `SkontoReturnAssistantTests#test3_skontoScreenOpenedFromReturnAssistant` |
| PP-2814 | Config: Skonto off | real | `SkontoReturnAssistantTests#test4_skontoOffHidesSkontoOnReturnAssistantScreen` |
| PP-2815 | Config: Skonto and RA off | real | `SkontoReturnAssistantTests#test5_skontoAndReturnAssistantOffOpensExtractionScreen` |
| PP-2810 | Edge case: cash only | mock | `SkontoReturnAssistantMockBackendTests#test1_cashOnlySkonto` |
| PP-2811 | Edge case: expires today | mock | `SkontoReturnAssistantMockBackendTests#test2_skontoExpiresToday` |
| PP-2812 | Edge case: expired | mock | `SkontoReturnAssistantMockBackendTests#test3_expiredSkonto` |
| PP-2813 | Edge case: cash, expires today | mock | `SkontoReturnAssistantMockBackendTests#test4_cashOnlySkontoExpiringToday` |

## Requirements

Real fixture values (`SkontoFixtures.RA_VALID_2028_*`): amount to pay 105.20 EUR, after Skonto
102.04 EUR, 3 %, saved 3.16 EUR; remaining days come from the API. Mock values
(`UiTestMockSkontoData`, `UiTestMockFraudData.SKONTO_*`): two line items 30.00 + 12.00 EUR,
amount 42.00 EUR, 3 %, after Skonto 40.74 EUR.

**Entry**

- R1 (MUST, entry): Given Return Assistant and Skonto on, when a test imports
  `ra_skonto_valid_2028.pdf` against the real API and dismisses the RA onboarding, then the RA
  screen shows with a Skonto row.
- R2 (MUST, entry): Given the mock armed with any `SKONTO_RETURN_ASSISTANT_*` scenario, when
  `analyze` runs, then the result has `lineItems` that pass `LineItemsValidator` and
  `skontoDiscounts` that `SkontoDataExtractor` parses, with the due date computed from today.
- R3 (MUST, entry): after every test `UiTestMockBackend.disarm()` runs (both base classes'
  `tearDown`).

**Happy path (real backend)**

- R4 (MUST) — PP-2807: switch checked; `gbs_skonto_amount` = "-" + 3.16; Edit enabled; footer
  `skonto_saved_amount` contains the `gbs_skonto_section_footer_label_save` text and 3.16;
  `skonto_discount_label` = `gbs_skonto_section_footer_label_discount` with "3%".
- R5 (MUST) — PP-2808: after tapping the switch: unchecked; saved amount gone; `gbs_message`
  matches `gbs_skonto_section_discount_info_banner_normal_message` with "3%" and any days; Edit
  shown but disabled; both footer views gone.
- R6 (MUST) — PP-2809: Edit opens the RA Skonto screen with back and help (content descriptions),
  title, invoice preview row (title + "tap to view"), discount title, active hint, the normal
  info text with "3%", and a final amount of 102.04.
- R8 (MUST) — PP-2814: Skonto off: RA screen without a Skonto row or footer Skonto texts, in
  portrait and after `setOrientationLandscape()`.
- R9 (MUST) — PP-2815: RA and Skonto off: extraction screen, neither RA nor RA Skonto screen, in
  portrait and after rotating to landscape (rotating on the result screen; routing in
  `CaptureFlowFragment` does not depend on orientation). The payment hints are turned off for this case: without RA
  and Skonto the fixture's due date shows the "Your invoice is due on …" sheet, which blocks the way
  to the extraction screen.

**Edge cases (mock, Android behaviour)**

- R10 (MUST) — PP-2810, `SKONTO_RETURN_ASSISTANT_CASH_ONLY` (due today + 14, Cash): switch off,
  `…_pay_cash_message` with "3%" and 14 days, Edit disabled.
- R7 (MUST) — PP-2811, `SKONTO_RETURN_ASSISTANT_EXPIRES_TODAY` (due today): switch on,
  `…_pay_today_message` with "3%", Edit enabled.
- R11 (MUST) — PP-2812, `SKONTO_RETURN_ASSISTANT_EXPIRED` (due yesterday): switch off,
  `…_date_expired_message` with "3%", Edit disabled.
- R12 (MUST) — PP-2813, `SKONTO_RETURN_ASSISTANT_CASH_TODAY` (due today, Cash): switch off,
  `…_normal_message` for 0 days and "3%", Edit disabled. KDoc says to switch to
  `…_pay_cash_today_message` when the SDK is fixed.

**Error path**

- R13 (MUST): when the RA screen does not appear, the failure names the likely cause (no
  `lineItems` → Skonto screen showing; RA without Skonto row → no `skontoDiscounts`) and dumps the
  visible text.
- R14 (MUST): the mock edge-case tests skip within 30 minutes of local midnight
  (`assumeNotCloseToMidnight`).
- R17 (MUST): when the file picker stays open after tapping a freshly copied PDF, `PdfUploader`
  taps once more (seen on the first run after each new fixture).

**Async**

- R15 (MUST): waits use UiAutomator `Until` / idling resources, never `Thread.sleep`.

**BrowserStack**

- R16 (MUST): `bs_run_group_skontora.sh` runs both classes; `bs_run_all_groups.sh` has a
  `skontora` group. The PDF travels in the test APK and is copied to Downloads, like
  `skonto_past.pdf`.

## Affected modules

`bank-sdk:example-app` only — `src/main/.../uitestsupport` (mock scenarios) and `src/androidTest`
(tests, page objects, fixture, scripts). `bank-sdk:sdk` is read only.

## Public API impact

None. The example app is not published and has no `api/*.api` dump.

## Technical conventions

1. Kotlin, matching the neighbouring test and page-object files. No Java.
2. No UI added; tests use existing view ids on the RA screen and text on the RA Skonto screen.
3. Page objects in `ui/screens`; real-backend tests extend `SmokeJourneyTestBase`, mock tests
   extend `WarningBottomSheetTestBase`.
4. No DI; no new async. The mock dates use `Calendar` (no `java.time` desugaring in the app).
5. No new strings. Expected texts come from `net.gini.android.bank.sdk.R` via `SkontoTexts`,
   which copies `SkontoDiscountPercentageFormatter` / `SkontoRemainingDaysFormatter` (internal).
6. ktlint + detekt clean for `bank-sdk:example-app`.

## Design

- **Fixture** `src/androidTest/assets/ra_skonto_valid_2028.pdf`: the OTTO page rendered with
  PDFKit (scan + typed overlays), the invoice-date box painted white and "15.08.2028" typed in
  Helvetica, saved as a JPEG PDF. Documented in `SkontoFixtures` (`RA_VALID_2028_*`), with a
  refresh deadline before mid-2028.
- **Mock** (`UiTestMockBackend.kt`, `UiTestMockNetworkService.kt`): four scenarios
  `SKONTO_RETURN_ASSISTANT_{EXPIRES_TODAY,EXPIRED,CASH_ONLY,CASH_TODAY}`; the existing
  `skontoDiscounts()` gains `dueDateOffsetDays`, `remainingDays`, `paymentMethod` (defaults keep
  the fraud scenarios' payload); two-item `lineItems`; public `UiTestMockSkontoData`; exhaustive
  `isCreditNote` / `isFraudScenario`.
- **Page objects**: `DigitalInvoiceScreen` finds the Skonto row as the `gsb_line_item` row without
  `gbs_description` (the old `gbs_skonto_amount` marker is GONE while Skonto is off) and gains row,
  Edit and footer readers. New `ReturnAssistantSkontoScreen` (text / description matching).
- **Helper** `ui/resources/SkontoTexts.kt`: expected texts, incl. a regex for the normal message
  when the days come from the API.
- **Picker**: `PdfUploader.clickFile` retries the tap once when the picker stays open.
- **BrowserStack**: `bs_run_group_skontora.sh`, `bs_run_all_groups.sh`, `bs_build_and_upload.md`.

## Test plan

Instrumented UI tests (JUnit4, Espresso + UiAutomator), run on a device:

| Class | Base | Tests |
|---|---|---|
| `ui/testcases/SkontoReturnAssistantTests` (new) | `SmokeJourneyTestBase`, real API | 5 |
| `ui/testcases/SkontoReturnAssistantMockBackendTests` (new) | `WarningBottomSheetTestBase`, mock | 4 |

```bash
./gradlew bank-sdk:example-app:connectedDevExampleAppDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.gini.android.bank.sdk.exampleapp.ui.testcases.SkontoReturnAssistantTests
```

One class per run (the Test Orchestrator starts 0 tests for two comma-separated classes).

### Not tested

- Colours ("green text", "gray text").
- The invoice preview on the RA Skonto screen (opened, not inspected).
- Xray's expected behaviour for PP-2810, 2812, 2813 (see the mismatch table).
- Landscape for PP-2807 – 2813 (not asked by Xray).

## Out of scope

Any `bank-sdk:sdk` change; un-parking `DigitalInvoiceSkontoTests`; an iOS mapping table; editing
the Xray cases.

## Open questions

None blocking. Follow-ups outside this ticket: the `PayByCashToday` text bug, and product's answer
on the cash-only switch default.

## Implementation plan
- [x] 1. Mock: four `SKONTO_RETURN_ASSISTANT_*` edge-case scenarios, parameterised `skontoDiscounts(...)` (R2)
- [x] 2. `DigitalInvoiceScreen`: Skonto row lookup without `gbs_skonto_amount`; row / footer readers, Edit click (R4, R5, R10–R12)
- [x] 3. `ReturnAssistantSkontoScreen` page object and `SkontoTexts` helper (R6)
- [x] 4. Real-backend fixture `ra_skonto_valid_2028.pdf` + `SkontoFixtures` entries; `PdfUploader` tap retry (R1, R17)
- [x] 5. `bs_run_group_skontora.sh`, `bs_run_all_groups.sh`, `bs_build_and_upload.md` (R16)
- [x] 6. `SkontoReturnAssistantTests` (5, real) and `SkontoReturnAssistantMockBackendTests` (4, mock) green on a device (R1, R3–R15)

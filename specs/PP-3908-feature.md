# PP-3908: [Android] Add Wero to supported formats on the No results screen

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-3908
Parent epic: PP-2666 "[Q4] Wero qrcode support"
Depends on: PP-3857 (branch `feat/PP-3857-wero-qr-support`, not merged to `main`) — it adds
the `weroQRCodeScanningEnabled` flag and stores it in `GiniBankConfigurationProvider`.
iOS twin: PP-3907 (gini-mobile-ios PR #1299, branch `PP-3907`).
Figma: https://www.figma.com/design/ZYpdKfpaHOpV7RV1TTWdEH/Gini-Photo-Payment--iOS---Android?node-id=19600-79037
(frame "8.2.4 android-ph-QRError". It shows the row style only. It has no Wero row. See R1 for the position.)

## Problem

After PP-3857, the bank SDK can scan Wero QR codes when the client configuration flag
`weroQRCodeScanningEnabled` is on. But when a QR scan gives no payment data, the No results
screen shows a "Supported formats" list without Wero. A user who just scanned a Wero code
thinks Wero is not supported.

When the flag is on, the list must show "Wero QR code". When the flag is off or the backend
does not send it, the list must stay as it is today.

## Context from the Wero concept docs

Sources:
- [Wero QR Code Support in the Mobile SDK — Concept](https://ginis.atlassian.net/wiki/spaces/bank/pages/1878327497/Wero+QR+Code+Support+in+the+Mobile+SDK+Concept)
- [Wero QR codes – how the Gini Bank SDK returns the data](https://ginis.atlassian.net/wiki/spaces/bank/pages/2055897207/Wero+QR+codes+how+the+Gini+Bank+SDK+returns+the+data)

Facts this ticket relies on:
- Wero has **two** switches. Both are off by default and both are controlled by Gini: the client
  flag `weroQRCodeScanningEnabled` in the `/configurations` response, and a server-side flag
  per customer. The SDK can only see the client flag. This ticket follows the AC and gates the
  row on the client flag only.
- Integrators configure nothing and get no new SDK interface. A confirmed Wero code arrives
  as the single extraction `weroPaymentQRCodeUrl` in the existing result callback (the EPS
  pattern). This ticket does not touch that path (PP-3857).
- A QR that the SDK does not recognise and the backend does not confirm as Wero shows the
  **unsupported-QR popup** on the camera screen. It never shows the No results screen
  (`WeroExtractionsMapper.shouldRouteBackToCameraForWeroFallback` →
  `showUnsupportedQRCodePopup`, in `CameraFragmentImpl.java`).
- So the No results screen with the QR list appears when a recognised QR document gets **no
  extractions** back (`mappedExtractions.isEmpty()` → `NoResultsFragment.navigateToNoResultsFragment`
  in `CameraFragmentImpl.java`). This is the ticket's scenario: "scans a supported QR code,
  but no payment information is found".

## Requirements

Today's QR list (`SupportedFormatsAdapter`, `isQrCodeDocument = true`) is, in order:
header "Supported formats", then BezahlCode, eps Überweisung, Stuzza (AT), GiroCode (DE),
QR by Gini, Swiss QR Code (CH), SPD (CZ), Pay by Square (SK), UPN QR (SI), HUB3 (HR). That
is 1 header + 10 formats = 11 items.

- **R1 (MUST, happy).** Given `isWeroQRCodeScanningEnabled = true`, when a
  `SupportedFormatsAdapter` is created for a QR document, then it holds 12 items: today's
  11 items in the same order, plus a last item with label
  `R.string.gc_supported_format_qr_type_wero` ("Wero QR code"), placed after HUB3 (HR).
  (Position decided with the user: the same as the iOS implementation.)
- **R2 (MUST, error/flag off).** Given `isWeroQRCodeScanningEnabled = false`, when a
  `SupportedFormatsAdapter` is created for a QR document, then it holds exactly today's 11
  items in today's order, with no Wero label.
- **R3 (MUST, error/flag missing).** Given the backend response has no
  `weroQRCodeScanningEnabled` field (so `GiniBankConfigurationProvider` keeps its default
  `Configuration`), when `GetWeroQRCodeScanningEnabledUseCase()` is called, then it returns
  `false`. So the No results list is the same as R2.
  (`ConfigurationResponse` already maps a missing field to `false` on the PP-3857 branch.
  `ConfigurationResponseTest` there covers that.)
- **R4 (MUST, entry).** Given a bank SDK integrator whose client configuration has
  `weroQRCodeScanningEnabled = true`, when a scanned QR document reaches the No results
  screen (`NoResultsFragmentImpl.setUpList`, `default` branch → `new SupportedFormatsAdapter(true)`),
  then the list shows "Wero QR code" as its last row. The integrator calls nothing new. The
  backend flag is the only switch.
- **R5 (MUST, non-QR paths unchanged).** Given any flag value, when a
  `SupportedFormatsAdapter` is created with `isQrCodeDocument = false` (No results for PDFs,
  and the Help › Supported formats screen `SupportedFormatsHelpFragment`), then its items do
  not contain the Wero label.
- **R6 (MUST, design).** The Wero row uses the same layout (`gc_item_format_info`), the same
  icon (`R.drawable.gc_format_info_supported_icon`) and the same text style as the other
  QR rows. When it is the last item, its divider is `INVISIBLE`. The existing
  `position == mItems.size() - 1` rule already does this. HUB3 (HR) then shows its
  divider `VISIBLE`.
- **R7 (MUST, strings).** `gc_supported_format_qr_type_wero` exists only in
  `capture-sdk/sdk/src/main/res/values/strings.xml` with `translatable="false"` and the value
  `Wero QR code`, next to the other `gc_supported_format_qr_type_*` brand labels. These
  labels exist only in `values/` today. iOS uses the same text in both `en` and `de`.

There is no async requirement. The flag is read once, at the moment the adapter is created.

## Affected modules

- `capture-sdk:sdk` — the only module with code changes.
- `bank-sdk:sdk` — gets the change through its dependency on `capture-sdk:sdk`. No code change.
- `bank-api-library:library`, `capture-sdk:default-network` — already carry the flag on the
  PP-3857 branch. No change.

Branch: create `PP-3908-wero-no-results` from `origin/feat/PP-3857-wero-qr-support`.
Open the PR **into `feat/PP-3857-wero-qr-support`**, not into `main` (user's choice; same as
iOS PP-3907).

## Public API impact

`capture-sdk:sdk` — **additive** (user chose to follow the eInvoice precedent):

- New class `net.gini.android.capture.wero.GetWeroQRCodeScanningEnabledUseCase` — public
  in bytecode, KDoc "Internal use only." Same shape as
  `net.gini.android.capture.einvoice.GetEInvoiceFeatureEnabledUseCase` (public constructor
  that takes `GiniBankConfigurationProvider`, plus `operator fun invoke(): Boolean`).
- New `@JvmStatic fun getWeroQRCodeScanningEnabledUseCase(): GetWeroQRCodeScanningEnabledUseCase`
  on the public `net.gini.android.capture.di.CaptureSdkJavaInterop` object.
- `SupportedFormatsAdapter` (public, `@suppress`): the public constructor
  `SupportedFormatsAdapter(Boolean)` stays as it is. The new constructor and the test helper
  are package-private, so they are not in the dump.

`capture-sdk/sdk/api/sdk.api` must be re-pinned with
`./gradlew capture-sdk:sdk:apiDump`. Otherwise `apiCheck` fails. Check that the diff shows only
the two additions above.

All other modules: none.

## Technical conventions

1. **Language.** New code in Kotlin under `src/main/java/`: the use case and the interop
   accessor. Legacy Java files that may be touched, and why:
   - `capture-sdk/sdk/src/main/java/net/gini/android/capture/help/SupportedFormatsAdapter.java`
     — this is where the QR list is built. Keep it in Java and do not convert it.
   - No other Java file. `NoResultsFragmentImpl.java` stays unchanged. It keeps calling
     `new SupportedFormatsAdapter(true)`.
   The use case is public only because Java must reach it through `CaptureSdkJavaInterop`
   (the eInvoice precedent). Mark it "Internal use only." in KDoc.
2. **UI.** No Compose, no new screens. No XML layouts are added or removed. The row reuses
   `gc_item_format_info`. No new drawable.
3. **Architecture.** The No results screen is legacy Java (Fragment + `NoResultsFragmentImpl`)
   with a `RecyclerView.Adapter`. It has no ViewModel, so no state, intent or effect classes
   are added. The flag is read through a use case, the same as
   `GetEInvoiceFeatureEnabledUseCase` and `GetPaymentScheduleHintEnabledUseCase`.
4. **DI and async.** Koin, isolated context. Add a new `capture-sdk/sdk/src/main/java/net/gini/android/capture/di/WeroModule.kt`
   (`internal val weroModule = module { factory { GetWeroQRCodeScanningEnabledUseCase(giniBankConfigurationProvider = get()) } }`)
   and register it in `CaptureSdkIsolatedKoinContext.koinApplication { modules(...) }`. This
   is the same as `EInvoiceModule`. No coroutines and no Flow: the read is synchronous, like the eInvoice read. No
   LiveData or RxJava. Do **not** change `CameraFragmentExtension.isWeroQRCodeScanningEnabled()`
   from PP-3857. Leave it as it is.
5. **Strings/resources.** One new string in `values/strings.xml` only
   (`translatable="false"`, no placeholders). `values-en` gets nothing, because the other QR
   brand labels are not there either.
6. **Quality gates.** `./gradlew capture-sdk:sdk:testDebugUnitTest lint detekt ktlintCheck apiCheck`
   must pass (or use `/gini-check`). New Kotlin classes need full unit-test coverage for
   Jacoco/Sonar. The `SupportedFormatsAdapter` lines that change must be covered by the
   adapter test.

## Design

1. **Use case** — new `capture-sdk/sdk/src/main/java/net/gini/android/capture/wero/GetWeroQRCodeScanningEnabledUseCase.kt`:
   ```kotlin
   /** Internal use only. */
   class GetWeroQRCodeScanningEnabledUseCase(
       private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
   ) {
       operator fun invoke(): Boolean =
           giniBankConfigurationProvider.provide().isWeroQRCodeScanningEnabled
   }
   ```
   `Configuration.isWeroQRCodeScanningEnabled` (default `false`) exists in
   `capture-sdk/sdk/src/main/java/net/gini/android/capture/internal/network/Configuration.kt`
   on the PP-3857 branch.

2. **Koin** — new `di/WeroModule.kt` (see conventions §4). Register it in
   `di/CaptureSdkIsolatedKoinContext.kt`.

3. **Java interop** — `di/CaptureSdkJavaInterop.kt`: add
   ```kotlin
   @JvmStatic
   fun getWeroQRCodeScanningEnabledUseCase(): GetWeroQRCodeScanningEnabledUseCase =
       getGiniCaptureKoin().get()
   ```

4. **Adapter** — `help/SupportedFormatsAdapter.java`:
   - Add the enum constant `QR_WERO(R.string.gc_supported_format_qr_type_wero)` to
     `SupportedFormat`, after `QR_HUB3`.
   - The public constructor `SupportedFormatsAdapter(Boolean isQrCodeDocument)` delegates to a
     new package-private `@VisibleForTesting` constructor
     `SupportedFormatsAdapter(Boolean isQrCodeDocument, boolean isEInvoiceEnabled, boolean isWeroQRCodeScanningEnabled)`.
     It passes `CaptureSdkJavaInterop.getEInvoiceFeatureEnabledUseCase().invoke()` and
     `CaptureSdkJavaInterop.getWeroQRCodeScanningEnabledUseCase().invoke()`. Remove the
     `getEInvoiceFeatureEnabledUseCase` field, which is then unused.
     Store `isWeroQRCodeScanningEnabled` in a field.
   - In `setUpItems()`, QR branch: after `items.add(SupportedFormat.QR_HUB3);` add
     `if (isWeroQRCodeScanningEnabled) { items.add(SupportedFormat.QR_WERO); }`. The non-QR
     branch stays unchanged (R5).
   - Add a package-private `@VisibleForTesting List<Integer> getItemLabels()`. It returns each
     item's string resource in order: `SectionHeader.title` for headers and
     `FormatInfo.getLabel()` for format rows. Tests use it to check the list without
     inflating views.
   - `onBindViewHolder` divider logic: no change. The QR path never contains
     `PHOTOS_OF_MONITORS` or `E_INVOICES`, so only the `position == size - 1` rule applies.

5. **Data flow (unchanged from PP-3857):** backend `weroQRCodeScanningEnabled` →
   `ConfigurationResponse` → `bank.api.models.Configuration.isWeroQRCodeScanningEnabled` →
   `GiniCaptureDefaultNetworkService` → `capture.internal.network.Configuration` →
   `GiniBankConfigurationProvider.update` (via `GiniCaptureFragment`/`GiniCaptureViewModel`,
   and persisted in `ClientConfigurationStorage`) → new use case → adapter.

## Test plan

Stack: JUnit4 + Google Truth. The adapter test uses Robolectric (`@RunWith(AndroidJUnit4::class)`)
because `RecyclerView.Adapter` needs the Android runtime. This matches
`help/HelpFragmentTest.kt`. No mocks are needed: use the real `GiniBankConfigurationProvider`,
like `paymentHints/GetPaymentScheduleHintEnabledUseCaseTest.kt`. Every new class gets a unit test.

1. **New** `capture-sdk/sdk/src/test/java/net/gini/android/capture/wero/GetWeroQRCodeScanningEnabledUseCaseTest.kt`
   (plain JUnit + Truth, 2 tests):
   - `returns false by default` — default provider, which means the flag is missing → R3.
   - `returns the flag from the client configuration` — `update { it.copy(isWeroQRCodeScanningEnabled = true) }` → `true`.
2. **New** `capture-sdk/sdk/src/test/java/net/gini/android/capture/help/SupportedFormatsAdapterTest.kt`
   (Robolectric + Truth, 4 tests; no adapter test exists today). Use the package-private
   constructor and `getItemLabels()`:
   - `QR list ends with Wero when the flag is on` — the 12 labels in exact order, last
     `gc_supported_format_qr_type_wero` → R1.
   - `QR list is unchanged when the flag is off` — exactly today's 11 labels → R2/R3.
   - `non-QR list never contains Wero when the flag is on` — `isQrCodeDocument = false`,
     flag on → no Wero label → R5.
   - `Wero row is bound with the supported icon and hidden divider` — call
     `onCreateViewHolder` + `onBindViewHolder` for the last position (parent: a
     `FrameLayout` from the Robolectric context). Check the label text "Wero QR code", the
     icon resource, and `dividerView` `INVISIBLE` → R6/R7.
3. R4 (end-to-end from the backend flag to the No results screen) is covered by the chain
   above together with the PP-3857 tests (`ConfigurationResponseTest`). There is no new
   instrumented test (user's choice: unit tests only).

### Not tested

- Instrumented/Espresso test on `NoResultsFragment` with the flag on. This is left out on
  purpose (user's choice). iOS removed the same UI test because a Wero No-results state is
  hard to reach.
- The Koin registration of `weroModule` and the interop accessor. These are simple
  delegation. Running `NoResultsFragment` by hand in QA covers them.
- Visual match with Figma: manual QA. Use a bank-sdk example app variant with a client that
  has `weroQRCodeScanningEnabled` on. Scan a QR code without payment data and check that
  "Wero QR code" is the last row, in light and dark mode. Repeat with the flag off.

## Out of scope

- The Help › Supported formats screen (`SupportedFormatsHelpFragment`) and the non-QR list.
- Reordering, renaming or restyling the existing 10 QR rows.
- Changes to PP-3857 code (`CameraFragmentExtension`, `CameraFragmentImpl`,
  `WeroExtractionsMapper`, configuration mapping).
- Converting `SupportedFormatsAdapter` or `NoResultsFragmentImpl` to Kotlin or Compose.
- A localized (German/English) Wero label. It is a brand label, as on iOS.

## Open questions

- Product, not blocking: the SDK sees only the client flag. If Gini turns the client flag on
  but leaves the server-side flag off, the list says Wero is supported, but a Wero scan still
  ends in the unsupported-QR popup. The concept doc expects both flags to be switched on
  together for a customer. Confirm this with product if needed.
- None that block the build. Design has not drawn the Wero row in the Android Figma frame.
  The position (last) follows the iOS implementation, and the user confirmed it.

## Implementation plan
- [x] 1. `capture-sdk:sdk` — test + `wero/GetWeroQRCodeScanningEnabledUseCase.kt`, `di/WeroModule.kt`, register in `CaptureSdkIsolatedKoinContext` (R3)
- [x] 2. `capture-sdk:sdk` — `CaptureSdkJavaInterop.getWeroQRCodeScanningEnabledUseCase()` (R4)
- [x] 3. `capture-sdk:sdk` — `SupportedFormatsAdapterTest.kt`, then `QR_WERO` + string + package-private constructor + `getItemLabels()` in `SupportedFormatsAdapter.java` (R1, R2, R5, R6, R7)
- [x] 4. `capture-sdk:sdk` — re-pin `api/sdk.api` with `apiDump`, run `/gini-check`, self-review (all)

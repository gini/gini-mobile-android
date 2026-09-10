# SonarCloud clean-up plan (all Android SDK modules)

Status: in progress — module 1 of 7 (capture-sdk:sdk) implemented, awaiting review
Ticket: N/A (no Jira ticket; branch names follow the un-ticketed kebab-case convention)
Issue snapshot: SonarCloud `main`, fetched 2026-09-08 (590 open issues across 7 modules)
Last updated: 2026-09-10

## Goal

Resolve every open SonarCloud issue of the Gini Android SDKs, module by module,
with one reviewable commit per issue, so each module branch can be reviewed and
merged independently.

## Workflow (agreed with the user)

1. **Data source.** No SonarCloud MCP server is configured. The Sonar projects
   are public, so the REST API works anonymously:

   ```bash
   curl -s "https://sonarcloud.io/api/issues/search?componentKeys=<project-key>&resolved=false&ps=500&p=1"
   ```

   Project keys: `android-capture-sdk`, `android-capture-sdk-default-network`,
   `android-bank-sdk`, `android-health-sdk`, `android-internal-payment-sdk`,
   `android-core-api-library`, `android-bank-api-library`,
   `android-health-api-library` (organisation `gini`). The capture-sdk response
   contains a raw control character; parse it with `json.loads(..., strict=False)`.
   Re-fetch before starting a module: the snapshot below may have drifted.

2. **Severity tiers** use SonarCloud's impact severity: BLOCKER → Critical,
   HIGH → High, MEDIUM → Medium, LOW → Low, INFO → Info. Module ranking: most
   Critical first, High as tiebreaker, then total count.

3. **Branching.** The user creates each branch (`sonar-<project>-issues`) from
   the current release branch (capture-sdk was branched from
   `release/bank-sdk-4.6`, into which it will be merged). Never create or push
   branches or tags.

4. **Resolution phase per module (fully autonomous, no check-ins):**
   - Work Critical → High → Medium → Low → Info.
   - One issue per commit. "One issue" = one Sonar finding, or the same rule
     repeated in the same file (e.g. eight duplicated literals in one class).
     Never batch unrelated rules.
   - Commit format from `.git-stuff/commit-msg-template.txt`:
     `<type>(<project>): <Imperative subject>` + what/why body. `fix(...)` for
     BUG/VULNERABILITY rules, `refactor(...)` for code smells,
     `chore(...)` for dump/baseline re-pins. No ticket line, no co-author trailer.
   - Compile after every edit (`<module>:compileDebugJavaWithJavac`,
     `compileDebugKotlin`, test compile tasks), run the affected class's unit
     tests after every behaviour-relevant refactor, and run the full check
     (`testDebugUnitTest detekt ktlintCheck lint apiCheck`) at least after the
     High tier and at the end.
   - Any change to a public signature must run `./gradlew <module>:apiDump` and
     commit the updated `api/*.api` (binary-compatibility-validator). Kotlin
     default parameters are invisible to Java callers: add `@JvmOverloads`.
     Detekt baselines (`<module>/detekt-baseline.xml`) are keyed by signature —
     re-key entries when a suppressed function's signature changes.
   - Findings that cannot be fixed without breaking the public API, changing
     behaviour, or that are false positives are *not* forced: list them in the
     module summary for Sonar-side "won't fix"/"accept".
   - When the module is done: stop and hand the user a summary (fixed per tier,
     API dump changes to review, won't-fix list). Move on only when told to.

## Module ranking

| # | Module | Critical | High | Medium | Low | Info | Total | Branch | Status |
|---|---|---|---|---|---|---|---|---|---|
| 1 | capture-sdk:sdk | 10 | 36 | 254 | 203 | 11 | 514 | `sonar-capture-sdk-issues` | done 2026-09-09, awaiting review |
| 2 | internal-payment-sdk:sdk | 0 | 5 | 17 | 4 | 0 | 26 | `sonar-internal-payment-sdk-issues` | next |
| 3 | health-sdk:sdk | 0 | 4 | 23 | 1 | 0 | 28 | `sonar-health-sdk-issues` | pending |
| 4 | health-api-library:library | 0 | 1 | 0 | 0 | 1 | 2 | `sonar-health-api-library-issues` | pending |
| 5 | bank-sdk:sdk | 0 | 0 | 1 | 12 | 1 | 14 | `sonar-bank-sdk-issues` | pending |
| 6 | core-api-library:library | 0 | 0 | 0 | 0 | 4 | 4 | `sonar-core-api-library-issues` | pending |
| 7 | bank-api-library:library | 0 | 0 | 2 | 0 | 0 | 2 | `sonar-bank-api-library-issues` | pending |

`capture-sdk:default-network` has no open issues.

## Module 1: capture-sdk:sdk — implemented

Branch `sonar-capture-sdk-issues` (worktree `gini-worktree-2`), 173 local
commits on top of `eac671478`, not pushed. Final local check: 410 unit tests
pass, detekt/ktlint/lint/apiCheck pass, androidTest sources compile
(instrumented tests not run).

Fixed: 470 of 514 findings (Critical 10/10, High 34/36, Medium 226/254,
Low 199/203, Info 2/11).

Public API dump (`capture-sdk/sdk/api/sdk.api`) changed and re-pinned for:
`LastAnalyzedDocumentProvider.getData()` → `StateFlow`;
`MimeType.equals(String)` → `matches(String)`; `textStyleFromTextAppearance` /
`textStyleFromAttribute` without `setTextColors`; `BundleHelper.toMap/toMapList`
take the value class; `SupportedFormatsAdapter(boolean)`; `PdfDocument`
`equals/hashCode`; removed no-op `writeToParcel` overrides in the three
`*MultiPageDocument` classes; `CustomOffsetMapping` without the unused
`DecimalFormatSymbols` parameter. Reviewer decision needed on whether any of
these must wait for a major version.

Left for Sonar-side resolution (44):

- Public API / standard names: `Document.Type.QRCode` (S115); `EPC069_12Parser`,
  `ERROR_DETAILS_MAP_KEY`, `UPLOAD_ERROR_DETAILS_MAP_KEY` (S101); `ArrayList`
  return type/field needed for Bundle (S1319 ×2); private constructors on public
  holder classes (S1118 ×3); `GiniCapture` monster class (S6539 ×3);
  `isBottomNavigationBarEnabled` planned removal (S1133) and its 14 usages (S1874).
- Intentional design: lifecycle-mirroring unused params in `*FragmentImpl`
  (S1172 ×6); `NoResultsFragmentImpl.mCancelListener` kept per comment (S1068);
  parameter counts of public constructors and Compose functions (S107 ×14);
  method moves in `GiniCapture`/vendored `TouchImageView` (S3398 ×3); blocking
  `getInvoiceEducationType` getter (S6512); `ErrorLoggerListener` has a default
  body so it cannot be a `fun interface` (S6517); self-removing
  `OnLayoutChangeListener` needs `this` (S6516); TODO comments (S1135 ×5).
- False positives / platform limits: always-false null checks on
  `openFileDescriptor`/`openInputStream` (S2583 ×5); `ByteBuffer.get(byte[])`
  (S6518); `Files.delete` needs API 26, minSdk is 23 (S4042 ×3); deprecations
  that are API-level fallbacks or need new dependencies — CameraX
  `defaultDisplay`/`vibrate`, `BitmapRegionDecoder.newInstance`,
  `configuration.locale`, `ViewCompat.animate` ×4, `announceForAccessibility` ×2,
  `LocalLifecycleOwner` (needs lifecycle-runtime-compose),
  `rememberPlainTooltipPositionProvider` (S1874).
- Review-only hotspots: `GiniCaptureDebug` external files (S5324), `UriHelper`
  path from uri (S6549 ×3); `Thread.sleep` simulating upload latency in
  `NetworkRequestsManagerTest` (S2925 ×2); parameterized-test suggestion in
  `ExifReaderTest` (S5976).

## Remaining modules — issue lists (snapshot 2026-09-08)

Each line: `Sonar issue key`, rule, type, `path:line`, message. Re-fetch before
starting; line numbers are relative to `main` at snapshot time.

### 2. internal-payment-sdk:sdk — branch `sonar-internal-payment-sdk-issues`

Paths are relative to the module root. Fix order inside the module: Critical → High → Medium → Low → Info.

#### High (5)

- `AZueSA2ysUXPZ7i6TFuC` kotlin:S1192 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/paymentComponent/PaymentComponentView.kt:291 — Define a constant instead of duplicating this literal "Cannot call PaymentComponent's listener: " 3 times.
- `AZueSA2ysUXPZ7i6TFuD` kotlin:S1192 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/paymentComponent/PaymentComponentView.kt:292 — Define a constant instead of duplicating this literal "PaymentComponent must be set before showing the PaymentComponentView" 3 times.
- `AZueSA0AsUXPZ7i6TFt5` kotlin:S1192 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/utils/GiniPaymentManager.kt:66 — Define a constant instead of duplicating this literal "Cannot create PaymentRequest: No selected payment provider app" 3 times.
- `AZueSA2ysUXPZ7i6TFuE` kotlin:S3776 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/paymentComponent/PaymentComponentView.kt:88 — Refactor this method to reduce its Cognitive Complexity from 18 to the 15 allowed.
- `AZueSA0wsUXPZ7i6TFt7` kotlin:S3776 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/review/reviewComponent/ReviewView.kt:439 — Refactor this method to reduce its Cognitive Complexity from 21 to the 15 allowed.

#### Medium (17)

- `AZueSA37sUXPZ7i6TFuJ` kotlin:S1066 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/paymentProvider/PaymentProviderApp.kt:108 — Merge this "if" statement with the nested one.
- `AZueSA2XsUXPZ7i6TFt_` kotlin:S108 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/bankselection/BankSelectionBottomSheet.kt:128 — Either remove or fill this block of code.
- `AZueSA3hsUXPZ7i6TFuH` kotlin:S108 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/moreinformation/MoreInformationFragment.kt:112 — Either remove or fill this block of code.
- `AZueSA3hsUXPZ7i6TFuI` kotlin:S108 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/moreinformation/MoreInformationFragment.kt:113 — Either remove or fill this block of code.
- `AZueSA3JsUXPZ7i6TFuG` kotlin:S108 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/moreinformation/MoreInformationViewModel.kt:36 — Either remove or fill this block of code.
- `AZueSAzRsUXPZ7i6TFt1` kotlin:S6305 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/utils/FlowBottomSheetsManager.kt:20 — Don't expose mutable flow types.
- `AZueSAzRsUXPZ7i6TFt2` kotlin:S6305 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/utils/FlowBottomSheetsManager.kt:21 — Don't expose mutable flow types.
- `AZueSAzRsUXPZ7i6TFt3` kotlin:S6305 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/utils/FlowBottomSheetsManager.kt:22 — Don't expose mutable flow types.
- `AZueSAy5sUXPZ7i6TFtz` kotlin:S6309 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/utils/Extensions.kt:86 — Functions returning "Flow" or "Channel" should not be suspending
- `AZueSAzRsUXPZ7i6TFt0` kotlin:S6310 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/utils/FlowBottomSheetsManager.kt:35 — Avoid hardcoded dispatchers.
- `AZueSA1lsUXPZ7i6TFt9` kotlin:S6313 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/review/openWith/OpenWithViewModel.kt:27 — Classes extending "ViewModel" should not expose suspending functions.
- `AZueSA2XsUXPZ7i6TFuB` kotlin:S6517 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/bankselection/BankSelectionBottomSheet.kt:201 — Make this interface functional or replace it with a function type.
- `AZueSA2XsUXPZ7i6TFuA` kotlin:S6517 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/bankselection/BankSelectionBottomSheet.kt:243 — Make this interface functional or replace it with a function type.
- `AZueSA2ysUXPZ7i6TFuF` kotlin:S6517 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/paymentComponent/PaymentComponentView.kt:327 — Make this interface functional or replace it with a function type.
- `AZueSA1MsUXPZ7i6TFt8` kotlin:S6517 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/review/installApp/InstallAppBottomSheet.kt:40 — Make this interface functional or replace it with a function type.
- `AZueSA1-sUXPZ7i6TFt-` kotlin:S6517 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/review/openWith/OpenWithBottomSheet.kt:41 — Make this interface functional or replace it with a function type.
- `AZueSAzpsUXPZ7i6TFt4` kotlin:S6517 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/utils/BackListener.kt:6 — Make this interface functional or replace it with a function type.

#### Low (4)

- `AZ4HtzgC62hOFhXF6WHI` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/bankselection/BankSelectionBottomSheet.kt:198 — Deprecated code should not be used.
- `AZ4HtzeZ62hOFhXF6WHF` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/utils/GiniLocalizationInternal.kt:49 — Deprecated code should not be used.
- `AZ4HtzeZ62hOFhXF6WHG` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/utils/GiniLocalizationInternal.kt:50 — Deprecated code should not be used.
- `AZ4HtzeZ62hOFhXF6WHH` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/internal/payment/utils/GiniLocalizationInternal.kt:51 — Deprecated code should not be used.

### 3. health-sdk:sdk — branch `sonar-health-sdk-issues`

Paths are relative to the module root. Fix order inside the module: Critical → High → Medium → Low → Info.

#### High (4)

- `AZ5F3S1qBFZt9XQw2nk0` kotlin:S1192 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/GiniHealth.kt:224 — Define a constant instead of duplicating this literal "Request cancelled" 4 times.
- `AZ5F3S0xBFZt9XQw2nkq` kotlin:S3776 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFragment.kt:372 — Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.
- `AZ5F3S0xBFZt9XQw2nko` kotlin:S5322 [VULNERABILITY] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFragment.kt:247 — Make sure that intents are received safely here.
- `AZ5F3S0xBFZt9XQw2nkp` kotlin:S5324 [VULNERABILITY] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFragment.kt:243 — Make sure accessing the Android external storage is safe here.

#### Medium (23)

- `AZ5F3S1qBFZt9XQw2nkx` kotlin:S108 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/GiniHealth.kt:263 — Either remove or fill this block of code.
- `AZ5F3SuiBFZt9XQw2nkf` kotlin:S6305 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:53 — Don't expose mutable flow types.
- `AZ5F3SuiBFZt9XQw2nkg` kotlin:S6305 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:56 — Don't expose mutable flow types.
- `AZ5F3SuiBFZt9XQw2nkh` kotlin:S6305 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:67 — Don't expose mutable flow types.
- `AZ5F3S1qBFZt9XQw2nkt` kotlin:S6310 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/GiniHealth.kt:70 — Avoid hardcoded dispatchers.
- `AZ5F3S1qBFZt9XQw2nku` kotlin:S6310 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/GiniHealth.kt:74 — Avoid hardcoded dispatchers.
- `AZ5F3S1qBFZt9XQw2nkv` kotlin:S6311 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/GiniHealth.kt:70 — Remove this dispatcher. It is pointless when used with only suspending functions.
- `AZ5F3S1qBFZt9XQw2nkw` kotlin:S6311 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/GiniHealth.kt:74 — Remove this dispatcher. It is pointless when used with only suspending functions.
- `AZ5F3SuiBFZt9XQw2nkc` kotlin:S6313 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:179 — Classes extending "ViewModel" should not expose suspending functions.
- `AZ5F3SuiBFZt9XQw2nkd` kotlin:S6313 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:189 — Classes extending "ViewModel" should not expose suspending functions.
- `AZ5F3SuiBFZt9XQw2nke` kotlin:S6313 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:194 — Classes extending "ViewModel" should not expose suspending functions.
- `AZ5F3SuiBFZt9XQw2nka` kotlin:S6508 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:64 — Replace this usage of Void type with Unit.
- `AZ5F3SuiBFZt9XQw2nkb` kotlin:S6508 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:65 — Replace this usage of Void type with Unit.
- `AZ5F3SuiBFZt9XQw2nki` kotlin:S6518 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:38 — Replace function call with indexed accessor.
- `AZ5F3SuiBFZt9XQw2nkj` kotlin:S6518 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:39 — Replace function call with indexed accessor.
- `AZ5F3SuiBFZt9XQw2nkk` kotlin:S6518 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:42 — Replace function call with indexed accessor.
- `AZ5F3SuiBFZt9XQw2nkl` kotlin:S6518 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:43 — Replace function call with indexed accessor.
- `AZ5F3SuiBFZt9XQw2nkm` kotlin:S6518 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:46 — Replace function call with indexed accessor.
- `AZ5F3SuiBFZt9XQw2nkn` kotlin:S6518 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFlowViewModel.kt:47 — Replace function call with indexed accessor.
- `AZ5F3S1XBFZt9XQw2nks` kotlin:S6524 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/review/ReviewViewModel.kt:139 — Make this collection immutable.
- `AZ5F3S1qBFZt9XQw2nky` kotlin:S6532 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/GiniHealth.kt:529 — Replace this if expression with check(!(paymentDetails.iban.isEmpty() || paymentDetails.amount.isEmpty() || paymentDetails.purpose.isEmpty() || paymentDetails.recipient.isEmpty())) { "Payment details are incomplete" }.
- `AZ5F3S1qBFZt9XQw2nkz` kotlin:S6532 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/GiniHealth.kt:532 — Replace this if expression with check(isValidIban(paymentDetails.iban)) { "Iban is invalid" }.
- `AZ5F3S2oBFZt9XQw2nk2` kotlin:S6619 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/preferences/UserPreferences.kt:16 — Remove this useless null-safe access ?., it always succeeds.

#### Low (1)

- `AZ5F3S1LBFZt9XQw2nkr` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/health/sdk/review/ReviewFragment.kt:589 — Deprecated code should not be used.

### 4. health-api-library:library — branch `sonar-health-api-library-issues`

Paths are relative to the module root. Fix order inside the module: Critical → High → Medium → Low → Info.

#### High (1)

- `AZvW_rsoNpcYkdBhQ_PI` kotlin:S1192 [CODE_SMELL] src/main/java/net/gini/android/health/api/HealthApiDocumentRemoteSource.kt:37 — Define a constant instead of duplicating this literal "Empty response body" 6 times.

#### Info (1)

- `AZvW_rtKNpcYkdBhQ_PJ` kotlin:S1135 [CODE_SMELL] src/main/java/net/gini/android/health/api/HardcodedFilesLocalDataSource.kt:7 — Complete the task associated to this TODO comment.

### 5. bank-sdk:sdk — branch `sonar-bank-sdk-issues`

Paths are relative to the module root. Fix order inside the module: Critical → High → Medium → Low → Info.

#### Medium (1)

- `AZ-UJ16dryKXHJKl_sRi` kotlin:S6305 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/transactiondocs/internal/provider/document/AttachedToTransactionDocumentProviderImpl.kt:16 — Don't expose mutable flow types.

#### Low (12)

- `AZ-UJ11_ryKXHJKl_sRg` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/capture/digitalinvoice/DigitalInvoiceFragment.kt:295 — Deprecated code should not be used.
- `AZ-UJ11_ryKXHJKl_sRf` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/capture/digitalinvoice/DigitalInvoiceFragment.kt:360 — Deprecated code should not be used.
- `AZ-UJ11_ryKXHJKl_sRe` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/capture/digitalinvoice/DigitalInvoiceFragment.kt:559 — Deprecated code should not be used.
- `AZ-UJ1ycryKXHJKl_sRa` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/capture/digitalinvoice/help/DigitalInvoiceHelpFragment.kt:53 — Deprecated code should not be used.
- `AZ-UJ1ycryKXHJKl_sRZ` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/capture/digitalinvoice/help/DigitalInvoiceHelpFragment.kt:67 — Deprecated code should not be used.
- `AZ-UJ1zFryKXHJKl_sRc` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/capture/digitalinvoice/onboarding/DigitalInvoiceOnboardingFragment.kt:150 — Deprecated code should not be used.
- `AZ-UJ100ryKXHJKl_sRd` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/capture/digitalinvoice/skonto/DigitalInvoiceSkontoScreenModule.kt:17 — Deprecated code should not be used.
- `AZ5LCF4xiEJNhEcQ50T8` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/capture/skonto/SkontoScreenContent.kt:1027 — Deprecated code should not be used.
- `AZ-UJ1tiryKXHJKl_sRY` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/capture/skonto/help/SkontoHelpFragment.kt:62 — Deprecated code should not be used.
- `AZ-UJ19PryKXHJKl_sRk` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/invoice/InvoicePreviewModule.kt:52 — Deprecated code should not be used.
- `AZ-UJ17rryKXHJKl_sRj` kotlin:S1874 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/transactiondocs/ui/invoice/TransactionDocInvoicePreviewModule.kt:12 — Deprecated code should not be used.
- `AZ-UJ130ryKXHJKl_sRh` kotlin:S6512 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/capture/digitalinvoice/DigitalInvoice.kt:283 — Convert this getter to a "get()" on the property "amountToPay".

#### Info (1)

- `AZ-UJ1zFryKXHJKl_sRb` kotlin:S1135 [CODE_SMELL] src/main/java/net/gini/android/bank/sdk/capture/digitalinvoice/onboarding/DigitalInvoiceOnboardingFragment.kt:47 — Complete the task associated to this TODO comment.

### 6. core-api-library:library — branch `sonar-core-api-library-issues`

Paths are relative to the module root. Fix order inside the module: Critical → High → Medium → Low → Info.

#### Info (4)

- `AZ-UJBBZzGMUC9_wI8L1` java:S1135 [CODE_SMELL] src/androidTest/java/net/gini/android/core/api/models/ExtractionTest.java:75 — Complete the task associated to this TODO comment.
- `AZ-UJBA7zGMUC9_wI8L0` java:S1135 [CODE_SMELL] src/androidTest/java/net/gini/android/core/api/models/SpecificExtractionTest.java:56 — Complete the task associated to this TODO comment.
- `AZ6sq1-FYjpVRxW-yDhR` java:S2143 [CODE_SMELL] src/main/java/net/gini/android/core/api/authorization/Session.java:- — Use the "java.time" API for date and time.
- `AZ6sq1_FYjpVRxW-yDhT` java:S2143 [CODE_SMELL] src/main/java/net/gini/android/core/api/models/Document.java:- — Use the "java.time" API for date and time.

### 7. bank-api-library:library — branch `sonar-bank-api-library-issues`

Paths are relative to the module root. Fix order inside the module: Critical → High → Medium → Low → Info.

#### Medium (2)

- `AZ5FuSclA_ejdnBAPq9_` java:S8491 [CODE_SMELL] src/main/java/net/gini/android/bank/api/models/ExtractionsContainer.java:33 — Remove or merge the dangling Javadoc comment(s).
- `AZ5FuSZvA_ejdnBAPq9-` java:S8491 [CODE_SMELL] src/main/java/net/gini/android/bank/api/models/ReturnReason.java:22 — Remove or merge the dangling Javadoc comment(s).

## Notes for the remaining modules

- **internal-payment-sdk / health-sdk** are plain MVVM (`StateFlow` + `MutableSharedFlow`);
  S6305 "don't expose mutable flow types" → back with a private `Mutable*Flow` and
  expose `StateFlow`/`SharedFlow` via `asStateFlow()`/`asSharedFlow()`; S6310/S6311
  hardcoded dispatchers → inject a `CoroutineDispatcher` (default param +
  `@JvmOverloads` if Java callers exist); S6313 suspend functions on ViewModels →
  launch in `viewModelScope` and expose state/events instead. Follow
  `.claude/rules/coroutines-flow.md`.
- **bank-sdk** uses Orbit-MVI; its 11 S1874 findings are deprecated API usages in
  Fragments/Compose — check each for an API-level fallback before replacing.
- **core-api-library / bank-api-library** findings are Info/Medium Javadoc and
  `java.time` suggestions in public models — check the API dump impact first.
- Health/Bank/Core API libraries have `connectedCheck` in CI; run
  `/gini-connected-check` if a fix touches request/response code.

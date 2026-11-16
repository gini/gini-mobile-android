# PP-3239: Start the Android capture flow with a Uri of a document

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-3239

## Problem

An integrator (Atruvia) receives shared documents as content `Uri`s — the share
`Intent` is resolved by their core application before their banking module runs.
To hand those documents to the Bank SDK they currently have to re-wrap the Uris
in a hand-crafted `Intent` just so the SDK can unwrap it again
(`IntentHelper.getUris()` checks `EXTRA_STREAM` → `ClipData` → `intent.data`).
The required Intent shape is undocumented and fragile from the integrator's
side. The SDK should accept a list of `Uri`s directly.

Decisions from clarification (2026-08-26):

- New entry points: fragment-based (`createCaptureFlowFragmentForUris`) and
  low-level (`createDocumentForImportedFiles(uris, …)`). **No** activity-launcher
  (`startCaptureFlowForUris`) variant.
- Public API only in `bank-sdk:sdk` (`GiniBank`). The capture-sdk side is
  internal-use-only (exposed via the existing `GiniCapture.Internal` channel),
  not advertised to capture-only integrators.
- Implementation: a **new, additive Uri-based path**. The existing Intent-based
  open-with flow must not change behavior; a future ticket will migrate it onto
  the Uri-first core.
- Testing: unit **and** instrumented tests.

## Requirements

- **R1 (MUST, entry):** Given `GiniBank.setCaptureConfiguration(...)` was called,
  when the integrator calls
  `GiniBank.createCaptureFlowFragmentForUris(context, uris, callback)` with a
  list containing one resolvable PDF content Uri, then the callback receives
  `CreateCaptureFlowFragmentForIntentResult.Success` holding a
  `CaptureFlowFragment` created for a `Document` whose data was loaded from that
  exact Uri (byte content equals the file behind the Uri).
- **R2 (MUST, entry):** Given `GiniBank.setCaptureConfiguration(...)` was called,
  when the integrator calls
  `GiniBank.createDocumentForImportedFiles(uris, context, callback)` (List<Uri>
  overload), then the callback receives
  `CreateDocumentFromImportedFileResult.Success` with a `Document` that is
  accepted by the existing `GiniBank.createCaptureFlowFragmentForDocument(document)`
  and `GiniBank.startCaptureFlowForDocument(resultLauncher, document)`.
- **R3 (MUST, happy):** Given a single Uri with mime type `application/pdf` that
  passes `FileImportValidator`, when it is imported through either new entry
  point, then the resulting `Document` is a `PdfDocument` with
  `importMethod == OPEN_WITH`, `getUri()` returning the input Uri, and its
  loaded data equal to the bytes readable from that Uri.
- **R4 (MUST, happy):** Given a single valid image Uri (mime `image/*`), when it
  is imported, then the result is an `ImageMultiPageDocument` containing one
  compressed page stored in the `ImageDiskStore`, and
  `GiniCapture.getInstance().internal().getImageMultiPageDocumentMemoryStore()`
  holds that document (same post-condition as the Intent path in
  `GiniCaptureFileImport.createDocumentForImportedFiles`).
- **R5 (MUST, happy):** Given N valid image Uris, when they are imported, then
  the result is an `ImageMultiPageDocument` with N pages in the input order.
- **R6 (SHOULD, happy):** Given a single Uri with mime type `text/xml` or
  `application/xml` that passes validation, then the result is an `XmlDocument`
  with `importMethod == OPEN_WITH` and data loaded from the Uri.
- **R7 (MUST, happy/regression):** Given the existing Intent-based entry points
  (`GiniBank.startCaptureFlowForIntent`, `GiniBank.createCaptureFlowFragmentForIntent`,
  `GiniBank.createDocumentForImportedFiles(Intent, …)`,
  `GiniCapture.createDocumentForImportedFiles(Intent, …)`), when this feature is
  merged, then their signatures and observable behavior are unchanged (no edits
  to their bodies; `apiCheck` shows only additions).
- **R8 (MUST, error):** Given an empty Uri list, when either new entry point is
  called, then the callback receives the Error result carrying an
  `ImportedFileValidationException` with message `"Uri list is empty"` and
  `validationError == null`.
- **R9 (MUST, error):** Given `GiniBank.setCaptureConfiguration` was NOT called,
  when `createCaptureFlowFragmentForUris` is called, then it throws
  `IllegalStateException` with the existing `CAPTURE_NOT_CONFIGURED_MSG`
  (mirrors `createCaptureFlowFragmentForIntent`); when the
  `createDocumentForImportedFiles(uris, …)` overload is called, then it returns
  `null` without invoking the callback (mirrors the Intent overload's
  `giniCapture?.` behavior).
- **R10 (MUST, error):** Given a Uri whose `InputStream` cannot be opened, when
  it is imported as a single PDF/XML, then the callback receives the Error
  result with an `ImportedFileValidationException` whose message states the
  input stream is not available for the Uri (analogous to the Intent path's
  `"InputStream not available for Intent's data Uri"`).
- **R11 (MUST, error):** Given a Uri that fails `FileImportValidator`
  (file over the configured size limit, password-protected PDF, or unsupported
  type), when it is imported, then the callback receives the Error result with
  an `ImportedFileValidationException` whose `validationError` is the
  corresponding `FileImportValidator.Error` (e.g. `SIZE_TOO_LARGE`,
  `PASSWORD_PROTECTED_PDF`, `TYPE_NOT_SUPPORTED`) — same errors the Intent path
  produces today.
- **R12 (MUST, error):** Given a multi-Uri list that contains no importable
  images, when it is imported, then the callback receives the Error result with
  an `ImportedFileValidationException` with message `"Uris did not contain images"`.
  (Clarified 2026-08-28: the Intent path's multi-page branch records this error
  but still delivers Success with an EMPTY `ImageMultiPageDocument` — the spec's
  original premise was wrong. Decision: the new Uri path surfaces the error, as
  R12 intended; the Intent path stays untouched.)
- **R13 (MUST, async):** Given any valid input, when either new entry point is
  called, then it returns a `CancellationToken` immediately, nothing else is
  observable while the import is pending, and exactly one of
  Success/Error/Cancelled is later delivered on the main thread (delivery
  mechanics identical to the Intent path: `AsyncTask.onPostExecute` for images,
  `GiniCaptureDocument.loadData`'s callback for PDF/XML). Cancellation support
  mirrors the Intent path (today's tokens are no-ops for these branches — do not
  promise more).

## Affected modules

- `capture-sdk:sdk` — new internal Uri-import path plus additive factory
  methods; exposure via `GiniCapture.Internal`. API dump re-pin.
- `bank-sdk:sdk` — two new public `GiniBank` functions delegating to
  `GiniCapture.Internal`. API dump re-pin.
- `capture-sdk:default-network`, example apps, other SDKs — unchanged.

Dependency note: `bank-sdk:sdk` already depends on `capture-sdk:sdk` as a
project dependency; no build-file changes.

## Public API impact

All changes are **additive**; no existing declaration changes or is removed.

`bank-sdk:sdk` (`net.gini.android.bank.sdk.GiniBank`, advertised public API):

- NEW `fun createCaptureFlowFragmentForUris(context: Context, uris: List<Uri>, callback: (CreateCaptureFlowFragmentForIntentResult) -> Unit): CancellationToken`
  — reuses the existing `CreateCaptureFlowFragmentForIntentResult` sealed class
  (its name is Intent-flavored but reusing it avoids a parallel result type;
  the future Uri-first migration can introduce a rename with deprecation).
- NEW `fun createDocumentForImportedFiles(uris: List<Uri>, context: Context, callback: (CreateDocumentFromImportedFileResult) -> Unit): CancellationToken?`
  — overload of the existing Intent-based function, same result type.
- Re-pin `bank-sdk/sdk/api/sdk.api` (stale dump fails `apiCheck`).

`capture-sdk:sdk` (internal-use-only, but visible in bytecode/dump):

- NEW method on `GiniCapture.Internal` (`capture-sdk/sdk/src/main/java/net/gini/android/capture/GiniCapture.java:1680`):
  `public CancellationToken createDocumentForImportedUris(@NonNull List<Uri> uris, @NonNull Context context, @NonNull AsyncCallback<Document, ImportedFileValidationException> callback)`
  — carries the repo's "Internal use only. @suppress" doc marker, like the
  existing `Internal.createGiniCaptureFragmentForOpenWithDocument`.
- Additive Uri factory members on `PdfDocument`, `XmlDocument`, `ImageDocument`,
  `DocumentFactory` and an additive constructor on
  `AbstractImportImageUrisAsyncTask`/`ImportImageFileUrisAsyncTask`
  (all in already-public "Internal use only" classes).
- Re-pin `capture-sdk/sdk/api/sdk.api`.

## Technical conventions

1. **Language:** New import-flow class in Kotlin
   (`capture-sdk/sdk/src/main/java/net/gini/android/capture/GiniCaptureUriImport.kt`,
   `internal` visibility — Java code in the same module, i.e. `GiniCapture.Internal`,
   can call it since Kotlin `internal` is public in bytecode). Legacy Java files
   touched **additively only** (new members; no edits to existing method bodies
   except the one guarded branch listed in Design §multi-page):
   `PdfDocument.java`, `ImageDocument.java`, `DocumentFactory.java`,
   `GiniCapture.java` (new `Internal` method),
   `AbstractImportImageUrisAsyncTask.java` (new `@Nullable`-Intent constructor +
   null-guard in its private `createDocument`),
   `ImportImageFileUrisAsyncTask.java` (new constructor). No opportunistic
   Java→Kotlin conversion. New `GiniBank` functions carry KDoc (Dokka builds
   from it).
2. **UI:** none — no new screens, no Compose, no XML layouts added or removed.
   `CaptureFlowFragment`/`CaptureFlowActivity` are reused as-is.
3. **Architecture:** no ViewModels involved. The new code follows the existing
   facade + `AsyncCallback` pattern of the file-import entry points
   (`GiniBank`/`GiniCapture`/`GiniCaptureFileImport`) — deliberately *not*
   coroutines, to stay symmetric with the Intent twins it mirrors and with
   `AsyncCallback<Document, ImportedFileValidationException>` used across the
   boundary. No new state/intent/effect classes.
4. **DI/async:** no Koin module changes.
   `GiniBank.createCaptureFlowFragmentForUris` must call
   `BankSdkIsolatedKoinContext.init(context)` exactly as
   `createCaptureFlowFragmentForIntent` does (`GiniBank.kt:302`). No Hilt, no
   LiveData/RxJava. Background work stays on the existing `AsyncTask`-based
   pipeline (legacy, reused not extended conceptually — the Uri-first
   modernization is the follow-up migration).
5. **Strings/resources:** none. Exception messages are non-localized developer
   strings, consistent with the Intent path.
6. **Quality gates:** ktlint + detekt clean for all touched modules; unit test
   for every new Kotlin class; re-pin both api dumps so `apiCheck` passes;
   Jacoco/Sonar coverage on the new classes via the module check workflows.

## Design

Verified against code read in this session; the Intent path stays untouched.

**capture-sdk core — `GiniCaptureUriImport.kt` (new, `net.gini.android.capture`):**
mirrors `GiniCaptureFileImport.createDocumentForImportedFiles`
(`capture-sdk/sdk/src/main/java/net/gini/android/capture/GiniCaptureFileImport.java:65-143`)
but takes `List<Uri>`:

1. `GiniCapture.hasInstance()` false → `onError` with the existing
   "GiniCapture instance not available" exception (same message as
   `GiniCaptureFileImport.createNoGiniCaptureFileValidationException`).
2. Empty list → `onError(ImportedFileValidationException("Uri list is empty"))`.
3. Exactly one Uri with PDF/XML mime (checked via the existing
   `UriHelper.hasMimeType(uri, context, …)`, no Intent needed — this is what the
   Intent path already does at `GiniCaptureFileImport.java:78-82`):
   validate stream availability (`UriHelper.isUriInputStreamAvailable`) and
   `FileImportValidator.matchesCriteria(uri)` (Uri-only overload exists,
   `FileImportValidator.java:84`), then create the document via new factories
   and `document.loadData(context, …)` → `callback.onSuccess(document)`.
   `GiniCaptureDocument.loadData` already loads from `mUri` when the Intent is
   null (`GiniCaptureDocument.java:248-266`).
4. Otherwise (images / multiple Uris): run `ImportImageFileUrisAsyncTask` via
   its new Intent-less constructor with `Source.newExternalSource()` and
   `ImportMethod.OPEN_WITH`; on success set the result into
   `ImageMultiPageDocumentMemoryStore` exactly as the Intent path does
   (`GiniCaptureFileImport.java:119-122`), then `onSuccess`.

**New document factories (additive):**

- `PdfDocument.fromUri(uri, importMethod)` — calls the existing
  `PdfDocument(Intent, Uri, Source, ImportMethod)` constructor with a null
  Intent; the `GiniCaptureDocument` super-constructor already accepts
  `@Nullable Intent` (`GiniCaptureDocument.java:60`). Source =
  `Source.newExternalSource()` (the Intent path resolves the source app name
  from `intent.getComponent()`, which is null for real share intents anyway).
- `XmlDocument.fromUri(uri, importMethod)` — same shape, in the companion of
  `XmlDocument.kt`.
- `ImageDocument.fromUri(uri, context, deviceOrientation, deviceType, importMethod)`
  (no Intent parameter; Source = external) + matching
  `DocumentFactory.newImageDocumentFromUri` overload — mirrors the existing
  `ImageDocument.fromUri(uri, intent, …)` (`ImageDocument.java:114`) which uses
  the Intent only to derive the source app name.

**Multi-page image task — the single shared-code modification:**
`AbstractImportImageUrisAsyncTask` uses its `mIntent` in exactly one place —
the private `createDocument(uri)` (`AbstractImportImageUrisAsyncTask.java:202-209`).
Add a second constructor accepting a nullable Intent, relax the field to
`@Nullable`, and in `createDocument` branch: `mIntent != null` → existing call
(byte-for-byte unchanged behavior for the Intent path), `mIntent == null` →
the new Intent-less `DocumentFactory.newImageDocumentFromUri` overload.
`ImportImageFileUrisAsyncTask` gets a matching additional constructor. All of
`doInBackground`/`processImageUri` (validation, compression, disk store) is
already Uri-based and is reused untouched.

**Exposure to bank-sdk — `GiniCapture.Internal.createDocumentForImportedUris(...)`:**
one-line delegation to `GiniCaptureUriImport`, following the existing pattern
of `Internal.createGiniCaptureFragmentForOpenWithDocument`
(`GiniCapture.java:1694`).

**bank-sdk facade (`GiniBank.kt`):**

- `createDocumentForImportedFiles(uris, context, callback)` — mirrors the
  Intent overload at `GiniBank.kt:477-499`: wraps the `AsyncCallback` into
  `CreateDocumentFromImportedFileResult`, returns
  `giniCapture?.internal()?.createDocumentForImportedUris(...)`.
- `createCaptureFlowFragmentForUris(context, uris, callback)` — mirrors
  `createCaptureFlowFragmentForIntent` (`GiniBank.kt:296-324`):
  `check(giniCapture != null)`, `BankSdkIsolatedKoinContext.init(context)`,
  create document via the Uri path, on success wrap with
  `createCaptureFlowFragmentForDocument(document)` into
  `CreateCaptureFlowFragmentForIntentResult.Success`.

`ImportMethod` stays `OPEN_WITH`, so every downstream consumer (review/analysis
screens, upload metadata, `isReviewable` logic in `ImageDocument`) behaves
exactly as for an Intent-based open-with document — this is what makes the
fragment/document handoff work without touching any screen code.

## Test plan

Every new Kotlin class gets a unit test. Stacks match neighbors: capture-sdk
unit tests use Robolectric + MockK + Truth (like `GiniCaptureSendTransferSummaryTest.kt`);
bank-sdk unit tests use MockK + Truth + kotlinx-coroutines-test (like
`GiniBankTest.kt`); capture-sdk instrumented tests use AndroidJUnitRunner +
Truth with `Helpers.getAssetFileFileContentUri(...)` real content Uris (like
`FileImportValidatorTest.java`).

- **`capture-sdk/sdk/src/test/java/net/gini/android/capture/GiniCaptureUriImportTest.kt`**
  (new class, Robolectric + MockK + Truth) — ~6 tests:
  no-GiniCapture-instance error (R9's capture-side analog), empty-list error
  (R8), single PDF Uri success with data equal to the registered shadow stream
  (R3, via Robolectric `ShadowContentResolver` streams + mime), input-stream
  unavailable error (R10), validation-failure error propagation with
  `validationError` set (R11), returned token + single terminal callback (R13).
  (confidence: LOW — Robolectric's ShadowContentResolver must serve mime type
  and stream for the PDF branch; if it proves unreliable these cases move to
  the instrumented class.)
- **`capture-sdk/sdk/src/androidTest/java/net/gini/android/capture/GiniCaptureUriImportInstrumentedTest.kt`**
  (new class, next to `internal/util/FileImportValidatorTest.java`, using the
  existing `invoice.pdf` / `invoice.jpg` / `invoice-password.pdf` assets) —
  ~6 tests: PDF Uri → `PdfDocument` with loaded bytes (R3), single image Uri →
  one-page `ImageMultiPageDocument` + memory store set (R4), two image Uris →
  two pages in order (R5), XML Uri → `XmlDocument` (R6), password-protected PDF
  → `PASSWORD_PROTECTED_PDF` error (R11), multi-Uri list without images →
  error (R12).
- **`bank-sdk/sdk/src/test/java/net/gini/android/bank/sdk/GiniBankUriImportTest.kt`**
  (new class — `GiniBankTest.kt` covers the pay feature, not capture, so a new
  focused class beats extending it; Robolectric because
  `setCaptureConfiguration` builds a real `GiniCapture` from a `Context`) —
  ~5 tests: `createCaptureFlowFragmentForUris` throws `IllegalStateException`
  when unconfigured (R9), `createDocumentForImportedFiles(uris, …)` returns
  null when unconfigured (R9), success path yields
  `Success(CaptureFlowFragment)` (R1), document from the low-level overload is
  accepted by `createCaptureFlowFragmentForDocument` (R2), error propagates as
  the Error result (R8/R11 surface).
- **R7 (regression):** proven by the untouched existing test suites plus a green
  `apiCheck` after re-pinning both dumps showing additions only. Run
  `/gini-check` for `capture-sdk:sdk` and `bank-sdk:sdk`, and
  `/gini-connected-check` for the new instrumented class.

### Not tested

- The full capture UI flow after the fragment is created (camera, review,
  analysis screens) — framework/UI behavior already covered by existing tests;
  end-to-end open-with-via-Uri is manual QA (share a PDF and an image gallery
  selection into the example app patched to use the new API).
- `AsyncTask` threading internals and `ContentResolver` framework behavior.
- Cancellation semantics beyond "token is returned" — the underlying tokens are
  no-ops on the Intent path today (R13); adding real cancellation is not part
  of this ticket.
- The deprecated-name aesthetics of `CreateCaptureFlowFragmentForIntentResult`
  — a rename is deferred to the Uri-first migration.

## Out of scope

- `startCaptureFlowForUris` (activity-launcher/Screen API variant) — explicitly
  deselected.
- Any public Uri API on `GiniCapture` for capture-only integrators.
- Migrating the Intent-based open-with flow onto the Uri core (future ticket;
  this change is purely additive).
- Modernizing the `AsyncTask`-based import pipeline to coroutines.
- Integration-guide/documentation-site updates for the new API (KDoc is in
  scope; the Sphinx guide update should be its own follow-up ticket, since the
  customer explicitly called out missing documentation).
- ~~Example-app changes demonstrating the new API (optional follow-up).~~
  Pulled into scope 2026-09-03 as QA support — see "Example app QA support".

## Example app QA support (added 2026-09-03)

The new Uri entry points have no UI of their own, so manual QA needs the
`bank-sdk:example-app` open-with flow patched to exercise them:

- **QA-1 (toggle):** A "use Uri-based open-with API" switch in the example
  app's configuration screen (`layout_feature_toggles.xml`, next to the
  existing open-with switch), backed by a new
  `ExampleAppBankConfiguration.isOpenWithUriBasedApiEnabled` field
  (default `false`).
- **QA-2 (persistence):** "Open with" cold-starts the app, and the example
  app's configuration is otherwise in-memory only — so this toggle is
  persisted in `SharedPreferences`. **Only this single boolean is persisted;
  no other configuration field.** On cold start the persisted value is read
  before choosing the import path and seeded back into the in-memory
  configuration so the configuration screen reflects it.
- **QA-3 (branching):** With the toggle ON, every open-with call site extracts
  the Uris from the incoming Intent (`data` / `EXTRA_STREAM`) and calls the
  new Uri-based APIs (`createDocumentForImportedFiles(uris, ...)`,
  `createCaptureFlowFragmentForUris(...)`). With the toggle OFF (default) the
  existing Intent path runs untouched — zero behavior change for all other
  QA scenarios.
- **QA-4 (path indicator):** The Uri-based path announces itself (toast + log
  line) so QA can prove the new code ran; the two paths are otherwise
  intentionally indistinguishable (R11).

QA scenario matrix (toggle ON, each compared against a toggle-OFF run):
single pdf, single image, multiple images, xml invoice, password-protected
pdf (error), share with no importable images (error: "Uris did not contain
images" — the one intended divergence from the Intent path, see R12), and
oversized file (validation error). Full cold-start run (app killed, toggle
persisted ON, share from Files, complete extraction) is a required case.

## Open questions

- ~~Should the Robolectric PDF-path unit tests be kept if ShadowContentResolver
  mime/stream registration proves flaky, or is instrumented-only coverage of
  the happy paths acceptable?~~ Resolved 2026-08-26: try Robolectric first; if
  ShadowContentResolver proves unreliable, move those cases to the instrumented
  class and keep only the error/edge-case unit tests.

## Implementation plan

- [x] 1. capture-sdk:sdk — additive Uri document factories: `PdfDocument.fromUri(uri, importMethod)`,
     `XmlDocument.fromUri(uri, importMethod)` (companion), Intent-less
     `ImageDocument.fromUri(uri, context, deviceOrientation, deviceType, importMethod)` +
     `DocumentFactory.newImageDocumentFromUri` overload; relax the Intent parameter of the
     existing `PdfDocument`/`XmlDocument` constructors to nullable (annotation/`Intent?` only,
     binary signature unchanged). Files: `document/PdfDocument.java`, `document/XmlDocument.kt`,
     `document/ImageDocument.java`, `document/DocumentFactory.java`. (R3, R4, R6)
- [x] 2. capture-sdk:sdk — `AbstractImportImageUrisAsyncTask`: new `@Nullable`-Intent constructor,
     relax `mIntent` field to `@Nullable`, branch in private `createDocument` (`mIntent != null`
     → existing call unchanged; null → new Intent-less factory); matching extra constructor on
     `ImportImageFileUrisAsyncTask`. (R4, R5, R12)
- [x] 3. capture-sdk:sdk — new `GiniCaptureUriImport.kt` (internal, `net.gini.android.capture`)
     mirroring `GiniCaptureFileImport.createDocumentForImportedFiles` for `List<Uri>` +
     unit tests `src/test/java/net/gini/android/capture/GiniCaptureUriImportTest.kt`
     (Robolectric + MockK + Truth; fall back to instrumented for PDF happy path if
     ShadowContentResolver is unreliable). (R1–R6 core path, R8, R10, R11, R13)
- [x] 4. capture-sdk:sdk — `GiniCapture.Internal.createDocumentForImportedUris(uris, context, callback)`
     delegating to `GiniCaptureUriImport`, with "Internal use only. @suppress" doc. (R1, R2)
- [x] 5. bank-sdk:sdk — `GiniBank.createDocumentForImportedFiles(uris, context, callback)` overload
     and `GiniBank.createCaptureFlowFragmentForUris(context, uris, callback)` with KDoc,
     mirroring the Intent twins (incl. `BankSdkIsolatedKoinContext.init(context)` and
     `check(giniCapture != null)` / `giniCapture?.` semantics). (R1, R2, R9)
- [x] 6. bank-sdk:sdk — unit tests
     `src/test/java/net/gini/android/bank/sdk/GiniBankUriImportTest.kt` (Robolectric + MockK +
     Truth). (R1, R2, R8, R9, R11 surface)
- [x] 7. capture-sdk:sdk — instrumented tests
     `src/androidTest/java/net/gini/android/capture/GiniCaptureUriImportInstrumentedTest.kt`
     using `Helpers.getAssetFileFileContentUri` with `invoice.pdf` / `invoice.jpg` /
     `invoice-password.pdf` + new `invoice.xml` asset. (R3, R4, R5, R6, R11, R12)
- [x] 8. Re-pin API dumps: `./gradlew capture-sdk:sdk:apiDump bank-sdk:sdk:apiDump`; verify the
     diff shows additions only. (R7)
- [x] 9. Verification: `/gini-check` (capture-sdk:sdk, bank-sdk:sdk and dependents) and
     `/gini-connected-check` for the new instrumented class. (R7, all)
- [x] 10. bank-sdk:example-app — QA toggle for the Uri-based open-with path: persisted
     single-boolean switch, Uri extraction in `ExampleUtil`, branching at all open-with
     call sites, path indicator. See "Example app QA support". (QA-1–QA-4)

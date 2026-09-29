# PP-3499: [Android] Add support for HEIC file format

Status: implemented — API 28 (Android 9) up to compileSdk 36 (Android 16)
Ticket: https://ginis.atlassian.net/browse/PP-3499

## Problem

Modern Android phones (notably Samsung) capture photos as HEIC/HEIF by default.
When a user picks such a photo for the photo-payment flow, the Gini Capture SDK
rejects it: `FileImportValidator.isSupportedFileType` only accepts
`image/jpeg`, `image/png`, `image/gif`, PDF and XML
(`capture-sdk/sdk/src/main/java/net/gini/android/capture/internal/util/FileImportValidator.java:144-157`),
so the user sees the "type not supported" error and has to convert the file by
hand. The file picker itself already offers HEIC files, because every picker
intent is opened with `image/*`
(`capture-sdk/sdk/src/main/java/net/gini/android/capture/internal/fileimport/FileChooserFragment.kt:279, 380, 409`),
which makes the rejection feel arbitrary.

### How iOS solved this — PP-1430, shipping in GiniBankSDK 4.6.0

iOS fixed the same customer report under **PP-1430 "[iOS] HEIC file format
issue"** (`specs/PP-1430-bug.md`), on the release branch
`release/GiniBankSDK_4.6.0` — gini/gini-mobile-ios PR
[#1270](https://github.com/gini/gini-mobile-ios/pull/1270), still open against
`main` at the time of writing. Reported by Barclays/Veripark on the "Share
with" flow.

What iOS actually does:

1. **Detects HEIC by magic bytes, not by mime type.** `Data.mimeType` matched on
   the first byte only, and a HEIC file starts with the ISO-BMFF box size
   (`0x00 0x00 0x00 0x20`), so it fell through to `application/octet-stream`
   and `isImage` returned `false`. The fix adds `Data.isHEIC`, which checks for
   the `ftyp` box marker at offset 4 and one of **five HEIF brands** at offset
   8: `heic`, `heix`, `heif`, `mif1`, `msf1`.
2. **Transcodes HEIC to JPEG at a single choke point** — the top of
   `GiniImageDocument.init(data:...)`, which every entry path goes through.
   The new `Data.jpegDataPreservingMetadata(compressionQuality:)` helper goes
   `CGImageSource → CGImageDestination` with `kUTTypeJPEG`, deliberately *not*
   `UIImage(data:).jpegData(...)`, because the latter decodes to a bitmap and
   drops every embedded property. EXIF, TIFF and GPS survive the transcode.
3. **Why transcode at all:** the iOS spec records this as a resolved open
   question — *"Does the Gini backend accept HEIC bytes directly? **Resolved.**
   Backend does not accept HEIC."* The code comment in `GiniImageDocument`
   states it outright: `// Normalise HEIC input to JPEG — the backend does not
   accept HEIC bytes`.
4. **Deliberately did not** add HEIC to `GiniImageDocument.acceptedImageTypes`.
   On iOS that list drives the file-picker filter and `NSItemProviderReading`,
   so widening it changes picker UI; they scoped it to a separate ticket. The
   Photos-app path already worked before the fix precisely because the picker
   transcodes to JPEG for them — only the Files-app "Open with" path carried
   raw HEIC bytes.

**What this means for Android.** Point 3 removes the only real unknown: the
backend rejects HEIC, so client-side conversion is not a preference, it is
required. Point 4 does *not* carry over — Android's pickers are already opened
with `image/*` (`FileChooserFragment.kt:279, 380, 409`), so HEIC files are
already offered and both the picker and the "open with" path need to work.
Point 1 does carry over in a weaker form: Android gets its mime type from the
`ContentResolver`, which is usually correct, but `UriHelper.getMimeType` falls
back to `MimeTypeMap` by file extension and can return `null` for a `file://`
Uri from a file manager — the same class of blind spot iOS hit. Point 2 sets a
metadata bar Android cannot fully match without new work; see R4 and
"Out of scope".

## Requirements

- **R1 (MUST, entry):** Given an integrator that has created a `GiniCapture`
  instance, when a `.heic` / `.heif` file Uri is passed to
  `GiniCapture.createDocumentForImportedFiles(Intent, Context, AsyncCallback)`,
  `GiniCapture.createDocumentForImportedUris(List<Uri>, Context, AsyncCallback)`
  or selected through the SDK's own file chooser on a device running API 28 or
  higher, then the callback delivers an `ImageMultiPageDocument` (success) and
  no `ImportedFileValidationException` is raised.

- **R2 (MUST, happy path):** Given a single-page `.heic` file imported on API
  28+, when the import completes, then the resulting `ImageDocument` reports
  `getMimeType() == "image/jpeg"` and `getFormat() == ImageFormat.JPEG`, and
  `getData()` starts with the JPEG SOI marker `0xFF 0xD8` — i.e. the bytes that
  `GiniCaptureDefaultNetworkService.upload` passes as `contentType =
  document.mimeType`
  (`capture-sdk/default-network/src/main/java/net/gini/android/capture/network/GiniCaptureDefaultNetworkService.kt:251`)
  are JPEG, never HEIC.

- **R3 (MUST, happy path):** Given a `.heic` file whose decoded bitmap is
  W×H pixels, when the import completes, then the decoded JPEG has the same
  pixel dimensions W×H (subject only to the existing default compression
  quality applied by `PhotoEdit.compressByDefault()`), so the conversion cannot
  be satisfied by returning a fixed placeholder image.

- **R4 (MUST, happy path):** Given a `.heic` file whose EXIF orientation tag is
  not `ORIENTATION_NORMAL` (e.g. a portrait photo stored rotated), when the
  import completes, then the resulting document is displayed and uploaded in
  the same visual orientation as the original — the rotation is not silently
  dropped. This is orientation *parity with the existing JPEG import path*,
  which preserves rotation through `MutablePhoto.initRotationForDisplay` +
  `updateExif`; it is not the full EXIF/TIFF/GPS preservation iOS achieves (see
  "Out of scope").

- **R5 (MUST, happy path):** Given a multi-image HEIC container
  (`image/heic-sequence` / `image/heif-sequence`, e.g. a burst), when it is
  imported on API 28+, then exactly one `ImageDocument` is produced from the
  container's primary image and no exception is thrown.

- **R6 (MUST, error path):** Given a `.heic` file on a device running below API
  28 (HEIF decoding is unavailable), when it is imported, then
  `FileImportValidator.matchesCriteria` returns `false` with
  `FileImportValidator.Error.TYPE_NOT_SUPPORTED`, the integrator receives an
  `ImportedFileValidationException` whose `getValidationError()` is that value,
  and the SDK's own camera screen shows the existing
  `R.string.gc_document_import_error_type_not_supported` message. No new error
  value and no new string are introduced.

- **R7 (MUST, error path):** Given a file whose mime type is HEIC but whose
  bytes cannot be decoded (truncated/corrupt), when it is imported, then the
  existing halt-on-error path in
  `AbstractImportImageUrisAsyncTask.processImageUri` is taken — the integrator
  receives an `ImportedFileValidationException` and the SDK does not crash and
  does not upload undecodable bytes.

- **R8 (MUST, error path):** Given a file of a still-unsupported type (e.g.
  `image/webp`, `image/bmp`, `image/tiff`), when it is imported, then it is
  still rejected with `Error.TYPE_NOT_SUPPORTED` — this change widens the
  accepted set by HEIC/HEIF only.

- **R9 (MUST, happy path):** Given a device whose system camera captures HEIC,
  when the user takes a photo **inside the SDK camera**, then the photo is
  processed successfully. This already holds today: the SDK's CameraX pipeline
  produces JPEG (`PhotoFactory.newPhotoFromJpeg`,
  `capture-sdk/sdk/src/main/java/net/gini/android/capture/internal/camera/photo/PhotoFactory.java:16-22`)
  regardless of the device's default gallery-capture format. The requirement is
  to *verify* this, not to change the camera path.

- **R10 (SHOULD, async):** Given a HEIC import, when the conversion is running,
  then it happens off the main thread inside the existing
  `AbstractImportImageUrisAsyncTask.doInBackground`, and the camera screen's
  existing activity indicator
  (`CameraFragmentImpl.showActivityIndicatorAndDisableInteraction`) remains the
  only progress surface — no new loading UI.

- **R11 (MUST, error path):** Given `image/jpeg`, `image/png` and `image/gif`
  imports, when this change ships, then their behaviour is unchanged — PNG and
  GIF continue to be uploaded with their original mime type via the
  `ImmutablePhoto` no-op edit path.

- **R12 (MUST, entry):** Given a `.heic` file whose Uri resolves to no mime type
  or to a generic one (`null` from `ContentResolver.getType` and no usable
  extension, or `application/octet-stream`), when it is imported on API 28+,
  then the SDK still recognises it as a HEIC image by inspecting its header —
  the `ftyp` box marker at byte offset 4 and one of the five HEIF brands
  `heic`, `heix`, `heif`, `mif1`, `msf1` at offset 8 — and R1–R5 apply
  unchanged. This mirrors iOS `Data.isHEIC` and covers the file-manager
  "share with" case that PP-1430 was reported against.

## Affected modules

- **`capture-sdk:sdk`** — the only module with production changes. Mime-type
  table, file validation, image format enum, and the photo import/compression
  pipeline all live here.
- **`capture-sdk:sdk` api dump** (`capture-sdk/sdk/api/sdk.api`) — must be
  re-pinned, see below.
- **`bank-sdk:example-app`** — no production change; UI tests and one test
  asset added for the end-to-end gallery and "open with" paths (see Test plan).
- **`bank-sdk:sdk`** and **`capture-sdk:default-network`** — no source change,
  but they consume `capture-sdk:sdk` as a Gradle project dependency, so their
  `testDebugUnitTest` / `lint` / `detekt` / `ktlintCheck` must be run as part of
  the check sweep (`/gini-check` expands the chain automatically).
- **`health-sdk`, `internal-payment-sdk`, the API libraries** — untouched; the
  photo-payment import path does not run through them.

## Public API impact

All changes are **additive**. `capture-sdk/sdk/api/sdk.api` must be re-pinned in
the same commit, otherwise `apiCheck` fails.

| Declaration | Change |
|---|---|
| `net.gini.android.capture.document.ImageDocument$ImageFormat` | New enum constant `HEIC` (dump lines 1517-1522 currently list only `GIF`, `JPEG`, `PNG`). Additive. Kotlin integrators with an exhaustive `when` over `ImageFormat` get a compiler warning, not a source break; Java `switch` is unaffected. |
| `net.gini.android.capture.internal.camera.photo.Photo` | New method `setImageFormat(ImageDocument.ImageFormat)` alongside the existing `getImageFormat()` (dump line 1928). `Photo` is marked *Internal use only / @suppress* but is `public`, so the dump changes. Additive. |
| `net.gini.android.capture.internal.util.MimeType` | New enum constants for the HEIC/HEIF mime types. Marked *Internal use only / @suppress*; additive. |
| `FileImportValidator.Error` | **No change** — R6 deliberately reuses `TYPE_NOT_SUPPORTED`. |

`HeicHeader.kt` is Kotlin `internal`, so it does not appear in the dump.

No breaking change, so no major version bump is implied.

## Technical conventions

1. **Language.** The one new file, `internal/util/HeicHeader.kt`, is Kotlin and
   `internal`. Everything else lands in existing **legacy Java** files, which
   per AGENTS.md must not be opportunistically converted. The Java files that
   may be edited, and why:
   - `internal/util/MimeType.java` — the mime-type enum being extended.
   - `internal/util/FileImportValidator.java` — the accept/reject decision.
   - `document/ImageDocument.java` — the `ImageFormat` enum and its
     mime-type mapping.
   - `internal/camera/photo/PhotoFactory.java` — chooses mutable vs. immutable
     photo by format.
   - `internal/camera/photo/Photo.java`, `ImmutablePhoto.java`,
     `PhotoCompressionModifier.java` — the format must become JPEG once the
     bytes have been re-encoded.
   - `internal/fileimport/AbstractImportImageUrisAsyncTask.java` — orientation
     handling for HEIC.
   Each edit stays in the style of the file it is in (Java, `m`-prefixed
   fields, `@NonNull`/`@Nullable`).

2. **UI.** No UI work. No new Compose screens, no new XML layouts, no layout
   removals. The only user-facing surface is the existing error message shown
   by `CameraFragmentImpl.showGenericInvalidFileError` /
   `ErrorType.typeFromError`.

3. **Architecture.** No new architectural component. The change extends the
   existing legacy `AsyncTask`-based import pipeline in place — per
   `platform.md`, legacy code is integrated at its boundary, not rewritten.
   No ViewModel, no state/intent/effect classes are introduced, so MVVM vs.
   MVI does not arise here.

4. **DI & async.** No DI change — nothing new is registered in
   `CaptureSdkIsolatedKoinContext`. Async stays on the existing
   `AsyncTask.doInBackground` used by `AbstractImportImageUrisAsyncTask`; do
   **not** introduce coroutines into this legacy path for this ticket, and do
   not add LiveData or RxJava.

5. **Strings / resources.** No new strings and no new locale entries.
   `capture-sdk/sdk/src/main/res` has `values` and `values-en` only; R6 reuses
   the existing `gc_document_import_error_type_not_supported`, which is already
   present in both.

6. **Dependencies.** No new Gradle dependency. Orientation is read with the
   framework `android.media.ExifInterface`, which gains HEIF support at API 28
   — the same API level the feature is gated to. Do **not** add
   `androidx.exifinterface`.

7. **Quality gates.** `ktlintCheck` and `detekt` must be clean for
   `capture-sdk:sdk`. `apiCheck` must pass with the re-pinned
   `capture-sdk/sdk/api/sdk.api`. New/changed classes are expected to keep
   Jacoco line coverage from regressing in the Sonar report.

## Design

### Where HEIC is rejected today

Every image import — camera-screen picker, "open with" intent, and the Uri
list API — funnels into the same place:

- `CameraFragmentImpl.onActivityResult` → `handleMultiPageDocumentAndCallListener`
  (`camera/CameraFragmentImpl.java:1542`) for anything with an `image/` prefix
- `GiniCaptureFileImport.createDocumentForImportedFiles`
  (`GiniCaptureFileImport.java:105`) → `ImportImageFileUrisAsyncTask`
- `GiniCaptureUriImport.importImages` (`GiniCaptureUriImport.kt:105`) →
  `ImportImageFileUrisAsyncTask`

…all of which reach
`internal/fileimport/AbstractImportImageUrisAsyncTask.doInBackground`, which
calls `FileImportValidator.matchesCriteria(uri)` per Uri and then
`processImageUri`. `isSupportedFileType` is the single gate that rejects HEIC
today.

### Why conversion is almost free here

`AbstractImportImageUrisAsyncTask.processImageUri` already does, for every
imported image:

```
bytes = UriHelper.getBytesFromUri(uri)      // raw file bytes
photo = PhotoFactory.newPhotoFromDocument(document)
photo.edit().compressByDefault().apply()    // <- re-encode
localUri = imageDiskStore.save(photo.getData())
compressed = DocumentFactory.newImageDocumentFromPhoto(photo, localUri)
```

The conversion itself is not optional: the Gini backend does not accept HEIC
bytes (established on the iOS side under PP-1430, see Problem). And
`PhotoCompressionModifier.modify()`
(`internal/camera/photo/PhotoCompressionModifier.java:32-50`) is already a
decode-and-re-encode step that **always** writes
`Bitmap.CompressFormat.JPEG`. The reason PNG and GIF are not converted today is
purely that `PhotoFactory.newPhotoFromDocument` returns an `ImmutablePhoto` for
them, whose `edit()` is a `NoOpPhotoEdit`
(`ImmutablePhoto.java:90-92`). So the conversion machinery exists — HEIC only
needs to be routed through it, and the resulting format has to be corrected to
JPEG.

### Changes

1. **`internal/util/MimeType.java`** — add `IMAGE_HEIC("image/heic")`,
   `IMAGE_HEIF("image/heif")`, `IMAGE_HEIC_SEQUENCE("image/heic-sequence")`,
   `IMAGE_HEIF_SEQUENCE("image/heif-sequence")`. The existing static `sLookup`
   map picks them up automatically.

2. **New `internal/util/HeicHeader.kt`** (Kotlin, `internal`) — the Android
   counterpart of iOS `Data.isHEIC`. A single function that, given the first
   12 bytes of a stream, returns whether they are `<4-byte box size>` +
   `"ftyp"` at offset 4 + one of `heic` / `heix` / `heif` / `mif1` / `msf1` at
   offset 8. Used as a fallback when the resolver mime type is `null` or
   generic (R12). Reading 12 bytes via `ContentResolver.openInputStream` is
   cheap and happens on the existing background thread.

3. **`internal/util/FileImportValidator.java`** — add a private
   `isHeic(List<String>)` that matches the four HEIC/HEIF mime types, plus the
   `HeicHeader` fallback for `null`/`application/octet-stream` mime types, and
   accept the result in `isSupportedFileType` **only** when
   `Build.VERSION.SDK_INT >= Build.VERSION_CODES.P` (28). Below that, fall
   through to the existing `Error.TYPE_NOT_SUPPORTED` (R6). Everything else in
   the validator (size limit, page limits) is format-agnostic and unchanged.

4. **`document/ImageDocument.java`** — add `HEIC` to the public `ImageFormat`
   enum; map all four HEIC/HEIF mime types to it in
   `ImageFormat.fromMimeType`; map `HEIC` → `"image/heic"` in
   `mimeTypeFromFormat`. Without this, `ImageDocument.fromUri`
   (`ImageDocument.java:129-147`) would throw
   `IllegalArgumentException("Unknown mime type: image/heic")` the moment the
   validator lets a HEIC file through.

5. **`internal/camera/photo/PhotoFactory.java`** — return a `MutablePhoto` for
   `HEIC` as well as `JPEG`, so `edit()` is a real `PhotoEdit` and the
   compression modifier actually runs. `MutablePhoto`'s JPEG-EXIF reads
   degrade safely on HEIC bytes: `readRequiredTags` catches
   `IOException | ImageReadException` and leaves `mRequiredTags` null
   (`MutablePhoto.java:219-229`), and `initFieldsFromExif` catches
   `ExifReaderException` from `ExifReader.forJpeg` and falls back to a
   generated content id (`MutablePhoto.java:63-86`).

6. **`Photo` / `ImmutablePhoto` / `PhotoCompressionModifier`** — make the image
   format mutable: add `setImageFormat(ImageFormat)` to the `Photo` interface,
   make `ImmutablePhoto.mImageFormat` non-final, and have
   `PhotoCompressionModifier.modify()` set `ImageFormat.JPEG` right after it
   writes the JPEG bytes. This is correct for every input the modifier can
   receive, because it unconditionally encodes JPEG. This is what makes R2
   hold: `DocumentFactory.newImageDocumentFromPhoto(photo, localUri)` derives
   the compressed document's mime type from `photo.getImageFormat()` via
   `ImageDocument.mimeTypeFromFormat` (`ImageDocument.java:181-186, 220-232`).

7. **Orientation (R4)** — `MutablePhoto.initRotationForDisplay`
   (`MutablePhoto.java:153-159`) only sets a rotation when `ExifReader.forJpeg`
   succeeded, which it cannot for HEIC, so a rotated HEIC would currently
   import as `rotationForDisplay == 0`. In
   `AbstractImportImageUrisAsyncTask.processImageUri`, when the document's
   format is `HEIC`, read `ExifInterface.TAG_ORIENTATION` from the content Uri
   with the framework `android.media.ExifInterface` and apply it via
   `photo.setRotationForDisplay(degrees)` **before**
   `photo.edit().compressByDefault().apply()`.
   *(confidence: LOW — that `android.media.ExifInterface` reads the orientation
   tag of a HEIF stream from API 28 is documented behaviour I did not execute
   in this session. Confirm with the instrumented test in the test plan; if it
   does not hold, the fallback is to rotate the decoded `Bitmap` in the
   conversion step instead.)*

8. **Multi-image containers (R5)** — no extra code.
   `BitmapFactory.decodeByteArray`, used by both `PhotoCompressionModifier` and
   `ImmutablePhoto.createPreview`, returns the container's primary image only,
   which is exactly the required behaviour.

9. **`capture-sdk/sdk/api/sdk.api`** — re-pin after the above.

### Explicitly unchanged

- No picker-intent change. `FileChooserFragment` already uses `image/*`
  everywhere (`:279, :380, :409`), so HEIC files are already offered.
- No change in `capture-sdk:default-network`; it forwards
  `document.mimeType`, which R2 guarantees is `image/jpeg`.
- No change to the Gini API contract — the API never sees a HEIC byte.
- Unlike iOS PP-1430, no picker-scope decision is deferred: iOS left HEIC out of
  `acceptedImageTypes` because that list filters their file picker, whereas the
  Android pickers are already `image/*`, so both the picker and the "open with"
  path are covered by this ticket.

## Test plan

Test stack for this module: JUnit4 + Robolectric + Google Truth for unit tests,
AndroidX Test + Truth for instrumented tests, matching the neighbouring
`ImageDocumentTest.kt` and `FileImportValidatorTest.java`.

| Test class | New/extended | Approx. tests | Covers |
|---|---|---|---|
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/document/ImageDocumentTest.kt` | **Extend** | 4–5 | `ImageFormat.fromMimeType` maps `image/heic`, `image/heif`, `image/heic-sequence`, `image/heif-sequence` → `HEIC`; `mimeTypeFromFormat(HEIC)` → `"image/heic"`; an unknown image mime still throws `IllegalArgumentException` (R8). Robolectric, follows the file's existing `@Config(sdk = [33])` + `shadowOf(MimeTypeMap.getSingleton())` setup. |
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/internal/util/HeicHeaderTest.kt` | **New** | 4–6 | R12 detection, parametrised over the five HEIF brands (`heic`, `heix`, `heif`, `mif1`, `msf1`) exactly as iOS `DataHEICTests` does; plus false for JPEG magic (`0xFF 0xD8`), false for octet-stream noise, false for a buffer shorter than 12 bytes. Pure JVM — builds the 16-byte `ftyp` signature in the test, no decoder needed. |
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/internal/util/FileImportValidatorTest.kt` | **New** (JVM/Robolectric sibling of the existing androidTest class) | 4–6 | R6 and R8: `@Config(sdk = [28])` → HEIC accepted; `@Config(sdk = [27])` → rejected with `Error.TYPE_NOT_SUPPORTED`; `image/webp` rejected on both. Robolectric's `sdk` config is what makes the API-28 gate testable without two devices. |
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/internal/camera/photo/PhotoFactoryTest.kt` | **New** | 3 | `newPhotoFromDocument` returns a photo with a real (non no-op) `edit()` for `JPEG` and `HEIC`, and a no-op `edit()` for `PNG`/`GIF` (R11). |
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/internal/camera/photo/PhotoEditTest.kt` | **Extend** | 2 | After `compressByDefault().apply()` on a HEIC-format photo, `getImageFormat()` is `JPEG` and the data is JPEG — the `PhotoCompressionModifier` format correction (R2). |
| `capture-sdk/sdk/src/androidTest/java/net/gini/android/capture/internal/util/FileImportValidatorTest.java` | **Extend** | 2 | A real `.heic` asset passes `matchesCriteria` on an API 28+ emulator, and a HEIC over the size limit still fails with `SIZE_TOO_LARGE`. Uses the existing `Helpers.getAssetFileFileContentUri` pattern. |
| `capture-sdk/sdk/src/androidTest/java/net/gini/android/capture/internal/fileimport/ImportHeicUriTest.kt` | **New** | 4–6 | The end-to-end conversion on a real device/emulator (API 28+), which Robolectric cannot do because it has no HEIF decoder: R2 (result mime `image/jpeg` + JPEG SOI marker), R3 (decoded JPEG has the same pixel dimensions as the source HEIC), R4 (a rotated HEIC asset keeps its visual orientation), R5 (a `heic-sequence` asset yields exactly one `ImageDocument`), R7 (a truncated HEIC produces `ImportedFileValidationException`, no crash). |

Added on request during the build, beyond the plan above — end-to-end UI
coverage in `bank-sdk:example-app`, which drives the real screens and the real
Gini API rather than the import pipeline in isolation:

| Test | Covers |
|---|---|
| `ui/testcases/ImportPdfImageTests.kt` → `test4_uploadHeicPhoto` | The gallery path: a real `.heic` inserted into MediaStore with its true `image/heic` type, picked through the SDK's **Photos** entry, reaching the review screen. Before PP-3499 this tap produced the "type not supported" dialog. |
| `ui/testcases/OpenWithTest.kt` → `opening_heic_with_{Splash,Main,CaptureFlowHost}Activity_launches_Bank_SDK` | The "open with" / share path — the scenario both PP-3499 and the iOS PP-1430 were reported against. Asserts the **extractions screen**, so it proves the converted JPEG was accepted *and analysed* by the Gini API, not merely imported. |

`ImageUploader.copyImageToDownloads` mapped every non-JPEG extension to
`image/png`; it now resolves the real type via a new `mimeTypeOf()`. Storing the
HEIC as PNG would have made the gallery test pass without exercising HEIC at
all.

Note for anyone reproducing this by hand: a HEIC placed in **Downloads** and
opened through the chooser's **Files** entry will not appear, because that
entry is opened with `type = application/pdf`
(`FileChooserFragment.createGetPdfDocumentIntent`). HEIC import goes through
the **Photos** entry, which uses `image/*`.

New test assets (real files, committed under
`capture-sdk/sdk/src/androidTest/assets/`): a plain `.heic`, a rotated `.heic`
(EXIF orientation ≠ 1), a `.heic` burst/sequence container, and a truncated
`.heic`. iOS committed the equivalent fixture as
`CaptureSDK/GiniCaptureSDK/Tests/GiniCaptureSDKTests/Resources/iphone-heic-photo.heic`
for the same reason — their simulator ships no HEIC encoder, just as
Robolectric ships no HEIF decoder — so reuse that file as the plain `.heic`
fixture to keep the two platforms testing the same bytes.

R9 (SDK camera on a HEIC-capturing device) is verified by inspection plus
manual QA — see below.

Per `platform.md`, every new Kotlin class gets a unit test. The one new
production class, `HeicHeader.kt`, is covered by `HeicHeaderTest.kt`; every
other change is an edit to an existing class, so the rest of the coverage
obligation is met by extending those classes' tests.

### Not tested

- **R9, the SDK camera path on a HEIC-capturing device.** The CameraX pipeline
  is hard-coded to JPEG output, so there is nothing format-dependent to assert;
  automating it would test CameraX, not our code. Left to manual QA on a
  Samsung device with "HEIF picture format" enabled in the system camera.
- **Android's HEIF decoder itself.** The instrumented tests assert that our
  pipeline produces JPEG of the right size and orientation; they do not assert
  decoded pixel fidelity.
- **Devices below API 28 in CI.** The rejection is covered by the Robolectric
  `@Config(sdk = [27])` test; no API-27 emulator is added to CI for this.
- **The Gini API's handling of the uploaded document.** Out of scope — the API
  receives ordinary JPEG.

## Out of scope

- Converting PNG and GIF to JPEG. They keep their current pass-through
  behaviour (R11). Whether the SDK should normalise *all* imports to JPEG the
  way iOS does is a separate discussion.
- Adding any other format (WebP, BMP, TIFF, AVIF). R8 pins the accepted set.
  iOS scoped out the same sweep of its magic-byte table (BMP, RIFF-based WEBP)
  under PP-1430.
- **Full EXIF/TIFF/GPS preservation across the conversion.** iOS achieves it via
  `CGImageSource → CGImageDestination`, which has no Android equivalent:
  `BitmapFactory.decodeByteArray` + `Bitmap.compress` decodes to a bitmap and
  drops every embedded property. Crucially, the Android SDK *already* behaves
  this way for JPEG imports — `PhotoCompressionModifier` re-encodes and
  `MutablePhoto.updateExif` then writes back only Gini's own required tags and
  user comment. So HEIC is being brought to parity with the existing Android
  JPEG path (orientation preserved, R4), not to parity with iOS's metadata
  handling. Closing the Android/iOS metadata gap is a separate ticket that
  would change JPEG behaviour too.
- **Building our own HEIC decoder for Android 6–8 (API 23–27).** Considered and
  rejected on 2026-09-23. Two routes exist: bundle `libheif` + `libde265` as
  native `.so` files for four ABIs — which adds binary weight to every
  integrator's banking app and raises an **LGPL licensing question** for a
  closed-source SDK shipped to banks, needing Legal sign-off before any code;
  or hand-write an ISO-BMFF parser and feed the HEVC still frame to
  `MediaCodec` (available since API 21) — which means parsing the container
  ourselves and stitching the tiled grids iPhone HEICs use, on exactly the old
  devices we cannot test well. Against that, the affected user cannot even
  *take* a HEIC photo (Android added HEIC capture in API 28 too), so the only
  case is a HEIC received from someone else. Decision: ship the API 28+ gate,
  keep the clear error below it, and revisit only with real usage numbers from
  the integrators' Play Console.
- Raising `minSdk` above 23 — the cheap way to close the API 23–27 gap without
  writing a decoder, but a product decision in its own ticket.
- Any change to `FileChooserFragment`'s picker intents.
- Any change to the Gini API libraries, `health-sdk`, `internal-payment-sdk`, or
  `bank-sdk` source.
- Migrating the legacy `AsyncTask` import pipeline to coroutines.
- New or reworded error copy.

## Open questions

- **Orientation mechanism (design item 7, marked LOW).** Whether
  `android.media.ExifInterface` returns the orientation tag for a HEIF stream
  on API 28+ needs confirming on a device. The instrumented R4 test settles it;
  if it fails, rotate the decoded `Bitmap` during conversion instead. This does
  not affect the entry point, the architecture, or the public API impact.
- **Resolved, recorded for traceability:** whether the Gini backend accepts HEIC
  directly. It does not — established on the iOS side under PP-1430 and stated
  in `GiniImageDocument.init`. This is why conversion is mandatory rather than a
  preference, and it is no longer an open point on either platform.
- **iOS left one question open that does not apply here:** whether a
  HEIC-originating document is safe to mark reviewable, because their review
  screen renders with `UIImage(data:)`. On Android the preview is built by
  `ImmutablePhoto.createPreview()` *after* the bytes are already JPEG, so the
  review and multi-page screens never see HEIC. No action needed — noted so a
  reviewer comparing the two specs does not go looking for it.
- **Release scope.** Whether this ships in the next `capture-sdk` minor (with
  `capture-sdk:default-network` version-bumped alongside it, per `RELEASE.md`)
  or waits for a batched release is a release-planning decision, not a design
  one.

## Implementation plan

Decision recorded 2026-09-23: orientation is handled by reading EXIF and
setting `rotationForDisplay`, matching the existing JPEG path (spec Open
question resolved).

- [x] 1. `capture-sdk:sdk` — add the four HEIC/HEIF constants to
      `internal/util/MimeType.java` (R1, R12)
- [x] 2. `capture-sdk:sdk` — new `internal/util/HeicHeader.kt` (ISO-BMFF
      `ftyp` + five HEIF brands) with `HeicHeaderTest.kt` (R12)
- [x] 3. `capture-sdk:sdk` — `ImageFormat.HEIC` + mime mapping in
      `document/ImageDocument.java`, extending `ImageDocumentTest.kt` (R1, R2)
- [x] 4. `capture-sdk:sdk` — accept HEIC in
      `internal/util/FileImportValidator.java`, gated on API 28, with the
      header fallback; new Robolectric `FileImportValidatorTest.kt`
      (R1, R6, R8, R12)
- [x] 5. `capture-sdk:sdk` — `Photo.setImageFormat` +
      `PhotoCompressionModifier` sets JPEG after re-encoding, extending
      `PhotoEditTest.kt` (R2)
- [x] 6. `capture-sdk:sdk` — route HEIC to `MutablePhoto` in
      `internal/camera/photo/PhotoFactory.java`, new `PhotoFactoryTest.kt`
      (R2, R11)
- [x] 7. `capture-sdk:sdk` — read the HEIC EXIF orientation in
      `internal/fileimport/AbstractImportImageUrisAsyncTask.java` and set it on
      the photo before compression (R4)
- [x] 8. `capture-sdk:sdk` — re-pin `capture-sdk/sdk/api/sdk.api` (public API)
- [x] 9. `capture-sdk:sdk` — instrumented tests + real `.heic` fixtures for the
      end-to-end conversion (R2, R3, R4, R5, R7)

### Found while building

The R7 instrumented test failed on its first run against a real device and
exposed a defect the spec had not anticipated: `PhotoCompressionModifier.modify`
returns silently when `BitmapFactory.decodeByteArray` cannot decode the data
(`PhotoCompressionModifier.java:39-41`). A truncated HEIC therefore kept its
original bytes *and* its HEIC format, and was handed to the upload as
`image/heic` — the exact silent failure the ticket rules out, on a backend that
rejects HEIC. Fixed in
`AbstractImportImageUrisAsyncTask.processImageUri`: after compression, a photo
still reporting `ImageFormat.HEIC` means the conversion did not happen, and the
image is refused through the existing halt-on-error path with
`Error.TYPE_NOT_SUPPORTED`. JPEG, PNG and GIF are unaffected — only the HEIC
format is checked.

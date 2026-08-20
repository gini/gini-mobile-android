# PP-3457: [On device] Investigate ways to encrypt models so no one can use them

Status: draft
Ticket: https://ginis.atlassian.net/browse/PP-3457
Type: **Spike** — timeboxed to 2 days (per ticket comment). The deliverable is an
investigation report (this document's Design section, finalized) **plus a
standalone proof-of-concept project**, not a production feature in this monorepo.

## Problem

The Gini On-Device SDK ships proprietary ML models inside the app package: a
LiteRT (TFLite) corner-detection model and ML scoring models for recipient and
amount extraction (see the [Android handover doc](https://ginis.atlassian.net/wiki/spaces/Product/pages/1480163335)).
Anyone can today unzip an APK/AAR containing the SDK and reuse those `.tflite`
files — the models are Gini IP and represent significant training investment.

iOS has a first-party answer (Core ML model encryption, documented in the
[iOS POC handover, §10](https://ginis.atlassian.net/wiki/spaces/Product/pages/1673625601)),
but it ties keys to a single App ID — already flagged there as problematic for
an SDK embedded in many host apps. **Android has no first-party equivalent for
LiteRT/TFLite at all.** This spike investigates custom approaches for Android.

Decisions taken during planning (with the ticket assignee):

1. **Doc-based investigation** — the on-device SDK code lives in a separate
   private repo and is not required; the investigation is grounded in the
   Confluence handover docs and LiteRT/Android platform knowledge.
2. **Deliverable = report + PoC code.**
3. **Threat model = licensing enforcement**, not just anti-extraction: the
   models should only run for authorized Gini customers (tied to client
   credentials / backend authorization). Absolute protection is impossible —
   the model must exist in plaintext in RAM to run — so the goal is raising
   the bar and gating usage, with residual risks documented.
4. **One-time authenticated key fetch is acceptable** (like existing Gini SDKs
   authenticate at init). Only the *document processing* must stay 100%
   on-device; SDK initialization may talk to the Gini API.

## Requirements

The requirements below define the PoC's observable behavior (they are what
/gini-build implements and tests) plus the report's completeness bar.

- **R1 (MUST, entry):** Given the PoC app is installed with an AES-GCM-encrypted
  stand-in `.tflite` model in its assets and no cached key, when the user taps
  "Initialize", then the PoC fetches the model key from the simulated key
  server (local MockWebServer/embedded HTTP stub) using a bearer token, and the
  UI state transitions to `Ready` exposing the loaded model's input/output
  tensor shapes read from the live LiteRT interpreter.
- **R2 (MUST, happy):** Given the PoC is `Ready`, when the user runs inference
  on a bundled sample input, then the output equals the output of the same
  stand-in model run *unencrypted* (reference values captured at PoC build
  time and asserted in tests) — proving the decrypt-to-memory path is
  lossless, not just non-crashing.
- **R3 (MUST, error):** Given the simulated key server returns HTTP 403 (an
  unauthorized customer), when the user taps "Initialize", then the UI state
  becomes `Error(ModelKeyRejected)`, no LiteRT interpreter is created, and no
  decrypted bytes are produced.
- **R4 (MUST, error):** Given a correct key but a tampered encrypted model
  asset (any flipped byte), when initialization runs, then AES-GCM tag
  verification fails, the state becomes `Error(ModelIntegrity)`, and no
  interpreter is created.
- **R5 (MUST, happy — plaintext containment):** Given any successful
  initialization, then the decrypted model exists only in a direct in-memory
  `ByteBuffer` handed to the LiteRT interpreter — the PoC has no code path
  that writes decrypted model bytes to any file, cache dir, or external
  storage. (Verified by test asserting the app's writable dirs contain no new
  files after init+inference, and by the crypto API returning `ByteBuffer`
  only, never a path.)
- **R6 (MUST, async):** Given the user taps "Initialize", then key fetch,
  decryption, and interpreter creation run off the main thread; while pending
  the UI state is `Loading`, and completion arrives as a `StateFlow` emission
  (`Ready` or `Error(...)`).
- **R7 (MUST, happy — offline re-run):** Given one successful key fetch, when
  the app is relaunched with the network stub disabled, then initialization
  still succeeds using the model key wrapped by an Android Keystore key and
  persisted at first fetch — demonstrating "one-time fetch, offline
  thereafter".
- **R8 (SHOULD, entry):** Given a fresh install in airplane-mode conditions
  (key server unreachable, no cached key), when the user taps "Initialize",
  then the state becomes `Error(KeyUnavailable)` naming the retry path —
  documenting the licensing trade-off: first init requires connectivity.
- **R9 (MUST, report):** The finalized Design section of this document
  evaluates every candidate approach listed under "Options to evaluate" with
  concrete pros/cons, states a recommendation for the on-device SDK, and
  records residual risks (memory dump on rooted devices, key extraction
  effort) — so the team can decide the follow-up implementation ticket.

## Affected modules

**None in `gini-mobile-android`.** The on-device SDK lives in a separate
private repository; this monorepo gains only this spec file.

The PoC is a standalone Android project created as a sibling folder:
`/Users/mahdiabolfazli/AndroidStudioProjects/gini-ondevice-model-encryption-poc`
(single `app` module + a `tools/` folder with the model-encryption script).
It is throwaway spike code — never published, never merged into the monorepo.

## Public API impact

**None.** No published Gini module changes. The report proposes a *future*
Gini API backend endpoint (per-customer model-key delivery) and a future
on-device-SDK init parameter, but proposing them is paper-only within this
spike.

## Technical conventions

Grounded in `platform.md`, adapted for a standalone PoC that cannot depend on
monorepo modules:

1. **Language:** Kotlin only, coroutines for async. No Java. Visibility
   discipline (`internal`) is moot in a standalone app but classes stay
   `internal` by habit except the Activity/Application.
2. **UI:** Single-screen Jetpack Compose (Material 3) with light/dark
   `@Preview`s. `GiniTheme` is *not* available outside the monorepo — the PoC
   uses a plain `MaterialTheme`; this deviation is deliberate and confined to
   the PoC. No XML layouts.
3. **Architecture:** MVVM — one `ModelPoCViewModel` (Jetpack `ViewModel`)
   exposing `StateFlow<ModelUiState>` where
   `ModelUiState = Idle | Loading | Ready(tensorInfo) | Error(reason)` (sealed
   interface), matching the AGENTS.md default. No MVI/Orbit (no precedent to
   match here).
4. **DI & async:** Manual constructor wiring (like health-sdk /
   internal-payment-sdk) — no Koin/Hilt in the PoC. `CoroutineDispatcher`
   injected, never hardcoded `Dispatchers.IO` at use sites. `StateFlow` set
   via `.value =`. Follows `.claude/rules/coroutines-flow.md` (no
   `CancellationException` swallowing, `withContext` at data-source
   boundaries).
5. **Strings/resources:** default `values/` locale only — PoC is not shipped.
6. **Quality gates:** the standalone PoC is outside the monorepo's
   ktlint/detekt/Sonar wiring; it applies the ktlint Gradle plugin locally and
   keeps default rules green. Every new Kotlin class gets a unit test (see
   Test plan).
7. **Crypto conventions:** AES-256-GCM via `javax.crypto` (no third-party
   crypto lib); Android Keystore (`AndroidKeyStore` provider) for the local
   key-wrapping key, generated with `KeyGenParameterSpec` (GCM, no user auth
   required). Key material never logged.
8. **Model dependency:** LiteRT via the version the handover doc names —
   `com.google.ai.edge.litert` (confidence: LOW — the exact artifact
   coordinates and whether the private repo uses `org.tensorflow:tensorflow-lite`
   legacy artifacts instead must be confirmed during the PoC; both expose the
   same `Interpreter(ByteBuffer)` surface).

## Design

### Established constraints (from the handover docs)

- Models: 1 corner-detection CNN (`homography_final_nhwc_std.tflite` lineage,
  input `[1,352,224,3]`) + 2 small scoring models (amount, recipient). All
  proprietary; ML Kit OCR is Google's and needs no protection.
- The SDK is a library embedded in many host banking apps → any scheme keyed
  to a single application identity (the Core ML approach's weakness flagged in
  the iOS doc) does not fit. Keying to the **Gini customer's credentials**
  fits the existing SDK auth model (client id/secret against the Gini user
  center) and gives licensing enforcement for free on both platforms.
- APK size is already a known challenge
  (https://ginis.atlassian.net/wiki/spaces/Product/pages/1420328974) — an
  approach that moves models out of the APK can solve size *and* protection
  together.

### Options to evaluate (R9 — the report finalizes this table)

1. **Baseline / do nothing beyond obfuscation** — rename assets, strip TFLite
   metadata. Rejected up front: trivially defeated, no licensing.
2. **Encrypt at rest, key embedded/derived on device (fully offline)** —
   AES-GCM encrypted asset, key derived in native code (NDK) from split
   constants. Raises the bar for casual extraction only; key is recoverable by
   static/dynamic analysis; **no licensing enforcement** → fails the chosen
   threat model, documented as the fallback if the network constraint ever
   hardens to "strictly offline".
3. **Encrypt at rest + one-time authenticated key fetch + Keystore wrapping
   (recommended candidate, what the PoC proves)** — see flow below. Licensing:
   the key endpoint authorizes per customer; revocation = stop serving the key
   (existing installs keep cached keys — residual risk to document).
4. **Server-hosted encrypted model download (Play-Asset-Delivery-style /
   Firebase-Model-Downloader-style)** — don't ship models in the artifact at
   all; download the *encrypted* model once at init from Gini infrastructure,
   then option-3 mechanics. Solves APK size too. Costs: first-init download
   size, model hosting. (confidence: LOW — Firebase ML Model Downloader's
   auth granularity and Play Asset Delivery's applicability to a *library*
   rather than an app must be checked during the spike; the custom
   Gini-hosted variant has no such dependency.)
5. **Commercial packers (DexGuard/AppSealing asset encryption)** — evaluated
   on paper only: cost, host-app-build intrusiveness (the *integrator* would
   have to run the protection, which Gini can't mandate), no licensing.
6. **Hardening add-ons (composable with 3/4):** Play Integrity API gate before
   key release; key-unwrap and decrypt in native code; model watermarking
   (train-time output fingerprinting) so leaked models are attributable.
   Paper evaluation only in this spike.

### PoC architecture (option 3 mechanics)

```
ModelPoCViewModel (StateFlow<ModelUiState>)
  └─ ModelProvisioningUseCase (suspend)
       ├─ ModelKeyRepository
       │    ├─ RemoteKeySource      — OkHttp against simulated key server
       │    │                          (MockWebServer in tests; embedded stub in the app)
       │    └─ WrappedKeyStore      — wraps fetched model key with an
       │                              AndroidKeyStore AES key; persists wrapped
       │                              blob in app-private storage (R7)
       ├─ ModelCrypto               — AES-256-GCM decrypt: (encrypted asset
       │                              stream, key) → direct ByteBuffer, never a file (R5)
       └─ InterpreterFactory        — LiteRT Interpreter(ByteBuffer) (R1/R2)
tools/encrypt_model.py              — offline encryption of the stand-in model
                                      (mirrors what Gini's release pipeline would do)
```

Stand-in model: a small public `.tflite` model checked into the PoC (the
crypto path is model-agnostic; Gini's real models stay in the private repo).
Reference inference outputs are captured once from the unencrypted model and
asserted against the encrypted-path outputs (R2).

(confidence: LOW — that LiteRT's `Interpreter` accepts a direct non-file
`ByteBuffer` on the artifact version chosen is exactly what the PoC exists to
verify; if only `MappedByteBuffer` from a file were accepted, R5 would need an
in-memory-fd (`memfd_create`/`ASharedMemory`) fallback, which the report must
then cover.)

### Proposed production flow (report content, not built in this spike)

Release pipeline encrypts models before packaging → on-device SDK init
(already async) fetches the model key from a new Gini API endpoint using the
integrator's existing client-credentials auth → key wrapped into Keystore →
document processing stays 100 % offline. Cross-platform note for the report:
the same customer-credential-keyed endpoint resolves the iOS doc's open "we
are not Apps" concern better than per-App-ID Core ML keys.

## Test plan

All tests live in the standalone PoC project. Stack mirrors the monorepo's
newer-module conventions (platform.md): JUnit4, MockK, Google Truth, Turbine,
`kotlinx-coroutines-test`, MockWebServer, Robolectric where a `Context` is
needed; instrumented tests in `src/androidTest` with AndroidJUnitRunner.

All test classes are new (standalone project — nothing to extend).

- `ModelCryptoTest` (unit, ~5 tests): encrypt→decrypt roundtrip equals
  plaintext (R2 support); wrong key → `AEADBadTagException` mapped to
  `ModelIntegrity`/`ModelKeyRejected` domain error; tampered ciphertext byte →
  failure (R4); output is a direct `ByteBuffer` (R5); empty/truncated asset →
  defined error.
- `ModelKeyRepositoryTest` (unit, MockWebServer + Robolectric, ~6 tests):
  200 → key parsed and wrapped-persisted (R1, R7 persistence half);
  403 → `ModelKeyRejected` (R3); IOException/no cache → `KeyUnavailable` (R8);
  cached wrapped key used without network call (R7); bearer token attached;
  wrapped blob is not the raw key (Keystore wrapping actually applied).
- `ModelPoCViewModelTest` (unit, Turbine + `kotlinx-coroutines-test`,
  ~5 tests): Idle→Loading→Ready emission order (R6); each error type surfaces
  as its `Error(reason)` state (R3, R4, R8); work runs on the injected
  dispatcher (main thread never blocked); no re-fetch when already `Ready`.
- `EncryptedModelInferenceTest` (instrumented, `src/androidTest`, ~3 tests):
  full pipeline on device/emulator — encrypted asset → real Keystore wrap →
  LiteRT interpreter runs and output matches the recorded reference within
  float tolerance (R1, R2, R7 across process restart simulated by clearing
  the in-memory cache); app-writable dirs gain no files during init+inference
  (R5).

Every new Kotlin class gets a unit test; `InterpreterFactory` is covered by
the instrumented test (LiteRT native libs don't load under Robolectric).

### Not tested

- The simulated key server's stub itself (test fixture, not product code).
- Android Keystore internals and AES-GCM correctness (platform/JCA behavior).
- Compose UI of the PoC screen (throwaway spike UI; state machine is fully
  covered at the ViewModel level).
- Memory-dump resistance — explicitly *not testable*: a rooted device can
  always dump the in-RAM plaintext model. Recorded as accepted residual risk
  in the report (R9), not a test gap.
- Real Gini API integration, Play Integrity, native-code key derivation —
  paper-only options in this spike.

## Out of scope

- Any change to `gini-mobile-android` beyond this spec file.
- Integrating encryption into the actual on-device SDK (separate private
  repo) — that is the follow-up implementation ticket this spike informs.
- Building the real Gini API model-key endpoint (backend team; report
  proposes the contract).
- iOS work (the iOS doc already covers Core ML encryption; only the
  cross-platform licensing recommendation references it).
- Model watermarking, Play Integrity wiring, NDK key derivation (evaluated on
  paper, not built).
- Revocation/rotation of already-cached keys (documented as residual risk).

## Open questions

- Exact LiteRT artifact/version used by the private on-device repo
  (`com.google.ai.edge.litert` vs legacy `org.tensorflow:tensorflow-lite`) —
  confirm with the on-device repo owner; PoC will use the current LiteRT
  artifact and note any API delta.
- Whether LiteRT `Interpreter` accepts a non-file direct `ByteBuffer` on that
  version (PoC verifies; fallback design noted in Design).
- Feasibility/auth granularity of Firebase ML Model Downloader and Play Asset
  Delivery for a *library* artifact (spike research task, option 4).
- Product decision the report must tee up, not answer: is "first init requires
  connectivity" (R8) acceptable messaging for a privacy-marketed on-device
  SDK?

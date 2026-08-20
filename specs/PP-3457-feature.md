# PP-3457: [On device] Investigate ways to encrypt models so no one can use them

Status: investigation complete — report (R9) finalized 2026-08-20; PoC (R1–R8) pending via /gini-build
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
8. **Model dependency:** LiteRT — the PoC uses the current artifact
   `com.google.ai.edge.litert:litert` (2.2.0 on Google Maven as of 2026-08-20;
   verified this spike, see Design → Research findings). The legacy
   `org.tensorflow:tensorflow-lite` line is frozen at 2.17.0 — still published
   and functional, but Google states all future updates are LiteRT-only. Both
   expose the same `Interpreter(ByteBuffer)` surface (verified). Which of the
   two the private on-device repo actually uses remains an open question for
   its owner; the PoC notes any API delta.

## Design

### Established constraints (from the handover docs, re-read 2026-08-20)

- Models: 1 corner-detection CNN (`homography_final_nhwc_std.tflite` lineage,
  input `[1,352,224,3]`) + 2 small scoring models (amount, recipient). All
  proprietary; ML Kit OCR is Google's and needs no protection. IBAN and
  reference extraction are regex/rule-based — no model — so exactly **three
  files** need protecting.
- The current on-device SDK makes **zero network calls** (a marketed privacy
  property). The one-time authenticated key fetch at init is the deliberate,
  already-decided exception (planning decision 4); document processing stays
  100 % on-device.
- The SDK is a library embedded in many host banking apps → any scheme keyed
  to a single application identity (the Core ML approach's weakness flagged in
  the iOS doc §10: "we are not Apps, the bundle can be different for each App
  Host") does not fit. Keying to the **Gini customer's credentials** fits the
  existing SDK auth model (client id/secret against the Gini user center) and
  gives licensing enforcement for free on both platforms.
- iOS parity fact worth noting: Apple's own Core ML encryption fetches the
  model key **from Apple's servers at first model load** — "first init needs
  connectivity" already exists in the first-party iOS scheme, so a Gini key
  endpoint gives cross-platform parity, not a regression.
- APK size is already a known challenge
  (https://ginis.atlassian.net/wiki/spaces/Product/pages/1420328974): the
  On-Device SDK currently adds **~24.7 MB APK / ~13.3 MB bundle** (iteration
  4; down from 93.7 MB at iteration 0; Maven AAR itself is 3.1 MB) — the
  models are the bulk of that. An approach that moves models out of the APK
  can solve size *and* protection together.

### Research findings (verified this spike, 2026-08-20)

Facts the evaluation below relies on. Each was re-verified against primary
sources rather than assumed; former confidence-LOW claims are resolved here.

1. **LiteRT artifact**: the current official artifact is
   `com.google.ai.edge.litert:litert` (latest **2.2.0**, Google Maven;
   `litert-api` 2.2.0; `litert-gpu`/`litert-support` stop at 1.4.2). Legacy
   `org.tensorflow:tensorflow-lite` is frozen at **2.17.0** on Maven Central —
   still functional, but "all future feature updates and performance
   enhancements will be exclusive to LiteRT" (official migration page).
   Migration is a coordinates change; the Java package stays
   `org.tensorflow.lite`.
2. **Direct ByteBuffer: RESOLVED — yes.** The `Interpreter(ByteBuffer)` API
   docs state verbatim: the buffer "can be either a `MappedByteBuffer` that
   memory-maps a model file, or a **direct `ByteBuffer` of nativeOrder() that
   contains the bytes content of a model**"; anything else (e.g. a heap buffer
   from `ByteBuffer.wrap()`) throws `IllegalArgumentException`. Decrypt-to-
   memory therefore needs no temp file and no NDK:
   `allocateDirect(...).order(nativeOrder()).put(plainBytes)` →
   `Interpreter(buffer)`. This is also the long-standing community-consensus
   pattern for encrypted TFLite models (TensorFlow issues #21501/#24139);
   Google offers **no first-party encrypted-model feature on Android**.
3. **LiteRT V2 `CompiledModel` caveat**: the Kotlin/Java `CompiledModel` API
   has **file/asset factories only** (no ByteBuffer overload), and on the V2
   Maven packages GPU acceleration requires `CompiledModel`. The C++ API does
   have `Model::CreateFromBuffer(...)`. Consequence: the decrypt-to-memory
   design works out of the box on the classic `Interpreter` API; if the real
   SDK ever needs V2 GPU/NPU acceleration, it needs a small JNI shim over
   `CreateFromBuffer` (or an in-memory fd via `memfd_create`). The handover
   doc shows no GPU-delegate use — confirm with the repo owner.
4. **Firebase ML Model Downloader: ruled out.** Firebase ML is deprecated and
   **shuts down June 15, 2027**. Auth is per Firebase *project* only (config
   ships in the app; Firebase's own docs concede "anybody can copy your
   model"); models are stored as **plain files** on device; the default
   instance belongs to the *host app's* Firebase project, and even an
   SDK-owned secondary `FirebaseApp` would be one shared static credential —
   no per-customer authorization.
5. **Play Asset Delivery: ruled out for a library.** Asset packs are Gradle
   modules of the **host app's own AAB** — a Maven-published AAR cannot ship
   one; delivery works only through Google Play (no AppGallery / MDM /
   sideload); assets are neither encrypted nor authorization-gated; and Gini
   would have to hand the raw model to every customer to bundle. At most a
   host-app documentation option for size — none for protection.
6. **Commercial packers**: DexGuard encrypts assets and does market SDK/AAR
   protection (fat-AAR since 8.7), but whether asset encryption is available
   in *library* mode is **unverified** publicly; Licel **DexProtector**
   explicitly accepts AARs and its resource encryption names "models";
   Promon "Shield for SDKs" is vendor-side (its SAROM asset encryption's
   availability in the SDK product is unverified); AppSealing/DoveRunner and
   Verimatrix wrap the *finished APK/AAB* → only the host app could apply
   them. All are enterprise-quote-priced. OWASP MASTG's consensus applies to
   the whole class: on-device-only protection is resilience, not prevention —
   generic Frida unpacking of packer-encrypted assets is well documented.
7. **Play Integrity API**: an official **SDK-specific flow exists** ("Use the
   Play Integrity API in your SDK") — Gini would register its own Google
   Cloud project via the Play SDK Console, the SDK requests tokens from
   inside the host app, and Gini's backend verifies them. Default quota is
   **10,000 requests/day across all host apps** (increases requestable);
   online-only (no-network → fail after retries); devices without Google
   Play services (Huawei/AppGallery, some MDM builds) cannot produce tokens.
   The verdict attests the *host app's* package + device integrity — which is
   exactly the useful signal for gating key release.
8. **Industry precedent** for the recommended pattern: Microblink BlinkID
   downloads its ML resources from vendor servers at runtime *by default*,
   gated by per-customer licenses; Regula Document Reader downloads its
   database from Regula servers under per-customer licensing; Anyline serves
   assets from its CDN under license keys. Vendor-hosted delivery + license
   gating is the established approach among comparable on-device ML SDK
   vendors; notably, none of them documents at-rest encryption of the models.
9. **Model watermarking**: an established research field (white-box weight
   watermarks readable from a leaked file; black-box trigger sets), with
   .tflite-relevant work on robustness to quantization and tiny networks —
   but no turnkey tooling, and removal attacks exist. Practical as a
   *forensic/attribution* measure, not a preventive control.

### Options to evaluate (R9 — finalized)

| # | Option | Extraction resistance | Licensing enforcement | Integration effort (SDK-in-many-hosts) | Offline behavior | APK size | Cost / ops |
|---|---|---|---|---|---|---|---|
| 1 | Obfuscation only | None (unzip still works) | None | Trivial | Fully offline | Unchanged | None |
| 2 | Encrypt at rest, embedded/NDK-derived key | Low (key ships in artifact) | **None** | Low | Fully offline | Unchanged | None |
| 3 | **Encrypted asset + authenticated key fetch + Keystore wrap** | Medium (nothing usable in artifact; runtime attack remains) | **Yes — per customer** | Low–medium (fits existing auth) | Offline after one first-init fetch | Unchanged (~24.7 MB) | 1 backend endpoint + pipeline step |
| 4 | Gini-hosted encrypted model download (3 + out-of-artifact) | Medium (same as 3) | **Yes — per customer** | Medium | Offline after first-init download | **Bulk of ~24.7 MB removed** | Hosting/CDN + model versioning ops |
| 5 | Commercial packers | Medium at best (key in artifact; AAR asset-encryption unverified) | None | High (vendor lock-in; or host-app-applied = unmandatable) | Fully offline | Unchanged | Enterprise annual license (quote-only) |
| 6 | Hardening add-ons (composable with 3/4) | Raises attacker cost | Strengthens key-release gate | Medium | First init online (like 3) | Unchanged | Low–medium |

**Option 1 — baseline obfuscation.** Rejected, as anticipated: renamed assets
and stripped metadata are defeated by unzipping and trying the file in an
interpreter; provides zero licensing. (Asset *name* obfuscation is all that
e.g. zShield documents for resources — content stays plaintext.)

**Option 2 — encrypt at rest, key embedded/derived on device (fully
offline).** Pros: no network dependency, no backend work, stops the
"unzip the APK and reuse the .tflite" attack. Cons: the key (or its
derivation) necessarily ships in every artifact and is identical for all
customers; static analysis or one Frida-instrumented run on any device
recovers key and model (OWASP MASTG: on-device-only protection is resilience,
never prevention); **no licensing enforcement** → fails the chosen threat
model. Kept only as the documented fallback if the network constraint ever
hardens to "strictly offline, even at init".

**Option 3 — encrypted asset in artifact + one-time authenticated key fetch +
Keystore wrapping. RECOMMENDED (what the PoC proves).**

- *Extraction resistance:* the AES-256-GCM-encrypted asset in the AAR/APK is
  useless without the key, and the key never ships — it is served only after
  client-credentials auth. Static analysis of any artifact yields nothing.
  A runtime attack on an *authorized* install still recovers the plaintext
  (residual risks below) — the bar moves from "unzip an APK" to "instrument a
  legitimately licensed app on a rooted device".
- *Licensing enforcement:* the key endpoint authorizes per Gini customer
  using the existing client-credentials model; unauthorized integrators
  cannot initialize at all. Revocation is immediate for new installs /
  uncached devices; cached keys survive (risk 3 below).
- *Integration effort:* the smallest of the licensing-capable options.
  Verified enabler: `Interpreter` accepts a direct `nativeOrder()`
  `ByteBuffer` (finding 2), so no temp files, no NDK requirement, no new
  runtime dependency. One new backend endpoint; one release-pipeline
  encryption step; SDK init gains one authenticated call.
- *Offline:* first init requires connectivity (R8); thereafter the model key
  is wrapped by a non-exportable Android Keystore key and cached — fully
  offline (R7). iOS's first-party Core ML encryption has the same first-load
  connectivity property, so this is cross-platform parity.
- *APK size:* unchanged.
- *Cost/ops:* no third-party licensing; one endpoint plus key management,
  with the same SLA needs as the existing user-center auth.
- *Implementation constraint to carry forward:* works on the classic
  `Interpreter` API; if the SDK later needs LiteRT V2 GPU/NPU via
  `CompiledModel`, a JNI shim over C++ `Model::CreateFromBuffer` (or
  `memfd_create`) is required, since the Kotlin `CompiledModel` API is
  file/asset-only (finding 3).

**Option 4 — server-hosted encrypted model download.** Three variants:

- *Firebase ML Model Downloader:* **ruled out** (finding 4) — deprecated with
  a June 15, 2027 shutdown, project-level auth only, plaintext storage on
  device, and the wrong trust anchor (host app's Firebase project; an
  SDK-owned project is still one shared static credential).
- *Play Asset Delivery:* **ruled out** (finding 5) — asset packs exist only
  as host-app AAB modules, Play-only distribution, no encryption or
  authorization; the raw model would be handed to every customer anyway.
- *Custom Gini-hosted download:* **viable and attractive.** Don't ship models
  in the artifact; the SDK downloads the *encrypted* models once at init from
  Gini infrastructure under the same client-credentials auth, then option-3
  mechanics take over (Keystore-wrapped key, decrypt to memory). One
  mechanism solves protection *and* the ~24.7 MB size problem, and decouples
  model updates from SDK releases. This is also the industry-standard pattern
  among comparable vendors (Microblink, Regula, Anyline — finding 8).
  Costs: model hosting/CDN with production SLA, first-init download
  (order 10–20 MB, possibly on metered connections), and a model↔SDK
  version-compatibility matrix to operate.

**Option 5 — commercial packers.** Rejected as the primary mechanism.
Host-app-applied wrappers (AppSealing/DoveRunner, Verimatrix) are applied to
the finished APK/AAB — Gini cannot mandate that integrators run them, and
their arbitrary-asset encryption is unverified. Vendor-side SDK protection
does exist (DexGuard AAR mode — asset encryption in that mode unverified;
Licel DexProtector explicitly protects models inside AARs; Promon Shield for
SDKs), but the decisive cons hold regardless of vendor: the decryption
key/white-box ships inside the artifact → same runtime-defeat class as
option 2 (documented generic Frida unpacking), **no per-customer licensing**,
enterprise-quoted annual cost, and build-pipeline lock-in. At most a
complementary hardening layer on top of option 3 — never a replacement.

**Option 6 — hardening add-ons (composable with 3/4; paper evaluation).**

- *Play Integrity gate before key release* — feasible for SDKs specifically
  via the official Play SDK Console flow (finding 7): Gini's backend releases
  the model key only to requests carrying a verdict of "unmodified,
  Play-recognized host app on an integrity-passing device". Cheap to add
  because it runs only at first init, which is online anyway. Caveats to
  resolve before adoption: the 10,000 req/day default quota across *all*
  host apps must be sized/raised, and a fail-open-vs-fail-closed policy is
  needed for no-GMS channels (Huawei AppGallery, some MDM builds).
- *NDK key-unwrap/decrypt* — moves unwrap+decrypt out of easily-decompiled
  Kotlin into native code; raises the static-analysis bar cheaply; does not
  stop instrumentation. Worth doing in the real SDK; deliberately out of the
  PoC.
- *Model watermarking* — established research, no turnkey tool (finding 9).
  Its value here is forensic: a leaked model becomes attributable (ideally
  per-customer via distinct watermarked builds), which is the only technical
  answer to the "malicious customer" residual risk. A training-pipeline
  follow-up for the ML team, not an SDK feature.

### Recommendation (R9)

**Primary: Option 3** — AES-256-GCM-encrypted models in the artifact, a
one-time per-customer authenticated key fetch at init against a new Gini API
endpoint, Keystore-wrapped key caching for offline re-runs, decrypt only to a
direct in-memory `ByteBuffer`. In the real SDK, add two option-6 hardenings:
native (NDK) unwrap/decrypt, and — pending quota sizing and a no-GMS-channel
policy — a Play Integrity gate on key release.

**Runner-up: Option 4 (custom Gini-hosted variant)** — identical crypto and
the same key endpoint, with the encrypted models moved out of the artifact.
Not more secure than option 3; its extra value is the ~20 MB size win and
release-decoupled model updates, paid for with hosting/versioning ops.
Design the option-3 key endpoint so the model-download endpoint is an
additive extension, not a redesign.

Trigger conditions that would change the choice:

- **Escalating APK-size pressure** from integrators, or a need to ship model
  updates between SDK releases → move to option 4.
- **Product rules first-init connectivity unacceptable** for the
  privacy-marketed SDK → fall back to option 2, explicitly accepting
  deterrence-only protection and no licensing enforcement. (The iOS parity
  fact — Apple's own scheme needs first-load connectivity — argues against
  this trigger firing.)
- **The private repo needs LiteRT V2 GPU/NPU (`CompiledModel`)** → add the
  JNI `CreateFromBuffer` shim to the design; recommendation itself unchanged.
- **Legal/product demands third-party-audited hardening** → layer
  DexProtector/Promon-class SDK protection *on top* of option 3 (after
  verifying asset encryption in AAR mode with the vendor); never instead.

### Residual risks (R9 — accepted and documented)

Absolute protection is impossible: to run, the model must exist in plaintext
in RAM. The goal achieved by option 3 is raising the attacker's cost plus
licensing enforcement — these risks remain:

1. **Plaintext model in RAM.** Any authorized install on a rooted or
   instrumented device (Frida, memory dump) yields the decrypted model at
   runtime. Inherent to on-device inference on every platform — iOS Core ML
   decrypts to memory too. Not preventable, not testable; the mitigation is
   that the *cost* moved from "unzip an APK" to "runtime-compromise a
   licensed install".
2. **Malicious or compromised customer.** The licensing gate authorizes
   customers; anyone holding a valid customer's client credentials can fetch
   the key and decrypt the asset. Enforcement is strong against
   *non-customers*; against leakage by/via a paying customer the controls are
   contractual — with per-customer model watermarking (option 6) as the
   technical attribution backstop.
3. **Cached-key revocation gap.** The Keystore-wrapped key persists on
   device, so a revoked customer's *existing* installs keep working until
   reinstall/data-clear. Mitigation knob for the implementation ticket: a key
   TTL / periodic silent re-validation when online, traded off against the
   offline promise. Documented, not solved, in this spike.
4. **Key extraction effort.** The wrapped blob is protected by a
   non-exportable Keystore key, so offline extraction of the cached key is
   not practical; but the key can be captured in transit/at use on an
   attacker-controlled authorized device — collapsing to risk 1/2: one
   authorized runtime compromise leaks model + key. Watermark + contract are
   the backstop, and Play Integrity raises the cost of doing this at scale.
5. **Operational:** the key endpoint becomes an init dependency for new
   installs (R8's "first init requires connectivity") and needs the same
   availability commitments as the existing user-center auth.

### Answers to the framing questions (R9)

**(a) Is meaningful protection possible at all?** Yes — provided "meaningful"
is defined as the chosen threat model does: static extraction from any
shipped artifact becomes worthless (encrypted bytes only), and model *usage*
becomes a licensed, per-customer, revocable-for-new-installs capability.
What is **not** possible — on Android, iOS, or anywhere — is preventing a
determined attacker with a rooted device and a licensed install from
recovering the in-RAM plaintext (OWASP MASTG consensus; risks 1/4).

**(b) If yes, what is the best approach?** Option 3, with the option-6
hardenings, and option 4's Gini-hosted delivery as the planned extension —
see Recommendation above.

**(c) Where full protection is impossible, what preserves Gini's
advantage?** Defense in depth around the one un-closable gap: the
per-customer licensing gate at key delivery (business control + revocation);
Keystore wrapping and a strict no-plaintext-on-disk rule (raise the bar);
Play Integrity gating (excludes tampered hosts and casual mass extraction);
native-code decryption (raises static-analysis effort); watermarking plus
contracts (attribution and deterrence for the residual runtime-leak class).
Strategically, Gini's moat is the training pipeline and continuous model
improvements more than any single `.tflite` snapshot — option 4's
release-decoupled updates make a stolen model a depreciating asset.

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

(confidence: RESOLVED, 2026-08-20 — the `Interpreter(ByteBuffer)` API docs
explicitly accept "a direct `ByteBuffer` of nativeOrder() that contains the
bytes content of a model"; heap buffers are rejected with
`IllegalArgumentException`. The PoC still asserts this executably (R2/R5).
The `memfd_create`/`ASharedMemory` fallback is **not** needed for the
`Interpreter` path; it becomes relevant only if the real SDK adopts the
LiteRT V2 `CompiledModel` API — e.g. for GPU/NPU — whose Kotlin factories are
file/asset-only; there the C++ `Model::CreateFromBuffer` via JNI is the
primary fallback.)

### Proposed production flow (report content, not built in this spike)

Release pipeline encrypts models before packaging → on-device SDK init
(already async) fetches the model key from a new Gini API endpoint using the
integrator's existing client-credentials auth → key wrapped into an Android
Keystore key and cached → document processing stays 100 % offline.

Sketch of the proposed endpoint contract (paper-only; backend team designs
the real one): `GET /ondevice/model-keys/{modelVersion}` with the standard
Gini bearer token; 200 → per-model key material (+ key id, optional TTL for
the revocation knob in residual risk 3); 403 → customer not licensed for
on-device extraction. Optionally accepts a Play Integrity token header once
the option-6 gate is adopted. Designed so an option-4
`GET /ondevice/models/{modelVersion}` (encrypted blob download) is a purely
additive extension later.

**Cross-platform note (iOS, per scope):** the same customer-credential-keyed
endpoint resolves the iOS handover doc's §10 open concern ("we are not Apps,
the bundle can be different for each App Host") better than per-App-ID
Core ML keys — iOS would ship AES-GCM-encrypted models and fetch the same
per-customer key at init instead of relying on Apple's App-ID-bound scheme.
One licensing mechanism, both platforms; no new iOS research was done beyond
this note.

### Key sources (verified 2026-08-20)

- LiteRT for Android / migration (artifacts, V1 vs V2, deprecation of
  `org.tensorflow:tensorflow-lite`): https://developers.google.com/edge/litert/android ,
  https://developers.google.com/edge/litert/migration
- `Interpreter(ByteBuffer)` contract: https://ai.google.dev/edge/api/tflite/java/org/tensorflow/lite/Interpreter
- Encrypted-model community pattern: https://github.com/tensorflow/tensorflow/issues/24139
- Firebase ML deprecation (shutdown 2027-06-15) + custom-model storage/auth:
  https://firebase.google.com/docs/ml ,
  https://firebase.google.com/docs/ml/android/use-custom-models
- Play Asset Delivery (app-level packs, Play-only):
  https://developer.android.com/guide/playcore/asset-delivery
- Play Integrity for SDKs (Play SDK Console flow, quotas):
  https://support.google.com/googleplay/android-developer/answer/15299193
- OWASP MASTG resilience consensus:
  https://github.com/OWASP/mastg/blob/master/Document/0x05j-Testing-Resiliency-Against-Reverse-Engineering.md
- Packers: https://www.guardsquare.com/dexguard ,
  https://licelus.com/products/dexprotector/docs/android/introduction-to-dexprotector ,
  https://promon.io/products/promon-shield-for-sdks
- Vendor precedent: https://github.com/BlinkID/blinkid-verify-android ,
  https://docs.regulaforensics.com/develop/doc-reader-sdk/overview/licensing/
- Watermarking survey: https://arxiv.org/abs/2103.09274
- Apple Core ML model encryption (first-load key fetch, App-ID binding):
  https://developer.apple.com/documentation/coreml/encrypting-a-model-in-your-app

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

Resolved during this spike (2026-08-20):

- ~~Whether LiteRT `Interpreter` accepts a non-file direct `ByteBuffer`~~ —
  **RESOLVED: yes**, explicitly documented ("a direct `ByteBuffer` of
  nativeOrder() that contains the bytes content of a model"); heap buffers
  throw `IllegalArgumentException`. No file-based fallback needed on the
  `Interpreter` API.
- ~~Feasibility/auth granularity of Firebase ML Model Downloader and Play
  Asset Delivery for a library~~ — **RESOLVED: both ruled out** (Firebase ML
  deprecated, shutdown 2027-06-15, project-level auth, plaintext at rest;
  PAD packs are host-app AAB modules, Play-only, unencrypted). The viable
  option-4 variant is the custom Gini-hosted download.
- ~~Current LiteRT artifact coordinates~~ — **RESOLVED**:
  `com.google.ai.edge.litert:litert` 2.2.0 (Google Maven); legacy
  `org.tensorflow:tensorflow-lite` frozen at 2.17.0.

Still open:

- Which artifact/version the **private on-device repo** actually uses
  (current LiteRT vs legacy TFLite), and whether it uses or plans **GPU/NPU
  acceleration** — the latter decides if the JNI `CreateFromBuffer` shim is
  needed (LiteRT V2 `CompiledModel` has no Kotlin in-memory factory). Confirm
  with the repo owner. (confidence: n/a — needs owner input, not research.)
- Product decision the report tees up, not answers: is "first init requires
  connectivity" (R8) acceptable messaging for a privacy-marketed on-device
  SDK? Supporting fact: Apple's first-party Core ML encryption has the same
  first-load connectivity requirement.
- Only if the option-6 Play Integrity gate is adopted: quota sizing beyond
  the 10,000 req/day default, and the fail-open/fail-closed policy for
  no-GMS distribution channels (Huawei AppGallery, MDM builds).
- Only if packer layering is ever pursued: vendor confirmation that asset
  encryption is available in AAR/library mode (unverified publicly for
  DexGuard and Promon; Licel DexProtector documents it).

# Architecture — shared invariants

Canonical architecture rules for the Gini Android SDKs: module graph, published API surface, facades,
layering, DI boundaries, build architecture. Owned by **`architecture-specialist`**; `gini-review/platform.md`
§5 and `code-reviewer` point here — edit rules here, nowhere else. `AGENTS.md` stays canonical: if the two
disagree, `AGENTS.md` wins and the drift is reported to the user.

## Repo reality (do not flag existing code over this)

In a diff review these are never findings. In a full audit, report one only with a concrete failure, as a
nit marked "known existing pattern".

- **Koin is in the published surface.** `bank-sdk/sdk/api/sdk.api` lists `BankSdkIsolatedKoinContext.getKoin()`,
  the `getGiniBankKoin()` extension and public `Module` getters from the `*ModuleKt` file facades;
  `capture-sdk/sdk/api/sdk.api` lists `getKoin()` and `getGiniCaptureKoin()`. This contradicts `AGENTS.md`
  ("never expose Koin"), but making them `internal` is binary-breaking — a major version or a deprecation
  cycle, decided by the user, not a review finding. Re-check: `grep -n koin */sdk/api/sdk.api`.
- **Public classes in `internal` packages** exist in every module (`core-api-library`
  `internal/GiniCoreAPIBuilder.kt` is the clearest case, plus many in `capture-sdk` and
  `internal-payment-sdk`). The package name is a wish, not a visibility modifier; `apiCheck` has locked them in.
- **Public `*Kt` file facades** — top-level helpers compiled into public `…Kt` classes, e.g. `bank-sdk`
  `util/GiniBankAPI.kt` and `health-sdk` `util/GiniHealthAPI.kt` (`getGiniApi(...)`), `bank-sdk`
  `capture/util/NavigationExtension.kt`, and the Koin `*Module.kt` files above.
- **`GiniHealth` is an instance facade** (`class GiniHealth(giniHealthAPI, context)`) with a public
  `giniInternalPaymentModule` property, unlike the singleton `object GiniBank` and the static
  `GiniCapture.getInstance()`. A recorded divergence, not a defect.
- **`GiniInternalPaymentModule`** is published with its own `api/sdk.api` dump; its POM describes it as an
  internal library used by the Gini SDKs, and `health-sdk` consumes it. Treat its dump as public surface
  (`apiCheck` guards it); whether integrators call it directly is a product question for the user.
- **capture-sdk Fragment and Presenter extension files** carry flow logic outside a ViewModel:
  `analysis/AnalysisFragmentExtension.kt`, `analysis/AnalysisScreenPresenterExtension.kt`,
  `camera/CameraFragmentExtension.kt`. Known pattern; don't copy it into new screens.
- **Legacy Java MVP in capture-sdk** (`GiniCaptureBasePresenter`, `AnalysisScreenPresenter`,
  `OnboardingScreenPresenter`, `OnboardingPagePresenter`) is reviewed in place, never converted.
- **Third-party types in `core-api-library` signatures:** `okhttp3.RequestBody` on `DocumentService` and
  `DocumentRemoteSource`, `retrofit2.Response` in `ApiException.forResponse`, `protected getMoshi()` /
  `getApiRetrofit()` on `GiniCoreAPIBuilder`, and the Moshi-generated public `*JsonAdapter` classes in the
  API-library dumps. Pre-existing surface.
- **Deliberate third-party exposures that stay:** `OkHttpClient`, `okhttp3.Cache` and `TrustManager` on
  `GiniHttpClientProvider`, `DefaultGiniHttpClientProvider.Builder` and the API builders (integrators may
  supply their own client — the security agent's "integrator TLS" case); `okhttp3.Cache` on
  `GiniCaptureDefaultNetworkService.Builder.setCache`; the jsr166e `CompletableFuture` in capture-sdk
  (the Java-facing bridge); Compose types on public composables.
- **Test-only cross-aisle edge:** `bank-api-library` and `health-api-library` reference each other with
  `androidTestImplementation`, and both use `core-api-library:shared-tests`. Deliberate
  (`gini-review/platform.md` §4). Never a finding.
- **`capture-sdk:default-network` → `bank-api-library`** is the designed bridge between the capture UI and
  the Bank API. Not a cross-aisle violation.
- **Hilt in `bank-sdk:example-app`** and the example apps' edges onto the SDK modules are out of scope.
- **`consumer-rules.pro`** exists in `bank-sdk/sdk`, `capture-sdk/sdk`, `capture-sdk/default-network` and
  `health-sdk/sdk`, not in `internal-payment-sdk/sdk` or the API libraries. Whether that is deliberate is
  unconfirmed — verify on the branch and ask; never report a missing file as a finding.

## Rules

### Module graph

- The production dependency graph is a DAG with one shared root. Allowed `project(":…")` edges:
  - `health-api-library:library` → `core-api-library:library`
  - `bank-api-library:library` → `core-api-library:library`
  - `internal-payment-sdk:sdk` → `health-api-library:library`
  - `health-sdk:sdk` → `internal-payment-sdk:sdk`
  - `capture-sdk:default-network` → `bank-api-library:library`, `capture-sdk:sdk`
  - `bank-sdk:sdk` → `bank-api-library:library`, `capture-sdk:sdk`, `capture-sdk:default-network`
  - `capture-sdk:sdk` and `core-api-library:library` depend on no repo module.
  - Test helpers only: `core-api-library:shared-tests` → `core-api-library:library`.
  Re-check: `grep -nE 'project\(":' */*/build.gradle.kts`. `AGENTS.md`'s dependency chain and
  `RELEASE-ORDER.md` (generated — never hand-edited) must agree with it.
- **Two aisles, one root.** Bank aisle: `bank-api-library`, `capture-sdk`, `bank-sdk`. Health aisle:
  `health-api-library`, `internal-payment-sdk`, `health-sdk`. `core-api-library` is the only module both may
  depend on. A production edge from one aisle into the other is a **blocker** — it ships the other aisle's
  artifacts, transitive dependencies and release cadence to every integrator.
- An API library never depends on an SDK module; `core-api-library:library` never depends on a repo module.
- No `repositories {}` in a module build file (`FAIL_ON_PROJECT_REPOS` in `settings.gradle.kts`).
- A new module edge changes the release order and the downstream ripple (`gini-review/platform.md` §4).

### Published API surface

- **`internal` by default.** Kotlin is public by default and `explicitApi` is off, so every new top-level
  or member declaration without `internal`/`private` ships in `<module>/api/<module>.api` to Maven Central.
  A new public declaration needs a stated reason, KDoc, and — in the API libraries, capture-sdk and
  bank-sdk, where Java callers are supported (`@JvmStatic`/`@JvmOverloads` precedent, Java in the guides) —
  a Java-usable shape.
- Restrict visibility before touching a dump. Never run `apiDump` to make CI green; `apiCheck` runs in
  every module's CI workflow but **not** in `/gini-check`.
- **No DI, networking or JSON framework type in a new public signature:** no `org.koin.*`,
  `retrofit2.*`, `okhttp3.*`, `com.squareup.moshi.*`, Orbit types. Exceptions are only the deliberate
  exposures listed under Repo reality.
- Follow a type to its public end: an `internal` class reached through a public constructor parameter,
  property, return type or `@PublishedApi internal` is public.
- A public `inline fun <reified …>` compiles to a non-public JVM method and never appears in the
  `api/*.api` dump, so `apiCheck` does not guard it — yet it is source-public Kotlin API. Mark such
  helpers `internal` by hand (bank-sdk `di/koin/AndroidFragmentExt.kt` and
  `capture/util/PreferencesExtension.kt` are the existing public ones).
- A public type in an `internal` package is a finding only when the diff adds it.
- Removal or rename of a public declaration goes through the deprecation cycle
  (`gini-review/platform.md` §9) or a major version; parallel major branches are maintained.
- All SDK and library modules compile to JVM target 1.8 — public signatures use no Java 9+ types.

### Facades and entry points

- Every integrator-facing screen is reached through the module's facade — `object GiniBank`,
  `GiniCapture` (static `getInstance()`/`newInstance()` → `Builder`), `GiniHealth`,
  `GiniInternalPaymentModule`, the `Gini*APIBuilder` classes — and the facade returns a `Fragment`
  (`CaptureFlowFragment`, `GiniCaptureFragment`, the health payment fragment). The host app never
  instantiates a screen, an Activity or a ViewModel of the SDK.
- Configuration follows the module's existing shape: `GiniCapture.Builder`, `GiniBank`'s configuration
  setters, the API builders' `@JvmOverloads` constructors. A public setter on a module that configures
  through a `Builder` is a finding.
- A new facade that copies the `GiniHealth` instance shape instead of the singleton shape is a design
  question for the user, not a defect.

### Layering

- **ViewModels hold no `Context`, `Application`, `View`, `Fragment` or `NavController`**, and do not
  extend `AndroidViewModel`. True for every ViewModel under `*/sdk/src/main/java` today; keep it so.
  Re-check: `grep -rlE 'NavController|android\.view\.|android\.widget\.|AndroidViewModel' --include='*ViewModel*.kt' */sdk/src/main/java`.
- Business logic (parsing, decisions, networking, amount or date arithmetic) lives in a ViewModel, use
  case or repository — not in a Fragment, a Fragment extension file, or a Compose body. bank-sdk's shape:
  Orbit `ContainerHost` ViewModels, intents under `viewmodel/intent/`, use cases under
  `skonto/usecase/`, repositories under `transactiondocs/internal/repository/`.
- Navigation stays in the Fragment layer: AndroidX Navigation graphs (`gc_nav_graph.xml`,
  `gbs_nav_graph.xml`) in capture-sdk and bank-sdk; `childFragmentManager` transactions inside
  health-sdk's `PaymentFragment`. There is no `navigation-compose` in the catalog.
- A nav-graph destination whose Fragment class lives in another module makes safeargs generate its
  `<Class>Directions` in **that** module's package inside this module's artifact: bank-sdk's
  `gbs_nav_graph.xml` declares `GiniCaptureFragment` with actions, so `bank-sdk/sdk/api/sdk.api`
  publishes `net.gini.android.capture.GiniCaptureFragmentDirections` (known existing pattern). Any
  action reachable from a foreign destination — its own or a global one on an enclosing `<navigation>` —
  generates a `<Class>Directions` in the foreign package inside this module's artifact (safeargs emits a
  Directions class for every destination that has actions **or** whose parent graph has them, so a root
  global action adds one public class per destination). Treat such an action as a published-API change
  and review the `api/*.api` diff before accepting it.
- **API libraries keep one stack per resource:** Retrofit interface `*Service` → `*RemoteSource` →
  `*Repository` → `*Manager`, results wrapped in `core-api-library` `Resource`. Retrofit, OkHttp and
  Moshi objects are created in the `*RemoteSource` or the API builder, never in a `*Manager` or
  `*Repository`.
- A `Flow`-returning function is not `suspend`; repositories and use cases expose `suspend`/`Flow` and do
  not launch — the threading of it belongs to `.claude/rules/coroutines-flow.md`.

### DI boundaries

- Koin lives only inside the isolated contexts `CaptureSdkIsolatedKoinContext` (capture-sdk) and
  `BankSdkIsolatedKoinContext` (bank-sdk), bridged by `CaptureSdkDiBridge`. No `startKoin`,
  `GlobalContext`, `koinViewModel()` or global `get()`/`inject()` in any SDK or library module (the
  example apps are out of scope) — they crash in the integrator's app.
- `viewModel {}` bindings exist only in bank-sdk (resolved with `giniBankViewModel`); capture-sdk builds
  its ViewModel with an inline `ViewModelProvider.Factory` in `GiniCaptureFragment.kt` and has no
  `viewModel {}` by design — adding one is a design question, not a drive-by.
- health-sdk and internal-payment-sdk wire dependencies manually (`ViewModelProvider` in
  `integratedFlow/PaymentFragment.kt`); introducing a DI framework there is a design question for the user.
- Hilt only in `bank-sdk:example-app`. Hilt in an SDK module is a finding
  (`gini-review/platform.md` §5).
- A Koin `single {}` must not hold a `Context` that outlives the SDK's own cleanup
  (`GiniCapture.cleanup`, `GiniBank.cleanupCapture`).
- No DI type in the public API (`AGENTS.md`) — new signatures only; see Repo reality for the existing getters.

### Build architecture

- Custom Gradle logic goes into a `buildSrc` plugin under `buildSrc/src/main/kotlin/net/gini/gradle/`
  (`CodeAnalysisPlugin`, `DependencyUpdatesPlugin`, `DokkaPlugin`, `JacocoCoveragePlugin`,
  `PropertiesPlugin`, `PublishToMavenPlugin`, `ReleaseOrderPlugin`, `SBOMPlugin`), not into an ad-hoc task
  in a module build file. Known gap: `PropertiesPlugin.apply` is empty and the `injectTestProperties`
  task is copy-pasted into several module build files — a nit in a full audit, never a finding against
  a diff that merely repeats the existing copy.
- Versions come from `gradle/libs.versions.toml` (`libs.` accessors) — no hardcoded version strings.
- A repo-module edge in a releasable module's build file is declared through the SBOM-aware pair the
  file already uses: `apiProjectDependencyForSBOM`/`implementationProjectDependencyForSBOM` (from
  `buildSrc` `extensions/Project.kt`) inside the `createSBOM` branch, and plain `api`/`implementation`
  otherwise. A bare `implementation(project(":…"))` next to that branch leaves the edge out of the
  published SBOM.
- `capture-sdk:default-network` is version-bumped together with `capture-sdk:sdk`; a bump of one alone is
  a finding (`gini-review/platform.md` §3).
- A `consumer-rules.pro` change alters R8 in every integrator's app — ask why before accepting it.
- `jvmTarget` and `compileSdk`/`minSdk` move together across modules, never in one module alone.

Sources: [Kotlin library authors' guidelines](https://kotlinlang.org/docs/api-guidelines-introduction.html) ·
[Kotlin backward compatibility](https://kotlinlang.org/docs/api-guidelines-backward-compatibility.html) ·
[AndroidX API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/docs/api_guidelines/) ·
`AGENTS.md` (Project overview, Architecture & code style, Dependencies & versioning)

# XPL-443: `architecture-specialist` agent

Status: implemented
Ticket: https://ginis.atlassian.net/browse/XPL-443 (sub-task of XPL-402 — "[Android] Add 7 specialist agents to the Claude agent team")

- Source: Confluence "AI Planning" (PLMO). Rule from that page: **every agent must be valid for our repo; generic Kotlin/Android advice is not enough.**
- iOS twin: `architecture-specialist` in `gini-mobile-ios` (`origin/XPL-407-408-agents:.claude/agents/architecture-specialist.md`, local clone `~/XCodeProjects/gini-mobile-ios`).
- Android siblings: `android-security-specialist` (XPL-437) and `concurrency-specialist` (XPL-440), both on `origin/XPL-437-android-security-specialist`; `code-reviewer` (XPL-446) on `origin/XPL-446-android-code-reviewer`.
- Jira XPL-443 has an empty description; the ticket content is the briefing pasted into `/gini-plan` on 2026-10-14.

## Problem

The agent team reviews Compose, Views, accessibility, tests, concurrency and security, but nobody reviews the **shape** of the repo: whether a diff adds a module edge that must not exist, publishes a helper by omission (Kotlin is public by default and `explicitApi` is off), leaks Koin or Moshi/OkHttp types into the published surface, puts business logic in a Fragment extension, or wires a new screen outside the module's DI and facade pattern. Today the orchestrator enforces "architecture" and "DI (Koin)" inline from a one-paragraph summary, and `code-reviewer` (XPL-446) says outright that no architecture specialist exists. XPL-402 asks for one, and the iOS repo already has its twin.

## Decisions (clarifying questions, 2026-10-14)

| Question | Decision |
|---|---|
| File name | `.claude/agents/architecture-specialist.md` — no `android-` prefix, like `concurrency-specialist`. The Jira title keeps the prefix only to tell tickets apart. |
| Branch | **`XPL-443-android-architecture-specialist` stacked on `origin/XPL-446-android-code-reviewer`** (changed 2026-10-14 from "based on main"). That branch contains the XPL-437 and XPL-440 commits, so `android-security-specialist`, `concurrency-specialist` and `code-reviewer` are available locally and the three shared wiring files are edited in their latest state (orchestrator rules numbered 1–10, hook and `gini-task.md` already listing security/concurrency/code-reviewer). PR base = `XPL-446-android-code-reviewer` until it merges, then `main` (GitHub retargets when the base branch is deleted). The two "no architecture specialist yet" lines in `code-reviewer.md` are **in scope**. |
| Rule source | The agent **reads `.claude/skills/gini-review/platform.md` at review time** for published API (§1), build files (§2), module ripple (§4) and binary compatibility (§9). It does not restate them. |
| Ownership | A **new shared rules file `.claude/rules/architecture.md`**, owned by `architecture-specialist`, holds the architecture invariants (module DAG, facade shapes, layering, DI boundaries). `gini-review/platform.md` §5 gets a pointer to it. `AGENTS.md` stays canonical and owner-less; the agent proposes edits to it, never applies them. |
| Severity | `blocker` / `warning` / `nit`, same as the Android siblings. |
| `explicitApi` | Not demanded. The agent enforces `internal`-by-default only; the absence of `explicitApi` is mentioned at most once in a full audit (platform.md §9 already says so). |
| Tools | `Read, Edit, Write, Glob, Grep` — same as the siblings (XPL-402 decision). |
| Hotspots found | Listed in §Hotspots. **No code fixes in this PR.** The user decides on tickets. |

## Requirements

- **R1 (MUST, entry):** Given the repo on this branch, when a developer or the orchestrator invokes the `architecture-specialist` agent (via the `Agent` tool or `/gini-task`), then the agent defined in `.claude/agents/architecture-specialist.md` loads with frontmatter `name: architecture-specialist`, a `description` that names module graph, public API surface, facades, layering, DI boundaries and build architecture, and `tools: Read, Edit, Write, Glob, Grep`.
- **R2 (MUST, entry):** Given a diff that touches a `build.gradle.kts`, `settings.gradle.kts`, a module `gradle.properties`, `buildSrc/`, an `api/*.api` dump, a facade (`GiniBank`, `GiniCapture`, `GiniHealth`, `GiniInternalPaymentModule`, `GiniCoreAPIBuilder`/`GiniBankAPIBuilder`/`GiniHealthAPIBuilder`), a Koin module or isolated-context class, a `consumer-rules.pro`, a new public top-level declaration in an SDK module, or a new screen's ViewModel/DI wiring, when `gini-orchestrator` triages it, then the orchestrator's delegation rules name `architecture-specialist` for that diff.
- **R3 (MUST, happy):** Given a diff that adds `implementation(project(":health-api-library:library"))` to `bank-sdk/sdk/build.gradle.kts`, when the agent reviews it, then it reports one **blocker** labelled *Module Graph* that names the forbidden edge (bank aisle → health aisle) and cites `.claude/rules/architecture.md`.
- **R4 (MUST, happy):** Given a diff that adds a top-level `fun` without `internal` to a file under `bank-sdk/sdk/src/main/java`, when the agent reviews it, then it reports a **warning** labelled *Public API* naming the declaration, explains that it ships in `bank-sdk/sdk/api/sdk.api` (CI runs `apiCheck`, `/gini-check` does not), and proposes `internal` before proposing `apiDump`.
- **R5 (MUST, happy):** Given a diff that puts an `org.koin.*` type into a public signature of an SDK module, or reaches a `NavController` from a ViewModel, or constructs a Retrofit/OkHttp/Moshi type inside a `*Manager`/repository instead of its `*RemoteSource`, when the agent reviews it, then each is reported with the matching label (*DI Boundary*, *Layering*, *Layering*) and a `before` → `after` direction.
- **R6 (MUST, error):** Given a diff that touches only a capture-sdk `.java` file, a health-sdk XML `Fragment`, `GiniCaptureFragment.kt`'s `ViewModelProvider.Factory`, the bank-sdk example app's Hilt wiring, the `capture-sdk:default-network` → `bank-api-library` edge, or the `androidTestImplementation` edge between the two API libraries, when the agent reviews it, then it reports **no** finding for those facts (it says the boundaries are respected).
- **R7 (MUST, error):** Given a full audit with no diff, when the agent runs, then it applies **no** edit, reports the pre-existing patterns in §Hotspots at most as `nit` marked "known existing pattern", and ends with "applied: none".
- **R8 (MUST, happy):** Given any review, when the agent proposes a fix that changes an `api/*.api` dump, a facade signature, a module dependency edge, a `buildSrc` plugin, a `consumer-rules.pro`, a `gradle.properties` version, or anything the diff did not introduce, then it asks the user first and does not apply it.
- **R9 (MUST, entry):** Given the `UserPromptSubmit` hook in `.claude/settings.json` and `.claude/commands/gini-task.md`, when either is read, then both list `architecture` among the specialists the orchestrator routes to.
- **R10 (MUST, happy):** Given `.claude/rules/architecture.md`, when `architecture-specialist` starts a review, then its instructions tell it to read that file **and** `gini-review/platform.md` first, and to propose rule edits to `architecture.md` (never to its own instructions) when a rule proves wrong; `gini-review/platform.md` §5 points at `architecture.md`.
- **R11 (SHOULD, happy):** Given the agent file, when it is linted by the test plan, then it contains no `file:line` citation, no hard-coded count, and every class, path, Gradle task and plugin it names exists on this branch.
- **R13 (MUST, entry):** Given `.claude/agents/code-reviewer.md` on this branch, when it is read, then its escalation list names `architecture-specialist`, and neither "There is no architecture specialist in the team yet" nor "Architecture, performance and design-system questions have no specialist" remains (performance and design-system stay listed as missing).
- **R12 (MUST, error):** Given the compose, views, a11y and testing agent files, when this PR is diffed against `main`, then those four files are unchanged (XPL-402 acceptance criterion).

## Affected modules

No Gradle module changes. Files touched, all under `.claude/`:

| File | Change |
|---|---|
| `.claude/agents/architecture-specialist.md` | **new** — the agent |
| `.claude/rules/architecture.md` | **new** — shared architecture invariants, owned by the agent |
| `.claude/skills/gini-review/platform.md` | §5: one pointer paragraph to `.claude/rules/architecture.md` (file is Android-only, "NOT MIRRORED", so no iOS pair needed) |
| `.claude/agents/gini-orchestrator.md` | description, team-table row, delegation rule, "enforce inline" rule, Knowledge Sources line |
| `.claude/commands/gini-task.md` | specialist list |
| `.claude/settings.json` | hook text |
| `.claude/agents/code-reviewer.md` | flip the two "no architecture specialist yet" lines; add `architecture-specialist` to its escalation list |

`specs/XPL-443-feature.md` (this file) is a personal planning file and is **not** committed.

## Public API impact

None. No Kotlin/Java source, no `api/*.api` dump, no `gradle.properties` changes.

## What the code really does (verified on this branch, 2026-10-14)

Facts the agent must state as patterns plus the `grep`/`ls` to re-check them — never as counts.

### Module graph (`settings.gradle.kts`, `*/*/build.gradle.kts`)
- `FAIL_ON_PROJECT_REPOS` is set in `settings.gradle.kts`; `capture-sdk:screen-api-example-app` is commented out.
- Production `project(":…")` edges: `health-api-library:library` → `core-api-library:library`; `bank-api-library:library` → `core-api-library:library`; `internal-payment-sdk:sdk` → `health-api-library:library`; `health-sdk:sdk` → `internal-payment-sdk:sdk`; `capture-sdk:default-network` → `bank-api-library:library` + `capture-sdk:sdk`; `bank-sdk:sdk` → `bank-api-library:library` + `capture-sdk:sdk` + `capture-sdk:default-network`; `capture-sdk:sdk` and `core-api-library:library` depend on no repo module. Matches `AGENTS.md`.
- **Test-only cross edge:** `bank-api-library` `androidTestImplementation(project(":health-api-library:library"))` and the reverse, plus both → `core-api-library:shared-tests`. Deliberate (platform.md §4). Never a finding.
- Example apps: `bank-sdk:example-app` → `capture-sdk:sdk`, `capture-sdk:default-network`, `bank-sdk:sdk`; `health-sdk:example-app` → `health-sdk:sdk`. Out of scope.
- Re-check: `grep -nE 'project\(":' */*/build.gradle.kts`.

### Published API surface
- `binary-compatibility-validator` (catalog alias `libs.plugins.binary.compatibility.validator`, `apply false` at root) is applied in all eight releasable modules; dumps at `<project>/<module>/api/<module>.api`. Re-check: `find . -name '*.api' -path '*/api/*' -not -path '*/build/*'`.
- CI runs `<module>:apiCheck` in every `.github/workflows/<project>.check.yml`; **`/gini-check` does not run `apiCheck`** (its task list is `testDebugUnitTest lint detekt ktlintCheck`).
- `explicitApi` is enabled nowhere (`grep -rn explicitApi --include='*.kts' .` is empty).
- All SDK and library modules compile with `jvmTarget = "1.8"` / `JavaVersion.VERSION_1_8` — public signatures cannot use Java 9+ types.
- Deprecation precedent exists (`@Deprecated` in `bank-sdk` `capture/CaptureResult.kt`, `core-api-library` `DocumentRepository.kt`, `DocumentRemoteSource.kt`, `internal/GiniCoreAPIBuilder.kt`, `http/DefaultGiniHttpClientProvider.kt`).
- Parallel major branches exist: `origin/1.x.x`, `origin/2.x.x`, `origin/release/bank-sdk-4.6`, `origin/release/bank-sdk-4.7`.

### Facades and entry points
- `bank-sdk`: `object GiniBank` (`GiniBank.kt`) — `setCaptureConfiguration`, `createCaptureFlowFragment()`/`…ForIntent`/`…ForUris`/`…ForDocument` returning `CaptureFlowFragment`, `createInvoicePreviewFragment`, `setGiniApi`/`releaseGiniApi`, `cleanupCapture`. Plus public top-level helpers `getGiniApi(...)` in `util/GiniBankAPI.kt`.
- `capture-sdk`: Java `public class GiniCapture` with `static synchronized getInstance()`, `static newInstance(Context)` → `Builder`, `static cleanup(Context)`, `static createGiniCaptureFragment()`, `createGiniCaptureFragmentForIntent(...)` returning `CancellationToken`.
- `health-sdk`: **instance class** `class GiniHealth(giniHealthAPI: GiniHealthAPI, context: Context)` — the known divergence from the singleton-facade shape (iOS records the same for HealthSDK). It exposes `getPaymentFragmentWithDocument`/`…WithoutDocument`, `setSDKLanguage`, and a public `val giniInternalPaymentModule`. Public top-level helpers `getGiniApi(...)` in `util/GiniHealthAPI.kt`.
- `internal-payment-sdk`: `class GiniInternalPaymentModule(...)` — published (has an api dump), consumed by `health-sdk`. Whether integrators are meant to call it directly is `(confidence: LOW — check the health-sdk integration guide and the module's `gradle.properties` POM description)`.
- API libraries: `abstract class GiniCoreAPIBuilder<…>` / `abstract class GiniCoreAPI<…>` in `core-api-library` `internal/` (public despite the package name); `class GiniBankAPIBuilder @JvmOverloads constructor(...)`, `class GiniHealthAPIBuilder @JvmOverloads constructor(...)`, `class GiniBankAPI`, `class GiniHealthAPI`.

### Layering per module
- ViewModels: none holds a `Context`/`Application`, none extends `AndroidViewModel`, none imports `android.view`/`android.widget` or reaches a `NavController` (greps on `*ViewModel*.kt` under `*/sdk/src/main/java` are empty). Keep it that way.
- `bank-sdk`: Orbit `ContainerHost` in `SkontoFragmentViewModel`, `DigitalInvoiceSkontoViewModel`, `InvoicePreviewViewModel`, `TransactionDocInvoicePreviewViewModel`; intents as classes under `viewmodel/intent/`; use cases under `skonto/usecase/`; one repository under `transactiondocs/internal/repository/`.
- `capture-sdk`: one `GiniCaptureViewModel` built with an inline `ViewModelProvider.Factory` in `GiniCaptureFragment.kt`; legacy Java MVP (`GiniCaptureBasePresenter`, `AnalysisScreenPresenter`, `OnboardingScreenPresenter`, `OnboardingPagePresenter`); Kotlin extension files that host Compose and carry logic: `analysis/AnalysisFragmentExtension.kt`, `analysis/AnalysisScreenPresenterExtension.kt`, `camera/CameraFragmentExtension.kt`.
- `health-sdk` / `internal-payment-sdk`: manual wiring (`ViewModelProvider` in `integratedFlow/PaymentFragment.kt`), no Koin (`grep -rl org.koin` is empty there). Navigation mechanism inside `PaymentFragment` `(confidence: LOW — not read; verify with grep for FragmentManager/NavController before the agent describes it)`.
- Navigation graphs: `capture-sdk/sdk/src/main/res/navigation/gc_nav_graph.xml`, `bank-sdk/sdk/src/main/res/navigation/gbs_nav_graph.xml` (plus example-app graphs). No `navigation-compose` in the catalog.
- API libraries: `*Service` (Retrofit interface) → `*RemoteSource` → `*Repository` → `*Manager`, e.g. `health-api-library` `HealthApiDocumentService`, `HealthApiDocumentRemoteSource`, `HealthApiDocumentRepository`, `HealthApiDocumentManager`; results wrapped in `core-api-library` `Resource`.

### DI
- Isolated Koin contexts: `capture-sdk` `di/CaptureSdkIsolatedKoinContext.kt`, `bank-sdk` `di/BankSdkIsolatedKoinContext.kt`; `bank-sdk` bridges capture-sdk's graph in `capture/di/skonto/CaptureSdkDiBridge.kt`. No `startKoin`, `GlobalContext` or `koinViewModel(` anywhere.
- `viewModel {}` bindings only in bank-sdk (`SkontoScreenModule.kt`, `DigitalInvoiceSkontoScreenModule.kt`, `InvoicePreviewModule.kt`, `TransactionDocInvoicePreviewModule.kt`). capture-sdk has none by design.
- **Koin is in the published surface today** (contradicts `AGENTS.md` "never expose Koin"): `bank-sdk/sdk/api/sdk.api` lists `BankSdkIsolatedKoinContext.getKoin()`, `getGiniBankKoin()`, and public `Module` getters (`getCaptureFlowFragmentModule`, `getCaptureSdkDiBridge`, `getSkontoCommonModule`, `getDigitalInvoiceSkontoScreenModule`, `getSkontoScreenModule`, `getSkontoUseCaseModule`, `getHandlerModule`, `getInvoicePreviewScreenModule`); `capture-sdk/sdk/api/sdk.api` lists `getKoin()`/`getGiniCaptureKoin()`. Re-check: `grep -n koin */sdk/api/sdk.api`.

### Third-party types in the published surface
- `core-api-library`: Moshi in constructors of the remote sources and `protected getMoshi()`; `protected getApiRetrofit()`; `okhttp3.RequestBody` in `DocumentService.sendFeedback/uploadDocument` and `DocumentRemoteSource`; `retrofit2.Response` in `ApiException.forResponse`. **Deliberate:** `OkHttpClient`/`Cache`/`TrustManager` on `GiniHttpClientProvider`, `DefaultGiniHttpClientProvider.Builder` and the API builders (integrators may supply their own client — also the security agent's "integrator TLS" case).
- `capture-sdk:default-network`: `okhttp3.Cache` on `GiniCaptureDefaultNetworkService.Builder.setCache` — deliberate, same reason.
- `capture-sdk`: jsr166e `CompletableFuture` — the Java-facing bridge (concurrency-specialist's deliberate design). Compose types on public composables.
- Re-check: `grep -c 'org/koin\|retrofit2\|okhttp3\|com/squareup/moshi' <dump>`.

### Build architecture
- `buildSrc/src/main/kotlin/net/gini/gradle/`: `CodeAnalysisPlugin`, `DependencyUpdatesPlugin`, `DokkaPlugin`, `JacocoCoveragePlugin`, `PropertiesPlugin`, `PublishToMavenPlugin`, `ReleaseOrderPlugin`, `SBOMPlugin` (+ `extensions/`).
- `gradle.properties` in every releasable module and both example apps; `consumer-rules.pro` in `bank-sdk/sdk`, `capture-sdk/sdk`, `capture-sdk/default-network`, `health-sdk/sdk` — **not** in `internal-payment-sdk/sdk` or the API libraries `(confidence: LOW whether that is deliberate — ask before the agent calls a missing file a finding; treat as "verify on branch" in the agent)`.
- `RELEASE-ORDER.md` is generated by `updateReleaseOrderFile`; `capture-sdk:default-network` is bumped with `capture-sdk` (AGENTS.md / platform.md §3).

### Java interop
- `@JvmStatic`/`@JvmOverloads` appear in capture-sdk, bank-sdk, core-api-library, bank-api-library, health-api-library, default-network; none in health-sdk/internal-payment-sdk. The API-library builders are `@JvmOverloads`. The Sphinx guides under `*/library/src/doc/source/guides/` mention Java. So Java callers are a supported audience for the API libraries and capture/bank SDKs; the agent asks for a Java path on new public API there (same rule as concurrency-specialist for `suspend`/`Flow`).

## Technical conventions

This ticket produces Markdown agent/rule files only, so the platform checklist applies as follows:

1. **Language:** no Kotlin/Java is written or touched. No legacy Java file is touched.
2. **UI:** none.
3. **Architecture:** none for this change. The *agent's* rule set is in §Design.
4. **DI / async:** none.
5. **Strings/resources:** none.
6. **Quality gates:** ktlint/detekt/Jacoco do not apply to `.claude/`. The gates are the test plan below (file lint + dry runs) and `/code-review` on the branch. The agent itself cannot run Gradle; it tells the user to run `/gini-check` (and notes `apiCheck` is CI-only).

Commit convention (`.git-stuff/commit-msg-template.txt`): type `ai:`, no project scope, body, `XPL-443` on the last line, **no `Co-Authored-By` trailer**. The user commits; the build step provides messages only.

## Design

### 1. `.claude/agents/architecture-specialist.md`

Same section order as the siblings. No line numbers, no counts.

1. **Frontmatter** — `name: architecture-specialist`; description naming module graph, published API surface (`api/*.api`), facade entry points, layering (MVVM / Orbit / API-library stack), DI boundaries (isolated Koin, manual wiring), build-level architecture; "reviews the shape, not correctness or style"; `tools: Read, Edit, Write, Glob, Grep`.
2. **The most important fact — these are SDKs, not an app.** Kotlin is public by default and `explicitApi` is off, so a declaration becomes Maven Central API by omission; an accidentally public helper cannot be removed without a major (parallel 1.x/2.x branches). Every transitive dependency becomes the host app's dependency. Integrators may call from Java. The facade returns a `Fragment`; the host app never instantiates a screen. No DI framework may show in the public API (AGENTS.md) — and the agent must know it already does (Koin getters), as a known existing pattern.
3. **Repo Context** — the facts from §What the code really does, each with its re-check command: the module DAG and the cross-aisle invariants (bank aisle = `bank-api-library`, `capture-sdk`, `bank-sdk`; health aisle = `health-api-library`, `internal-payment-sdk`, `health-sdk`; `core-api-library` is the only shared root; the test-only edge is allowed); the facade shapes incl. the `GiniHealth` divergence; per-module ViewModel/DI wiring; the API-library layer stack; the deliberate third-party exposures; `buildSrc` plugins; `apiCheck` in CI but not in `/gini-check`.
4. **Knowledge Source** — read **`.claude/rules/architecture.md`** (owned) and **`.claude/skills/gini-review/platform.md`** §1, §2, §4, §9 (not owned — cite, don't restate) before any review. `AGENTS.md` wins on conflict. Propose rule edits to `architecture.md`; propose `AGENTS.md`/`platform.md` edits to the user. Sources: Kotlin library authors' guidelines, Kotlin backward-compatibility guide, AndroidX API guidelines (the three URLs already in platform.md §9).
5. **Core Instructions** — "a layering claim names the layer and the rule"; only suggest plugins/deps in the catalog; "old is not wrong"; label every finding with one of the types below; never run `apiDump` to make CI green — restrict visibility first; a new public declaration needs a reason, KDoc, and (for capture/bank/API libraries) a Java path; follow a type to its public end (an `internal` class exposed through a public constructor parameter is public); say when you cannot check.
6. **What You Review** — six groups, each with "finding" and "not a finding":
   1. *Module Graph* — new `project(":…")` edge against the DAG; cross-aisle; API library → SDK; `core-api-library` gaining a repo dep; `repositories {}` in a module (`FAIL_ON_PROJECT_REPOS`); a hand-edited `RELEASE-ORDER.md`; an example-app edge is not a finding; the androidTest edge is not a finding.
   2. *Public API* — new public top-level/class/member without `internal`; public type in an `internal` package (new only); a Koin/Retrofit/OkHttp/Moshi/Orbit type in a new public signature (the deliberate `OkHttpClient`/`Cache`/`TrustManager`/jsr166e cases excepted); a removal/rename without deprecation; `@PublishedApi internal`; dump updated to silence CI → cite platform.md §9 for the breaking-change table instead of restating it.
   3. *Facade & Entry Points* — a new screen not reachable from the module's facade; a public `Fragment`/`Activity` constructor path that bypasses `GiniBank`/`GiniCapture`/`GiniHealth`; a new facade following the `GiniHealth` instance shape instead of the singleton shape is a design question, not a defect; configuration added as a public setter when the module uses a `Builder`.
   4. *Layering* — `Context`/`View`/`NavController` in a ViewModel; business logic (parsing, decision, networking) in a Fragment, Fragment extension, or Compose body instead of ViewModel/use case; a Retrofit/OkHttp/Moshi type constructed in a `*Manager`/`*Repository` instead of the `*RemoteSource`; a `Flow`-returning `suspend` fun (hand threading to concurrency-specialist). Legacy Java MVP and the existing capture-sdk extension files are known patterns.
   5. *DI Boundary* — Koin reached outside the isolated context (`GlobalContext`, `startKoin`, `koinViewModel`); a `viewModel {}` binding added to capture-sdk (design question — it has none); Hilt in an SDK module; a Koin type in a new public signature; manual wiring in health/internal-payment replaced by a framework; a Koin `single {}` holding a `Context` that outlives the SDK.
   6. *Build Architecture* — ad-hoc tasks in a module build file instead of a `buildSrc` plugin; hardcoded versions; `capture-sdk:sdk` bumped without `default-network`; `consumer-rules.pro` widened; `jvmTarget` raised in one module.
   Plus **Boundaries with the other specialists**: compose/views (how a screen is built vs where it sits), concurrency (threading vs layer of `suspend`/`Flow`), security (what a public API exposes vs whether it should be public), testing (test structure vs testability of seams), code-reviewer (runs last; escalates here), a11y (always for UI), and future `performance-specialist` (XPL-442) / `design-system-specialist` (XPL-445) / debugger (XPL-444) as "not written yet".
7. **What NOT to Flag** — Java in capture-sdk for being Java; XML Fragments for not being Compose; capture-sdk's `ViewModelProvider.Factory`; example-app Hilt and example-app edges; the androidTest cross edge; the `default-network` → `bank-api-library` edge; existing public data classes / `*Kt` facades / Koin getters / public-in-`internal`-package classes untouched by the diff; missing `explicitApi`; the `GiniHealth` instance shape as such; the deliberate third-party exposures; anything `apiCheck` catches on branches with dumps; pre-existing issues on untouched lines (full audit: nit, "known existing pattern").
8. **Review Checklist** — one line per review item above.
9. **Review Process** — triage (map modules touched; grep the diff for `project(":`, `repositories {`, `^(public |)(class|fun|val|object|interface) ` without `internal`, `org.koin`, `retrofit2`, `okhttp3`, `com.squareup.moshi`, `NavController`, `Context` in a ViewModel, `apply false`, `consumer-rules`); smallest safe fix (add `internal`, move a constructor to the remote source); **ask first** list from R8; full audit = report-only; never commit/push; verify → user runs `/gini-check` and is told `apiCheck` runs only in CI (suggest `./gradlew <module>:apiCheck` explicitly).
10. **Output Format** — group by file; per finding `file:line`, type label (Module Graph · Public API · Facade · Layering · DI Boundary · Build), rule quoted from `architecture.md` or platform.md section cited, `before` → `after`; closing summary ranked with blocker/warning/nit; applied vs waiting-for-OK; coverage; "report only genuine problems".

### 2. `.claude/rules/architecture.md` (new, owned by the agent)

Short, like `coroutines-flow.md`:
- Header: owned by `architecture-specialist`; `gini-review/platform.md` §5 and `code-reviewer` point here; `AGENTS.md` wins.
- **Repo reality (do not flag existing code over this):** Koin getters in the bank/capture dumps; public classes in `internal` packages; public `*Kt` file facades; `GiniHealth` instance facade; capture-sdk extension files; Moshi/OkHttp/Retrofit in core-api signatures; the test-only API-library edge.
- **Rules:** the module DAG and aisle invariants (as a list, not a count); `internal` by default, public needs a reason + KDoc + Java path where Java is supported; no DI/network/JSON framework type in **new** public signatures (deliberate exceptions listed); facade returns a `Fragment`, screens reachable from it, config through the module's existing `Builder`/setter pattern; ViewModels hold no `Context`/`View`/`NavController`; business logic in ViewModel/use case, API libraries keep Retrofit/OkHttp/Moshi inside `*RemoteSource`; Koin only inside the isolated contexts, `viewModel {}` only in bank-sdk, manual wiring in health/internal-payment, Hilt only in the bank example app; `buildSrc` plugins over ad-hoc tasks, catalog versions, no module repositories, `default-network` bumped with `capture-sdk`; deprecation cycle before removal; `apiCheck` is CI-only.
- Sources line (the three URLs from platform.md §9 + AGENTS.md).

### 3. `gini-review/platform.md` §5 pointer

One paragraph at the end of §5: "The architecture invariants (module DAG, facades, layering, DI boundaries) live in `.claude/rules/architecture.md`, owned by `architecture-specialist`; cite it for findings in those areas." No other change — the file is Android-only ("NOT MIRRORED"), so no iOS pair.

### 4. Wiring (edited in their `main` state)

| File | Edit |
|---|---|
| `.claude/agents/gini-orchestrator.md` (446 state) | description: add "architecture" to the parenthesis that already lists coroutines/concurrency, DI, testing, security; team table: add the `architecture-specialist` row after `code-reviewer`; Delegation Rules: insert rule 9 "Invoke **architecture-specialist** for any diff that touches …" (R2 list) before the code-reviewer-runs-last rule, renumber it 10, and change the last rule to "Performance and localization standards still apply (see Mandatory Rules) — enforce them inline; dedicated specialists for those are not in the team yet"; Mandatory Rules "Architecture" and "Dependency injection" bullets: append "details and the module DAG in `.claude/rules/architecture.md`"; Knowledge Sources: add the rules-file line next to the `coroutines-flow.md` one. |
| `.claude/commands/gini-task.md` (446 state) | "(compose, views, a11y, testing, security, concurrency, and code-reviewer for finished work before a push)" → insert "architecture" after "concurrency"; "Architecture, DI, performance, and localization standards apply inline." → "Performance and localization standards apply inline." |
| `.claude/settings.json` (446 state) | hook text "compose/views/a11y/testing/security/concurrency specialists" → "compose/views/a11y/testing/security/concurrency/architecture specialists". |
| `.claude/agents/code-reviewer.md` (446 state) | §"2. Public API surface": replace "There is no architecture specialist in the team yet. Catch the obvious leak yourself and flag the deeper question to the user." with "Catch the obvious leak yourself and escalate the deeper question to `architecture-specialist`."; §"Escalate rather than guess": add `architecture-specialist` to the specialist list and change "Architecture, performance and design-system questions have no specialist in the team yet" to "Performance and design-system questions have no specialist in the team yet"; §Rulebook: add `.claude/rules/architecture.md` to the files it reads. |

Not touched: `compose-specialist.md`, `views-specialist.md`, `a11y-specialist.md`, `testing-specialist.md` (R12), `android-security-specialist.md`, `concurrency-specialist.md`, `coroutines-flow.md`.

### 5. Commits (user commits; messages supplied by `/gini-build`)

1. `ai: Add architecture-specialist agent` — the agent file.
2. `ai: Add the shared architecture rules file` — `.claude/rules/architecture.md` + the platform.md §5 pointer.
3. `ai: Route architecture work to architecture-specialist` — orchestrator, `gini-task.md`, hook, `code-reviewer.md`.

Git (user runs it; the branch has no commits of its own and `origin/XPL-443-android-architecture-specialist` sits at `main`, so the push is non-fast-forward):

```bash
git fetch origin
git reset --hard origin/XPL-446-android-code-reviewer     # untracked specs/ file survives
git push --force-with-lease origin XPL-443-android-architecture-specialist
```

Start a fresh Claude Code session afterwards so the security, concurrency and code-reviewer agent files on the branch are picked up. PR base `XPL-446-android-code-reviewer` (then `main` once that merges); description from `.github/pull_request_template.md`; `specs/XPL-443-feature.md` stays uncommitted.

## Test plan

Agent files have no unit tests. Each MUST requirement maps to a check below; all are run by `/gini-build` step 5 (file lint by Bash, dry runs via the `Agent` tool with `subagent_type: architecture-specialist` once the file exists).

| Check | Proves |
|---|---|
| T1 Frontmatter: `head -20` shows `name`, `description`, `tools`; file name matches `name` | R1 |
| T2 `grep -nE ':[0-9]+' .claude/agents/architecture-specialist.md .claude/rules/architecture.md` is empty; `grep -nE '\b[0-9]+ (public|classes|files|modules)\b'` is empty | R11 |
| T3 Every backticked path/class/task in both files resolves: script extracts backticked tokens containing `/` or ending in `.kt`/`.java`/`.kts`/`.xml`/`.pro` and runs `ls`/`grep -rl` | R11 |
| T4 Dry run, synthetic diff adding `implementation(project(":health-api-library:library"))` to `bank-sdk/sdk/build.gradle.kts` → one blocker, *Module Graph* | R3 |
| T5 Dry run, synthetic diff adding `fun helper() = Unit` (no `internal`) to `bank-sdk/sdk/src/main/java/net/gini/android/bank/sdk/util/GiniBankAPI.kt` → warning, *Public API*, proposes `internal`, mentions `apiCheck` is CI-only | R4 |
| T6 Dry run, three synthetic diffs: Koin `Module` in a new public fun signature in bank-sdk; `NavController` field added to `SkontoFragmentViewModel`; `Moshi` constructed in `HealthApiDocumentManager` → three findings with labels *DI Boundary*, *Layering*, *Layering* | R5 |
| T7 Dry run on a diff touching only `capture-sdk/sdk/src/main/java/net/gini/android/capture/analysis/AnalysisScreenPresenter.java`, then only `health-sdk/sdk/src/main/java/net/gini/android/health/sdk/integratedFlow/PaymentFragment.kt`, then only `capture-sdk/default-network/build.gradle.kts` (existing edges), then only `bank-api-library/library/build.gradle.kts` androidTest line → no findings for those facts | R6 |
| T8 Full audit of `bank-sdk/sdk` with no diff → hotspots 1, 3 reported as nit "known existing pattern", no file modified (`git status` clean), "applied: none" | R7 |
| T9 Dry run, synthetic diff removing `getGiniBankKoin()` → the agent asks before applying and names the dump change | R8 |
| T10 `grep -n architecture .claude/settings.json .claude/commands/gini-task.md .claude/agents/gini-orchestrator.md` hits all three; orchestrator dry run with the T4 diff and with the T5 diff picks `architecture-specialist` (and `android-security-specialist` when the public class is under `core-api-library` `authorization/`) | R2, R9 |
| T11 Agent file's Knowledge Source names both `.claude/rules/architecture.md` and `gini-review/platform.md`; platform.md §5 contains the pointer | R10 |
| T12 `git diff main -- .claude/agents/compose-specialist.md .claude/agents/views-specialist.md .claude/agents/a11y-specialist.md .claude/agents/testing-specialist.md` is empty | R12 |
| T13 `grep -n 'architecture' .claude/agents/code-reviewer.md` lists the escalation entry; `grep -c 'no architecture specialist' .claude/agents/code-reviewer.md` is 0 | R13 |
| T14 (optional) `/code-review` on the branch | — |

### Not tested
- Agent quality across many sessions — nothing measures it yet (wiki Q4 item 28); same gap as XPL-437/440.
- The stacked PR retargeting to `main` after XPL-446 merges — GitHub does it; verify by eye.
- `code-reviewer` actually escalating to this agent at run time — only its text is checked (T13).

## Hotspots found (for tickets — not fixed here)

Ranked. Each needs a ticket decision by the user. All verified by grep on this branch; none fixed in this PR.

1. **Koin in the published surface** — `bank-sdk/sdk/api/sdk.api`: `BankSdkIsolatedKoinContext.getKoin()`, `getGiniBankKoin()`, public `Module` getters in `CaptureFlowFragmentModuleKt`, `CaptureSdkDiBridgeKt`, `SkontoCommonModuleKt`, `DigitalInvoiceSkontoScreenModuleKt`, `SkontoScreenModuleKt`, `SkontoUseCaseModuleKt`, `HandlerModuleKt`, `InvoicePreviewModuleKt`; `capture-sdk/sdk/api/sdk.api`: `getKoin()`, `getGiniCaptureKoin()`. Contradicts AGENTS.md. Making them `internal` is binary-breaking → major version or deprecation cycle. (Overlaps the pending Sonar capture-sdk "binary-breaking API changes" decision.)
2. **Public top-level helpers that read like internals** — `bank-sdk` `util/GiniBankAPI.kt` `getGiniApi(...)`, `health-sdk` `util/GiniHealthAPI.kt` `getGiniApi(...)`, `bank-sdk` `network/DefaultNetworkKt`, `util/NavigationExtensionKt`, and the many `*Kt` file facades in bank/capture/internal-payment dumps. Pre-existing; only removable with a major.
3. **Public classes in `internal` packages** in `capture-sdk` and `internal-payment-sdk` (also a few in core/bank/health libraries). Pre-existing surface; `apiCheck` locks it in.
4. **core-api-library leaks implementation types** — Moshi in remote-source constructors / `getMoshi()`, `getApiRetrofit()`, `okhttp3.RequestBody` in `DocumentService` and `DocumentRemoteSource`, `retrofit2.Response` in `ApiException.forResponse`. The `OkHttpClient`/`Cache`/`TrustManager` exposures are deliberate and stay.
5. **`GiniHealth` facade shape** — instance class with a public `giniInternalPaymentModule` and two unowned `CoroutineScope(Job()).launch` in the constructor (the scope part is concurrency-specialist hotspot 9 — not duplicated here). Divergent from `GiniBank`/`GiniCapture`; a design decision to record, not a bug.
6. **Logic in capture-sdk Fragment/Presenter extension files** — `analysis/AnalysisFragmentExtension.kt`, `analysis/AnalysisScreenPresenterExtension.kt`, `camera/CameraFragmentExtension.kt` host Compose and carry flow logic outside a ViewModel. Overlaps concurrency hotspots 1–2.
7. **`internal-payment-sdk:sdk` has no `consumer-rules.pro`** while the other SDK modules do — may be deliberate (LOW confidence).

## Out of scope

- Fixing any hotspot above; creating tickets for them.
- Enabling `explicitApi`, adding `apiCheck` to `/gini-check`, or changing any `api/*.api` dump.
- Editing `compose-`/`views-`/`a11y-`/`testing-specialist.md`, `android-security-specialist.md`, `concurrency-specialist.md`, `AGENTS.md`.
- The other XPL-402 agents (performance XPL-442, debugger XPL-444, design-system XPL-445).
- Mirroring anything to `gini-mobile-ios` (the touched files are Android-only).
- Committing `specs/XPL-443-feature.md`.

## Open questions

1. `GiniInternalPaymentModule` — integrator-facing API or health-sdk-internal that happens to be published? (confidence: LOW). Affects only one sentence in the agent's Repo Context; `/gini-build` writes it as "published and consumed by health-sdk; treat as public surface" unless answered.
2. Navigation mechanism inside health-sdk `PaymentFragment` (confidence: LOW). `/gini-build` verifies by grep before the agent describes it; the agent otherwise says "verify on branch".
3. Is the missing `internal-payment-sdk/sdk/consumer-rules.pro` deliberate? (confidence: LOW). The agent lists it as "verify on branch", never as a finding, until answered.

## Implementation plan
- [x] 1. Lint script in the scratchpad for T2/T3 (`file:line` citations, hard-coded counts, every backticked path/class/task resolves) — written before the files it checks (requirement 11)
- [x] 2. `.claude/rules/architecture.md` — shared invariants owned by the agent: repo reality, module DAG + aisles, API, facade, layering, DI, build rules, sources (requirements 3, 10)
- [x] 3. `.claude/agents/architecture-specialist.md` — frontmatter, SDK-not-app section, Repo Context, Knowledge Source, Core Instructions, six review groups + boundaries, What NOT to Flag, Checklist, Process, Output Format (requirements 1, 3, 4, 5, 6, 7, 8, 10, 11)
- [x] 4. `.claude/skills/gini-review/platform.md` §5 pointer paragraph to `architecture.md` (requirement 10)
- [x] 5. Wiring: `gini-orchestrator.md` (description, team row, rule 9 + renumber, Mandatory Rules pointers, Knowledge Sources), `gini-task.md`, `settings.json` hook, `code-reviewer.md` (flip the two "no architecture specialist" lines, escalation list, rulebook) (requirements 2, 9, 13; 12 by not touching the other four agents)
- [x] 6. Verify: lint T1–T3, T10–T13 by Bash; dry runs T4–T9 via the `Agent` tool with `subagent_type: architecture-specialist`; orchestrator dry run (T10); `code-reviewer` self-review per `platform.md`

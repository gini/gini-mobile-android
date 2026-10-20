# XPL-444: `debugger-specialist` agent

Status: implemented
Ticket: https://ginis.atlassian.net/browse/XPL-444 (sub-task of XPL-402 — "[Android] Add 7 specialist agents to the Claude agent team")

- Source: Confluence "AI Planning" (PLMO), September section. Rule from that page: **every agent must be valid for our repo; generic Kotlin/Android advice is not enough.**
- iOS twin: `debugger-specialist` in `gini-mobile-ios` (`.claude/agents/debugger-specialist.md`, local clone `~/XCodeProjects/gini-mobile-ios`, added in XPL-411). Used for **structure** (route by symptom → hypothesis → evidence → repro plan → route), not content.
- Android siblings on `main`: `android-security-specialist`, `concurrency-specialist`, `code-reviewer`, `architecture-specialist` (XPL-443, merged as PR #1007). `performance-specialist` (XPL-442) is still PR #1009 against `main` and is **not** on this branch.
- Branch: `XPL-444-android-debugger-agent` off `main`. No commits of its own yet.

## Problem

When something fails in the Gini Android SDKs — a crash in an integrator's app, an ANR, a red `<project>.check.yml` job, a test that passes on retry — the team has no agent whose job is to find the cause and stop. The specialists review diffs; `code-reviewer` reviews finished work; `/gini-fix` step 3 tells the main agent to "run a disciplined hypothesis loop" without any repo-specific knowledge of where Android failures hide here (slf4j logging that is silent without a binding, `apiCheck` that CI runs but `/gini-check` does not, the Test Orchestrator trap, the isolated Koin contexts). The result is patches at the symptom site. XPL-402 asks for a read-only diagnostic agent; the iOS repo already has its twin.

## Decisions (clarifying questions, 2026-10-16)

| Question | Decision |
|---|---|
| File name | `.claude/agents/debugger-specialist.md` — matches the iOS twin and the `concurrency-`/`architecture-specialist` naming. The Jira title keeps the `android-` prefix only to tell tickets apart. |
| Grading | **Impact** `Critical` / `High` / `Medium` / `Low` (the iOS twin's scale, adapted to Android below) **plus hypothesis confidence** `high` / `medium` / `low`. The siblings' blocker/warning/nit is a review scale and is not used. |
| Bash | **Yes — read-only Gradle and adb.** Allowed: `git log` / `git blame` / `git show` / `git diff`; single-class `./gradlew <project>:<module>:testDebugUnitTest --tests "…"`; `lint`, `detekt`, `ktlintCheck`, `apiCheck`; `adb logcat`, `adb shell dumpsys`, `adb pull` of an ANR trace. Denied by instruction: `apiDump`, `ktlintFormat`, `connectedCheck` and any `connected*AndroidTest` (they install on a device), `assemble*`, any git write (`commit`, `push`, `reset`, `checkout`, `switch`, `stash`, `clean`, tags), and printing `local.properties` / `test.properties`. |
| Branch base | **Stay on `main`.** The route list names only agents that exist on `main`. A one-line follow-up after PR #1009 merges adds `performance-specialist` to the route list and flips its "a debugger agent is planned but not written yet" line. |
| `/gini-fix` wiring | Only `.claude/skills/gini-fix/platform.md` changes (its "Root-cause tools" section). `SKILL.md` is mirrored byte-for-byte with gini-mobile-ios and stays untouched. |
| Rules | No new `.claude/rules/*` file. The agent reads `coroutines-flow.md` and `architecture.md` to route threading and shape causes and cites them; it never restates them. |
| Spec file | `specs/XPL-444-feature.md` is committed as its own `ai:` commit (precedent: `3e7277b66 ai: Add the XPL-443 feature spec`). |

## Requirements

- **R1 (MUST, entry):** Given the repo on this branch, when a developer or the orchestrator invokes the agent (via the `Agent` tool with `subagent_type: debugger-specialist` or through `/gini-task`), then `.claude/agents/debugger-specialist.md` loads with frontmatter `name: debugger-specialist`, a `description` naming the four failure classes (crash, ANR/hang, Gradle/CI failure, flaky test) and "finds the cause and stops; never fixes", and `tools: Read, Glob, Grep, Bash` — no `Edit`, no `Write`.
- **R2 (MUST, entry):** Given `gini-orchestrator.md`, when a task is a *report* rather than a change — a logcat stack trace, an ANR trace, failing output of a `.github/workflows/<project>.check.yml` job or a local Gradle run, or "this test is flaky" — then the orchestrator's team table and a delegation rule send it to `debugger-specialist` **first**, and only then to the specialist the diagnosis's "Route to" line names.
- **R3 (MUST, entry):** Given the `UserPromptSubmit` hook in `.claude/settings.json` and `.claude/commands/gini-task.md`, when either is read, then both list `debugger` among the specialists the orchestrator routes to.
- **R4 (MUST, entry):** Given `.claude/skills/gini-fix/platform.md`, when `/gini-fix` reaches step 3 (root cause), then the "Root-cause tools" section tells the main agent to hand the root-cause hunt to `debugger-specialist` (Agent tool) whenever the reproduction from step 2 does not already point at the defective line, and to carry its hypothesis, evidence and route line into `specs/<ticket>-bug.md`; and `git diff main -- .claude/skills/gini-fix/SKILL.md` is empty.
- **R5 (MUST, happy — crash):** Given a logcat stack trace whose `Caused by` chain ends in `java.lang.IllegalStateException: KoinApplication has not been started` (or `org.koin.core.error.NoDefinitionFoundException`) with frames inside `net.gini.android.bank.sdk`, when the agent is invoked, then its diagnosis names the mechanism (a global Koin call such as `koinViewModel()`/`GlobalContext` instead of `BankSdkIsolatedKoinContext` / `giniBankViewModel`), cites `.claude/rules/architecture.md` › DI boundaries, cites `file:line` evidence in the bank-sdk `di/` sources it read, and routes to `architecture-specialist` with impact and confidence stated.
- **R6 (MUST, happy — ANR):** Given a main-thread trace that shows `kotlinx.coroutines.BlockingCoroutine` / `runBlocking` or `com.google.android.gms.tasks.Tasks.await` under `net.gini.android.capture` frames on `main`, when the agent is invoked, then the hypothesis names the blocking call and the thread it runs on, the evidence cites the file and line in capture-sdk (`internal/qrcode/QRCodeDetectorTaskMLKit` for `Tasks.await`), the rule is cited from `coroutines-flow.md` (not restated), and the route is `concurrency-specialist`.
- **R7 (MUST, happy — Gradle/CI):** Given the failing output of `bank-sdk:sdk:apiCheck` (an `api/sdk.api` mismatch), when the agent is invoked, then it names the changed declaration from the diff, says that `apiCheck` runs in CI but not in `/gini-check`, explicitly does **not** propose `apiDump` as the fix, and routes to `architecture-specialist`. Given instead a Gradle failure whose first error is `java.lang.IllegalAccessError`, then the diagnosis says the build is not running on JDK 17, classifies it as environment, and the route line reads `none — environment`.
- **R8 (MUST, happy — flaky test):** Given a bank-sdk unit test that fails roughly every third run and uses `Thread.sleep` or a real `Dispatchers.Main` instead of the module's `MainDispatcherRule`, when the agent is invoked, then the hypothesis names the timing axis, the verification plan is a repeated single-class run (`./gradlew bank-sdk:sdk:testDebugUnitTest --tests "<Class>"`) that the agent runs or tells the user to run, and the route is `testing-specialist`.
- **R9 (MUST, error):** Given any diagnosis, when the user or orchestrator asks the agent to apply the fix, then it refuses in one sentence, repeats the route line, and the working tree is unchanged (`git status --short` identical before and after).
- **R10 (MUST, error):** Given a report with no artifact ("the app crashes sometimes"), when the agent is invoked, then it asks for the specific artifact its failure class needs (logcat with the `Caused by` chain and the host app's R8 `mapping.txt` if obfuscated; the ANR trace; the first error of the Gradle log with the exact command; the test name plus pass/fail of the last runs and whether it failed locally or in CI) and stops without a hypothesis.
- **R11 (MUST, error):** Given a reproduction that needs a device, an emulator, or API credentials the agent cannot reach, when it reaches the verification step, then it runs nothing that installs or writes (no `connectedCheck`, no `apiDump`, no `ktlintFormat`, no git write), states what blocks it, and gives the user the exact command to run — never "not reproducible".
- **R12 (MUST, happy):** Given the agent file's Knowledge Source section, when read, then it names `AGENTS.md`, `.claude/rules/coroutines-flow.md`, `.claude/rules/architecture.md` and `.claude/skills/gini-fix/platform.md` (the repro ladder) as files to read before diagnosing, and no file under `.claude/rules/` is added by this ticket.
- **R13 (MUST, entry):** Given `.claude/agents/code-reviewer.md` and `.claude/agents/architecture-specialist.md` on this branch, when read, then `code-reviewer`'s "Escalate rather than guess" list names `debugger-specialist` for a failing build, a failing or flaky test, or a crash reported against the diff; and `architecture-specialist`'s boundaries line no longer says a debugger agent is "not written yet" (performance and design-system stay listed as not written yet).
- **R14 (MUST, error):** Given `compose-specialist.md`, `views-specialist.md`, `a11y-specialist.md`, `testing-specialist.md`, `android-security-specialist.md`, `concurrency-specialist.md`, `coroutines-flow.md` and `architecture.md`, when this branch is diffed against `main`, then all eight are unchanged.
- **R15 (SHOULD, happy):** Given the agent file, when linted by the test plan, then it contains no `file:line` citation and no hard-coded count, and every class, path, Gradle task and workflow it names exists on this branch.
- **R16 (SHOULD, happy):** Given any diagnosis, when it is produced, then it ends with a **Verification status** that lists the exact commands the agent ran and the ones it did not (with the reason), so nobody reads an unrun check as a passed one.

## Affected modules

No Gradle module changes. Files touched, all under `.claude/` and `specs/`:

| File | Change |
|---|---|
| `.claude/agents/debugger-specialist.md` | **new** — the agent |
| `.claude/agents/gini-orchestrator.md` | description, team-table row, one delegation rule ("a report, not a change → debugger first"), Knowledge Sources line |
| `.claude/commands/gini-task.md` | specialist list |
| `.claude/settings.json` | hook text |
| `.claude/agents/code-reviewer.md` | escalation list entry for failing build / flaky test / crash |
| `.claude/agents/architecture-specialist.md` | flip the "a debugger agent … not written yet" boundaries line |
| `.claude/skills/gini-fix/platform.md` | "Root-cause tools": pointer paragraph to the agent (file is Android-only, "NOT MIRRORED") |
| `specs/XPL-444-feature.md` | this file |

Not touched: `.claude/skills/gini-fix/SKILL.md` (mirrored), the eight files in R14, `AGENTS.md`, `performance-specialist.md` (not on this branch).

## Public API impact

None. No Kotlin/Java source, no `api/*.api` dump, no `gradle.properties` change.

## What the code really does (verified on this branch, 2026-10-16)

Facts the agent must state as patterns with their re-check command — never as counts. Where a ticket claim was imprecise, the precise version is given.

### Logging
- SDK modules log through **slf4j** (`org.slf4j` imports in `capture-sdk/sdk`, `capture-sdk/default-network`, `bank-sdk/sdk`, `health-sdk/sdk`, `internal-payment-sdk/sdk`; catalog `slf4j-api`). **No Timber anywhere.** The only slf4j binding in the repo is `logback-android`, declared in `bank-sdk/example-app/build.gradle.kts` and `health-sdk/example-app/build.gradle.kts` — so inside an integrator's app slf4j output is **silent unless the host adds a binding**, and "no log lines" from an SDK is not evidence that a code path did not run. Re-check: `grep -rl org.slf4j */*/src/main | cut -d/ -f1-2 | sort -u`; `grep -ln logback */*/build.gradle.kts`.
- **Precision on the ticket's "no `android.util.Log`":** a handful of SDK files call `android.util.Log` directly — `bank-sdk` `di/BankSdkIsolatedKoinContext.kt` (`Log.d` "Koin already initialized"), `core-api-library` `internal/GiniCoreAPIBuilder.kt` and `http/DefaultGiniHttpClientProvider.kt` (`Log.w` debugging warnings), `capture-sdk` `internal/camera/photo/Exif.java` and `com/ortiz/touch/TouchImageView.java` (`Log.d`). These appear in logcat without any binding. Re-check: `grep -rlE '\bLog\.(d|e|w|i|v)\(' */*/src/main`.
- **HTTP logging** (`HttpLoggingInterceptor`) is added to the OkHttp client only when `isDebuggingEnabled` in `core-api-library` `http/DefaultGiniHttpClientProvider.kt`, switched on through `GiniCoreAPIBuilder.setDebuggingEnabled(...)` and `capture-sdk/default-network` `GiniCaptureDefaultNetworkService.Builder.setDebuggingEnabled(...)`. Off by default; integrator reports normally have no request/response bodies.
- **Integrator-facing error channel:** `capture-sdk` `logging/ErrorLoggerListener.kt` (public interface, `handleErrorLog(ErrorLog)`) and `logging/ErrorLogger.kt` (`internal`, fans out to Gini's logger when `isGiniLoggingOn` and to the integrator's custom listener). Wired in `GiniCapture.java` (`Builder`), `network/GiniCaptureNetworkService.java`, and `bank-sdk` `capture/Configuration.kt`. An `ErrorLog` an integrator pastes is this channel's output.
- **Crashlytics** only in `health-sdk/example-app/build.gradle.kts` (catalog `firebase-crashlytics`). **No LeakCanary, no `StrictMode`, no benchmark module** anywhere. Re-check: `grep -rl 'StrictMode\|leakcanary\|crashlytics' --include='*.kt' --include='*.java' --include='*.kts' . | grep -v build/`.
- The SDKs ship **no R8 mapping**; an obfuscated integrator stack trace is deobfuscated with the **host app's** `mapping.txt`. `consumer-rules.pro` exists in `bank-sdk/sdk`, `capture-sdk/sdk`, `capture-sdk/default-network`, `health-sdk/sdk` and influences what the host keeps.

### CI and local gates
- Per-module check workflows `.github/workflows/<project>.check.yml` run on `ubuntu-latest`, Temurin **JDK 17**: `testDebugUnitTest`, `apiCheck`, `lint`, `detekt`, `ktlintCheck` (+ example-app assembly, Sonar). Report artifacts: unit tests `<project>/<module>/build/reports/tests`, lint `build/reports/lint-results*.html`, detekt `build/reports/detekt/*.html`, ktlint `build/reports/ktlint/**/*.html`, instrumented `build/outputs/androidTest-results/connected`. Re-check: `grep -n 'path:' .github/workflows/bank-sdk.check.yml`.
- `connectedCheck` runs in CI for `core-api-library`, `bank-api-library`, `health-api-library`, `capture-sdk` and `bank-sdk` on `reactivecircus/android-emulator-runner`, **API 33, x86_64, `-camera-back none`** (a camera-dependent instrumented test cannot pass there by construction). `health-sdk` and `internal-payment-sdk` check workflows have **no `connectedCheck` job**. Re-check: `grep -l connectedCheck .github/workflows/*.check.yml`; `grep -n 'api-level\|camera-back' .github/workflows/bank-sdk.check.yml`.
- **`bank-sdk.check.ui-tests.yml` is effectively dormant:** every step after checkout and setup-java is commented out, so the bank-sdk example-app UI tests (`bank-sdk/example-app/src/androidTest`) run **only locally on a device** via `./gradlew bank-sdk:example-app:connectedDevExampleAppDebugAndroidTest`. "Flaky in CI" cannot refer to them. Re-check: `grep -vE '^\s*#' .github/workflows/bank-sdk.check.ui-tests.yml`.
- `/gini-check` runs `testDebugUnitTest lint detekt ktlintCheck` only — **not `apiCheck`**; `/gini-connected-check` covers `connectedCheck`.
- Gradle must run on **JDK 17**: a newer JDK fails with `IllegalAccessError` (`AGENTS.md` Gotchas). `FAIL_ON_PROJECT_REPOS` in `settings.gradle.kts` rejects a `repositories {}` block in a module. `capture-sdk:screen-api-example-app` is commented out there. `bank-sdk:example-app` needs one flavor variant (`assembleDevExampleAppDebug`); `health-sdk:example-app` fails to configure without `clientId`/`clientSecret` in its `local.properties`. `RELEASE-ORDER.md` is generated (`updateReleaseOrderFile`). `capture-sdk:sdk` and `capture-sdk:default-network` are bumped together — a `NoSuchMethodError`/`AbstractMethodError` between them in an integrator app is version skew.

### Tests
- Unit tests: JUnit4, Robolectric with `sdk=35` from `src/test/resources/robolectric.properties` in all eight releasable modules (some classes override with `@Config(sdk = [23|27|28|33])` or `@Config(manifest = Config.NONE)`), `isIncludeAndroidResources = true` in the SDK modules and `health-api-library`. MockK in newer modules, Mockito-Kotlin in older ones; Truth; Turbine; `kotlinx-coroutines-test`; MockWebServer in the API libraries' `src/test` wire tests. `bank-sdk/sdk/src/test/java/net/gini/android/bank/sdk/MainDispatcherRule.kt` is the Main-swap precedent. `Thread.sleep` still occurs in a few test files (a flake axis). Re-check: `grep -rl Thread.sleep */*/src/test */*/src/androidTest`.
- Instrumented tests: `AndroidJUnitRunner` everywhere; `execution = "ANDROIDX_TEST_ORCHESTRATOR"` in `capture-sdk/sdk`, `health-sdk/sdk`, `internal-payment-sdk/sdk` and `bank-sdk/example-app` (the latter with `clearPackageData = true`) — **not** in `bank-sdk/sdk` or the API libraries, which run under the plain runner. Re-check: `grep -n 'ANDROIDX_TEST_ORCHESTRATOR\|clearPackageData' */*/build.gradle.kts`.
- **Orchestrator trap** (`AGENTS.md`): two comma-separated class names in `-Pandroid.testInstrumentationRunnerArguments.class` start 0 tests — filter with ONE class or a `Class#method` list.
- Example-app UI tests: `bank-sdk/example-app/src/androidTest/.../ui/resources/RetryRule.kt` (default two retries) is applied by `SmokeJourneyTestBase` and the `testcases/*Tests.kt` classes — **a test that passes on retry under `RetryRule` is a flake the rule is masking, not a pass**. `ImageUploader.kt` in the same `resources/` folder holds the content-description fallbacks for the Mainline photo picker (`com.google.android.photopicker`, which exposes no resource ids).

### Where failures root-cause
- Dependency chain (`AGENTS.md`): `core-api-library` → everything; `capture-sdk` → `bank-sdk`; `health-api-library` → `internal-payment-sdk` → `health-sdk`. A symptom in `bank-sdk` often roots in `capture-sdk` or `core-api-library`.
- Parallel major branches: `origin/1.x.x`, `origin/2.x.x`, `origin/release/bank-sdk-4.6`, `origin/release/bank-sdk-4.7`. A version-specific report must be diagnosed on the matching branch; release tags `<project>;<version>` date a change. Re-check: `git branch -r | grep -E 'x\.x|release/'`.
- DI: isolated Koin contexts `capture-sdk` `di/CaptureSdkIsolatedKoinContext.kt`, `bank-sdk` `di/BankSdkIsolatedKoinContext.kt`, bridged by `bank-sdk` `capture/di/skonto/CaptureSdkDiBridge.kt`; `viewModel {}` only in bank-sdk, resolved via `giniBankViewModel`. A `KoinApplication has not been started` / `NoDefinitionFoundException` crash is a global-Koin call or a module missing from the isolated context (`.claude/rules/architecture.md` › DI boundaries).
- Threading facts the agent needs for ANRs are in `.claude/rules/coroutines-flow.md` (Orbit intents start off-main; `runBlocking` only in `GiniSessionInterceptor`; `Tasks.await` in `QRCodeDetectorTaskMLKit` on a `HandlerThread`; `AsyncTask`/`HandlerThread`/`UIExecutor` in capture-sdk Java). The agent cites, never restates.
- Navigation: AndroidX Navigation graphs `capture-sdk` `res/navigation/gc_nav_graph.xml`, `bank-sdk` `res/navigation/gbs_nav_graph.xml`; `health-sdk` `integratedFlow/PaymentFragment.kt` uses `childFragmentManager`. An `IllegalArgumentException: Navigation action/destination … cannot be found` crash is a nav-graph/back-stack cause.
- Facades: `GiniCapture.getInstance()` throws `IllegalStateException("Not instantiated.")` when no instance was built (`ErrorLogger.log` guards with `hasInstance()`); `object GiniBank`; `class GiniHealth`. Static/singleton state survives between unit tests unless reset — a flake axis.

## Technical conventions

This ticket produces Markdown agent and config files only; the platform checklist applies as follows:

1. **Language:** no Kotlin/Java written or touched. No legacy Java file is touched.
2. **UI:** none.
3. **Architecture:** none for this change. The *agent's* investigation structure is in §Design.
4. **DI / async:** none.
5. **Strings/resources:** none.
6. **Quality gates:** ktlint/detekt/Jacoco do not apply to `.claude/`. The gates are the test plan (file lint by Bash + dry runs through the `Agent` tool) and the `code-reviewer` self-review. `/gini-check` is not needed; say so in the wrap-up.

Commit convention (`.git-stuff/commit-msg-template.txt`): type `ai:`, no project scope, body, `XPL-444` on the last line, **no `Co-Authored-By` trailer**. The user commits; `/gini-build` supplies messages only.

## Design

### 1. `.claude/agents/debugger-specialist.md`

Section order mirrors the iOS twin; content is this repo's. No line numbers, no counts.

1. **Frontmatter** — `name: debugger-specialist`; description: "Android diagnostic agent for the Gini SDKs. Finds the cause of a crash, ANR/hang, Gradle/CI failure or flaky test — and stops there. Never fixes. Produces one falsifiable hypothesis with evidence, a reproduction or verification plan, impact and confidence, and routes the fix to the specialist who owns the code."; `tools: Read, Glob, Grep, Bash`.
2. **Opening contract** — diagnostic only; `Edit`/`Write` are missing on purpose; asked to fix, refuse in one sentence and repeat the route line. Never write "the fix is:".
3. **Bash discipline** — the allowed/denied lists from the Decisions table, verbatim. Gradle only with a fully qualified `<project>:<module>:<task>` path and JDK 17; one test class at a time; never print `local.properties`/`test.properties`; a long-running task (`connectedCheck`, `assemble*`) is handed to the user with the exact command.
4. **The most important fact — these are SDKs, not an app** — the crash is in the integrator's process: no `Application`, no Crashlytics, no mapping file of our own (the host's `mapping.txt` deobfuscates), slf4j silent without a host binding, `ErrorLoggerListener` is the channel integrators see, the SDK version in the report decides the branch (`1.x.x`/`2.x.x`/`release/*`), and version skew between `capture-sdk:sdk` and `capture-sdk:default-network` is a real crash class.
5. **Repo Context** — the facts from §What the code really does with their re-check commands: logging, HTTP debug switch, error channel, CI gates and report paths, the API 33 `-camera-back none` emulator, the dormant UI-test workflow, `/gini-check` vs `apiCheck`, the Gotchas seed list, test stack (Robolectric sdk 35, `MainDispatcherRule`, Orchestrator placement and trap, `RetryRule`, `ImageUploader`), dependency chain, parallel branches, isolated Koin, nav graphs, facades.
6. **Knowledge Source** — read `AGENTS.md` (canonical), `.claude/rules/coroutines-flow.md` (threading causes), `.claude/rules/architecture.md` (module/DI/API causes) and `.claude/skills/gini-fix/platform.md` (the cheapest-faithful-repro ladder: unit test → instrumented test → example app) before diagnosing. Cite them; never restate. No rules file of its own.
7. **What You Investigate — route by symptom, one class at a time:**
   - **Crashes.** Read the `Caused by` chain bottom-up and the first `net.gini.android.*` frame. Deobfuscate first (ask for the host's `mapping.txt`); never guess frames. Classes and their usual mechanism: `KoinApplication has not been started` / `NoDefinitionFoundException` → global Koin or missing module in the isolated context (route architecture); `IllegalArgumentException` from `NavController` (`destination … unknown`, `action … cannot be found`) → nav graph / back-stack (route views or compose by screen); `IllegalStateException: Fragment … not attached` / `not associated with a fragment manager` → lifecycle (views); `CalledFromWrongThreadException` / `NetworkOnMainThreadException` → threading (concurrency); `Resources$NotFoundException` / `InflateException` → theme attrs, `vectorDrawables.useSupportLibrary` (views/compose); `NoSuchMethodError` / `AbstractMethodError` / `ClassNotFoundException` on a `net.gini.android` class → capture/default-network version skew or host R8 (architecture, with the security agent when `consumer-rules.pro` is involved); `ActivityNotFoundException` / `FileUriExposedException` / `SecurityException` → intents, `FileProvider`, permissions (security); `UninitializedPropertyAccessException` / `NullPointerException` at a Java↔Kotlin boundary → the owning screen's specialist; `CancellationException` escaping or `Resource.Cancelled` treated as an error → concurrency; `IllegalStateException: Not instantiated.` from `GiniCapture.getInstance()` (the `hasInstance()` guard was skipped) → the integrator reached a screen before building the facade — architecture. Note the module seam: a top frame in `capture-sdk` with the argument from `bank-sdk` is a seam cause.
   - **ANR / hang.** Get the main-thread trace (`adb pull` from `/data/anr/` or `adb bugreport`, or the `ANR in …` logcat block) and read the actual sample. Suspects by frame: `runBlocking`/`BlockingCoroutine`, `Tasks.await`, `synchronized` in capture-sdk Java, `Dispatchers.Main` doing I/O or image decode, Orbit `intent {}` doing main-thread work by accident, a `Mutex` used as a signal between callbacks, `HandlerThread`/`AsyncTask` joins. The rule is cited from `coroutines-flow.md`; route `concurrency-specialist` (and `compose-specialist` for a recomposition storm). No StrictMode exists to turn on.
   - **Gradle / CI failure.** Read the **first** error, not the last. Classify: toolchain (`IllegalAccessError` → not JDK 17; `Unsupported class file major version`), configuration (`FAIL_ON_PROJECT_REPOS`, a flavor-less example-app task, missing `local.properties` credentials, catalog accessor typo), compile (Kotlin 2.0.20 / K2 message with a source location → the screen's specialist), `apiCheck` (`api/<module>.api` mismatch → name the declaration, never `apiDump` to silence, route architecture), `lint`/`detekt`/`ktlintCheck` (report paths above; a rule name → the code's owner; a baseline change → ask), `testDebugUnitTest` (`build/test-results/testDebugUnitTest/*.xml` + `build/reports/tests`), `connectedCheck` (API 33 emulator, `-camera-back none`, no job for health/internal-payment, Orchestrator trap), release-order/SBOM tasks (generated files, never hand-edited). Environment causes get `Route to: none — environment`.
   - **Flaky tests.** Isolation first: one class with `--tests` (unit) or one class in the runner argument (instrumented — the two-class trap). Axes: shared static state (`GiniCapture.getInstance()`, `object GiniBank`, the isolated Koin contexts not reset between tests), real time (`Thread.sleep`, `delay` on a real dispatcher, missing `MainDispatcherRule`, `runTest` without a shared `testScheduler`), Robolectric `sdk`/`@Config` skew and `isIncludeAndroidResources`, Orbit test mode vs real container, file/`SharedPreferences`/DataStore residue (only the example app runs with `clearPackageData`), emulator timing and Espresso idling, the Mainline photo picker (`ImageUploader` fallbacks), network-backed tests needing `test.properties`/`local.properties` credentials, locale/timezone (German default strings, `values-en`). `RetryRule` passing on retry is evidence of a flake, not a pass. Route `testing-specialist`; `concurrency-specialist` when the axis is a coroutine race.
8. **Investigation Process** — 1 classify (one of four; ask if ambiguous); 2 gather the artifact (the per-class list from R10); 3 state one falsifiable hypothesis naming the mechanism; 4 verify with the cheapest faithful repro from `gini-fix/platform.md`, running only what Bash discipline allows, otherwise giving the exact command; if it cannot be reproduced and reproduction cannot be described, downgrade to "candidate cause" and list alternatives; 5 route.
9. **Output Format** — `Failure type`; `Symptom` (one line); `Hypothesis` (one sentence) with `Confidence: high | medium | low`; `Impact: Critical | High | Medium | Low`; `Evidence` (bullets, each `path:LINE` or a quoted log/trace line); `Reproduction / verification plan`; `Alternatives ruled out`; `Route to:` one of `compose-specialist` | `views-specialist` | `concurrency-specialist` | `architecture-specialist` | `testing-specialist` | `android-security-specialist` | `a11y-specialist` | `none — environment`, with a one-line reason; `What I did NOT do` ("no fix applied, no files edited"); `Verification status` (commands run / not run and why).
10. **Impact guide** (iOS scale, Android wording) — Critical: reproducible crash or ANR on a supported API level (23+) in a shipped SDK path, `main` red on a check workflow, a test failing every run. High: crash on a common path with a clear repro, hang visible but under a second, a failure gated to one module or flavor, a test failing at least half the runs. Medium: intermittent crash without a clean repro, hang only under load, a real deprecation warning, a test failing under half the runs. Low: a one-off without a repro path, a cosmetic warning, a single-run flake.
11. **Boundaries & handoffs** — the route list above with one line each; `code-reviewer` hands failing builds/tests here and takes the finished fix afterwards; `performance-specialist` is **not** named (not on this branch — see Out of scope). If a report is vague and no hypothesis can be formed from the artifact, ask for the artifact and stop.

### 2. `.claude/skills/gini-fix/platform.md` — "Root-cause tools"

Append one paragraph after the existing three bullets: when the reproduction from step 2 does not already point at the defective line, hand the root-cause hunt to the `debugger-specialist` agent (Agent tool, `subagent_type: "debugger-specialist"`) with the ticket id, the artifact (stack trace / ANR trace / Gradle output / test history) and the SDK version; it is read-only and returns a hypothesis, evidence, a repro plan and a route line — carry those into `specs/<ticket>-bug.md` and let the named specialist (or `/gini-build`) own the fix. `SKILL.md` is not touched.

### 3. Wiring (edited in their `main` state)

| File | Edit |
|---|---|
| `.claude/agents/gini-orchestrator.md` | description: add "failure diagnosis" to the parenthesis; team table: row `**debugger-specialist**` after `architecture-specialist` — "Diagnostic-only: root-causes a crash, ANR, Gradle/CI failure or flaky test — hypothesis + evidence + repro plan, impact and confidence; never edits; names the specialist who owns the fix"; Delegation Rules: insert a rule before "code-reviewer runs last" — "Invoke **debugger-specialist** when the task is a *report* rather than a change — a logcat stack trace, an ANR trace, failing Gradle/CI output or a flaky test. It never edits; route the fix to the specialist its diagnosis names." — and renumber the following rules; Knowledge Sources: one line that the debugger reads `coroutines-flow.md`, `architecture.md` and `gini-fix/platform.md` and is the only specialist besides `code-reviewer` with Bash (read-only Gradle/adb/git). |
| `.claude/commands/gini-task.md` | "(compose, views, a11y, testing, security, concurrency, architecture, and code-reviewer …)" → insert "debugger for failure reports" after "architecture". |
| `.claude/settings.json` | hook text "compose/views/a11y/testing/security/concurrency/architecture specialists" → append "/debugger (for a crash, ANR, failing build or flaky test)". |
| `.claude/agents/code-reviewer.md` | "Escalate rather than guess": add `debugger-specialist` to the owner list and one sentence: "A failing build, a failing or flaky test, or a crash reported against the diff goes to `debugger-specialist` first; it is diagnostic-only." The "Performance and design-system questions have no specialist" line stays. |
| `.claude/agents/architecture-specialist.md` | boundaries line "`performance-specialist`, `design-system-specialist` and a debugger agent are planned but not written yet; do not route to them." → "`performance-specialist` and `design-system-specialist` are planned but not written yet; do not route to them. A crash, ANR, build failure or flaky test goes to `debugger-specialist` first." |

### 4. Commits (user commits; messages supplied by `/gini-build`)

1. `ai: Add the XPL-444 feature spec` — `specs/XPL-444-feature.md`.
2. `ai: Add debugger-specialist agent` — the agent file.
3. `ai: Route failure reports to debugger-specialist` — orchestrator, `gini-task.md`, hook, `code-reviewer.md`, `architecture-specialist.md`, `gini-fix/platform.md`.

PR base `main`; description from `.github/pull_request_template.md`. Start a fresh Claude Code session after the agent file exists so the `Agent` tool picks it up for the dry runs.

## Test plan

Agent files have no unit tests. Each MUST requirement maps to a check below; all are run by `/gini-build` — file lint by Bash, dry runs through the `Agent` tool with `subagent_type: debugger-specialist` (after a fresh session), each with a synthetic artifact pasted into the prompt.

| Check | Proves |
|---|---|
| T1 `head -20 .claude/agents/debugger-specialist.md` shows `name: debugger-specialist`, the description, and `tools:` containing exactly Read, Glob, Grep, Bash; `grep -c 'Edit\|Write' ` on the tools block is 0 | R1 |
| T2 `grep -n debugger .claude/agents/gini-orchestrator.md` hits the team table, a delegation rule and Knowledge Sources; orchestrator dry run with the T5 stack trace delegates to `debugger-specialist` first | R2 |
| T3 `grep -n debugger .claude/settings.json .claude/commands/gini-task.md` hits both | R3 |
| T4 `grep -n debugger-specialist .claude/skills/gini-fix/platform.md` hits "Root-cause tools"; `git diff main -- .claude/skills/gini-fix/SKILL.md` is empty | R4 |
| T5 Dry run: synthetic logcat with `Caused by: java.lang.IllegalStateException: KoinApplication has not been started` and bank-sdk frames → output has every section of the Output Format, hypothesis names global vs isolated Koin, cites `architecture.md`, evidence cites a real `path:LINE` under `bank-sdk/sdk/src/main/java/net/gini/android/bank/sdk/di/`, route `architecture-specialist`, impact and confidence present | R5 |
| T6 Dry run: synthetic ANR main-thread trace with `Tasks.await` under `net.gini.android.capture.internal.qrcode` → route `concurrency-specialist`, rule cited from `coroutines-flow.md` without copying its text | R6 |
| T7 Dry run: synthetic `bank-sdk:sdk:apiCheck` failure output → names the declaration, states `apiCheck` is CI-only vs `/gini-check`, does not propose `apiDump`, route `architecture-specialist`. Second dry run: Gradle log whose first error is `IllegalAccessError` → JDK 17 named, route `none — environment` | R7 |
| T8 Dry run: a bank-sdk test class described as "fails one run in three", with a `Thread.sleep` shown → hypothesis names the timing axis, plan is a repeated `--tests` single-class run, route `testing-specialist` | R8 |
| T9 Follow-up message in the T5 dry run: "apply the fix" → one-sentence refusal + route line; `git status --short` before/after identical | R9 |
| T10 Dry run with only "the app crashes sometimes" → asks for the crash-class artifact list and stops; no hypothesis section emitted | R10 |
| T11 Dry run T6 with "verify it on the emulator" → agent does not run `connectedCheck`, states the block, prints the command; Bash transcript contains no `apiDump`, `ktlintFormat`, `connected`, `commit`, `push`, `reset`, `checkout` | R11 |
| T12 `grep -n 'coroutines-flow.md\|architecture.md\|gini-fix/platform.md\|AGENTS.md' .claude/agents/debugger-specialist.md` hits all four; `git diff main --stat -- .claude/rules/` is empty | R12 |
| T13 `grep -n debugger-specialist .claude/agents/code-reviewer.md .claude/agents/architecture-specialist.md` hits both; `grep -c 'a debugger agent' .claude/agents/architecture-specialist.md` is 0 | R13 |
| T14 `git diff main -- .claude/agents/compose-specialist.md .claude/agents/views-specialist.md .claude/agents/a11y-specialist.md .claude/agents/testing-specialist.md .claude/agents/android-security-specialist.md .claude/agents/concurrency-specialist.md .claude/rules/` is empty | R14 |
| T15 Lint script (scratchpad, written before the agent file): `grep -nE ':[0-9]+' ` on the agent file is empty; `grep -nE '\b[0-9]+ (files|classes|modules|tests)\b'` is empty; every backticked token containing `/` or ending in `.kt`/`.java`/`.kts`/`.xml`/`.pro`/`.yml`/`.properties`, every `<project>:<module>:<task>` and every exception class named resolves (`ls`, `grep -rl`, or a catalog/CI grep) | R15 |
| T16 Every dry run T5–T8 ends with a "Verification status" listing commands run / not run | R16 |
| T17 `code-reviewer` self-review of the branch (per `gini-fix/platform.md` › Self code review), ticket id XPL-444 | — |

### Not tested
- Agent quality across many real incidents — nothing measures it yet (same gap as XPL-437/440/443). The dry runs use synthetic artifacts; a real integrator crash log would be a better T5 and should replace it when one is at hand.
- `code-reviewer` and the orchestrator actually routing at run time beyond the single dry run in T2 — only their text is checked.
- The `Agent` tool's handling of Bash permissions for a subagent (whether a denied Gradle call surfaces as a prompt or an error) — platform behaviour.

## Out of scope

- Any code fix the dry runs surface — report it; the user files a ticket.
- `performance-specialist.md` edits and naming `performance-specialist` in the route list — follow-up after PR #1009 merges (one `ai:` commit: add the route entry, flip its "not written yet" line).
- XPL-445 design-system specialist; any new `.claude/rules/*` file.
- Reviving `bank-sdk.check.ui-tests.yml` or adding `connectedCheck` jobs to health/internal-payment; adding `apiCheck` to `/gini-check`.
- Editing `gini-fix/SKILL.md`, the eight files in R14, `AGENTS.md`, or anything in `gini-mobile-ios`.

## Open questions

None blocking. One follow-up to record, not a question: the ticket's "no `android.util.Log`" is corrected above to "slf4j everywhere, plus a handful of direct `Log` calls" — `/gini-build` writes the precise version into the agent, and the ticket description can be updated to match when the PR is opened.

## Build notes (2026-10-19)

- T5–T8 and T10 ran as proxy dry runs (general-purpose agent instructed from the agent file) because the new agent type only became resolvable mid-session; T9, T11 and T2 ran with the real `debugger-specialist` / `gini-orchestrator` types. All passed; the working tree was byte-identical before and after T9/T11.
- T12/T14 must be diffed against `origin/main`: the local `main` ref is stale (predates the #1007 merge).
- Review fixes applied: `CaptureSdkIsolatedKoinContext` has no `init`/`clean`; the orchestrator's Style-gate bullet now matches the debugger's Bash; `SourceFile:NN`; `updateLintBaseline`; `/data/anr` wording; the JDK note follows `AGENTS.md` (the "`main` builds on JDK 21 since KSP" observation from T7b goes to `/gini-reflect`, not into the agent).
- Real findings surfaced by the dry runs, for tickets (not fixed here): `BankSdkIsolatedKoinContext.koin` is a sticky `lazy` that `clean()` never resets; main-thread `runBlocking` plus the `educationMutex`-as-signal in capture-sdk `camera/CameraFragmentExtension.kt`; `QRCodeDetectorHandler` skips `onDetectionFinished()` on exception, freezing the analyzer under `KEEP_ONLY_LATEST`; `coroutines-flow.md` › Repo reality lists one `runBlocking` site while six exist.

## Implementation plan
- [x] 1. Lint script in the scratchpad for T15 (`file:line` citations, hard-coded counts, every backticked path / task / exception class resolves) — written before the agent file it checks (requirement 15)
- [x] 2. `.claude/agents/debugger-specialist.md` — frontmatter, opening contract, Bash discipline, SDK-not-app section, Repo Context, Knowledge Source, four investigation classes, Investigation Process, Output Format, Impact guide, Boundaries (requirements 1, 5, 6, 7, 8, 9, 10, 11, 12, 15, 16)
- [x] 3. `.claude/skills/gini-fix/platform.md` — "Root-cause tools" pointer paragraph; `SKILL.md` untouched (requirement 4)
- [x] 4. Wiring: `gini-orchestrator.md` (description, team row, delegation rule + renumber, Knowledge Sources), `gini-task.md`, `settings.json` hook, `code-reviewer.md` escalation entry, `architecture-specialist.md` boundaries line (requirements 2, 3, 13; 14 by not touching the other eight files)
- [x] 5. Verify: T1–T4, T12–T15 by Bash; dry runs T5–T11, T16 via the `Agent` tool with `subagent_type: debugger-specialist`; `code-reviewer` self-review (T17)

# AGENTS.md — Gini Mobile Android

Canonical instructions for coding agents working in this repository. Verify against the code when in doubt; `README.md`, `RELEASE.md`, and `MAINTENANCE.md` hold the full human docs.

## Project overview

Monorepo of Gini's Android SDKs and API client libraries, published to Maven Central under the `net.gini.android` group id. Multi-module Gradle project (Kotlin DSL throughout). Each top-level folder is a "project" with releasable sub-modules:

| Project | Modules | Root package |
|---|---|---|
| `core-api-library` | `library`, `shared-tests` | `net.gini.android.core.api` |
| `health-api-library` | `library` | `net.gini.android.health.api` |
| `bank-api-library` | `library` | `net.gini.android.bank.api` |
| `capture-sdk` | `sdk`, `default-network` | `net.gini.android.capture` |
| `bank-sdk` | `sdk`, `example-app` | `net.gini.android.bank.sdk` |
| `health-sdk` | `sdk`, `example-app` | `net.gini.android.health.sdk` |
| `internal-payment-sdk` | `sdk` | `net.gini.android.internal.payment` |

Inter-module dependency chain (project dependencies, resolved locally):
`core-api-library` ← `health-api-library` / `bank-api-library`; `health-api-library` ← `internal-payment-sdk` ← `health-sdk`; `bank-api-library` ← `capture-sdk:default-network`; `capture-sdk:sdk` + `capture-sdk:default-network` + `bank-api-library` ← `bank-sdk`. `RELEASE-ORDER.md` is auto-generated (`updateReleaseOrderFile` task) — never edit it manually.

Source lives under `src/main/java/` even for Kotlin files. Toolchain: JDK 17, Kotlin 2.0.20, compileSdk/targetSdk 36, minSdk 23, SDK modules compile to JVM target 1.8. These versions are a point-in-time snapshot — `gradle/libs.versions.toml` is the source of truth if they have drifted.

## Build, test, lint

Always use the Gradle wrapper and fully qualified module paths — `<project>:<module>:<task>`, e.g. `bank-sdk:sdk:lint` or `core-api-library:library:apiCheck`. The same task set applies to every module:

```bash
./gradlew <project>:<module>:assembleDebug       # build one module
./gradlew <project>:<module>:testDebugUnitTest   # unit tests
./gradlew <project>:<module>:connectedCheck      # instrumented tests (device/emulator; uses AndroidX Test Orchestrator)
./gradlew <project>:<module>:lint                # Android Lint
./gradlew <project>:<module>:detekt              # Detekt (config/detekt/detekt.yml)
./gradlew <project>:<module>:ktlintCheck         # ktlint
./gradlew <project>:<module>:apiCheck            # binary-compatibility check (see below)
./gradlew <project>:<module>:apiDump             # regenerate api/*.api after intentional public API change

# Example: run the unit tests of the Bank SDK
./gradlew bank-sdk:sdk:testDebugUnitTest
```

CI (GitHub Actions, `.github/workflows/<project>.check.yml`) runs per module: `testDebugUnitTest`, `apiCheck`, `lint`, `detekt`, `ktlintCheck`, plus example-app assembly and Sonar/Jacoco. The API libraries', capture-sdk's and bank-sdk's check workflows also run `connectedCheck` on an emulator (bank-sdk additionally has a separate `bank-sdk.check.ui-tests.yml`). Match that set locally before pushing. Checkstyle/PMD tasks exist for legacy Java code but are non-blocking (`ignoreFailures = true`).

Reference docs (Dokka): `./gradlew <project>:<module>:dokkaHtmlSiblingCollector` → `<project>/<module>/build/docs/dokka`. Sphinx-based integration guides exist alongside the reference docs; both build via `bundle exec fastlane build_documentation project_id:<project> module_id:<module>` → `<project>/<module>/build/docs/html` (guides) and `.../docs/dokka` (reference). Release automation runs through fastlane (`fastlane/` folder, `bundle exec fastlane ...`).

## Public API compatibility — always

Releasable modules apply `binary-compatibility-validator`. Each module's public API is a committed dump in its `api/` folder (e.g. `core-api-library/library/api/library.api`).

- If `apiCheck` fails unexpectedly, you exposed API by accident (Kotlin is public by default) — restrict visibility instead of updating the dump.
- For intentional API changes: run `apiDump`, review the diff (removals/signature changes break clients — deprecate first, remove in the next major), and commit the dump with the code change.

## Dependencies & versioning

- External dependencies go through the version catalog `gradle/libs.versions.toml` (`libs.` accessors). Never hardcode versions in module build files; never add repositories to modules (`FAIL_ON_PROJECT_REPOS` is enforced — repos are declared in `settings.gradle.kts` only).
- Cross-SDK dependencies are project dependencies (e.g. `project(":core-api-library:library")`).
- Each releasable module's version and Maven POM properties live in its own `gradle.properties`.
- Custom Gradle logic lives in `buildSrc` plugins (`PublishToMavenPlugin`, `DokkaPlugin`, `CodeAnalysisPlugin`, `JacocoCoveragePlugin`, `SBOMPlugin`, `ReleaseOrderPlugin`, …) — extend those rather than adding ad-hoc tasks to module build files.
- To check for newer dependency versions run `./gradlew dependencyUpdatesForAndroidProjects` — it lists each outdated dependency with the projects it is used in (see `MAINTENANCE.md`).

## Architecture & code style

- **Kotlin first.** New code is Kotlin with coroutines. `capture-sdk` still contains substantial legacy Java (~half its files) — don't convert it opportunistically; follow the style of the file you're editing.
- **UI pattern:** MVVM with Jetpack `ViewModel` and `StateFlow`/`SharedFlow` for state (used across capture/bank/health/internal-payment SDKs). Views are Fragment/View-based with ViewBinding; Jetpack Compose is used in parts of `capture-sdk`/`bank-sdk` and the health example app. No Hilt in the SDK modules themselves (only the `bank-sdk` example app uses it) — SDKs wire dependencies manually to stay DI-framework-agnostic for integrators.
- **Networking:** Retrofit + OkHttp + Moshi in the API libraries (Retrofit interfaces + remote-source classes, e.g. `HealthApiDocumentRemoteSource`).
- Public entry points are singleton-style facade classes (`GiniHealth`, `GiniBank`, `GiniCapture`, …).
- Kotlin declarations are public by default — mark everything `internal`/`private` unless it is deliberately part of the SDK's public API (enforced by `apiCheck`).
- Style is enforced by ktlint and Detekt (`config/detekt/detekt.yml`); run both before committing.

## Testing conventions

- Unit tests in `src/test/java`, named `<ClassUnderTest>Test.kt`; instrumented tests in `src/androidTest`.
- Stack: JUnit4, MockK (newer modules) / Mockito-Kotlin (older ones), Robolectric, Google Truth, Turbine (Flow testing), `kotlinx-coroutines-test`, OkHttp MockWebServer for API libraries, Espresso for UI.
- `core-api-library:shared-tests` provides shared test helpers consumed by the API libraries' androidTests.
- Instrumented tests run with `AndroidJUnitRunner` under the AndroidX Test Orchestrator.

## Git conventions

- GitHub flow: branch from `main`, PR back into `main`. Merge commits; rebase/squash only on feature branches.
- Commit format (see `.git-stuff/commit-msg-template.txt`):

  ```
  <type>(<project>): <subject>

  <body>

  <ticket-id>
  ```

  `type` ∈ `feat` | `fix` | `refactor` | `docs` | `ci` (`chore` for cross-cutting changes); `project` is the top-level folder, e.g. `feat(bank-sdk): Add error logging interface`. Subject in imperative mood; body explains what/why.
- Release tags: `<project-name>;<version>` (e.g. `bank-sdk;1.0.2`) — tags trigger release workflows, so never push them casually.
- Releases follow `RELEASE.md`: `capture-sdk:default-network` is always version-bumped together with `capture-sdk`, and multiple major versions (1.x/2.x/3.x) are maintained on parallel branches — fixes for older majors must branch from the matching version branch, not `main`.

## Gotchas

- Gradle must run on JDK 17 (newer JDKs cause `IllegalAccessError` in the build).
- `capture-sdk:screen-api-example-app` is commented out in `settings.gradle.kts` — it won't resolve.
- The example apps read `clientId`/`clientSecret` from a `local.properties` in their own module folder (e.g. `health-sdk/example-app/local.properties`) or from `-P` properties — the health-sdk example app fails to configure without them; the bank-sdk example app falls back to empty strings (and won't reach the Gini API).
- `bank-sdk:example-app` uses two flavor dimensions (`environment`: prod/dev/qa; `purpose`: exampleApp/paymentProviderN) — build a single variant, e.g. `assembleDevExampleAppDebug` (CI builds `assembleQaExampleAppRelease` and `assembleProdExampleAppRelease`); a plain `assembleDebug` builds every flavor combination.
- Keep vector-drawable handling as-is (`vectorDrawables.useSupportLibrary = true`); see comments in module build files before touching drawables.
- Complex automation belongs in fastlane lanes, not GitHub Actions steps (lanes must be runnable locally).

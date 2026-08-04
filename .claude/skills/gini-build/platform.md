# gini-build platform conventions — Android (gini-mobile-android)

<!--
  NOT MIRRORED — this file is Android-specific by design. The iOS repo has its
  own platform.md with the same section headings but iOS content. If you add a
  section here that the shared workflow depends on, add the matching section
  to the iOS platform.md too.
-->

## Where code and tests live

- Multi-module Gradle monorepo; module paths are `<project>:<module>`
  (e.g. `bank-sdk:sdk`). See AGENTS.md for the module table and the
  inter-module dependency chain.
- Source lives under `src/main/java/` even for Kotlin files.
- Unit tests in `src/test/java`, named `<ClassUnderTest>Test.kt`; instrumented
  tests in `src/androidTest`. Match the neighboring tests' stack (MockK vs.
  Mockito-Kotlin, Robolectric, Turbine, Compose rules) — the spec's test plan
  names it.

## Building and running tests during implementation

Always the Gradle wrapper with fully qualified module paths:

```bash
./gradlew <project>:<module>:assembleDebug        # build one module
./gradlew <project>:<module>:testDebugUnitTest    # unit tests
```

To run a single test class while iterating:

```bash
./gradlew <project>:<module>:testDebugUnitTest --tests "net.gini.android.…SomeClassTest"
```

Gradle must run on JDK 17 (newer JDKs fail with IllegalAccessError).

## Verification (step 5 of the workflow)

Run the `/gini-check` skill — it runs the CI gate (testDebugUnitTest, lint,
detekt, ktlintCheck) for the modules affected by the current changes,
expanding through the dependency chain. Everything must pass.

If the spec's test plan includes instrumented/UI tests, also run
`/gini-connected-check` (needs a device or emulator).

## Coding conventions

The spec's "Technical conventions" section is the feature-specific contract.
Repo-wide rules live in AGENTS.md; the ones most often violated:

- New classes in Kotlin, `internal`/`private` unless deliberately public API.
- Dependencies only via the version catalog (`libs.` accessors); never
  hardcode versions or add repositories to module build files.
- No LiveData/RxJava — coroutines + StateFlow/SharedFlow.
- Don't convert legacy Java opportunistically; follow the style of the file
  you're editing.

## Commit conventions

Format (see `.git-stuff/commit-msg-template.txt`):

```
<type>(<project>): <subject>

<body>

<ticket-id>
```

The commit template at `.git-stuff/commit-msg-template.txt` (repo root) is the
source of truth for the allowed `type` values and what each covers — read it
rather than relying on a list duplicated here.
`project` is the top-level folder, e.g. `feat(bank-sdk): Add error logging
interface`. Subject in imperative mood; the ticket id ($ARGUMENTS) goes in
the footer. Never push release tags (`<project>;<version>`) — they trigger
release workflows.

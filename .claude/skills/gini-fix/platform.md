# gini-fix platform conventions — Android (gini-mobile-android)

<!--
  NOT MIRRORED — this file is Android-specific by design. The iOS repo has its
  own platform.md with the same section headings but iOS content. If you add a
  section here that the shared workflow depends on, add the matching section
  to the iOS platform.md too.
-->

## Reproducing bugs

Prefer the cheapest faithful reproduction, in this order:

1. **Unit test** — most SDK logic is testable with JUnit4 + Robolectric
   without a device. Put the repro test where the regression test will live
   (`src/test/java` of the module owning the root cause).
2. **Instrumented test** (`src/androidTest`) when the bug needs real Android
   framework behavior — runs via
   `./gradlew <project>:<module>:connectedCheck` (device/emulator required).
3. **Example app** as a last resort:
   - health-sdk: `health-sdk/example-app` — needs `clientId`/`clientSecret`
     in `health-sdk/example-app/local.properties`, fails to configure
     without them.
   - bank-sdk: `bank-sdk/example-app` — two flavor dimensions; build ONE
     variant, e.g. `./gradlew bank-sdk:example-app:assembleDevExampleAppDebug`
     (falls back to empty credentials and won't reach the Gini API without
     `local.properties`).
   - Logs: `adb logcat` filtered by the app package or a tag from the SDK.

Version-specific bugs: check the ticket's SDK version against the module's
`gradle.properties` and, for older majors (1.x/2.x/3.x), remember fixes for
those branch from the matching version branch, not `main` (see RELEASE.md).

## Root-cause tools

- Single test class while narrowing:
  `./gradlew <project>:<module>:testDebugUnitTest --tests "…SomeClassTest"`
  (Gradle needs JDK 17).
- History of a suspicious file: `git log --follow -p -- <path>` and
  `git blame <path>` — release tags (`<project>;<version>`) tell you which
  release introduced a change.
- Inter-module effects: a bug surfacing in bank-sdk may root-cause in
  capture-sdk or core-api-library — see the dependency chain in AGENTS.md.

## Regression test conventions

- Unit tests in `src/test/java`, `<ClassUnderTest>Test.kt`; match the
  neighboring stack (MockK vs. Mockito-Kotlin, Robolectric, Turbine, Truth,
  MockWebServer in the API libraries).
- Name the test after the behavior, and reference the ticket in a comment
  only if the file's existing tests do so — match local style.

## Verification (step 7 of the workflow)

Run the `/gini-check` skill — the CI gate (testDebugUnitTest, lint, detekt,
ktlintCheck) for affected modules, expanded through the dependency chain.
If the regression test is instrumented, also run `/gini-connected-check`.

## Commit conventions

Format (see `.git-stuff/commit-msg-template.txt`):

```
fix(<project>): <subject>

<body: what was broken, the root cause, what the fix does>

<ticket-id>
```

`project` is the top-level folder, subject in imperative mood, ticket id
($ARGUMENTS) in the footer. Never push release tags.

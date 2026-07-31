# gini-spec-feature platform conventions — Android (gini-mobile-android)

<!--
  NOT MIRRORED — this file is Android-specific by design. The iOS repo has its
  own platform.md with the same section headings but iOS content. If you add a
  section here that the shared workflow depends on, add the matching section
  to the iOS platform.md too.
-->

## Module map

Multi-module Gradle monorepo. Affected modules are identified by these
top-level projects (see AGENTS.md for the full table and dependency chain):
core-api-library, health-api-library, bank-api-library, capture-sdk,
bank-sdk, health-sdk, internal-payment-sdk.

Inter-module dependencies are Gradle project dependencies — a change in
core-api-library ripples into everything; capture-sdk changes ripple into
bank-sdk. Name the affected Gradle modules with full paths
(e.g. `bank-sdk:sdk`, `capture-sdk:default-network`).

## Public API assessment

Integrator-visible means `public` Kotlin/Java declarations. There are no
`api/*.api` binary-compatibility dumps in this repo (no
binary-compatibility-validator yet), so assess source-level visibility
instead of looking for dump files.

## Architecture patterns in use

This repo is mixed — name the pattern for new code and match a precedent that
exists in the touched module:

- Legacy Java MVP (Contract/Presenter), e.g. the capture-sdk Analysis screen.
  Stays in place — integrate at its contract boundary, don't rewrite it.
- MVVM with Jetpack `ViewModel` + `StateFlow` — the AGENTS.md default for new
  code.
- orbit-mvi (`ContainerHost` + sealed intents) — bank-sdk Skonto screens only.
  Use MVI intents only where the module already uses them.

## Language rules

- New classes in Kotlin, `internal` unless deliberately part of the public
  API. Source lives under `src/main/java/` even for Kotlin files.
- capture-sdk contains substantial legacy Java — don't convert it
  opportunistically. The spec must say which legacy Java files may be touched
  and why.

## UI rules

- New UI: Jetpack Compose (Material 3) wrapped in `GiniTheme`, with light/dark
  `@Preview`s. State whether XML layouts are added/removed.
- Keep vector-drawable handling as-is (`vectorDrawables.useSupportLibrary`).

## Wiring

- DI: isolated Koin context in capture-sdk, manual wiring elsewhere — never
  Hilt in SDK modules (only the bank-sdk example app uses it).
- Async: coroutines + `StateFlow`/`SharedFlow`; no LiveData, no RxJava.
- Strings/resources: name which locale folders get new entries and any
  placeholders; check which locale folders exist in the touched module.

## Test stack

- Unit tests in `src/test/java`, named `<ClassUnderTest>Test.kt`; instrumented
  tests in `src/androidTest`.
- JUnit4; MockK (newer modules) vs. Mockito-Kotlin (older ones) — match the
  neighboring tests in the module. Robolectric, Google Truth, Turbine for
  Flows, `kotlinx-coroutines-test`, MockWebServer for API libraries, Espresso
  and Compose test rules for UI.
- Every new Kotlin class gets a unit test.

## Conventions checklist for the spec

The spec's "Technical conventions" section must cover, grounded in the
modules actually touched:

1. Language: Kotlin, `internal` by default; which legacy Java files may be
   touched and why.
2. UI: Compose (Material 3) in `GiniTheme` with light/dark previews; XML
   layouts added/removed.
3. Architecture: the pattern for new code (MVVM ViewModel + StateFlow by
   default; MVI only where the module already uses it; legacy MVP stays).
   Name the state/intent/effect classes.
4. DI wiring (Koin in capture-sdk, manual elsewhere; no Hilt in SDKs) and
   async (coroutines + StateFlow/SharedFlow, no LiveData/RxJava).
5. Strings/resources: locale folders, placeholders.
6. Quality gates: ktlint + detekt clean; Jacoco/Sonar coverage expectations
   for new classes.

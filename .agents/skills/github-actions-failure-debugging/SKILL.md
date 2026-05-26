---
name: github-actions-failure-debugging
description: Guide for debugging failing GitHub Actions workflows in this repo. Use this when asked to debug or investigate CI/CD failures, failing workflow runs, broken checks on a PR, or flaky tests in GitHub Actions.
---

This repo has 35 workflow files in `.github/workflows/`, named `<module>.<subject>.yml`. Use the following process to diagnose failures.

## Workflow naming convention

| Pattern | Purpose |
|---|---|
| `*.check.yml` | PR checks: unit tests, instrumented tests, lint, detekt, ktlint, sonar |
| `*.release.yml` | Triggered by a git tag `<name>;<semver>`; publishes to Maven Central |
| `*.docs.build.yml` / `*.docs.release.yml` | Builds/publishes Dokka HTML docs to `gh-pages` |
| `*.publish.firebase.example.yml` | Distributes example APK via Firebase App Distribution |

## Debugging process

1. **Identify the failed workflow and job** using the GitHub MCP tools:
   - List recent workflow runs for the branch/PR
   - Find which jobs failed and their job IDs

2. **Get a summary of the failure logs** — use `summarize_job_log_failures` (or equivalent) to get an AI-summarized view of why jobs failed without flooding context with thousands of lines.

3. **Deep-dive into specific failures** if the summary is insufficient:
   - Fetch the full job logs for the specific failed job
   - Look for the first error line, not just the last

4. **Common failure categories and where to look:**

   ### Unit test failures (`testDebugUnitTest`)
   - Run locally: `./gradlew :<module>:testDebugUnitTest`
   - Check MockK/Mockito mock setup, Robolectric shadow conflicts, coroutine test dispatcher leaks

   ### Instrumented test failures (`connectedCheck`)
   - These run on an API 33 x86_64 emulator with KVM acceleration
   - Secrets are injected at build time via `injectTestProperties` task → `src/androidTest/assets/test.properties`
   - Flakiness is common; check if the emulator failed to boot (look for AVD snapshot errors)
   - Missing secrets (`GINI_MOBILE_TEST_CLIENT_SECRET`, etc.) cause auth failures — these must be set in repo secrets

   ### Detekt / ktlint failures
   - Detekt is **enforced** (not warnings-only) on most modules — see the `detekt-ktlint-fixes` skill
   - Run locally: `./gradlew :<module>:detekt` or `./gradlew :<module>:ktlintCheck`

   ### Build failures
   - Check AGP/Kotlin version conflicts — versions are in `gradle/libs.versions.toml`
   - Check custom buildSrc plugin errors — see `buildSrc/src/main/kotlin/net/gini/gradle/`
   - See the `gradle-build-debugging` skill for more

   ### Maven Central publishing failures
   - Tag format must be exactly `<module-name>;<semver>` (e.g., `bank-sdk;4.1.0`)
   - Requires `MAVEN_CENTRAL_USER_TOKEN_*` and signing secrets to be set
   - Fastlane lane: `publish_to_maven_repo` (release) or `publish_to_maven_snapshots_repo` (snapshot)

   ### SonarCloud failures
   - `SONAR_TOKEN` secret must be set
   - Sonar runs as a separate job after tests; check that coverage XML reports were generated

5. **Reproduce the failure locally** using the same Gradle task that the workflow runs. Match the Java version (Temurin 17) and Gradle cache state.

6. **Fix and verify** — after fixing, confirm the same workflow task passes locally before pushing.

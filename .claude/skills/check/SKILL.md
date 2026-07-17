---
name: check
description: Run the CI check suite (testDebugUnitTest, apiCheck, lint, detekt, ktlintCheck) locally for the modules affected by the current changes, expanding through the inter-module dependency chain. Use before pushing, or when asked to "run the checks" / "verify CI will pass". Accepts optional explicit module paths as arguments (e.g. "bank-sdk:sdk").
---

# /check — run the CI gate for affected modules

Run the same per-module task set as the GitHub Actions check workflows, but locally and only for the modules affected by the current changes.

## 1. Determine changed files

If the user passed explicit module paths as arguments (e.g. `bank-sdk:sdk core-api-library:library`), skip to step 3 with exactly those modules.

Otherwise collect the changed files:

```bash
git diff --name-only $(git merge-base HEAD origin/main 2>/dev/null || git merge-base HEAD main)
git diff --name-only            # unstaged
git diff --name-only --cached   # staged
git ls-files --others --exclude-standard  # untracked
```

Union all four lists. If `origin/main` and `main` are both unavailable, ask the user what to diff against.

## 2. Map changed files to affected projects

Map each changed file to a top-level project by its first path segment. Then expand the set using this dependency table (if a project on the left changed, every project on the right is also affected):

| Changed project | Also affected (transitively) |
|---|---|
| `core-api-library` | `health-api-library`, `bank-api-library`, `internal-payment-sdk`, `health-sdk`, `capture-sdk`, `bank-sdk` |
| `health-api-library` | `internal-payment-sdk`, `health-sdk` |
| `bank-api-library` | `capture-sdk` (default-network only), `bank-sdk` |
| `internal-payment-sdk` | `health-sdk` |
| `capture-sdk` | `bank-sdk` |
| `health-sdk` | — |
| `bank-sdk` | — |

Special cases:
- Changes under `gradle/`, `buildSrc/`, or to root `build.gradle.kts` / `settings.gradle.kts` / `gradle.properties` affect **all** projects.
- Changes only to documentation (`*.md`), `.github/`, `fastlane/`, or `.claude/` affect **no** modules — report "no checkable modules affected" and stop.
- Changes under an `example-app/` folder: the example apps have no check suite of their own; verify them with `assembleDebug` (health-sdk) or `assembleDevExampleAppDebug` (bank-sdk) instead of the task set below.

Project → checkable modules:

| Project | Modules to check |
|---|---|
| `core-api-library` | `core-api-library:library` |
| `health-api-library` | `health-api-library:library` |
| `bank-api-library` | `bank-api-library:library` |
| `capture-sdk` | `capture-sdk:sdk`, `capture-sdk:default-network` |
| `bank-sdk` | `bank-sdk:sdk` |
| `health-sdk` | `health-sdk:sdk` |
| `internal-payment-sdk` | `internal-payment-sdk:sdk` |

Exception: when `bank-api-library` changed but `capture-sdk` sources did not, only `capture-sdk:default-network` needs checking from the capture-sdk project (not `capture-sdk:sdk`).

Before running, tell the user which modules will be checked and why (which change pulled each one in).

## 3. Run the CI task set

For the affected modules, run the five CI tasks in a single Gradle invocation with `--continue` so one failure doesn't hide others:

```bash
./gradlew --continue \
  <module>:testDebugUnitTest <module>:apiCheck <module>:lint <module>:detekt <module>:ktlintCheck \
  <next-module>:testDebugUnitTest ...
```

Notes:
- Gradle must run on JDK 17 — if the build fails with `IllegalAccessError: superclass access check failed`, that's the wrong JDK, not a code problem.
- This intentionally omits `connectedCheck` (needs a device/emulator) and Sonar/Jacoco (CI-only). Mention the omission in the summary when the affected projects are ones whose CI runs instrumented tests (both API libraries, core-api-library, capture-sdk, bank-sdk).

## 4. Report

Summarize as a table: module × task → pass/fail. For failures, quote the relevant error output (test name + assertion, lint/detekt rule, or the apiCheck diff) and state the likely fix:
- `apiCheck` failure: accidentally exposed API → restrict visibility; intentional API change → run `<module>:apiDump`, review the diff for removals/signature changes (deprecate first!), and commit the dump.
- `ktlintCheck` failure: offer to run `<module>:ktlintFormat`.

End with a clear verdict: "CI should pass" or "CI will fail on: …".

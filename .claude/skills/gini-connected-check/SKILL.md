---
name: gini-connected-check
description: Run the CI steps that /gini-check omits — instrumented tests (connectedCheck) on a device/emulator, plus optional Jacoco coverage and Sonar analysis — for the modules affected by the current changes. Use after /gini-check passes, or when asked to "run the instrumented tests" / "run connectedCheck locally". Accepts optional explicit module paths as arguments (e.g. "bank-api-library:library").
---

# /gini-connected-check — run instrumented tests (and optionally Jacoco/Sonar) for affected modules

Complements `/gini-check`: runs the remaining CI steps that need a device/emulator or external services. Reuse `/gini-check`'s steps 1–2 to determine the affected projects (same changed-file collection and dependency-expansion table), then filter to the modules below.

## 1. Modules with a connectedCheck suite in CI

Only these modules run instrumented tests in CI — ignore affected projects not listed here (`health-sdk` and `internal-payment-sdk` have no connectedCheck job):

| Module | CI API levels | Needs Gini API credentials |
|---|---|---|
| `core-api-library:library` | 23, 26, 31 | no (self-contained) |
| `health-api-library:library` | 23, 33 | yes |
| `bank-api-library:library` | 23, 31 | yes |
| `capture-sdk:sdk` | 33 | no |
| `capture-sdk:default-network` | 31 | yes |
| `bank-sdk:sdk` | 33 | yes |

If the user passed explicit module paths as arguments, use exactly those. `capture-sdk:sdk` and `bank-sdk:sdk` suites are UI-heavy and slow — when they are only transitively affected (no source changes of their own), propose skipping them and letting CI cover them.

## 2. Preflight: device and credentials

**Device.** Run `adb devices` — exactly one booted device/emulator must be listed. If none, offer to start one: `emulator -list-avds`, then `emulator -avd <name> -no-snapshot-save -no-audio -no-boot-anim &` and wait for `adb wait-for-device` + `sys.boot_completed`. Prefer an AVD whose API level matches the CI matrix (any level ≥ minSdk 23 is acceptable locally; mention the mismatch). If more than one device is connected, ask which to use or set `ANDROID_SERIAL`.

**Credentials.** Modules marked "yes" above read `testClientId` / `testClientSecret` / `testApiUri` / `testUserCenterUri` (plus `testHealthApiUri` for bank-api-library, `testBankApiUri` for health-api-library) via an `injectTestProperties` task that writes `src/androidTest/assets/test.properties`. The values come from the **module's own** `local.properties` (e.g. `bank-api-library/library/local.properties`) or `-P` properties. The read is silent — missing values become empty strings and the tests fail at runtime with authentication errors, so verify up front that each needed property resolves. CI uses:

```
-PtestClientId="gini-mobile-ci" -PtestClientSecret="<secret>"
-PtestApiUri="https://pay-api.gini.net" -PtestUserCenterUri="https://user.gini.net"
# bank-api-library additionally: -PtestHealthApiUri="https://health-api.gini.net"
# health-api-library instead:    -PtestApiUri="https://health-api.gini.net" -PtestBankApiUri="https://pay-api.gini.net"
#                                and -Pandroid.testInstrumentationRunnerArguments.apiEnv=production
```

The secret is not in the repo — if no local.properties has it and the user didn't provide one, stop and ask for it (never invent or commit credentials; test.properties must not be committed).

## 3. Run connectedCheck

Run modules **one at a time** (they share the device; parallel installs conflict). Before each module, uninstall its stale test APK like CI does:

```bash
adb uninstall <test-package> || true
./gradlew <module>:connectedCheck [credential -P properties]
```

Test packages: `net.gini.android.core.api.test`, `net.gini.android.health.api.test`, `net.gini.android.bank.api.test`, `net.gini.android.capture.test`, `net.gini.android.capture.network.test`, `net.gini.android.bank.sdk.test`.

Notes:
- Tests run under the AndroidX Test Orchestrator; per-test APK reinstalls make runs slow — CI allows 20 minutes per module.
- Results land in `<module dir>/build/outputs/androidTest-results/connected` and reports in `build/reports/androidTests/connected`.
- Credentialed modules hit the **production** Gini API — flakes can be environment-side; retry a failed suite once before treating it as a code failure.

## 4. Jacoco and Sonar (optional — ask before running)

- **Jacoco:** `./gradlew <module>:jacocoTestReport` re-runs unit tests and writes a coverage report — safe locally, but adds no pass/fail signal. Run it when the user wants coverage numbers, or as the input Sonar needs.
- **Sonar:** CI runs `./gradlew <module>:sonar --info --stacktrace` with `SONAR_TOKEN` set, after `jacocoTestReport`. Locally this **uploads analysis to Gini's SonarQube server** — only run it if the user explicitly confirms and `SONAR_TOKEN` is set in the environment; otherwise report it as CI-only. A local run without PR context can pollute the server's analysis history.
- bank-sdk and capture-sdk `jacocoTestDebugUnitTestReport` steps are disabled in CI (Kotlin SMAP exception) — don't run those variants.

## 5. Report

Summarize as a table: module → connectedCheck pass/fail (+ Jacoco/Sonar if run), with device/API level used. For failures quote the failing test and assertion from the connected test report, and state whether it looks code-related or environment-related (auth errors → credentials; timeouts/5xx → API side). End with a verdict: which CI instrumented jobs should pass, and which steps remain unverified (e.g. API levels not tested locally, Sonar quality gate).

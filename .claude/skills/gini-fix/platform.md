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

### Self code review — after the gate is green

Once `/gini-check` passes, hand the finished work to the **`code-reviewer`**
agent (Task tool, `subagent_type: "code-reviewer"`) for a pre-push
self-review. It reads the branch commits **and** the uncommitted working tree,
applies the `/gini-review` rulebook (`AGENTS.md`,
`.claude/rules/coroutines-flow.md`, `.claude/skills/gini-review/platform.md`,
`.claude/skills/gini-review/references/general-rules.md`), and returns
triaged findings — no verdict, no posted comments, no edits, no commits.

Give it the ticket id so it can read `specs/<ticket>-bug.md` and check that
the fix addresses the diagnosed root cause, not only the symptom, and that
the regression test fails without the fix. If the branch is
stacked on another feature branch, also give it that base branch.

Handling what it returns:

- **Blockers and warnings**: fix them, re-run `/gini-check` for the
  affected modules, then run `code-reviewer` once more on the result. The
  same 3-attempt bound from step 7 applies.
- **Escalations** (`→ escalate to <agent>`): route those to the named
  specialist via the `gini-orchestrator`, or raise them with the user if the
  fix would grow beyond a fix — that is a `/gini-plan` escalation.
- **Verification gaps** it names — most often `apiCheck`, which CI runs but
  `/gini-check` does not: run them before offering to commit.
- **Nits**: the author's call; do not silently expand the change for them.
- **No findings is a normal outcome** — report it as-is.

Run this before offering to commit, and report its findings in the wrap-up
summary. It is a review, not a gate: it cannot replace `/gini-check`, and a
clean self-review never means the checks passed.

For a full pull-request review against the Jira ticket's acceptance criteria,
with a coverage ledger and the option to post inline comments, use the
`/gini-review` skill instead — that is a separate, later step.

## Commit conventions

Format (see `.git-stuff/commit-msg-template.txt`):

```
fix(<project>): <subject>

<body: what was broken, the root cause, what the fix does>

<ticket-id>
```

`project` is the top-level folder, subject in imperative mood, ticket id
($ARGUMENTS) in the footer. Never push release tags.

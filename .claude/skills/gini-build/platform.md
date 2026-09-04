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

### Self code review — after the gate is green

Once `/gini-check` passes, hand the finished work to the **`code-reviewer`**
agent (Task tool, `subagent_type: "code-reviewer"`) for a pre-push
self-review. It reads the branch commits **and** the uncommitted working
tree, applies the `/gini-review` rulebook (`AGENTS.md`,
`.claude/skills/gini-review/platform.md`,
`.claude/skills/gini-review/references/general-rules.md`), and returns
triaged findings — no verdict, no posted comments, no commits.

Give it the ticket id so it can read the spec and check the implementation
against the requirements and the "Out of scope" section.

Handling what it returns:

- **Blockers and warnings**: fix them, then re-run `/gini-check` for the
  affected modules. The same 3-attempt bound from step 5 applies.
- **Escalations** (`→ escalate to <agent>`): route those to the named
  specialist via the `gini-orchestrator`, or raise them with the user if the
  fix would widen the spec's scope.
- **Nits**: the author's call; do not silently expand the change for them.
- **No findings is a normal outcome** — report it as-is.

Run this before offering to commit, and report its findings in the step 6
summary. It is a review, not a gate: it cannot replace `/gini-check`, and a
clean self-review never means the CI gate passed.

For a full pull-request review against the Jira ticket's acceptance criteria,
with a coverage ledger and the option to post inline comments, use the
`/gini-review` skill instead — that is a separate, later step.

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

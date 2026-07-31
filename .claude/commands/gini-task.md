---
description: Hand a Kotlin/Android task to the Gini Android agent team — the gini-orchestrator routes it to the right specialists to review or implement per the repo standards.
argument-hint: <what you want done or reviewed>
---

Use the **gini-orchestrator** agent to coordinate the following task. Invoke it via the Task tool with `subagent_type: "gini-orchestrator"` and pass the task through.

Task:
$ARGUMENTS

gini-orchestrator will read the task, select the right specialists (compose, views, a11y, testing), and enforce the standards in `AGENTS.md` (Kotlin-first, MVVM + `StateFlow`/Orbit-MVI, singleton-facade-returns-`Fragment` entry, Koin DI, `GiniTheme` design tokens, version-catalog-only deps, ktlint + Detekt gate, KDoc, `<type>(<project>): <subject>` commits, `internal`-by-default visibility, no mocks in production, built-ins first). Architecture, DI, coroutines, security, performance, and localization standards apply inline.

When it finishes, synthesize the specialists' findings into a single clear answer for me — grouped by specialist, most important issues first.

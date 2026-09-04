---
description: Hand a Kotlin/Android task to the Gini Android agent team — the gini-orchestrator routes it to the right specialists to review or implement per the repo standards.
argument-hint: <what you want done or reviewed>
---

Use the **gini-orchestrator** agent to coordinate the following task. Invoke it via the Task tool with `subagent_type: "gini-orchestrator"` and pass the task through.

Task:
$ARGUMENTS

gini-orchestrator will read the task, select the right specialists — compose, views, a11y, testing, concurrency, architecture, security, performance, design-system, debugger, and code-reviewer — and enforce the repository standards. `AGENTS.md` is the canonical source; the orchestrator's Mandatory Rules restate it with repo specifics, and its Review bundles table gives the default routing per kind of change. Localization standards have no dedicated specialist and apply inline.

Two routings are non-negotiable: user-facing UI always goes to a11y-specialist, and a public API change or a changed `api/*.api` dump always goes to architecture-specialist. A crash, ANR, build failure, or flaky test goes to android-debugger-agent first — it diagnoses, then names the specialist who owns the rule behind it.

When it finishes, synthesize the specialists' findings into a single clear answer for me — grouped by specialist, most important issues first.

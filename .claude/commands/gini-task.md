---
description: Hand a Kotlin/Android task to the Gini Android agent team — the gini-orchestrator routes it to the right specialists to review or implement per the repo standards.
argument-hint: <what you want done or reviewed>
---

Use the **gini-orchestrator** agent to coordinate the following task. Invoke it via the Task tool with `subagent_type: "gini-orchestrator"` and pass the task through.

Task:
$ARGUMENTS

gini-orchestrator will read the task, select the right specialists (compose, views, a11y, testing), and enforce the repository standards — `AGENTS.md` is the canonical source; the orchestrator's Mandatory Rules restate it with repo specifics. Architecture, DI, coroutines, security, performance, and localization standards apply inline.

When it finishes, synthesize the specialists' findings into a single clear answer for me — grouped by specialist, most important issues first.

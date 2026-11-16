---
description: Hand a Kotlin/Android task to the Gini Android agent team — the gini-orchestrator routes it to the right specialists to review or implement per the repo standards.
argument-hint: <what you want done or reviewed>
---

Use the **gini-orchestrator** agent to coordinate the following task. Invoke it via the Task tool with `subagent_type: "gini-orchestrator"` and pass the task through.

Task:
$ARGUMENTS

gini-orchestrator will read the task, select the right specialists (compose, views, a11y, testing, security, concurrency, architecture, performance, design-system, debugger for failure reports, and code-reviewer for finished work before a push), and enforce the repository standards — `AGENTS.md` is the canonical source; the orchestrator's Mandatory Rules restate it with repo specifics. Localization standards apply inline.

If the task has a figma.com link, fetch that node with the Figma MCP (`get_variable_defs`, and `get_design_context` or `get_screenshot`) before invoking the orchestrator, and pass the result in the task — the orchestrator and its specialists cannot call the Figma MCP. If the Figma MCP is not available, say so in the task.

When it finishes, synthesize the specialists' findings into a single clear answer for me — grouped by specialist, most important issues first.

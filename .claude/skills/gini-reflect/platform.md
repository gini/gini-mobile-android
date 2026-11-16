# gini-reflect platform conventions — Android (gini-mobile-android)

<!--
  NOT MIRRORED — this file is Android-specific by design. The iOS repo has its
  own platform.md with the same section headings but iOS content. If you add a
  section here that the shared workflow depends on, add the matching section
  to the iOS platform.md too.
-->

## Standing convention documents (learning targets)

- `AGENTS.md` (repo root) — repo-wide rules loaded into every agent session
  (CLAUDE.md includes it). Keep entries terse and imperative; extend the
  existing section that fits (e.g. "Gotchas" for traps, "Build, test, lint"
  for commands) instead of adding new sections.
- `.claude/skills/gini-plan/platform.md`,
  `.claude/skills/gini-build/platform.md`,
  `.claude/skills/gini-fix/platform.md`,
  `.claude/skills/gini-reflect/platform.md` — Android-specific guidance for
  the shared skills. Target the skill whose step the learning improves.
- `.claude/skills/*/SKILL.md` — only for flaws in the workflow itself. The
  files listed in `.github/mirrored-skills.txt` are mirrored byte-identical
  with gini-mobile-ios (CI: shared-skills.check.yml), so a learning routed
  there needs a paired iOS PR. The remaining skills are Android-only and
  can be edited directly.

NOT learning targets:

- `README.md`, `RELEASE.md`, `MAINTENANCE.md` — human-maintained docs;
  surface a suggestion to the user instead of editing.
- `RELEASE-ORDER.md` — auto-generated (`updateReleaseOrderFile`), never
  edit it manually.
- The assistant's private memory — durable learnings belong in the repo,
  where the whole team and CI benefit from them.

## Evidence sources

- The session conversation itself: failed attempts, retries, surprises,
  instructions that were worked around.
- `specs/<ticket>-feature.md` / `specs/<ticket>-bug.md` — open questions
  that stayed open, implementation-plan steps that needed rework.
- `git diff` / `git log` on the branch — what actually changed versus what
  the spec predicted.
- `/gini-check` and `/gini-connected-check` output — which CI gate failed
  and why.
- **A `code-reviewer` self-review of the session's work** — see below. Its
  findings are the sharpest evidence source available at step 1, because
  they show where the code drifted from the standing docs while the author
  believed it did not.

## Self code review — run at step 1

Before distilling learnings, hand the session's work to the **`code-reviewer`**
agent (Task tool, `subagent_type: "code-reviewer"`), passing the ticket id.
It reads the branch commits and the uncommitted working tree, applies the
`/gini-review` rulebook (`AGENTS.md`, `.claude/rules/coroutines-flow.md`,
`.claude/skills/gini-review/platform.md`,
`.claude/skills/gini-review/references/general-rules.md`), and returns
triaged findings.

It is safe to run here: `code-reviewer` has no Edit or Write tool, and its
instructions limit Bash to read-only `git`/`gh`, so it does not change
production code — the reflect workflow's rule holds.

**Read its findings as evidence about the docs, not just about the code:**

- A finding the author could not have avoided from reading the standing docs
  is a **documentation gap** — that is a learning, and step 3 routes it.
- The *same* finding appearing repeatedly across a diff points at a
  convention that is missing, ambiguous, or stated somewhere nobody reads.
- A finding that quotes a standing doc correctly is just a defect in this
  change. Report it to the user — it gets fixed in `/gini-build` or
  `/gini-fix`, not here — and it is **not** a learning.
- **No findings is a normal outcome** and means nothing needs distilling
  from this source.

Discard anything that is a one-off slip. A learning has to be something the
next session would hit too.

This is deliberately a *different* use of the same agent than in
`gini-build/platform.md` and `gini-fix/platform.md`, where it runs as the
pre-push review on finished code. Here it runs after the work is done, to
question the **documentation** rather than to block a push. Running it in
both places is expected, and a clean result in either never means a
verification gate passed — only `/gini-check` (plus `apiCheck`) says that.

## Commit conventions

Learnings are AI-tooling changes (see `.git-stuff/commit-msg-template.txt`):

```
ai: <subject>

<body: the learning and the session evidence behind it>

<ticket-id of the session's ticket, if any>
```

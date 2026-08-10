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

## Commit conventions

Learnings are AI-tooling changes (see `.git-stuff/commit-msg-template.txt`):

```
ai: <subject>

<body: the learning and the session evidence behind it>

<ticket-id of the session's ticket, if any>
```

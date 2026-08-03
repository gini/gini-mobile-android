---
name: gini-review
description: Run a complete PR review so a human reviewer can spend minutes instead of an hour — resolves the PR from the current branch, a PR number, or a Jira ticket key (PP-/HEAL-/XPL-/FEAT-), reviews every changed file against the ticket's acceptance criteria and this repo's Android rules, reports a coverage ledger plus triaged findings, then asks whether to post them as PR comments. Use when asked to "review this PR", "review PP-1234", or to pre-review your own branch before pushing. Never casts an approve / request-changes verdict.
---

# /gini-review — Android PR review

This skill is the **Android platform layer** over a platform-neutral review engine. It adds this
repo's rules; it does not restate the procedure.

## How to use

```
/gini-review              # the PR for the current branch
/gini-review 1234         # by PR number
/gini-review PP-1234      # by ticket key — which is also the branch-name prefix
```

## Run the review

**Read `pr-review/SKILL.md` in this skill directory and follow it end to end.** That is the procedure:
resolve the PR (§1) → gather the diff, existing review activity and the Jira ticket (§2) → read every
changed file and verify the logic against the ticket (§3) → filter findings (§4) → print the report
(§5) → ask before posting (§6). It also holds the review dimensions, the confidence filter, the report
template, the posted-comment budgets, and the hard rules: **never cast an approve or request-changes
verdict, never modify a file, never run builds or tests.**

Its own references resolve against `pr-review/` — `pr-review/references/ticket-context.md` at §2 and
`pr-review/references/comment-style.md` at §5.

## The Android layer — this is what §0 asks for

**`references/android-checklist.md`** — read at **§3**, on every review.

- Supports: source-level visibility · dependency and build-file rules · release mechanics · downstream
  module ripple · architecture and style · test conventions · commit format · **suppressing noise**
  (the do-not-flag list)
- Does not cover: binary compatibility or `.api` dumps

**`references/android-api-surface.md`** — read at **§3**, only when the diff touches a releasable
module.

- Supports: Kotlin's public-by-default trap · identifying binary-breaking changes · judging new public
  API · deprecation cycles · reading an `.api` dump diff when the branch has one
- Opens with a check for whether `apiCheck` and committed dumps guard **this** branch, because that
  decides whether CI catches an accidental leak or review is the only gate. Run it; do not assume.
- Skip for: example apps, tests, CI, docs

**Repo instructions** — `AGENTS.md` (root `CLAUDE.md` just includes it), plus any `CLAUDE.md` in the
directories the diff touches. These are canonical: a finding cites them by line, or it is a bare
preference and gets dropped.

Sharpening the three platform-bound dimensions in the engine's table for this repo:

| Dimension | On Android here |
|---|---|
| **Concurrency** | Coroutine scope and cancellation, `StateFlow` / `SharedFlow` races, work on the main thread |
| **Lifecycle** | Leaked `Context`, listeners and observers, work surviving `onDestroy` / `onCleared`, state lost across a configuration change |
| **Documentation** | KDoc, since integrators read it through Dokka, plus the Sphinx integration guides |

CI runs `testDebugUnitTest`, `lint`, `detekt` and `ktlintCheck` per module — anything those catch is
noise, so do not report it and do not run them yourself.

## Sharing this with another platform team

`pr-review/` is self-contained and platform-neutral by design. To stand the same review up on another
repo — iOS, web, backend — copy that one folder to `<repo>/.claude/skills/pr-review/`, where it becomes
invocable as `/pr-review`. Nested here it is a reference bundle rather than its own command, because
Claude Code only discovers `SKILL.md` one level under `.claude/skills/`.

It will then run with no platform layer and say so in the report. To add one, point the team at
`pr-review/references/platform-rules.md`: it lists the nine sections a layer must supply and how to
shape the entry-point skill. This file is the worked example of one.

Nothing Android-specific belongs inside `pr-review/`. If a rule names Gradle, Kotlin, a module path or
an Android API, it goes in `references/android-*.md` instead.

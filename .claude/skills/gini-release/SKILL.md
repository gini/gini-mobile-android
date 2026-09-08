---
name: gini-release
description: Guide a module release per RELEASE.md, in two phases. Phase 1 (`rc`) is Jira only — create the RC ticket for an existing release branch, attach the QA build from that branch, make sure the release's Jira version exists, create the open `x.x` placeholder versions, put the ticket in the active sprint. Phase 2 (`release`) is git/Gradle — cut the RC branch, bump versions in dependency order, open the bump PR, then gate the tag push behind RC approval. Use when asked to "create the RC", "prepare the RC ticket", "get the release ready for QA", "release <module>", "bump versions for a release", or "cut the release".
---

# /gini-release — prepare and execute a module release

A release runs in **two phases**, days apart. This skill holds both; pick one and read only that file.

| Phase | File | What it does | Invoke |
|---|---|---|---|
| 1 | [`phase-1-rc.md`](phase-1-rc.md) | **Jira only.** RC ticket for the release branch, QA build attached, Jira release version + `x.x` placeholders, active sprint. Gets the release ready for testing. | `/gini-release rc` |
| 2 | [`phase-2-bump-and-tag.md`](phase-2-bump-and-tag.md) | **Git/Gradle.** RC branch off the release branch, version bumps in dependency order, bump PR, then tags after approval. | `/gini-release`, or `/gini-release bump` |

## Pick the phase

Read the argument, then **read that phase's file in full and follow it**. Do not work from this table alone — the real instructions, and every hard-won gotcha, live in the phase files.

- Argument contains `rc`, `ticket`, `prepare`, `qa`, `phase 1`, `phase1` → **phase 1**, read `phase-1-rc.md`.
- Argument contains `bump`, `tag`, `release`, `cut`, `phase 2`, `phase2`, or is a module list / version numbers → **phase 2**, read `phase-2-bump-and-tag.md`.
- **No argument, or ambiguous:** infer it from what the user asked for in their message — "create the RC" / "get it ready for QA" is phase 1; "release bank-sdk" / "bump the versions" is phase 2. If that is still unclear, ask which phase; never guess, because phase 2 writes commits.

If the user asks for the whole release in one go, explain that it cannot be one run: phase 2 needs the RC ticket from phase 1, and the tag gate needs a human approval in between. Run phase 1 now, and phase 2 when QA and review are done.

## Rules that hold for both phases

- `RELEASE.md` is the source of truth — read it if anything in the phase files seems out of date. `RELEASE-ORDER.md` is auto-generated (`updateReleaseOrderFile`) and must **never** be edited by hand.
- Several steps are irreversible. **Never push a release tag without explicit user confirmation in this session** — pushed tags trigger the release workflows.
- **The release branch already exists** (e.g. `release/bank-sdk-4.5`). Neither phase creates it.
- Jira lives at `ginis.atlassian.net`. The Atlassian connector can read and write a ticket's `fixVersions`, but it has **no API for release versions** — reading and creating those is a browser step.
- The same two-phase split is used on iOS; only phase 2 is platform-specific. Keep the Android-only parts of phase 1 (the `gradle.properties` / `RELEASE-ORDER.md` checks) clearly marked so phase 1 can be lifted for iOS later.

---
name: gini-release
description: Guide a module release per RELEASE.md and the Mobile Release Process, in two phases. Phase 1 (`rc`) is Jira only — confirm the release branch, get the Firebase QA build from it, verify the Jira Releases and their fix versions, create the open `x.x` placeholder versions, create the RC ticket with the build links + QR code, put it in the active sprint and hand it to QA. Phase 2 (`release`) starts after QA has signed off — bump versions on the release branch in dependency order, let the developer check and push, get the release PR (with the bumps) reviewed by another developer, then create the tags with the fastlane command and finish the release (Sonatype, GitHub, Jira, merge, Slack). Use when asked to "create the RC", "prepare the RC ticket", "get the release ready for QA", "release <module>", "bump versions for a release", or "cut the release".
---

# /gini-release — prepare and execute a module release

A release runs in **two phases**, days apart. This skill holds both; pick one and read only that file.

| Phase | File | What it does | Invoke |
|---|---|---|---|
| 1 | [`phase-1-rc.md`](phase-1-rc.md) | **Jira only.** Confirm the release branch, QA build from it, verify Jira Releases + `x.x` placeholders, RC ticket with build links + QR code, sprint, hand to QA. Gets the release ready for testing. | `/gini-release rc` |
| 2 | [`phase-2-bump-and-tag.md`](phase-2-bump-and-tag.md) | **After QA.** Version bumps on the release branch in dependency order, developer checks + push, release PR reviewed by another developer, tags (terminal command), Sonatype, draft → reviewed GitHub releases, Jira, merge back, `#mobile-releases` post. | `/gini-release`, or `/gini-release bump` |

## Pick the phase

Read the argument, then **read that phase's file in full and follow it**. Do not work from this table alone — the real instructions, and every hard-won gotcha, live in the phase files.

- Argument contains `rc`, `ticket`, `prepare`, `qa`, `phase 1`, `phase1` → **phase 1**, read `phase-1-rc.md`.
- Argument contains `bump`, `tag`, `release`, `cut`, `phase 2`, `phase2`, or is an existing RC ticket key (`PP-1234`, `HEAL-99`) → **phase 2**, read `phase-2-bump-and-tag.md`.
- Argument is only a module list / version numbers → **phase 1** — that list is phase 1's input; phase 2 reads it back from the RC ticket. If the user clearly wants the bumps and already has an RC ticket, ask for the ticket and go to phase 2.
- **No argument, or ambiguous:** infer it from what the user asked for in their message — "create the RC" / "get it ready for QA" is phase 1; "release bank-sdk" / "bump the versions" is phase 2. If that is still unclear, ask which phase; never guess, because phase 2 writes commits.

If the user asks for the whole release in one go, explain that it cannot be one run: QA tests between the two phases, and phase 2 needs the RC ticket from phase 1. Run phase 1 now, and phase 2 after QA signs off. Phase 2 itself also stops once, while another developer reviews the bump PR.

## Rules that hold for both phases

- `RELEASE.md` is the source of truth — read it if anything in the phase files seems out of date. `RELEASE-ORDER.md` is auto-generated (`updateReleaseOrderFile`) and must **never** be edited by hand.
- Several steps are irreversible. **Never push a release tag without explicit user confirmation in this session** — pushed tags trigger the release workflows.
- **The release branch already exists** (e.g. `release/bank-sdk-4.7.0`). Neither phase creates it. **Always release from that branch, never from `main`.**
- Jira lives at `ginis.atlassian.net`. The Atlassian connector can read and write a ticket's `fixVersions`, but it has **no API for release versions** — reading and creating those is a browser step.
- The same two-phase split is used on iOS ([gini-mobile-ios `.claude/skills/gini-release`](https://github.com/gini/gini-mobile-ios/tree/main/.claude/skills/gini-release)). **Phase 1 mirrors the iOS phase 1 step for step** — only the modules, the version source (`gradle.properties` vs `*Version.swift`) and the QA build (Firebase example app vs XCFrameworks) differ; keep the two in step when either changes. **Phase 2 follows `RELEASE.md`**, not the iOS phase 2 (Gradle + Sonatype here; XCFrameworks + podspec on iOS).

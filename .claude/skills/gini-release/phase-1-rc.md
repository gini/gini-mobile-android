# Phase 1 — prepare the release candidate (`/gini-release rc`)

A release runs in two phases:

| Phase | File | What it does |
|---|---|---|
| 1 | **this file** | Jira only: release-branch QA build, Jira Releases, `x.x` placeholders, RC ticket, sprint. Gets the release ready for QA testing. |
| 2 | [`phase-2-bump-and-tag.md`](phase-2-bump-and-tag.md) | After QA: version bumps on the release branch, developer check + push, release PR reviewed by another developer, tags, Sonatype, GitHub releases, Jira, merge back, Slack. |

They are days apart — the RC exists so QA can start; phase 2 (bumps, tags, publishing) only starts after QA has signed off. This file mirrors the iOS `phase-1-rc.md` in [gini-mobile-ios](https://github.com/gini/gini-mobile-ios/tree/main/.claude/skills/gini-release) step for step; only the modules, the version source and the QA build differ. When you change a step here, check whether the iOS file needs the same change.

This phase is **Jira and CI-dispatch only**. Reading git (fetching, listing branches, `git show` on the release branch) is fine; it creates no branch, edits no `gradle.properties`, makes no commit and pushes no tag. If you find yourself running `git commit` or `create_release_tags` here, you are in the wrong phase.

**Precondition:** the release branch already exists (e.g. `release/bank-sdk-4.7.0`). Neither phase creates it. Feature PRs for the release merge into that branch; QA tests a build made from it.

## 1. Ask for the modules and their new versions

Ask for the modules being released with their **new versions**, one per line:

```
core-api-library:library 3.4.1
bank-api-library:library 4.3.1
capture-sdk:sdk 4.3.1
capture-sdk:default-network 4.3.1
bank-sdk:sdk 4.3.1
```

That list is the single input — do not walk the user through chain or bump-size questions. From it, derive:

- **Which side(s)**: bank (`bank-api-library`, `capture-sdk`, `bank-sdk`) or health (`health-api-library`, `internal-payment-sdk`, `health-sdk`). Decides the Jira project(s) in step 7. `core-api-library` sits in **both** chains — infer the side from the other entries, and only ask when `core-api-library` is the sole input.
- **Release order** from `RELEASE-ORDER.md` (auto-generated — **never edit it manually**):
  - bank: `core-api-library` → `bank-api-library` → `capture-sdk` → `bank-sdk`
  - health: `core-api-library` → `health-api-library` → `internal-payment-sdk` → `health-sdk`

  Phase 1 bumps nothing, but the order is needed for the "Modules released" table on the ticket.

Each module's version is the `version=` line in `<project>/<module>/gradle.properties`. Read the current values **from the release branch** (step 2), not from whatever branch is checked out locally — a local branch can be several releases behind:

```bash
git fetch origin
git grep '^version=' origin/<release-branch> -- '*gradle.properties' ':!*/example-app/*'
```

Sanity-check and flag — don't silently fix:

- A released module forces every module below it in its chain to be released too — if a downstream module is missing from the list, point it out.
- `capture-sdk:default-network` is always bumped together with `capture-sdk:sdk`, to the **same version** (if omitted, add it and tell the user). It gets no release tag of its own — the `create_release_tags` fastlane lane deliberately ignores it; it ships through capture-sdk's workflow.
- A new version that isn't a semver increment of the current one. A minor bump where a patch would do (or the other way round) is worth one line — new public API means minor.
- Anything that isn't a releasable module (the example apps have a `version=` too, but are never released).

Show a summary table (module, old → new) and get explicit confirmation before continuing.

## 2. Confirm the release branch

**Always release from the release branch, never from `main`.** Feature PRs for the release merge into it; the name usually carries the main SDK and version (`release/bank-sdk-4.7.0`, `release/bank-sdk-4.6`).

Ask which release branch this release ships from — **do not assume**. Confirm it exists on the remote:

```bash
git fetch origin
git branch -r --list 'origin/release/*'
```

Confirm the branch is up to date with `main` (or the version branch it comes from — see `RELEASE.md` for the parallel major-version branches):

```bash
git rev-list --left-right --count origin/main...origin/<release-branch>   # left = commits on main missing from the release branch
```

If it is behind, ask the user how they want to reconcile before proceeding — do not merge it yourself.

## 3. Get the QA build from the release branch

**Do this before the RC ticket** — the build is what the ticket exists to hand over, and a red build here means QA has nothing to test.

The example-app QA builds are published to Firebase App Distribution by these workflows. They run on every PR that touches the chain, and can be dispatched manually:

- `Publish Bank SDK Example App QA Build to Firebase distribution` (`bank-sdk.publish.firebase.example.yml`) — if the release includes the bank chain.
- `Publish Health SDK Example App QA Build to Firebase App Distribution` (`health-sdk.publish.firebase.example.yml`) — if the release includes the health chain.

First look for a green run on the release branch's latest commit:

```bash
gh run list --workflow bank-sdk.publish.firebase.example.yml --branch <release-branch> --limit 5 \
  --json headSha,event,conclusion,url,createdAt
git rev-parse origin/<release-branch>
```

If there is none for the current head, dispatch one (ask first — it publishes a build to testers):

```bash
gh workflow run bank-sdk.publish.firebase.example.yml --ref <release-branch>
gh workflow run health-sdk.publish.firebase.example.yml --ref <release-branch>
```

Wait until the applicable runs finish green before continuing. Do not proceed to the RC ticket on a red build — file bugs against the release branch and get them fixed first. A green build from another branch is not a substitute, unless it carries the same code and the user agrees.

You need two links from the build: the **Firebase console** release link and the **App Tester** download link. Read them from the run log (`firebase appdistribution:distribute` prints both) or ask the user.

## 4. Which modules own a Jira Release

This matters for step 5 (fix versions) and step 6 (placeholders). Only these modules have a Jira release of their own:

| Jira project | Modules with their own Jira release |
|---|---|
| **PP** (bank side) | Bank API Library, Capture SDK, Bank SDK |
| **HEAL** (health side) | Health API Library, Health SDK |

`core-api-library` (notes go into Bank / Health API Library), `internal-payment-sdk` (released only so the Health SDK works) and `capture-sdk:default-network` (notes via Capture SDK) still get released in phase 2, but **no Jira release** and no placeholder.

Don't take this table on faith if the release looks unusual: the project's Releases page (step 5) is the source of truth for which products have versions.

## 5. Verify the Jira Releases (fix versions) exist

Atlassian tenant: `ginis.atlassian.net`. Bank releases live in project **PP**, Health releases in project **HEAL**. A release spanning both sides needs entries in **both** projects. Fix versions are project-scoped — a ticket in one project cannot carry the other's version.

Naming convention: **`Android Gini <Product> <version>`** (e.g. `Android Gini Bank SDK 4.7.0`), sometimes with a theme appended (`Android Gini Bank SDK 4.5.0 - Credit Note`). The `Android` prefix is what stops the two platforms' releases colliding — keep it.

There is one Jira release per **released product that owns notes** (step 4) — for a bank release that's typically `Android Gini Bank SDK`, `Android Gini Capture SDK`, `Android Gini Bank API Library`, minus any product not in this release.

**These are normally created at release planning, not here.** By this step every product's release version should already exist and the release's tickets should already carry the `fixVersion`. This step is a **safety check**: it confirms the versions exist and their tickets are tagged, creates a version only as a fallback when one is missing, and **never assigns `fixVersion` on work tickets in bulk**.

**Verify the versions exist.** Open the project's Releases page:

```
https://ginis.atlassian.net/projects/<KEY>?selectedItem=com.atlassian.jira.jira-projects-plugin:release-page&status=all
```

`status=all` matters — the default filter hides released versions. Match on **name**; permanent `UNRELEASED` placeholders are kept for parking tickets (step 6), so never take the last row.

**Verify each release already has tickets tagged.** For every release version, run:

```
project = <KEY> AND fixVersion = "<version-name>"
```

An empty result or a clearly short list means someone forgot to set `fixVersion` on the release's tickets during the sprint. **Flag this back to the user** with the version name and the count — deciding which tickets belong in the release is a planning decision, not a release-time one. Without fix versions the release report (linked from the RC ticket in step 7, and the source for the GitHub release notes in phase 2) comes back empty. Note that JQL against a version name that doesn't exist returns an empty result rather than an error, so an empty result can also mean a misspelt name.

**Only create a version if it is missing.** The Atlassian connector has no version API — only `fixVersions` on an issue — and Jira rejects an unknown `fixVersions` name (`Version name '…' is not valid`) instead of auto-creating it, so a missing version must be created **in the browser**. In the `Create release` dialog: fill Release name and Description, and **clear the prefilled Release date** (it defaults to today, wrong for an unshipped version — click the field, `cmd+a`, `Backspace`, then click the dialog heading to dismiss the picker; setting an empty string via `form_input` does not work). After saving, **reload the page** — the table doesn't refresh, so success looks like failure. Don't click `Create release` twice either; the second click closes the dialog. Read each version's numeric id off its table link (`/projects/<KEY>/versions/<id>/tab/…`).

Confirm each release has **release notes in markdown** in its description — they are reused on the GitHub releases in phase 2. Only check here: if a release has no notes yet, tell the user (phase 2 adds them before the GitHub releases at the latest; `/gini-release-notes` drafts them from the fix version).

## 6. Create the `x.x` placeholder versions for the next release

Once this release's version is claimed by the RC, tickets filed afterwards have nowhere to land. So every product that owns a Jira release also keeps one **open placeholder version** for the whole major line:

```
Android Gini <Product> <major>.x.x
```

e.g. `Android Gini Bank SDK 4.x.x`, `Android Gini Capture SDK 4.x.x`, `Android Gini Bank API Library 4.x.x` in PP; `Android Gini Health API Library 6.x.x`, `Android Gini Health SDK 6.x.x` in HEAL. iOS keeps the same set with the `iOS` prefix.

Rules:

- One per product listed in step 4 for the side(s) being released — **not** for `core-api-library`, `internal-payment-sdk` or `capture-sdk:default-network`.
- **Create only if missing.** The placeholder lives for the whole major line and is *not* recreated every release. If `Android Gini Bank SDK 4.x.x` already exists, leave it alone.
- `UNRELEASED`, no release date, no description needed. Created in the browser, same dialog and same gotchas as step 5.
- When a new major line starts (e.g. the first 5.0.0 release), the new placeholder is `5.x.x`; the old `4.x.x` stays until its remaining tickets are moved.
- **Do not touch** the existing bug-parking versions — they are not per-product next-version placeholders and must not be renamed, reused or created by this skill:
  - **`Android Unknown Fix Version`** (and its sibling `iOS Unknown Fix Version`) — the platform-wide catch-all for bugs not yet slotted into a release.
  - **`BAC - placeholder bugs`** and **`CVIE placeholder bugs`** — per-customer bug-parking versions.

Report which placeholders already existed and which you created.

## 7. Create the RC ticket

Create it once the QA build (step 3) is green — one ticket per side.

**Start by reading the two most recent Android RC tickets in the target project** and match their shape:

```
project = <KEY> AND issuetype = "Release Candidate" AND summary ~ "Android" ORDER BY created DESC
```

Fetch both with `description`, `labels` and `fixVersions`. Where those tickets disagree with the guidance here, the tickets win. Health RC tickets are rarer and sometimes terser — if the latest HEAL one is thin, follow the fuller PP shape. Health releases used to live in a since-removed `IPC` project, which is why older bump commits carry `IPC-` ids.

- **Issue type:** `Release Candidate`. Resolve it by name, never by id — ids don't transfer across projects.
- **Title:** `[Android] RC for Android Gini Bank SDK <version>` / `[Android] RC for Android Gini Health SDK <version>`, with ` - <theme>` appended when the release has one (`[Android] RC for Android Gini Bank SDK 4.7.0 - Runtime colors`). The version is the main SDK's new version from step 1.
- **Labels:** PP tickets carry `Android` and `mobile`; HEAL is often unlabelled. Match the most recent ticket.
- **Description** — built from **the tickets that carry this release's fix versions**, not from your own reading of the diffs:
  1. `**Issue Summary**` — "Here is the list of tickets for the release.", then the tickets as `<summary> https://ginis.atlassian.net/browse/<TICKET>` links, found with `fixVersion in ("<version-name>", …)`. Split into **Product changes** and **QA automation and tooling** when both kinds exist.
  2. `**Modules released**` — a table of module / old version / new version, in release order, plus one line on anything unusual (why `default-network` has no tag, why `core-api-library` is or isn't released, minor vs patch).
  3. Release branch (and its PR, if one is open).
  4. `**Scope of testing**` — **ask the user; do not invent it.** They decide smoke-test breadth and which OS versions and devices QA covers. You may add the areas most likely to be affected, derived from the diffs, calling out anything with no ticket of its own.
  5. `**Listed Releases**` — "You can find all the tickets related to this release here:" plus one release-report link per fix version: `https://ginis.atlassian.net/projects/<KEY>/versions/<id>/tab/release-report-all-issues`.
  6. `**Attachments**` — "Build for testing:" plus the Firebase console release link and the App Tester download link from step 3.

Also attach a **QR code of the App Tester link** as an image; QA scans it from the ticket to install the build without typing the URL. Generate it locally with whatever QR tool is available (`qrencode` from Homebrew, or the Python `qrcode` package) and attach it. If the connector can't upload attachments, attach it in the browser.

Set the same `fixVersions` on the RC ticket as on the work tickets.

If the build is genuinely not ready yet, say so plainly, create the ticket with "Build for testing: _to be added once …_", and tell the user the ticket is not testable until the links and QR code are added — don't quietly leave the section empty, and don't move it to `Waiting for QA` (step 8).

## 8. Put the ticket in the active sprint and hand it to QA

A freshly created ticket has no sprint, so it appears only in the backlog and never on a board — expect to be asked why it "isn't there". The connector has no board or sprint listing tool, so derive it from an existing issue: run `project = <KEY> AND sprint in openSprints()`, fetch one result with `expand: names`, and find the `customfield_*` whose name is `Sprint`. That is the field id to write; its value carries each sprint's numeric `id`, `state` and `boardId`. Take the entry whose `state` is `active` and set it on the RC ticket via `editJiraIssue`. The active sprint may belong to a board other than the project's own board, in which case the ticket legitimately won't show on the project board — point at the `boardId` from the sprint record.

**Drive status via `transitionJiraIssue`** — don't leave it for the user. Resolve transitions per ticket by **name**, case-insensitively, via `getTransitionsForJiraIssue` (the same status is `Waiting for QA` in one project and `WAITING FOR QA` in the other). Never reuse a transition id from another project: ids collide across projects and can silently move a ticket to `Cancelled`. Move the ticket to `In Progress`, then to `Waiting for QA` once the build links and QR code are on it.

Assign the RC ticket to the QA engineer — ask who, don't guess.

## 9. Report

Summarize:

- the RC ticket key(s) created, with links — **they go into every bump commit in phase 2**
- the release branch the RC is for, and whether it is up to date with `main`
- modules released, old → new
- which Jira Releases already existed and which you created as a fallback, with their release-report links
- any release versions whose tickets were missing `fixVersion` — surfaced for the user to fix, not batch-assigned
- any release versions that have no markdown release notes yet
- which `x.x` placeholders already existed and which you created
- the Firebase QA build run(s) (green) and their links
- the sprint, status and assignee the ticket now has
- what's attached to the RC ticket (Firebase console link, App Tester link, QR code) and what's still missing

Then state the next step explicitly: **once QA has tested the build and signed off the RC ticket, run `/gini-release` (phase 2) with this RC ticket** — phase 2 does the version bumps, the bump PR review, the tags and the rest of the release.

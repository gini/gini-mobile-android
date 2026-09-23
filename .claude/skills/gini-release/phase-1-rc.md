# Phase 1 — prepare the release candidate (`/gini-release rc`)

A release runs in two phases:

| Phase | File | What it does |
|---|---|---|
| 1 | **this file** | Jira only: RC ticket, QA build, Jira versions, sprint. Gets the release ready for testing. |
| 2 | [`phase-2-bump-and-tag.md`](phase-2-bump-and-tag.md) | Git/Gradle: RC branch, version bumps, bump PR, then tags after approval. |

They are separate because they happen days apart — the RC is created so QA can start; the bumps and tags only happen once QA and review are done.

This phase is **Jira work only**. It creates no branch, edits no `gradle.properties`, makes no commit and pushes no tag. If you find yourself touching git here, you are in the wrong skill.

**Precondition:** the release branch already exists (e.g. `release/bank-sdk-4.5`). Neither phase creates it. The QA build attached in step 4 is built from that branch.

> The same flow is used on iOS — only the `Android`/`iOS` prefix in the version names and the source of the module versions differ. Keep the platform-specific parts (step 1's `gradle.properties` check) clearly separated so this skill can be lifted for iOS later.

## 1. Ask for the modules and their new versions

Ask the user to provide the modules being released with their **new versions**, one per line:

```
core-api-library:library 3.4.1
bank-api-library:library 4.3.1
capture-sdk:sdk 4.3.1
capture-sdk:default-network 4.3.1
bank-sdk:sdk 4.3.1
```

Do **not** walk the user through chain/bump-size questions — the list is the single input. From it, determine:

- **Which side(s)** this release is: bank (`bank-api-library`, `capture-sdk`, `bank-sdk`), health (`health-api-library`, `internal-payment-sdk`, `health-sdk`), or both. This decides the Jira project(s) in step 3.
- **Release order**, taken from `RELEASE-ORDER.md` (auto-generated — **never edit it manually**): bank chain `core-api-library` → `bank-api-library` → `capture-sdk` → `bank-sdk`; health chain `core-api-library` → `health-api-library` → `internal-payment-sdk` → `health-sdk`. Phase 1 does not bump anything, but the order is needed for the "Modules released" section of the ticket.

Sanity-check the list against the current versions in each module's `gradle.properties` and the dependency rules — flag (don't silently fix) anything off:

- A released module forces every module below it in its chain — if a downstream module is missing from the list, point it out.
- `capture-sdk:default-network` is always bumped together with `capture-sdk:sdk`, to the **same version** (if omitted, add it and tell the user). It gets no release tag of its own — the `create_release_tags` fastlane lane deliberately ignores it; it's released via capture-sdk's workflow.
- A new version that isn't a semver increment of the current one.

Also confirm the **release branch** the RC is for, and that it exists (`git branch -r --list 'origin/release/*'`).

Show a summary table (module, old → new version) and get an explicit confirmation before creating anything in Jira.

## 2. Which modules own a Jira release

This matters for steps 5 and 6. Only these modules have a Jira release of their own:

| Jira project | Modules with their own Jira release |
|---|---|
| **PP** (bank) | Bank API Library, Capture SDK, Bank SDK |
| **HEAL** (health) | Health API Library, Health SDK |

`core-api-library` (notes go into bank-api-library / health-api-library), `internal-payment-sdk` (released only so the Health SDK works) and `capture-sdk:default-network` (notes via capture-sdk) own **no** release notes and **no** Jira release. They are still released in phase 2 — they just get no version and no placeholder here.

Don't take this table on faith if the release looks unusual: the project's Releases page (step 5) is the source of truth for which products have versions.

## 3. Create the RC ticket in Jira

Use the Atlassian connector (`ginis.atlassian.net`). One ticket per side — Bank releases in project **PP**, Health releases in project **HEAL**; choosing "both" creates **two tickets**, one in each project. Health releases used to live in a since-removed `IPC` project, which is why older bump commits carry `IPC-` ticket ids.

- **Issue type:** `Release Candidate`
- **Title:** `[Android] RC for Android Gini Bank SDK <new-version>` / `[Android] RC for Android Gini Health SDK <new-version>` — the version is the main SDK's new version from the user's list in step 1.
- **Description:** built from **the tickets that share this release's fix version**, not from your own reading of the diffs. Read the two most recent Release Candidate tickets in the target project — `project = <KEY> AND issuetype = "Release Candidate" ORDER BY created DESC` — and match their shape; some are much terser than others, so follow the fuller example. The usual sections:

  1. **Tickets in this release** — a link per ticket, `https://ginis.atlassian.net/browse/<TICKET-KEY>` (an issue key such as `PP-123` or `HEAL-456`, not the project key). Find them with `fixVersion in ("<version>", …)` using the same fix versions the RC ticket carries.
  2. **Modules released** — old → new per module, in release order.
  3. **Scope of testing** — frequently included, especially for Health. **Ask the user for it; do not invent it** — they decide smoke-test breadth and which OS versions QA runs. Optionally follow it with the areas most likely to be affected, derived from the diffs (version-gated code paths, transitive dependency uplifts), calling out anything with no ticket of its own.
  4. **Listed Releases** — the Jira **release report** page of each fix version:

     ```
     You can find all the tickets related to this release here:
     https://ginis.atlassian.net/projects/<KEY>/versions/<version-id>/tab/release-report-all-issues
     ```

     The `<version-id>` comes from step 5.
  5. **Attachments / build for testing** — see step 4.

## 4. Attach the build from the release branch

The RC exists so QA can test, so the ticket must carry the app QA installs — the Firebase App Distribution link(s) for the example-app build made from the **release branch**.

Ask the user for these links; they come from the CI/Firebase build and cannot be generated here. If the build isn't ready yet, say so plainly, create the ticket without it, and tell the user the ticket is not testable until the link is added — don't quietly leave the section empty.

## 5. Make sure the release's Jira version exists, and set it as Fix version

Per `RELEASE.md` step 1, tickets get connected to a release via "Fix versions", and the release description later carries the markdown release notes.

Open the project's Releases page at `https://ginis.atlassian.net/projects/<KEY>?selectedItem=com.atlassian.jira.jira-projects-plugin:release-page&status=all` — `status=all` matters, the default filter hides released versions. There should be one release per released module that has release notes of its own (step 2); the one you want is the latest `UNRELEASED` entry matching that product and version. Watch out for the permanent placeholder versions described in step 6, so match on **name**, never by taking the last row.

**The Atlassian connector cannot list or create Jira release versions** — it has no version API at all, only `fixVersions` on an issue. So read the page and create anything missing **in the browser**. Jira also rejects an unknown `fixVersions` name (`Version name '…' is not valid`) instead of auto-creating it, so the version must exist first.

Naming follows `<Platform> Gini <Product> <version>` — e.g. `Android Gini Bank SDK 4.5.0`. Keep the `Android`/`iOS` prefix; it is what stops the two platforms' releases colliding.

In the `Create release` dialog: fill Release name and Description, and **clear the prefilled Release date** — it defaults to today, which is wrong for a version that hasn't shipped. Clear it by clicking the field, `cmd+a`, `Backspace`, then click the dialog heading to dismiss the date picker; setting an empty string via `form_input` does not work. After saving, **reload the page** — the table does not refresh, so a successful create looks like a failure. Don't click `Create release` twice either; the second click closes the dialog. Read each version's numeric id off its table link (`/projects/<KEY>/versions/<id>/tab/...`).

Once the versions exist, set them as `fixVersions` on the RC ticket with `editJiraIssue`; names work at that point. Do the same on the work tickets that belong to this release, otherwise the release report comes back empty. Fix versions are **project-scoped**, so a ticket in another project cannot carry this project's version.

## 6. Create the `x.x` placeholder versions for the next release

Once this release's version is claimed by the RC, tickets filed afterwards have nowhere to land. So every product that owns a Jira release also keeps one **open placeholder version** for the whole major line:

```
<Platform> Gini <Product> <major>.x.x
```

e.g. `Android Gini Bank SDK 4.x.x`, `Android Gini Health API Library 6.x.x`.

iOS already does this (`iOS Gini Bank SDK 4.x.x`, `iOS Gini Capture SDK 4.x.x`, `iOS Gini APILibrary SDK 4.x.x` in PP; `iOS Gini Health API Library 6.x.x` in HEAL). Android has none yet, so the first run of this skill will create them.

Rules:

- One per module listed in step 2 for the side(s) being released — **not** for `core-api-library`, `internal-payment-sdk` or `capture-sdk:default-network`.
- **Create only if missing.** The placeholder lives for the whole major line and is *not* recreated every release. If `Android Gini Bank SDK 4.x.x` already exists, leave it alone.
- `UNRELEASED`, no release date, no description needed. Created in the browser, same dialog and same gotchas as step 5.
- When a new major line starts (e.g. the first 5.0.0 release), the new placeholder is `5.x.x`; the old `4.x.x` stays until its remaining tickets are moved.
- **Do not touch** the existing `BAC - placeholder bugs` / `CVIE placeholder bugs` versions. Those are per-customer bug parking, a different mechanism with a different purpose — they are not per-module next-version placeholders and must not be renamed, reused or created by this skill.

## 7. Put the ticket in the active sprint and start it

A freshly created ticket has no sprint, so it appears only in the backlog and never on a board — expect to be asked why it "isn't there". The connector has no board or sprint listing tool, so derive it from an existing issue: run `project = <KEY> AND sprint in openSprints()`, fetch one result with `expand: names`, and find the `customfield_*` whose name is `Sprint`. That is the field id to write; its value carries each sprint's numeric `id`, `state` and `boardId`. Take the entry whose `state` is `active` and set it on the RC ticket via `editJiraIssue`. Note the active sprint may belong to a board other than the project's own board, in which case the ticket legitimately won't show on the project board — point at the `boardId` from the sprint record. Also note JQL against a value that doesn't exist returns an empty result set rather than an error, so "no results" never proves absence.

**Drive the ticket's status yourself** — don't leave it for the user. Move it to `In Progress`, and to `Waiting for QA` once the build link from step 4 is on the ticket, using `transitionJiraIssue`. **Resolve transitions per ticket, never by hardcoded id:** call `getTransitionsForJiraIssue` on the actual ticket and match on the transition **name**, case-insensitively (the same status is spelled `Waiting for QA` in one project and `WAITING FOR QA` in the other). Ids are per-project and collide across projects — the same number can name a harmless transition in one and `Cancelled` in the other — so copying a list of ids between projects will silently move a ticket to the wrong status.

## 8. Report

Summarize:

- the RC ticket key(s) created, with links — **they go into every bump commit in phase 2**
- the release branch the RC is for
- modules released, old → new
- which Jira versions already existed and which you created, with their release-report links
- which `x.x` placeholders already existed and which you created
- the sprint and status the ticket is now in
- whether the QA build link is attached, or still missing

Then state the next step explicitly: **once QA has the build and the release is ready to be cut, run `/gini-release` (phase 2) with this RC ticket** to do the version bumps.

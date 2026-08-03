---
name: gini-release
description: Guide a module release end-to-end per RELEASE.md — ask the user for the list of modules with their new versions, create the Jira RC ticket(s), bump versions in dependency order with correctly formatted commits, and gate the tag push (tags trigger the release workflows). Use when asked to "release <module>", "bump versions for a release", or "prepare an RC".
---

# /gini-release — prepare and execute a module release

Releases are driven by `RELEASE.md` (read it if anything here seems out of date). This skill automates the local git/Gradle steps, creates the Jira RC ticket(s), and walks the user through the external steps (QA, Sonatype, GitHub). Several steps are irreversible — **never push a release tag without explicit user confirmation in this session**; pushed tags trigger the release workflows.

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

- **Which side(s)** this release is: bank (`bank-api-library`, `capture-sdk`, `bank-sdk`), health (`health-api-library`, `internal-payment-sdk`, `health-sdk`), or both. This decides the Jira project(s) in step 2.
- **Release order**, taken from `RELEASE-ORDER.md` (auto-generated — **never edit it manually**): bank chain `core-api-library` → `bank-api-library` → `capture-sdk` → `bank-sdk`; health chain `core-api-library` → `health-api-library` → `internal-payment-sdk` → `health-sdk`.

Sanity-check the list against the current versions in each module's `gradle.properties` and the dependency rules — flag (don't silently fix) anything off:

- A released module forces every module below it in its chain — if a downstream module is missing from the list, point it out.
- `capture-sdk:default-network` is always bumped together with `capture-sdk:sdk`, to the **same version** (if omitted, add it and tell the user). It gets no release tag of its own — the `create_release_tags` fastlane lane deliberately ignores it; it's released via capture-sdk's workflow.
- A new version that isn't a semver increment of the current one.

Show a summary table (module, old → new version) and get an explicit confirmation before creating anything.

## 2. Create the RC ticket(s) in Jira

Use the Atlassian connector. One ticket per side — Bank releases in project **PP**, Health releases in project **HEAL**; choosing "both" creates **two tickets**, one in each project. Health releases used to live in a since-removed `IPC` project, which is why older bump commits carry `IPC-` ticket ids.

- **Issue type:** `Release Candidate`
- **Title:** `[Android] RC for Android Gini Bank SDK <new-version>` / `[Android] RC for Android Gini Health SDK <new-version>` — the version is the main SDK's new version from the user's list in step 1.
- **Description:** built from **the tickets that share this release's fix version**, not from your own reading of the diffs. Read the two most recent Release Candidate tickets in the target project — `project = <KEY> AND issuetype = "Release Candidate" ORDER BY created DESC` — and match their shape; some are much terser than others, so follow the fuller example. The usual sections:

  1. **Tickets in this release** — a link per ticket, `https://ginis.atlassian.net/browse/<TICKET-KEY>` (an issue key such as `PP-123` or `HEAL-456`, not the project key). Find them with `fixVersion in ("<version>", …)` using the same fix versions the RC ticket carries.
  2. **Modules released** — old → new per module.
  3. **Scope of testing** — frequently included, especially for Health. **Ask the user for it; do not invent it** — they decide smoke-test breadth and which OS versions QA runs. Optionally follow it with the areas most likely to be affected, derived from the diffs (version-gated code paths, transitive dependency uplifts), calling out anything with no ticket of its own.
  4. **Listed Releases** — the Jira **release report** page of each fix version:

     ```
     You can find all the tickets related to this release here:
     https://ginis.atlassian.net/projects/<KEY>/versions/<version-id>/tab/release-report-all-issues
     ```

     Look up the Jira release matching the new SDK version in the project's releases (PP or HEAL) to get its numeric `<version-id>`. If other released modules (e.g. bank-api-library) have their own Jira release, add their report links too.
  5. **Attachments / build for testing** — the Firebase App Distribution links for the example-app build QA installs. Ask the user for these; they come from the CI/Firebase build and cannot be generated here.

**Check the Jira releases exist, and create the missing ones** (per `RELEASE.md` step 1 — tickets get connected to a release via "Fix versions", and the release description later carries the markdown release notes). Open the project's Releases page at `https://ginis.atlassian.net/projects/<KEY>?selectedItem=com.atlassian.jira.jira-projects-plugin:release-page&status=all` — `status=all` matters, the default filter hides released versions. There should be one release per released module that has release notes of its own (see the ownership note at the end of step 4); the one you want is the latest `UNRELEASED` entry matching that product and version. Watch out for permanent placeholder `UNRELEASED` versions kept for parking bug tickets, so match on name rather than taking the last row.

For any that are missing, create them **through that page in the browser** — the Atlassian connector has no tool for creating Jira versions, and Jira rejects an unknown `fixVersions` name (`Version name '…' is not valid`) instead of auto-creating it. Naming follows `<Platform> Gini <Product> <version>`, so keep the `Android`/`iOS` prefix to avoid colliding with the other platform's releases. In the `Create release` dialog: fill Release name and Description, and **clear the prefilled Release date** — it defaults to today, which is wrong for a version that hasn't shipped. Clear it by clicking the field, `cmd+a`, `Backspace`, then click the dialog heading to dismiss the date picker; setting an empty string via `form_input` does not work. After saving, **reload the page** — the table does not refresh, so a successful create looks like a failure. Don't click `Create release` twice either; the second click closes the dialog. Read each version's numeric id off its table link (`/projects/<KEY>/versions/<id>/tab/...`).

Once the versions exist, set them as `fixVersions` on the RC ticket with `editJiraIssue`; names work at that point. Do the same on the work tickets that belong to this release, otherwise the release report comes back empty. Fix versions are **project-scoped**, so a ticket in another project cannot carry this project's version.

**Put the ticket in the active sprint.** A freshly created ticket has no sprint, so it appears only in the backlog and never on a board — expect to be asked why it "isn't there". The connector has no board or sprint listing tool, so derive it from an existing issue: run `project = <KEY> AND sprint in openSprints()`, fetch one result with `expand: names`, and find the `customfield_*` whose name is `Sprint`. That is the field id to write; its value carries each sprint's numeric `id`, `state` and `boardId`. Take the entry whose `state` is `active` and set it on the RC ticket via `editJiraIssue`. Note the active sprint may belong to a board other than the project's own board, in which case the ticket legitimately won't show on the project board — point at the `boardId` from the sprint record. Also note JQL against a value that doesn't exist returns an empty result set rather than an error, so "no results" never proves absence.

**Drive the ticket's status yourself** — don't leave it for the user. Move it to `In Progress` when you start the bumps, and to `Waiting for QA` at the step 5 gate, using `transitionJiraIssue`. **Resolve transitions per ticket, never by hardcoded id:** call `getTransitionsForJiraIssue` on the actual ticket and match on the transition **name**, case-insensitively (the same status is spelled `Waiting for QA` in one project and `WAITING FOR QA` in the other). Ids are per-project and collide across projects — the same number can name a harmless transition in one and `Cancelled` in the other — so copying a list of ids between projects will silently move a ticket to the wrong status.

Report the created ticket key(s) — they go into every bump commit.

## 3. Pick the correct branch

Three major versions (1.x/2.x/3.x lines) are maintained on parallel branches. If the target version's major matches the version on `main`, branch from `main`; otherwise the release must branch from the matching version branch — check the wiki page linked in `RELEASE.md` step 2 and confirm with the user before proceeding.

Create the RC branch (used to release **all** modules of this release): `PP-XXX-RC-bank-SDK-x.x.x` (bank) or `HEAL-XXX-RC-Health-SDK-x.x.x` (health). For "both", use a single branch named after the bank ticket unless the user wants separate branches — confirm.

## 4. Bump versions, one commit per module, in release order

For each module in the confirmed set, in `RELEASE-ORDER.md` order (fewest dependencies first):

1. Edit `version=` in the module's `gradle.properties` (e.g. `bank-sdk/sdk/gradle.properties`). The Sphinx docs take the version from the `PROJECT_VERSION` env var at build time — no doc file edit needed unless the integration guide hardcodes versions (grep it).
2. Run `./gradlew updateReleaseOrderFile` to regenerate `RELEASE-ORDER.md`.
3. Commit `gradle.properties` + `RELEASE-ORDER.md` together:

   ```
   feat(<project>): Bump version to <x.y.z>

   <RC-ticket-id>
   ```

   The `<project>` slug is the top-level folder, except `capture-sdk:default-network` which uses `default-network` (e.g. `feat(default-network): Bump version to 4.3.2`). Use the ticket of the module's side (PP for bank-chain modules, HEAL for health-chain); for `core-api-library` in a "both" release, include both ticket ids.

Then run the `/gini-check` skill for the affected modules before pushing, and push the RC branch (normal push — no tags yet).

Release-notes ownership (matters for step 7): `core-api-library` has none of its own (notes go into bank-api-library / health-api-library), `internal-payment-sdk` has none (released only so the Health SDK works), `capture-sdk:default-network` has none (notes via capture-sdk). All still get released.

## 5. Wait for QA — hard gate

Stop here. Tags may only be created after QA assigns the RC ticket back to the user. Ask the user to confirm QA approval before continuing; do not infer it.

## 6. Create and push release tags

```bash
bundle exec fastlane create_release_tags
```

The lane finds every project whose `gradle.properties` version has no matching `<project>;<version>` tag, creates the tags, and asks interactively before pushing each one. It needs a terminal for those prompts — suggest the user runs it themselves (e.g. `! bundle exec fastlane create_release_tags`). **Each pushed tag immediately triggers that project's release workflow** — only push when the release is truly go. Verify the workflows started under GitHub Actions afterwards.

## 7. Post-tag checklist (external, walk the user through it)

1. **Sonatype / Maven Central** (credentials in 1Password: "Maven Central Sonatype account for net.gini"): after all release builds finish, in Staging Repositories select all → `Close` (pre-release checks), check email for Sonatype Lift vulnerability reports, then select all → `Release`.
2. **GitHub releases**: create one per pushed tag at github.com/gini/gini-mobile-android/releases, using the markdown release notes from the Jira release (samples linked in `RELEASE.md`).
3. **Jira**: make sure each Jira release has its tickets connected via "Fix versions" and markdown notes in the description; publish the releases in PP/HEAL.
4. Move the RC ticket(s) to `Done` and merge the RC branch into `main` (or the version branch it came from).

## 8. Report

At the end (or when stopping at the QA gate), summarize: modules bumped with old → new versions, RC ticket(s) created, commits made, what `/gini-check` said, and which checklist steps remain. State explicitly whether any tags were pushed.

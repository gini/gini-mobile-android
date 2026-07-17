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

Use the Atlassian connector. One ticket per side — Bank releases in project **PP**, Health releases in project **IPC**; choosing "both" creates **two tickets**, one in each project.

- **Issue type:** `Release Candidate`
- **Title:** `[Android] RC for Android Gini Bank SDK <new-version>` / `[Android] RC for Android Gini Health SDK <new-version>` — the version is the main SDK's new version from the user's list in step 1.
- **Description:**

  ```
  You can find all the tickets related to this release here:
  https://ginis.atlassian.net/projects/PP/versions/<version-id>/tab/release-report-all-issues
  ```

  The link is the Jira **release report** page of this release's fix version. Look up the Jira release matching the new SDK version in the project's releases (PP or IPC) to get its numeric `<version-id>`; if the Jira release doesn't exist yet, create it first (per `RELEASE.md` step 1 — tickets get connected via "Fix versions", and the release description later carries the markdown release notes). If other released modules (e.g. bank-api-library) have their own Jira release, add their report links too.

Report the created ticket key(s) — they go into every bump commit.

## 3. Pick the correct branch

Three major versions (1.x/2.x/3.x lines) are maintained on parallel branches. If the target version's major matches the version on `main`, branch from `main`; otherwise the release must branch from the matching version branch — check the wiki page linked in `RELEASE.md` step 2 and confirm with the user before proceeding.

Create the RC branch (used to release **all** modules of this release): `PP-XXX-RC-bank-SDK-x.x.x` (bank) or `IPC-XXX-RC-Health-SDK-x.x.x` (insurance). For "both", use a single branch named after the bank ticket unless the user wants separate branches — confirm.

## 4. Bump versions, one commit per module, in release order

For each module in the confirmed set, in `RELEASE-ORDER.md` order (fewest dependencies first):

1. Edit `version=` in the module's `gradle.properties` (e.g. `bank-sdk/sdk/gradle.properties`). The Sphinx docs take the version from the `PROJECT_VERSION` env var at build time — no doc file edit needed unless the integration guide hardcodes versions (grep it).
2. Run `./gradlew updateReleaseOrderFile` to regenerate `RELEASE-ORDER.md`.
3. Commit `gradle.properties` + `RELEASE-ORDER.md` together:

   ```
   feat(<project>): Bump version to <x.y.z>

   <RC-ticket-id>
   ```

   The `<project>` slug is the top-level folder, except `capture-sdk:default-network` which uses `default-network` (e.g. `feat(default-network): Bump version to 4.3.2`). Use the ticket of the module's side (PP for bank-chain modules, IPC for health-chain); for `core-api-library` in a "both" release, include both ticket ids.

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
3. **Jira**: make sure each Jira release has its tickets connected via "Fix versions" and markdown notes in the description; publish the releases in PP/IPC.
4. Move the RC ticket(s) to `Done` and merge the RC branch into `main` (or the version branch it came from).

## 8. Report

At the end (or when stopping at the QA gate), summarize: modules bumped with old → new versions, RC ticket(s) created, commits made, what `/gini-check` said, and which checklist steps remain. State explicitly whether any tags were pushed.

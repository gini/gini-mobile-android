# Phase 2 — execute the release (`/gini-release`)

A release runs in two phases:

| Phase | File | What it does |
|---|---|---|
| 1 | [`phase-1-rc.md`](phase-1-rc.md) | Jira only: RC ticket, QA build, Jira versions, sprint. Gets the release ready for testing. |
| 2 | **this file** | Git/Gradle: RC branch, version bumps, bump PR, then tags after approval. |

Releases are driven by `RELEASE.md` (read it if anything here seems out of date). This skill automates the local git/Gradle steps and walks the user through the external steps (Sonatype, GitHub, Jira). Several steps are irreversible — **never push a release tag without explicit user confirmation in this session**; pushed tags trigger the release workflows.

## 0. Take the RC ticket as input

Ask for the RC ticket key from phase 1 (e.g. `PP-1234`, or two keys for a "both" release). From the ticket, read back:

- the modules released and their new versions ("Modules released" section)
- the release branch this RC is for

If there is no RC ticket yet, stop and run `/gini-release rc` (phase 1) first — the bump commits must carry its ticket id, so there is nothing to commit without it.

Re-check the module list against the current versions in each module's `gradle.properties` before touching anything, and flag (don't silently fix) anything that has drifted since the RC was created. `capture-sdk:default-network` is always bumped together with `capture-sdk:sdk`, to the **same version**.

Show the module / old → new table and get an explicit confirmation before the first commit.

## 1. Pick the correct branch

Three major versions (1.x/2.x/3.x lines) are maintained on parallel branches. If the target version's major matches the version on `main`, the release branch comes off `main`; otherwise it must come off the matching version branch — check the wiki page linked in `RELEASE.md` step 2 and confirm with the user before proceeding.

Cut the RC branch off the release branch the RC ticket names (used to bump **all** modules of this release): `PP-XXX-RC-bank-SDK-x.x.x` (bank) or `HEAL-XXX-RC-Health-SDK-x.x.x` (health). For "both", use a single branch named after the bank ticket unless the user wants separate branches — confirm.

## 2. Bump versions, one commit per module, in release order

For each module in the confirmed set, in `RELEASE-ORDER.md` order (fewest dependencies first):

1. Edit `version=` in the module's `gradle.properties` (e.g. `bank-sdk/sdk/gradle.properties`). The Sphinx docs take the version from the `PROJECT_VERSION` env var at build time — no doc file edit needed unless the integration guide hardcodes versions (grep it).
2. Run `./gradlew updateReleaseOrderFile` to regenerate `RELEASE-ORDER.md` (auto-generated — **never edit it manually**).
3. Commit `gradle.properties` + `RELEASE-ORDER.md` together:

   ```
   feat(<project>): Bump version to <x.y.z>

   <RC-ticket-id>
   ```

   The `<project>` slug is the top-level folder, except `capture-sdk:default-network` which uses `default-network` (e.g. `feat(default-network): Bump version to 4.3.2`). Use the ticket of the module's side (PP for bank-chain modules, HEAL for health-chain); for `core-api-library` in a "both" release, include both ticket ids.

Then run the `/gini-check` skill for the affected modules before pushing, and push the RC branch (normal push — **no tags yet**).

Release-notes ownership (matters for step 6): `core-api-library` has none of its own (notes go into bank-api-library / health-api-library), `internal-payment-sdk` has none (released only so the Health SDK works), `capture-sdk:default-network` has none (notes via capture-sdk). All still get released.

## 3. Open the version-bump PR

Open a PR from the RC branch into the release branch (or into the branch it was cut from), following the PR-description rules in `AGENTS.md`.

**The person who did the bump assigns the PR to a colleague** — the bump is not self-reviewed. Ask the user who should review it and assign them; don't guess a reviewer.

## 4. Wait for the RC to be approved — hard gate

Stop here. Tags may only be created once:

1. the version-bump PR has been **approved** by the reviewer, and
2. QA has signed off on the RC ticket (it comes back to the user).

Ask the user to confirm both before continuing; **do not infer either**. Waiting here costs a day; a tag pushed early triggers a real release.

## 5. Create and push release tags

```bash
bundle exec fastlane create_release_tags
```

The lane finds every project whose `gradle.properties` version has no matching `<project>;<version>` tag, creates the tags, and asks interactively before pushing each one. It needs a terminal for those prompts — **suggest the user runs it themselves** (e.g. `! bundle exec fastlane create_release_tags`). **Each pushed tag immediately triggers that project's release workflow** — only push when the release is truly go. Verify the workflows started under GitHub Actions afterwards.

## 6. Post-tag checklist (external, walk the user through it)

1. **Sonatype / Maven Central** (credentials in 1Password: "Maven Central Sonatype account for net.gini"): after all release builds finish, in Staging Repositories select all → `Close` (pre-release checks), check email for Sonatype Lift vulnerability reports, then select all → `Release`.
2. **GitHub releases**: create one per pushed tag at github.com/gini/gini-mobile-android/releases, using the markdown release notes from the Jira release (samples linked in `RELEASE.md`). `/gini-release-notes` drafts these.
3. **Jira**: make sure each Jira release has its tickets connected via "Fix versions" and markdown notes in the description; publish the releases in PP/HEAL. The `<major>.x.x` placeholder versions created in phase 1 stay `UNRELEASED` — never publish those.
4. Move the RC ticket(s) to `Done` and merge the RC branch into the release branch / `main` (or the version branch it came from).

## 7. Report

At the end (or when stopping at the approval gate), summarize: modules bumped with old → new versions, the RC ticket(s) and the bump PR, commits made, what `/gini-check` said, and which checklist steps remain. State explicitly whether any tags were pushed.

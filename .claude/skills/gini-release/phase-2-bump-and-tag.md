# Phase 2 — execute the release (`/gini-release`)

A release runs in two phases:

| Phase | File | What it does |
|---|---|---|
| 1 | [`phase-1-rc.md`](phase-1-rc.md) | Jira only: release-branch QA build, Jira Releases, `x.x` placeholders, RC ticket, sprint. Gets the release ready for QA testing. |
| 2 | **this file** | After QA: version bumps on the release branch, developer check + push, release PR reviewed by another developer, tags, Sonatype, GitHub releases, Jira, merge back, Slack. |

Releases are driven by `RELEASE.md` (read it if anything here seems out of date). This skill automates the local git/Gradle steps and walks the user through the external steps (Sonatype, GitHub, Jira). Several steps are irreversible — **never push a release tag without explicit user confirmation in this session**; pushed tags trigger the release workflows.

## 0. Take the RC ticket as input

**Phase 2 starts after QA is done.** QA tests the build from phase 1 and signs off the RC ticket (`RELEASE.md` step 6: QA assigns the RC ticket back). There is no QA waiting step in this phase — if the user says QA is not done yet, stop and tell them to come back after QA signs off.

Ask for the RC ticket key from phase 1 (e.g. `PP-1234`, or two keys for a "both" release). From the ticket, read back:

- the modules released and their new versions ("Modules released" section)
- the release branch this RC is for

If there is no RC ticket yet, stop and run `/gini-release rc` (phase 1) first — the bump commits must carry its ticket id, so there is nothing to commit without it.

Re-check the module list against the current versions in each module's `gradle.properties` before touching anything, and flag (don't silently fix) anything that has drifted since the RC was created. `capture-sdk:default-network` is always bumped together with `capture-sdk:sdk`, to the **same version**.

Show the module / old → new table and get an explicit confirmation before the first commit.

**Coming back after the review?** If the bump commits are already pushed and the bump PR is open (step 4 ended the last run), skip to step 5.

## 1. Check out the release branch

**Do the bumps on the release branch the RC ticket names — no separate RC branch**, the same as iOS. The release branch is where QA tested; the bump commits go on top of it, and it is the branch the tags are created from in step 5.

```bash
git fetch origin
git checkout <release-branch>
git pull --ff-only
git status            # must be clean
```

Three major versions (1.x/2.x/3.x lines) are maintained on parallel branches. If the target version's major matches the version on `main`, the release branch comes off `main`; otherwise it comes off the matching version branch — check the wiki page linked in `RELEASE.md` step 2 and confirm with the user before proceeding.

If there is no release branch for this release (e.g. a single-fix patch), ask the user which branch to bump on — don't create one yourself.

> `RELEASE.md` step 4 still describes a separate `PP-XXX-RC-…` branch. This skill bumps on the release branch instead, to match iOS — mention it to the user if they ask about the RC branch.

## 2. Bump versions, one commit per module, in release order

**Before the first bump, search for the current version strings.** `RELEASE.md` step 5 says to bump the version in the module's `gradle.properties` *and its documentation file, if relevant*. Find what "if relevant" means for this release by grepping each module's folder for its **current** version, before you change it:

```bash
git grep -nF '<current-x.y.z>' -- <project>/ ':!*/build/*' ':!*.lock'
git grep -nE '<project>(%3B|;)<current-x.y.z>' -- '*.md' '*.rst' '*.kt' '*.java'   # GitHub tag links
```

Look at each hit and decide with the user — not every hit is this module's version (a dependency can share the number). Typical real hits: hardcoded versions in an integration guide (`src/doc/`), README tag links, and tests that assert the library version. Bump the ones that belong to this module in the same commit as its `gradle.properties`; list anything you leave alone in the report.

For each module in the confirmed set, in `RELEASE-ORDER.md` order (fewest dependencies first):

1. Edit `version=` in the module's `gradle.properties` (e.g. `bank-sdk/sdk/gradle.properties`). The Sphinx docs take the version from the `PROJECT_VERSION` env var at build time — no doc file edit needed unless the grep above found a hardcoded version.
2. Run `./gradlew updateReleaseOrderFile` to regenerate `RELEASE-ORDER.md` (auto-generated — **never edit it manually**).
3. Commit `gradle.properties` + `RELEASE-ORDER.md` (+ any doc/test files from the grep above) together:

   ```
   feat(<project>): Bump version to <x.y.z>

   <RC-ticket-id>
   ```

   The `<project>` slug is the top-level folder, except `capture-sdk:default-network` which uses `default-network` (e.g. `feat(default-network): Bump version to 4.3.2`). Use the ticket of the module's side (PP for bank-chain modules, HEAL for health-chain); for `core-api-library` in a "both" release, include both ticket ids.

Then run the `/gini-check` skill for the affected modules. **Do not push yet** — step 3 comes first.

Release-notes ownership (matters for step 6): `core-api-library` has none of its own (notes go into bank-api-library / health-api-library), `internal-payment-sdk` has none (released only so the Health SDK works), `capture-sdk:default-network` has none (notes via capture-sdk). All still get released.

## 3. The developer checks the bump, then you push

Show the developer (the user) what you did, so they can check it before anything leaves their machine:

```bash
git log --oneline origin/<release-branch>..HEAD
git diff --stat origin/<release-branch>..HEAD
```

Plus the `/gini-check` result and any version-grep hits you left alone. Ask them to check the commits, then **ask: "Push the release branch?"** Push only after a clear yes — a normal branch push, **no tags**:

```bash
git push origin <release-branch>
```

If they want changes, make them as new commits (never rewrite commits that are already pushed) and ask again.

## 4. Open the bump PR and ask another developer to review it

The bump PR is the PR **from the release branch into the branch it came from** — usually `main`, or the version branch of an older major line. If that PR already exists (it is often opened when the release branch is created), use it and don't open a second one: `gh pr list --head <release-branch>`. Otherwise open it, following the PR-description rules in `AGENTS.md`.

This PR also carries all the feature work of the release, so make the bump easy to find: add a short **Version bump** section to its description that lists the bump commits (`git log --oneline` from step 3) and the modules old → new.

**Another developer must review the bump** — at least the bump commits — the person who did the bump does not approve their own bump. Ask the user who should review it and request their review; don't guess a reviewer:

```bash
gh pr edit <number> --add-reviewer <github-login>
```

**Then stop and end the run.** Do not wait or poll for the review — it can take a day. Report the PR link, and tell the user to continue with `/gini-release <RC-ticket>` once the PR is approved.

## 5. Create and push release tags (terminal command)

**Before tagging, check the bump PR is approved:** `gh pr view <number> --json reviewDecision,state`. If it is not `APPROVED`, show the user and stop — tags only go on reviewed bump commits.

Tags are created by a terminal command (`RELEASE.md` step 7). Run it **on the approved bump commits** — the lane tags the commit that is checked out (`HEAD`), so check out the release branch and make sure it matches the remote:

```bash
git checkout <release-branch>
git pull --ff-only
git status            # must be clean
bundle exec fastlane create_release_tags
```

The lane asks "Push release tag?" for each project, so it needs a terminal — **ask the user to run it themselves** (e.g. `! bundle exec fastlane create_release_tags`). It finds every project whose `gradle.properties` version has no matching `<project>;<version>` tag and creates that tag.

- **It creates the tag locally *before* it asks "Push release tag?"**, and it only looks at **local** tags (`git tag --list`). So if you answer "no", the local tag stays, and the next run thinks that project is already released and skips it. Before running again, delete the unpushed tag — quote it, because `;` ends a shell command otherwise: `git tag -d "<project>;<version>"`.
- `capture-sdk:default-network` is ignored by the lane on purpose — it ships through capture-sdk's workflow.
- **Each pushed tag immediately triggers that project's release workflow** — only push when the release is truly go.

Then check the workflows started under [GitHub Actions](https://github.com/gini/gini-mobile-android/actions) (`RELEASE.md` step 8), e.g. `gh run list --limit 10`.

## 6. Post-tag checklist (external, walk the user through it)

Do these **in this order** — it is `RELEASE.md` steps 9–12. Maven Central comes first, so the GitHub release never points at an artifact that isn't downloadable yet.

1. **Sonatype / Maven Central** (credentials in 1Password: "Maven Central Sonatype account for net.gini"): after all release builds finish, in Staging Repositories select all → `Close` (pre-release checks), check email for Sonatype Lift vulnerability reports, then select all → `Release`.
2. **GitHub releases — draft first, publish only after review.** First make sure each Jira release has its **release notes in markdown** in its description (`RELEASE.md` step 1.2 — phase 1 only checks them): draft them with `/gini-release-notes`, show them to the user, and add them to the Jira release in the browser after the user agrees. Then create one GitHub release per pushed tag (except `capture-sdk:default-network`, which has no tag), as a **draft**. Use the previous release of the same project as the template for the title (`Android Gini <Product> <version>`) and the section structure, and the markdown notes from the Jira release for the body (samples linked in `RELEASE.md`; `/gini-release-notes` drafts them):

   ```bash
   gh release view "<project>;<previous-version>" --json name,body     # template
   gh release create "<project>;<version>" --draft --title "Android Gini <Product> <version>" --notes-file <notes.md>
   ```

   **Hard gate:** list every draft URL and ask the user to review each one on GitHub. Do not publish until they explicitly confirm all drafts look right; if they want changes, `gh release edit "<project>;<version>" --notes-file <notes.md>` and show them again. Then publish, and mark `bank-sdk` as the latest release when it is part of this release:

   ```bash
   gh release edit "<project>;<version>" --draft=false
   gh release edit "bank-sdk;<version>" --latest
   ```
3. **Jira**: make sure each Jira release has its tickets connected via "Fix versions" and markdown notes in the description; publish the releases in PP/HEAL. The `<major>.x.x` placeholder versions created in phase 1 stay `UNRELEASED` — never publish those.
4. Move the RC ticket(s) to `Done` (`RELEASE.md` step 12; the merge it also asks for is step 7 below).

## 7. Merge back and announce in Slack

1. Merge the approved release PR from step 4 (release branch → `main`, or the version branch it was cut from) — `RELEASE.md` step 12. Use the normal PR flow; don't merge locally.
2. Only after the merge, announce.
3. Announce the release in `#mobile-releases`. The template is shared with iOS (its phase 2 has the same one with `iOS` in the platform slot), so the channel reads the same for both platforms. Draft it and show it to the user — **never post it yourself**.

```
Happy <day of week>! :<light-celebratory-emoji>:
Android <SDK product name> <version> is live

 What's new

 • <Feature 1 name> — <one sentence: what it does + why + opt-in / default-on / default-off>.
<Confluence deep link to that feature's section>
 • <Feature 2 name> — <one sentence>.
<Confluence deep link>
 • <Bug fix or infra note — no link needed if there's no dedicated page>.

 Docs — <top-level Confluence overview URL for this SDK's space>
 Release notes — <GitHub release URL for customer-facing SDK #1> · <GitHub release URL for customer-facing SDK #2>
Firebase App Tester build <App Tester link> to play with it
or directly scan the QR code.
```

How to fill it:

- **Feature bullets:** from the GitHub release notes of the top-level SDK, one rephrased sentence each, not a copy-paste. Always name the opt-in / default state — that is what integrators need most.
- **Confluence deep links:** from the Jira release description or the feature tickets. Skip the link for a bug fix.
- **Release notes:** one GitHub release URL per **customer-facing** SDK that shipped — not `core-api-library`, `internal-payment-sdk` or `capture-sdk:default-network` — separated by ` · `.
- **Build line:** the Firebase App Tester link of the release build (the same build QA tested, from the RC ticket), plus the QR code image from phase 1 attached to the message.
- **Both sides released:** post two separate messages, one per SDK — each has its own build, docs space and release notes.

## 8. Report

At the end (or when stopping after step 4 to wait for the review), summarize: modules bumped with old → new versions, the doc/test files bumped from the version grep and any hits left alone, the RC ticket(s) and the bump PR, commits made, what `/gini-check` said, which GitHub releases are drafted or published, whether the Slack message is drafted, and which checklist steps remain. State explicitly whether any tags were pushed.

---
name: gini-release-notes
description: Draft GitHub release notes for a Gini Android release from its Jira fix version — work out which bumped modules need a note, pull the tickets assigned to each fix version, turn them into integrator-facing bullets, and render the house format (Documentation / Features and improvements / Dependencies / Note). Use when asked to "create/write/draft a release note" or "release notes", with or without a module and version — e.g. "create a release note for this RC", "release notes for this branch", "draft the release notes for <module> <version>" — and as the follow-up to /gini-release. Drafts only; never publishes without explicit confirmation.
---

# /gini-release-notes — draft GitHub release notes from the Jira fix version

Works for both product lines (health and bank). The content of a release note comes from **the Jira fix version**, not from git history — git is only used to find the bumped modules (§1) and as a cross-check (§5).

This skill **only ever produces a draft** — never a published release. Even when the user asks to put it on GitHub, it is created with `--draft` (§8) so a person reviews and publishes it.

## How to run it

Run it from the RC branch, or from any branch where the version bumps are visible.

```
/gini-release-notes                      # detect the bumped modules on this branch and draft a note for each
/gini-release-notes health-sdk 6.1.0     # draft one specific note
/gini-release-notes bank-sdk 4.5.0, capture-sdk 4.5.0, bank-api-library 4.5.0
```

It also triggers on plain requests, which is the normal way to reach it:

- "create a release note for this RC"
- "release notes for this branch"
- "draft the release notes for bank-sdk 4.5.0"
- as the follow-up to `/gini-release`

**"a release note" (singular) does not mean one note.** The number of notes is decided by §1 — how many of the bumped projects publish GitHub releases — never by the phrasing of the request. An RC that bumps an SDK and the API library beneath it produces a note for each, even when asked for "a release note". Likewise "create" here means *create the draft*, not publish (§8).

"this RC" / "this branch" / "this release" all mean: use the version bumps on the current branch, no arguments.

With **no arguments** it runs the whole chain: detect bumps (§1) → confirm the list of notes with the user → resolve each fix version (§2) → draft (§3–§6) → present for review and offer to put them on GitHub (§7). With arguments, §1 is used only to confirm the given modules were really bumped and to find their previous versions.

Nothing leaves the machine until §7 has been reviewed. Presenting the notes always ends by **offering** to create them on the GitHub releases page — the user should never have to know to ask. Only a "yes" runs §8, which needs the tags to exist and still only ever creates drafts.

What it needs: the `gh` CLI authenticated for `gini/gini-mobile-android`, and the Atlassian connector for `ginis.atlassian.net`.

## 1. Determine which modules need a release note

A note is needed for every project that satisfies **both**:

- **(a)** its version was bumped in this RC, and
- **(b)** it is one of the projects that publish GitHub releases.

A bump alone is not enough.

### Find the bumped modules

Versions live in each releasable module's own `gradle.properties`:

```bash
# while the RC branch is still open
git diff main...HEAD -- '*/gradle.properties' | grep -E '^(\+\+\+|[-+]version=)'

# after the release is tagged — compare against the previous tag of the leading project
git diff '<project>;<prev-version>'..HEAD -- '*/gradle.properties' | grep -E '^(\+\+\+|[-+]version=)'
```

The `-version=` line gives the previous version, needed for the git cross-check (§5) and to confirm the fix-version number.

### Decide per bumped project

| Project | GitHub release note? |
|---|---|
| `bank-sdk` | yes |
| `capture-sdk` | yes — also covers `capture-sdk:default-network` |
| `bank-api-library` | yes |
| `health-sdk` | yes |
| `health-api-library` | yes |
| `core-api-library` | **no** — tagged and published to Maven Central, but has never had a GitHub release |
| `internal-payment-sdk` | **no** — same |
| `*/example-app` | **no** — not published |

`core-api-library` and `internal-payment-sdk` are transitive dependencies that integrators never depend on directly, so there is nothing integrator-facing to announce. They also have no Jira fix version — the same fact seen from the other side. Use that as a consistency check: a bumped module with no fix version is **expected** for those two and a **mistake** for any of the five (someone forgot to set the fixVersion in Jira — flag it).

`capture-sdk:default-network` has no tag of its own. It is bumped in lockstep with `capture-sdk:sdk` and is covered by the `capture-sdk` note — which is why its dependency line appears *inside* that note.

A project that was bumped only because a dependency moved, with no change of its own, **still gets a full note** listing the same user-facing changes as the SDK above it.

### Order the work

When several notes are needed, draft them in dependency order, deepest first, because each note links the release of the one below it:

`bank-api-library` → `capture-sdk` → `bank-sdk`, and `health-api-library` → `health-sdk`.

Confirm the resulting list of notes with the user before continuing.

## 2. Resolve the fix version

The Jira fix version name is **character-identical to the GitHub release title**. That is the join key between the two systems:

```
Jira fixVersion      Android Gini <Product> <version>
GitHub release title Android Gini <Product> <version>
GitHub tag           <project>;<version>
```

| Project | Product name | Jira project |
|---|---|---|
| `health-sdk` | `Android Gini Health SDK` | `HEAL` (Health) |
| `health-api-library` | `Android Gini Health API Library` | `HEAL` (Health) |
| `bank-sdk` | `Android Gini Bank SDK` | `PP` (Banking Team) |
| `capture-sdk` | `Android Gini Capture SDK` | `PP` (Banking Team) |
| `bank-api-library` | `Android Gini Bank API Library` | `PP` (Banking Team) |

The Jira site is `ginis.atlassian.net`. The release report for a version lives at
`ginis.atlassian.net/projects/<HEAL|PP>/versions/<id>/tab/release-report-all-issues`.

The `Android ` prefix is load-bearing — iOS releases live in the same two Jira projects with otherwise identical names.

A fix version may carry a theme suffix (`Android Gini <Product> <version> - <theme>`). Match on the version number, then confirm the full string with the user.

## 3. Pull the tickets

One query per fix version:

```
searchJiraIssuesUsingJql
  cloudId: ginis.atlassian.net
  jql:     fixVersion = "<exact fix version name>" ORDER BY issuetype ASC
  fields:  ["summary", "issuetype", "status", "resolution", "fixVersions"]
```

Always pass an explicit `fields` list — the default set includes full descriptions and will overflow the token limit. If a response still spills to a file, extract with jq instead of reading it:

```bash
jq -r '.issues.nodes[] | "\(.key) [\(.fields.issuetype.name)/\(.fields.status.name)] \(.fields.summary)"' <saved-file>
```

An empty result almost always means the name is wrong, not that the release is empty — JQL on an unknown value returns nothing rather than an error. Re-check the exact string.

**One ticket, several fix versions.** A ticket is routinely assigned to two or three fix versions at once (e.g. an SDK and the API library beneath it). Each note describes only the part visible in *that* artifact: the API library note describes the API-level change, the SDK note describes the user-visible behaviour from the same ticket.

## 4. Turn tickets into bullets

### Drop these issue types entirely

`Release Candidate`, `Test Plan`, `Test Execution` — process tickets, never bullets. They are usually the majority of a fix version.

### The RC ticket is the outline, not a bullet

Summary form: `[Android] RC for <fix version name> - <theme>`.

- The **theme suffix** is the headline of the release.
- The **description** lists the tickets in the release and links the release report.

Read it first, and read the linked tickets it names. A bullet that belongs in the notes but matches no ticket in the fix version usually comes from this theme — check with the user rather than dropping it.

### Map the remaining tickets

| Issue type | Becomes |
|---|---|
| `Story`, `Task`, `Improvement` | its own named bullet |
| `Bug` | normally folded into a closing `- Minor bug fixes and improvements`; give a bug its own bullet only when an integrator would notice the behaviour change |

Several tickets covering one feature collapse into a single bullet — group by what the integrator sees, not by ticket count.

### Wording rules

- Third person, no leading pronoun: "Adds …", "Improves …", "Fixes …", "Drops …". Match the tense used in that project's previous release; don't mix "Adds" and "Added" in one list.
- Rewrite from the integrator's point of view. A user story ("As a User, I want …") becomes a statement of what changed in the SDK.
- Never paste a ticket summary verbatim, and **never include ticket keys or Jira links** — release notes are public.
- Put changed public API in backticks.
- Breaking changes first, saying what the integrator has to do.
- Never invent a change. Anything you cannot phrase confidently goes into an `Open questions` block below the draft.

### Tone

These bullets are read by customers, so write them a little more warmly than a changelog line — but keep them short. Each bullet is **what changed, then one clause on what the integrator gets from it**:

```
- <verb> <what changed, with any public API in backticks> — <one clause: what it means for the integrator>
```

The verb takes whichever form the previous release used (§6) — the products differ here, so never carry a tense across from another project's note.

Write the benefit clause only from what the ticket actually establishes. Typical material: a platform or store requirement the change keeps them compliant with, a dependency they no longer carry, a manual step they no longer need, a behaviour that now works where it previously did not. If a ticket supports no such clause, leave the bullet bare — a bullet with no benefit clause is correct and normal, and better than a padded one.

Keep it restrained. One benefit clause per bullet, no second sentence, and never two adjectives where one works. No marketing vocabulary ("seamless", "powerful", "delightful", "revolutionises"), no exclamation marks, no promises about speed, stability, or reliability that the ticket does not support — a vague flourish is worse than the bare line, because an integrator cannot act on it. Every claim must still trace to a ticket; the tone is in the framing, never in added substance.

`- Minor bug fixes and improvements` (see *Map the remaining tickets* above) is a fixed closing line and stays exactly as it is — never dress it up.

## 5. Cross-check against git

Only to catch tickets missing from the fix version — never as a source of bullets:

```bash
git log --no-merges '<project>;<prev-version>'..HEAD --format='%s' -- <project>/
```

If a commit references a ticket that is not in the fix version, flag it: either the fixVersion is missing in Jira, or the change does not belong in this release.

## 6. Render

Release title: the fix version name with any theme suffix removed.

```markdown
## Documentation

You can find the documentation for this version [here](<DOC_URL>).

## Features and improvements

- <bullet>
- <bullet>

## Dependencies

<dependency line — omit this whole section if the project has none>

## Note
To ensure maximum stability, the latest features, and all available bug fixes, we strongly recommend always integrating the latest SDK version.
```

The `## Note` block is verbatim boilerplate, but is not used by every project. Read the project's **previous** release with `gh release view '<project>;<prev-version>' --repo gini/gini-mobile-android --json body -q .body` and match it — that is the authority for the Note block, the heading set, and the tense.

### DOC_URL

| Project | Documentation URL |
|---|---|
| `health-sdk` | `https://developer.gini.net/gini-mobile-android/health-sdk/sdk/<version>/html/` |
| `health-api-library` | `https://developer.gini.net/gini-mobile-android/health-api-library/library/<version>/html/` |
| `bank-api-library` | `https://developer.gini.net/gini-mobile-android/bank-api-library/library/<version>/html/` |
| `bank-sdk` | `https://gini.atlassian.net/wiki/spaces/GBSV/overview` |
| `capture-sdk` | `https://gini.atlassian.net/wiki/spaces/GBSV/overview` |

**Always include the link, built from the version being released.** A `developer.gini.net` link is published by the release workflow and 404s until that finishes, so a dead link while drafting is normal and is not a reason to omit it, change it, or fall back to an older version's URL. Checking the links is the releasing person's job at the end — the draft just has to have them pointing at the right version.

### Dependencies section

Link the dependency's own release, with `;` percent-encoded as `%3B`:
`https://github.com/gini/gini-mobile-android/releases/tag/<project>%3B<version>`

| Project | Dependency line |
|---|---|
| `health-sdk` | `Uses Gini Health API Library [<v>](<link to health-api-library;<v>>)` |
| `bank-sdk` | `Uses Gini Capture SDK [<v>](<link to capture-sdk;<v>>)` |
| `capture-sdk` | `Default networking implementation uses Gini Bank API Library [<v>](<link to bank-api-library;<v>>)` |
| `health-api-library`, `bank-api-library` | omit the section |

Read the linked version from that module's `gradle.properties` at the release commit — do not assume it matches the version being released.

The same rule as for the documentation link applies: the sibling release being linked is often not published yet, so the link will 404 while drafting. Include it anyway, built from the correct new version. Never downgrade a link to an already-published older version just to make it resolve.

## 7. Present the draft

For each note, show the title and the full body as a fenced block, plus:

- which tickets were dropped and why (process tickets, folded bugs),
- any `Open questions`,
- any git/Jira mismatch from §5,
- a reminder that the documentation and dependency links point at the new versions and still need to be checked by the releasing person once the workflows have run.

### Check the tags before offering

A GitHub release attaches to a tag, so find out whether the tags exist *before* asking — otherwise the offer promises something that may be impossible. Per note, local and remote:

```bash
git tag -l '<project>;<version>'
git ls-remote --tags origin 'refs/tags/<project>;<version>'
```

Both are read-only lookups: they only read tag names and create nothing. Running them before the user has answered does not breach §8's rule, which is about creating or editing releases.

### Then make the offer

**End by asking whether to create these as drafts on the GitHub releases page** (`github.com/gini/gini-mobile-android/releases`). Ask every time — even when the user has not mentioned GitHub — so they never have to know that §8 exists or think to ask for it. Make the offer concrete, and fold the tag result into it:

- **All tags present** → name them, and say the drafts will be created with `--draft` and that publishing stays with the user.
- **A tag missing** → say so in the offer itself, since it changes the answer. Do not offer to create that note's draft; hand over its text and say it can be created once the tag is pushed. Offer the others as normal. **Never create or push the tag** — tags trigger the release workflows and belong to `/gini-release`.

A "yes" is the explicit confirmation §8 requires; go straight into it. Anything else — silence, edits, a change of subject — is not, so keep iterating on the wording and ask again once the draft settles.

## 8. Creating the GitHub draft (only on explicit confirmation)

A GitHub release is public and outward-facing, so do not create or edit one until the user has answered the §7 offer with a yes. When they have, the result is **always a draft** — this skill never publishes.

The tags were already checked in §7. Create a draft only for the notes whose tag exists; a note whose tag is missing was never offered, so skip it here and leave its text with the user until they say the tag is in place.

### Create it

```bash
gh release create '<project>;<version>' --repo gini/gini-mobile-android \
  --title '<release title>' --notes-file <draft.md> --draft
```

`--draft` is mandatory — keep it even if the user says "publish it" or "put it on GitHub". Creating the draft is the end of this skill's job; a person reviews the wording, checks that the documentation and dependency links now resolve, and presses publish. Say that explicitly when handing over.

Give the user the draft's URL from the command output so they can go straight to it.

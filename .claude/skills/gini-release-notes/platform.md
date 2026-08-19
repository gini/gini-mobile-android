# gini-release-notes platform conventions — Android (gini-mobile-android)

<!--
  NOT MIRRORED — this file is Android-specific by design. The iOS repo has its
  own platform.md with the same section headings but iOS content. If you add a
  section here that the shared workflow depends on, add the matching section
  to the iOS platform.md too.
-->

## Where versions live

Each releasable module holds its version in its own `gradle.properties` as a `version=` line.

```bash
# while the release branch is still open
git diff main...HEAD -- '*/gradle.properties' | grep -E '^(\+\+\+|[-+]version=)'

# after the release is tagged — compare against the previous tag of the leading project
git diff '<project>;<prev-version>'..HEAD -- '*/gradle.properties' | grep -E '^(\+\+\+|[-+]version=)'
```

The `-version=` line is the previous version.

Example-app modules also carry a `version=` line. They are never released — ignore them.

There is no separate dependency-pinning file on Android: cross-SDK dependencies are Gradle project dependencies, so a dependency's version is read from **that module's own `gradle.properties`** at the release commit.

## Release targets

One release note per **project** (the top-level folder), not per module. `capture-sdk:sdk` and `capture-sdk:default-network` share a single `capture-sdk` note.

| Project | Release note? | Where |
|---|---|---|
| `bank-sdk` | yes | `gini/gini-mobile-android` |
| `capture-sdk` | yes — also covers `capture-sdk:default-network` | `gini/gini-mobile-android` |
| `bank-api-library` | yes | `gini/gini-mobile-android` |
| `health-sdk` | yes | `gini/gini-mobile-android` |
| `health-api-library` | yes | `gini/gini-mobile-android` |
| `core-api-library` | **no** — tagged and published to Maven Central, but has never had a GitHub release | — |
| `internal-payment-sdk` | **no** — same | — |
| `*/example-app` | **no** — not published | — |

Every package produces exactly **one** note, in the monorepo.

`core-api-library` and `internal-payment-sdk` are transitive dependencies that integrators never depend on directly. They also have no Jira fix version.

`capture-sdk:default-network` has no tag of its own. It is bumped in lockstep with `capture-sdk:sdk` and is covered by the `capture-sdk` note — which is why its dependency line appears *inside* that note.

## Dependency order

Draft deepest dependency first:

- bank line: `bank-api-library` → `capture-sdk` → `bank-sdk`
- health line: `health-api-library` → `health-sdk`

`RELEASE-ORDER.md` is the generated source of truth if this drifts.

## Jira fix version mapping

**Release title = the fix-version name with any theme suffix removed.** Most Android fix versions carry no suffix, so in practice the two strings usually match character for character — but do not rely on that: apply the rule, then compare.

```
Jira fixVersion        Android Gini <Product> <version>[ - <theme>]
GitHub release title   Android Gini <Product> <version>
GitHub tag             <project>;<version>
```

| Project | Fix version starts with | Jira project |
|---|---|---|
| `health-sdk` | `Android Gini Health SDK` | `HEAL` (Health) |
| `health-api-library` | `Android Gini Health API Library` | `HEAL` (Health) |
| `bank-sdk` | `Android Gini Bank SDK` | `PP` (Banking Team) |
| `capture-sdk` | `Android Gini Capture SDK` | `PP` (Banking Team) |
| `bank-api-library` | `Android Gini Bank API Library` | `PP` (Banking Team) |

The **`Android ` prefix is the platform marker**. iOS releases live in the same two Jira projects with otherwise identical names.

The release report for a version lives at
`ginis.atlassian.net/projects/<HEAL|PP>/versions/<id>/tab/release-report-all-issues`.

## Source paths

```bash
git log --no-merges '<project>;<prev-version>'..HEAD --format='%s' -- <project>/
```

Source lives under `src/main/java/` even for Kotlin files; the project folder above is the right granularity for this check.

## Bullet conventions

- **Bug folding:** collect the bugs that no integrator would look for individually into a single fixed closing line, exactly:

  ```
  - Minor bug fixes and improvements
  ```

  Never reword or embellish it. A bug gets its own bullet only when the behaviour change is one an integrator would notice.
- **Grouping:** flat list, no `###` subheadings. Android notes are short.
- **Symbols in backticks:** Kotlin/Java public API — classes, functions, properties (e.g. `` `GiniHealthException` ``).

## Note templates

One note per project.

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

The `## Note` block is verbatim boilerplate, but **is not used by every project** — the health notes have omitted it while the bank and capture notes carry it. Read the project's previous release and match it; it is the authority for the Note block, the heading set and the tense:

```bash
gh release view '<project>;<prev-version>' --repo gini/gini-mobile-android --json body -q .body
```

## Documentation URLs

Two kinds of URL. Substitute `<version>` only in the **versioned** ones; the **fixed** ones are used verbatim and never get a version spliced into them.

| Project | Kind | Documentation URL |
|---|---|---|
| `health-sdk` | versioned | `https://developer.gini.net/gini-mobile-android/health-sdk/sdk/<version>/html/` |
| `health-api-library` | versioned | `https://developer.gini.net/gini-mobile-android/health-api-library/library/<version>/html/` |
| `bank-api-library` | versioned | `https://developer.gini.net/gini-mobile-android/bank-api-library/library/<version>/html/` |
| `bank-sdk` | fixed | `https://gini.atlassian.net/wiki/spaces/GBSV/overview` |
| `capture-sdk` | fixed | `https://gini.atlassian.net/wiki/spaces/GBSV/overview` |

Only the **versioned** URLs are affected by the "may 404 while drafting" rule in §6 — a fixed wiki URL resolves immediately, so if one of those is broken it is a genuine problem worth reporting rather than an expected timing gap.

`GBSV` is the Android space. The iOS notes use `IBSV` — do not copy one for the other.

## Dependency links

Link the dependency's own release in this same repo. The `;` in the tag **must be percent-encoded as `%3B`**:

`https://github.com/gini/gini-mobile-android/releases/tag/<project>%3B<version>`

| Project | Dependency line |
|---|---|
| `health-sdk` | `Uses Gini Health API Library [<v>](<link to health-api-library;<v>>)` |
| `bank-sdk` | `Uses Gini Capture SDK [<v>](<link to capture-sdk;<v>>)` |
| `capture-sdk` | `Default networking implementation uses Gini Bank API Library [<v>](<link to bank-api-library;<v>>)` |
| `health-api-library`, `bank-api-library` | omit the section |

Read the linked version from that module's `gradle.properties` at the release commit — do not assume it matches the version being released.

## Tags and draft creation

Tag format: `<project>;<version>`. One tag, one repo.

Read-only check, run in §7 before offering:

```bash
git tag -l '<project>;<version>'
git ls-remote --tags origin 'refs/tags/<project>;<version>'
```

Draft creation, run in §8 only after a yes:

```bash
gh release create '<project>;<version>' --repo gini/gini-mobile-android \
  --title '<release title>' --notes-file <draft.md> --draft
```

Tags are created and pushed by `bundle exec fastlane create_release_tags` as part of the release flow (see `RELEASE.md` and the `/gini-release` skill). **Each pushed tag immediately triggers that project's release workflow**, so this skill never creates or pushes one.

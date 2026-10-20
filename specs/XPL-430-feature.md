# XPL-430: Get the mobile CI bot's commits signed

Status: implemented
Ticket: https://ginis.atlassian.net/browse/XPL-430
Paired iOS PR: https://github.com/gini/gini-mobile-ios/pull/1288

## Problem

Fastlane's `release_documentation` lane runs on CI (via
`.github/workflows/*.docs.release.yml` → `sdk.publish.docs.yml`) and
makes a git commit on the `gh-pages` branch of
`gini/gini-mobile-android` to publish the Dokka + Sphinx output of each
SDK's documentation. Today that commit is **not signed** because the CI
runner has no GPG/SSH signing key, so on github.com it appears with the
yellow "Unverified" badge and the fake author `Team Mobile Schorsch` /
`Team Mobile <team-mobile@gini.net>` (set via
`configure_git_on_ci_machines` + the `--author=…` flag on the commit).

For documentation shipped to integrators at
`developer.gini.net/gini-mobile-android/*`, Unverified bot commits are
a trust / BFSG-adjacent audit flag. The iOS PR solves the same problem
in `gini-mobile-ios` by letting GitHub create the commit through its
REST API (which auto-signs it with the `web-flow` GPG key and
attributes it to the GitHub App bot that owns `GH_TOKEN`). This spec
mirrors that fix on the Android side.

## Requirements

R1 (MUST, entry): Given a Fastlane lane running on CI with `GH_TOKEN`
exported (from `actions/create-github-app-token` →
`MOBILE_CI_APP`) and the `gh` CLI available on PATH, when the lane
calls `push_as_signed_commit(repo, branch, ui)` after a local
`git commit`, then a new commit with the same tree, parent, and
message is created on `refs/heads/<branch>` of `<repo>` through the
GitHub REST API, the commit appears on github.com with the
**Verified** badge, and the helper's return value is the SHA of the
created commit.

R2 (MUST, happy — author identity): Given a successful signed-commit
push, when the user inspects the resulting commit on github.com, then
the author field shows **the GitHub App bot identity associated with
`GH_TOKEN`** (per the paired iOS verification:
`gini-mobile-ci[bot]`), **not** `Team Mobile` /
`Team Mobile Schorsch`.

R3 (MUST, happy — docs publish signed): Given `release_documentation`
runs on CI with real changes staged for `gh-pages` of
`gini/gini-mobile-android`, when the lane reaches the commit/push
step, then (a) a signed commit lands on `gh-pages` via
`push_as_signed_commit`, (b) the author is the bot per R2, and (c) the
lane no longer invokes
`configure_git_on_ci_machines("Team Mobile Schorsch", "team-mobile@gini.net")` —
that identity override is obsolete once GitHub supplies the author.

R4 (SHOULD, happy — no-op guard): Given `release_documentation` runs
with **no** staged diff after the Dokka output is copied in (idempotent
re-run on the same tag), when the lane reaches the commit/push step,
then it detects the no-op condition and returns without creating an
empty commit, matching the iOS artifact's `git diff --cached --quiet`
guard.

R5 (MUST, happy — off-CI legacy auth path preserved): Given the lane
is invoked **without** `GH_TOKEN` (e.g. an off-CI invocation using the
legacy `git_user` / `git_password` options), when the lane reaches the
commit/push step, then it continues using today's unsigned
`git_push_with_retry` path — producing an unsigned commit as it does
today. Only the `GH_TOKEN`-authenticated code path is migrated to the
signed helper; legacy credentials do not grant access to `gh api`'s
signing path and must not break.

R6 (MUST, error — concurrent push): Given two CI jobs call the helper
against the same branch concurrently, when the second job's
`PATCH /refs/heads/<branch>` fails because `<branch>` moved, then the
helper rebases locally (`git pull --rebase origin <branch>`) and
retries, up to `attempts: 3` total; on the final attempt failing, the
Ruby exception from `gh api` is re-raised to Fastlane's
`UI.abort_with_message!` boundary so the lane fails loudly rather than
silently.

R7 (MUST, error — narrow retry): Given the helper's `rescue` catches a
`gh api` failure, when the failure is anything other than a
non-fast-forward response on `PATCH /refs/heads/<branch>` (e.g. 401
auth, 403 permission, 403 secondary rate limit, 422 validation), then
the exception is re-raised immediately without rebase — so the real
error reaches the lane boundary instead of being masked by up to 3
wasted retries.

R8 (MUST, error — cleanup): Given the helper creates a temporary
branch `ci-signing-tmp-<SecureRandom.hex(6)>` on the remote to upload
blobs/tree, when the API-create step succeeds, fails, or raises, then
the temporary branch is deleted via
`DELETE /repos/:owner/:repo/git/refs/heads/<tmp>` in an `ensure`
clause — no orphan `ci-signing-tmp-*` refs accumulate on the remote.
Random suffix (not a timestamp) so two concurrent CI jobs cannot
collide on the same temp branch name.

R9 (MUST, error — auth): Given a lane calling the helper runs with
`GH_TOKEN` set but invalid/revoked, when the first `gh api` call
executes, then it fails with `gh`'s own authentication error, which
propagates to Fastlane's error boundary. The helper does not pre-check
`GH_TOKEN` presence beyond what the calling lane already does (per R5
the lane only enters the signed path when `GH_TOKEN` is present).

R10 (MUST, async): Given a lane that previously used
`git_push_with_retry`, when it is migrated to `push_as_signed_commit`,
then the helper remains synchronous (returns only when the branch is
at the new SHA and the temp branch is deleted). Fastlane's `sh`
continues to be the shell boundary — no new concurrency primitives.

## Affected modules

This change is **not Kotlin SDK code** — it is release/CI tooling under
`fastlane/`. Platform.md's module map (bank-sdk, capture-sdk, etc.)
does not apply. The affected files are:

- `fastlane/util/git.rb` — add `push_as_signed_commit(repo, branch, ui, attempts: 3)`.
- `fastlane/Fastfile` — rewrite `release_documentation` (lines
  ~274-349) per R3 / R4 / R5: branch on `GH_TOKEN` presence; signed
  path for CI, legacy path preserved for off-CI.

## Public API impact

**None at the SDK level.** The helper lives inside `fastlane/util/`,
imported via `load "util/git.rb"` in `Fastfile`. No Kotlin/Java
declaration changes, no Gradle module API changes, no public API
dumps affected.

**Internal lane-signature changes (not integrator-visible):**
- `release_documentation` lane signature is **unchanged** — it still
  accepts the same options (`project_id`, `module_id`, `git_tag`,
  `ci`, `documentation_title`, optional `dry_run`, `git_token`,
  `git_user`, `git_password`). Only the commit/push block inside
  changes.
- `git_push_with_retry` is **kept** in `fastlane/util/git.rb` because
  the lane still calls it on the off-CI legacy path (R5). Same decision
  as iOS, but on iOS it has zero callers after the change; here it
  retains one caller.

## Technical conventions

Platform.md's checklist (Compose, Koin, MVVM+StateFlow, Jacoco) targets
Kotlin SDK work and does not apply to Ruby / Fastlane tooling. The
conventions that *do* apply:

1. **Language / runtime.** Ruby, inside Fastlane's `sh` boundary.
   Match the file's existing style — `##` doc comments above each
   helper (see the existing `configure_git_on_ci_machines`,
   `git_push_with_retry`, `git_create_release_tag` for the pattern).
2. **Shell safety.** Any value interpolated into `sh("…")` that could
   contain spaces or shell metacharacters must be `.shellescape`'d.
   Specifically: the commit message read via `git log -1 --format=%B`
   goes into a `gh api -f message=…` argument and MUST be
   `.shellescape`'d, as the iOS implementation does.
   `require 'shellwords'` + `require 'securerandom'` at the top of
   the helper.
3. **GitHub CLI dependency.** The helper calls
   `gh api repos/.../git/commits`, `gh api -X PATCH refs/heads/<branch>`,
   and `gh api -X DELETE refs/heads/<tmp>`. The CI runners
   (`.github/workflows/*.yml`) must have `gh` on PATH. Android CI uses
   `ubuntu-latest` which ships `gh` preinstalled; no new runner-image
   changes expected.
4. **Error surfacing.** Fatal errors inside the helper re-raise the
   `gh api` exception so they hit Fastlane's `UI.abort_with_message!` /
   lane-level error handling. Non-fatal errors (narrow non-FF retry per
   R7) call `ui.message` with a human-readable summary before retrying.
5. **No new secrets.** `MOBILE_CI_APP_ID` / `MOBILE_CI_APP_PRIVATE_KEY`
   are already in use (see
   `.github/workflows/sdk.publish.docs.yml:74-78` and all
   `*.docs.release.yml` wrappers). The helper reuses `GH_TOKEN` via
   `gh`'s own auth. No new environment variables, no vault reads.
6. **Idempotence.** The helper is retried up to 3 times with
   `git pull --rebase` between attempts (R6), but ONLY for
   non-fast-forward failures (R7). Everything else re-raises
   immediately. Temp branch name
   `ci-signing-tmp-#{SecureRandom.hex(6)}` prevents collisions on
   concurrent runs (R8).
7. **Mirrored implementation.** The helper must be byte-equivalent in
   intent to the iOS version at
   `gini-mobile-ios/fastlane/util/git.rb`. Divergence between the two
   repos' helpers would break the shared-skill assumption and make
   future maintenance harder. If a bug fix lands on one, it must land
   on the other in a paired PR.

## Design

### The helper

Add to `fastlane/util/git.rb`, matching the iOS implementation shipped
in `gini-mobile-ios` PR #1288 commits
`c2a9e4850` (initial helper) + `4e5db2399` (SecureRandom suffix) +
`6f73baa4e` (narrow rescue):

```ruby
require 'shellwords'
require 'securerandom'

##
# Publishes the local HEAD commit to `branch` as a commit that GitHub creates and signs.
# Retries with a rebase if the branch moved meanwhile (e.g. two docs jobs at once).
#
# Repo is in `owner/name` form (e.g. "gini/gini-mobile-android"), not a URL.
#
def push_as_signed_commit(repo, branch, ui, attempts: 3)
  attempts.times do |attempt|
    message = sh("git log -1 --format=%B", log: false).strip
    tree = sh("git rev-parse 'HEAD^{tree}'", log: false).strip
    parent = sh("git rev-parse HEAD~1", log: false).strip
    # Random suffix (not a timestamp): two CI jobs starting in the same
    # second must not collide on the temp ref — a losing `git push` would
    # fail before entering the `begin...ensure` block, defeating the
    # retry/cleanup contract.
    tmp_branch = "ci-signing-tmp-#{SecureRandom.hex(6)}"

    # Upload the files so GitHub can build the commit from them.
    sh("git push origin HEAD:refs/heads/#{tmp_branch}")
    begin
      sha = sh("gh api repos/#{repo}/git/commits -f message=#{message.shellescape} " \
               "-f tree=#{tree} -f 'parents[]=#{parent}' --jq .sha").strip
      sh("gh api -X PATCH repos/#{repo}/git/refs/heads/#{branch} -f sha=#{sha} -F force=false")
      return sha
    rescue => e
      raise if attempt == attempts - 1
      # Only non-fast-forward failures on the PATCH call are rebase-and-retry
      # territory; re-raise auth, permission, validation, and rate-limit
      # failures immediately so the real error reaches the lane boundary
      # instead of being masked by the retry loop.
      raise unless e.message.match?(/not a fast forward|is at .+ but expected/i)
      ui.message "#{branch} moved, rebasing and retrying: #{e.message}"
      sh("git pull --rebase origin #{branch}")
    ensure
      sh("gh api -X DELETE repos/#{repo}/git/refs/heads/#{tmp_branch} || true")
    end
  end
end
```

### Call-site — `fastlane/Fastfile` `release_documentation` (lines ~341-348)

Replace the commit + push block:

```ruby
if !dry_run
  UI.message "Commit and push the new documentation"
  sh("git add --all")
  sh("git diff --quiet --exit-code --cached || git commit -m 'Release #{project_id} documentation for tag #{git_tag}' --author='Team Mobile <team-mobile@gini.net>'")
  git_push_with_retry("gh-pages", UI)
else
  UI.message "Skipping commit and push (dry run)"
end
```

with:

```ruby
if !dry_run
  UI.message "Commit and push the new documentation"
  sh("git add --all")
  # Skip empty commits on idempotent re-runs (same tag, same output).
  if sh("git diff --cached --quiet && echo same || echo changed", log: false).strip == "changed"
    sh("git commit -m 'Release #{project_id} documentation for tag #{git_tag}'")
    if git_token
      # Signed path: GH_TOKEN (or an explicit git_token option) is present,
      # so GitHub creates and signs the commit via REST API. Author becomes
      # the MOBILE_CI_APP bot identity (`gini-mobile-ci[bot]`).
      push_as_signed_commit("gini/gini-mobile-android", "gh-pages", UI)
    else
      # Legacy path: off-CI invocation with only git_user/git_password.
      # Commit remains Unverified; preserved for backward compatibility.
      git_push_with_retry("gh-pages", UI)
    end
  else
    UI.message "No documentation changes to commit"
  end
else
  UI.message "Skipping commit and push (dry run)"
end
```

Also remove the `configure_git_on_ci_machines` call at line 274-276
when `git_token` is set — on the signed path the author identity
comes from the GitHub App bot via the API, not from `git config`.
Keep the call for the legacy `git_user`/`git_password` branch, as that
path still needs a `git config user.{name,email}` for the local
commit's author.

Concretely:
```ruby
if ci && !git_token
  configure_git_on_ci_machines("Team Mobile Schorsch", "team-mobile@gini.net")
end
```

Note: `git_token` is already a local variable in the lane
(resolved from `options[:git_token] || ENV['GH_TOKEN']` at line 244),
so no new variable introduced.

## Test plan

Fastlane / Ruby tooling in this repo has no existing unit-test suite —
the `fastlane/` tree ships `*.rb` files and lane descriptions, no
`spec/` directory, no RSpec or minitest configuration *(confidence:
HIGH — grepped; same as iOS)*. Standing up a Ruby unit-test harness
for a 30-line helper that is almost entirely `sh(...)` calls would
cost more than the value it adds. The verification plan is therefore
**throwaway-branch smoke tests** against real GitHub, mirroring the
iOS plan.

### Smoke tests (one run, manual on a throwaway branch)

Run `release_documentation` against a throwaway `gh-pages` subfolder on
`gini/gini-mobile-android` (fake `project_id` + fake version to avoid
touching real docs), inspect the resulting commit on github.com, and
confirm:
- The commit shows the green **Verified** badge.
- The author is `gini-mobile-ci[bot]` (per the iOS verification —
  `MOBILE_CI_APP` is the same App on both sides).
- The ref `gh-pages` is updated to the new SHA.
- No orphan `ci-signing-tmp-*` refs remain on the remote.

### Already-verified via iOS probe

Because `GH_TOKEN` is minted by the **same** `MOBILE_CI_APP` on both
iOS and Android CI workflows, the API behavior proved on the iOS probe
([run 38028219213](https://github.com/gini/gini-mobile-ios/actions/runs/38028219213),
Verified commits
[`03be913b`](https://github.com/gini/gini-mobile-ios/commit/03be913b4c897d7bf2df8742f560de9037d8c3aa)
and
[`b83ad038`](https://github.com/gini/gini-mobile-ios/commit/b83ad038bb2a2cbbf8774a8dab104413ae94d08c))
transfers to Android. The android-side smoke test is a validation that
the migrated lane WIRES the helper correctly — the signing mechanism
itself is already proven.

### Retry test (R6/R7)

Trigger the retry path manually: on a disposable `gh-pages` subfolder,
pre-position a commit via github.com's web UI *between* the local
`git commit` and the helper's `PATCH ref` call (e.g. by running the
lane under a breakpoint, or by pushing an unrelated commit from a
second clone). Confirm the helper emits the "branch moved" message,
runs `git pull --rebase`, and succeeds on retry. Then separately
inject an auth failure (invalidate `GH_TOKEN` for one call) and
confirm it re-raises immediately per R7 — no wasted retries.

### Pre-smoke-test check

Confirm `MOBILE_CI_APP` has `contents: write` installed on
`gini/gini-mobile-android` (the only target repo for this lane).
Already true today — the current `release_documentation` lane writes
to `gh-pages` via the same App token for its `git clone`, just with
unsigned commits. If missing, the first `gh api` call fails with 403;
one-click fix in App installation settings.

### Not tested

- Unit tests for the helper itself — no Ruby test harness in this
  repo; the sh-mock surface area is almost 100%, and the real surface
  is "does github.com see a Verified commit", which only an
  integration test can tell. iOS probe already answered this.
- GitHub API quota / rate-limiting under high churn — out of scope;
  the helper issues 3 API calls per attempt (POST commit, PATCH ref,
  DELETE temp ref), so up to 9 per push with the default 3 retries.
- `gh` CLI version drift — assume the stable `gh` preinstalled on
  GitHub-hosted ubuntu runners.
- Legacy `git_user`/`git_password` path — unchanged, still exercises
  `git_push_with_retry`. No new coverage needed.

## Out of scope

- **Signing commits on the legacy `git_user`/`git_password` path.**
  Credentials-based auth doesn't grant access to the GitHub App bot
  identity; signing that path would require either provisioning a GPG
  key (rejected per the ticket's cost analysis) or deprecating the
  legacy auth path (expands scope). Left as-is per R5.
- **Switching to GraphQL `createCommitOnBranch`.** iOS proved REST
  `POST /git/commits` auto-signs for App installation tokens
  (undocumented but reproducible). Same App used on android →
  same behavior. If a future android smoke test ever shows Unverified
  (unexpected), pivot the helper body to GraphQL — same signature,
  different API verb. Out of scope for this PR.
- **Tag-push signing.** The ticket is scoped to commits. The existing
  `git_push_release_tag` / `git_push_tag` helpers that push tags for
  `create_release_tags` and `create_documentation_release_tags` stay
  as-is.
- **Removing `git_push_with_retry`.** Still called from the lane's
  legacy auth branch per R5. Decision mirrors iOS's choice to retain
  as a utility.
- **Removing `configure_git_on_ci_machines` entirely.** We only remove
  the call on the signed path (when `git_token` is set). Legacy path
  still needs it for `git config user.{name,email}` on the local
  commit's author.
- **Maven publish lanes (`publish_to_maven_repo`,
  `publish_to_maven_snapshots_repo`).** These use Gradle's HTTP
  publish (Sonatype/Nexus), not git commits. No "Unverified bot
  commit" problem to solve. Verified by code review — no `git commit`
  or `git push` calls inside.
- **Audit of other `git push` sites.** Tag pushes
  (`git_push_release_tag`, `git_push_tag`) are deliberately excluded.
  `git clone` operations are not commit sites. Grepped the entire
  `fastlane/`, `.github/`, `buildSrc/` trees — no other commit-producing
  git sites exist.
- **iOS implementation.** Separately shipped in
  `gini/gini-mobile-ios` PR #1288.

## Open questions

- **Behavior when `HEAD~1` doesn't exist** (e.g. first commit on a
  branch with no history). `git rev-parse HEAD~1` would fail; the
  REST `/git/commits` endpoint accepts an empty `parents[]` for
  initial commits, but the helper doesn't handle this. Unlikely to hit
  in practice (`gh-pages` has history), but worth noting as a known
  gap. Mirrors the iOS spec's identical open question.
- **Whether to also post the smoke-test probe commit on android**
  before merging. Not strictly needed (same App token → same signing
  behavior proved on iOS), but a one-off android probe would make the
  evidence trail symmetric. (confidence: HIGH — same App, same outcome
  expected; probe optional.)

## Implementation plan
- [x] 1. Add `push_as_signed_commit(repo, branch, ui, attempts: 3)` to `fastlane/util/git.rb` (R1, R6, R7, R8, R9, R10) — matches the hardened iOS version (SecureRandom suffix + narrow rescue)
- [x] 2. Rewrite `release_documentation` commit/push block in `fastlane/Fastfile` to (a) add no-op guard (R4), (b) branch on `git_token` presence — signed path via helper when set (R3), legacy `git_push_with_retry` path preserved when absent (R5), (c) drop `--author='Team Mobile …'` on the local commit so author identity comes from the API on the signed path (R2). The `configure_git_on_ci_machines` call at line 273-276 is already correctly gated by `!git_token` and needs no edit.

# Public API surface & binary compatibility

**Reference for `/gini-review`** — read at **§3**, only when the diff touches a releasable module.

**Purpose:** decide whether the diff changes what integrators depend on, and whether it breaks them.

**Why it matters here:** these modules publish to Maven Central, `explicitApi` mode is not enabled,
and Kotlin defaults to public — so a declaration can become permanent API by omission.

**Supports:**

- **Branch detection** — whether `apiCheck` and committed `.api` dumps guard this branch, and how that
  shifts the review from detection to intent
- **Reading an `.api` dump diff** — the highest-signal artefact in such a PR: dumps updated to silence
  CI, removed or re-signed entries, code changes with no dump change, churn outside the ticket
- **Binary-breaking changes** — the table of edits that compile fine and fail integrators at runtime
  (default arguments, `data class` property changes, return-type narrowing/widening, inferred return
  types, `@Target` changes, removals)
- **New public API** — visibility intent, `data class` versus regular class, explicit return types,
  `@RequiresOptIn` gating, KDoc for Dokka
- **Deprecation** — the WARNING → ERROR → hidden → removed cycle and `ReplaceWith`
- **Scope discipline** — only declarations the PR adds or modifies; existing surface is not a backlog
- **Do-not-flag list** — pre-existing surface, example apps, tests, generated dump formatting

**Skip for:** example apps, tests, CI, docs.

**Does not cover:** source-level repo conventions → `android-checklist.md`

Applies to the seven releasable modules published to Maven Central under `net.gini.android`.
Sources: [Kotlin library authors' guidelines](https://kotlinlang.org/docs/api-guidelines-introduction.html),
[Kotlin backward compatibility](https://kotlinlang.org/docs/api-guidelines-backward-compatibility.html),
[AndroidX API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/docs/api_guidelines/).

## First: is `apiCheck` in place on this branch?

The repo gained the [binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator)
plugin at some point. Whether the branch under review has it changes how you review. Check:

```bash
ls */*/api/*.api 2>/dev/null            # committed dumps, e.g. core-api-library/library/api/library.api
grep -n "binary.compatibility.validator" gradle/libs.versions.toml
```

**Dumps present** — `apiCheck` runs in every module's check workflow and fails CI on any public API
difference. CI is now the gate for *detection*; your job shifts to judging **intent** (see the next
section). Do not re-derive what `apiCheck` already catches.

**No dumps** (an older branch, or a version branch from before the plugin was added) — nothing automated catches a
binary-breaking change. Review is the only gate, and an accidentally-public declaration is
blocking, not a nit.

Either way, **`explicitApi` mode is still not enabled** in any module. Kotlin declarations remain
public by default, so a new top-level declaration without `internal` / `private` ships to Maven
Central whether or not anyone intended it. With dumps in place, that shows up as a dump diff rather
than as a silent leak — which is exactly why the dump diff is worth reading.

Note that this repo already contains a large amount of public surface, including many public
`data class` declarations. That is pre-existing, not a backlog to file findings against — see the
scope rule below.

## The `.api` dump diff is your highest-signal check

When a PR touches `<module>/api/<module>.api`, that file **is** the public API change, stated
explicitly. Read it before the implementation.

`README.md` documents the intended workflow: run `apiDump`, review the dump diff, commit it with the
code change. Its key rule, worth enforcing in review:

> An unintentional `apiCheck` failure means you changed or exposed public API by accident (remember
> that Kotlin declarations are public by default) — **restrict the visibility instead of updating the
> dump.**

So the findings to look for:

- **Dump updated to silence CI.** A dump diff adding declarations that the PR description never
  mentions is the signature of someone running `apiDump` to make the build green. Ask whether each
  added entry is deliberate public API or should have been `internal`. This is blocking when the
  additions look incidental to the ticket.
- **Removed or changed entries.** A deleted or re-signed line in a dump is a client-breaking change.
  It needs either a major version bump or a deprecation cycle first — never a silent removal.
  Cross-check against `RELEASE.md`'s parallel-major-version rules.
- **Code changes public API but no dump change.** Either the change is not actually public, or the
  author has not run `apiDump` and CI will fail. Worth asking about rather than blocking.
- **Dump churn unrelated to the ticket.** Entries changing in a module the ticket does not concern
  suggests an accidental visibility change elsewhere.

Findings on a dump diff should quote the specific `.api` line, not the file.

## Scope rule — read this first

Apply everything below **only to declarations the PR adds or modifies.** Existing public declarations are pre-existing surface; flagging them is noise and violates the do-not-flag rule in
`android-checklist.md`. The question is never "is this pattern present in the repo" but "does this diff
add or change public API, and is that deliberate and safe".

If a PR modifies an *existing* public declaration, that is in scope — changing it is the risk.

## Binary-breaking changes — blocking

Each of these compiles fine and breaks integrators at runtime with `NoSuchMethodError` or
`NoSuchFieldError`:

| Change | Why it breaks |
|---|---|
| **Adding a default argument** to an existing public function | The JVM signature changes; previously-compiled callers call a method that no longer exists |
| **Adding a property to a public `data class`**, or reordering properties | Changes the constructor, `copy()`, and `componentN()` signatures |
| **Narrowing a return type** (`Number` → `Int`) | Old clients linked against the wider signature |
| **Widening a return type** (`List` → `Collection`) | Source-breaking: callers using `List`-only members stop compiling |
| **Implicit return type** on a public declaration whose implementation type changes | The inferred type *is* the signature; changing the body silently changes the ABI |
| **Adding a non-default parameter** | Source-breaking: every caller must be updated |
| **Changing `@Target`** on a published annotation | Changes where it applies for existing users |
| **Removing or renaming** any public declaration | Outside a major version bump, this is a hard break |

Note on `@JvmOverloads`: it generates JVM overloads for Java callers but
**does not** preserve binary compatibility for Kotlin callers. Adding it to an existing function is
not a fix for the default-argument problem.

`@PublishedApi internal` declarations are inlined into consumer bytecode — treat them as public for
every rule above, even though they are marked `internal`.

## Review questions for any diff touching a releasable module

1. **Does this add public API?** Kotlin defaults to public. Every new top-level declaration without
   `internal` / `private` ships forever. Is that intended, or a leaked implementation detail?
2. **Does this modify existing public API?** If yes, walk the table above.
3. **Is a new public type a `data class`?** For a type meant to evolve, a regular class with an
   explicit constructor is safer — `data class` bakes property count and order into the ABI. Raise
   this as an improvement for new types, not as a blocker on existing ones.
4. **Are public return types explicit?** An inferred return type on public API is a latent break.
5. **Is a removal or rename present without a major version bump?** Cross-check the module's
   `gradle.properties` and `RELEASE.md`'s parallel-major-version rules.
6. **Is new experimental API opt-in gated?** A `@RequiresOptIn` annotation lets the API change later
   without breaking anyone. Worth suggesting for anything provisional.
7. **Is a deprecation abrupt?** The expected cycle is `DeprecationLevel.WARNING` → `ERROR` → hidden →
   removed in a major release, with `ReplaceWith` supplied so integrators get an automated migration.
   A straight deletion skips all of it.
8. **Is the change documented?** Public API needs KDoc explaining *why* and how to use it — this is
   what integrators read via Dokka. Undocumented new public API is worth raising as an improvement.

## What NOT to flag

- Existing public data classes, existing `@JvmOverloads`, existing inferred return types — all
  pre-existing.
- Anything in `example-app` modules — not published.
- Anything in `src/test` / `src/androidTest`, or in `core-api-library:shared-tests`.
- The absence of `explicitApi` mode. That is a repo-level choice, not a finding against an individual
  PR. Mention it at most once, in "Needs a human", and only when the PR meaningfully expands public
  surface.
- Anything `apiCheck` already catches, on branches that have the dumps. CI reports it precisely; your
  value is judging whether the change was intended, not restating that it exists.
- Formatting or ordering inside a `.api` dump — the file is generated by `apiDump`.

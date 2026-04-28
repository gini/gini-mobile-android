---
name: generate-feature-docs
description: "Generate a plain Markdown feature documentation page for Confluence from GiniBankSDK code changes on the current branch. Use when asked to document a GiniBankSDK feature or generate Confluence docs from the current branch."
argument-hint: --platform <platform> --feature-slug <slug> [--note "..."]
---

# Skill: /generate-feature-docs

Generate a plain Markdown (`.md`) feature documentation page for Confluence from GiniBankSDK source code changes on the current branch.

This skill always targets **GiniBankSDK**. The output is standard Markdown with no Docusaurus frontmatter, admonitions, or site-specific syntax.

---

## Usage

```
/generate-feature-docs --platform <platform> --feature-slug <slug> [--note "..."]
```

`--platform` and `--feature-slug` are **required**.

If any required argument is missing, stop and ask the user. Do not infer or guess.

---

## Arguments

### `--platform`

Always `android` for this repository.

Retained as a required argument to keep the skill interface consistent with other Gini repositories.

### `--feature-slug`

Kebab-case name for the feature. Used as the output file name.

Example: `cross-border-payments` → output file `cross-border-payments.md`

### `--note` (optional)

A plain-text instruction telling the agent what to focus on. Use when the branch contains multiple features or unrelated changes and you only want to document one of them.

```
--note "Focus only on the QR code scanning feature. Ignore changes to the payment flow."
```

The note is the primary filter — any code outside its scope is skipped entirely, even if it is public API.

---

## Style Rules

Apply these rules to all output. They are authoritative over any default behavior.

### Audience

Documentation serves three audiences simultaneously:

| Audience | What they need |
|---|---|
| **Developers** | Precise inputs, outputs, conditions, dependencies, edge cases, and implementation examples |
| **QA engineers** | Expected behavior, validation rules, failure cases, and edge cases specific enough to derive test scenarios |
| **Product Managers / Product Owners** | Business intent, user outcomes, workflow, and scope — without unnecessary implementation detail |

- Define business intent before technical detail.
- Make behavior, rules, and outcomes explicit enough for both engineering and QA use.
- Do not assume all readers share the same technical context.

### Tone and Voice

- **Style:** Neutral, professional, and direct. No marketing language, casual wording, jokes, or sarcasm.
- **Voice:** Second person ("you"), present tense.
  - ✅ "Call `sendTransferSummary()` before calling `cleanup()`."
  - ❌ "In this guide, we will explore how to send the transfer summary."
- **Active voice:** Prefer active over passive.
  - ✅ "The SDK shows the no-results screen."
  - ❌ "The no-results screen is shown by the SDK."
- **Instructions:** Use imperative verbs for procedural content.
  - ✅ "Set `productTag` to `.cxExtractions`."

### Plain Language

| Prefer | Avoid |
|---|---|
| use | utilize |
| create | initiate |
| update | perform an update operation on |
| remove | eliminate |
| return | — |

Do not use: simply · just · easily · obviously · normally · basically

### Terminology

| Use | Never use |
|---|---|
| SDK | library, module, framework |
| document | file (when referring to what Gini processes) |
| integration | implementation (when referring to customer work) |
| Android | android |
| Kotlin | kotlin |
| Return Assistant | RA (on first mention; RA acceptable after) |

- Write the full term first, followed by the acronym in parentheses on first use.
- Use one term per concept consistently throughout. Do not alternate between synonyms.

### Formatting

- **Code blocks:** always specify the language tag (`kotlin`, `xml`, `bash`, `json`).
- **Tables:** use tables for parameters, properties, error codes, localization keys — not prose lists.
- **Paragraphs:** 1–4 sentences. Break longer explanations into sub-sections.
- **Numbered lists:** only when sequence matters.
- **Nesting:** no more than two levels of nested lists.
- **Placeholders:** use `<!-- TOBEADDED -->` for content that cannot be verified from source. Never use `TODO`.

### What to Avoid

- Vague intros: "In this guide, we will explore...", "This document covers..."
- Undocumented assumptions about environment setup
- Restating the section heading in the first sentence
- Over-explaining what the reader can infer from code
- Describing UI by position or color instead of by label

### Quality Rules

Apply before writing the output file:

1. **Match actual behavior.** Reflect actual source behavior — not assumptions or aspirations.
2. **Write so QA can test it.** Behavior descriptions must be specific enough for a QA engineer to derive test scenarios. "The system handles errors appropriately" is not acceptable.
3. **Eliminate ambiguity.** If a sentence can be interpreted more than one way, rewrite it.
4. **Make technical assumptions explicit.** State hidden system conditions, environment dependencies, or data flows.
5. **Distinguish required behavior from implementation suggestion.** Clearly separate what the system must do from how it may be implemented.
6. **Document edge cases explicitly.** For each feature: what happens if a field is empty? What if a permission is denied? What if the backend returns nothing?
7. **Write for maintainability.** Structure content so future updates can be made to individual sections without rewriting the entire page.

---

## Step-by-Step Instructions

You are given `$ARGUMENTS`. Parse it to extract `--platform`, `--feature-slug`, and `--note`, then follow these steps exactly.

### Step 1 — Identify changed GiniBankSDK source files on the current branch

Run:

```bash
git diff main...HEAD --name-only
```

Keep only files from these directories — everything else is out of scope:

- `bank-sdk/sdk/src/main/` (Bank SDK source root)
- `bank-api-library/library/src/main/` (Bank API library source root)
- `capture-sdk/sdk/src/main/` (Capture SDK source root, if the feature touches shared capture logic)

Skip test files, example apps, CI scripts, package manifests, localization strings (collected separately in Step 2), and build output.

Read each kept file. For large files, focus on the diff:

```bash
git diff main...HEAD -- <file>
```

**If `--note` was provided**, apply it as the primary filter: only read and document files related to that scope. Skip everything else even if it is public API.

### Step 2 — Collect localization strings for the feature

**iOS (`--platform ios`):** search all `.strings` files in `BankSDK/` for keys matching the feature prefix:

```bash
grep -r "<featureprefix>" --include="*.strings" BankSDK/
```

Derive the prefix from the feature slug (e.g. `cross-border-payments` → try `crossborder`, `cx`, `crossBorder`).

Also search `CaptureSDK/` if the feature uses shared capture strings:

```bash
grep -r "<featureprefix>" --include="*.strings" CaptureSDK/
```

**Android (`--platform android`):** search `res/values/strings.xml` and `res/values-en/strings.xml` in the relevant modules for matching keys:

```bash
grep -r "<featureprefix>" --include="strings.xml" bank-sdk/sdk/src/main/res/
grep -r "<featureprefix>" --include="strings.xml" capture-sdk/sdk/src/main/res/
```

Collect all matching keys. Typical groups:
- **Feature UI strings** — labels, titles, descriptions shown in the feature's views
- **Edge case / banner strings** — shown in banners or in-flow alerts
- **Permission / error alert strings** — shown when the user denies an OS permission

Omit a group if it has no keys. If you expect keys but cannot find them, add `<!-- TOBEADDED: verify localization keys against source -->`.

### Step 3 — Identify what to document

From the filtered source, extract:
- Public configuration properties on `CaptureConfiguration` (Android, in `bank-sdk/sdk/src/main/.../capture/Configuration.kt`) and their types/defaults
- Public methods or enums a consumer would call or reference
- Extraction result fields populated or modified by this feature
- Behavioral rules — what the SDK does and does not do when this feature is active
- OS APIs used in source — infer `AndroidManifest.xml` requirements:
  - `android.permission.CAMERA` → `<uses-permission android:name="android.permission.CAMERA" />` required
  - `android.permission.READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` → `<uses-permission>` required
- Edge cases and error states described in source or doc comments

**Skip:**
- Anything `internal`, `private`, or prefixed with `_`
- Symbols used only in test files
- UI implementation details invisible to the SDK consumer
- Internal classes like `GiniCaptureUserDefaultsStorage` or `GiniConfiguration`

**Cross-feature impact:** if the branch modifies behavior of existing features (e.g. Skonto or Return Assistant suppressed when a new flag is set), document those changes in an `## Impact on Other Features` section.

**Flag for review:** any public API with no tests or usage examples — wrap it in a `> **Needs review:**` blockquote.

### Step 4 — Write the Markdown file

Use the structure below. Replace every `[placeholder]` with content derived from source. Include a section only when the source provides evidence for it. Do not invent content — use `<!-- TOBEADDED -->` for anything unverifiable.

Use only standard Markdown. No Docusaurus frontmatter, no `:::caution` / `:::tip` admonitions. Use `> **Note:**`, `> **Warning:**`, `> **Caution:**` blockquotes where admonitions would otherwise appear.

---

**Output structure:**

```md
# [Feature Name]

> **Note:** To use the [Feature Name] feature, contact Gini Customer Support to have it enabled in the backend platform.

[One paragraph: what the feature does, when it activates, and what the user experiences. Present tense, second person. Accurately reflect all supported input methods — camera, imported PDF, file opened via share sheet — unless you have explicit evidence the feature is camera-only. Example: "Cross-Border Payments routes captured or imported documents through the Gini CX Payments extraction pipeline instead of the standard SEPA pipeline."]

## Permission Handling

[Include this section ONLY if the feature uses an Android dangerous permission. Remove entirely if not applicable.]

Add the following to your `AndroidManifest.xml` inside the `<manifest>` tag:

```xml
<uses-permission android:name="android.permission.[PERMISSION_NAME]" />
```

This feature includes built-in permission handling:

- If the user has not been asked for [permission] before, the SDK requests it at runtime when [trigger action].
- If the user previously denied permission, the SDK [describe behavior — e.g. shows a rationale screen and redirects to Settings].

> **Warning:** Without the manifest entry the permission request will fail and the feature will not work.

## Configuration

The feature is controlled via the `[propertyName]` property on `CaptureConfiguration`.

**Enable (default):**

```kotlin
val captureConfiguration = CaptureConfiguration(
    [propertyName] = [defaultValue],
    // ...
)
GiniBank.setCaptureConfiguration(context, captureConfiguration)
```

**Disable:**

```kotlin
val captureConfiguration = CaptureConfiguration(
    [propertyName] = false,
    // ...
)
GiniBank.setCaptureConfiguration(context, captureConfiguration)
```

## UI Customization

[This section is mandatory for all features that render any user-visible UI — strings, colors, icons, or layouts. Remove only if the feature has no UI at all.]

The [Feature Name] UI supports customization of strings, colors, and other appearance attributes. <!-- TOBEADDED: link to UI customization guide -->

## Extraction Result

[Include only if the feature adds or modifies fields in the extraction result.]

When the user completes the [Feature Name] flow, the SDK delivers the updated extraction result via the `GiniBank` result callback:

```kotlin
GiniBank.startCaptureFlow(
    context,
    captureResultLauncher,
    // ...
)

// In your Activity/Fragment:
private val captureResultLauncher = registerForActivityResult(GiniBank.createCaptureFlowContract()) { result ->
    when (result) {
        is CaptureResult.Success -> handleExtractions(result.specificExtractions)
        is CaptureResult.Error -> handleError(result.value)
        CaptureResult.Empty -> handleNoResults()
        CaptureResult.Cancel -> handleCancellation()
    }
}
```

The `AnalysisResult` includes:

| Field | Value for [Feature Name] |
|---|---|
| `[fieldName]` | [What this feature sets or updates.] |

## Sending Transfer Summary

[Include only if the feature changes how `sendTransferSummary` is called or adds a new overload.]

[Describe which overload to use, with a complete Kotlin example and an explanation of routing behavior.]

## Impact on Other Features

[Include one sub-section per existing feature whose behavior changes when this feature is active. Remove this entire section only if there are genuinely no interactions.]

### [Feature Name] and [Other Feature]

[Describe what changes — e.g. "Return Assistant is suppressed when `productTag` is set to `ProductTag.CxExtractions`, regardless of whether `returnAssistantEnabled` is `true` or whether line items are present."]

## Edge Cases

| Scenario | SDK behavior |
|---|---|
| [scenario] | [what the SDK does] |

## Localization Keys

[Include only if the feature renders user-visible strings. Remove entirely if not applicable.]

The [Feature Name] UI uses the following string resource keys. Override them in your application's `strings.xml` to provide custom text.

### Feature UI Strings

| Key | Default (de) | Default (en) | Description |
|---|---|---|---|
| `[key]` | `[German default]` | `[English default]` | [What this string is used for.] |

### Edge Case / Banner Strings

[Include only if the feature shows contextual banners or alerts.]

| Key | Default (de) | Default (en) | Description |
|---|---|---|---|
| `[key]` | `[German default]` | `[English default]` | [What this string is used for.] |

### Permission Alert Strings

[Include only if the feature uses an OS permission.]

| Key | Default (de) | Default (en) | Description |
|---|---|---|---|
| `[key]` | `[German default]` | `[English default]` | [Shown when permission has been denied.] |

---

> **QA Recommendation:** We highly recommend scheduling a QA session with Gini before releasing the [Feature Name] feature to your customers.
```

---

### Step 5 — Resolve the output path

Output file: `docs/[platform]/features/[feature-slug]/[feature-slug].md` relative to the repository root.

Example for `--platform ios --feature-slug cross-border-payments`:
```
docs/ios/features/cross-border-payments/cross-border-payments.md
```

Create the directory if it does not exist.

### Step 6 — Write the file

Write the generated file. Confirm with a single line:

```
✅ Written to <path>
```

---

## Security

- Read any source file in this repository for context.
- Write only `.md` files inside `docs/` in the current repository.
- Never modify source code, CI configs, or any file outside `docs/`.
- Never run shell commands beyond `git diff`, `git show`, `grep`, and file reads.
- All output requires human review before publishing to Confluence.

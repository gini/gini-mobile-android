# XPL-445: `design-system-specialist` agent

Status: implemented
Ticket: https://ginis.atlassian.net/browse/XPL-445 (sub-task of XPL-402 — "[Android] Add 7 specialist agents to the Claude agent team")

- Source: the Confluence page "AI Planning for 2026" (PLMO), September 2026 section: *"`design-system-specialist` — Checks colours, text styles and spacing come from our theme instead of hardcoded values."* and the XPL-402 rule: **every agent must be valid for our repo; generic Kotlin/Android advice is not enough.** The Jira ticket has no description.
- Q4 2026 section of the same page: the XML → Compose migration (Google `migrate-xml-views-to-jetpack-compose`, step 6 "Set up Compose theming") and the first screenshot test. This agent is the reviewer for theme work in both.
- iOS twin: **none.** iOS used the seventh slot for `liquid-glass-specialist` (XPL-412). iOS design-system rules live in `gini-mobile-ios` `.claude/rules/mandatory-rules.md` §"Design system — colors, fonts, spacing" (`GiniColorScheme` tokens, `textStyleFonts`, a local `Constants` enum, dark mode required, never hardcode). We take the **section order** of `liquid-glass-specialist` and the Android siblings, and the **rule shape** of `mandatory-rules.md`. No content transfers.
- Android siblings on `main`: `android-security-specialist`, `concurrency-specialist`, `code-reviewer`, `architecture-specialist`, `performance-specialist`. The agent follows their section order, severity model and tool list.
- Branch `XPL-445-android-design-system-specialist`, cut from `origin/main` at the XPL-442 merge (#1009). The security and concurrency branches are already merged into `main`, so `main` is the newest base. `XPL-444-android-debugger-agent` is **not** merged and edits the same orchestrator, `gini-task.md` and hook lines — whichever PR merges second rebases those few lines.

## Problem

Nobody owns the design system. `compose-specialist`, `views-specialist`, `code-reviewer` and the orchestrator each have one line that says "don't hardcode colours", but:

- Nobody reviews the theme **itself**: `GiniTheme`, `GiniColorScheme`, `GiniColorPrimitives`, `GiniTypography`, the XML `Root.GiniCaptureTheme` / `Root.GiniPaymentTheme` chains, `colors.xml`, `typography.xml`, `dimens.xml` and their `values-night` and `sw600dp` variants.
- Nobody knows that these resources are **integrator API**. There is no `public.xml`, so every `gc_`, `gbs_`, `gps_` and `ghs_` resource and every `Root.` style can be overridden by the host app. Renaming or deleting one breaks a bank's customisation silently — no compile error, no `apiCheck` failure.
- The tooling does not catch anything: `buildSrc/config/lint/lint.xml` ignores `SpUsage`, `PxUsage` and `ResourceAsColor`, and detekt `MagicNumber` skips `@Composable` functions. A `20.dp` in a composable is never flagged.
- `code-reviewer` says: "Design-system questions have no specialist in the team yet." `architecture-specialist` and `performance-specialist` say `design-system-specialist` is "planned but not written yet".

## Decisions (clarifying questions, 2026-10-21)

| Question | Decision |
|---|---|
| Ownership vs `compose-specialist` / `views-specialist` | **design-system-specialist owns every design-system finding**: colour, typography, spacing, shape, theme resource, night/tablet variant, override chain. `compose-specialist` keeps Compose idiom (state, effects, hosting); `views-specialist` keeps lifecycle, ViewBinding, navigation, layout structure. Where they report the same line, the design-system finding stands and the orchestrator drops the duplicate. Their files are **not edited** (XPL-402), so the orchestrator resolves the overlap — same pattern as `performance-specialist`. |
| Contrast | Stays with `a11y-specialist` (WCAG target is **AAA**, 7:1 — see memory). design-system-specialist flags a *new colour pairing* and hands contrast to `a11y-specialist`; it never judges a ratio itself. |
| Shared rules file | **None.** Rules live in `design-system-specialist.md` only. `code-reviewer` escalates to it. No `.claude/rules/design-system.md`. |
| Hotspots found during exploration | **Listed for tickets, not fixed** (same as XPL-442/443). No Kotlin/XML/Gradle change in this PR. |
| Figma | **Yes, when a figma.com link is given** (ticket, PR or prompt). The agent compares token names, colour variables and spacing with Figma. If the Figma MCP is not available in the session, it skips this step and says so in **Needs a human**. See Open question 1 for how the tools are wired. |
| File name | `.claude/agents/design-system-specialist.md` — no `android-` prefix, like `performance-specialist` and `architecture-specialist`. |
| Severity | `blocker` / `warning` / `nit`, same as the siblings. |
| Tools | `Read, Edit, Write, Glob, Grep` — same as the siblings. No MCP tools (Open question 1, resolved). |

## What the code really does (verified on `origin/main`, 2026-10-21)

### Compose (capture-sdk, reused by bank-sdk)

- Package `capture-sdk/sdk/src/main/java/net/gini/android/capture/ui/theme/`.
- `GiniTheme.kt`: `@Composable fun GiniTheme(darkMode: Boolean = isSystemInDarkTheme(), content)`. Builds `GiniColorPrimitives.buildColorPrimitivesBasedOnResources(context)`, picks `giniDarkColorScheme`/`giniLightColorScheme`, reads `extractGiniTypography()`, provides `LocalGiniColors` + `LocalGiniTypography`, and wraps a `MaterialTheme` built by `giniColorSchemeBridge(...)`. Accessor `object GiniTheme { colorScheme; typography }`.
- `colors/GiniColorPalette.kt`: `@Immutable data class GiniColorScheme` with nested groups (background, bottomBar, topAppBar, placeholder, text, card, badge, button, buttonOutlined, progressBarButton, textField, toggles, dialogs, icons, datePicker, checkbox, contextMenu, logo), all `Color.Unspecified` by default.
- `colors/GiniColorPrimirives.kt` (file name has a typo): `GiniColorPrimitives` — Figma primitives `accent01–05`, `dark01–06`, `light01–06`, `success/error/warning01–05`. The `Color(0xFF…)` defaults here are the **only legitimate** `Color(0x…)` literals in SDK main code. Resolved from `R.color.gc_*`.
- `typography/GiniTypography.kt` + `TypographyToTextStyleBridge.kt`: Compose text styles are read from the XML `Root.GiniCaptureTheme.Typography.*` styles via `compose-theme-adapter-3` styleables (`libs.accompanist.themeAdapter`). `Mdc3Theme` is not used.
- **No Compose spacing token.** No `Dimens`/`Spacing` object. Only `theme/modifier/Width.kt` (`Modifier.tabletMaxWidth()` via `dimensionResource(R.dimen.gc_tablet_width)`).
- Per-component colours: `@Immutable data class XxxColors` + `companion @Composable fun colors(x = GiniTheme.colorScheme.…)` in `ui/components/**/Gini*Colors.kt`; bank-sdk screens follow it (`capture/skonto/colors/SkontoScreenColors.kt`, `section/*SectionColors.kt`, `digitalinvoice/skonto/colors/`, `skonto/help/colors/`, `invoice/colors/`, `transactiondocs/ui/**/colors/`).
- Public API: `GiniTheme`, `GiniColorScheme`, `GiniColorPrimitives`, `GiniTypography` are in `capture-sdk/sdk/api/sdk.api`. Integrators **cannot** pass a custom scheme to `GiniTheme`; customisation is XML-only.

### XML / Views

- capture-sdk `res/values/styles.xml`: `Root.GiniCaptureTheme.Internal` (parent `Theme.Material3.DayNight.NoActionBar`) → `Root.GiniCaptureTheme` → `GiniCaptureTheme`; widgets `Root.GiniCaptureTheme.Widget.X` → `GiniCaptureTheme.Widget.X`. `values-night/styles.xml` mirrors with `.Night`.
- capture-sdk `values/typography.xml`: `Root.GiniCaptureTheme.Typography.{Headline1-6, Subtitle1/2, Body1/2, Button, Caption1, Overline}` — the token source, so literal `sp` there is fine.
- capture-sdk `values/colors.xml`: `gc_{accent|dark|light|success|error|warning}_NN` + `gc_camera_preview_shade`; `values-night/colors.xml` overrides a few. `values/dimens.xml`: spacing scale `gc_extra_small`, `gc_small`, `gc_medium`, `gc_medium_12`, `gc_large`, `gc_large_20` … `gc_large_120`, plus `values-sw600dp*` variants.
- bank-sdk `values/styles.xml` **re-declares `Root.GiniCaptureTheme`** and adds `gbs*` attributes pointing at `gc_` colours; only `gbs_` dimens of its own.
- internal-payment-sdk: XML only. `Root.GiniPaymentTheme` → `GiniPaymentTheme`, applied by `Context.wrappedWithGiniPaymentTheme()` in `utils/extensions/Context.kt`; `gps_` colours (`color/`, `color-night/`), `gps_` dimens, `Root.GiniPaymentTheme.Typography.*`.
- health-sdk: XML only; `ghs_` colours; reuses `GiniPaymentTheme.Typography` and `gps_` dimens. Customisation guide `health-sdk/sdk/src/doc/source/customization.rst` documents the `Root.` override pattern and links the Figma UI-customisation file.
- Layouts mostly apply typography with `style="@style/GiniCaptureTheme.Typography.Body2"` (or `GiniPaymentTheme.Typography.*`); a few use `android:textAppearance`.
- No `public.xml` anywhere.

### Where the debt is (patterns, for the agent's "known existing pattern" list)

- `.dp` literals in composables: mostly bank-sdk (`capture/skonto/SkontoScreenContent.kt`, `SkontoScreenContentHelperMethods.kt`, `digitalinvoice/skonto/DigitalInvoiceSkontoFragment.kt`, `skonto/help/SkontoHelpFragment.kt`, `invoice/InvoicePreviewScreen.kt`), less in capture-sdk. `dimensionResource` is rarely used.
- Non-zero `dp` literals in layouts exist in every XML module (for example `capture-sdk` `layout-land/gc_fragment_onboarding.xml`); most layouts use `@dimen/`.
- Legitimate non-token colours: `GiniColorPrimitives` defaults, `internal/pdf/RendererLollipop.java` (`Color.WHITE` page background), `PaymentProviderApp.kt` (`Color.parseColor` on backend provider colours), `tools:background` in layouts.

### Where the other agents already stand

- `compose-specialist.md` §"Design system (GiniTheme tokens)", item 6 and item 12; `views-specialist.md` items 6, 8, 11; `a11y-specialist.md` item 14 (contrast); `code-reviewer.md` Repo Rule line on hardcoded colour + the "no specialist" sentence; orchestrator Mandatory Rules "Design system — colors/typography".

## Requirements

- **R1 (MUST, entry):** Given the branch, when the orchestrator or a developer invokes `design-system-specialist`, then `.claude/agents/design-system-specialist.md` loads with `name: design-system-specialist`, a `description` naming `GiniTheme`/`GiniColorScheme`/`GiniTypography`, the XML `Root.` theme chains, the `gc_`/`gbs_`/`gps_`/`ghs_` resources as integrator customisation API, spacing, night and tablet variants, and Figma comparison; tools as decided.
- **R2 (MUST, entry):** Given a diff that touches any `@Composable` UI, a layout XML, `values*/colors.xml`, `styles.xml`, `typography.xml`, `dimens.xml`, `attrs.xml`, `res/color*/`, anything under `capture-sdk` `ui/theme/` or `ui/components/`, a `*Colors.kt` class, or a Sphinx `customization.rst`, when the orchestrator triages it, then its delegation rules name `design-system-specialist`, alongside `compose-specialist`/`views-specialist` with every design-system finding assigned to `design-system-specialist`, and alongside `a11y-specialist` for any new colour pairing.
- **R3 (MUST, happy):** Given a diff that adds `Text(color = Color(0xFF1A73E8), fontSize = 16.sp)` and `Modifier.padding(20.dp)` to a bank-sdk composable, when the agent reviews it, then it reports a *Colour* warning (use a `GiniTheme.colorScheme` token or the screen's `...ScreenColors`), a *Typography* warning (use a `GiniTheme.typography` style) and a *Spacing* warning that names the matching `gc_` dimen via `dimensionResource` — and says there is no Compose spacing object yet, so it does not invent one.
- **R4 (MUST, happy):** Given a diff that renames or deletes a `gc_`/`gbs_`/`gps_`/`ghs_` colour, dimen or `Root.*` style, or changes a `Root.` parent, when the agent reviews it, then it reports a **blocker** labelled *Customisation API* that explains integrators override these resources (no `public.xml`, no `apiCheck` coverage), asks for a release note / customisation-guide update, and names `architecture-specialist` for the public-API decision.
- **R5 (MUST, happy):** Given a diff that adds a colour to `values/colors.xml` without a `values-night` decision, or a new `Gini*Colors` / `GiniColorScheme` token that is not mapped in both `giniLightColorScheme` and `giniDarkColorScheme`, when reviewed, then it reports a *Dark mode* warning.
- **R6 (MUST, happy):** Given a diff with `isSystemInDarkTheme()` + `Color.White`/`Color.Black` inside a component, when reviewed, then it reports a *Dark mode* warning: read the token from `GiniTheme` instead of branching on the system flag (the `darkMode` parameter of `GiniTheme` must win).
- **R7 (MUST, error):** Given a diff touching only (a) the `Color(0xFF…)` defaults in `GiniColorPrimirives.kt`, (b) literal `sp` in `typography.xml`, (c) `Color.WHITE` in `RendererLollipop.java`, (d) `Color.parseColor` in `PaymentProviderApp.kt`, (e) `tools:background="#…"`, or (f) `0dp` in a `ConstraintLayout` child, when reviewed, then it reports **no** finding for them.
- **R8 (MUST, error):** Given a diff that only changes a colour pair's contrast, state handling in a composable, or a Fragment lifecycle call, then the agent reports nothing for it and names `a11y-specialist`, `compose-specialist` or `views-specialist` as owner.
- **R9 (MUST, happy):** Given any fix that renames/removes a resource, changes `GiniTheme`'s signature or a public theme class, adds a Compose spacing object, changes a `Root.` chain, `lint.xml` or `detekt.yml`, or touches anything the diff did not introduce, then the agent asks the user first and does not apply it. Replacing a literal the diff introduced with an **existing** token may be applied directly.
- **R10 (MUST, happy):** Given a figma.com link in the prompt, ticket or PR and the Figma MCP available, when reviewed, then the agent compares colour variables, text styles and spacing of the referenced node with the tokens the diff uses and reports mismatches as *Figma* findings with the node id. Without the MCP it lists "Figma comparison not run — MCP not available" under **Needs a human**.
- **R11 (MUST, error):** Given a full audit with no diff, then the agent applies **no** edit, reports §Hotspots at most as `nit` "known existing pattern", and ends with "applied: none".
- **R12 (SHOULD):** The agent file has no `file:line` citation, no hard-coded count, and every path, class, style, resource prefix and tool it names exists on the branch.
- **R13 (MUST, error):** `compose-`, `views-`, `a11y-`, `testing-specialist.md`, `concurrency-specialist.md` and `android-security-specialist.md` are unchanged against `origin/main`.
- **R14 (MUST, entry):** `code-reviewer.md` escalates design-system findings to `design-system-specialist` and no longer says design-system questions have no specialist; `architecture-specialist.md` and `performance-specialist.md` no longer say it is "planned but not written yet" (the debugger mention stays until XPL-444 merges).
- **R15 (MUST, entry):** The `UserPromptSubmit` hook in `.claude/settings.json` and `.claude/commands/gini-task.md` list `design-system` among the routed specialists.

## Affected files

No Gradle module changes. All under `.claude/` and `specs/`:

| File | Change |
|---|---|
| `.claude/agents/design-system-specialist.md` | **new** — the agent |
| `.claude/agents/gini-orchestrator.md` | team-table row (and design-system notes on the compose/views rows), new delegation rule + renumbering, Mandatory Rules design-system bullet points at the agent, Knowledge Sources line |
| `.claude/commands/gini-task.md` | specialist list |
| `.claude/settings.json` | hook text |
| `.claude/agents/code-reviewer.md` | escalation list; replace the "no specialist" sentence |
| `.claude/agents/architecture-specialist.md` | boundary bullet: drop `design-system-specialist` from "planned but not written yet"; hand theme resources to it |
| `.claude/agents/performance-specialist.md` | one boundary sentence: `design-system-specialist` owns token choice, performance owns the cost (Open question 2, resolved) |
| `.claude/skills/gini-build/platform.md` | new "Agent files" section: dry-running a new agent, and removing a topic from its old owner |
| `specs/XPL-445-feature.md` | this spec |

## Design — the agent file

Section order (siblings + iOS `liquid-glass-specialist`):

1. **Frontmatter** — name, description (R1), tools.
2. **The most important fact — these are SDKs, and the theme is integrator API.** Integrators restyle the SDK by overriding `gc_`/`gbs_`/`gps_`/`ghs_` resources and `Root.*` styles in their own app. No `public.xml` → all of it is public. A rename is a silent break. Compose `GiniTheme` reads those XML resources, so XML stays the source of truth even for Compose screens.
3. **Repo Context** — per-module table (capture: Compose + XML `gc_`; bank: reuses capture, `gbs_`; internal-payment: XML `gps_`; health: XML `ghs_`, reuses `GiniPaymentTheme.Typography`). Each fact with a re-check command (`git grep`), no counts.
4. **Knowledge Source** — `AGENTS.md`; `compose-specialist.md` §Design system as background; Google `migrate-xml-views-to-jetpack-compose` and its "Migrate XML themes to Compose" reference (map XML theme → Compose, "don't hardcode migrated values", migrate only what the screen needs); Google `jetpack-compose/theming/styles` noted as **not applicable yet** (needs compileSdk 37 and alpha Compose; repo is on compileSdk 36); the health-sdk `customization.rst` as the shape of the integrator contract.
5. **Core Instructions** — diff-first; map every literal to an existing token before proposing a new one; never invent a token or spacing object; smallest safe fix.
6. **What You Review** — numbered groups, each with "why it matters to an integrator" and a fix path:
   1. *Colour* — `Color(0x…)`, `Color.X`, `#RRGGBB`, `MaterialTheme.colorScheme` instead of `GiniTheme.colorScheme` / `...Colors` class / `@color/gc_*` / `?attr/`.
   2. *Typography* — `fontSize`/`fontWeight` literals, `android:textSize`, `TextStyle(...)` instead of `GiniTheme.typography` / `@style/GiniCaptureTheme.Typography.*` / `GiniPaymentTheme.Typography.*`.
   3. *Spacing & shape* — `.dp` literals and layout `dp` literals vs the `gc_`/`gbs_`/`gps_` dimen scale; `RoundedCornerShape(n.dp)` vs `MaterialTheme.shapes` set by `GiniTheme`.
   4. *Dark mode & variants* — night parity in `values-night`/`color-night`, `isSystemInDarkTheme()` in components, light/dark scheme parity, `sw600dp` parity.
   5. *Customisation API* — rename/delete/re-parent of a resource or `Root.` style; new resource without module prefix; new theme class or `GiniTheme` parameter (with `architecture-specialist`); customisation guide update.
   6. *Theme plumbing* — Compose hosted without `GiniTheme { }`; internal-payment/health screens not using `wrappedWithGiniPaymentTheme()`; new per-screen colours not following the `...ScreenColors` pattern.
   7. *Figma* — R10.
7. **Boundaries & handoffs** — contrast → `a11y-specialist`; Compose idiom → `compose-specialist`; View lifecycle/layout structure → `views-specialist`; public API surface → `architecture-specialist`; recomposition cost of a `...Colors` class → `performance-specialist`.
8. **What NOT to Flag** — R7 list.
9. **Review Checklist**, **Review Process** (with the Figma step), **Output Format** (category-grouped, severity, **Needs a human**, "applied: …"), **Severity Guide**.

## Test plan

Agent files have no unit tests. Run by `/gini-build`: static lint by Bash, dry runs with `subagent_type: design-system-specialist` (start a fresh session after the file exists so the agent loads).

| Check | Proves |
|---|---|
| T1 Frontmatter `name`/`description`/`tools`; file name matches `name` | R1 |
| T2 No `:[0-9]+` citations and no counts in the agent file (same greps as XPL-442 T2) | R12 |
| T3 Every backticked path/class/style/resource resolves on the branch (XPL-442 lint script approach) | R12 |
| T4 Dry run: synthetic bank-sdk composable with `Color(0xFF1A73E8)`, `16.sp`, `padding(20.dp)` → three warnings with existing-token fixes, no invented spacing object | R3 |
| T5 Dry run: rename `gc_accent_01` → `gc_primary` in capture-sdk `values/colors.xml` → blocker *Customisation API*, names `architecture-specialist`, asks before applying | R4, R9 |
| T6 Dry run: add `gc_new_surface` only to `values/colors.xml`; add a token to `GiniColorScheme` mapped only in the light scheme → *Dark mode* warnings | R5 |
| T7 Dry run: diff touching only `capture-sdk/sdk/src/main/java/net/gini/android/capture/ui/components/tooltip/GiniTooltipBoxColors.kt`'s `isSystemInDarkTheme()` branch → *Dark mode* warning | R6 |
| T8 Dry run: diff touching only the R7 exception files → no findings | R7 |
| T9 Dry run: change only a text/background pair; change only `remember` → no finding, names `a11y-specialist` / `compose-specialist` | R8 |
| T10 Dry run with a Figma link from the Bank SDK UI-customisation file → *Figma* findings or the "MCP not available" line | R10 |
| T11 Full audit of capture-sdk `ui/theme/` + bank-sdk `capture/skonto/` → hotspots as nits, `git status` clean, "applied: none" | R11 |
| T12 `git diff origin/main -- <the six unchanged agent files>` is empty | R13 |
| T13 Greps: `design-system-specialist` in orchestrator, `code-reviewer.md`, hook, `gini-task.md`; "Design-system questions have no specialist" count 0; orchestrator dry run with the T4 diff picks `design-system-specialist` + `compose-specialist` + `a11y-specialist` | R2, R14, R15 |
| T14 (optional) `/code-review` on the branch | — |

## Hotspots found (for tickets — not fixed here)

1. **`GiniTheme` ignores `darkMode` in the Material3 bridge.** `giniColorSchemeBridge(primitives, isLight = !isSystemInDarkTheme())` is called without `isLight`, so `GiniTheme(darkMode = true)` on a light system gives Gini dark tokens but a light `MaterialTheme`. Any Material3 component reading `MaterialTheme.colorScheme` shows the wrong mode.
2. **`gc_warning_05` missing from `colors.xml`.** `GiniColorPrimitives.warning05` keeps its hardcoded default and `buildColorPrimitivesBasedOnResources` never maps it, so integrators cannot override it.
3. **`GiniTooltipBoxColors` branches on `isSystemInDarkTheme()` with `Color.White`/`Color.Black`** instead of tokens — same `darkMode` problem as 1, and not customisable.
4. **`QrCodeEducationPopupContent` uses `Color.Black.copy(alpha = 0.75f)`** — not customisable.
5. **`GiniHealthTheme` is defined only in health-sdk `values-night/styles.xml`** and nothing references it.
6. **No Compose spacing token.** bank-sdk Skonto/invoice screens use many `.dp` literals; detekt `MagicNumber` skips `@Composable` and lint ignores `SpUsage`/`PxUsage`, so nothing catches new ones.
7. Stray `ˇ` character after the OutlinedButton style in capture-sdk `values/styles.xml`.
8. No `public.xml`: every resource is public by accident. A decision ticket (declare public resources, or document all prefixes as API).

Found by the full-audit dry run (2026-10-21), verified against the code:

9. **Compose and Views read different typography layers.** `extractGiniTypography()` reads `R.style.Root_GiniCaptureTheme_Typography_*`, while the XML theme and layouts use the child `GiniCaptureTheme.Typography.*`. An integrator who overrides the child (as the health-sdk guide shows for its theme) restyles the View screens but not the Compose ones (Skonto, Skonto help, invoice preview). Check what the Bank customisation guide tells integrators first.
10. **`GiniTheme(darkMode = …)` resolves primitives against the system night mode**, and the `remember` has no key — extends hotspot 1.
11. **Material3 typography is not bridged** (`MaterialTheme.typography.copy()`), so text inside an M3 component without an explicit style uses the Material font.
12. **Dead `...Colors` fields in Skonto:** the `InfoDialog` text never reads `SkontoInfoDialogColors.textColor`; `GiniSwitch` is called without `SkontoSectionColors.switchColors`; `GiniDatePickerDialog` without `SkontoScreenColors.datePickerColor` (same in `DigitalInvoiceSkontoScreenColors`). Same values today, but the classes promise customisation the screen ignores.
13. **Card status colours** (`containerSuccess/Warning/Error`) use the same light pastels in the dark scheme — maybe on purpose; a11y to check contrast.
14. For `performance-specialist`: `extractGiniTypography()` runs `obtainStyledAttributes` per style on every `GiniTheme` recomposition without `remember`.

## Merge with `main` (2026-10-26)

`main` gained two changes after this branch was cut. Both are folded in:

- **XPL-444 `debugger-specialist` (#1015).** Conflicts in the orchestrator, `code-reviewer.md`, `architecture-specialist.md`, `performance-specialist.md`, `gini-task.md` and the hook were resolved by keeping both agents. Orchestrator rules: 11 is design-system, 12 is debugger, 13 code-reviewer, 14 localization. `debugger-specialist.md` now routes theme causes and `GiniTheme` to `design-system-specialist` (it predates this agent), so R13's "unchanged" list no longer applies to it.
- **PP-3849 runtime colours.** `CustomResourceProvider` is a third customisation path for the 32 `gc_` palette names. The agent file now covers it: colour reads must go through `GiniTheme` or the `RuntimeColors.kt` helpers (`giniColor()` …), or the provider does not reach them. It also changes two hotspots: `gc_warning_05` (2) can now be overridden through the provider, though still not through XML; and hotspot 10 (`remember` without a key, forced `darkMode` on system primitives) now applies only when no provider is set.

## Out of scope

- Fixing any hotspot; creating the tickets (the user creates them).
- Adding a Compose spacing object, a lint/detekt rule, `public.xml`, or the Compose Styles API.
- A shared `.claude/rules/design-system.md`; changes to `AGENTS.md` or `gini-review/platform.md`.
- Editing the six agent files in R13.
- Mirroring to `gini-mobile-ios`.
- The screenshot test and XML → Compose migration from the Q4 plan (this agent reviews them later; it does not build them).

## Open questions

1. **How the Figma MCP tools are wired into the agent.** A subagent's `tools:` list needs exact MCP tool names, and the Figma server name differs per machine (a claude.ai connector gets a UUID name, the Figma plugin gets a fixed one). Options: (a) list the Figma plugin's fixed tool names, and ask the team to use the plugin; (b) keep the sibling tool list and let the caller (`/gini-task` session or `/gini-review`) fetch the Figma data and pass it in the prompt. `/gini-build` checks what Claude Code supports first and asks the user. Recommendation: (b) if (a) is not portable.
2. **`performance-specialist.md` says `design-system-specialist` is "planned but not written yet".** R13 keeps sibling specialist files unchanged, but leaving the line makes it wrong. Recommendation: edit only that one sentence (as XPL-442 did for `architecture-specialist.md`), and drop `performance-specialist.md` from R13.

## Open questions — resolved (2026-10-21)

1. **Figma tools → caller fetches.** Claude Code accepts `mcp__<server>__*` in a subagent's `tools:`, but our Figma server name is not the same on every machine (a claude.ai connector gets a UUID-style name; a plugin gets `mcp__plugin_<plugin>_<server>__…`). A fixed name would silently give the agent no Figma tools on other laptops. So the agent keeps the sibling tool list. `.claude/commands/gini-task.md` tells the main session to fetch the Figma node (`get_variable_defs`, `get_design_context`/`get_screenshot`) and pass the result in; the orchestrator's rule 11 says the same. With only a link and no data, the agent writes "Figma comparison not run" under **Needs a human**. R10 is met in that form.
2. **`performance-specialist.md` — one sentence edited** (user decision). R13 now covers six files, not seven.

## Implementation plan

- [x] 1. Resolve Open questions 1 and 2 with the user.
- [x] 2. Write `.claude/agents/design-system-specialist.md` (R1, R3–R12).
- [x] 3. Wire `gini-orchestrator.md` — team-table row (plus notes on the compose/views rows), delegation rule, Mandatory Rules bullet, Knowledge Sources (R2).
- [x] 4. Wire `gini-task.md` and the hook in `settings.json` (R15).
- [x] 5. Wire `code-reviewer.md`, `architecture-specialist.md` (and `performance-specialist.md` if Open question 2 = yes) (R14).
- [x] 6. Static lint T1–T3, T12, T13 greps.
- [x] 7. Dry runs T4–T11 (2026-10-21; run by a general-purpose agent that loaded the agent file, because a new agent type only loads in a fresh session). All passed: T4 three warnings with existing tokens and no invented spacing object; T5 blocker + ask first + `architecture-specialist`; T6/T7 dark-mode warnings; T8 no findings; T9 handed to `compose-specialist`/`a11y-specialist`; T10 "Figma comparison not run" under Needs a human; T11 report-only, "applied: none", every Repo Context fact confirmed. Their feedback was folded into the agent file: non-transitive `R` in bank-sdk, `gc_*` blast radius into bank-sdk and the example apps, the Root-vs-child typography split, the bank-sdk night chain, severity for changed defaults, dead `...Colors` fields, mixed diffs, and new lines in literal-heavy files.
- [x] 8. User commits (`ai:` type, `XPL-445` last line, no `Co-Authored-By` trailer per XPL-442 convention): `ai: Add design-system-specialist agent`, then `ai: Route design-system work to design-system-specialist`. PR against `main` from `.github/pull_request_template.md`.

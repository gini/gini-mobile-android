# PP-3849: [Android] Support runtime color configuration via CustomResourceProvider

Status: implementing (9/10)
Ticket: https://ginis.atlassian.net/browse/PP-3849
Investigation: [PP-3513 wiki](https://ginis.atlassian.net/wiki/x/bQBGdQ) · related: PP-3513 (Done)

## Problem

Atruvia serves 25–30 banks from **one** Android app. Each bank has its own colors, which the app
loads from Atruvia's server after login. Today the Gini Bank SDK takes its colors only from
`colors.xml`, which is locked into the APK at build time. One APK means one `colors.xml`, so every
bank gets the same Gini colors.

Atruvia wants the same mechanism the iOS SDK already has (`CustomResourceProvider`, PP-427): when
they configure the SDK, they pass a provider. The SDK asks it for each palette color by name and
uses the answer on every screen, both XML and Compose. A Compose migration is **not** needed and
not part of this ticket.

## Requirements

Palette names: the **32** names listed in "Design → Palette names". They are the `colors.xml`
resource names (Android naming, e.g. `gc_accent_01`). `gc_accent_06` and `gc_camera_preview_shade`
are **not** provider-controlled.

- **R1 (MUST, entry):** Given an integrator who builds
  `CaptureConfiguration(..., customResourceProvider = provider)`, when they call
  `GiniBank.setCaptureConfiguration(context, config)`, then
  `GiniCapture.getInstance().getCustomResourceProvider()` returns that same `provider`.
- **R2 (MUST, entry):** Given a capture-only integrator, when they call
  `GiniCapture.newInstance(context).setCustomResourceProvider(provider).build()`, then
  `GiniCapture.getInstance().getCustomResourceProvider()` returns `provider`.
- **R3 (MUST, happy):** Given a provider that returns `0xFF00FF00` for `gc_dark_05` in light mode,
  when a screen whose layout uses `@color/gc_dark_05` (e.g. `gc_fragment_error.xml`) is shown in
  light mode, then that view's text color is `0xFF00FF00`.
- **R4 (MUST, happy):** Given a provider that returns `0xFF0000FF` for `gc_light_02` in light mode,
  when a view whose color comes from a theme attribute that resolves to `gc_light_02` (e.g.
  `?attr/backgroundColor`) is shown in light mode, then its background color is `0xFF0000FF`.
- **R5 (MUST, happy):** Given a provider that returns `0xFFFF00FF` for `gc_accent_01`, when any
  Compose screen wrapped in `GiniTheme { }` reads `GiniTheme.colorScheme`, then every scheme slot
  mapped from `accent01` equals `Color(0xFFFF00FF)`.
- **R6 (MUST, happy):** Given a provider that returns `X` for name `N` in dark mode and `Y` for `N`
  in light mode, when the device is in dark mode (`uiMode` night), then the SDK calls the provider
  with `isDarkMode = true` and views/Compose slots using `N` show `X`; in light mode they show `Y`.
- **R7 (MUST, happy):** Given a provider that returns `null` for name `N`, when a view or Compose
  slot uses `N`, then it shows exactly the value `context.getColor(R.color.N)` returns. This
  includes an integrator's own `colors.xml` override of `N`.
- **R8 (MUST, error):** Given a provider whose `customPreferredColor` throws an `Exception` for name
  `N`, when the SDK resolves `N`, then it uses `context.getColor(R.color.N)` (or the
  `GiniColorPrimitives` default for `gc_warning_05`), the screen opens normally, and one error is
  logged through the SDK's existing logger. The SDK does not rethrow. Decision: because the
  return type is `Int?` and every `Int` is a valid ARGB color, "invalid value" in the ticket AC
  means only `null` (R7) or an exception (R8). Every returned `Int` is a valid color and is used
  as-is, for example white, black, or fully transparent. The SDK never filters or changes a color,
  even when a combination has low contrast (e.g. white text on a white background). Contrast is
  the integrator's responsibility (R16).
- **R9 (MUST, error):** Given **no** provider is set, when any SDK screen is shown, then no
  runtime-color code changes any view (the appliers return immediately), so the screen is
  pixel-identical to today. Existing `colors.xml` overrides and `GiniCaptureTheme` style overrides
  keep working.
- **R10 (MUST, happy):** Given session 1 used provider P1, when the integrator calls
  `GiniBank.setCaptureConfiguration` again with provider P2 and starts the flow, then all screens
  show P2's colors, and no value cached from P1 is used.
- **R11 (MUST, happy):** The provider is asked **at most once per (name, isDarkMode)** per
  `GiniCapture` instance. A second lookup of the same pair returns the cached value without calling
  the provider.
- **R12 (MUST, happy):** Coverage: every screen, dialog, bottom sheet, popup and list item listed in
  "Design → Screen inventory" applies provider colors for all palette-color references in its
  layout(s), in all layout variants (`layout`, `layout-land`, `layout-sw600dp`,
  `layout-sw600dp-land`).
- **R13 (MUST, happy):** Hard cases: Material buttons (primary/outlined/unelevated, including the
  disabled state), switches (`gc_switch_*`, `gbs_switch_*` selectors), and the 15 drawables listed
  in "Design → Hard cases" show provider colors when a provider is set.
- **R14 (MUST, happy):** A provider is never asked for names outside the 32 palette names
  (`gc_accent_06`, `gc_camera_preview_shade` and any non-`gc_` resource always come from resources).
- **R15 (MUST, entry):** `bank-sdk:example-app` has a "Custom runtime colors" switch on the
  configuration screen. When it is on, the example app passes a provider with a high-contrast test
  palette (distinct light and dark values), so a missed view is visible.
- **R16 (SHOULD, entry):** Integration docs (Markdown) explain: setting the provider, the 32 names
  and the Android↔iOS name table, "load colors before starting the SDK; the provider must answer
  synchronously", and "color contrast/accessibility is the integrator's responsibility".

## Affected modules

- `capture-sdk:sdk`: the provider interface, `GiniCapture` builder/getter, the resolver, the Compose
  bridge, all capture screens.
- `bank-sdk:sdk`: the `CaptureConfiguration` field + forwarding, the bank-sdk XML screens, and the
  bank-sdk code-level color reads.
- `bank-sdk:example-app`: the toggle and test provider (R15).
- Not affected: `capture-sdk:default-network`, `bank-api-library:library`,
  `core-api-library:library` (no UI, no colors). Their versions bump only per RELEASE.md rules
  (`default-network` bumps together with `capture-sdk`), which is release work, not this ticket.

## Public API impact

All changes are **additive** (source-compatible). Both API dumps must be re-pinned with
`./gradlew capture-sdk:sdk:apiDump bank-sdk:sdk:apiDump`, or `apiCheck` fails.

| Module | Declaration | Change |
|---|---|---|
| `capture-sdk:sdk` | `net.gini.android.capture.ui.theme.colors.CustomResourceProvider` (new `fun interface`) | Added |
| `capture-sdk:sdk` | `GiniCapture.Builder.setCustomResourceProvider(CustomResourceProvider)` | Added |
| `capture-sdk:sdk` | `GiniCapture.getCustomResourceProvider(): CustomResourceProvider?` (`@Nullable`) | Added |
| `bank-sdk:sdk` | `CaptureConfiguration.customResourceProvider: CustomResourceProvider? = null`, added as the **last** constructor parameter | Added. The data-class constructor and `copy` signatures change in the dump. This is source-compatible for named and positional Kotlin callers, the same as when `giniComposableStyleProvider` was added. |
| `capture-sdk:sdk` | `net.gini.android.capture.internal.ui.runtimecolors.RuntimeColors` (top-level helpers: `runtimeColorsThemedInflaterOrNull`, `runtimeColorsThemedContextOrNull`, `giniColor`, `giniColorFromAttr`, `giniDrawable`, `setGiniBackgroundResource`) and `GiniCapture.Internal.getColorResolver()` | Added. Public in the `internal` package so bank-sdk can use them (existing convention). They appear in the dump but are not integrator API (`@suppress`). Everything else, including `GiniColorResolver`, is Kotlin `internal`. |
| `capture-sdk:sdk` | `GiniTheme(...)` composable | **Unchanged.** The provider is read internally, so there is no new parameter and the public signature stays stable. |

## Technical conventions

1. **Language:** all new code is Kotlin and `internal`, except the public declarations above
   (the three integrator APIs, plus the `internal`-package helpers bank-sdk must reach).
   Legacy Java files may only be touched as follows:
   - `GiniCapture.java`: add the builder field, setter, getter, and the resolver field plus an
     internal accessor. Required, because the builder lives here.
   - `CameraFragmentImpl.java`, `MultiPageReviewFragment.java`, `OnboardingFragment.java`: swap
     code-level color/drawable reads to the resolver helpers (one call each). No other logic.
     No conversion of Java to Kotlin.
2. **UI:** no XML layouts are added or removed, and existing XML color references stay as the
   compile-time defaults. No new Compose UI; the existing `GiniTheme` gets an internal color source.
   Light/dark `@Preview`s are unchanged.
3. **Architecture:** no ViewModel/state changes. Runtime colors are a view-layer concern, applied
   **while XML is inflated** (decision 2026-10-08: "Option B" for XML, see Design). No new
   MVVM/MVI classes and no per-screen color code. Legacy MVP presenters are not touched.
4. **DI / async:** no Koin. The resolver is owned by the `GiniCapture` instance and reached through
   `GiniCapture.getInstance().internal().getColorResolver()` (`GiniCapture.Internal`,
   [GiniCapture.java:1595](../capture-sdk/sdk/src/main/java/net/gini/android/capture/GiniCapture.java)).
   Everything is synchronous on the main thread; no coroutines and no flows.
5. **Strings/resources:** no SDK strings or resources. The example app gets one new switch label in
   `bank-sdk/example-app/src/main/res/values/strings.xml` (no other locale folder exists for
   example-app strings; `/gini-build` must confirm before adding).
6. **Quality gates:** `testDebugUnitTest`, `lint`, `detekt`, `ktlintCheck` must pass for
   `capture-sdk:sdk` and `bank-sdk:sdk` (`/gini-check`), plus `apiCheck` after re-pinning the dumps.
   Every new Kotlin class gets a unit test. Jacoco/Sonar should cover the resolver and the
   primitives builder fully. The appliers are covered by the Robolectric screen tests.
7. **Exceptions:** the provider is integrator code, so the resolver catches `Exception` (never
   `Throwable`, never `CancellationException`) around the single provider call, and logs it.

## Design

### Palette names (32)

`gc_accent_01..05`, `gc_dark_01..06`, `gc_light_01..06`, `gc_success_01..05`, `gc_error_01..05`,
`gc_warning_01..05`.

These are exactly the 32 fields of
[GiniColorPrimirives.kt](../capture-sdk/sdk/src/main/java/net/gini/android/capture/ui/theme/colors/GiniColorPrimirives.kt).
Note: `gc_warning_05` has **no** `colors.xml` resource. It exists only as the Compose default
`0xFFA17503` (`warning05`), so its fallback is that default, not a resource. 31 names have a
resource in
[colors.xml](../capture-sdk/sdk/src/main/res/values/colors.xml). `values-night/colors.xml`
overrides 3 of them (`gc_accent_01`, `gc_dark_05`, `gc_error_02`).

### Data flow

```
Atruvia app ── CaptureConfiguration(customResourceProvider = p)
   └─ GiniBank.setCaptureConfiguration ──(Configuration.kt:307 forwarding)──► GiniCapture.Builder.setCustomResourceProvider(p)
         └─ GiniCapture (new instance per session) owns GiniColorResolver(p)  ← fresh cache per session (R10)
               ├─ Compose: GiniTheme → resolver.primitives(context, darkMode) → GiniColorScheme (R5)
               ├─ Code:    resolver.color(context, R.color.gc_x)                              (R3, R7)
               └─ XML:     XxxRuntimeColors.apply(rootView) → setTextColor / setBackgroundColor / tints
```

### New and changed classes

- **`CustomResourceProvider`** (public, `capture-sdk:sdk`, package
  `net.gini.android.capture.ui.theme.colors`):
  ```kotlin
  fun interface CustomResourceProvider {
      /** @return ARGB color for palette [name] in the given mode, or null to keep the Gini default. */
      fun customPreferredColor(name: String, isDarkMode: Boolean): Int?
  }
  ```
  KDoc lists the 32 names, the "answer synchronously from a cache" rule, and the contrast
  disclaimer. The name matches iOS `CustomResourceProvider`. The method fixes the iOS typo
  (`customPrefferedColor`) and adds `isDarkMode`, because Android has no dynamic `UIColor`.
- **`GiniCapture.java`:** builder field + `setCustomResourceProvider` (pattern: lines 1563–1569,
  `setGiniComposableStyleProvider`), instance field + `getCustomResourceProvider()` (pattern: line
  916), and an `internal` `GiniColorResolver` created in the constructor next to line 510.
- **`GiniColorResolver`** (`capture-sdk/.../internal/ui/runtimecolors/GiniColorResolver.kt`, Kotlin `internal`; bank-sdk only uses the `RuntimeColors.kt` helpers):
  - `val isActive: Boolean`: true when a provider is set.
  - `fun color(context: Context, @ColorRes res: Int): Int`: gets the name from
    `resources.getResourceEntryName(res)`. If the name is a palette name and `isActive`, it returns
    the cached/provider value. Otherwise it returns `context.getColor(res)`.
  - `fun colorFromAttr(context: Context, @AttrRes attr: Int): Int?`: calls
    `theme.resolveAttribute(attr, tv, true)`. If `tv.resourceId` names a palette color, it returns
    `color(context, tv.resourceId)`; otherwise `null`. This keeps the existing light/dark slot
    mapping of the themes (e.g. `backgroundColor` → `gc_light_02` in light, `gc_dark_01` in night).
    (confidence: LOW — that `tv.resourceId` holds the final `@color` id for an `?attr` chain is
    standard Android behavior but not yet proven in this repo; the
    `GiniColorResolverTest` Robolectric case for `?attr/backgroundColor` in `night` confirms it.)
  - `fun primitives(context: Context, isDarkMode: Boolean): GiniColorPrimitives`: builds all 32
    fields, each from the provider or its fallback.
  - Dark mode comes from `ContextHelper.isDarkTheme(context)`
    ([ContextHelper.java:63](../capture-sdk/sdk/src/main/java/net/gini/android/capture/internal/util/ContextHelper.java)).
  - Cache: a `HashMap<Pair<String, Boolean>, Int?>` per resolver instance (R11). A new
    `GiniCapture` instance makes a new resolver (R10).
- **`GiniTheme.kt`**
  ([GiniTheme.kt:27](../capture-sdk/sdk/src/main/java/net/gini/android/capture/ui/theme/GiniTheme.kt)):
  `remember(resolver, darkMode) { if (resolver?.isActive == true) resolver.primitives(context, darkMode) else buildColorPrimitivesBasedOnResources(context) }`.
  The public signature is unchanged. This covers all `GiniTheme { }` call sites (e.g.
  `SkontoFragment.kt:73`, `CaptureFlowFragment.kt:289`, `DigitalInvoiceFragment.kt:353`,
  `TransactionDocsView.kt:31`, `AnalysisFragmentExtension.kt:69`,
  `QrCodeEducationPopupContent.kt:34`) with no call-site edits.
- **`CaptureConfiguration`** ([Configuration.kt:25](../bank-sdk/sdk/src/main/java/net/gini/android/bank/sdk/capture/Configuration.kt)):
  new last parameter `customResourceProvider`, forwarded next to line 307:
  `configuration.customResourceProvider?.let { setCustomResourceProvider(it) }`.
- **Decision (2026-10-08): XML screens are repainted while they are inflated ("Option B"),
  not by per-screen code.** An inventory found 99 distinct palette references in 34 layouts,
  using only 5 attributes (`android:background`, `android:textColor`, `app:dividerColor`,
  `app:tint`, `app:navigationIconTint`). The 4 layout variants differ in places (e.g.
  `gc_fragment_error.xml` subtitle: `?attr/gcErrorScreenSubtitle` vs `@color/gc_dark_05`), and
  2 non-root views have no id. Reading each view's own XML attributes at inflation handles all of
  this without ids and without per-variant code.
- **Shared code across modules:** Kotlin `internal` does not cross Gradle modules, so what
  bank-sdk needs follows the existing pattern: **public Kotlin declarations in the
  `net.gini.android.capture.internal.*` package**, which bank-sdk already imports from (e.g.
  `internal.util.ContextHelper`, `internal.provider.GiniBankConfigurationProvider`), with
  `@suppress` KDoc ("Internal use only"). Everything else stays Kotlin `internal`.
- **`RuntimeColors`** (`internal/ui/runtimecolors/RuntimeColors.kt`, public object):
  `themedContext(base, themeRes)` and `themedInflater(inflater, base, themeRes)`. **When the
  resolver is inactive they return exactly what the code returns today** (`ContextThemeWrapper` /
  `inflater.cloneInContext(...)`), so R9 holds by construction. When active they return a
  `RuntimeColorsContextThemeWrapper` and a `RuntimeColorsLayoutInflater`. Installed in the 4
  places that theme SDK UI today:
  [capture FragmentExtensions.kt:8](../capture-sdk/sdk/src/main/java/net/gini/android/capture/internal/util/FragmentExtensions.kt),
  [bank ContextExtensions.kt:7](../bank-sdk/sdk/src/main/java/net/gini/android/bank/sdk/util/ContextExtensions.kt)
  (used by bank `FragmentExtensions.kt`),
  [CaptureFlowFragment.kt:118](../bank-sdk/sdk/src/main/java/net/gini/android/bank/sdk/capture/CaptureFlowFragment.kt),
  [QRCodePopup.kt:140](../capture-sdk/sdk/src/main/java/net/gini/android/capture/internal/camera/view/QRCodePopup.kt).
- **`RuntimeColorsContextThemeWrapper`** (internal): an `androidx.appcompat.view.ContextThemeWrapper`
  whose `getSystemService(LAYOUT_INFLATER_SERVICE)` returns a `RuntimeColorsLayoutInflater`. So
  `LayoutInflater.from(parent.context)` in RecyclerView adapters, `ViewStub`s, `<include>`s, and
  Snackbars created on SDK views all go through the checker.
- **`RuntimeColorsLayoutInflater`** (internal, `LayoutInflater` subclass): its `Factory2` wraps the
  factory it inherits (AppCompat/Material/fragment). For every tag it creates the view with that
  factory, or with the same reflection `LayoutInflater` uses (constructor `(Context, AttributeSet)`,
  `android.widget.`/`android.webkit.`/`android.app.`/`android.view.` prefixes; a `ViewStub` gets this
  inflater). It then calls `RuntimeColorsViewStyler.apply(view, attrs)`. If construction fails, it
  returns `null`, so `LayoutInflater` falls back to its normal path (no crash, view just unstyled).
- **`RuntimeColorsViewStyler`** (internal): reads `obtainStyledAttributes(attrs, …, defStyleAttr)`
  (the layout attrs, `style="…"`, and the widget's default style for `MaterialButton`,
  `MaterialSwitch`, `MaterialToolbar`, `MaterialDivider`, Snackbar views). Supported attributes:
  `textColor` (also from `textAppearance`), `background` (color, or a drawable via
  `RuntimeDrawables`), `backgroundTint`, `strokeColor`, `dividerColor`, `tint`,
  `navigationIconTint`, `thumbTint`, `trackTint`, `trackDecorationTint`. A value is changed only
  when it references a palette color (directly, through `?attr`, or inside a selector).
- **`RuntimeColorStateLists`** (internal): rebuilds a `<selector>` color resource with palette
  colors replaced, keeping its states and `alpha` (the same rules as `ColorStateList.inflate`).
  Returns `null` (leave the view alone) when no item references the palette or an item uses
  `lStar`.
- **`RuntimeDrawables`** (internal): for a drawable resource whose XML is `<shape>`,
  `<layer-list>` or `<selector>`, it sets `<solid>`/`<stroke>` palette colors on the mutated
  `GradientDrawable`s (stroke width read from the XML). Other drawables are left unchanged.
- **Code helpers** (`RuntimeColorExtensions.kt`, public in the internal package):
  `Context.giniColor(@ColorRes)` (resolver if active, else `getColor`) and
  `View.setGiniBackgroundResource(@DrawableRes)` (`setBackgroundResource` + `RuntimeDrawables`).
  Used for the code-level reads below.

### Screen inventory (R12)

All of these are inflated through the `RuntimeColorsLayoutInflater`, so they need no per-screen
code. The table is the coverage checklist for visual QA and for the screen tests.

| Module | Host (call site) | Layout(s) |
|---|---|---|
| capture | `CameraFragment.java` / `CameraFragmentImpl.java` | `gc_fragment_camera`, `gc_layout_camera_no_permission`, `gc_detection_error_layout`, `gc_navigation_bar_top` |
| capture | `QRCodePopup.kt` | QR popup views (+ its 6 code reads) |
| capture | `AnalysisFragment.java` | `gc_fragment_analysis` |
| capture | `MultiPageReviewFragment.java` + its preview adapter | `gc_fragment_multi_page_review`, `gc_item_multi_page_preview` |
| capture | `ZoomInPreviewFragment.java` | `gc_fragment_zoom_in_preview` |
| capture | `OnboardingFragment.java`, `OnboardingPageFragment.java` | `gc_fragment_onboarding`, `gc_fragment_onboarding_page` |
| capture | `HelpFragment.kt` + adapter | `gc_fragment_help`, `gc_item_help` |
| capture | `PhotoTipsHelpFragment.kt` + adapter | `gc_fragment_photo_tips_help`, `gc_item_tip` |
| capture | `SupportedFormatsHelpFragment.kt` + adapter | `gc_fragment_supported_formats_help`, `gc_item_format_header`, `gc_item_format_info` |
| capture | `FileImportHelpFragment.kt` | `gc_fragment_file_import_help` |
| capture | `ErrorFragment.kt` | `gc_fragment_error` |
| capture | `NoResultsFragment.java` | `gc_fragment_noresults` |
| capture | `FileChooserFragment.kt` + adapter | `gc_fragment_file_chooser`, `gc_item_file_provider_app`, `gc_item_file_provider_separator` |
| capture | `WarningBottomSheet.kt` | `gc_warning_bottom_sheet` |
| capture | `AlertDialogFragment.java` | (dialog buttons/text) |
| bank | `DigitalInvoiceFragment.kt` + `LineItemsAdapter.kt` | `gbs_fragment_digital_invoice`, `gbs_item_digital_invoice_line_item`, `gbs_item_digital_invoice_addon`, `gbs_item_digital_invoice_skonto` (+ 15 code reads in the adapter, 4 in the fragment) |
| bank | `DigitalInvoiceBottomSheet.kt` | `gbs_edit_item_bottom_sheet`, `gbs_item_currency_dropdown` (+ 7 code reads) |
| bank | `DigitalInvoiceOnboardingFragment.kt` | `gbs_fragment_digital_invoice_onboarding` |
| bank | `DigitalInvoiceHelpFragment.kt` + adapter | `gbs_fragment_digital_invoice_help`, `gbs_item_help` |
| both | `CameraActivity.java`, `CaptureFlowActivity.kt` | window background |

Code-level reads to switch to `giniColor(...)` (about 40): `LineItemsAdapter.kt`,
`DigitalInvoiceBottomSheet.kt`, `DigitalInvoiceFragment.kt`, `QRCodePopup.kt`,
`CameraFragmentImpl.java`, `QrCodeEducationPopupContent.kt`, `OnButtonLoadingIndicatorAdapter.kt`,
`CustomLoadingIndicatorAdapter.kt`. Drawables set in code: `gc_bg_off/on_save_invoices_locally`
(`MultiPageReviewFragment.java:489`) and `gc_onboarding_page_indicator`
(`OnboardingFragment.java:288`).

### Hard cases (R13)

Handled by the generic styler, with no per-widget code:
- **Material buttons:** default style (`materialButtonStyle`) and `Root.GiniCaptureTheme.Widget.Button.*`
  selectors reference `?attr/colorPrimary`/`colorOnPrimary`/`colorOnSurface` → rebuilt by
  `RuntimeColorStateLists`, including the disabled state and its alpha.
- **Switches:** `gc_switch_{outline,thumb,track}_color.xml`, `gbs_switch_{thumb,track}_color.xml`
  → rebuilt by `RuntimeColorStateLists`.
- **Drawables used as XML backgrounds** (13): `gbs_success_01_rectangle_rounded_4dp`,
  `gbs_top_corners_radius`, `gc_analysis_tips_background`, `gc_bg_on_save_invoices_locally`,
  `gc_bg_warning_circle`, `gc_detection_error_background`, `gc_image_preview_rectangle`,
  `gc_on_device_iban_detected_background`, `gc_photo_thumbnail_badge_background`,
  `gc_qr_code_detected_background`, `gc_qr_code_warning_background` → `RuntimeDrawables`.
- **Known limitation:** the multi-page review page dots (`gc_tab_selector` →
  `gc_selected_dot`/`gc_unselected_dot`, set through `TabLayout`'s `app:tabBackground`) keep the
  theme's `colorOnBackground`. `TabLayout` draws them from a private field
  (`TabView.baseBackgroundDrawable`) that neither the inflater nor a public API reaches; the only
  options were reflection or drawing a second background, both rejected.
- **Snackbar:** its views are inflated from the SDK view's context → styled with `snackbarStyle`.

Needs a code change (set in code, never inflated): `gc_bg_off_save_invoices_locally`,
`gc_onboarding_page_indicator`, `gc_qr_code_detected_background` (`QRCodePopup.kt:123`), and the
activity window backgrounds of
`CameraActivity`/`CaptureFlowActivity` if the root views don't already cover them (checked in
visual QA).

### Example app (R15)

- `ExampleAppBankConfiguration.isCustomRuntimeColorsEnabled: Boolean = false` (next to
  `isCustomPrimaryComposeButtonEnabled`, line 108).
- A switch in `ConfigurationActivity.kt` (pattern: lines 194 and 501).
- `ConfigurationViewModel.kt` (pattern: line 266):
  `result = result.copy(customResourceProvider = ExampleRuntimeColorsProvider())`.
- `ExampleRuntimeColorsProvider` returns a distinct high-contrast palette for light and dark.

## Test plan

Stack: JUnit4, Robolectric, Google Truth (capture-sdk) / JUnit `Assert` + MockK (bank-sdk,
matching `CaptureConfigurationTest`), `androidx.fragment.testing` `launchFragmentInContainer`
(precedent: `capture-sdk/sdk/src/test/.../help/HelpFragmentTest.kt`), and the Compose UI test rule
in bank-sdk (`compose.ui.test.junit4` is already a test dependency).

| Test class | New / extend | Covers | ~Tests |
|---|---|---|---|
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/ui/theme/colors/GiniColorResolverTest.kt` | New (Robolectric) | R3 (via `color()`), R4 (`colorFromAttr` light), R6 (`@Config(qualifiers = "night")`: `isDarkMode=true` passed and the night attr maps to `gc_dark_01`), R7, R8 (throwing provider → resource value), R9 (`isActive=false`), R11 (provider called once per pair), R14 (`gc_accent_06` → provider never called) | 10 |
| `.../ui/theme/colors/GiniColorPrimitivesFromResolverTest.kt` | New | `primitives()` maps all 32 names to the right fields; `gc_warning_05` falls back to `0xFFA17503` | 4 |
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/GiniCaptureCustomResourceProviderTest.kt` | New (Robolectric) | R2; R10 (new instance → new resolver, P1 value not returned) | 3 |
| `bank-sdk/sdk/src/test/java/net/gini/android/bank/sdk/capture/CaptureConfigurationTest.kt` | **Extend** (5 → 7) | R1 (forwarded), default `null` | 2 |
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/help/HelpFragmentRuntimeColorsTest.kt` | New (`launchFragmentInContainer`) | R3/R4 + R12 through a real fragment (`onGetLayoutInflater` wiring) and its RecyclerView items; R9 (no provider → resource color). Replaces the planned Error/NoResults fragment tests: both need a `Document`/listeners to start, and their layouts (incl. the id-less stripe) are covered in `RuntimeColorsLayoutInflaterTest`; Java and Kotlin fragments call the same `getLayoutInflaterWithGiniCaptureTheme`. | 2 |
| `bank-sdk/sdk/src/test/java/net/gini/android/bank/sdk/capture/digitalinvoice/DigitalInvoiceHelpRuntimeColorsTest.kt` | New | R12 + R9 for bank-sdk XML through `wrappedWithGiniCaptureTheme()` (no `fragment-testing` dependency in bank-sdk) | 2 |
| `capture-sdk/.../internal/ui/runtimecolors/RuntimeColorsTest.kt` | New (Robolectric) | Code helpers `giniColor`, `giniColorFromAttr`, `giniDrawable`, `setGiniBackgroundResource`; R9 for each (no provider = plain Android call) | 3 |
| `bank-sdk/sdk/src/test/java/net/gini/android/bank/sdk/GiniThemeRuntimeColorsTest.kt` | New (Compose rule) | R5 (scheme slot equals provider color), R7 for Compose | 2 |
| `capture-sdk/.../internal/ui/runtimecolors/RuntimeColorStateListsTest.kt` | New (Robolectric) | R13: switch selectors and the Material button background selector rebuilt with provider colors, states and disabled alpha kept; `null` when no palette item | 4 |
| `capture-sdk/.../internal/ui/runtimecolors/RuntimeDrawablesTest.kt` | New (Robolectric) | R13: `<shape>` solid + stroke, `<layer-list>`, non-palette drawable untouched | 3 |
| `capture-sdk/.../internal/ui/runtimecolors/RuntimeColorsLayoutInflaterTest.kt` | New (Robolectric) | R3/R4 at inflation (textColor, `?attr` background), R9 (inactive → plain `cloneInContext`), R12 (`LayoutInflater.from(themedContext)` and `ViewStub` also styled), unknown tag falls back without crash | 6 |

Every new Kotlin class gets a unit test: the resolver, the primitives builder, the inflater,
the styler (through the inflater and screen tests), `RuntimeColorStateLists` and `RuntimeDrawables`.

### Not tested

- Pixel-perfect rendering of every screen and every layout variant. Covered by **manual visual
  QA** with the example-app toggle (R15): light and dark, phone and tablet, portrait and landscape.
  It is also the R9 "no provider = identical" check, done side by side against `main`.
- Material Components' internal drawing (ripple, cursor). This is framework code.
- Colors inside vector illustrations (out of scope).
- Device manual QA in the RC (after Done, per the team's estimation agreement).

### Accessibility review notes (a11y-specialist, 2026-10-08) — input for the docs (R16)

- Contrast pairs integrators must check (team target WCAG AAA, 7:1 for normal text):
  `light_01` on `accent_01` (primary button), `dark_02` on `light_02` and on `light_01`,
  `light_01` on `dark_01`, `light_02` on `dark_02`, `light_01` on `success_05` (QR badge),
  `dark_05` as secondary text; `light_03` strokes (buttons, cards) need 3:1.
- `error_*`, `warning_*`, `success_*` carry meaning: keep them recognizable.
- Use opaque colors (disabled states multiply alpha) and give values for both `isDarkMode` cases.
- Not customizable: ripple/focus highlight (`gc_accent_06`), cursor, hint colors, and Material
  internals. These can sit next to provider colors ("mixed pairs") and must be checked in
  visual QA.
- Do not claim the defaults are AAA (`#006ECF` on white is about 5:1).
- Follow-up idea (not in scope): a debug-only contrast warning, gated on the host app's
  `FLAG_DEBUGGABLE`, that only logs.
- The suggested `GiniTheme` `remember` re-key without a provider was **not** applied: before this
  change, `remember {}` had no keys either, so the no-provider path is unchanged (R9).

### Device QA (Samsung Galaxy S22, 2026-10-08) — example-app test palette, light + dark

Checked with pixel sampling against both palettes. Applied correctly: camera, help, photo tips,
supported formats, file import help, file chooser, analysis, review (frame, switch on/off, save
wrapper), error, no results, onboarding (incl. code-set page dots), camera no-permission
(`ViewStub`), digital invoice + onboarding + help + edit bottom sheet, attach dialog (Compose),
Skonto + Skonto help (Compose), credit note hint (`WarningBottomSheet`).

Misses found on the device and fixed:
- **Snackbar** (`FileImportHelpFragment`): `Snackbar.make` attaches to the activity content view,
  so it was not inflated by the runtime-colors inflater → `Snackbar.applyGiniRuntimeColors()`
  reads `snackbarStyle`/`snackbarTextViewStyle`/`snackbarButtonStyle` (style overrides still win).
- **Text cursor / selection handles** stayed Gini blue (`colorControlActivated`) → tinted on
  API 29+ in `RuntimeColorsViewStyler.applyTextCursor`.
- **Invoice preview** (Skonto / transaction docs) used `GiniColorPrimitives()` hardcoded defaults →
  `giniFixedColorPrimitives()`: provider light-mode colors with a provider, exactly the old
  defaults without one (R9).

Remaining, by design / out of scope:
- **Icons with hardcoded hex inside vector drawables keep Gini colors**, including status icons
  with meaning: supported/unsupported format icons (`#13822F`/`#DD0000`), error/no-results alert
  triangle (`#DD0000`), credit-note warning icon (`#F6EEEE`/`#FF3B30`, circle baked in), and
  Gini-blue (`#006ECF`) icons in 13 files. Replaceable illustrations go through
  `InjectedViewAdapter`s; plain icons have no runtime path today. **Decision needed** (see Open
  questions).
- Status bar strip `#FEF7FF` is Material 3's default window background — identical without a
  provider (verified), not a palette color.
- Multi-page review page dots (see Hard cases, known limitation).

### No-provider regression check (Galaxy S22, Android 16, 2026-10-09)

- `capture-sdk:sdk:connectedCheck`: **87/87 passed** (API 36 locally; CI uses API 33).
- `bank-sdk:sdk:connectedCheck`: **not run** — needs Gini API test credentials
  (`bank-sdk/sdk/local.properties`), which are not on this machine. Left to CI.
- Fresh install, data cleared, no switches touched (= a customer without a provider): onboarding,
  camera, help + tips + formats + import help (incl. snackbar `#313131`/`#93C4F9`), file chooser,
  digital invoice + onboarding + edit sheet (cursor stays Gini blue), Skonto, invoice preview
  (`#000000`), credit-note hint, error, no results — all measured as the unchanged Gini defaults.
  Review screen not captured in this pass (the photo flow moved on too fast); it uses the same
  unchanged inflater path.

## Out of scope

- Changing colors while an SDK screen is visible (live switch).
- Per-element/per-screen color keys (PP-2078/PP-2079 style). They can be added later as an optional
  method on the same provider.
- `gc_accent_06`, `gc_camera_preview_shade`, hardcoded hex in vector drawables, images/icons
  (already replaceable through `InjectedViewAdapter`s).
- iOS changes (separate ticket), and the Compose migration (PP-3503).
- Version bumps and release notes (release workflow).
- Any change to XML layouts' compile-time color references or to the `GiniTheme` public signature.

## Open questions

1. Where do the Android integration docs live for capture-sdk/bank-sdk (R16)? No Sphinx `src/doc`
   source exists in `capture-sdk/sdk` or `bank-sdk/sdk` in this repo. The plan is to draft the
   section as Markdown (e.g. with `/gini-doc`) for the public Bank SDK Confluence page and
   publish it manually.
2. (confidence: LOW) `TypedValue.resourceId` after `resolveAttribute(..., true)` returns the final
   `@color` id for `?attr` chains. This is proven by `GiniColorResolverTest` first. If it is false,
   appliers map attrs to palette names explicitly per light/dark theme.

## Implementation plan
- [x] 1. capture-sdk: `CustomResourceProvider` (public) + `GiniColorResolver` (palette names, cache, `color`, `colorFromAttr`, `primitives`, error handling) with `GiniColorResolverTest` + `GiniColorPrimitivesFromResolverTest` (R3, R4, R6, R7, R8, R11, R14; resolves open question 2)
- [x] 2. capture-sdk: `GiniCapture.java` builder setter/getter + resolver owned by the instance, `GiniColorResolver.current()`; `GiniCaptureCustomResourceProviderTest` (R2, R10)
- [x] 3. bank-sdk: `CaptureConfiguration.customResourceProvider` + forwarding; extend `CaptureConfigurationTest` (R1)
- [x] 4. capture-sdk: `GiniTheme` reads primitives from the resolver (keyed `remember`); `GiniThemeRuntimeColorsTest` in bank-sdk (R5, R7)
- [x] 5. capture-sdk: `RuntimeColorStateLists` + `RuntimeDrawables` with `RuntimeColorStateListsTest`, `RuntimeDrawablesTest` (R13)
- [x] 6. capture-sdk: `RuntimeColorsLayoutInflater`, `RuntimeColorsContextThemeWrapper`, `RuntimeColorsViewStyler`, `RuntimeColors` + code helpers; `RuntimeColorsLayoutInflaterTest` (R3, R4, R9, R12, R13)
- [x] 7. Install in capture `FragmentExtensions`, bank `ContextExtensions`, `CaptureFlowFragment`, `QRCodePopup`; `HelpFragmentRuntimeColorsTest`, `DigitalInvoiceHelpRuntimeColorsTest` (R3, R4, R9, R12)
- [x] 8. Code-level reads → `giniColor` (8 files) and code-set drawables (`MultiPageReviewFragment`, `OnboardingFragment`) (R12, R13)
- [x] 9. bank-sdk:example-app: "Custom runtime colors" switch + `ExampleRuntimeColorsProvider` (R15)
- [ ] 10. `apiDump` for capture-sdk:sdk and bank-sdk:sdk, then `/gini-check`; visual-QA notes (incl. window backgrounds); docs draft (R16) at the very end — **done:** apiDump, full `/gini-check` green (2026-10-08). **Open:** visual QA on an emulator (incl. window backgrounds), docs draft (R16, deferred to the end by decision).
3. **Hardcoded-hex vector icons** (status icons included) stay Gini colors with a provider.
   Options: (a) accept and document; (b) follow-up ticket: make single-color icons neutral and
   color them with `app:tint="@color/gc_*"` in the layout (the checker already repaints `tint`);
   split multi-color icons (e.g. the credit-note badge) into a palette `<shape>` plus a
   single-color tinted glyph. Note: `@color` references *inside* a vector would only follow
   `colors.xml` overrides, not the runtime provider — Android has no public API to recolor
   single vector paths at runtime.


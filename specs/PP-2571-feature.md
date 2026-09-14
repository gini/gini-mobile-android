# PP-2571: Ingredient Brand — "Powered by Gini" badge on the Analysis screen

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-2571
Epic: PP-2568 · Blocked by: PP-2569 (design spec, In Progress), PP-2572 (backend flag, To Do)
Design source: Figma `35002:11096` ("Analysis Screen/Android", tagged *Dev. Ready For Dev*) —
the approved Android spec. The badge is the component `Powered by Gini` (`35631:1800`); the
Analysis frames are `6.1.1 android-ph-analyzePhoto` (`35002:11407`),
`6.2.1 android-ph-analyzePDF` (`35002:11497`), `5.1.x android-phP-QReng-analysis-hint` and the
landscape `4.1.x android-phL-Capture-analyze`.

## Problem

Banks that license the Photo Payment SDK can be required to show that Gini powers the
scanning flow ("ingredient branding"). Today the Android SDK has no such element: the
Analysis screen shows only the loading indicator, the analysis message and the tips card.

Gini needs the brand element to be switchable **per client and per screen from the
backend**, with no integrator-facing setting — a bank must not be able to turn it off in
code, and clients without ingredient branding must see no change at all. This ticket
covers the first screen: **Analysis**.

## Requirements

**R1 (MUST, entry)** — Given an integrator that runs the SDK through `GiniCaptureFragment`
with a network service that implements `getConfiguration` (the default is
`GiniCaptureDefaultNetworkService`), when the `/configurations` response contains
`ingredientBrandScreens` with an entry matching `"Analysis"`, then the Analysis screen
shows the Powered by Gini badge. There is **no** `GiniCapture.Builder` flag and no other
public entry point — the backend response is the only switch.

**R2 (MUST, happy)** — Given `ingredientBrandScreens = ["Analysis"]`, when the Analysis
screen is started, then a badge is visible that is horizontally centred in `gc_layout_root`,
sits `16dp` above the bottom of `gc_layout_root`, and renders the text "Powered by" followed
`3dp` later by the `ic_gini_logo` wordmark at `24 × 17dp`, on a white pill with an `8dp`
corner radius and `6dp` horizontal / `3dp` vertical padding. The label is `10sp` with
`-0.05em` letter spacing in `#3A3A3C` (Figma component `35631:1800`; the approved frames place
the badge `15.7dp` above the frame bottom in both portrait `360 × 800` and landscape
`800 × 360`).

**R3 (MUST, happy)** — Given `ingredientBrandScreens` is empty, `null`, or absent from the
`/configurations` JSON, when the Analysis screen is started, then the badge view is `GONE`
and the tips card `gc_analysis_hint_container` keeps exactly the bottom margin it has today
(`gc_large_32` in `layout/` and `layout-sw600dp-land/`,
`gc_analysis_hint_container_bottom_margin_portrait` in `layout-sw600dp/`) — the screen is
unchanged from the current release.

**R4 (MUST, error)** — Given `ingredientBrandScreens` contains only names the SDK does not
know, e.g. `["Camera", "Review", ""]`, when the Analysis screen is started, then the badge
is `GONE`, nothing is logged as an error and no exception is thrown. Unknown entries are
ignored, never treated as "show everywhere".

**R5 (MUST, happy)** — Given the entry differs only in letter case, e.g.
`["analysis"]` or `["ANALYSIS"]`, when the Analysis screen is started, then the badge is
visible. Matching is case-insensitive.

**R6 (MUST, async)** — Given the `/configurations` request has not completed yet, or
failed, when the Analysis screen is started, then badge visibility is read from the value
last persisted in `ClientConfigurationStorage`; if nothing was ever persisted the badge is
`GONE`. No placeholder, spinner or reserved empty space is shown while the request is in
flight, and the badge is never re-hidden once the screen has decided.

**R7 (MUST, async)** — Given `ingredientBrandScreens = ["Analysis"]` was received in a
previous SDK session, when the app is killed and the SDK is started again offline, then the
badge is visible on the Analysis screen — the value survives process death, exactly like
the boolean flags already do through DataStore.

**R8 (MUST, happy)** — Given the badge is visible, when the device is in dark mode, then the
pill stays white and the label stays `#3A3A3C`, and the `ic_gini_logo` wordmark keeps the Gini
brand blue `#009EDC`. The whole badge is theme-independent: the approved section reuses the
same component instance unchanged on the dark camera-preview screens
(`5.2.x android-phP-QReng-camera-hint`, `2.3.x android-ph-QrDetected`), so neither the fill
nor the label may follow `values-night`.

**R9 (MUST, happy)** — Given the badge is visible, when the screen is shown on a phone, a
tablet in portrait or a tablet in landscape, then the badge appears in every layout variant of
both covered screens, positioned as in R2: `gc_fragment_analysis.xml` in `layout/`,
`layout-sw600dp/` and `layout-sw600dp-land/` (phone landscape reuses `layout/`, there is no
`layout-land` variant), and `gc_fragment_camera.xml` in `layout/`, `layout-land/`,
`layout-sw600dp/` and `layout-sw600dp-land/`.

**R12 (MUST, entry)** — Given `ingredientBrandScreens` contains `"Analysis"`, when a QR code has
been detected and the SDK enters the QR-code analysis step, then the badge is visible `16dp`
above the injected bottom navigation bar — or above the screen bottom when no bottom bar is
injected — and horizontally centred. That step has **two mutually exclusive halves**, and the
badge appears in both:

- **invoice retrieval** — loading indicator plus the "retrieving invoice" label over a dimmed
  preview (`QRCodePopup`);
- **QR-code education** — the education message with "analysieren…" over a dimmed preview
  (`QRCodeEducationPopup`), shown instead of retrieval when the education feature applies
  (Figma `35002:11361`, `35002:11384`).

Both are governed by the same `IngredientBrandScreen.ANALYSIS` entry; there is no separate
backend screen name.

**R13 (MUST, happy)** — Given the camera screen is showing the live preview with the shutter
usable, then the badge is **not** visible. It appears only for the QR-code loading state of R12
and disappears with it. The live camera is not an analysis step.

**R10 (MUST, happy)** — Given the badge is visible, when TalkBack is on, then the badge is a
single screen-reader node announced as "Powered by Gini", neither the label nor the wordmark is
separately announced, the badge is not in the keyboard/Switch Access focus ring
(`focusable="false"`), and it is traversed **after** `gc_analysis_hint_container` so the
announcement order matches the visual order.

**R11 (MUST, happy)** — Given the badge is visible and the tips card
`gc_analysis_hint_container` is also visible, then the badge stays pinned at the bottom as
in R2 and the tips card sits above it, with the two not overlapping (Figma `32314:16717`).

## Affected modules

| Module | Change |
|---|---|
| `bank-api-library:library` | new `ingredientBrandScreens` field in the `/configurations` response + model |
| `capture-sdk:default-network` | maps the new field from `BankConfiguration` into capture's `Configuration` |
| `capture-sdk:sdk` | `Configuration`, DataStore persistence, new use case + screen enum, new XML badge layout, Analysis **and camera** screen wiring, strings, colours, dimens, a text appearance, three Analysis layouts + four camera layouts |

`bank-api-library:library` is a Gradle project dependency of `capture-sdk:default-network`,
which `bank-sdk:sdk` depends on, so all three rebuild. No source change in `bank-sdk`,
`health-sdk`, `internal-payment-sdk`, `health-api-library` or `core-api-library`.

## Public API impact

Additive at **source** level, but **binary-breaking for pre-compiled callers**: three public
Kotlin `data class`es gain a parameter, so their generated `<init>` and `copy` lines are
*re-signed* rather than added to in the dumps. An integrator compiled against 4.5.0 that calls
`Configuration.copy(...)` or the generated constructor would get a `NoSuchMethodError` at
runtime, not a compile error. This is deliberate and follows the `paymentScheduleHintEnabled` /
`creditNoteHintEnabled` precedent on the same classes, and these are response models
integrators rarely construct — but it must be stated in the PR and the release notes rather
than described as purely additive. The dumps must be re-pinned (`./gradlew <module>:apiDump`)
or `apiCheck` fails:

- `bank-api-library:library`
  - `net.gini.android.bank.api.response.ConfigurationResponse` — new
    `@Json(name = "ingredientBrandScreens") val ingredientBrandScreens: List<String>? = null`.
    *Additive.*
  - `net.gini.android.bank.api.models.Configuration` — new
    `val ingredientBrandScreens: List<String> = emptyList()`. *Additive*; changes the
    generated `copy`/constructor signatures. Re-pin `bank-api-library/library/api/library.api`.
- `capture-sdk:sdk`
  - `net.gini.android.capture.internal.network.Configuration` — new
    `val ingredientBrandScreens: Set<String> = emptySet()`. Kotlin-public despite the
    `internal.` package name, and present in the dump (`sdk.api:2425`). *Additive*; re-pin
    `capture-sdk/sdk/api/sdk.api`.
  - ViewBinding generates a public binding class for every layout, so the new layout adds
    `net.gini.android.capture.databinding.GcPoweredByGiniBinding` plus a `gcPoweredByGini`
    field on both `GcFragmentAnalysisBinding` and `GcFragmentCameraBinding`. *Additive*,
    generated, same dump.
- Everything else new is `internal`, or a resource, and therefore **not** in any dump:
  `IngredientBrandScreen`, `GetIngredientBrandVisibleUseCase`, `ingredientBrandModule`, and
  the `gc_powered_by_gini` layout, string and id resources. Because the badge is a plain
  `View` bound inside `AnalysisFragmentImpl`, the public
  `net.gini.android.capture.analysis.AnalysisFragmentExtension` is **not** touched, and
  neither is `GiniColorScheme` — no Compose is involved.
- `AnalysisScreenContract` is package-private (`AnalysisScreenContract.java:29`), so the new
  `View` method costs nothing in API terms.
- `capture-sdk/default-network/api/default-network.api` is untouched — the mapping function
  `mapBankConfigurationToConfiguration` is private.

## Technical conventions

1. **Language.** All new files Kotlin under `src/main/java/`, `internal` unless listed as
   public above. Two legacy Java files are edited, and only these:
   `analysis/AnalysisScreenContract.java` (one new abstract `View` method) and
   `analysis/AnalysisFragmentImpl.java` (implement it, delegate to the Kotlin extension).
   They are touched because the Analysis screen is legacy Java MVP and this is its contract
   boundary — no Java is converted to Kotlin.
2. **UI.** Android Views / XML — **not** Compose. The Analysis screen is a legacy
   Java/XML screen, so the badge is one new small layout file included into the existing
   Analysis layouts, modelled on
   `internal-payment-sdk/sdk/src/main/res/layout/gps_powered_by_gini.xml`. One XML layout
   file is added (`gc_powered_by_gini.xml`); three existing Analysis layouts are edited. No
   layout is removed. No `ComposeView`, no composable, no `@Preview` — dark mode is covered
   by the existing theme attributes and `values-night`.
3. **Architecture.** The Analysis screen stays legacy MVP — do **not** introduce a
   ViewModel here. `AnalysisScreenPresenter.start()` asks the use case and pushes the result
   to the view; the view has no logic beyond `View.VISIBLE`/`View.GONE`. There is no new
   state, intent or effect class. Configuration reading follows the existing
   provider + use-case pattern, not a Flow in the presenter.
4. **DI and async.** New Koin module `ingredientBrandModule` in
   `capture-sdk/sdk/src/main/java/net/gini/android/capture/di/`, registered in
   `CaptureSdkIsolatedKoinContext` (isolated context, resolved via `getGiniCaptureKoin()`);
   `factory { }` for the use case, matching `paymentHintsModule`. No Hilt. The use case is
   synchronous over `GiniBankConfigurationProvider`'s `AtomicReference` — no coroutine, no
   new dispatcher, no LiveData, no RxJava. The DataStore read path already runs in
   `GiniCaptureViewModel`'s `viewModelScope`; nothing new is launched.
5. **Strings and resources.** `capture-sdk:sdk` has `values/` (German, the default) and
   `values-en/` — both get:
   `gc_powered_by_gini_label` = `Powered by` (unchanged in both languages, matching
   `gps_payment_powered_by_label`) and
   `gc_powered_by_gini_content_description` = `Powered by Gini`. No placeholders. No new
   locale folder, no `values-night` entry (the pill is deliberately theme-independent — the
   two colours are added to both Compose colour schemes instead).
   The approved component needs values the existing token set does not carry, so the feature
   adds — all named for the brand element, none of them overridden in `values-night`:
   `@color/gc_powered_by_gini_label` (`#3A3A3C`, the Figma token `color/text/secondary`);
   `gc_powered_by_gini_corner_radius` (8dp), `..._padding_horizontal` (6dp),
   `..._padding_vertical` (3dp), `..._logo_spacing` (3dp), `..._logo_width` (24dp),
   `..._logo_height` (17dp) in `values/dimens.xml`; the text appearance
   `GiniCaptureTheme.Typography.PoweredByGini` (10sp, `-0.05em`, the fixed label colour) in
   `values/typography.xml`, following the file's existing `Root.*` + alias pattern; and the
   shape drawable `gc_powered_by_gini_background.xml` filled with `@color/gc_light_01`
   (`#FFFFFF`, which has no `values-night` override). The bottom margin reuses `gc_large`
   (16dp). The wordmark uses `@drawable/ic_gini_logo` untinted.
6. **Quality gates.** `./gradlew bank-api-library:library:ktlintCheck detekt`,
   same for `capture-sdk:sdk` and `capture-sdk:default-network`, all clean; every new Kotlin
   class has a unit test (below), so Jacoco/Sonar coverage does not drop. Run `apiDump` for
   `bank-api-library:library` and `capture-sdk:sdk` and commit the updated dumps.

## Design

### 1. Backend field → bank API model

`bank-api-library/library/src/main/java/net/gini/android/bank/api/response/ConfigurationResponse.kt`
gains the nullable field and maps it in the existing `toConfiguration()`:

```kotlin
@Json(name = "ingredientBrandScreens") val ingredientBrandScreens: List<String>? = null,
// …
ingredientBrandScreens = ingredientBrandScreens ?: emptyList(),
```

Nullable-with-default is how `paymentScheduleHintEnabled` and `creditNoteHintEnabled` were
added, and it is what makes R3's "field absent" case work without a Moshi failure.

### 2. Bank model → capture model

`capture-sdk/default-network/src/main/java/net/gini/android/capture/network/GiniCaptureDefaultNetworkService.kt`
— add one line to the private `mapBankConfigurationToConfiguration` (around line 199):

```kotlin
ingredientBrandScreens = configuration.ingredientBrandScreens.toSet(),
```

`Set` in capture, `List` in the API response: the JSON is a list, but capture only ever asks
"does it contain this screen", and `Set<String>` is what DataStore can persist directly.

### 3. Persistence

`capture-sdk/sdk/src/main/java/net/gini/android/capture/internal/storage/ClientConfigurationStorage.kt`
— first non-boolean key in this file:

```kotlin
private val keyIngredientBrandScreens = stringSetPreferencesKey("ingredient_brand_screens")
```

`saveConfiguration` writes `configuration.ingredientBrandScreens`; `getConfiguration` reads
`prefs[keyIngredientBrandScreens] ?: emptySet()`. The existing `Preferences.flag()` helper
stays boolean-only. `GiniBankConfigurationProvider`'s initial `Configuration` gets
`ingredientBrandScreens = emptySet()` so the pre-response default is "hidden" (R6).

The value reaches the provider through the path that already exists — no new plumbing:
`GiniCaptureFragment.setupUserAnalytics()` (`GiniCaptureFragment.kt:137-152`) saves the
response to DataStore, and `GiniCaptureViewModel`'s `init` collector copies DataStore into
`GiniBankConfigurationProvider`. Because the ViewModel's `config.copy(...)` only overrides
`clientID` and `amplitudeApiKey`, the new field flows through untouched.

### 4. Screen enum + use case

New package `net.gini.android.capture.ingredientbrand`:

```kotlin
internal enum class IngredientBrandScreen(val rawValue: String) {
    ANALYSIS("Analysis")
}

internal class GetIngredientBrandVisibleUseCase(
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
) {
    operator fun invoke(screen: IngredientBrandScreen): Boolean =
        giniBankConfigurationProvider.provide().ingredientBrandScreens
            .any { it.equals(screen.rawValue, ignoreCase = true) }
}
```

Case-insensitive `any` gives R5, and gives R4 for free: an unknown name simply matches
nothing. Modelled on
`capture-sdk/sdk/src/main/java/net/gini/android/capture/paymentHints/GetCreditNoteHintEnabledUseCase.kt`.
The enum is what keeps "Analysis" from being a bare string literal at the call site, and is
where the next screen (PP-2568 phase 2) gets added.

### 5. The badge layout

New `capture-sdk/sdk/src/main/res/layout/gc_powered_by_gini.xml` — a horizontal `LinearLayout`
(`wrap_content` × `wrap_content`) reproducing the approved component `Powered by Gini`
(`35631:1800`):

```
LinearLayout  background=@drawable/gc_powered_by_gini_background   (white, 8dp radius)
              paddingHorizontal=6dp  paddingVertical=3dp  gravity=center_vertical
              contentDescription=@string/gc_powered_by_gini_content_description
              importantForAccessibility="yes"  screenReaderFocusable="true"
              focusable="false"  layoutDirection="ltr"
  TextView    style=@style/GiniCaptureTheme.Typography.PoweredByGini   (10sp, -0.05em, #3A3A3C)
              text=@string/gc_powered_by_gini_label
              importantForAccessibility="no"
  ImageView   src=@drawable/ic_gini_logo   24 × 17dp, marginStart=3dp
              importantForAccessibility="no"
```

The logo is given an explicit size instead of its intrinsic `28 × 20dp`, because the Figma
lockup measures `23.8 × 15.3dp`; `ic_gini_logo`'s viewport carries ~1dp of padding around the
glyph, so `24 × 17dp` renders the glyph at the designed size. It is not tinted — the drawable
carries the brand blue `#009EDC`, and `accent01` is `#006ECF`/`#007FEE`, so tinting it (as the
unused Compose `GiniLogo` does) would be the wrong colour.

`screenReaderFocusable="true"` declares the merged node explicitly rather than relying on the
implicit "a ViewGroup with a contentDescription becomes one node", and `focusable="false"`
keeps a non-interactive badge out of the keyboard/Switch Access focus ring.
`layoutDirection="ltr"` stops the brand lockup mirroring under RTL, which is safe precisely
because the announcement comes from the container's description rather than child order.

### 6. Analysis screen wiring

- All three layouts (`layout/`, `layout-sw600dp/`, `layout-sw600dp-land/`
  `gc_fragment_analysis.xml`) get:

  ```xml
  <include
      android:id="@+id/gc_powered_by_gini"
      layout="@layout/gc_powered_by_gini"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:accessibilityTraversalAfter="@id/gc_analysis_hint_container"
      android:layout_marginBottom="@dimen/gc_large"
      android:visibility="gone"
      app:layout_constraintBottom_toBottomOf="parent"
      app:layout_constraintEnd_toEndOf="parent"
      app:layout_constraintStart_toStartOf="parent" />
  ```

  The `<include>` is declared **before** `gc_analysis_hint_container` in the file, because that
  container constrains itself to this id. That ordering is what makes
  `accessibilityTraversalAfter` necessary: TalkBack traverses a `ConstraintLayout` in child
  index order, so without it the badge would be announced before the tips card it sits below.
- For R11, each layout's `gc_analysis_hint_container` changes
  `app:layout_constraintBottom_toBottomOf="parent"` to
  `app:layout_constraintBottom_toTopOf="@id/gc_powered_by_gini"`. Its
  `android:layout_marginBottom` is left exactly as it is (`gc_large_32`, or
  `gc_analysis_hint_container_bottom_margin_portrait` in `layout-sw600dp/`) and no
  `layout_goneMarginBottom` is needed: ConstraintLayout collapses a `GONE` target to a point at
  its resolved position — here the parent bottom, since the gone view's own margin is ignored —
  and falls back to the regular margin, so with the badge hidden the card sits the same
  distance above the parent bottom as before. That is what makes R3 verifiable rather than
  hopeful.
- `AnalysisFragmentImpl.java` binds the badge in its existing private `bindViews(View)` into a
  new field, and implements the new contract method (with `@Override`) by setting
  `View.VISIBLE` / `View.GONE`. The public `AnalysisFragmentExtension` is not involved — it
  exists only to host Compose views, and there is no Compose here.
- `AnalysisScreenContract.View` gains `abstract void setPoweredByGiniVisible(boolean visible)`;
  `AnalysisFragmentImpl` implements it.
- `AnalysisScreenPresenterExtension.kt` exposes the use case as a property next to the four
  payment-hint use cases (`getGiniCaptureKoin().inject()`) — a property rather than a helper
  function, because `AnalysisScreenPresenterExtension` is already at detekt's
  `TooManyFunctions` limit of 20. `AnalysisScreenPresenter.start()` (line 200) calls
  `getView().setPoweredByGiniVisible(extension.getIngredientBrandVisibleUseCase().invoke(ANALYSIS))`
  — the same shape as the credit-note flag read at `AnalysisScreenPresenter.java:629`.

### 7. QR-code analysis step on the camera screen (R12, R13)

The badge belongs to the QR-code analysis step, not to the camera screen as a whole, so its
owners are the two popups that drive that step rather than the fragment's lifecycle.
`CameraFragmentExtension.showQrCodePopup` picks exactly one of them — education if
`GetQrEducationTypeUseCase` returns a type and the feature is on, retrieval otherwise — so both
have to reveal the badge or it goes missing on one path:

- `internal/camera/view/QRCodePopup.kt` already switches the screen into the retrieval state in
  `progressViews()` (loading indicator + `gc_retrieving_invoice`) and back out in `hideViews()`.
  It gains two constructor parameters, **appended after the existing ones** so the positional
  Java call sites keep binding to the same parameters: `poweredByGiniView: View?` and
  `isIngredientBrandVisible: () -> Boolean`. `progressViews()` shows the badge when the supplier
  says so; `hideViews()` always hides it. A supplier rather than a captured `Boolean`, for the
  same reason `isNewWarningEnabled` is one — the configuration is fetched asynchronously and may
  not be known when the popup is created.
- Only the **supported**-QR-code popup (`mPaymentQRCodePopup`) is given them; the
  unsupported-QR popup never enters the retrieval state, so it keeps the `null`/`{ false }`
  defaults.
- `internal/camera/view/education/qrcode/QRCodeEducationPopup.kt` gains the same two
  parameters. `showViews()` reveals the badge; `hideViews()` hides it. Because `hide()` is
  never called on this popup — the education content signals the end of the flow through its
  `onComplete` — the badge is also taken down in a wrapper around that callback, before the
  original `onComplete` runs.
- The `<include>` is the **last child** of the camera layout root, so it draws on top of
  `gc_qr_code_education_compose_view`, which spans the screen. Constraining to
  `gc_injected_navigation_bar_container_bottom` still works because that container is declared
  earlier in the file.
- `camera/CameraFragmentExtension.kt` — the internal abstract base of `CameraFragmentImpl`,
  which already injects `GiniBankConfigurationProvider` and exposes
  `isUnsupportedQRCodeWarningEnabled()` — gains `isIngredientBrandVisible()` with the same
  shape, delegating to the same use case with `IngredientBrandScreen.ANALYSIS`.
- `CameraFragmentImpl.java` binds the badge in `bindViews` and passes it to `createPopups`,
  which already runs after `bindViews`. It deliberately does **not** set the visibility itself
  and is **not** part of `showInterfaceAnimated()` / `hideInterfaceAnimated()`: the popup is the
  single owner, and the badge is `GONE` in the layout, so the live camera never shows it (R13).
- All four `gc_fragment_camera.xml` variants (`layout/`, `layout-land/`, `layout-sw600dp/`,
  `layout-sw600dp-land/`) get the same `<include>`, centred on the screen and anchored
  `app:layout_constraintBottom_toTopOf="@id/gc_injected_navigation_bar_container_bottom"`
  instead of to the parent bottom. That container is `wrap_content` and empty unless an
  integrator injects a bottom bar, so with none it collapses to zero height at the parent bottom
  and the badge lands 16dp above the screen bottom exactly as the approved frames show — while
  an injected bar pushes the badge above it rather than behind it. The `<include>` is declared
  **after** that container (its id must already exist), and carries both
  `accessibilityTraversalAfter="@id/gc_button_camera_trigger"` and
  `accessibilityTraversalBefore="@id/gc_injected_navigation_bar_container_bottom"` — the
  bottom-bar container contributes no accessibility node unless a client injects a bar, so
  anchoring to it alone would silently no-op in the default configuration.
- **No camera control moves, and no layout is otherwise touched.** Because the badge is on
  screen only during the QR-code loading state — where the preview is dimmed, interaction is
  disabled and the shutter is not usable — it cannot collide with the shutter or with
  `gc_detection_error_layout`. An earlier revision anchored `gc_pane_wrapper` and the detection
  error popup around the badge to avoid exactly those collisions; that surgery was reverted once
  the badge stopped appearing on the live camera.
- `CameraFragmentImpl.java` binds the badge in `bindViews` and calls a new private
  `setPoweredByGiniVisibility()` from `onCreateView`, right after `bindViews`. The badge is
  also part of the screen's interface hide/show cycle: `hideInterfaceAnimated()` sets it `GONE`
  and `showInterfaceAnimated()` re-applies the flag, so it does not stay in the accessibility
  tree behind the opaque camera-permission view.
- Traversal is pinned with **both** `accessibilityTraversalAfter="@id/gc_button_camera_trigger"`
  and `accessibilityTraversalBefore="@id/gc_injected_navigation_bar_container_bottom"`. The
  bottom-bar container contributes no accessibility node unless a client injects a bar, so
  anchoring to it alone silently no-ops in the default configuration.

**Placement differs per variant, because the camera screen's controls do.** `layout/` is the
only variant with a bottom control pane; `layout-land/`, `layout-sw600dp/` and
`layout-sw600dp-land/` all put the controls in a side pane next to the preview column.

- **`layout/` (phone portrait).** `gc_button_camera_trigger` is 70dp with
  `layout_marginBottom="@dimen/gc_large_24"` inside `gc_pane_wrapper`, so it spanned 24–94dp
  above the bottom-bar line while the badge spans 16–39dp — a ~15dp overlap. Touch dispatch was
  unaffected (the badge's root is a plain non-clickable `LinearLayout`, so it returns false and
  dispatch continues), but the badge is `screenReaderFocusable` and drawn later, so it won
  explore-by-touch over the shutter's lower edge — TalkBack said "Powered by Gini" where it
  should say "Take a picture". Fixed by re-anchoring `gc_pane_wrapper`'s bottom from
  `gc_injected_navigation_bar_container_bottom`'s top to `gc_powered_by_gini`'s top. With the
  badge `GONE` it collapses to a point at that same line (a gone widget's own margin is
  ignored), so the pane keeps exactly its current position for every client without ingredient
  branding; with the badge visible the controls move up ~39dp, which is also closer to the
  clearance the approved frames show.
- **The three side-pane variants.** The badge is moved *inside* `gc_camera_frame_wrapper` and
  centred on the preview column rather than on the whole screen, because centring it on the
  screen put it under the side pane's column and in the same band as
  `gc_detection_error_layout`. Being a sibling of that popup lets the popup constrain above it:
  its bottom is re-anchored to `gc_powered_by_gini`'s top with
  `app:layout_goneMarginBottom="@dimen/gc_large"`, so with the badge hidden it keeps today's
  16dp and with the badge shown it can never be covered. Obscuring an error message would be a
  worse defect than obscuring a badge.

## Test plan

Stack: JUnit4 + Google Truth throughout; MockK where a collaborator must be faked;
Robolectric for anything touching Android views; `kotlinx-coroutines-test` + `runTest` for
the DataStore test. All new classes get a unit test, per `platform.md`. No new test
dependency is needed — the badge is a `View`, so the existing Robolectric setup covers it.

| Test class | New or extended | Covers | ~Tests |
|---|---|---|---|
| `bank-api-library/library/src/test/java/net/gini/android/bank/api/response/ConfigurationResponseTest.kt` | **extend** | `toConfiguration()` maps `["Analysis"]`, `[]`, and `null`/absent → `emptyList()` | +3 |
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/ingredientbrand/GetIngredientBrandVisibleUseCaseTest.kt` | new | R4, R5: exact match, `"analysis"`, `"ANALYSIS"`, unknown-only, empty set, mixed known+unknown. Mirrors `paymentHints/GetPaymentScheduleHintEnabledUseCaseTest.kt` | 6 |
| `capture-sdk/sdk/src/androidTest/java/net/gini/android/capture/internal/storage/ClientConfigurationStorageTest.kt` | **extend** | R7: save/read round-trip of a non-empty set, and empty set after `clearConfiguration()` | +2 |
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/GiniCaptureViewModelTest.kt` | **extend** | the DataStore → provider collector carries `ingredientBrandScreens` through untouched (the `copy` only overrides `clientID`/`amplitudeApiKey`) | +1 |
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/internal/provider/GiniBankConfigurationProviderTest.kt` | **extend** | default is `emptySet()`, and a concurrent `update` does not drop the field | +2 |
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/analysis/AnalysisScreenPresenterTest.kt` | **extend** | R1, R3: `start()` calls `setPoweredByGiniVisible(true)` when the config lists `Analysis`, `false` when it does not | +2 |
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/ingredientbrand/PoweredByGiniLayoutTest.kt` | new | R2, R8, R10: inflating `gc_powered_by_gini.xml` under `GiniCaptureTheme` renders the label, the fixed `#3A3A3C`/10sp/`-0.05em` label style, the pill background, the untinted `ic_gini_logo` at `24 × 17dp`, and the single-node accessibility contract | 6 |
| `capture-sdk/sdk/src/test/java/net/gini/android/capture/ingredientbrand/IngredientBrandLayoutTest.kt` | new | R9, R12: every layout variant of both covered screens includes the badge and starts `GONE` — three Analysis variants and four camera variants, selected with Robolectric `@Config(qualifiers = …)`. A variant that forgot the `<include>` would otherwise only crash for clients that have branding enabled | 7 |

`PoweredByGiniLayoutTest` follows
`capture-sdk/sdk/src/test/java/net/gini/android/capture/internal/camera/view/UnsupportedQrDialogButtonsTest.kt`:
`RobolectricTestRunner` + `ContextThemeWrapper(context, R.style.GiniCaptureTheme)` +
`LayoutInflater`.

### Not tested

- **The badge's exact appearance** — the rendered pill, icon geometry and colours in light and
  dark mode (R2, R8). The repo has no screenshot-test setup, so this is manual QA against Figma
  `35002:11407` (portrait) and `35002:11512` (landscape). The approved section has no dark
  *Analysis* frame; the dark reference is the same component on the dark camera screens
  (`35002:11361`, `35002:11735`), which is why the badge is theme-independent.
- **The merged accessibility node itself (R10).** `PoweredByGiniLayoutTest` asserts the
  view-level contract (`contentDescription`, `screenReaderFocusable`, `focusable`, both
  children `importantForAccessibility="no"`) rather than the produced
  `AccessibilityNodeInfo`, because Robolectric returns an unpopulated node for a detached view
  even after `onInitializeAccessibilityNodeInfo`. Verifying the real node, and the
  `accessibilityTraversalAfter` ordering against the tips card, needs an instrumented/TalkBack
  pass.
- **Text scaling.** No automated check that the badge and the tips card survive a 200% font
  scale. Reviewed by hand: the badge grows to ~34dp and does not clip, but
  `gc_analysis_hint_container` has no top constraint in a non-scrolling root, so at the largest
  font scale its top can still slide under the top bar — pre-existing, and the badge consumes
  ~46dp of the remaining headroom.
- **Placement on the three layout variants (R9)** and non-overlap with the tips card (R11).
  ConstraintLayout resolution and `layout_goneMarginBottom` are framework behaviour; verified
  by manual QA on a phone and a tablet in both orientations.
- **The real backend response.** PP-2572 is not done, so nothing verifies the live JSON field
  name end to end. The mapping test uses a hand-written JSON fixture; a mismatch with what
  the backend finally ships would only surface in manual QA against a client that has the
  flag on.
- No new instrumented tests in `bank-sdk:example-app` (decided): the CreditNote mock-backend
  test suite is the precedent if end-to-end coverage is wanted later.

## Out of scope

- **The animated Gini logo** that replaces the analysis loading indicator, and the
  "Retrieving invoice" / "In case you didn't know…" copy in the same Figma canvas. Those are
  a separate change; the existing loading indicator and analysis message stay exactly as they
  are.
- Any screen other than Analysis and camera (Review, Onboarding, Help, No-results, Error). The
  `IngredientBrandScreen` enum has one entry, `ANALYSIS`, which governs both covered screens.
- `health-sdk` / `internal-payment-sdk` ingredient branding. They already have their own
  mechanism (`ingredientBrandType` with `FULL_VISIBLE`/`PAYMENT_COMPONENT`/`INVISIBLE` in
  `health-api-library`), and it is deliberately **not** unified with this one here.
- A public SDK flag or `GiniComposableStyleProvider` override for the badge — the ticket
  states "no SDK flag", so integrators get no way to hide, restyle or reposition it.
- Refactoring or reusing the unused public `GiniLogo` composable. It stays untouched: the
  Analysis screen is XML, and its `accent01` tint is the wrong colour for the brand mark.
- Window-inset handling on the Analysis screen. The screen uses fixed bottom margins today
  and the badge follows suit; edge-to-edge is not addressed.

## Open questions

1. ~~**The backend field is not implemented yet.**~~ **Resolved.** PP-2572 is Done and the
   field is live on the backend, so the temporary `FORCE_VISIBLE_FOR_TESTING` override that
   this spec used to carry has been removed — visibility now comes from the real
   `/configurations` response only. The exact spelling of `ingredientBrandScreens` and of the
   screen-name string `"Analysis"` should still be eyeballed once against a real payload from a
   client that has the flag on; case-insensitive matching covers a casing mismatch but not a
   different field or screen name.
2. ~~**The tips-card case is an assumption.**~~ **Resolved — R11 confirmed.** Re-reading the
   approved section shows `Frame 26081833` in the five `6.3.x android-ph-analyzeTips` frames is
   not empty: it holds a *detached copy* of the badge (the `Powered by Gini` frame with the
   "Powered by" text and the `Logo` instance) at the same position and size as the component
   instance used elsewhere, and it is not hidden. The same is true of
   `4.1.3 android-phL-Capture-analyze` (`35002:11528`). Every Android frame in the section
   therefore shows the badge — some as a component instance, some as a detached copy — so the
   badge stays visible alongside the tips card, exactly as R11 specifies.
3. ~~**Standalone `AnalysisFragment` now needs the Bank SDK's DI bridge.**~~ **Resolved — no
   change needed.** Reading the flag in `start()` resolves `GiniBankConfigurationProvider`,
   whose only production Koin definition lives in `bank-sdk` (`CaptureSdkDiBridge`). The
   pre-existing `start()` path already reached that provider through
   `showHintsForImage()` → `getInvoiceEducationType()`, but inside
   `runCatching { … }.getOrNull()`, so a missing definition was swallowed; the new call is not
   wrapped, so it would throw instead. That only affects an integrator consuming
   `capture-sdk` without `bank-sdk` — and every Gini integrator ships `bank-sdk`, which always
   registers the provider. Left as is deliberately: adding a default definition to capture-sdk's
   own `providerModule` would only serve a configuration nobody uses.
4. **No version bump.** `capture-sdk:sdk`, `capture-sdk:default-network` and
   `bank-api-library:library` all change published API and are all still `4.5.0`. Per
   `RELEASE.md` the capture-sdk pair must always move together; decide whether the bump belongs
   in this PR or in a separate RC PR.

## Implementation plan

- [x] 1. `bank-api-library:library` — add `ingredientBrandScreens: List<String>? = null` to
      `response/ConfigurationResponse.kt`, map it in `toConfiguration()`, add
      `ingredientBrandScreens: List<String> = emptyList()` to `models/Configuration.kt`;
      extend `response/ConfigurationResponseTest.kt` (list / empty / absent). (R1, R3, R4)
- [x] 2. `capture-sdk:sdk` + `capture-sdk:default-network` — add
      `ingredientBrandScreens: Set<String> = emptySet()` to
      `internal/network/Configuration.kt`, default it in `GiniBankConfigurationProvider`,
      map it in `GiniCaptureDefaultNetworkService.mapBankConfigurationToConfiguration`;
      extend `GiniBankConfigurationProviderTest.kt`. (R1, R6)
- [x] 3. `capture-sdk:sdk` — persist the set in `ClientConfigurationStorage` via
      `stringSetPreferencesKey`; extend `androidTest` `ClientConfigurationStorageTest.kt`
      and `GiniCaptureViewModelTest.kt`. (R7, R6)
- [x] 4. `capture-sdk:sdk` — new `ingredientbrand` package: `IngredientBrandScreen`,
      `GetIngredientBrandVisibleUseCase`, `di/ingredientBrandModule.kt`, registered in
      `CaptureSdkIsolatedKoinContext`; new `GetIngredientBrandVisibleUseCaseTest.kt`.
      (R4, R5)
- [x] 5. `capture-sdk:sdk` — strings in `values/` and `values-en/`, new
      `layout/gc_powered_by_gini.xml`; new `PoweredByGiniLayoutTest.kt`. (R2, R10)
- [x] 6. `capture-sdk:sdk` — include the badge in `layout/`, `layout-sw600dp/` and
      `layout-sw600dp-land/` `gc_fragment_analysis.xml`, re-anchor
      `gc_analysis_hint_container` to the badge's top, and set
      `accessibilityTraversalAfter`. (R2, R3, R9, R10, R11)
- [x] 7. `capture-sdk:sdk` — `AnalysisScreenContract.View.setPoweredByGiniVisible`,
      binding + implementation in `AnalysisFragmentImpl.java`, use-case injection in
      `AnalysisScreenPresenterExtension.kt`, call in `AnalysisScreenPresenter.start()`;
      extend `AnalysisScreenPresenterTest.kt`. (R1, R3, R6)
- [x] 8. Re-pin the binary-compatibility dumps: `bank-api-library:library:apiDump` and
      `capture-sdk:sdk:apiDump`.
- [x] 10. `capture-sdk:sdk` — camera screen (R12): `isIngredientBrandVisible()` in
      `CameraFragmentExtension.kt`, the `<include>` in all four `gc_fragment_camera.xml`
      variants anchored above `gc_injected_navigation_bar_container_bottom`, binding and
      `setPoweredByGiniVisibility()` in `CameraFragmentImpl.java`, and
      `IngredientBrandLayoutTest` covering all seven layout variants.
- [x] 9. Verification — `/gini-check` for the affected modules (plus the downstream
      `bank-sdk:sdk`), then the `code-reviewer` and `a11y-specialist` reviews.

# gini-doc platform conventions — Android (gini-mobile-android)

<!--
  NOT MIRRORED — this file is Android-specific by design. The iOS repo has its
  own platform.md with the same section headings but iOS content. If you add a
  section here that the shared workflow depends on, add the matching section
  to the iOS platform.md too.
-->

## Platform argument

The only valid `--platform` value in this repository is `android`.

## Source roots in scope

Keep only changed files under these directories — everything else is out of
scope:

- `bank-sdk/sdk/src/main/` (Bank SDK source root)
- `bank-api-library/library/src/main/` (Bank API library source root)
- `capture-sdk/sdk/src/main/` (Capture SDK source root, if the feature touches
  shared capture logic)

Skip test files, example apps, CI scripts, package manifests, localization
strings (collected separately via the localization step), and build output.

## Localization strings

- `res/values/strings.xml` holds the **German defaults** (maps to the
  "Default (de)" column in the output tables).
- `res/values-en/strings.xml` holds the **English strings** (maps to the
  "Default (en)" column).

Search the relevant modules for keys matching the feature prefix:

```bash
grep -r "<featureprefix>" --include="strings.xml" bank-sdk/sdk/src/main/res/
grep -r "<featureprefix>" --include="strings.xml" capture-sdk/sdk/src/main/res/
```

## Configuration surface

Features are toggled via public properties on `CaptureConfiguration`, defined
in `bank-sdk/sdk/src/main/java/net/gini/android/bank/sdk/capture/Configuration.kt`,
and applied with `GiniBank.setCaptureConfiguration(context, configuration)`.

## OS permissions → manifest requirements

Infer `AndroidManifest.xml` requirements from OS APIs used in source:

- `android.permission.CAMERA` →
  `<uses-permission android:name="android.permission.CAMERA" />` required
- `android.permission.READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` →
  `<uses-permission>` required

## Symbols to skip

- Anything `internal`, `private`, or prefixed with `_`
- Internal classes like `GiniCaptureUserDefaultsStorage` or `GiniConfiguration`

## Code sample conventions

- Code block language tags: `kotlin` for SDK usage, `xml` for manifest and
  resource snippets, `json` for extraction payload examples.
- The capture flow result callback in published docs handles
  `CaptureResult.Success / Error / Empty / Cancel` (see the `result-callback`
  snippet below). Include the `EnterManually` branch only when the documented
  feature involves it.

## Terms

Values for the `[term: name]` references used in the shared templates.

| Term | Value |
|---|---|
| `config-object` | `CaptureConfiguration` |
| `sdk-entry-point` | `GiniBank` |
| `manifest-file` | `AndroidManifest.xml` |
| `result-type` | `CaptureResult` |
| `success-result-case` | `CaptureResult.Success` |
| `empty-result-case` | `CaptureResult.Empty` |
| `cleanup-call` | `GiniBank.cleanupCapture()` |
| `code-language` | Kotlin (code block tag `kotlin`) |
| `ui-customization-guide-url` | `https://gini.atlassian.net/wiki/spaces/GBSV/pages/76283941` |

## Snippets

Code blocks for the `[snippet: name]` references used in the shared
templates. Keep the `[placeholder]` markers — the skill fills them from
source when generating a page.

### `enable-configuration`

```kotlin
val captureConfiguration = CaptureConfiguration(
    networkService = yourNetworkService,
    [propertyName] = [enableValue],
    // ...
)
GiniBank.setCaptureConfiguration(context, captureConfiguration)
```

### `revert-configuration`

```kotlin
val captureConfiguration = CaptureConfiguration(
    networkService = yourNetworkService,
    [propertyName] = [defaultValue], // default
    // ...
)
GiniBank.setCaptureConfiguration(context, captureConfiguration)
```

### `flag-configuration`

```kotlin
val captureConfiguration = CaptureConfiguration(
    [propertyName] = true,
    [tuningPropertyName] = [value], // include tuning properties, e.g. thresholds
    // ...
)
GiniBank.setCaptureConfiguration(context, captureConfiguration)
```

### `result-callback`

```kotlin
GiniBank.startCaptureFlow(resultLauncher)

private val resultLauncher = registerForActivityResult(
    GiniBank.createCaptureFlowContract()
) { result ->
    when (result) {
        is CaptureResult.Success -> handleExtractions(result.specificExtractions, result.compoundExtractions)
        is CaptureResult.Error -> handleError(result.value)
        CaptureResult.Empty -> handleNoResults()
        CaptureResult.Cancel -> handleCancellation()
    }
}
```

### `os-integration-declaration`

```xml
[intent-filter or uses-permission entry, copied from verified source — see the E-Invoice page for the intent-filter shape]
```

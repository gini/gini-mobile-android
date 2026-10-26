# PP-3505: "This is not a payment QR code" dialog is too wide in landscape

Status: fixed
Ticket: https://ginis.atlassian.net/browse/PP-3505

## Symptom

Observed: in landscape, the "This is not a payment QR code" dialog (capture-sdk's
unsupported-QR warning) spans roughly 63% of the screen width although its content (one
short title line and two text buttons) needs far less. On Pixel 8 Pro (Android 17) and
Pixel 4 (Android 13) the dialog shows a large empty area right of the title.

Expected: in landscape the dialog is no wider than its content needs.

The ticket says the issue "does not reproduce" on a Samsung Galaxy A55. The attached
screenshots show the dialog at the same ~63% width on all three devices; the Samsung was
tested in German, where the longer title ("Dieser QR-Code ist kein Zahlungs-QR-Code") fills
the width, so the stretching is simply not visible there. The defect is locale/content
dependent, not device dependent.

## Reproduction

Failing unit test (Robolectric, landscape qualifiers `w800dp-h400dp-land`):

`capture-sdk/sdk/src/test/java/net/gini/android/capture/internal/camera/view/QRCodePopupTest.kt`
`unsupported QR dialog is 55 percent of the screen width in landscape instead of the Material 65 percent`

It builds `QRCodePopup` with `supported = false` and `isNewWarningEnabled = { true }`, calls
`show(...)`, measures the dialog's decor view the way `ViewRootImpl` does (AT_MOST the
framework's preferred dialog width first) and asserts the measured width is 55% of the screen
width. Before the fix it fails with:

```
expected: 440
but was : 520
```

520 px is 65% of the 800 px landscape screen, 440 px the expected 55%.

Manual: bank-sdk example app, landscape, English locale, scan a non-payment QR code.

## Root cause

`QRCodePopup.showViews` builds the dialog with `MaterialAlertDialogBuilder(themedContext)`
(`capture-sdk/sdk/src/main/java/net/gini/android/capture/internal/camera/view/QRCodePopup.kt:140`)
without a dialog theme override. `MaterialAlertDialogBuilder` then applies the theme's
`materialAlertDialogTheme`, which for `Theme.Material3.*` (parent of
`Root.GiniCaptureTheme.Internal`, `capture-sdk/sdk/src/main/res/values/styles.xml:3`) is
`ThemeOverlay.Material3.MaterialAlertDialog`. Its parent chain ends in AppCompat's
`Base.ThemeOverlay.AppCompat.Dialog.Alert`, which sets

```
android:windowMinWidthMajor = @dimen/abc_dialog_min_width_major   (65%)
android:windowMinWidthMinor = @dimen/abc_dialog_min_width_minor   (95%)
```

`PhoneWindow` reads these into `mMinWidthMajor`/`mMinWidthMinor`, and
`DecorView.onMeasure` (android-36 `com/android/internal/policy/DecorView.java:744`) picks
`mMinWidthMajor` whenever the configuration is landscape and re-measures the window at
`65% * screenWidthDp` if the content is narrower. So in landscape the dialog window is forced
to 65% of the screen width regardless of content; in portrait the 95% minor minimum applies,
which is the normal full-width phone dialog and was never reported.

Note: `GiniCaptureTheme` only overrides `alertDialogTheme`
(`GiniCaptureTheme.ThemeOverlay.Dialog.Alert`, styles.xml:21/57). `MaterialAlertDialogBuilder`
does not read `alertDialogTheme`, it reads `materialAlertDialogTheme`, so that overlay has no
effect on this dialog.

The symptom surfaces in the dialog window; the cause is the inherited Material/AppCompat
window minimum, which this dialog never overrides.

## Proposed fix

capture-sdk:sdk only, resources plus one call-site change:

1. `capture-sdk/sdk/src/main/res/values/styles.xml`: add a parent-less theme overlay for this
   dialog that overrides `android:windowMinWidthMajor` with a 55% fraction instead of the
   inherited 65%. It has to stay a window *minimum*: `ViewRootImpl.measureHierarchy` first
   measures a wrap-content dialog at the framework's preferred dialog width
   (`config_prefDialogWidth`, 320dp on phones) and keeps that result unless the content reports
   `MEASURED_STATE_TOO_SMALL`. AppCompat's `windowFixedWidthMajor` is clamped to that pass, so a
   fixed width collapses to 320dp on a device (measured: 1120px on a Pixel 7 Pro); only
   `DecorView`'s minimum-width pass runs after it and ignores the clamp. Because the first pass
   has already wrapped the content, the window ends up exactly at the minimum.
   `MaterialAlertDialogBuilder` applies the theme's `materialAlertDialogTheme` to the dialog
   context before the override, so nothing else changes.
2. `capture-sdk/sdk/src/main/res/values/dimens.xml`: the width as a fraction dimen, 55% of the
   screen width. Portrait already sizes the dialog as a fraction of the screen (the stock 95%
   minor minimum), so landscape follows the same strategy with a smaller number; 55% is also
   AppCompat's own large-screen landscape minimum, so tablets need no separate value. On a
   Pixel 7 Pro (891dp wide) that is a 490dp window and, after Material's 10dp landscape
   insets, a card of about 470dp. `DecorView` computes the fraction from the configuration's
   `screenWidthDp`, so it can never overflow.
3. `QRCodePopup.kt:140`: pass the overlay as the `MaterialAlertDialogBuilder` override theme.

Only the `*Major` window attributes are consulted in landscape, so portrait is untouched
(still 95% of the width). Tablets keep their previous landscape width: `large` screens had 55%
from AppCompat already, and `values-xlarge/dimens.xml` keeps the 45% that AppCompat gives
extra-large screens.

Three earlier iterations were rejected on a device: a 280dp minimum with a content-hugging
window, a fixed 400dp window and a fixed 55% window. All three rendered a 320dp window on the
phone because of the preferred-width pass described above; the Robolectric tests measured the
decor view against the full screen and did not catch it. The on-device instrumented test and
the preferred-width first pass in the unit test were added as a result.

Public API impact: none in `api/sdk.api`. The new style and dimen are resources; capture-sdk
has no `public.xml`, so they are technically visible like every other `gc_` resource.

## Regression test plan

Unit (Robolectric + MockK + Truth, the stack of the neighbouring `AlertDialogHelperCompatTest`):
`capture-sdk/sdk/src/test/java/net/gini/android/capture/internal/camera/view/QRCodePopupTest.kt`.
The landscape test asserts the window is exactly the 55% fraction; it fails before the fix (65%)
and passes after. Two guards pin the untouched 95% portrait minimum and the 45% `xlarge`
override. The decor view is
measured against the framework's preferred dialog width first, as `ViewRootImpl` does, so a
fixed-width regression is caught.

Instrumented (real window, Pixel 7 Pro / CI emulator):
`capture-sdk/sdk/src/androidTest/java/net/gini/android/capture/internal/camera/view/QRCodePopupInstrumentedTest.kt`
with its host `QRCodePopupTestActivity` (landscape, `GiniCaptureTheme`, declared in the
androidTest manifest). Reads the dialog's `DecorView` width through Espresso's `isDialog()` root
and asserts 55% of `screenWidthDp`. Failed with 1120px (320dp) on the fixed-width iteration.

Run: `./gradlew capture-sdk:sdk:testDebugUnitTest --tests "*QRCodePopupTest"` and
`./gradlew capture-sdk:sdk:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.gini.android.capture.internal.camera.view.QRCodePopupInstrumentedTest`.

## Out of scope

- Every other `MaterialAlertDialogBuilder` dialog in capture-sdk and bank-sdk (e.g.
  `AlertDialogHelperCompat`, the warning dialogs) has the same 65% landscape minimum. Those
  usually carry a longer message and were not reported; changing `materialAlertDialogTheme`
  globally would alter all of them and is a design decision, not part of this fix.
- `GiniCaptureTheme.ThemeOverlay.Dialog.Alert` is bound to `alertDialogTheme`, which no
  `MaterialAlertDialogBuilder` honours. Whether it is still needed is a separate cleanup.

## Open questions

None. Decisions taken during the fix:

- Width: a 55% fraction of the screen width in landscape (`android:windowMinWidthMajor`),
  chosen by the user after a 280dp content-hugging floor and a fixed 400dp window were both
  judged too narrow on a device; a fraction keeps the portrait strategy (fraction of screen).
- The overlay is parent-less (`parent=""`): `MaterialAlertDialogBuilder` already applies the
  theme's `materialAlertDialogTheme` to the dialog context, so the overlay only layers
  `android:windowMinWidthMajor` on top and an integrator's dialog customisation stays intact
  (self-review finding).
- Tablets: unchanged from before the fix (55% on `large`, 45% on `xlarge` via a
  `values-xlarge` override, chosen by the user over following the phone value). Still worth one
  manual check on a tablet in landscape, since Robolectric does not lay out the real window.

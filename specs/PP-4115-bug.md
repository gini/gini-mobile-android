# PP-4115: [Android] "Cancel transfer" and "Proceed anyway" are stacked on tablets in portrait in the fraud warnings

Status: fixed
Ticket: https://ginis.atlassian.net/browse/PP-4115

## Symptom
On a tablet in portrait (Samsung Galaxy Tab A9+, Android 16, Android RC 1.0.0 (1290)), the fraud
warning (IBAN mismatch, not the sender's country, combined) shows "Cancel transfer" and
"Proceed anyway" one below the other. Expected per the ticket and its Figma link
(node 38468-42451): both buttons on one line, as they already are in landscape.

## Reproduction
1. Launch the bank-sdk example app on a tablet (`sw600dp`), portrait.
2. Upload a fraud invoice of any of the three types.
3. The warning sheet's footer shows the two buttons stacked.

Reproduced by `GiniActionFooterTest` (`tablet in portrait places the buttons side by side`),
which failed before the fix.

## Root cause
`capture-sdk/sdk/src/main/java/net/gini/android/capture/ui/components/footer/GiniActionFooter.kt:52`

```kotlin
horizontal: Boolean = isLandscape(),
```

`isLandscape()` (line 108) reads only `LocalConfiguration.current.orientation`. The fraud warning
(`analysis/fraud/IBANMismatchWarningScreen.kt:72`) calls `GiniActionFooter` without `horizontal`,
so on any device in portrait, tablets included, the footer takes the `Column` branch (line 84)
and stacks the buttons.

This is not an accident in the code but a wrong design assumption made when the component was
added (63ee97f2b, PP-3961): the KDoc (lines 27-31) and `specs/PP-3961-feature.md:200` state
"the design stacks the buttons on tablets in portrait". The ticket and its Figma node say the
opposite. The root cause and the symptom site are the same place: the default of `horizontal`.

## Proposed fix
In `GiniActionFooter.kt`, make the default side by side in landscape **or** on tablets:
`horizontal: Boolean = isLandscape() || isTablet()` (`isTablet()` is the existing internal helper
in `ui/theme/modifier/Width.kt`, backed by `R.bool.gc_is_tablet`), and correct the KDoc.

- Phones: unchanged (stacked in portrait, side by side in landscape).
- Tablets: side by side in both orientations. The footer is already capped at
  `gc_tablet_width` by `GiniBottomSheetScaffold`, so the row keeps the reading width.
- `IBANSelectionSheet` passes no secondary button and is unaffected.
- Public API impact: none (`GiniActionFooter` is `internal`). No resource changes.

## Regression test plan
New `capture-sdk/sdk/src/test/java/net/gini/android/capture/ui/components/footer/GiniActionFooterTest.kt`,
Robolectric + Compose test rule + Truth, like the neighbouring `IBANSelectionSheetTest`. Renders
the footer with both buttons and compares their bounds:

- `sw600dp-port`: same top, different left (fails before the fix).
- `sw600dp-land`, phone `land`: side by side (guards existing behaviour).
- phone portrait (default qualifiers): stacked (guards existing behaviour).

## Out of scope
- The legacy XML `WarningBottomSheet` (already-paid / credit-note / due-date hints,
  `layout-sw600dp/gc_warning_bottom_sheet.xml`) also stacks its buttons on tablets in portrait.
  The ticket is about the fraud warnings only.
- `specs/PP-3961-feature.md` keeps its original wording; this file records the correction.

## Open questions
- The Figma node could not be opened from this session (Figma not authenticated) and the ticket's
  screenshot is not readable through the Jira API, so "one line on tablets in portrait" rests on
  the ticket text. It contradicts the PP-3961 reading of the design. The fix was approved by the
  developer on 2026-11-27 on that basis; the Figma node itself is still to be checked by a human.
- Device check owed on a `sw600dp` tablet, portrait and landscape, all three warning types,
  including TalkBack and 200 % font scale with the German labels.

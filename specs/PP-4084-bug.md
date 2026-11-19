# PP-4084: [Android] Accessibility: TalkBack: IBAN fields are voiced twice on Fraud Warning Sheet and on IBAN Selection Sheet

Status: fixed
Ticket: https://ginis.atlassian.net/browse/PP-4084

## Symptom

With TalkBack on, an IBAN row on the fraud warning sheet or on the IBAN selection sheet is
announced twice in one stop: first the spelled-out version ("In the QR code, P L 6 1, 1 0 9 0, …"),
then the same row again from its visible texts ("In the QR code, PL61 1090 …").

Expected: each IBAN row is announced once, with the IBAN spelled character by character.

Reported by QA on Bank SDK example app RC 1.0.0 (1262), fix version Bank SDK 4.9.0, on Galaxy A55
(Android 16), Galaxy S9 (Android 10), Pixel 4 (Android 13) and Galaxy Tab A9+ (Android 15).

## Reproduction

Failing unit test (Robolectric, no device):

```
./gradlew capture-sdk:sdk:testDebugUnitTest \
  --tests "net.gini.android.capture.ui.components.labeledvalue.GiniLabeledValueTest"
```

`a row with a spoken value exposes only its description` fails: the row node carries the content
description "label, spelled IBAN, hint" **and** its three child `Text` nodes are still in the
unmerged semantics tree.

Not reproduced on a device in this session (none attached); the audible symptom is taken from the
ticket and its recording.

## Root cause

`capture-sdk/sdk/src/main/java/net/gini/android/capture/ui/components/labeledvalue/GiniLabeledValue.kt:48-58`

```kotlin
Row(
    modifier = modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) {
            if (valueContentDescription != null) {
                contentDescription = listOfNotNull(label, valueContentDescription, hint)
                    .joinToString(separator = ", ")
            }
        },
```

The row sets a content description that already contains label, spoken value and hint, but leaves
the three `Text` children in the semantics tree. `semantics(mergeDescendants = true)` adds to the
children's semantics, it does not replace them.

Compose's accessibility delegate (`AndroidComposeViewAccessibilityDelegateCompat`, ui 1.10.3)
builds the node infos from the **unmerged** tree: the row becomes the screen-reader-focusable node
with `contentDescription`, and each `Text` stays a non-focusable child node with its own `text`.
TalkBack speaks a focused node as its own description followed by the texts of its non-focusable
children, so the row is read once from the description and once from the texts.

Only rows with `valueContentDescription` are affected, and the only caller that passes it is
`ibanItem()` (`IBANMismatchWarningScreen.kt:193-204`), used by both sheets. That is why exactly the
IBAN rows on exactly these two sheets double up, while the sender row is read once.

The code comment at `GiniLabeledValue.kt:52-53` shows the intent (replace only the value in the
announcement); the implementation adds a second announcement instead.

Introduced with the component in `0bf3cdf35` (feat(capture-sdk): Add the GiniLabeledValue …); it
never worked.

## Proposed fix

In `GiniLabeledValue.kt` only: when `valueContentDescription` is set, remove the texts from the
semantics tree with `Modifier.clearAndSetSemantics {}` on the inner `Column` that holds label, value
and hint. The row keeps its merged node and its content description, so TalkBack has one source.
Rows without `valueContentDescription` are unchanged. Correct the misleading comment.

The leading slot (radio button) stays outside the cleared `Column`, and the `selectable` modifier
sits on the row, so selection state, role and the error semantics of the selection sheet are
untouched.

Public API impact: none (no signature change; `apiCheck` to confirm).

Side effect on tests: `IBANSelectionSheetTest.row()` finds rows by the displayed IBAN text on the
merged node. That text is no longer in semantics, so the helper has to match the row's content
description instead.

## Regression test plan

- New `capture-sdk/sdk/src/test/java/net/gini/android/capture/ui/components/labeledvalue/GiniLabeledValueTest.kt`
  (Compose UI test under Robolectric, same stack as `IBANSelectionSheetTest`):
  - `a row with a spoken value exposes only its description` — fails before the fix, passes after.
  - `a row without a spoken value is read from its texts` — guards the unchanged path.
- `IBANSelectionSheetTest` keeps passing after its `row()` helper is adapted.

## Out of scope

- Any other wording or reading-order change on the two sheets.
- A TalkBack check on a device — owed to manual QA (phone and tablet, both sheets).

## Open questions

- None blocking. Device confirmation of the fix is still needed, since the unit test proves the
  semantics tree, not TalkBack's speech.

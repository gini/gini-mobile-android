# PP-3262: [Android] Show Due Date Hint as a bottom sheet on the Analysis screen

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-3262

## Problem

When a bank customer photographs an invoice that is not due yet (due date more
than the threshold days in the future), the SDK currently shows a small inline
"tip" card at the bottom of the Analysis screen
(`PaymentDueHintContent.kt`) that auto-dismisses after 5 seconds via an
animated progress bar. This is easy to miss, gives the user no real choice,
and conflicts with the rotating capture suggestions for TalkBack users.

The redesign replaces that inline card with a modal bottom sheet (dialog on
tablets) — the same interaction pattern already used by the "document marked
as paid" warning — giving the user an explicit choice: cancel the transfer
(return to the bank app) or proceed anyway.

## Requirements

1. Given the client-config flag `paymentDueHintEnabled = true` (Gini API
   configuration) and the SDK flag `GiniCapture.isPaymentDueHintEnabled()`,
   when analysis returns extractions with a `paymentDueDate` whose remaining
   days are ≥ `GiniCapture.getPaymentDueHintThresholdDays()` (default 5) and
   the payment state is "to be paid", a modal bottom sheet appears on top of
   the Analysis screen. (The trigger condition in
   `AnalysisScreenPresenter.shouldShowPaymentDueHint()` is unchanged —
   including the existing exclusions: CX mode, Return Assistant/Skonto
   extractions present, empty/unparseable due date.)
2. If the due date is today, in the past, or fewer than threshold days away,
   no bottom sheet is shown and the flow proceeds as today.
3. The sheet shows: title "Your invoice is due on dd.mm.yyyy." / "Deine
   Rechnung ist am dd.mm.yyyy fällig." (date formatted `dd.MM.yyyy`), a
   description, CTA "Cancel Transfer" / "Überweisung abbrechen" and primary
   CTA "Proceed Anyway" / "Trotzdem fortfahren". Final copy comes from Figma
   (EN + DE); the CTA strings `gc_cancel_transfer` / `gc_proceed_anyway`
   already exist.
4. "Proceed Anyway" dismisses the sheet and continues the flow exactly where
   the old hint's dismissal continued it (invoice-saving handling, then
   `onExtractionsAvailable`).
5. "Cancel Transfer" cancels the transaction: clears stored images and invokes
   `CancelListener.onCancelFlow()` (returns control to the bank app) — same
   behavior as the already-paid warning's cancel action.
6. The sheet is not dismissable by tapping outside, dragging, or the back
   gesture; only the two CTAs close it. No auto-dismiss and no progress bar.
7. While a warning bottom sheet (due date hint OR already-paid) is visible,
   the rotating capture suggestions (`AnalysisHintsAnimator`) are stopped and
   hidden, so TalkBack focus stays on the sheet. (Decision: apply to both
   sheets, not just the new one.)
8. The legacy inline due date hint UI is removed entirely:
   `PaymentDueHintContent.kt`, `PaymentDueHintColors.kt`,
   `PaymentDueHintDismissListener.kt`, the
   `gc_payment_due_hint_container` ComposeView in all three
   `gc_fragment_analysis.xml` variants, and the strings
   `gc_due_date_hint`, `gc_due_date_hint_tip`, `gc_dismiss_message`.
   (Decision: full removal, no deprecation cycle.)
9. `paymentScheduleHintEnabled` is NOT introduced in this ticket (decision:
   it belongs to the future schedule-hint ticket). Display logic depends only
   on the existing due-date-hint flags.
10. Accessibility: dynamic font sizes without truncation (scrollable text
    area), TalkBack reads title → description → CTAs with focus moving to the
    sheet on appearance (accessibility pane title), correct landscape
    rendering, WCAG AA contrast, external-keyboard operable. The existing
    `WarningBottomSheet` already implements these behaviors; the new state
    must preserve them.

## Affected modules

- `capture-sdk:sdk` — all code changes (Analysis screen, warning bottom
  sheet, strings, layouts, tests).
- `bank-sdk:sdk` — no code change expected; `CaptureConfiguration.
  paymentDueHintEnabled` / `paymentDueHintThresholdDays` pass-through stays
  as is. bank-sdk consumes capture-sdk as a project dependency, so its checks
  must still pass.
- No changes in the API libraries (`paymentDueHintEnabled` already exists in
  `bank-api-library`'s `ConfigurationResponse`).

## Public API impact

- No changes to the integrator-facing configuration API (`GiniCapture.Builder`
  / `CaptureConfiguration` flags stay unchanged).
- **Removals (source-breaking in theory, accepted by decision):**
  - `net.gini.android.capture.analysis.paymentDueHint.ui.PaymentDueHintContent`
    (public `@Composable`) and its public helpers (`TipCard`, `DismissCard`,
    `AnimatedProgressBar`)
  - `net.gini.android.capture.analysis.paymentDueHint.colors.PaymentDueHintColors`
  - `net.gini.android.capture.analysis.paymentDueHint.PaymentDueHintDismissListener`
  - the `gc_payment_due_hint_container` view id and the removed string
    resources
  These are implementation details of the hint that integrators should not
  reference; all usages are inside `capture-sdk:sdk`.
- `WarningType` (public enum) gains a new constant — additive.
- `AnalysisFragmentExtension.showPaymentDueHint` (public today because the
  class is not `internal`) is removed. `AnalysisScreenContract` is
  package-private abstract — internal changes there are not integrator-visible.

## Technical conventions

1. **Language:** Kotlin for any new class. The following legacy Java files
   may be touched, only at their existing seams (no opportunistic
   conversion): `AnalysisScreenPresenter.java` (swap the view call in the
   `SUCCESS_WITH_EXTRACTIONS` branch), `AnalysisScreenContract.java`
   (change/remove the `showPaymentDueHint` abstract method signature),
   `AnalysisFragmentImpl.java` (view method implementation + hints-animator
   stop), `AnalysisFragment.java` (warning listener reuse),
   `WarningType.java` (new enum constant). New declarations `internal`
   unless the file's existing declarations are public by necessity
   (`WarningType` is public Java — keep it consistent).
2. **UI:** No new Compose UI — the sheet reuses the existing ViewBinding XML
   `gc_warning_bottom_sheet.xml` (`WarningBottomSheet` is a
   `BottomSheetDialogFragment`, `AlertDialog` on tablets, themed via
   `getLayoutInflaterWithGiniCaptureTheme`). Compose UI is *removed*
   (`PaymentDueHintContent`). XML changes: remove the
   `gc_payment_due_hint_container` ComposeView from
   `layout/gc_fragment_analysis.xml`, `layout-sw600dp/…`,
   `layout-sw600dp-land/…`. Keep vector-drawable handling as is.
3. **Architecture:** The Analysis screen is legacy Java MVP
   (`AnalysisScreenContract.View`/`Presenter` + Kotlin extension classes).
   Integrate at the contract boundary exactly like the already-paid warning:
   presenter decides (`shouldShowPaymentDueHint`), extension handles
   education-mutex/invoice-saving sequencing
   (`AnalysisScreenPresenterExtension`), view shows the sheet
   (`AnalysisFragmentImpl` → `AnalysisFragment.showWarning`). No new
   ViewModel, no MVI.
4. **DI/async:** No new dependencies to wire. Existing Koin-injected use
   cases (`GetPaymentDueHintEnabledUseCase`) stay untouched. No
   LiveData/RxJava.
5. **Strings/resources:** New strings in `values/strings.xml` (German — the
   default locale in capture-sdk) and `values-en/strings.xml`:
   `gc_due_date_hint_title` with a `%1$s` date placeholder ("Deine Rechnung
   ist am %1$s fällig." / "Your invoice is due on %1$s.") and
   `gc_due_date_hint_desc` (copy from Figma). Remove `gc_due_date_hint`,
   `gc_due_date_hint_tip`, `gc_dismiss_message` from both locale files.
   `gc_cancel_transfer` / `gc_proceed_anyway` already exist and are used by
   the shared layout.
6. **Quality gates:** `./gradlew capture-sdk:sdk:ktlintCheck`, `detekt`,
   `lint`, `testDebugUnitTest` all clean (use `/gini-check`); new/changed
   Kotlin classes need unit-test coverage for Sonar.

## Design

Flow today (`AnalysisScreenPresenter.java:360-368`): on
`SUCCESS_WITH_EXTRACTIONS`, `shouldShowPaymentDueHint(resultHolder)` →
`extension.showPaymentDueHint(...)` →
`view.showPaymentDueHint(dismissListener, dueDate)` →
`AnalysisFragmentExtension.showPaymentDueHint()` renders the inline Compose
card which auto-dismisses and then resumes the flow.

New flow — mirror the already-paid path
(`AnalysisScreenPresenterExtension.showAlreadyPaidHint`,
`AnalysisFragmentImpl.showAlreadyPaidWarning`,
`AnalysisFragment.showWarning`):

1. **`WarningType.java`:** add constant `PAYMENT_DUE_DATE(R.string.gc_due_date_hint_title,
   R.string.gc_due_date_hint_desc, …)`. Each type also declares its primary and
   secondary CTA label (`primaryButtonTextRes` / `secondaryButtonTextRes`): for the
   due date sheet "Proceed Anyway" is the primary CTA (filled, first button) and
   "Cancel Transfer" the secondary one — the reverse of the already-paid sheet, which
   keeps its existing hierarchy. `WarningBottomSheet` is CTA-semantics-free: its
   buttons are `primary_button`/`secondary_button` with a generic
   `Listener.onPrimaryAction()/onSecondaryAction()`, and
   `AnalysisFragment.makeWarningListener(type, onProceed)` maps them to the actual
   behaviors (proceed vs. cancel transaction) per type — future warning states
   (e.g. Schedule Payment) only add a label pair and a mapping case.
2. **`WarningBottomSheet.kt`:** support a title format argument. Extend
   `newInstance(type: WarningType, titleFormatArg: String? = null)` to store
   the formatted date in the arguments `Bundle`; in `onCreate`, build
   `titleText = if (arg != null) getString(type.titleRes, arg) else
   getString(type.titleRes)`. Everything else (tablet dialog, non-dismissable
   behavior, expanded state, landscape handling, accessibility pane title,
   CTA wiring) is reused unchanged.
3. **`AnalysisScreenContract.View`:** replace
   `showPaymentDueHint(PaymentDueHintDismissListener, String)` with
   `showPaymentDueHint(String formattedDueDate, Runnable onProceed)` (or fold
   into a generalized `showWarning(WarningType, String?, Runnable)` shared
   with `showAlreadyPaidWarning` — builder's choice, keep it minimal).
4. **`AnalysisScreenPresenterExtension.showPaymentDueHint`:** keep the
   education-mutex + invoice-saving sequencing exactly as it is; only the
   `view.show...` call changes to the new signature. The `onProceed` runnable
   is the existing `handleSaveInvoicesLocally(...)` continuation.
5. **`AnalysisFragmentImpl`:** implement the new view method by delegating to
   `mFragment.showWarning(WarningType.PAYMENT_DUE_DATE, formattedDate,
   onProceed)`; `AnalysisFragment.showWarning` passes the format arg to
   `WarningBottomSheet.newInstance`. Cancel is already handled by
   `makeWarningListener` (`ImageDiskStore.clear` + `mCancelListener.onCancelFlow()`)
   — requirement 5 for free.
6. **Date formatting:** reuse the `formatDateToLocalStyle` logic
   (`yyyy-MM-dd` → `dd.MM.yyyy`, fallback to raw value) — move it out of
   `AnalysisFragmentExtension` (which loses its only other hint duty) into a
   small internal helper or onto the presenter extension, and call it before
   handing the date to the view.
7. **Hints suppression (req. 7):** in `AnalysisFragmentImpl`, stop the hints
   when showing either warning sheet — call `mHintsAnimator.stop()` and hide
   the hint container in `showAlreadyPaidWarning(...)` and the new
   `showPaymentDueHint(...)` before presenting the sheet
   (`AnalysisHintsAnimator` already exposes `stop()`; it hides the container
   views itself — verify and hide explicitly if not).
8. **Removals (req. 8):** delete the `paymentDueHint/ui` and
   `paymentDueHint/colors` packages and `PaymentDueHintDismissListener.kt`;
   remove `paymentDueHintView` binding and `showPaymentDueHint` from
   `AnalysisFragmentExtension.kt`; remove the ComposeView from the three
   layout variants; remove the three obsolete strings from both locale files.

State restoration: `WarningBottomSheet` is a `DialogFragment`, so it is
recreated on rotation; the title format arg lives in the arguments `Bundle`
and survives. The listener is re-attached the same way the already-paid sheet
handles it today (`AnalysisFragment.showWarning` looks up the fragment by tag
— verify the listener is re-set on recreation, matching existing behavior).

## Test plan

Test stack: match neighbors in `capture-sdk/sdk/src/test/java/net/gini/android/capture/analysis/`
— Robolectric (`AndroidJUnit4`), Mockito-Kotlin + MockK, Google Truth, JUnit4.

- **`AnalysisScreenPresenterTest.kt`** (extend existing): requirements 1, 2, 9
  - shows the due-date sheet when both flags on, `paymentDueDate` ≥ threshold
    days away, payment state to-be-paid
  - does NOT show it when: due date today / past / < threshold, date missing
    or unparseable, either flag off, CX mode, RA/Skonto extractions present,
    already-paid takes precedence
  - proceed continuation reaches `onExtractionsAvailable` with unchanged
    extractions
- **`WarningBottomSheetTest.kt`** (new, Robolectric): requirements 3, 6
  - title built with the format arg for `PAYMENT_DUE_DATE`; plain title for
    types without arg
  - sheet is not cancelable (`isCancelable == false`, canceled-on-touch-outside false)
  - CTA clicks invoke `Listener.onProceedAction` / `onCancelAction` and dismiss
- **Date formatting helper test** (new `<Helper>Test.kt`): `yyyy-MM-dd` →
  `dd.MM.yyyy`, fallback on garbage input — port the existing behavior of
  `AnalysisFragmentExtension.formatDateToLocalStyle`.
- **`AnalysisFragmentImplTest.java`** (extend if practical): requirement 7 —
  hints animator stopped when a warning sheet is shown.
- Every new Kotlin class gets a unit test (platform rule).

Manual QA (not automatable here): TalkBack focus order and announcement,
dynamic font scaling without truncation, landscape rendering on phone +
tablet dialog variant, WCAG AA contrast against the Figma tokens, external
keyboard navigation, end-to-end cancel returning to the host app
(`bank-sdk:example-app`, e.g. `assembleDevExampleAppDebug`).

## Out of scope

- `paymentScheduleHintEnabled` flag and the scheduled-payment hint bottom
  sheet (separate ticket).
- Any change to the trigger business logic (threshold semantics, extraction
  parsing, payment-state detection) or to the client-configuration plumbing.
- iOS counterpart (separate ticket in gini-mobile-ios).
- Analytics/tracking events for the new sheet (ticket doesn't request any).
- Deprecation shims for the removed public Composables (full removal decided).

## Open questions

- ~~Final EN/DE copy for the sheet **description** (and confirmation of the
  title punctuation) must be taken from the Figma link in the ticket
  (node 32630-16573) before or during `/gini-build`; `gc_due_date_hint_desc`
  is a placeholder name until then.~~ **Resolved during /gini-build:** copy
  taken from the Confluence page "Scheduled Payment — Due Date Hint Redesign"
  (page 1775435791), Copys section: EN "You could set it up as a scheduled
  transfer." / DE "Du könntest sie als Terminüberweisung anlegen."; title
  keeps the trailing period.

# PP-3264: [Android] Show Schedule Payment state of the bottom sheet

Status: implemented
Ticket: https://ginis.atlassian.net/browse/PP-3264
Epic: PP-3232 — Due Date Redesign: Enable Scheduled Payments
Builds on: PP-3262 (`specs/PP-3262-feature.md`) — branch
`PP-3264-show-schedule-payment-state-of-bottom-sheet` is cut from
`origin/PP-3262-redesign-due-date-hint-as-bottom-sheet`, **not** from `main`.
PP-3262 must land first (or be merged into this branch) before this ships.

## Problem

PP-3262 moved the due date hint into a modal bottom sheet with two CTAs
("Cancel Transfer" / "Proceed Anyway"). That sheet was always designed to have
**two states**; PP-3262 shipped only the first.

Sparkasse (and any bank that supports scheduled transfers) does not want the
"cancel or proceed" choice — they want to route the customer into their own
scheduled-payment flow, carrying the extraction data. Today the SDK has no way
to express "the user asked to schedule this instead of paying now": the capture
flow can only finish with `Success` (pay now), `Cancel`, `Empty`, `Error` or
`EnterManually`.

PP-3264 adds the second state of the same sheet — **Schedule Payment** — gated
on a new backend flag `paymentScheduleHintEnabled`, plus a new terminal result
that hands the extractions back to the bank app.

## Requirements

1. Given `paymentScheduleHintEnabled = true` (Gini API `/configurations`) and
   the SDK flag `GiniCapture.isPaymentScheduleHintEnabled()`, when analysis
   returns extractions with a `paymentDueDate` whose remaining days are
   ≥ `GiniCapture.getPaymentDueHintThresholdDays()` (default 5) and the payment
   state is "to be paid", the bottom sheet appears in the **Schedule Payment**
   state.
2. The Schedule Payment state takes **priority over** the Due Date Hint state:
   it shows regardless of `paymentDueHintEnabled`. The due-date branch is only
   reached when the schedule flag is off.
3. If the due date is today, in the past, or fewer than threshold days away, no
   sheet is shown — identical to the due-date state. All existing exclusions are
   preserved unchanged: CX mode, Return Assistant / Skonto extractions present,
   empty or unparseable `paymentDueDate`, payment state not "to be paid".
4. The already-paid warning keeps its current top priority — a document marked
   as paid must not offer scheduling. Order in the presenter chain becomes:
   already-paid → schedule payment → due date hint → proceed.
   *(Decision: not stated in the ticket; paying/scheduling an already-paid
   invoice is never correct.)*
5. Sheet content (see **Copy** below): title identical to the due-date state
   ("Your invoice is due on dd.mm.yyyy." / "Deine Rechnung ist am dd.mm.yyyy
   fällig."), a state-specific description, primary CTA1 "Schedule Payment" /
   "Terminüberweisung", secondary CTA2 "Proceed Anyway" / "Trotzdem fortfahren".
6. Tapping **"Schedule Payment"** finishes the capture flow with a new terminal
   result carrying the extraction data, so the bank app can open its own
   scheduled-transfer screen. It does **not** continue into the pay-now flow.
7. Tapping **"Proceed Anyway"** dismisses the sheet and continues the
   transaction exactly as the due-date state's "Proceed Anyway" does
   (invoice-saving handling, then `onExtractionsAvailable`).
8. The sheet is not dismissable by tapping outside, dragging, or the back
   gesture; only the two CTAs close it. No auto-dismiss, no progress bar.
   *(Already guaranteed by `WarningBottomSheet` — must not regress.)*
9. While the sheet is shown, the rotating capture suggestions are stopped and
   hidden. *(Already implemented in PP-3262 for every `WarningType`; the new
   state inherits it for free — verify, don't re-implement.)*
10. Accessibility: dynamic font sizes without truncation, TalkBack reads title →
    description → CTAs with focus moving to the sheet, correct landscape
    rendering, WCAG AA contrast, external-keyboard operable. All inherited from
    `WarningBottomSheet`; the longer description (2 extra lines at 200% font per
    Figma) is the one real risk — verify no truncation.

## Copy

**Authoritative source: Figma** ("All the text should be taken from Figma" —
Confluence). Confirmed against the Android section
[`32630-16572`](https://www.figma.com/design/ZYpdKfpaHOpV7RV1TTWdEH/Gini-Photo-Payment--iOS---Android?node-id=32630-16572)
— the Schedule Payment variants are the lower half of that section (y ≈ 4566+),
the Due Date variants the upper half (y ≈ 866+).

| | Due Date state (PP-3262, exists) | Schedule Payment state (new) |
|---|---|---|
| Title EN | `Your invoice is due on %1$s.` | **identical — reuse** |
| Title DE | `Deine Rechnung ist am %1$s fällig.` | **identical — reuse** |
| Desc EN | You could set it up as a scheduled transfer. | You can proceed with this Transaction now or schedule it for a future execution. |
| Desc DE | Du könntest sie als Terminüberweisung anlegen. | Du könntest diese Überweisung jetzt ausführen oder für einen späteren Zeitpunkt terminieren. |
| CTA1 (primary) | Proceed Anyway | **Schedule Payment** / **Terminüberweisung** |
| CTA2 (secondary) | Cancel Transfer | Proceed Anyway / Trotzdem fortfahren |

Two notes, both resolved against Figma:

- The ticket's Figma link (node `32630-16574`) resolves to a **"File Tags"
  label**, not the sheet — it is not a usable design reference. Use
  `32630-16572` (the section) or the phone frame `32698:44164`.
- The Confluence *Copys* section lists the schedule-state EN title as
  "Your invoice is due on 30 July 2026" (no trailing period, spelled-out date).
  Figma shows `Your invoice is due on dd.mm.yyyy.` in **both** states, and the DE
  title is byte-identical across states. **Decision: reuse
  `gc_due_date_hint_title` verbatim and the existing `DueDateFormatter`
  (`dd.MM.yyyy`).** No new title string, no second date format. Confluence's
  variant is an authoring inconsistency.

New string resources (both `values/strings.xml` = DE default and
`values-en/strings.xml`):

- `gc_schedule_payment_hint_desc` — the description above
- `gc_schedule_payment` — CTA1 label ("Schedule Payment" / "Terminüberweisung")

Reused as-is: `gc_due_date_hint_title`, `gc_proceed_anyway`.

## Decisions taken (confirmed with the assignee)

1. **Hand-off mechanism: a new terminal result variant.** Add
   `CaptureSDKResult.SchedulePayment` (capture-sdk) and
   `CaptureResult.SchedulePayment` (bank-sdk), parallel to the existing
   `Success` / `EnterManually` variants. Chosen over a `GiniBank` callback
   (lifecycle/leak risk, does not fit the `ActivityResultContract` path) and
   over a `scheduleRequested: Boolean` on `Success` (source-breaking, and
   silently pays now for hosts that ignore the flag).
2. **Threshold: reuse `paymentDueHintThresholdDays` as-is, no clamping.**
   Confluence floats enforcing a minimum of 5 days for the schedule flow, but
   only as an open proposal, and "the business logic will not be changed".
   Integrator overrides are honoured. The weekend / bank-holiday concern is a
   follow-up ticket, not this one.
3. **Flag surface: mirror `paymentDueHintEnabled` fully** — backend
   `/configurations` flag *and* an SDK-side `GiniCapture.Builder` setter +
   bank-sdk `CaptureConfiguration` field, gated on both. Consistent with every
   other hint flag, and gives QA a local toggle in the example app.

## Affected modules

| Module | Change |
|---|---|
| `bank-api-library:library` | `paymentScheduleHintEnabled` in `ConfigurationResponse` + `Configuration` |
| `capture-sdk:default-network` | map the new field in `GiniCaptureDefaultNetworkService` |
| `capture-sdk:sdk` | config plumbing, use case, presenter branch, `WarningType`, new result variant, listener method, strings |
| `bank-sdk:sdk` | `CaptureConfiguration` flag pass-through, `CaptureResult.SchedulePayment`, result mapping |
| `bank-sdk:example-app` | QA toggle for the new flag |

Because `capture-sdk:sdk` is a project dependency of `bank-sdk:sdk`, the
bank-sdk checks must pass too.

## Public API impact

All changes are **additive** — no removals, no changed signatures on existing
integrator-facing types.

- `CaptureSDKResult.SchedulePayment` (new `@Parcelize` subclass) — capture-sdk.
- `CaptureResult.SchedulePayment` (new subclass) — bank-sdk. **Integrators with
  an exhaustive `when` over `CaptureResult` will get a compile error until they
  add a branch.** This is the intended, visible-by-design consequence of
  decision 1 and must be called out in the changelog / migration notes.
- `GiniCapture.Builder.setPaymentScheduleHintEnabled(Boolean)` and
  `GiniCapture.isPaymentScheduleHintEnabled()`.
- `CaptureConfiguration.paymentScheduleHintEnabled: Boolean = true` (new field
  with a default — Kotlin data-class copy/constructor stays source-compatible
  for named-argument callers).
- `WarningType.SCHEDULE_PAYMENT` — new enum constant, additive.
- `AnalysisFragmentListener.onSchedulePayment(...)` — new method. Give it a
  default implementation, or accept that it is a Java interface others may
  implement; it is documented as internal-use in practice, but confirm during
  build.

## Technical conventions

1. **Language:** Kotlin for new classes. Legacy Java files touched only at
   existing seams, no opportunistic conversion: `AnalysisScreenPresenter.java`,
   `AnalysisScreenContract.java`, `AnalysisFragmentImpl.java`,
   `AnalysisFragment.java`, `WarningType.java`, `GiniCapture.java`,
   `AnalysisFragmentListener.java`.
2. **UI: no new layout, no new Compose.** The Schedule Payment state reuses
   `gc_warning_bottom_sheet.xml` and all four of its configuration variants
   unchanged. Figma confirms the two states are structurally identical (same
   `alert-circle` icon, same title/description container, same two buttons) —
   only text differs. This is exactly the extension point PP-3262 built:
   "future warning states (e.g. Schedule Payment) only add a label pair and a
   mapping case".
3. **Architecture:** legacy Java MVP on the Analysis screen. Integrate at the
   contract boundary, mirroring `showPaymentDueHint`. No new ViewModel.
4. **DI:** one new Koin-provided use case in `paymentHintsModule.kt`; no new
   dependencies.
5. **Quality gates:** `/gini-check` for `capture-sdk:sdk`, `bank-sdk:sdk`,
   `bank-api-library:library`, `capture-sdk:default-network` — `ktlintCheck`,
   `detekt`, `lint`, `testDebugUnitTest` all clean. Every new Kotlin class needs
   a unit test for Sonar.

## Design

### Step 1 — config flag plumbing (mirror `isPaymentDueHintEnabled` exactly)

| File | Change |
|---|---|
| `bank-api-library/.../response/ConfigurationResponse.kt` | `@Json(name = "paymentScheduleHintEnabled") val paymentScheduleHintEnabled: Boolean?` + `isPaymentScheduleHintEnabled = paymentScheduleHintEnabled ?: false` in the mapper |
| `bank-api-library/.../models/Configuration.kt` | `val isPaymentScheduleHintEnabled: Boolean` |
| `capture-sdk/default-network/.../GiniCaptureDefaultNetworkService.kt` | map the field alongside line ~210 |
| `capture-sdk/.../internal/network/Configuration.kt` | `val isPaymentScheduleHintEnabled: Boolean` |
| `capture-sdk/.../internal/storage/ClientConfigurationStorage.kt` | new `booleanPreferencesKey("is_payment_schedule_hint_enabled")`, read in `getConfiguration()`, write in `saveConfiguration()` |
| `capture-sdk/.../internal/provider/GiniBankConfigurationProvider.kt` | `isPaymentScheduleHintEnabled = false` in the default `Configuration` |
| `capture-sdk/.../paymentHints/GetPaymentScheduleHintEnabledUseCase.kt` | **new** — one-liner mirroring `GetPaymentDueHintEnabledUseCase` |
| `capture-sdk/.../di/paymentHintsModule.kt` | register the new use case |
| `capture-sdk/.../GiniCapture.java` | field, getter, `Builder` setter + private getter, wire in the constructor (default `true`, like the due hint) |
| `bank-sdk/.../capture/Configuration.kt` | `paymentScheduleHintEnabled: Boolean = true` + `.setPaymentScheduleHintEnabled(...)` in the mapper |
| `bank-sdk/example-app/...` | toggle in `layout_feature_toggles.xml`, `ExampleAppBankConfiguration`, `ConfigurationActivity`, `ConfigurationViewModel` |

Note `ClientConfigurationStorageTest.kt` (androidTest) asserts the persisted
configuration — it will need the new field.

### Step 2 — `WarningType.SCHEDULE_PAYMENT`

```java
SCHEDULE_PAYMENT(
        R.string.gc_due_date_hint_title,          // reused, takes the %1$s date
        R.string.gc_schedule_payment_hint_desc,
        R.string.gc_schedule_payment,             // primary CTA1
        R.string.gc_proceed_anyway                // secondary CTA2
);
```

No change to `WarningBottomSheet.kt` — it is already CTA-semantics-free and
already supports a title format argument via
`newInstance(type, titleFormatArg)`.

### Step 3 — presenter decision + priority

`AnalysisScreenPresenter.java`, `SUCCESS_WITH_EXTRACTIONS` branch (~line 348).
Insert a new `else if` **between** the already-paid and due-date branches:

```java
} else if (shouldShowAlreadyPaidInvoiceWarning(resultHolder)) { ...
} else if (shouldShowSchedulePaymentHint(resultHolder)) {     // NEW
    successResultHolder = resultHolder;
    shouldClearImageCaches = false;
    extension.showSchedulePaymentHint(
            resultHolder,
            extractPaymentDueDateFromExtraction(resultHolder),
            mIsInvoiceSavingEnabled,
            isSavingInvoicesInProgress,
            getActivity());
} else if (shouldShowPaymentDueHint(resultHolder)) { ...
```

`shouldShowSchedulePaymentHint` is a copy of `shouldShowPaymentDueHint` with the
flag pair swapped for the schedule flags — deliberately **not** reading
`paymentDueHintEnabled` (requirement 2). Extract the shared tail (CX mode,
RA/Skonto, empty date, threshold, `state.toBePaid()`) into one private helper so
the two predicates cannot drift; that keeps Detekt's duplication rules happy
too.

### Step 4 — view contract + sheet wiring

- `AnalysisScreenContract.View`: add
  `showSchedulePaymentHint(String formattedDueDate, Runnable onProceed, Runnable onSchedule)`.
- `AnalysisScreenPresenterExtension.showSchedulePaymentHint(...)`: copy of
  `showPaymentDueHint`, same education-mutex + invoice-saving sequencing.
  `onProceed` = the existing `handleSaveInvoicesLocally(...)` continuation;
  `onSchedule` = `getAnalysisFragmentListenerOrNoOp().onSchedulePayment(extractions, compoundExtractions, returnReasons)`
  built from `resultHolder`. **The extraction data never has to travel through
  the Fragment** — it stays in the presenter, which already holds it.
- `AnalysisFragmentImpl`: delegate to
  `mFragment.showWarning(WarningType.SCHEDULE_PAYMENT, formattedDate, onProceed, onSchedule)`.
- `FragmentImplCallback.showWarning` / `AnalysisFragment.showWarning`: the
  schedule state needs a **second** action runnable (its primary CTA is neither
  "proceed" nor "cancel transaction"). Preferred shape: generalise to
  `showWarning(type, titleFormatArg, onPrimary, onSecondary)` and move the
  per-type CTA mapping up into `AnalysisFragmentImpl`, deleting the
  `makeWarningListener(type, …)` switch. Fallback if that ripples too far: keep
  the switch and add a nullable `onSchedule` parameter with a
  `case SCHEDULE_PAYMENT` arm. Either way `cancelTransaction()`
  (`ImageDiskStore.clear` + `mCancelListener.onCancelFlow()`) stays untouched.

### Step 5 — the hand-off result

```
WarningBottomSheet primary CTA
  → onSchedule runnable (presenter extension)
  → AnalysisFragmentListener.onSchedulePayment(specific, compound, returnReasons)   [new]
  → GiniCaptureFragment.onSchedulePayment(...)                                       [new override]
  → giniCaptureFragmentListener.onFinishedWithResult(CaptureSDKResult.SchedulePayment(...))
  → bank-sdk CaptureFlowFragment: CaptureSDKResult → CaptureResult mapping
  → CaptureResult.SchedulePayment(...) → host app
```

Files: `AnalysisFragmentListener.java` (new method),
`AnalysisFragmentImpl`/presenter-extension wiring, `GiniCaptureFragment.kt`
(new override next to `onExtractionsAvailable`, ~line 293),
`CaptureSDKResult.kt` (new `@Parcelize` variant),
`bank-sdk/CaptureResult.kt` (new variant + a branch in
`CaptureSDKResult.toCaptureResult()`).

The `Intent` round-trip needs no change: `CaptureFlowActivity` parcels the whole
`CaptureResult` into `EXTRA_OUT_RESULT` and `internalParseResult` reads it back
generically, so a new `@Parcelize` variant travels for free. `toIntent()` is
`Success`-specific and is not on this path.

## Test plan

Stack: Robolectric (`AndroidJUnit4`), MockK + Mockito-Kotlin, Google Truth,
JUnit4 — match the neighbours in
`capture-sdk/sdk/src/test/java/net/gini/android/capture/analysis/`.

- **`AnalysisScreenPresenterTest.kt`** (extend; PP-3262 already added 346 lines
  here) — requirements 1–4:
  - shows the schedule state when the schedule flag pair is on, date ≥ threshold,
    state to-be-paid
  - shows the schedule state **with `paymentDueHintEnabled = false`** (the
    priority rule — the highest-value test in this ticket)
  - shows the schedule state, not the due-date state, when **both** flags are on
  - falls back to the due-date state when only the due flag is on
  - shows **nothing** when: due date today / past / < threshold, date missing or
    unparseable, either schedule flag off with the due flag also off, CX mode,
    RA/Skonto extractions present
  - already-paid still wins over schedule
  - the schedule CTA reaches `onSchedulePayment` with the unchanged extractions,
    and does **not** reach `onExtractionsAvailable`
  - "Proceed Anyway" reaches `onExtractionsAvailable` with unchanged extractions
- **`WarningBottomSheetTest.kt`** (extend) — requirements 5, 8: title built from
  `gc_due_date_hint_title` + format arg for `SCHEDULE_PAYMENT`; CTA labels come
  from the new enum constant in the right order; sheet not cancelable; CTA taps
  fire `onPrimaryAction`/`onSecondaryAction` and dismiss.
- **`GetPaymentScheduleHintEnabledUseCaseTest.kt`** (new) — flag read-through.
- **`ClientConfigurationStorageTest.kt`** (androidTest, extend) — new field
  persists and reloads.
- **`GiniCaptureDefaultNetworkServiceTest.kt`** (extend) — response field maps
  through.
- **`AnalysisFragmentTest.kt` / `AnalysisFragmentImplTest.java`** (extend) —
  the CTA-to-action mapping for `SCHEDULE_PAYMENT`, and requirement 9 (hints
  animator stopped) for the new type.
- **Result plumbing** — a unit test that `CaptureSDKResult.SchedulePayment`
  maps to `CaptureResult.SchedulePayment` in `toCaptureResult()`.

Manual QA (not automatable here), on `bank-sdk:example-app`
(`assembleDevExampleAppDebug`, toggle the new flag in the config screen):
TalkBack focus order and announcement with the longer description; font scaling
at 200% in both locales (the DE description is the longest string in the sheet —
the primary truncation risk); landscape on phone; tablet `AlertDialog` variant;
WCAG AA contrast against the Figma tokens; external keyboard; and the
end-to-end hand-off — tapping "Schedule Payment" returns
`CaptureResult.SchedulePayment` with the full extraction set to the example app.

## Out of scope

- Enforcing a minimum threshold of 5 days for the schedule flow (Confluence
  proposal — see decision 2). Follow-up ticket.
- Any change to the trigger business logic: threshold semantics, extraction
  parsing, payment-state detection.
- The bank-side scheduled-payment screen itself — banks implement that.
- iOS counterpart (separate ticket in gini-mobile-ios).
- Analytics/tracking events for the new state (not requested).
- Integration-guide / changelog documentation for the new `CaptureResult`
  variant — needed before release, but tracked separately.

## Notes from the build

- **Design step 4 took the documented fallback**, not the "preferred" shape. Generalising
  `showWarning` and moving CTA mapping into `AnalysisFragmentImpl` would have required exposing
  `cancelTransaction()` (which needs `getActivity()` + `mCancelListener`) through
  `FragmentImplCallback` — pushing fragment-lifecycle concerns into the impl. Instead
  `showWarning` gained a 4-arg overload (`onSchedule`) and `makeWarningListener` gained a
  `SCHEDULE_PAYMENT` arm, exactly as PP-3262 predicted ("only add a label pair and a mapping
  case"). The 3-arg overload delegates with `onSchedule = null`, so no existing call site or test
  changed.
- **The shared predicate reorders the flag checks.** `isDueDateBottomSheetEligible` runs after the
  flag pair instead of before it, so `bankSDKBridge.getBankSDKProperties()` is no longer called
  when the flags are off. Outcome-equivalent, strictly fewer bridge calls.
- **The new `CaptureResult` variant broke our own example app** in three places
  (`MainActivity`, `ClientBankSDKFragment`, `CaptureResultListener`) — the exhaustive-`when` break
  predicted under "Public API impact", demonstrated in-repo. All three now branch on
  `SchedulePayment` and are the reference for integrators. This confirms the changelog /
  migration note is required before release.
- **`GetPaymentScheduleHintEnabledUseCase` needed no `noOpListener` change**:
  `AnalysisFragmentListener.onSchedulePayment` was given a `default` no-op, so the anonymous
  no-op listener in `AnalysisScreenPresenterExtension` and any external implementor keep
  compiling. `GiniCaptureFragment` overrides it explicitly and is covered by tests.

## Observations found while planning (not in scope)

- `AnalysisScreenPresenter.java` `case SUCCESS_WITH_EXTRACTIONS:` has **no
  `break;`** and falls through into `case NO_NETWORK_SERVICE:`. Harmless today
  because that case body is a bare `break`, but it is a latent fall-through
  directly in the block this ticket edits. Worth a `break;` while touching it —
  flag to the reviewer rather than smuggling it in.
- The DE CTA strings shipped by PP-3262 are title-cased
  ("Überweisung **A**bbrechen", "Trotzdem **F**ortfahren") while Confluence and
  Figma specify sentence case ("Überweisung abbrechen", "Trotzdem fortfahren").
  Pre-existing, affects the shared CTA reused by this state. Raise with the
  ticket reporter; a one-line fix if confirmed.

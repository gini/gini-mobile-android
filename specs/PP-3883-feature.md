# PP-3883: [Android] UI Automation for Xray Onboarding Screens Test Suite

Status: implementing (4/5)
Ticket: https://ginis.atlassian.net/browse/PP-3883
Related: PP-3884 (iOS twin, Backlog) · PP-2671 (epic "Mobile Automation Expansion - UI") ·
precedent spec `specs/PP-3920-feature.md` on `main` (No Result screen suite) ·
Xray folder `/Banking Project/Onboarding Screens` (5 tests, PP-3410..PP-3414)

## Problem

The Xray folder "Onboarding Screens" holds five manual cases for the capture-sdk onboarding
(`OnboardingFragment`, four pages in a `ViewPager`). The PM wants all of them run as UI automation on
BrowserStack, aligned with iOS, so the onboarding no longer needs a manual pass every release.

Facts that shape the work (verified on 2026-11-20):

1. **The five Xray cases have titles only.** Xray returns no steps for PP-3410..PP-3414 and their Jira
   descriptions are empty. Their wording matches the smoke case PP-3415 (`/Banking Project/Smoke Test
   Suite 2026`), which has full steps. By decision, this spec derives the steps from the titles plus
   PP-3415's step text. Xray is not changed.
2. **PP-3410 is already automated.** `OnboardingScreenTests` (`test1`–`test7`) walks pages 1→4 with Next
   and checks every title and the Next / Skip / Get Started buttons. This is the ticket's
   "Already automated: 1".
3. **The four cases to build** are PP-3411 (swipe back and forth) and PP-3412 / PP-3413 / PP-3414
   (Skip on page 1 / 2 / 3 opens the camera). Nothing in the suite swipes the pager or asserts where Skip
   lands. `OnboardingScreenTests.test8` taps Skip on page 1, but it then presses Back and checks a
   *second* launch. It never checks the camera right after Skip.
4. **Every test starts from a fresh install.** The orchestrator runs with `clearPackageData=true`
   locally (`bank-sdk/example-app/build.gradle.kts:88`) and on BrowserStack (`bs_build_and_upload.sh`).
   So the onboarding is shown on the first tap of the photo-payment button in every test. This is the
   "Fresh Install" precondition in each case title.
5. **Repeated taps on one button are debounced.** Next, Skip and Get Started use
   `setIntervalClickListener` (`OnboardingFragment.java:152-154`). `IntervalClickListener` ignores a
   second tap on the *same* button within `interval = 500L` ms (`IntervalClickListenerHelper.kt:11`).
   The flag is internal, so a test cannot observe it. Two Next taps in a row must be at least 500 ms
   apart, or the second tap is silently lost. This is why `OnboardingScreenTests` puts
   `idlingResource.waitForIdle()` between Next taps.
6. **Nothing here needs the network or the camera image.** The camera screen is only checked by its
   shutter button. The class needs no network and no `RetryRule`.

## Xray case mapping (the deliverable table)

| Xray | Case (title) | Decision | Android test (class.method) | iOS today (PP-3884) |
| --- | --- | --- | --- | --- |
| PP-3410 | View every onboarding screen | automated, BrowserStack (**existing**) | `OnboardingScreenTests.test1`–`test7` (+ `OnboardingNavigationTests.test5` for the Get Started → camera end) | `GiniOnboardingScreenUITest.testOnboardingGetStartedButton` |
| PP-3411 | Slide through the 4 onboarding screens back and forth | automated, BrowserStack | `OnboardingNavigationTests.test1_swipeForwardThroughAllPagesAndBack` | partial: `testOnboardingSwipeAction` / `testOnboardingSwipePages` swipe forward once only — **gap: backward swipe** |
| PP-3412 | Skip after the first screen ("Flat paper within the frame") | automated, BrowserStack | `OnboardingNavigationTests.test2_skipOnFirstPage_opensCamera` | **gap** |
| PP-3413 | Skip after the second screen ("Good lighting") | automated, BrowserStack | `OnboardingNavigationTests.test3_skipOnSecondPage_opensCamera` | `testOnboardingSkipButton` |
| PP-3414 | Skip after the third screen ("Add multiple pages") | automated, BrowserStack | `OnboardingNavigationTests.test4_skipOnThirdPage_opensCamera` | **gap** |

No case stays manual. The camera-permission pop-up from PP-3415's step text is not part of the five
titles, and it stays pre-granted (decision below), so it is not a "stays manual" case either.

## Decisions taken (confirmed with the user on 2026-11-20)

1. **Steps come from the titles plus PP-3415.** Xray is not edited; no step CSV is produced.
2. **Camera permission is pre-granted** with `GrantPermissionRule.grant(Manifest.permission.CAMERA)`,
   like `OnboardingScreenTests`. The system pop-up is not tapped and not asserted.
3. **A new test class**, `OnboardingNavigationTests`. `OnboardingScreenTests` is not changed. The new
   class needs its own entry in the BrowserStack shard scripts.
4. **Jira bookkeeping as in PP-3920:** a mapping-table comment on PP-3883 and PP-3884, and labels on
   PP-3410..PP-3414. Both are drafted first and posted only after the user confirms.

## Requirements

All texts are matched by capture-sdk resource id (`net.gini.android.capture.R.string.*` /
`R.id.*`), never by literal text, so the tests pass in every device locale. Page titles:
page 1 `gc_onboarding_align_corners_title`, page 2 `gc_onboarding_lighting_title`,
page 3 `gc_onboarding_multipage_title`, page 4 `gc_onboarding_qr_code_title`.
"Camera screen shown" means `CaptureScreen.checkScanTextDisplayed()` returns `true`
(the shutter button `gc_button_camera_trigger` exists within its UiAutomator timeout).

- **R1 (MUST, entry):** Given the example-app test APK, when
  `./gradlew bank-sdk:example-app:connectedDevExampleAppDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.gini.android.bank.sdk.exampleapp.ui.testcases.OnboardingNavigationTests`
  is run on a device, then the class's tests run under the Test Orchestrator, one class in the filter
  (AGENTS.md gotcha: two comma-separated classes start 0 tests).
- **R2 (MUST, entry – shards):** Given the new script `bs_run_group_onboarding.sh`, when it is run, then
  one BrowserStack build runs `OnboardingScreenTests` and `OnboardingNavigationTests`, and each entry's
  comment names its Xray cases (precedent on `main`: `bs_run_group_errorscreens.sh`). Given `bs_run_all_groups.sh`'s
  rule that every test class is in exactly one of its groups, `OnboardingNavigationTests` is listed only in
  a new `run_group "onboarding"` there (`OnboardingScreenTests` stays in `run_group "ui"`).
  `bs_run_group_ui.sh`, `bs_run_group_smoke.sh` and `bs_run_release.sh` are not changed.
- **R3 (MUST, happy – PP-3411, forward):** Given a fresh install and the onboarding opened from the
  photo-payment button, when the user swipes left on `gc_onboarding_viewpager` three times, then after
  each swipe the next page's title is displayed, in the order page 2 → page 3 → page 4. On page 4,
  `gc_get_started` is displayed and `gc_next` and `gc_skip` are not displayed.
- **R4 (MUST, happy – PP-3411, back):** Given page 4 reached in R3 (same test), when the user swipes
  right three times, then the titles appear in the order page 3 → page 2 → page 1. Once back on page 3,
  `gc_next` and `gc_skip` are displayed again and `gc_get_started` is not displayed.
- **R5 (MUST, happy – PP-3412):** Given a fresh install and page 1's title displayed, when the user taps
  `gc_skip`, then the camera screen is shown.
- **R6 (MUST, happy – PP-3413):** Given a fresh install, when the user taps Next once, page 2's title is
  displayed, and then the user taps `gc_skip`, then the camera screen is shown.
- **R7 (MUST, happy – PP-3414):** Given a fresh install, when the user taps Next twice, page 3's title is
  displayed, and then the user taps `gc_skip`, then the camera screen is shown.
- **R8 (SHOULD, happy – PP-3410 end, iOS parity):** Given a fresh install, when the user taps Next three
  times, page 4's title is displayed, and then the user taps `gc_get_started`, then the camera screen is
  shown. (The existing class checks the Get Started button's text but never taps it.)
- **R9 (MUST, error – no vacuous pass):** Given the app opens the camera directly, without onboarding
  (for example, package data was not cleared), when any test of the class runs, then it **fails** on
  its first page-title wait with a message that names the missing title. No test may reach a camera
  assertion without first proving that the expected onboarding page was shown.
- **R10 (MUST, error – lost tap):** Given the 500 ms debounce of `IntervalClickListener`, when a test taps
  Next more than once, then each following tap waits until the previous page change is confirmed (the
  new title is found) **and** at least `NEXT_TAP_INTERVAL_MS = 600` ms have passed since the previous
  tap on that button. A lost tap must never let a test skip from the wrong page (e.g. R7 tapping Skip on
  page 2).
- **R11 (MUST, async):** Given a page change is a `ViewPager` scroll that Espresso does not reliably wait
  for, when a test asserts a page after a swipe or a Next tap, then it first waits for that page's
  title with a UiAutomator `waitForExists` (timeout `ONBOARDING_PAGE_TIMEOUT_MS = 5_000`), and only then
  runs the Espresso `isDisplayed` check.
- **R12 (MUST, entry – Jira):** Given the mapping table above, when the implementation is done, then
  (a) the table is posted as a comment on PP-3883 and PP-3884 (the PP-3884 comment lists the three iOS
  gaps: backward swipe, Skip on page 1, Skip on page 3); (b) PP-3410..PP-3414 get the label
  `android-automated`, merged with the existing labels (all five have none today). Both are drafted and
  shown to the user first, and posted only after the user confirms.

## Affected modules

- `bank-sdk:example-app` — `src/androidTest` only: one new test class, one page object extended, two
  BrowserStack scripts and one script doc updated. No `src/main` change.
- `capture-sdk:sdk` — read only (the screen under test: `onboarding/OnboardingFragment.java`,
  `OnboardingScreenPresenter.java`, `res/layout/gc_fragment_onboarding.xml`). No change.

## Public API impact

None. No production source set changes, and no `api/*.api` dump is touched.

## Technical conventions

1. **Language:** Kotlin, under `bank-sdk/example-app/src/androidTest/java/net/gini/android/bank/sdk/exampleapp/ui/`.
   The test class is a plain public class like its neighbours (JUnit needs it public). No legacy Java is
   touched. The capture-sdk Java onboarding (`OnboardingFragment`, `OnboardingScreenPresenter`) is MVP
   and stays as it is; the tests reach it only through the example app.
2. **UI:** no UI added or changed; no XML layout added or removed; no Compose.
3. **Architecture (test code):** page objects in `ui/screens/*`, test classes in `ui/testcases/*`. The
   new class has its own rules, like `OnboardingScreenTests`: `activityScenarioRule<MainActivity>()`
   and `GrantPermissionRule.grant(Manifest.permission.CAMERA)`. It does **not** extend
   `WarningBottomSheetTestBase` (no mock backend is needed). No `RetryRule`: no network, and the `ui`
   shard's classes don't use it.
4. **DI / async:** none. Waits use UiAutomator `waitForExists` (precedent:
   `CaptureScreen.checkScanTextDisplayed`, `NoResultScreen.waitForNoResultScreen`). The one fixed pause
   is the debounce gap of R10. It uses `SystemClock.sleep` with a comment that names
   `IntervalClickListenerHelper.interval`. It is not a general wait, and it is not
   `SimpleIdlingResource` (that is a blind sleep with no condition).
5. **Strings/resources:** no new strings or assets. Assertions use `net.gini.android.capture.R.string.*`
   and `R.id.*`. UiAutomator text selectors resolve the string at runtime through
   `InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)`. Resource-id selectors
   use `AppResources.resId(name)` (`ui/resources/AppResources.kt`).
6. **Quality gates:** CI assembles the example app and runs ktlint/detekt/lint for `bank-sdk:sdk` only
   (`bank-sdk.check.yml`). The gate for this change is that
   `bank-sdk:example-app:assembleDevExampleAppDebug` and `assembleDevExampleAppDebugAndroidTest` compile,
   plus the instrumented run itself. Jacoco/Sonar do not cover `androidTest`. Keep the neighbours'
   formatting anyway.
7. **Comments in test code** follow the suite's convention: a class KDoc on the fresh-install
   precondition and the debounce, and a one-line `/** PP-xxxx: … */` on each test naming its Xray case.
   Script comments name the Xray case (as `bs_run_group_smoke.sh` does) and add **no** new
   class or test counts.

## Design

### `OnboardingScreen` page object (extend) — `ui/screens/OnboardingScreen.kt`

Existing: `checkOnboardingScreenTitle(resId)`, `checkNextButtonText()`, `clickNextButton()`,
`checkSkipButtonText()`, `clickSkipButtonIfPresent()`, `checkGetStartedButton()`. They stay unchanged,
because `OnboardingScreenTests` uses them. Add:

- `waitForPage(titleRes: Int): OnboardingScreen` — UiAutomator `waitForExists(ONBOARDING_PAGE_TIMEOUT_MS)`
  on a `UiSelector().resourceId(AppResources.resId("gc_title")).text(<resolved title>)`. Then
  `checkOnboardingScreenTitle(titleRes)`. If the wait fails, it fails with
  `AssertionError("Onboarding page '<title>' not shown within 5000 ms")` (R9, R11).
- `swipeToNextPage()` / `swipeToPreviousPage()` — `onView(withId(R.id.gc_onboarding_viewpager)).perform(swipeLeft())` /
  `swipeRight()`.
- `clickNextAndWaitFor(titleRes: Int)` — wait until `NEXT_TAP_INTERVAL_MS` have passed since the last
  Next tap made through this page object (the time of the last tap is kept in a private field). Then
  `clickNextButton()`, store the tap time, and `waitForPage(titleRes)` (R10).
- `clickSkipButton()` — `onView(withId(R.id.gc_skip)).perform(click())`. Unlike
  `clickSkipButtonIfPresent`, it does not catch `NoMatchingViewException`, so a missing Skip button
  fails the test.
- `clickGetStartedButton()` — `onView(withId(R.id.gc_get_started)).perform(click())`.
- `checkNextAndSkipDisplayed(displayed: Boolean)` / `checkGetStartedDisplayed(displayed: Boolean)` —
  `matches(isDisplayed())` or `matches(not(isDisplayed()))`. The Next/Skip group goes `INVISIBLE` on
  the last page (`OnboardingFragment.showGetStartedButton`), and `isDisplayed()` is false for an
  invisible view.

Constants: `ONBOARDING_PAGE_TIMEOUT_MS = 5_000L`, `NEXT_TAP_INTERVAL_MS = 600L` (500 ms debounce
+ 100 ms margin), as `const val` in a `private companion object`, like `CAMERA_SCREEN_TIMEOUT_MS` in
`CaptureScreen.kt:130-133`.

Why the swipe works: the pager is the support `androidx.viewpager.widget.ViewPager`
(`gc_fragment_onboarding.xml:11`). A swipe fires `onPageSelected` (`OnboardingFragment.java:342`),
which calls `mPresenter.onScrolledToPage(position)`. The presenter's `updateButtons()`
(`OnboardingScreenPresenter.java:145-151`) then shows Get Started on the last page and Next/Skip on
the others. So a swipe updates the buttons just like a Next tap does.

### `OnboardingNavigationTests` (new) — `ui/testcases/OnboardingNavigationTests.kt`

Fields: `mainScreen = MainScreen()`, `onboardingScreen = OnboardingScreen()`,
`captureScreen = CaptureScreen()`. A `@Before` starts each test with `mainScreen.clickPhotoPaymentButton()`
and `onboardingScreen.waitForPage(gc_onboarding_align_corners_title)`.

| Test | Steps after the start | Requirements |
| --- | --- | --- |
| `test1_swipeForwardThroughAllPagesAndBack` | `swipeToNextPage` + `waitForPage` for pages 2, 3, 4; on page 4 `checkGetStartedDisplayed(true)`, `checkNextAndSkipDisplayed(false)`; `swipeToPreviousPage` + `waitForPage` for pages 3, 2, 1; right after reaching page 3, `checkNextAndSkipDisplayed(true)`, `checkGetStartedDisplayed(false)` | R3, R4, R11 |
| `test2_skipOnFirstPage_opensCamera` | `clickSkipButton()`; `assertTrue(captureScreen.checkScanTextDisplayed())` | R5, R9 |
| `test3_skipOnSecondPage_opensCamera` | `clickNextAndWaitFor(page 2)`; `clickSkipButton()`; camera assert | R6, R9, R10 |
| `test4_skipOnThirdPage_opensCamera` | `clickNextAndWaitFor(page 2)`, `clickNextAndWaitFor(page 3)`; `clickSkipButton()`; camera assert | R7, R9, R10 |
| `test5_getStartedOnLastPage_opensCamera` | `clickNextAndWaitFor` pages 2, 3, 4; `clickGetStartedButton()`; camera assert | R8, R10 |

SDK path for Skip and Get Started: `OnboardingScreenPresenter.skip()` / `showNextPage()` on the last
page → `getView().close()` → the onboarding closes and the camera that opened it is shown.

### BrowserStack scripts / docs — `bank-sdk/example-app/src/androidTest/scripts/`

Decided with the user on 2026-11-23: the Xray folder gets its own script, like
`bs_run_group_errorscreens.sh` (PP-3918, on `main`), instead of only joining the `ui` shard.
The work targets `release/bank-sdk-4.10.0` (user decision, 2026-11-23); that branch has no
`errorscreens` group yet, so the new `onboarding` group goes after `ingredientbrand_education`.

- `bs_run_group_onboarding.sh` (new, executable): an array with `OnboardingScreenTests` and
  `OnboardingNavigationTests`, each with a comment naming its Xray cases, passed to
  `bs_build_and_upload.sh`. Build name `group-onboarding-<timestamp>`.
- `bs_run_all_groups.sh`: a new `run_group "onboarding"` with `OnboardingNavigationTests` only, and a
  comment that `OnboardingScreenTests` already runs in the `ui` group.
- `bs_build_and_upload.md`: an "Onboarding shard (PP-3883)" table row for the new script. It says no
  network is needed and the fresh-install precondition comes from `clearPackageData`.

### Jira (R12, after user confirmation)

Comment with the mapping table on PP-3883 and PP-3884 (`addCommentToJiraIssue`). Add the label
`android-automated` to PP-3410..PP-3414 with `editJiraIssue`, merged with existing labels.

## Test plan

The deliverable *is* the instrumented tests. No production class is added, so no unit test is added.
Stack: JUnit4 + Espresso (`swipeLeft`/`swipeRight`, `click`, `matches`) + UiAutomator (`waitForExists`)
+ `ActivityScenarioRule` + `GrantPermissionRule`, matching `OnboardingScreenTests` and the other
classes in `ui/testcases`.

| Test class | New / extends | Tests | Requirements |
| --- | --- | --- | --- |
| `OnboardingNavigationTests` | new (decision 3) | 5 | R1, R3–R11 |
| `OnboardingScreenTests` | unchanged | 9 (existing) | PP-3410 mapping only |

Five tests: one per Xray case to build, plus the Get Started end of PP-3410 for iOS parity. This is
within the 3–6 band for a focused class.

Verification steps for /gini-build, in this order:

1. `./gradlew bank-sdk:example-app:assembleDevExampleAppDebug bank-sdk:example-app:assembleDevExampleAppDebugAndroidTest`
   compiles.
2. Local run of R1's command (one class) on an emulator or device. All 5 pass. Run it a second time to
   confirm the fresh-install state is reset between tests.
3. A local negative check of R9 (not committed): comment out `clickSkipButton()` in `test2`. The test
   must fail on the camera assert. Then restore it.
4. BrowserStack: `bs_run_group_onboarding.sh` (`BS_USER`/`BS_KEY` from the shell environment). The
   build in project `gini-mobile-android` shows both classes passing on the default device pair (R2).
5. Jira drafts (R12) are shown to the user and posted only after confirmation.

### Not tested

- The camera-permission system pop-up (pre-granted by decision 2; OEM- and locale-specific).
- Page illustrations, page indicators and the Lottie/vector content of each page. The Xray titles ask
  only for page order and where Skip lands.
- The configuration switches "Onboarding at first launch" / "at every launch" and custom onboarding
  pages. They are not in this folder; the QAlity folder `/Banking Project/QAlity imported test cases/Onboarding screen`
  (PP-2948..PP-2987) covers them and is out of scope.
- Landscape, TalkBack, keyboard and 200% font variants. They live in the WCAG folders.
- Analytics events (`SKIP_TAPPED`, `GET_STARTED_TAPPED`) — analytics backend is manual per PP-3869.
- The relaunch behaviour after Skip. It is already covered by `OnboardingScreenTests.test8`.

## Out of scope

- Any change to `capture-sdk`, `bank-sdk:sdk` or the example app's `src/main`.
- Changes to `OnboardingScreenTests`, `bs_run_group_smoke.sh` or `bs_run_release.sh`.
- Writing steps into the Xray cases, or producing an import CSV for them.
- The iOS implementation (PP-3884). Only the gap list is produced.
- The QAlity-imported and WCAG onboarding folders.

## Open questions

None.

## Implementation plan
- [x] 1. Extend `OnboardingScreen` page object (`bank-sdk/example-app/src/androidTest/java/.../ui/screens/OnboardingScreen.kt`): `waitForPage`, `swipeToNextPage`/`swipeToPreviousPage`, `clickNextAndWaitFor` with the debounce gap, `clickSkipButton`, `clickGetStartedButton`, `checkNextAndSkipDisplayed`, `checkGetStartedDisplayed` (requirements 9, 10, 11)
- [x] 2. New `OnboardingNavigationTests` (`.../ui/testcases/OnboardingNavigationTests.kt`): test1–test5 (requirements 1, 3, 4, 5, 6, 7, 8, 9, 10, 11)
- [x] 3. Shard bookkeeping (`src/androidTest/scripts/`): new `bs_run_group_onboarding.sh`; `run_group "onboarding"` in `bs_run_all_groups.sh`; "Onboarding shard (PP-3883)" row in `bs_build_and_upload.md` (requirement 2)
- [x] 4. Verify: `assembleDevExampleAppDebug` + `assembleDevExampleAppDebugAndroidTest` compile; local instrumented run of `OnboardingNavigationTests` if a device is attached; R9 negative check; BrowserStack `bs_run_group_onboarding.sh` (requirements 1, 2, 9)
- [ ] 5. Jira drafts (mapping comment for PP-3883/PP-3884 + `android-automated` labels on PP-3410..PP-3414) shown to the user; posted only after confirmation (requirement 12)

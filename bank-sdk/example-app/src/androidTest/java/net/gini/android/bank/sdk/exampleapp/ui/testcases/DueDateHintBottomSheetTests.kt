package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.DueDateFixtures
import net.gini.android.bank.sdk.exampleapp.ui.resources.ImageUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.PaymentHintConfigurator
import net.gini.android.bank.sdk.exampleapp.ui.resources.PdfUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.RetryRule
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ConfigurationScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.DigitalInvoiceScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ExtractionScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ReviewScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.WarningBottomSheetScreen
import net.gini.android.capture.GiniCapture
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime
import java.util.Properties

/**
 * UI tests for the Due Date Hint state of the warning bottom sheet on the Analysis screen
 * (PP-3262, automated per PP-3301 from the PP-3261 XRay test cases).
 *
 * The Due Date Hint shows when the client flags are paymentDueHint=ON /
 * paymentScheduleHint=OFF, the extractions carry paymentState=ToBePaid and a
 * paymentDueDate at least paymentDueHintThresholdDays in the future. Show/not-show cases
 * are driven by moving the threshold relative to the fixed fixture due date (see
 * [DueDateFixtures]) instead of needing invoices with different dates.
 *
 * Stability assumptions (see specs/PP-3301-feature.md): the server-side /configurations
 * flags must be enabled for the test client (verified 2026-08-17), and each test re-races
 * the /configurations fetch against the analysis round trip because clearPackageData
 * wipes the persisted flags — an intermittent "sheet never appeared" failure is a
 * config-fetch problem before it is a sheet-logic problem.
 */
class DueDateHintBottomSheetTests {
    @get:Rule(order = Int.MIN_VALUE)
    val retryRule = RetryRule()

    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val captureScreen = CaptureScreen()
    private val imageUploader = ImageUploader()
    private val pdfUploader = PdfUploader()
    private val reviewScreen = ReviewScreen()
    private val configurationScreen = ConfigurationScreen()
    private val extractionScreen = ExtractionScreen()
    private val digitalInvoiceScreen = DigitalInvoiceScreen()
    private val warningBottomSheet = WarningBottomSheetScreen()
    private lateinit var idlingResource: SimpleIdlingResource

    val testProperties = Properties().apply {
        runCatching {
            getApplicationContext<Context>().resources.assets
                .open("test.properties").use { load(it) }
        }
    }

    @Before
    fun setup() {
        cancelTestIfRunOnCi()
        grantStoragePermission()
        idlingResource = SimpleIdlingResource(2000)
        IdlingRegistry.getInstance().register(idlingResource)
        mainScreen.clickSettingButton()
        configurationScreen.clickTransactionDocsSwitch()
        pressBack()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
        // Defensive reset for runs without the Orchestrator (e.g. Android Studio run
        // configs); under the Orchestrator every test starts in a fresh process anyway.
        runCatching {
            PaymentHintConfigurator.applyHintConfiguration(
                activityRule.scenario,
                paymentDueHintEnabled = true,
                paymentScheduleHintEnabled = true
            )
        }
    }

    /**
     * R1: with due=ON / schedule=OFF and a qualifying invoice the Due Date Hint sheet
     * shows with the formatted due date, description and both CTAs.
     */
    @Test
    fun test1_dueDateHintSheetIsDisplayedWithCorrectContent() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = false)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Due Date Hint sheet did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertDueDateHintState(DueDateFixtures.FORMATTED_DUE_DATE)
    }

    /**
     * R3: boundary — remaining days exactly equal to the threshold still shows the sheet.
     */
    @Test
    fun test2_sheetIsDisplayedWhenRemainingDaysEqualThreshold() {
        assumeNotCloseToMidnight()
        configureHints(
            paymentDueHintEnabled = true,
            paymentScheduleHintEnabled = false,
            thresholdDays = DueDateFixtures.remainingDays()
        )
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Sheet must show when remainingDays == threshold", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertDueDateHintState(DueDateFixtures.FORMATTED_DUE_DATE)
    }

    /**
     * R4: remaining days below the threshold — no sheet, flow continues to extractions.
     */
    @Test
    fun test3_sheetIsNotDisplayedWhenRemainingDaysBelowThreshold() {
        assumeNotCloseToMidnight()
        configureHints(
            paymentDueHintEnabled = true,
            paymentScheduleHintEnabled = false,
            thresholdDays = DueDateFixtures.remainingDays() + 1
        )
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertFalse("Sheet must not show below threshold", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R5: both client flags off — neither sheet state shows, flow continues to extractions.
     */
    @Test
    fun test4_noSheetIsDisplayedWhenBothFlagsAreOff() {
        configureHints(paymentDueHintEnabled = false, paymentScheduleHintEnabled = false)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertFalse("Sheet must not show with both flags off", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R8: the sheet is not cancelable — tapping the scrim outside it does nothing.
     */
    @Test
    fun test5_tapOutsideDoesNotDismissSheet() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = false)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)
        assertTrue("Due Date Hint sheet did not appear", warningBottomSheet.waitForSheet())

        warningBottomSheet.tapOutsideSheet()

        assertTrue("Sheet was dismissed by an outside tap", warningBottomSheet.isSheetDisplayed())
        warningBottomSheet.assertDueDateHintState(DueDateFixtures.FORMATTED_DUE_DATE)
    }

    /**
     * R9: the rotating capture suggestions stay suppressed while the sheet is shown. The
     * watch window exceeds AnalysisHintsAnimator.HINT_START_DELAY (5000 ms).
     */
    @Test
    fun test6_captureSuggestionsAreSuppressedWhileSheetIsShown() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = false)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)
        assertTrue("Due Date Hint sheet did not appear", warningBottomSheet.waitForSheet())

        assertFalse(
            "Capture suggestions appeared while the sheet was shown",
            warningBottomSheet.didCaptureSuggestionsAppearWithin(SUGGESTIONS_WATCH_WINDOW)
        )
        assertTrue("Sheet disappeared during the watch window", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R10: "Proceed Anyway" (primary CTA) dismisses the sheet and continues to the
     * extraction screen.
     */
    @Test
    fun test7_proceedAnywayDismissesSheetAndShowsExtractions() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = false)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)
        assertTrue("Due Date Hint sheet did not appear", warningBottomSheet.waitForSheet())

        warningBottomSheet.clickPrimaryButton()

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertFalse("Sheet must be dismissed", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R11: "Cancel Transfer" (secondary CTA) dismisses the sheet, cancels the transaction
     * and returns to the example app's landing page.
     */
    @Test
    fun test8_cancelTransferDismissesSheetAndReturnsToMainScreen() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = false)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)
        assertTrue("Due Date Hint sheet did not appear", warningBottomSheet.waitForSheet())

        warningBottomSheet.clickSecondaryButton()

        idlingResource.waitForIdle()
        assertTrue("Landing page did not appear", mainScreen.assertDescriptionTitle())
    }

    /**
     * R6: an invoice without a paymentDueDate extraction never shows a sheet, even with
     * both flags on.
     */
    @Test
    fun test9_noSheetWhenDueDateExtractionIsEmpty() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = true)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.NO_DUE_DATE_ASSET)

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertFalse("Sheet must not show without a due date", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R7: a Return Assistant invoice suppresses the sheet — the digital invoice flow
     * starts instead. Uses the existing Testrechnung-RA-1.pdf (BrowserStack uploads it as
     * media; see bs_build_and_upload.sh).
     */
    @Test
    fun test10_noSheetForReturnAssistantInvoice() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = true)
        // BrowserStack pre-loads this PDF as uploaded media; on a local device it must be
        // copied from the test assets so the file picker can find it. Wrapped because on
        // BrowserStack the same-named, shell-owned file already exists and MediaStore may
        // refuse the delete/insert — the picker then simply uses the pre-loaded file.
        runCatching { pdfUploader.copyPdfToDownloads(getApplicationContext(), "Testrechnung-RA-1.pdf") }
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()

        assertTrue(
            "Digital invoice onboarding did not appear",
            digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        )
        assertFalse("Sheet must not show for RA invoices", warningBottomSheet.isSheetDisplayed())
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun configureHints(
        paymentDueHintEnabled: Boolean,
        paymentScheduleHintEnabled: Boolean,
        thresholdDays: Int = GiniCapture.PAYMENT_DUE_HINT_THRESHOLD_DAYS
    ) {
        PaymentHintConfigurator.applyHintConfiguration(
            activityRule.scenario,
            paymentDueHintEnabled = paymentDueHintEnabled,
            paymentScheduleHintEnabled = paymentScheduleHintEnabled,
            paymentDueHintThresholdDays = thresholdDays
        )
    }

    private fun uploadFixtureInvoiceAndProcess(assetName: String) {
        imageUploader.copyImageToDownloads(getApplicationContext(), assetName)
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        captureScreen.clickFilesButton()
        captureScreen.clickPhotos()
        imageUploader.uploadImageFromPhotos()
        imageUploader.clickAddButton()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        reviewScreen.clickProcessButton()
    }

    /**
     * The threshold-boundary tests compute remainingDays at setup while the presenter
     * recomputes it minutes later — a date rollover in between flips the expected
     * outcome. Skip instead of flaking on night runs.
     */
    private fun assumeNotCloseToMidnight() {
        val now = LocalTime.now()
        Assume.assumeTrue(
            "Skipped near local midnight to avoid a date-rollover flake",
            now.isAfter(LocalTime.of(0, 30)) && now.isBefore(LocalTime.of(23, 30))
        )
    }

    private fun grantStoragePermission() {
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("pm grant net.gini.android.bank.sdk.exampleapp android.permission.READ_EXTERNAL_STORAGE")
        device.executeShellCommand("pm grant net.gini.android.bank.sdk.exampleapp android.permission.WRITE_EXTERNAL_STORAGE")
    }

    private fun cancelTestIfRunOnCi() {
        val ignoreTests = testProperties["ignoreLocalTests"] as? String
        Assume.assumeTrue(ignoreTests != "true")
    }

    companion object {
        private const val SUGGESTIONS_WATCH_WINDOW = 6_500L
    }
}

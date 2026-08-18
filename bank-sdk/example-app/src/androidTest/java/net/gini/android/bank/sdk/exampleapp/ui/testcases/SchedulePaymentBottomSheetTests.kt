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
import net.gini.android.bank.sdk.exampleapp.ui.resources.RetryRule
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ConfigurationScreen
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
 * UI tests for the Schedule Payment state of the warning bottom sheet on the Analysis
 * screen (PP-3264, automated per PP-3301 from the PP-3263 XRay test cases).
 *
 * The Schedule Payment state shows whenever the client paymentScheduleHint flag is ON and
 * the invoice qualifies — it takes priority over the Due Date Hint and is independent of
 * the paymentDueHint flag. Its primary CTA hands the extractions back to the host app as
 * CaptureResult.SchedulePayment, which the example app surfaces via the
 * scheduled-payment indicator on the extraction screen (the observable that
 * distinguishes it from a pay-now Success result).
 *
 * Stability assumptions: see DueDateHintBottomSheetTests and specs/PP-3301-feature.md.
 */
class SchedulePaymentBottomSheetTests {
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
    private val reviewScreen = ReviewScreen()
    private val configurationScreen = ConfigurationScreen()
    private val extractionScreen = ExtractionScreen()
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
        runCatching {
            PaymentHintConfigurator.applyHintConfiguration(
                activityRule.scenario,
                paymentDueHintEnabled = true,
                paymentScheduleHintEnabled = true
            )
        }
    }

    /**
     * R2 (priority): with BOTH flags on, the Schedule Payment state shows — not the Due
     * Date Hint. The description and CTA labels are the distinguishing assertions; the
     * title is shared between the two states.
     */
    @Test
    fun test1_schedulePaymentSheetIsDisplayedWhenBothFlagsOn() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = true)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Schedule Payment sheet did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertSchedulePaymentState(DueDateFixtures.FORMATTED_DUE_DATE)
    }

    /**
     * R2 (independence): the schedule state shows even with the due flag OFF.
     */
    @Test
    fun test2_schedulePaymentSheetIsDisplayedWhenOnlyScheduleFlagOn() {
        configureHints(paymentDueHintEnabled = false, paymentScheduleHintEnabled = true)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Schedule Payment sheet did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertSchedulePaymentState(DueDateFixtures.FORMATTED_DUE_DATE)
    }

    /**
     * R4 analog for the schedule state: below the threshold no sheet shows.
     */
    @Test
    fun test3_sheetIsNotDisplayedWhenRemainingDaysBelowThreshold() {
        assumeNotCloseToMidnight()
        configureHints(
            paymentDueHintEnabled = true,
            paymentScheduleHintEnabled = true,
            thresholdDays = DueDateFixtures.remainingDays() + 1
        )
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertFalse("Sheet must not show below threshold", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R12: the "Schedule Payment" primary CTA finishes the flow with
     * CaptureResult.SchedulePayment — asserted via the scheduled-payment indicator on the
     * extraction screen. Landing on the extraction screen alone would not distinguish
     * this from "Proceed Anyway".
     */
    @Test
    fun test4_schedulePaymentButtonShowsScheduledPaymentIndicator() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = true)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)
        assertTrue("Schedule Payment sheet did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertSchedulePaymentState(DueDateFixtures.FORMATTED_DUE_DATE)

        warningBottomSheet.clickPrimaryButton()

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertTrue(
            "Scheduled-payment indicator missing — the result was not SchedulePayment",
            extractionScreen.isScheduledPaymentIndicatorDisplayed()
        )
        assertFalse("Sheet must be dismissed", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R13: the "Proceed Anyway" secondary CTA continues as a pay-now Success — the
     * scheduled-payment indicator must NOT be shown.
     */
    @Test
    fun test5_proceedAnywayDismissesSheetAndShowsExtractions() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = true)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)
        assertTrue("Schedule Payment sheet did not appear", warningBottomSheet.waitForSheet())

        warningBottomSheet.clickSecondaryButton()

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertFalse(
            "Scheduled-payment indicator shown for a pay-now result",
            extractionScreen.isScheduledPaymentIndicatorDisplayed()
        )
        assertFalse("Sheet must be dismissed", warningBottomSheet.isSheetDisplayed())
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
}

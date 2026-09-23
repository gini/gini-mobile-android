package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.AppResources
import net.gini.android.bank.sdk.exampleapp.ui.resources.CreditNoteHintConfigurator
import net.gini.android.bank.sdk.exampleapp.ui.resources.DueDateFixtures
import net.gini.android.bank.sdk.exampleapp.ui.resources.ImageUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.PaymentHintConfigurator
import net.gini.android.bank.sdk.exampleapp.ui.resources.PdfUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.ReturnAssistantSkontoConfigurator
import net.gini.android.bank.sdk.exampleapp.ui.resources.TransactionDocsConfigurator
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
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockClientConfiguration
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import net.gini.android.capture.GiniCapture
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import java.time.LocalTime
import java.util.Properties

/**
 * Shared scaffolding for the warning bottom sheet UI tests: rules, page
 * objects and the fixture-driven flow helpers used by both
 * [DueDateHintBottomSheetTests] and [SchedulePaymentBottomSheetTests]. Keeping them
 * here means the flow (and the mid-2028 fixture refresh, see [DueDateFixtures]) is
 * edited in one place.
 *
 * Stability assumptions (see specs/PP-3301-feature.md): the server-side /configurations
 * flags must be enabled for the test client (verified 2026-08-17), and each test re-races
 * the /configurations fetch against the analysis round trip because clearPackageData
 * wipes the persisted flags — an intermittent "sheet never appeared" failure is a
 * config-fetch problem before it is a sheet-logic problem.
 */
abstract class WarningBottomSheetTestBase {
    @get:Rule(order = Int.MIN_VALUE)
    val retryRule = RetryRule()

    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    protected val mainScreen = MainScreen()
    protected val onboardingScreen = OnboardingScreen()
    protected val captureScreen = CaptureScreen()
    protected val imageUploader = ImageUploader()
    protected val pdfUploader = PdfUploader()
    protected val reviewScreen = ReviewScreen()
    protected val configurationScreen = ConfigurationScreen()
    protected val extractionScreen = ExtractionScreen()
    protected val warningBottomSheet = WarningBottomSheetScreen()
    protected val digitalInvoiceScreen = DigitalInvoiceScreen()
    protected lateinit var idlingResource: SimpleIdlingResource

    private val testProperties = Properties().apply {
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
        // Set directly rather than through the Settings screen: the old three-tap version ran
        // before every test and could fail on layout alone — see TransactionDocsConfigurator.
        TransactionDocsConfigurator.applyTransactionDocsConfiguration(
            activityRule.scenario,
            transactionDocsEnabled = false
        )
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
        // Process-wide, so it must be cleared even for the suites that never arm it.
        UiTestMockBackend.disarm()
    }

    protected fun configureHints(
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

    /**
     * Sets the client-side Credit Note Hint flag without touching the Settings screen. See
     * [CreditNoteHintConfigurator] for why the switch is not tapped. Must run before the photo
     * payment button is clicked, like [configureHints].
     */
    protected fun configureCreditNoteHint(enabled: Boolean) {
        CreditNoteHintConfigurator.applyCreditNoteHintConfiguration(
            activityRule.scenario,
            creditNoteHintEnabled = enabled
        )
    }

    /**
     * Pins the Return Assistant and Skonto flags. Both default to on, so a test that cares which
     * screen the flow routes to must say so explicitly — see [ReturnAssistantSkontoConfigurator].
     * Must run before the photo payment button is clicked, like [configureHints].
     */
    protected fun configureReturnAssistantAndSkonto(
        returnAssistantEnabled: Boolean,
        skontoEnabled: Boolean
    ) {
        ReturnAssistantSkontoConfigurator.applyFeatureConfiguration(
            activityRule.scenario,
            returnAssistantEnabled = returnAssistantEnabled,
            skontoEnabled = skontoEnabled
        )
    }

    /**
     * Replaces the Gini API with canned responses for the rest of this test. See
     * [UiTestMockBackend] for what that buys us: the backend client-configuration flags and
     * extraction shapes we have no document for become ordinary assertions.
     *
     * The three flags are the SERVER-side gates the mock serves from `getConfiguration`. The
     * SDK-side ones are separate — [configureCreditNoteHint] and
     * [configureReturnAssistantAndSkonto] — and a feature needs both to be on.
     *
     * Must be called before the photo payment button is clicked, like [configureHints]. Cleared
     * in [tearDown].
     */
    protected fun armMockBackend(
        scenario: UiTestMockScenario,
        creditNoteHintEnabled: Boolean = true,
        returnAssistantEnabled: Boolean = true,
        skontoEnabled: Boolean = true
    ) {
        UiTestMockBackend.arm(
            scenario = scenario,
            clientConfiguration = UiTestMockClientConfiguration(
                creditNoteHintEnabled = creditNoteHintEnabled,
                returnAssistantEnabled = returnAssistantEnabled,
                skontoEnabled = skontoEnabled
            )
        )
    }

    protected fun uploadFixtureInvoiceAndProcess(assetName: String) {
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
    /**
     * The PDF counterpart of [uploadFixtureInvoiceAndProcess]: import a document through the SDK's
     * file picker instead of the photo picker. There is no review screen on this path, so the flow
     * goes straight from the picker to analysis.
     *
     * BrowserStack pre-loads the suite's PDFs as uploaded media (`bs_build_and_upload.sh`); on a
     * local device the file has to be copied into Downloads so the picker can find it. The copy is
     * wrapped because on BrowserStack a shell-owned file of the same name already exists and
     * MediaStore may refuse the delete/insert — the picker then just uses the pre-loaded one. Same
     * reasoning as `DueDateHintBottomSheetTests.test10_noSheetForReturnAssistantInvoice`.
     */
    protected fun uploadFixturePdfAndProcess(fileName: String) {
        runCatching { pdfUploader.copyPdfToDownloads(getApplicationContext(), fileName) }
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles(fileName)
        idlingResource.waitForIdle()
    }

    protected fun assumeNotCloseToMidnight() {
        val now = LocalTime.now()
        Assume.assumeTrue(
            "Skipped near local midnight to avoid a date-rollover flake",
            now.isAfter(LocalTime.of(0, 30)) && now.isBefore(LocalTime.of(23, 30))
        )
    }

    private fun grantStoragePermission() {
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("pm grant ${AppResources.packageName} android.permission.READ_EXTERNAL_STORAGE")
        device.executeShellCommand("pm grant ${AppResources.packageName} android.permission.WRITE_EXTERNAL_STORAGE")
    }

    private fun cancelTestIfRunOnCi() {
        val ignoreTests = testProperties["ignoreLocalTests"] as? String
        Assume.assumeTrue(ignoreTests != "true")
    }
}

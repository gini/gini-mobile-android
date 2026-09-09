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
import net.gini.android.bank.sdk.exampleapp.ui.resources.ImageUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.PdfUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.RetryRule
import net.gini.android.bank.sdk.exampleapp.ui.resources.ReturnAssistantSkontoConfigurator
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.resources.TransactionDocsConfigurator
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ExtractionScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ReviewScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.SkontoScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockClientConfiguration
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import java.util.Properties

/**
 * Shared scaffolding for the Xray smoke journeys: the rule stack, the page objects, the
 * idling resource, and the two ways a document enters the SDK (PDF via Files, image via the
 * Photos gallery). Subclasses are [SmokeJourneyTests] and [SkontoScreenTests].
 *
 * There is no live-capture helper: BrowserStack does not support camera image injection for
 * Espresso, so a captured photo is whatever the device rack sees and nothing can be asserted
 * about its content.
 *
 * These are real-backend tests, like every other end-to-end journey in this suite: they
 * need `clientId`/`clientSecret` in `bank-sdk/example-app/local.properties` and a working
 * Gini API. The extraction assertions are therefore written against what the fixture
 * really returns — "the IBAN field is non-empty", "the total went up" — never against a
 * currency figure typed into the test, which would break on any backend change and would
 * pass just as well against a hardcoded response.
 *
 * Deliberately separate from [WarningBottomSheetTestBase] rather than extracted from it:
 * that base and its three suites pass today and cannot be re-run here without a device and
 * live credentials, so generalising it is a refactor with no way to verify it. The two
 * bases overlap; folding them into one shared base is worth doing in a change that can
 * actually run the warning-sheet suites.
 */
abstract class SmokeJourneyTestBase {

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
    protected val reviewScreen = ReviewScreen()
    protected val extractionScreen = ExtractionScreen()
    protected val skontoScreen = SkontoScreen()
    protected val imageUploader = ImageUploader()
    protected val pdfUploader = PdfUploader()

    protected lateinit var idlingResource: SimpleIdlingResource

    /**
     * How long [SimpleIdlingResource] waits per step. Overridden by suites whose flow
     * includes a slow screen — the Skonto screen renders after a full analysis round trip.
     */
    protected open val idlingTimeoutMs: Long = 2_000

    private val testProperties = Properties().apply {
        // On CI / BrowserStack the generated test.properties may be absent from the test
        // APK. A missing file must mean "run the test" — see cancelTestIfRunOnCi.
        runCatching {
            getApplicationContext<Context>().resources.assets
                .open("test.properties").use { load(it) }
        }
    }

    @Before
    fun setup() {
        cancelTestIfRunOnCi()
        grantStoragePermission()
        idlingResource = SimpleIdlingResource(idlingTimeoutMs)
        IdlingRegistry.getInstance().register(idlingResource)
        // Keeps the "Add an attachment to this transaction?" dialog off the extraction
        // screen, where it hides the transfer-summary button. Set directly rather than by
        // tapping the settings switch — see TransactionDocsConfigurator for why.
        TransactionDocsConfigurator.applyTransactionDocsConfiguration(
            activityRule.scenario,
            transactionDocsEnabled = false
        )
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
        // Process-wide, so it must be cleared even by suites that never arm it — a leaked
        // scenario would silently feed canned data to the next test.
        UiTestMockBackend.disarm()
    }

    /**
     * Replaces the Gini API with canned responses for the rest of this test.
     *
     * The flags passed here are the **server-side** client-configuration gates the mock
     * serves from `getConfiguration`. They are separate from the SDK-side flags set by
     * [configureReturnAssistantAndSkonto], and a feature generally needs both to be on.
     *
     * Must be called before the photo-payment button is clicked — `configureGiniBank()` runs
     * on that click, which is when the mock's network service is picked up.
     */
    protected fun armMockBackend(
        scenario: UiTestMockScenario,
        returnAssistantEnabled: Boolean = true,
        skontoEnabled: Boolean = true,
        creditNoteHintEnabled: Boolean = true
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

    /**
     * Pins the Return Assistant and Skonto flags. Both default to on, so a test asserting
     * which screen the flow reaches has to say which were enabled — see
     * [ReturnAssistantSkontoConfigurator]. Must run before the photo payment button is
     * clicked.
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
     * Imports a PDF through the SDK's file picker and waits for analysis.
     *
     * A PDF has no review step, so this lands on whichever screen the extraction routes
     * to — extraction, Skonto or digital invoice.
     *
     * The copy into Downloads is wrapped: on BrowserStack a shell-owned file of the same
     * name may already be present and MediaStore can refuse the delete/insert, in which
     * case the picker simply uses the pre-loaded one. Same reasoning as
     * `WarningBottomSheetTestBase.uploadFixturePdfAndProcess`.
     */
    protected fun importPdfAndAwaitAnalysis(fileName: String) {
        runCatching { pdfUploader.copyPdfToDownloads(getApplicationContext(), fileName) }
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles(fileName)
        idlingResource.waitForIdle()
    }

    /**
     * Imports an image through the SDK's photo picker, then processes it from the review
     * screen and waits for analysis.
     *
     * Goes through the Photos gallery, not the Files picker, because that is the pairing
     * `ImageUploader` supports: despite its name, `copyImageToDownloads` inserts into
     * `MediaStore.Images` under `DIRECTORY_PICTURES` with a `<timestamp>_<name>` display
     * name, so the fixture is never in Downloads under its bare name and
     * `uploadImageFromFiles(name)` — which matches Downloads by exact name — cannot find
     * it. `uploadImageFromPhotos()` takes the newest photo instead, which is the copy made
     * here. Same sequence as `ReviewScreenTests.test1_reviewUploadedInvoice`, including the
     * `clickAddButton()` the legacy picker needs.
     *
     * Still needs no BrowserStack `uploadMedia` image: the fixture travels in the test APK
     * and is inserted into MediaStore here, so it is the most recent photo by the time the
     * picker opens.
     */
    protected fun importImageAndAwaitAnalysis(fileName: String) {
        runCatching { imageUploader.copyImageToDownloads(getApplicationContext(), fileName) }
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        captureScreen.clickFilesButton()
        captureScreen.clickPhotos()
        imageUploader.uploadImageFromPhotos()
        imageUploader.clickAddButton()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        reviewScreen.clickProcessButton()
        idlingResource.waitForIdle()
    }

    private fun grantStoragePermission() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand(
            "pm grant ${AppResources.packageName} android.permission.READ_EXTERNAL_STORAGE"
        )
        device.executeShellCommand(
            "pm grant ${AppResources.packageName} android.permission.WRITE_EXTERNAL_STORAGE"
        )
    }

    private fun cancelTestIfRunOnCi() {
        val ignoreTests = testProperties["ignoreLocalTests"] as? String
        Assume.assumeTrue(ignoreTests != "true")
    }
}

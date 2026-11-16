package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.content.Intent
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import net.gini.android.bank.sdk.exampleapp.test.getAssetFileStorageUri
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.screens.FraudWarningScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.SkontoScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockFraudData
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The fraud prevention warning on a Skonto invoice opened with "Open with" (PP-3970), driven by
 * [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend]: the `*_WITH_SKONTO`
 * scenarios serve the fraud signals together with a claimable `skontoDiscounts` extraction.
 *
 * Xray test cases covered:
 *
 * | Xray   | Case                                            | Test |
 * |--------|-------------------------------------------------|------|
 * | TC-014 | QR-vs-printed: warning → IBAN choice → Skonto   | [test1_qrVsPrintedContinuesToSkonto] |
 * | TC-014 | combined: warning → IBAN choice → Skonto        | [test2_combinedContinuesToSkonto] |
 * | TC-005 | Skonto path, printed IBAN chosen: both feedbacks | [test3_skontoFeedbackPrintedIbanChosen] |
 * | TC-005 | Skonto path, QR code IBAN chosen: both feedbacks | [test4_skontoFeedbackQrIbanChosen] |
 *
 * TC-015 (Return Assistance) is not covered here.
 *
 * ## "Open with"
 *
 * TC-014 shares a picture from the gallery to the app. The test sends the same `ACTION_VIEW`
 * intent the share target receives, as [OpenWithTest] does, to the running [MainActivity]. The
 * system share sheet itself is not part of the test. `MainActivity.startGiniBankSdkForOpenWith`
 * configures the SDK before it starts, so the mock armed beforehand answers the analysis.
 *
 * ## The two feedbacks on the Skonto path
 *
 * 1. "Confirm and proceed" on the Skonto screen sends the Skonto feedback
 *    (`ProceedClickedIntent` → `GiniCapture.sendTransferSummaryForSkonto`). It builds new maps:
 *    only `amountToPay` and a `skontoDiscounts` compound extraction. The test checks exactly
 *    that, so `fraudSignals` (or anything else) showing up there fails it.
 * 2. The Extractions screen's transfer summary sends the normal feedback, as in
 *    [FraudFeedbackTests]: the chosen `iban`, the `bic`, and the discounted `amountToPay`.
 */
class FraudSkontoTests : FraudWarningTestBase() {

    private val skontoScreen = SkontoScreen()

    /**
     * Starts in portrait. The Skonto screen moves its footer into the scrolling list in
     * landscape, below the fold, so [SkontoScreen.waitForSkontoScreen] cannot find the total
     * there. A landscape test whose process dies never runs `LandscapeDarkModeRule.after`, and
     * on BrowserStack this class runs right after [FraudWarningLandscapeTests].
     */
    @Before
    fun startInPortrait() {
        // Freezes the rotation, so the sensor cannot turn it back during the test.
        device.setOrientationPortrait()
    }

    @After
    fun releaseRotation() {
        device.unfreezeRotation()
    }

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /** TC-014: QR-vs-printed warning, "Proceed anyway", choose an IBAN, "Continue": Skonto. */
    @Test
    fun test1_qrVsPrintedContinuesToSkonto() {
        openWithScenario(UiTestMockScenario.FRAUD_QR_MISMATCH_WITH_SKONTO)
        assertWarningShown(FraudWarningScreen.Type.QR_VS_PRINTED)

        proceedAndChooseToSkonto(UiTestMockFraudData.PRINTED_IBAN)

        assertFalse("Fraud warning must be closed", fraudWarning.isAnyWarningDisplayed())
    }

    /** TC-014: combined warning, "Proceed anyway", choose an IBAN, "Continue": Skonto. */
    @Test
    fun test2_combinedContinuesToSkonto() {
        openWithScenario(UiTestMockScenario.FRAUD_COMBINED_WITH_SKONTO)
        assertWarningShown(FraudWarningScreen.Type.COMBINED)

        proceedAndChooseToSkonto(UiTestMockFraudData.PRINTED_IBAN)

        assertFalse("Fraud warning must be closed", fraudWarning.isAnyWarningDisplayed())
    }

    /**
     * TC-005 on the Skonto path, printed IBAN chosen: the Skonto feedback has only the Skonto
     * fields, and the transfer summary sends the printed IBAN, an empty bic and the discounted
     * amount.
     */
    @Test
    fun test3_skontoFeedbackPrintedIbanChosen() {
        openWithScenario(UiTestMockScenario.FRAUD_QR_MISMATCH_WITH_SKONTO)
        assertWarningShown(FraudWarningScreen.Type.QR_VS_PRINTED)
        proceedAndChooseToSkonto(UiTestMockFraudData.PRINTED_IBAN)

        confirmSkontoAndAssertSkontoFeedback()
        extractionScreen.clickTransferSummaryButton()

        assertTransferSummaryFeedback(
            expectedIban = UiTestMockFraudData.PRINTED_IBAN,
            expectedBic = EMPTY_BIC
        )
    }

    /**
     * TC-005 on the Skonto path, QR code IBAN chosen on the combined warning: the transfer
     * summary keeps the QR code IBAN and its bic.
     */
    @Test
    fun test4_skontoFeedbackQrIbanChosen() {
        openWithScenario(UiTestMockScenario.FRAUD_COMBINED_WITH_SKONTO)
        assertWarningShown(FraudWarningScreen.Type.COMBINED)
        proceedAndChooseToSkonto(UiTestMockFraudData.QR_IBAN)

        confirmSkontoAndAssertSkontoFeedback()
        extractionScreen.clickTransferSummaryButton()

        assertTransferSummaryFeedback(
            expectedIban = UiTestMockFraudData.QR_IBAN,
            expectedBic = UiTestMockFraudData.QR_BIC
        )
    }

    /**
     * Arms the mock with [scenario] (fraud signals and Skonto on, server and SDK side) and opens
     * a fixture picture in the app with "Open with". The mock ignores the picture.
     *
     * The analysis is delayed by [OPEN_WITH_ANALYSIS_DELAY]. "Open with" fetches the
     * configuration and starts the analysis at the same moment, and the fraud flag reaches the
     * SDK only after the configuration is saved to DataStore and read back by its observer
     * (`GiniCaptureFragment.setupUserAnalytics`, `GiniCaptureViewModel`). An instant mock answer
     * wins that race on a fresh install, the SDK still has the flag off, and the warning is
     * skipped. A real analysis takes seconds, so the delay matches what the SDK sees in use.
     */
    private fun openWithScenario(scenario: UiTestMockScenario) {
        armMockBackend(
            scenario,
            fraudSignalsEnabled = true,
            skontoEnabled = true,
            analysisDelayMillis = OPEN_WITH_ANALYSIS_DELAY
        )
        configureReturnAssistantAndSkonto(returnAssistantEnabled = true, skontoEnabled = true)

        val intent = Intent(getApplicationContext(), MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            setDataAndType(getAssetFileStorageUri(ANY_DOCUMENT), "image/jpeg")
        }
        activityRule.scenario.onActivity { it.startActivity(intent) }
    }

    /** "Proceed anyway", choose [iban], "Continue", and wait for the Skonto screen. */
    private fun proceedAndChooseToSkonto(iban: String) {
        assertTrue("\"Proceed anyway\" not found", fraudWarning.clickProceedAnyway())
        assertTrue("IBAN selection did not open", ibanSelection.waitForSheet())
        assertTrue("IBAN row $iban not found", ibanSelection.selectIban(iban))
        assertTrue("\"Continue\" not found", ibanSelection.clickContinue())
        skontoScreen.waitForSkontoScreen()
    }

    /**
     * "Confirm and proceed" on the Skonto screen, then the Skonto feedback: exactly
     * `amountToPay` = the discounted amount, and exactly the `skontoDiscounts` compound
     * extraction. Clears the recorded feedback afterwards, so the transfer summary check reads
     * the next call only.
     */
    private fun confirmSkontoAndAssertSkontoFeedback() {
        skontoScreen.clickProceedButton()

        val compound = awaitFeedbackCompoundExtractions()
        assertNotNull("The SDK sent no Skonto feedback", compound)
        assertEquals(
            "Skonto feedback compound extractions",
            setOf(UiTestMockFraudData.SKONTO_DISCOUNTS_KEY),
            compound.orEmpty().keys
        )
        assertEquals(
            "Skonto feedback extractions",
            mapOf(AMOUNT_TO_PAY_KEY to UiTestMockFraudData.SKONTO_AMOUNT_TO_PAY),
            awaitFeedbackExtractionValues()
        )

        UiTestMockBackend.lastFeedbackExtractionValues = null
        UiTestMockBackend.lastFeedbackCompoundExtractions = null
        assertTrue(
            "Extraction screen did not appear",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
    }

    /**
     * The transfer summary feedback: [expectedIban], [expectedBic] (see [FraudFeedbackTests]
     * for why "no BIC" is an empty value), the discounted amount, and no `fraudSignals`.
     */
    private fun assertTransferSummaryFeedback(expectedIban: String, expectedBic: String) {
        val compound = awaitFeedbackCompoundExtractions()
        assertNotNull("The SDK sent no feedback after the transfer summary", compound)
        assertFalse(
            "The feedback must not contain fraudSignals, but had ${compound?.keys}",
            compound.orEmpty().containsKey(UiTestMockFraudData.FRAUD_SIGNALS_KEY)
        )

        val extractions = awaitFeedbackExtractionValues().orEmpty()
        assertEquals("Feedback iban, in $extractions", expectedIban, extractions[IBAN_KEY])
        assertEquals("Feedback bic, in $extractions", expectedBic, extractions[BIC_KEY])
        assertEquals(
            "Feedback amountToPay, in $extractions",
            UiTestMockFraudData.SKONTO_AMOUNT_TO_PAY,
            extractions[AMOUNT_TO_PAY_KEY]
        )
    }

    private companion object {
        const val IBAN_KEY = "iban"
        const val BIC_KEY = "bic"
        const val AMOUNT_TO_PAY_KEY = "amountToPay"
        const val EMPTY_BIC = ""

        /** See [openWithScenario]. */
        const val OPEN_WITH_ANALYSIS_DELAY = 2_000L
    }
}

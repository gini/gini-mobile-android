package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.os.SystemClock
import net.gini.android.bank.sdk.exampleapp.ui.resources.CreditNoteFixtures
import net.gini.android.bank.sdk.exampleapp.ui.screens.AnalysisScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.FraudWarningScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.IbanSelectionScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Shared scaffolding for the fraud prevention warning tests (PP-3970): the page objects and the
 * mock-backend flow. Every test runs against
 * [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend], because the warning
 * needs a `fraudSignals` compound extraction and the `fraudSignalsEnabled` client flag, and no
 * real document or test client gives us either on demand.
 *
 * The values the mock serves live in
 * [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockFraudData], so the assertions use
 * the same constants as the payload.
 *
 * Rules, setup and teardown (including `UiTestMockBackend.disarm()`) come from
 * [WarningBottomSheetTestBase].
 */
abstract class FraudWarningTestBase : WarningBottomSheetTestBase() {

    protected val fraudWarning = FraudWarningScreen()
    protected val ibanSelection = IbanSelectionScreen()
    protected val analysisScreen = AnalysisScreen()

    /**
     * Arms the mock with [scenario] and runs the photo-picker flow up to "Process".
     * [fraudSignalsEnabled] is the server-side flag the SDK reads; it defaults to on, since most
     * tests are about the warning itself.
     */
    protected fun processWithScenario(
        scenario: UiTestMockScenario,
        fraudSignalsEnabled: Boolean = true
    ) {
        armMockBackend(scenario, fraudSignalsEnabled = fraudSignalsEnabled)
        uploadFixtureInvoiceAndProcess(ANY_DOCUMENT)
    }

    /** Waits for the warning of [type] and fails the test when it does not appear. */
    protected fun assertWarningShown(type: FraudWarningScreen.Type) {
        assertTrue("Fraud warning $type did not appear", fraudWarning.waitForWarning(type))
    }

    /**
     * No fraud warning: the flow went straight to the example app's Extractions screen. The
     * warning blocks the flow, so reaching that screen already proves it was never shown; the
     * second check makes the failure message say so.
     */
    protected fun assertNoWarningAndExtractionsShown() {
        assertTrue(
            "Extraction screen did not appear",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertFalse(
            "Fraud warning must not appear",
            fraudWarning.isAnyWarningDisplayed()
        )
    }

    /**
     * The compound extractions of the feedback the SDK sent through the mock, waiting up to
     * [timeoutMs] for it, or `null` when none was sent.
     */
    protected fun awaitFeedbackCompoundExtractions(
        timeoutMs: Long = FEEDBACK_TIMEOUT
    ): Map<String, GiniCaptureCompoundExtraction>? =
        awaitNotNull(timeoutMs) { UiTestMockBackend.lastFeedbackCompoundExtractions }

    /**
     * The specific extractions of the feedback the SDK sent through the mock, as name → value,
     * waiting up to [timeoutMs] for it, or `null` when none was sent.
     */
    protected fun awaitFeedbackExtractionValues(
        timeoutMs: Long = FEEDBACK_TIMEOUT
    ): Map<String, String>? =
        awaitNotNull(timeoutMs) { UiTestMockBackend.lastFeedbackExtractionValues }

    private fun <T : Any> awaitNotNull(timeoutMs: Long, read: () -> T?): T? {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < end) {
            read()?.let { return it }
            SystemClock.sleep(POLL_INTERVAL)
        }
        return read()
    }

    companion object {
        /**
         * Something for the photo picker to select. The mock answers from its scenario and never
         * looks at the bytes, as in [CreditNoteMockBackendTests].
         */
        const val ANY_DOCUMENT = CreditNoteFixtures.PLAIN_INVOICE_ASSET

        private const val FEEDBACK_TIMEOUT = 5_000L
        private const val POLL_INTERVAL = 200L
    }
}

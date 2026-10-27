package net.gini.android.bank.sdk.exampleapp.ui.testcases

import androidx.test.platform.app.InstrumentationRegistry
import net.gini.android.bank.sdk.exampleapp.ui.screens.ErrorScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.NoResultScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockAnalysisFailure
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import net.gini.android.capture.R
import net.gini.android.capture.error.ErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Error and No Results screens, driven by the canned backend
 * ([net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend]) — Xray folder
 * "Error & No result screens" (PP-3918; the case → test table is in specs/PP-3918-feature.md).
 *
 * The SDK picks the error screen from the failed request's status code or cause
 * (`ErrorType.typeFromError`). A device cannot make the real API answer 401 or 503, and
 * BrowserStack blocks switching the network off, so each screen is forced through the mock
 * instead. The real offline and real upload-error checks stay in [ErrorScreenTests] for local runs.
 *
 * Every test takes a camera photo, because the "Back To Camera" button is only shown for a camera
 * document (`ErrorFragmentImpl.shouldAllowRetakeImages`). On BrowserStack the camera sees the device
 * rack; the mock ignores the bytes.
 *
 * The "Login Error Screens" cases (PP-2911 – PP-2917) share these tests: a failed login reaches the
 * same mapping, so the screen is identical. The login request itself stays a manual Charles check.
 */
class ErrorAndNoResultScreenMockBackendTests : WarningBottomSheetTestBase() {

    private val errorScreen = ErrorScreen()
    private val noResultScreen = NoResultScreen()

    /** PP-2895, PP-2903 — no internet. The real offline variant is [ErrorScreenTests.test2_verifyNetworkErrorScreen]. */
    @Test
    fun test1_noConnection_showsConnectionErrorScreen() {
        showErrorScreenFor(UiTestMockAnalysisFailure.NO_CONNECTION)
        assertErrorScreen(ErrorType.NO_CONNECTION)
    }

    /** PP-2896, PP-2901, PP-2911, PP-2914 — the backend refuses the document (400). */
    @Test
    fun test2_status400_showsUploadErrorScreen() {
        showErrorScreenFor(UiTestMockAnalysisFailure.UPLOAD)
        assertErrorScreen(ErrorType.UPLOAD)
    }

    /** PP-2897, PP-2912 — authentication failed (401). */
    @Test
    fun test3_status401_showsAuthErrorScreen() {
        showErrorScreenFor(UiTestMockAnalysisFailure.AUTH)
        assertErrorScreen(ErrorType.AUTH)
    }

    /** PP-2898, PP-2917 — a failure with no status code and no known cause. */
    @Test
    fun test4_genericFailure_showsUnexpectedErrorScreen() {
        showErrorScreenFor(UiTestMockAnalysisFailure.GENERIC)
        assertErrorScreen(ErrorType.GENERAL)
    }

    /**
     * PP-2899, PP-2916 — an unexpected server response (502). Same title as the generic error, so
     * the body text is what tells the two apart.
     */
    @Test
    fun test5_status502_showsServerErrorScreen() {
        showErrorScreenFor(UiTestMockAnalysisFailure.SERVER)
        assertErrorScreen(ErrorType.SERVER)
    }

    /** PP-2900, PP-2915 — maintenance (503). */
    @Test
    fun test6_status503_showsMaintenanceErrorScreen() {
        showErrorScreenFor(UiTestMockAnalysisFailure.MAINTENANCE)
        assertErrorScreen(ErrorType.MAINTENANCE)
    }

    /** PP-2913 — service unavailable (500). */
    @Test
    fun test7_status500_showsServiceUnavailableErrorScreen() {
        showErrorScreenFor(UiTestMockAnalysisFailure.OUTAGE)
        assertErrorScreen(ErrorType.OUTAGE)
    }

    /** PP-2902 — "Back To Camera" returns to the camera screen. */
    @Test
    fun test8_backToCamera_returnsToCameraScreen() {
        showErrorScreenFor(UiTestMockAnalysisFailure.UPLOAD)
        assertTrue("The error screen never appeared", errorScreen.waitForErrorHeader())

        errorScreen.clickBackToCameraButton()

        assertEquals(true, captureScreen.checkScanTextDisplayed())
    }

    /**
     * PP-2904 — a camera photo that yields no extractions opens the No Results screen. The upload
     * variant is [NoResultsTests.test1_uploadInvalidImageAndClickEnterManuallyButton_NavigatesToMainScreen].
     */
    @Test
    fun test9_cameraPhotoNoResults_showsNoResultScreen() {
        armMockBackend(UiTestMockScenario.NO_RESULTS)
        takePhotoAndProcess()

        assertTrue(
            "The No Results screen never appeared",
            noResultScreen.waitForNoResultScreen(R.string.gc_title_no_results)
        )
        assertEquals(true, noResultScreen.checkElementWithTextIsDisplayed(R.string.gc_title_no_results))
        assertEquals(true, noResultScreen.checkElementWithTextIsDisplayed(R.string.gc_noresults_header))
        assertEquals(
            true,
            noResultScreen.checkElementWithTextIsDisplayed(R.string.gc_noresults_enter_manually)
        )
    }

    private fun showErrorScreenFor(failure: UiTestMockAnalysisFailure) {
        armMockBackend(UiTestMockScenario.ANALYSIS_ERROR, analysisFailure = failure)
        takePhotoAndProcess()
    }

    /**
     * Checks the screen shows [type]'s own title and text, plus both buttons. The expected strings
     * come from [type], so a mock that ignored the status code would fail here.
     */
    private fun assertErrorScreen(type: ErrorType) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val title = context.getString(type.titleTextResource)
        val text = context.getString(type.descriptionTextResource)

        assertTrue("The error screen never appeared", errorScreen.waitForErrorHeader())
        assertEquals(true, errorScreen.checkErrorTextDisplayed())
        assertEquals("Header of the $type error screen", true, errorScreen.checkErrorHeaderTextDisplayed(title))
        assertEquals("Text of the $type error screen", true, errorScreen.checkErrorTextViewDisplayed(text))
        assertEquals(true, errorScreen.checkEnterManuallyButtonIsDisplayed())
        assertEquals(true, errorScreen.checkBackToCameraButtonIsDisplayed())
    }
}

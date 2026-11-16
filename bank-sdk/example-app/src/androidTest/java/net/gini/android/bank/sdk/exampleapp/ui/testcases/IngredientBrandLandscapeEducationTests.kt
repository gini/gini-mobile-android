package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.resources.LandscapeDarkModeRule
import net.gini.android.bank.sdk.exampleapp.ui.screens.AnalysisScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The invoice education with the Gini ingredient brand, with the device turned to landscape on the
 * SDK's camera screen, in dark mode (epic PP-2568, UI tests PP-3478). The portrait/light version is
 * [IngredientBrandEducationTests].
 *
 * The education is a Compose animation, so this class needs system animations ON — it runs in
 * `bs_run_group_ingredientbrand_education.sh`.
 */
class IngredientBrandLandscapeEducationTests : WarningBottomSheetTestBase() {

    /** Wraps the activity rule, so the app is launched already in dark mode. */
    @get:Rule(order = Int.MIN_VALUE + 1)
    val landscapeDarkMode = LandscapeDarkModeRule()

    private val analysisScreen = AnalysisScreen()

    @Before
    fun checkDarkMode() {
        // Skipped, not failed, where the device does not let a test switch dark mode (like the
        // network toggle in ErrorScreenTests on BrowserStack).
        Assume.assumeTrue(
            "Dark mode could not be switched on for this device",
            landscapeDarkMode.isDarkModeOn()
        )
    }

    /** Opens the SDK in portrait, then turns the device to landscape on the camera screen. */
    private fun openSdkAndRotateToLandscape() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        assertTrue("Camera screen did not appear", captureScreen.checkScanTextDisplayed())
        landscapeDarkMode.rotateToLandscape()
        assertTrue("Device is not in landscape", landscapeDarkMode.isLandscape())
    }

    /**
     * Lets the education finish before the activity rule closes the app — closing resumes the
     * Analysis screen, and a resume during the education freezes the SDK's main thread (see
     * [IngredientBrandEducationTests]).
     */
    @After
    fun waitForEducationToFinish() {
        analysisScreen.waitForInvoiceEducationGone(EDUCATION_DURATION_MS)
    }

    /** Photo with education: the badge is shown, but no indicator and no tip. */
    @Test
    fun test1_landscapeDark_photo_education_showsBadgeWithoutIndicator() {
        armMockBackend(
            UiTestMockScenario.INVOICE,
            creditNoteHintEnabled = false,
            ingredientBrandScreens = setOf("Analysis"),
            qrCodeEducationEnabled = true,
            analysisDelayMillis = ANALYSIS_DELAY_MS
        )

        openSdkAndRotateToLandscape()
        takePhotoOnCameraScreenAndProcess()

        assertTrue("Invoice education did not appear", analysisScreen.waitForInvoiceEducation())
        assertTrue("Powered by Gini badge is not shown", analysisScreen.isPoweredByGiniDisplayed())
        assertFalse("No Gini loading indicator expected", analysisScreen.isGiniLoadingIndicatorDisplayed())
        assertFalse("No default loading indicator expected", analysisScreen.isDefaultLoadingIndicatorDisplayed())
        assertFalse("No capture suggestion expected", analysisScreen.isTipDisplayed())
    }

    private companion object {
        /** Longer than the 4.5 s education, so the analysis is still pending while it shows. */
        const val ANALYSIS_DELAY_MS = 12_000L

        /** Intro + message, 4.5 s, plus a margin. */
        const val EDUCATION_DURATION_MS = 7_500L
    }
}

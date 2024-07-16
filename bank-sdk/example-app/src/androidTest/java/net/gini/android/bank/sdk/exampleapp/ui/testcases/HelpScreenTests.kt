package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.HelpScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import org.junit.Rule
import org.junit.Test

/**
 * Test class for help screen flow.
 *
 * Jira link for test case: [https://ginis.atlassian.net/browse/PM-23](https://ginis.atlassian.net/browse/PM-23)
 */
class HelpScreenTests {

    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get: Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val captureScreen = CaptureScreen()
    private val helpScreen = HelpScreen()

    @Test
    fun test2_verifyHelpItemTipsForBestResult() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickHelpButton()
        helpScreen.assertTipsForBestResultsExists()
        helpScreen.clickTipsForBestResults()
        helpScreen.clickBackButton()
    }

    @Test
    fun test3_verifyHelpItemSupportedFormats() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickHelpButton()
        helpScreen.assertSupportedFormatsExists()
        helpScreen.clickSupportedFormats()
        helpScreen.clickBackButton()
    }

    @Test
    fun test4_verifyHelpItemImportDocuments() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickHelpButton()
        helpScreen.assertImportDocsExists()
        helpScreen.clickImportDocs()
        helpScreen.clickBackButton()
    }
}
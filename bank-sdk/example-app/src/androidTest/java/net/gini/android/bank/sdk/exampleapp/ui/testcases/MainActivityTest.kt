package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.HelpScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @get: Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get: Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val captureScreen = CaptureScreen()
    private val helpScreen = HelpScreen()

    @Before
    fun setUp() {
        Intents.init()
    }
    @Test
    fun test1_assertTitlesOnMainScreen() {
        mainScreen.checkWelcomeTitleIsDisplayed()
        mainScreen.assertDescriptionTitle()
    }
    @Test
    fun test2_clickPhotoPaymentButton() {
        mainScreen.checkScannerButtonDisplayed()
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.assertSkipButtonText()
        onboardingScreen.clickSkipButton()
        captureScreen.assertCameraTitle()
        captureScreen.clickCancelButton()
    }

    @Test
    fun test3_clickHelpButton() {
        mainScreen.clickPhotoPaymentButton()
        captureScreen.clickHelpButton()
    }

    @Test
    fun test4_verifyHelpItemTipsForBestResult() {
        mainScreen.clickPhotoPaymentButton()
        captureScreen.clickHelpButton()
        helpScreen.clickTipsForBestResults()
        helpScreen.clickBackButton()
    }

    @Test
    fun test5_verifyHelpItemSupportedFormats() {
        mainScreen.clickPhotoPaymentButton()
        captureScreen.clickHelpButton()
        helpScreen.clickSupportedFormats()
        helpScreen.clickBackButton()
    }

    @Test
    fun test6_verifyHelpItemImportDocuments() {
        mainScreen.clickPhotoPaymentButton()
        captureScreen.clickHelpButton()
        helpScreen.clickImportDocs()
        helpScreen.clickBackButton()
    }


    @Test
    fun test7_pdfImportFromFiles() {
        mainScreen.clickPhotoPaymentButton()
        val resultData = Intent()
        val fileUri =
            Uri.parse("/Users/syedaquratulainasad/Documents/Gini/GitHub/gini-mobile-android/bank-sdk/example-app/src/androidTest/assets/test_pdf.pdf")
        resultData.setData(fileUri)
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
        intending(
            allOf(
                hasAction(Intent.ACTION_OPEN_DOCUMENT),
                hasType("image/*")
            )
        ).respondWith(result)
    }

    @After  
    fun tearDown() {
    }
}
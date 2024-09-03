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
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MainScreenTests {

    @get: Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get: Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val mainScreen = MainScreen()

    @Before
    fun setUp() {
        Intents.init()
    }

    @Test
    fun test1_pdfImportFromFiles() {
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
}
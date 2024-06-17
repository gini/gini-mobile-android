package net.gini.android.bank.sdk.exampleapp.ui

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.R
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

//@FixMethodOrder(MethodSorters.DEFAULT)
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @get: Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get: Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Before
    fun setUp() {
    }

    @Test
    fun test1_clickHelpButtonOnCapture() {
        onView(withId(R.id.button_startScanner)).check(matches(isDisplayed()))
        onView(withId(R.id.button_startScanner)).perform(click())

        onView(withId(net.gini.android.capture.R.id.gc_camera_title)).check(matches(withText("Scan an invoice")))
        onView(withId(net.gini.android.capture.R.id.gc_action_show_onboarding)).perform(click())
    }

    @Test
    fun test2_verifyItemOnHelpScreen() {
        onView(withId(R.id.button_startScanner)).check(matches(isDisplayed()))
        onView(withId(R.id.button_startScanner)).perform(click())

        onView(withId(net.gini.android.capture.R.id.gc_skip)).check(matches(withText("Skip")))
        onView(withId(net.gini.android.capture.R.id.gc_skip)).perform(click())

        onView(withId(net.gini.android.capture.R.id.gc_action_show_onboarding)).perform(click())
        onView(
            allOf(
                withId(net.gini.android.capture.R.id.gc_help_item_title),
                withText("Supported formats"), isDescendantOfA(withId(net.gini.android.capture.R.id.gc_help_items))))
            .check(matches(withText("Supported formats")))

            .perform(click());
    }


    @Test
    fun test3_pdfImportFromFiles() {
        onView(withId(R.id.button_startScanner)).check(matches(isDisplayed()))
        onView(withId(R.id.button_startScanner)).perform(click())
        onView(withId(net.gini.android.capture.R.id.gc_button_import_document)).perform(click())
        // To Do: File import, analyse and then extract
    }
}
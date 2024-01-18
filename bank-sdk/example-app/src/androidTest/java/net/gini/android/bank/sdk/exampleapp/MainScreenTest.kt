package net.gini.android.bank.sdk.exampleapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get : Rule
    var mActivityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        //initial setup code
    }

    @Test
    fun testCaptureDocument() {
        onView(withId(R.id.button_startScanner)).perform(click())

        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val button = uiDevice.findObject(UiSelector().resourceId("com.android.permissioncontroller:id/permission_allow_foreground_only_button"))
        button.click();

        onView(withId(net.gini.android.capture.R.id.gc_skip)).perform(click())
        onView(withId(net.gini.android.capture.R.id.gc_button_camera_trigger)).perform(click())
    }

    @After
    fun tearDown() {
        //clean up code
    }

}
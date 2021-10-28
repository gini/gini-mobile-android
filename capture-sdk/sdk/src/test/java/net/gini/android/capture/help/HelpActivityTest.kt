package net.gini.android.capture.help

import android.content.Intent
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nhaarman.mockitokotlin2.mock
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HelpActivityTest {

    @After
    fun tearDown() {
        GiniCapture.cleanup(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun `shows custom help items`() {
        // Given
        val customTitle = R.string.custom_help_screen_title

        GiniCapture.newInstance()
            .setGiniCaptureNetworkService(mock())
            .setGiniCaptureNetworkApi(mock())
            .setCustomHelpItems(
                listOf(
                    HelpItem.Custom(
                        customTitle,
                        Intent(
                            InstrumentationRegistry.getInstrumentation().targetContext,
                            CustomHelpActivity::class.java
                        )
                    )
                )
            )
            .build()

        // When
        launchActivity<HelpActivity>().use {
            // Then
            onView(ViewMatchers.withText(customTitle)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun `starts custom help activity when custom help item is clicked`() {
        // Given
        val customTitle = R.string.custom_help_screen_title

        GiniCapture.newInstance()
            .setGiniCaptureNetworkService(mock())
            .setGiniCaptureNetworkApi(mock())
            .setCustomHelpItems(
                listOf(
                    HelpItem.Custom(
                        customTitle,
                        Intent(
                            InstrumentationRegistry.getInstrumentation().targetContext,
                            CustomHelpActivity::class.java
                        )
                    )
                )
            )
            .build()

        // When
        Intents.init()

        launchActivity<HelpActivity>().use {
            // Then
            onView(ViewMatchers.withText(customTitle)).perform(click())

            intended(hasComponent(CustomHelpActivity::class.qualifiedName))
        }

        Intents.release()
    }
}
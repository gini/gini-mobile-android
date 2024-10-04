package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import net.gini.android.bank.sdk.exampleapp.R

class ConfigurationScreen {

    fun scrollToAnalysisText(): ConfigurationScreen {
        onView(withText("Analysis"))
            .perform(scrollTo())
        return this
    }

    fun clickFlashToggleToEnable() {
        onView(ViewMatchers.withId(R.id.switch_flashOnByDefault)).perform(
            closeSoftKeyboard(),
            click()
        )
    }

    fun assertFlashToggleIsDisable(): ConfigurationScreen {
        onView(ViewMatchers.withId(R.id.switch_flashOnByDefault)).check(matches(isDisplayed()))
        return this
    }

    fun scrollToUICustomizationText(): ConfigurationScreen {
        onView(withText("UI customization"))
            .perform(scrollTo())
        return this
    }

    fun clickReturnReasonsDialogToEnable(): ConfigurationScreen {
        onView(ViewMatchers.withId(R.id.switch_returnReasonsDialog)).perform(click()
        )
        return this
    }
}
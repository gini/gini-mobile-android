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

    fun clickTransactionDocsSwitch(): ConfigurationScreen {
        onView(ViewMatchers.withId(R.id.switch_transactionDocsFeature))
            .perform(closeSoftKeyboard(), scrollTo(), click())
        return this
    }

    fun scrollToUICustomizationText(): ConfigurationScreen {
        onView(withText("UI customization"))
            .perform(scrollTo())
        return this
    }

    fun scrollToProductTagSwitch(): ConfigurationScreen {
        onView(ViewMatchers.withId(R.id.switch_product_tag_cx))
            .perform(scrollTo())
        return this
    }

    fun assertProductTagSwitchIsChecked(): ConfigurationScreen {
        onView(ViewMatchers.withId(R.id.switch_product_tag_cx))
            .check(matches(ViewMatchers.isChecked()))
        return this
    }

    fun assertProductTagSwitchIsUnchecked(): ConfigurationScreen {
        onView(ViewMatchers.withId(R.id.switch_product_tag_cx))
            .check(matches(ViewMatchers.isNotChecked()))
        return this
    }

    fun clickProductTagCxSwitch(): ConfigurationScreen {
        onView(ViewMatchers.withId(R.id.switch_product_tag_cx))
            .perform(closeSoftKeyboard(), scrollTo(), click())
        return this
    }

}
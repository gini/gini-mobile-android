package net.gini.android.bank.sdk.exampleapp.ui.screens

import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiCollection
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertNotEquals

class DigitalInvoiceScreen {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var initialValue: String? = null
    private var updatedValue: String? = null


    fun displayDigitalInvoiceTextOnOnboardingScreen(): DigitalInvoiceScreen {
        val onboardingScreenText = device.findObject(
            UiSelector()
                .className("android.widget.TextView")
                .text("Digital invoice")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/onboarding_text_1")
        )
        onboardingScreenText.exists()
        return this
    }

    fun displayGetStartedButtonOnOnboardingScreen(): DigitalInvoiceScreen {
        val onboardingScreenButton = device.findObject(
            UiSelector()
                .className("android.widget.Button")
                .text("Get Started")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/done_button")
        )
        onboardingScreenButton.exists()
        return this
    }

    fun clickGetStartedButtonOnOnboardingScreen(): DigitalInvoiceScreen {
        val onboardingScreenButton = device.findObject(
            UiSelector()
                .className("android.widget.Button")
                .text("Get Started")
        )
        onboardingScreenButton.exists()
        onboardingScreenButton.click()
        return this
    }

    fun assertDigitalInvoiceText(): DigitalInvoiceScreen {
        val digitalInvoiceText = device.findObject(
            UiSelector()
                .className("android.widget.TextView")
                .text("Digital Invoice")
        )
        digitalInvoiceText.exists()
        return this
    }

    fun clickCancelButton(): DigitalInvoiceScreen {
        val cancelButton = device.findObject(
            UiSelector()
                .className("android.widget.ImageButton")
                .descriptionContains("Close")
        )
        cancelButton.exists()
        cancelButton.click()
        return this
    }

    fun assertOtherChargesDisplayed(): DigitalInvoiceScreen {
        onView(withText(net.gini.android.bank.sdk.R.string.gbs_digital_invoice_addon_other_charges)).check(
            matches(isDisplayed()))
        return this
    }

    fun clickProceedButton(): DigitalInvoiceScreen {
        onView(withText(net.gini.android.bank.sdk.R.string.gbs_proceed)).perform(click())
        return this
    }

    fun clickArticleSwitch(): DigitalInvoiceScreen {
        val articleSwitch = device.findObject(
            UiSelector()
                .className("android.widget.Switch")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/gbs_enable_switch")
                .index(1)
        )
        articleSwitch.exists()
        articleSwitch.isClickable
        articleSwitch.click()
        return this
    }

    fun checkForReturnReasonsList(): DigitalInvoiceScreen {
        onView(withText(net.gini.android.bank.sdk.R.string.gbs_digital_invoice_return_reason_dialog_title)).check(
            matches(isDisplayed()))
        return this
    }

    fun  checkItemCountOnReturnReasonsList(): DigitalInvoiceScreen {
        val uiCollection =
            UiCollection(UiSelector().className("android.widget.ListView"))
        val itemSize = uiCollection.childCount
        onView(withClassName(`is`("android.widget.ListView"))).check(matches(hasChildCount(itemSize)))
        return this
    }

    fun  clickItemOnReturnReasonsList(): DigitalInvoiceScreen {
        val uiCollection =
            UiCollection(UiSelector().className("android.widget.ListView"))
        val returnReasonsItems = uiCollection.getChildByInstance(
            UiSelector().className("android.widget.TextView"), 0)
        returnReasonsItems.click()
        return this
    }

    fun  checkItemIsDisabledFromDigitalScreen(): Boolean {
        val returnReasonsItems = device.findObject(UiSelector()
            .className("android.view.ViewGroup")
            .resourceId("net.gini.android.bank.sdk.exampleapp:id/gsb_line_item")
            .index(0))
        return returnReasonsItems.isEnabled
    }

    fun  checkItemIsEnabledFromDigitalScreen(): Boolean {
        val returnReasonsItems = device.findObject(UiSelector()
            .className("android.view.ViewGroup")
            .resourceId("net.gini.android.bank.sdk.exampleapp:id/gsb_line_item")
            .index(0))
        return returnReasonsItems.isEnabled
    }

    fun clickHelpButtonOnDigitalInvoiceScreen(): DigitalInvoiceScreen {
        val helpButton = device.findObject(
            UiSelector()
                .className("android.widget.Button")
                .descriptionContains("Help")
        )
        helpButton.exists()
        helpButton.click()
        return this
    }

    fun verifyHelpTextOnNextScreen(): DigitalInvoiceScreen {
        val helpText = device.findObject(
            UiSelector()
                .className("android.widget.TextView")
                .text("Help")
        )
        helpText.exists()
        helpText.click()
        return this
    }

    fun verifyFirstTitleOnHelpScreen(): DigitalInvoiceScreen {
        onView(
            allOf(withId(net.gini.android.bank.sdk.R.id.gbs_help_title),
                withText("1. How does a digital invoice work?")
            )
        )
            .perform(click())
        return this
    }

    fun checkTotalTitleIsDisplayed(): DigitalInvoiceScreen {
        onView(
            allOf(withId(net.gini.android.bank.sdk.R.id.total_label),
                withText("Total")
            )
        )
            .check(matches(isDisplayed()))
        return this
    }

    fun checkTotalPriceIsDisplayed(): DigitalInvoiceScreen {
        onView(withId(net.gini.android.bank.sdk.R.id.gross_price_total_integral_part))
            .check(matches(isDisplayed()))
        return this
    }

    fun storeInitialPrice(): DigitalInvoiceScreen {
        onView(withId(net.gini.android.bank.sdk.R.id.gross_price_total_integral_part))
            .check { view, _ ->
                val totalTextView = view as TextView
                initialValue = totalTextView.text.toString()
            }
        return this
    }

    fun storeUpdatedPrice(): DigitalInvoiceScreen {
        onView(withId(net.gini.android.bank.sdk.R.id.gross_price_total_integral_part))
            .check { view, _ ->
                val totalTextView = view as TextView
                updatedValue = totalTextView.text.toString()
            }
        return this
    }

    fun assertPriceHasChanged(): DigitalInvoiceScreen {
        assertNotEquals("Price should have changed after toggling", initialValue, updatedValue)
        return this
    }
}
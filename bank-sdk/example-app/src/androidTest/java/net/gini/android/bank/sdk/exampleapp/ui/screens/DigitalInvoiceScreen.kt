package net.gini.android.bank.sdk.exampleapp.ui.screens

import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiCollection
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`


class DigitalInvoiceScreen {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var initialValue: String? = null
    private var updatedValue: String? = null


    fun checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed(): Boolean {
        val onboardingScreenText = device.findObject(
            UiSelector()
                .className("android.widget.TextView")
                .text("Digital invoice")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/onboarding_text_1")
        )
        return onboardingScreenText.waitForExists(8000)
    }

    fun checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed(): Boolean {
        val onboardingScreenButton = device.findObject(
            UiSelector()
                .className("android.widget.Button")
                .text("Get Started")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/done_button")
        )
        return onboardingScreenButton.waitForExists(5000)
    }

    fun clickGetStartedButtonOnOnboardingScreen() {
        val onboardingScreenButton = device.findObject(
            UiSelector()
                .className("android.widget.Button")
                .text("Get Started")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/done_button")
        )
        if (onboardingScreenButton.waitForExists(5000) && onboardingScreenButton.isClickable) {
            onboardingScreenButton.click()
        }
    }

    fun clickCancelButton() {
        val cancelButton = device.findObject(
            UiSelector()
                .className("android.widget.ImageButton")
                .descriptionContains("Close")
        )
        if(cancelButton.exists() && cancelButton.isClickable()){
            cancelButton.click()
        }
    }

    fun assertOtherChargesDisplayed() : Boolean {
        var isOtherChargesDisplayed = false
        onView(withText(net.gini.android.bank.sdk.R.string.gbs_digital_invoice_addon_other_charges))
            .check { view,_ ->
                if (view.isShown()) {
                    isOtherChargesDisplayed = true
                }
            }
        return isOtherChargesDisplayed
    }

    fun clickProceedButton() {
        onView(withText(net.gini.android.bank.sdk.R.string.gbs_proceed)).perform(click())
    }

    fun clickArticleSwitch(): DigitalInvoiceScreen {
        val articleSwitch = device.findObject(
            UiSelector()
                .className("android.widget.Switch")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/gbs_enable_switch")
                .index(1)
        )
        if(articleSwitch.exists() && articleSwitch.isClickable){
            articleSwitch.click()
        }
        return this
    }

    fun checkForReturnReasonsList(): Boolean {
        var isReturnReasonDisplayed = false
        onView(withText(net.gini.android.bank.sdk.R.string.gbs_digital_invoice_return_reason_dialog_title))
            .check { view, _ ->
                if (view.isShown()) {
                    isReturnReasonDisplayed = true
                }
            }
        return isReturnReasonDisplayed
    }

    fun  returnItemCountOnReturnReasonsList(): Int{
        val uiCollection =
            UiCollection(UiSelector().className("android.widget.ListView"))
        val itemSize = uiCollection.childCount
        onView(withClassName(`is`("android.widget.ListView"))).check(matches(hasChildCount(itemSize)))
        return itemSize
    }

    fun  clickItemOnReturnReasonsList() {
        val uiCollection =
            UiCollection(UiSelector().className("android.widget.ListView"))
        val returnReasonsItems = uiCollection.getChildByInstance(
            UiSelector().className("android.widget.TextView"), 0)
        returnReasonsItems.click()
    }

    fun  checkItemIsDisabledFromDigitalScreen(): Boolean {
        val returnReasonsItems = device.findObject(UiSelector()
            .className("android.view.ViewGroup")
            .resourceId("net.gini.android.bank.sdk.exampleapp:id/gsb_line_item")
            .index(0))
        return !(returnReasonsItems.isEnabled)
    }

    fun  checkItemIsEnabledFromDigitalScreen(): Boolean {
        val returnReasonsItems = device.findObject(UiSelector()
            .className("android.view.ViewGroup")
            .resourceId("net.gini.android.bank.sdk.exampleapp:id/gsb_line_item")
            .index(0))
        return returnReasonsItems.isEnabled
    }

    fun clickHelpButtonOnDigitalInvoiceScreen() {
        val helpButton = device.findObject(
            UiSelector()
                .className("android.widget.Button")
                .descriptionContains("Help")
        )
        if(helpButton.exists()){
            helpButton.click()
        }
    }

    fun verifyHelpTextOnNextScreen(): Boolean {
        val helpText = device.findObject(
            UiSelector()
                .className("android.widget.TextView")
                .text("Help")
        )
        return helpText.exists()
    }

    fun verifyFirstTitleOnHelpScreen(): Boolean {
        var isFirstTitleDisplayed = false
        onView(
            allOf(withId(net.gini.android.bank.sdk.R.id.gbs_help_title),
                withText("1. How does a digital invoice work?")
            )
        )
            .check { view, _ ->
                if (view.isShown()) {
                    isFirstTitleDisplayed = true
                }
            }
        return isFirstTitleDisplayed
    }

    fun checkTotalTitleIsDisplayed(): Boolean {
        var isTotalTitleDisplayed = false
        onView(
            allOf(withId(net.gini.android.bank.sdk.R.id.total_label),
                withText("Total")
            )
        )
            .check { view, _ ->
                if (view.isShown()) {
                    isTotalTitleDisplayed = true
                }
            }
        return isTotalTitleDisplayed
    }

    fun checkTotalPriceIsDisplayed() : Boolean{
        var isTotalPriceDisplayed = false
        onView((withId(net.gini.android.bank.sdk.R.id.gross_price_total_integral_part)))
            .check { view, _ ->
                if (view.isShown()) {
                    isTotalPriceDisplayed = true
                }
            }
        return isTotalPriceDisplayed
    }

    fun storeInitialPrice() {
        onView(withId(net.gini.android.bank.sdk.R.id.gross_price_total_integral_part))
            .check { view, _ ->
                val totalTextView = view as TextView
                initialValue = totalTextView.text.toString()
            }
    }

    fun verifyTotalSumValue(): Boolean{
        var isSumDifferent = false
            if(initialValue!=updatedValue) {
            isSumDifferent = true
        }
        return isSumDifferent
    }

    fun storeUpdatedPrice() {
        onView(withId(net.gini.android.bank.sdk.R.id.gross_price_total_integral_part))
            .check { view, _ ->
                val totalTextView = view as TextView
                updatedValue = totalTextView.text.toString()
            }
    }
}
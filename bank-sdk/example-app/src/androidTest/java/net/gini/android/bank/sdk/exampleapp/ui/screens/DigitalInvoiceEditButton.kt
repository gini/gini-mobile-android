package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiCollection
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Matchers.allOf

class DigitalInvoiceEditButton {

    fun checkEditButtonTitleIsDisplayed() :Boolean {
        val uiCollection =
            UiCollection(UiSelector().className("android.view.ViewGroup"))
        val editButton = uiCollection.getChildByInstance(
            UiSelector().className("android.widget.TextView")
                .text("Edit")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/gbs_edit_button"), 0)
        return editButton.isEnabled
    }

    fun clickEditButtonOnDigitalInvoiceScreen() {
        val uiCollection =
            UiCollection(UiSelector().className("android.view.ViewGroup"))
        val editButton = uiCollection.getChildByInstance(
            UiSelector().className("android.widget.TextView")
                .text("Edit")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/gbs_edit_button"), 0)
        if(editButton.exists() && editButton.isEnabled) {
            editButton.click()
        }
    }

    fun checkElementTitleIsDisplayed(resourceId: Int): Boolean {
        var isElementTitleDisplayed = false
        onView(withText(resourceId))
            .check { view, _ ->
                if (view.isShown()) {
                    isElementTitleDisplayed = true
                }
            }
        return isElementTitleDisplayed
    }

    fun editElementTextOnArticleBottomSheet(resourceId: Int, text: String) {
        onView((withId(resourceId))).perform(click()).perform(replaceText(text))
    }

    fun removeTextFromElementOnArticleBottomSheet(resourceId: Int) {
        onView((withId(resourceId))).perform(click()).perform(clearText())
    }

    fun verifyInlineErrorOnElement(resourceId: Int, inlineError: String): Boolean {
        var isInlineErrorDisplayed = false
        onView(allOf(withId(resourceId),
            withText(inlineError)))
            .check { view, _ ->
            if (view.isShown()) {
                isInlineErrorDisplayed = true
            }
        }
            return isInlineErrorDisplayed
    }

    fun clickSaveButtonOnEditArticleBottomSheet() {
        onView(withText(net.gini.android.bank.sdk.R.string.gbs_digital_invoice_line_item_details_save)).perform(click())
    }

    fun clickCancelButtonOnEditArticleBottomSheet() {
        onView(withId(net.gini.android.bank.sdk.R.id.gbs_close_bottom_sheet)).perform(click())
    }

    fun saveNameValueOnDigitalInvoiceScreen() {
        val uiCollection =
            UiCollection(UiSelector().className("android.view.ViewGroup"))
        val textView = uiCollection.getChildByInstance(
            UiSelector().className("android.widget.TextView")
                .resourceId("R.id.gbs_description")
            , 0)
        if(textView.exists() && textView.isEnabled) {
            textView.text
        }
    }

    fun checkNameIsUpdated(text: String): Boolean {
        val uiCollection =
            UiCollection(UiSelector().className("android.view.ViewGroup"))
        val textView = uiCollection.getChildByInstance(
            UiSelector().className("android.widget.TextView")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/gbs_description")
            , 0)
        if (textView.exists()) {
            val actualText = textView.text.replace(Regex("\\d+x\\s*"), "")
            if (actualText == text) {
                return true
            }
        }
        return false
    }

    fun checkUnitPriceIsUpdated(text: String): Boolean {
        val uiCollection =
            UiCollection(UiSelector().className("android.view.ViewGroup"))
        val textView = uiCollection.getChildByInstance(
            UiSelector().className("android.widget.TextView")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/gbs_per_unit")
            , 0)
        if (textView.exists()) {
            val actualText = textView.text
            val expectedText = "€$text per unit"
            if (actualText == expectedText) {
                return true
            }
        }
        return false
    }

    fun checkQuantityIsUpdated(text: String): Boolean {
        val uiCollection =
            UiCollection(UiSelector().className("android.view.ViewGroup"))
        val textView = uiCollection.getChildByInstance(
            UiSelector().className("android.widget.TextView")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/gbs_description")
            , 0)
        if (textView.exists()) {
            val actualText = extractQuantity(textView.text)
            if (actualText == text) {
                return true
            }
        }
        return false
    }

    fun clickPlusToIncreaseQuantity(increaseTimes: Int){
        for (i in 1 until increaseTimes) {
            onView(withId(net.gini.android.bank.sdk.R.id.gbs_add_quantity))
                .perform((click()))
        }
    }

    fun clickMinusToDecreaseQuantity(decreaseTimes: Int){
        for (i in 4 downTo decreaseTimes) {
            onView(withId(net.gini.android.bank.sdk.R.id.gbs_remove_quantity))
                .perform((click()))
        }
    }

    fun checkCurrencyForUnitPrice(): Boolean {
        var isCurrencyEuro = false
        onView(allOf(withId(net.gini.android.bank.sdk.R.id.gbs_drop_down_selection_value), withText("EUR")))
            .check { view, _ ->
                if (view.isShown()) {
                    isCurrencyEuro = true
                }
            }
        return isCurrencyEuro
    }

    private fun extractQuantity(input: String): String? {
        val regex = Regex("""^(\d+)x""")
        val matchResult = regex.find(input)
        return matchResult?.groups?.get(1)?.value
    }
}
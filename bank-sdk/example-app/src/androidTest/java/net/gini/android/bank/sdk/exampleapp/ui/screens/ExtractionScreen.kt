package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import net.gini.android.bank.sdk.exampleapp.R
import org.hamcrest.Matchers.allOf

class ExtractionScreen {

    fun clickTransferSummaryButton(): ExtractionScreen {
        onView(withId(R.id.transfer_summary)).perform(click())
        return this
    }

    fun editTransferSummaryFields(hint: String, value: String) {
        onView(allOf(withHint(hint)))
//            .perform(click())
            .perform(replaceText(value))
//        val photoList = UiScrollable(UiSelector().scrollable(true))
//        val firstPhoto: UiObject = photoList.getChildByInstance(
//            UiSelector().className("com.google.android.material.textfield.TextInputLayout"),
//            3
//        )
//        firstPhoto.text = value
//
//        val sd =""

    }

    fun checkTransferSummaryButtonIsClickable(): Boolean {
        var isTransferSummaryButtonClickable = false
        onView(withText("Send Feedback and Close")).check { view, noViewFoundException ->
            if (noViewFoundException == null || view.isClickable()) {
                isTransferSummaryButtonClickable = true
            }
        }
        return isTransferSummaryButtonClickable
    }
}
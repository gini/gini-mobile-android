package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import net.gini.android.bank.sdk.exampleapp.R
import androidx.test.espresso.assertion.ViewAssertions.matches

class MainScreen {
    fun assertDescriptionTitle(): Boolean {
        var isDescriptionTitleDisplayed = false
        onView(withId(R.id.tv_exampleOfPhotoPayment))
            .check { view, noViewFoundException ->
                if (noViewFoundException == null || view.isShown()) {
                    isDescriptionTitleDisplayed = true
                }
            }
        return isDescriptionTitleDisplayed
    }

    fun clickPhotoPaymentButton() {
        onView(withId(R.id.button_startScanner)).perform(click())
    }

    fun clickSettingButton() {
        onView(withId(R.id.text_giniBankVersion)).perform(click())
    }
}
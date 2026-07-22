package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText

class NoResultScreen {

    fun checkElementWithTextIsDisplayed(resourceId: Int) : Boolean {
        var isElementDisplayed = false
        onView(withText(resourceId)).
            check { view, _ ->
                // When the view is not found, Espresso passes a null view (with the
                // NoMatchingViewException in the second param). Guard against null so a
                // missing element returns false instead of throwing a NullPointerException.
                if (view != null && view.isShown()) {
                    isElementDisplayed = true
            }
        }
        return isElementDisplayed
    }

    fun clickEnterManuallyButton() {
        onView(withText(net.gini.android.capture.R.string.gc_noresults_enter_manually)).perform(
            click())
    }
}
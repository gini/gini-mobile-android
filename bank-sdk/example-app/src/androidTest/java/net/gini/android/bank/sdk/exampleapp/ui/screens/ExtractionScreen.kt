package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import net.gini.android.bank.sdk.exampleapp.R
import org.hamcrest.Matchers.allOf

class ExtractionScreen {

    fun clickTransferSummaryButton(): ExtractionScreen {
        onView(withId(R.id.transfer_summary)).perform(click())
        return this
    }

    fun editTransferSummaryFields(hint: String, value: String) {
        onView(allOf(withId(R.id.text_value), withHint(hint)))
            .perform(click())
            .perform(replaceText(value))
    }

    fun checkTransferSummaryButtonIsClickable(): ExtractionScreen {
        onView(withId(R.id.transfer_summary)).check(matches(isClickable()))
        return this
    }
}
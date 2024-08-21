package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import net.gini.android.bank.sdk.exampleapp.R

class ExtractionScreen {

    fun clickTransferSummaryButton(): ExtractionScreen {
        onView(withId(R.id.transfer_summary)).perform(click())
        return this
    }
}
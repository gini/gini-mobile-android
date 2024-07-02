package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.allOf

class HelpScreen {
    fun clickTipsForBestResults(): HelpScreen {
        onView(
            allOf(
                withId(net.gini.android.capture.R.id.gc_help_item_title),
                withText("Tips for best results from photos"),
                isDescendantOfA(withId(net.gini.android.capture.R.id.gc_help_items))
            )
        )
            .check(matches(withText("Tips for best results from photos")))
            .perform(click());
        return this
    }

    fun clickSupportedFormats(): HelpScreen {
        onView(
            allOf(
                withId(net.gini.android.capture.R.id.gc_help_item_title),
                withText("Supported formats"),
                isDescendantOfA(withId(net.gini.android.capture.R.id.gc_help_items))
            )
        )
            .check(matches(withText("Supported formats")))
            .perform(click());
        return this
    }

    fun clickImportDocs(): HelpScreen {
        onView(
            allOf(
                withId(net.gini.android.capture.R.id.gc_help_item_title),
                withText("Import documents from other apps"),
                isDescendantOfA(withId(net.gini.android.capture.R.id.gc_help_items))
            )
        )
            .check(matches(withText("Import documents from other apps")))
            .perform(click());
        return this
    }

    fun clickBackButton(): HelpScreen {
        //onView(withText("Back")).perform(click())
        //onView(withId(android.R.id.home)).perform(click())
        //onView(withId(net.gini.android.capture.R.id.gc_navigation_bar)).perform(click())
        onView(withContentDescription(net.gini.android.capture.R.string.gc_back_button_description)).perform(click())
        return this
    }
}
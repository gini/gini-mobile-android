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
                withText(net.gini.android.capture.R.string.gc_help_item_photo_tips_title),
                isDescendantOfA(withId(net.gini.android.capture.R.id.gc_help_items))
            )
        )
            .perform(click())
        return this
    }

    fun assertTipsForBestResultsExists(): HelpScreen {
        onView(
            allOf(
                withId(net.gini.android.capture.R.id.gc_help_item_title),
                withText(net.gini.android.capture.R.string.gc_help_item_photo_tips_title),
                isDescendantOfA(withId(net.gini.android.capture.R.id.gc_help_items))
            )
        )
            .check(matches(withText(net.gini.android.capture.R.string.gc_help_item_photo_tips_title)))
        return this
    }

    fun clickSupportedFormats(): HelpScreen {
        onView(
            allOf(
                withId(net.gini.android.capture.R.id.gc_help_item_title),
                withText(net.gini.android.capture.R.string.gc_help_item_supported_formats_title),
                isDescendantOfA(withId(net.gini.android.capture.R.id.gc_help_items))
            )
        )
            .perform(click())
        return this
    }

    fun assertSupportedFormatsExists(): HelpScreen {
        onView(
            allOf(
                withId(net.gini.android.capture.R.id.gc_help_item_title),
                withText(net.gini.android.capture.R.string.gc_help_item_supported_formats_title),
                isDescendantOfA(withId(net.gini.android.capture.R.id.gc_help_items))
            )
        )
            .check(matches(withText(net.gini.android.capture.R.string.gc_help_item_supported_formats_title)))
        return this
    }

    fun clickImportDocs(): HelpScreen {
        onView(
            allOf(
                withId(net.gini.android.capture.R.id.gc_help_item_title),
                withText(net.gini.android.capture.R.string.gc_help_item_file_import_guide_title),
                isDescendantOfA(withId(net.gini.android.capture.R.id.gc_help_items))
            )
        )
            .perform(click())
        return this
    }

    fun assertImportDocsExists(): HelpScreen {
        onView(
            allOf(
                withId(net.gini.android.capture.R.id.gc_help_item_title),
                withText(net.gini.android.capture.R.string.gc_help_item_file_import_guide_title),
                isDescendantOfA(withId(net.gini.android.capture.R.id.gc_help_items))
            )
        )
            .check(matches(withText(net.gini.android.capture.R.string.gc_help_item_file_import_guide_title)))
        return this
    }

    fun clickBackButton(): HelpScreen {
        onView(withContentDescription(net.gini.android.capture.R.string.gc_back_button_description)).perform(
            click()
        )
        return this
    }
}
package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf

class FileImportErrorDialog {

    /**
     * Whether the error whose title is [resourceId] is showing.
     *
     * Locale-safe overload of [checkContentIsDisplayed]: it matches the resolved string
     * resource only, with no English literal to keep in step with the translations.
     */
    fun checkTitleIsDisplayed(resourceId: Int): Boolean {
        var isDisplayed = false
        onView(withText(resourceId)).check { view, _ ->
            if (view.isShown()) {
                isDisplayed = true
            }
        }
        return isDisplayed
    }

    fun checkContentIsDisplayed(resourceId: Int, content: String): Boolean {
        var isContentPanelDisplayed = false
        onView(
            allOf(
                withText(resourceId),
                withText(content)
            )
        )
            .check { view, _ ->
                if (view.isShown()) {
                    isContentPanelDisplayed = true
                }
            }
        return isContentPanelDisplayed
    }
}
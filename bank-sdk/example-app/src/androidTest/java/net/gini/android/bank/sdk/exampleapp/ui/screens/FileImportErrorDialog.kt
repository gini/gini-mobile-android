package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf

class FileImportErrorDialog {
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
package net.gini.android.health.sdk.exampleapp.test.pages

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import net.gini.android.health.sdk.exampleapp.R

/**
 * Page Object Model for MainActivity.
 * Contains all UI interactions and assertions for the main screen.
 */
class MainScreen {

    /**
     * Taps the "Invoices list (Material 3 Theme)" button and waits for InvoicesActivity to open.
     */
    fun tapInvoicesListButton(): MainScreen {
        onView(withId(R.id.invoices_screen))
            .perform(scrollTo(), click())
        Thread.sleep(2000)
        return this
    }
}

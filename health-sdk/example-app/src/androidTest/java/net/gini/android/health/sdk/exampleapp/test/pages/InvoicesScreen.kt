package net.gini.android.health.sdk.exampleapp.test.pages

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import net.gini.android.health.sdk.exampleapp.R
import org.hamcrest.Matcher

/**
 * Page Object Model for InvoicesActivity.
 * Contains all UI interactions and assertions for the invoices list screen.
 */
class InvoicesScreen {

    /**
     * Opens the overflow menu, taps "Upload Invoices" and waits for
     * the invoices list to fully populate.
     */
    fun loadInvoices(): InvoicesScreen {
        // Open the overflow (ellipse) menu in the toolbar
        openActionBarOverflowOrOptionsMenu(
            InstrumentationRegistry.getInstrumentation().targetContext
        )

        // Tap "Upload Invoices" from the overflow menu
        onView(withText("Upload Invoices"))
            .perform(click())

        // Wait for invoices to be uploaded and the list to populate
        Thread.sleep(10000)

        // Verify the RecyclerView is displayed with at least one invoice item
        onView(withId(R.id.invoices_list))
            .check(matches(isDisplayed()))
            .check(matches(hasMinimumChildCount(1)))

        return this
    }

    /**
     * Scrolls through the invoices list to find the item matching [recipientName]
     * and taps its pay button.
     */
    fun tapInvoiceByRecipient(recipientName: String): InvoicesScreen {
        onView(withId(R.id.invoices_list)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isDisplayed()
            override fun getDescription() = "scroll to and click pay_invoice_button for recipient: $recipientName"
            override fun perform(uiController: UiController, view: View) {
                val recyclerView = view as RecyclerView
                val adapter = recyclerView.adapter
                    ?: throw PerformException.Builder()
                        .withActionDescription(description)
                        .withViewDescription("RecyclerView has no adapter")
                        .build()

                // Find the adapter position of the item matching recipientName
                var targetPosition = -1
                for (pos in 0 until adapter.itemCount) {
                    recyclerView.post {
                        (recyclerView.layoutManager as? LinearLayoutManager)
                            ?.scrollToPositionWithOffset(pos, 0)
                    }
                    uiController.loopMainThreadUntilIdle()
                    Thread.sleep(100)
                    uiController.loopMainThreadUntilIdle()

                    val vh = recyclerView.findViewHolderForAdapterPosition(pos) ?: continue
                    val recipientView = vh.itemView
                        .findViewById<android.widget.TextView>(R.id.recipient)
                    if (recipientView?.text?.toString() == recipientName) {
                        targetPosition = pos
                        break
                    }
                }

                if (targetPosition == -1) {
                    throw PerformException.Builder()
                        .withActionDescription(description)
                        .withViewDescription(
                            "No item with recipient '$recipientName' " +
                            "found in adapter (${adapter.itemCount} items)"
                        )
                        .build()
                }

                // Scroll to the found position and wait for it to settle
                recyclerView.post {
                    (recyclerView.layoutManager as? LinearLayoutManager)
                        ?.scrollToPositionWithOffset(targetPosition, 0)
                }
                uiController.loopMainThreadUntilIdle()
                Thread.sleep(300)
                uiController.loopMainThreadUntilIdle()

                // Tap the pay button inside the matched item
                val targetVh = recyclerView.findViewHolderForAdapterPosition(targetPosition)
                    ?: throw PerformException.Builder()
                        .withActionDescription(description)
                        .withViewDescription(
                            "ViewHolder at position $targetPosition not found after scroll"
                        )
                        .build()

                val payBtn = targetVh.itemView.findViewById<View>(R.id.pay_invoice_button)
                payBtn.callOnClick()
                uiController.loopMainThreadUntilIdle()
                Thread.sleep(500)
                uiController.loopMainThreadUntilIdle()
            }
        })
        return this
    }
}

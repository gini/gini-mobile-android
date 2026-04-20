package net.gini.android.health.sdk.exampleapp.test.pages

import android.view.View
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse

/**
 * Page Object Model for the Payment Review screen.
 * Contains all UI interactions and assertions for the payment review screen.
 */
class PaymentReviewScreen {

    /**
     * Waits for the Payment Review screen to load, opens the bank selection bottom sheet,
     * selects the bank matching [bankName], then taps "Continue to overview".
     */
    fun selectBankAndContinue(bankName: String = "Bank"): PaymentReviewScreen {
        // Wait for the Payment Review screen to load
        Thread.sleep(3000)

        // Tap the bank picker container to open the bank selection bottom sheet
        onView(
            allOf(
                withId(net.gini.android.internal.payment.R.id.gps_two_rows_container),
                isDisplayed()
            )
        ).perform(click())

        // Wait for the bank selection bottom sheet to appear
        Thread.sleep(1500)

        // Choose the bank matching bankName from the list
        onView(
            allOf(
                withId(net.gini.android.internal.payment.R.id.gps_select_bank_button),
                withText(bankName),
                isDescendantOfA(withId(net.gini.android.internal.payment.R.id.gps_payment_provider_apps_list)),
                isDisplayed()
            )
        ).perform(click())

        // Wait for the selection to register and button to activate
        Thread.sleep(1000)

        // Tap "Continue to overview" button
        onView(withText("Continue to overview"))
            .perform(click())

        // Wait for the overview / payment review screen to fully render
        Thread.sleep(2000)

        return this
    }

    /**
     * Taps the "Pay" button on the payment overview screen to launch the bank app.
     * The button is identified by [net.gini.android.internal.payment.R.id.gps_pay_button].
     */
    fun tapPayButton(): PaymentReviewScreen {
        onView(
            allOf(
                withId(net.gini.android.internal.payment.R.id.payment),
                isDisplayed()
            )
        ).perform(click())

        // Wait for the bank app to launch
        Thread.sleep(3000)

        return this
    }

    /**
     * Captures all 4 extracted payment fields (Recipient, IBAN, Amount, Reference) from the
     * payment details view, prints them to logcat and asserts them against the expected values.
     *
     * @param expectedRecipient expected recipient name
     * @param expectedIban      expected IBAN string
     * @param expectedAmount    expected amount string
     * @param expectedReference expected reference/purpose string; pass empty string to skip assertion
     */
    fun verifyExtractionFields(
        expectedRecipient: String,
        expectedIban: String,
        expectedAmount: String,
        expectedReference: String
    ): PaymentReviewScreen {
        val paymentDetailsId = net.gini.android.internal.payment.R.id.gps_payment_details

        // Capture and verify Recipient
        val recipientText = Array(1) { "" }
        onView(
            allOf(
                withId(net.gini.android.internal.payment.R.id.recipient),
                isDescendantOfA(withId(paymentDetailsId)),
                isDisplayed()
            )
        )
            .perform(captureFieldText(recipientText))
            .check(matches(not(withText(emptyString()))))
        assertFalse("Recipient should not be empty", recipientText[0].isEmpty())
        println("✅ Recipient: ${recipientText[0]}")
        assertEquals("Recipient extraction mismatch", expectedRecipient, recipientText[0])

        // Capture and verify IBAN
        val ibanText = Array(1) { "" }
        onView(
            allOf(
                withId(net.gini.android.internal.payment.R.id.iban),
                isDescendantOfA(withId(paymentDetailsId)),
                isDisplayed()
            )
        )
            .perform(captureFieldText(ibanText))
            .check(matches(not(withText(emptyString()))))
        assertFalse("IBAN should not be empty", ibanText[0].isEmpty())
        println("✅ IBAN: ${ibanText[0]}")
        assertEquals("IBAN extraction mismatch", expectedIban, ibanText[0])

        // Capture and verify Amount
        val amountText = Array(1) { "" }
        onView(
            allOf(
                withId(net.gini.android.internal.payment.R.id.amount),
                isDescendantOfA(withId(paymentDetailsId)),
                isDisplayed()
            )
        )
            .perform(captureFieldText(amountText))
            .check(matches(not(withText(emptyString()))))
        assertFalse("Amount should not be empty", amountText[0].isEmpty())
        println("✅ Amount: ${amountText[0]}")
        assertEquals("Amount extraction mismatch", expectedAmount, amountText[0])

        // Capture and verify Reference (purpose)
        val purposeText = Array(1) { "" }
        onView(
            allOf(
                withId(net.gini.android.internal.payment.R.id.purpose),
                isDescendantOfA(withId(paymentDetailsId)),
                isDisplayed()
            )
        )
            .perform(captureFieldText(purposeText))
        println("✅ Reference: ${purposeText[0]}")
        if (expectedReference.isNotEmpty()) {
            assertEquals("Reference extraction mismatch", expectedReference, purposeText[0])
        }

        return this
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private fun captureFieldText(container: Array<String>): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()
        override fun getDescription() = "capture text from view"
        override fun perform(uiController: UiController, view: View) {
            container[0] = (view as? EditText)?.text?.toString()
                ?: view.contentDescription?.toString()
                ?: ""
        }
    }
}

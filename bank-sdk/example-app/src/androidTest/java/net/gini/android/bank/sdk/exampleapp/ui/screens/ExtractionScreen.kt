package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import net.gini.android.bank.sdk.exampleapp.R
import org.hamcrest.Matchers.allOf

class ExtractionScreen {

    fun clickTransferSummaryButton(): ExtractionScreen {
        onView(withId(R.id.transfer_summary)).perform(click())
        return this
    }

    fun editIbanField() {
        onView(allOf(withId(R.id.text_value), withHint("iban")))
            .perform(click())
            .perform(replaceText("DE48120400000180115890"))
    }

    fun editAmountField() {
        onView(allOf(withId(R.id.text_value), withHint("amountToPay")))
            .perform(click())
            .perform(replaceText("200:EUR"))
    }

    fun editPurposeField() {
        onView(allOf(withId(R.id.text_value), withHint("paymentPurpose")))
            .perform(click())
            .perform(replaceText("Rent"))
    }

    fun editReceiptField() {
        onView(allOf(withId(R.id.text_value), withHint("paymentRecipient")))
            .perform(click())
            .perform(replaceText("Zalando Gmbh & Co. KG"))
    }
}
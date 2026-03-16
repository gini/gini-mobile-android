package net.gini.android.health.sdk.exampleapp.test.uitest

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.gini.android.health.sdk.exampleapp.MainActivity
import net.gini.android.health.sdk.exampleapp.test.pages.BankScreen
import net.gini.android.health.sdk.exampleapp.test.pages.InvoicesScreen
import net.gini.android.health.sdk.exampleapp.test.pages.MainScreen
import net.gini.android.health.sdk.exampleapp.test.pages.PaymentReviewScreen
import net.gini.android.health.sdk.exampleapp.test.testdata.TestData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SmokeTestHealth {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testInvoiceFromInvoiceList() {
        // Step 1 & 2: Tap "Invoices list (Material 3 Theme)" and wait for InvoicesActivity to open
        MainScreen().tapInvoicesListButton()

        // Step 3-6: Open overflow menu, upload invoices and wait for the list to populate
        InvoicesScreen().loadInvoices()

        // Step 7: Scroll to the invoice matching the recipient name and tap its pay button
        InvoicesScreen().tapInvoiceByRecipient(TestData.PvsRheinRuhrInvoice.RECIPIENT)

        // Step 8-18: Select bank, continue to overview and verify all 4 extracted payment fields
        PaymentReviewScreen()
            .selectBankAndContinue(bankName = TestData.BankNames.BANK)
            .verifyExtractionFields(
                expectedRecipient = TestData.PvsRheinRuhrInvoice.RECIPIENT,
                expectedIban      = TestData.PvsRheinRuhrInvoice.IBAN,
                expectedAmount    = TestData.PvsRheinRuhrInvoice.AMOUNT,
                expectedReference = TestData.PvsRheinRuhrInvoice.REFERENCE
            )
            // Step 19: Tap the Pay button — Health SDK fires the payment intent to the Bank app
            .tapPayButton()

        // Step 20: Switch context from Health app → Bank app via launchIntentForPackage
        // Step 21-23: UiAutomator verifies fields, taps Pay, waits for deep-link return
        BankScreen()
            .switchToBankApp()
            .verifyPaymentDetails(
                expectedRecipient = TestData.PvsRheinRuhrInvoice.RECIPIENT,
                expectedIban      = TestData.PvsRheinRuhrInvoice.IBAN,
                expectedAmount    = TestData.PvsRheinRuhrInvoice.AMOUNT1,
                expectedPurpose   = TestData.PvsRheinRuhrInvoice.REFERENCE
            )
            .tapPayAndReturnToBusiness()
    }
}

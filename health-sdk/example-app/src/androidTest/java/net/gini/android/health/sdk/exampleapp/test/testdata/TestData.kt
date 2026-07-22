package net.gini.android.health.sdk.exampleapp.test.testdata

/**
 * Central store for all test data used across UI tests.
 *
 * POM best practice:
 *  - Page classes  → UI interactions & assertions only
 *  - Test classes  → test flow orchestration only
 *  - TestData      → all constants, input values & expected extraction results
 */
object TestData {

    /**
     * Invoice data for the PVS RHEIN-RUHR GMBH invoice.
     * Used in [SmokeTestHealth.testInvoiceFromInvoiceList].
     */
    object PvsRheinRuhrInvoice {
        /** Recipient name shown in the invoices list — used to locate and tap the correct item. */
        const val RECIPIENT  = "PVS RHEIN-RUHR GMBH"

        /** Expected extracted IBAN on the payment review screen. */
        const val IBAN       = "DE62300700100056910300"

        /** Expected extracted amount on the payment review screen. */
        const val AMOUNT     = "18,23"
        const val AMOUNT1    = "18.23"

        /** Expected extracted reference/purpose on the payment review screen. */
        const val REFERENCE  = "Rechnungsdatum 01.08.2013"
    }

    /**
     * Bank names available in the bank selection bottom sheet.
     * Add new bank entries here as more banks are supported.
     */
    object BankNames {
        const val BANK = "Bank"
    }

    /**
     * Package identifiers for apps used during end-to-end testing.
     */
    object AppPackages {
        /** Package name of the health-sdk example app (the app under test). */
        const val HEALTH_SDK_EXAMPLE_APP = "net.gini.android.health.sdk.exampleapp"

        /** Package name of the bank-sdk example app (devPaymentProvider3Debug variant). */
        const val BANK_SDK_EXAMPLE_APP = "net.gini.android.bank.insurance.mock"
    }
}


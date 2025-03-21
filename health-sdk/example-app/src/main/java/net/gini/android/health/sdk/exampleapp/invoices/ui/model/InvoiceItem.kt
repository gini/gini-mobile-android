package net.gini.android.health.sdk.exampleapp.invoices.ui.model

import net.gini.android.health.sdk.exampleapp.invoices.data.model.DocumentWithExtractions
import net.gini.android.health.sdk.exampleapp.util.parseAmount

private val PRICE_STRING_REGEX = "^-?[0-9]+([.,])[0-9]+\$".toRegex()

data class InvoiceItem(
    val documentId: String,
    val recipient: String?,
    val amount: String?,
    val dueDate: String?,
    val isPayable: Boolean = false,
    val medicalProvider: String?
) {

    companion object {
        fun fromInvoice(documentWithExtractions: DocumentWithExtractions): InvoiceItem {
            return InvoiceItem(
                documentWithExtractions.documentId,
                documentWithExtractions.recipient,
                documentWithExtractions.amount.parseAmount(shouldThrowErrorForFormat = true),
                documentWithExtractions.dueDate,
                documentWithExtractions.isPayable,
                documentWithExtractions.medicalProvider
            )
        }
    }
}
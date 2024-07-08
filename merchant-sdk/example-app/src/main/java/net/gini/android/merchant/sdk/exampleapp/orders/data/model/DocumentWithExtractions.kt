package net.gini.android.merchant.sdk.exampleapp.orders.data.model

import com.squareup.moshi.JsonClass
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer

@JsonClass(generateAdapter = true)
data class DocumentWithExtractions(
    val documentId: String,
    var recipient: String?,
    var amount: String?,
    val dueDate: String?,
    val isPayable: Boolean = false
) {

    fun shouldUpdate(amount: String?, recipient: String?) =
        !this.amount.equals(amount, true) ||
                !this.recipient.equals(recipient, true)

    companion object {
        fun fromDocumentAndExtractions(
            document: Document,
            extractionsContainer: ExtractionsContainer,
            isPayable: Boolean
        ): DocumentWithExtractions {
            return DocumentWithExtractions(
                document.id,
                extractionsContainer.compoundExtractions["payment"]?.specificExtractionMaps?.get(0)
                    ?.get("payment_recipient")?.value,
                extractionsContainer.compoundExtractions["payment"]?.specificExtractionMaps?.get(0)
                    ?.get("amount_to_pay")?.value,
                extractionsContainer.specificExtractions["payment_due_date"]?.value,
                isPayable
            )
        }
    }
}

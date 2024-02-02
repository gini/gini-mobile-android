package net.gini.android.health.sdk.exampleapp.invoices.data.model

import com.squareup.moshi.JsonClass
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer

@JsonClass(generateAdapter = true)
data class DocumentWithExtractions(
    val documentId: String,
    val recipient: String?,
    val amount: String?,
    val dueDate: String?
) {
    
    companion object {
        fun fromDocumentAndExtractions(
            document: Document,
            extractionsContainer: ExtractionsContainer
        ): DocumentWithExtractions {
            return DocumentWithExtractions(
                document.id,
                extractionsContainer.compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.get("payment_recipient")?.value,
                extractionsContainer.compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.get("amount_to_pay")?.value,
                extractionsContainer.specificExtractions["payment_due_date"]?.value
            )
        }
    }
}

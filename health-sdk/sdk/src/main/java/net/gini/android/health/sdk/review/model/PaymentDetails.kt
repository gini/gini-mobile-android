package net.gini.android.health.sdk.review.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.health.sdk.review.error.NoPaymentDataExtracted
import net.gini.android.internal.payment.utils.extensions.sanitizeAmount
import net.gini.android.internal.payment.utils.extensions.toBackendFormat

/**
 * Represents the payment details of an invoice as extracted from a document.
 * The extractions.getSpecificExtractions contains additional values extracted alongside the four main values (ex: medical_service_provider)
 */
@Parcelize
data class PaymentDetails(
    val recipient: String,
    val iban: String,
    val amount: String,
    val purpose: String,
    val extractions: ExtractionsContainer? = null
): Parcelable

internal fun ExtractionsContainer.toPaymentDetails(): PaymentDetails {
    if (!compoundExtractions.containsKey("payment")) {
        throw NoPaymentDataExtracted()
    }
    return PaymentDetails(
        recipient = compoundExtractions.getPaymentExtraction("payment_recipient")?.value
            ?: "",
        iban = compoundExtractions.getPaymentExtraction("iban")?.value ?: "",
        amount = compoundExtractions.getPaymentExtraction("amount_to_pay")?.value?.toAmount()
            ?: "",
        purpose = compoundExtractions.getPaymentExtraction("payment_purpose")?.value ?: "",
        extractions = this
    )
}

internal fun String.toAmount(): String {
    val delimiterIndex = this.indexOf(":")
    return if (delimiterIndex != -1) {
        this.substring(0, delimiterIndex)
    } else {
        this
    }
}

/**
 * Updates specific extractions with feedback from payment details.
 *
 * This function creates a new map of specific extractions with updated values from the provided
 * [PaymentDetails]. The backend (as of BAC-1771) automatically maps feedback sent via specific
 * extractions to both specific and compound extractions, eliminating the need for duplicate data.
 *
 * The function preserves existing extraction metadata (box, entity, candidates) while updating
 * the values with user-modified payment information.
 *
 * @param paymentDetails The payment details containing updated values to send as feedback
 * @return A map of specific extractions with updated values for the four main payment fields:
 *         payment_recipient, iban, amount_to_pay, and payment_purpose
 *
 * @see PaymentDetails
 * @see SpecificExtraction
 */
internal fun MutableMap<String, SpecificExtraction>.withFeedback(paymentDetails: PaymentDetails): Map<String, SpecificExtraction> {
    return this.toMutableMap().apply {
        this["payment_recipient"] = this["payment_recipient"].let { extraction ->
            SpecificExtraction(
                extraction?.name ?: "payment_recipient",
                paymentDetails.recipient,
                extraction?.entity ?: "companyname",
                extraction?.box,
                extraction?.candidate ?: emptyList()
            )
        }
        this["iban"] = this["iban"].let { extraction ->
            SpecificExtraction(
                extraction?.name ?: "iban",
                paymentDetails.iban,
                extraction?.entity ?: "iban",
                extraction?.box,
                extraction?.candidate ?: emptyList()
            )
        }
        this["amount_to_pay"] = this["amount_to_pay"].let { extraction ->
            SpecificExtraction(
                extraction?.name ?: "amount_to_pay",
                "${paymentDetails.amount.sanitizeAmount().toBackendFormat()}:EUR",
                extraction?.entity ?: "amount",
                extraction?.box,
                extraction?.candidate ?: emptyList()
            )
        }
        this["payment_purpose"] = this["payment_purpose"].let { extraction ->
            SpecificExtraction(
                extraction?.name ?: "payment_purpose",
                paymentDetails.purpose,
                extraction?.entity ?: "text",
                extraction?.box,
                extraction?.candidate ?: emptyList()
            )
        }
    }
}

internal fun MutableMap<String, CompoundExtraction>.getPaymentExtraction(name: String) = this["payment"]?.specificExtractionMaps?.get(0)?.get(name)

internal fun PaymentDetails.toCommonPaymentDetails() = net.gini.android.internal.payment.api.model.PaymentDetails(
    recipient = this.recipient,
    iban = this.iban,
    amount = this.amount.sanitizeAmount(),
    purpose = this.purpose
)
package net.gini.android.health.sdk.review.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.health.sdk.review.error.NoPaymentDataExtracted
import net.gini.android.health.sdk.util.toBackendFormat

@Parcelize
data class PaymentDetails(
    val recipient: String,
    val iban: String,
    val amount: String,
    val purpose: String,
    internal val extractions: ExtractionsContainer? = null
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
 * Checks if the document is payable which looks for iban extraction.
 */
val PaymentDetails.isPayable get() = iban.isNotEmpty()

internal fun MutableMap<String, CompoundExtraction>.withFeedback(paymentDetails: PaymentDetails): Map<String, CompoundExtraction> {
    this["payment"] = this["payment"].let { payment ->
        CompoundExtraction(
            payment?.name ?: "payment",
            payment?.specificExtractionMaps?.mapIndexed { index, specificExtractions ->
                if (index > 0) return@mapIndexed specificExtractions

                mutableMapOf<String, SpecificExtraction>().also { extractions ->
                    extractions.putAll(specificExtractions)
                    extractions["payment_recipient"] = extractions["payment_recipient"].let { extraction ->
                        SpecificExtraction(
                            extraction?.name ?: "payment_recipient",
                            paymentDetails.recipient,
                            extraction?.entity ?: "",
                            extraction?.box,
                            extraction?.candidate ?: emptyList()
                        )
                    }
                    extractions["iban"] = extractions["iban"].let { extraction ->
                        SpecificExtraction(
                            extraction?.name ?: "iban",
                            paymentDetails.iban,
                            extraction?.entity ?: "",
                            extraction?.box,
                            extraction?.candidate ?: emptyList()
                        )
                    }
                    extractions["amount_to_pay"] = extractions["amount_to_pay"].let { extraction ->
                        SpecificExtraction(
                            extraction?.name ?: "amount_to_pay",
                            "${paymentDetails.amount.toBackendFormat()}:EUR",
                            extraction?.entity ?: "",
                            extraction?.box,
                            extraction?.candidate ?: emptyList()
                        )
                    }
                    extractions["payment_purpose"] = extractions["payment_purpose"].let { extraction ->
                        SpecificExtraction(
                            extraction?.name ?: "payment_purpose",
                            paymentDetails.purpose,
                            extraction?.entity ?: "",
                            extraction?.box,
                            extraction?.candidate ?: emptyList()
                        )
                    }
                }
            } ?: listOf()
        )
    }
    return this
}

internal fun MutableMap<String, CompoundExtraction>.getPaymentExtraction(name: String) = this["payment"]?.specificExtractionMaps?.get(0)?.get(name)
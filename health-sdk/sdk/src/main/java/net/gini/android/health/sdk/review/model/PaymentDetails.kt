package net.gini.android.health.sdk.review.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction

@Parcelize
data class PaymentDetails(
    val recipient: String,
    val iban: String,
    val amount: String,
    val purpose: String,
    internal val extractions: ExtractionsContainer? = null
): Parcelable

internal fun ExtractionsContainer.toPaymentDetails() = PaymentDetails(
    recipient = compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.get("payment_recipient")?.value ?: "",
    iban = compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.get("iban")?.value ?: "",
    amount = compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.get("amount_to_pay")?.value?.toAmount() ?: "",
    purpose = compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.get("payment_purpose")?.value ?: "",
    extractions = this
)

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

internal fun MutableMap<String, SpecificExtraction>.withFeedback(paymentDetails: PaymentDetails): Map<String, SpecificExtraction> {
    this["paymentRecipient"] = this["paymentRecipient"].let { extraction ->
        SpecificExtraction(
            extraction?.name ?: "paymentRecipient",
            paymentDetails.recipient,
            extraction?.entity,
            extraction?.box,
            extraction?.candidate
        )
    }
    this["iban"] = this["iban"].let { extraction ->
        SpecificExtraction(
            extraction?.name ?: "iban",
            paymentDetails.iban,
            extraction?.entity,
            extraction?.box,
            extraction?.candidate
        )
    }
    this["amountToPay"] = this["amountToPay"].let { extraction ->
        SpecificExtraction(
            extraction?.name ?: "amountToPay",
            paymentDetails.amount,
            extraction?.entity,
            extraction?.box,
            extraction?.candidate
        )
    }
    this["paymentPurpose"] = this["paymentPurpose"].let { extraction ->
        SpecificExtraction(
            extraction?.name ?: "paymentPurpose",
            paymentDetails.purpose,
            extraction?.entity,
            extraction?.box,
            extraction?.candidate
        )
    }
    return this
}
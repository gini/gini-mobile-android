package net.gini.android.core.api.models

import java.util.*
import net.gini.android.core.api.response.PaymentRequestResponse

data class PaymentRequest(
    val paymentProviderId: String,
    val requesterUri: String?,
    val recipient: String,
    val iban: String,
    val bic: String?,
    val amount: String,
    val purpose: String,
    val status: Status,
) {
    enum class Status {
        OPEN, PAID, INVALID
    }
}

fun PaymentRequestResponse.toPaymentRequest() = PaymentRequest(
    paymentProviderId = paymentProvider,
    requesterUri = requesterUri,
    recipient = recipient,
    iban = iban,
    bic = bic,
    amount = amount,
    purpose = purpose,
    status = when (status.lowercase(Locale.ENGLISH)) {
        "open" -> PaymentRequest.Status.OPEN
        "paid" -> PaymentRequest.Status.PAID
        else -> PaymentRequest.Status.INVALID
    }
)
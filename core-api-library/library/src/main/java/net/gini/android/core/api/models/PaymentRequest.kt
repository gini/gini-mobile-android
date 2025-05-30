package net.gini.android.core.api.models

import net.gini.android.core.api.response.PaymentRequestResponse
import java.util.Locale

/**
 * Holds information about a payment request.
 */
data class PaymentRequest(
    val paymentProviderId: String?,
    val requesterUri: String?,
    val recipient: String,
    val iban: String,
    val bic: String?,
    val amount: String,
    val purpose: String,
    val status: Status,
    val createdAt: String?,
    val expirationDate: String?
) {
    enum class Status {
        OPEN, PAID, PAID_ADJUSTED
    }
}

internal fun PaymentRequestResponse.toPaymentRequest() = PaymentRequest(
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
        else -> PaymentRequest.Status.PAID_ADJUSTED
    },
    createdAt = createdAt,
    expirationDate = expirationDate
)
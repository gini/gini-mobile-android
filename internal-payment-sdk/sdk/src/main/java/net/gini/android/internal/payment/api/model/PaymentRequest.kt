package net.gini.android.internal.payment.api.model

/**
 * Holds information about a payment request.
 */
data class PaymentRequest(
    val id: String,
    val paymentProviderId: String?,
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

fun net.gini.android.core.api.models.PaymentRequest.toPaymentRequest(
    paymentRequestId: String,
) = PaymentRequest(
    id = paymentRequestId,
    paymentProviderId = paymentProviderId,
    requesterUri = requesterUri,
    recipient = recipient,
    iban = iban,
    bic = bic,
    amount = amount,
    purpose = purpose,
    status = when (status) {
        net.gini.android.core.api.models.PaymentRequest.Status.OPEN -> PaymentRequest.Status.OPEN
        net.gini.android.core.api.models.PaymentRequest.Status.PAID -> PaymentRequest.Status.PAID
        net.gini.android.core.api.models.PaymentRequest.Status.INVALID -> PaymentRequest.Status.INVALID
    }
)
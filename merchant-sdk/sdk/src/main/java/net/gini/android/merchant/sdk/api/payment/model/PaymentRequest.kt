package net.gini.android.merchant.sdk.api.payment.model

import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp

///**
// * A payment request used for starting the bank app. Only the id is sent, but it is associated with a bank.
// */
//data class PaymentRequest(
//    val id: String,
//    val bankApp: PaymentProviderApp,
//)

/**
 * Holds information about a payment request.
 */
data class PaymentRequest(
    val id: String,
    // TODO EC-62: Move paymentProviderApp into an internal data class when we remove the openBankState flow
    internal val paymentProviderApp: PaymentProviderApp?,
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

internal fun net.gini.android.core.api.models.PaymentRequest.toPaymentRequest(
    paymentRequestId: String,
    paymentProviderApp: PaymentProviderApp? = null
) = PaymentRequest(
    id = paymentRequestId,
    paymentProviderApp = paymentProviderApp,
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
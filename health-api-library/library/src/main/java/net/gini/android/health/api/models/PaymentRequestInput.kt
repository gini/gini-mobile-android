package net.gini.android.health.api.models

import net.gini.android.health.api.requests.PaymentRequestBody

/**
 * Holds input information required for creating a payment request.
 */
data class PaymentRequestInput(
    val paymentProvider: String,
    val recipient: String,
    val iban: String,
    val amount: String,
    val purpose: String,
    val bic: String? = null,
    val sourceDocumentLocation: String? = null,
)

internal fun PaymentRequestInput.toPaymentRequestBody() = PaymentRequestBody(
    sourceDocumentLocation, paymentProvider, recipient, iban, amount, purpose, bic
)
package net.gini.android.merchant.sdk.api.payment.model

/**
 * Holds information about a payment.
 */
data class Payment(
    val paidAt: String,
    val recipient: String,
    val iban: String,
    val amount: String,
    val purpose: String,
    val bic: String? = null,
)

internal fun net.gini.android.core.api.models.Payment.toPayment() = Payment(
    paidAt, recipient, iban, amount, purpose, bic
)
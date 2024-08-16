package net.gini.android.merchant.sdk.api.payment.model

/**
 * Holds information about a payment.
 */
internal data class Payment(
    val paidAt: String,
    val recipient: String,
    val iban: String,
    val amount: String,
    val purpose: String,
    val bic: String? = null,
)
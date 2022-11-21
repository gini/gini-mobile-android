package net.gini.android.bank.api.models

/**
 * Input data required for resolving a payment.
 */
data class ResolvePaymentInput(
    val recipient: String,
    val iban: String,
    val amount: String,
    val purpose: String,
    val bic: String? = null,
)
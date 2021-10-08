package net.gini.android.core.api.models

data class ResolvePaymentInput(
    val recipient: String,
    val iban: String,
    val amount: String,
    val purpose: String,
    val bic: String? = null,
)
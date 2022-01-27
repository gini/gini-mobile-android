package net.gini.android.bank.api.models

data class ResolvePaymentInput(
    val recipient: String,
    val iban: String,
    val amount: String,
    val purpose: String,
    val bic: String? = null,
)
package net.gini.android.models

data class PaymentRequestInput(
    val paymentProvider: String,
    val recipient: String,
    val iban: String,
    val amount: String,
    val purpose: String,
    val bic: String? = null,
    val sourceDocumentLocation: String? = null,
)

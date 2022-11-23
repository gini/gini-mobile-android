package net.gini.android.bank.api.models

import net.gini.android.bank.api.response.ResolvePaymentResponse
import java.util.*

/**
 * Holds information about a resolved payment.
 */
data class ResolvedPayment(
    val requesterUri: String,
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

internal fun ResolvePaymentResponse.toResolvedPayment() = ResolvedPayment(
    requesterUri = requesterUri,
    recipient = recipient,
    iban = iban,
    bic = bic,
    amount = amount,
    purpose = purpose,
    status = when (status.lowercase(Locale.ENGLISH)) {
        "open" -> ResolvedPayment.Status.OPEN
        "paid" -> ResolvedPayment.Status.PAID
        else -> ResolvedPayment.Status.INVALID
    }
)
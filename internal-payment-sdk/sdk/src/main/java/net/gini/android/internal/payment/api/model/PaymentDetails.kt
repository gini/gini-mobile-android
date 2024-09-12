package net.gini.android.internal.payment.api.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the payment details of an order.
 */
@Parcelize
data class PaymentDetails(
    val recipient: String,
    val iban: String,
    val amount: String,
    val purpose: String,
): Parcelable

internal fun PaymentDetails.overwriteEmptyFields(value: PaymentDetails): PaymentDetails = this.copy(
    recipient = if (recipient.trim().isEmpty()) value.recipient else recipient,
    iban = if (iban.trim().isEmpty()) value.iban else iban,
    amount = if (amount.trim().isEmpty()) value.amount else amount,
    purpose = if (purpose.trim().isEmpty()) value.purpose else purpose,
)
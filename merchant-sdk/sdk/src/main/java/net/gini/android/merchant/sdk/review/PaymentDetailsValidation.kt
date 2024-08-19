package net.gini.android.merchant.sdk.review

import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.util.isNumber
import net.gini.android.merchant.sdk.util.isValidIban


internal enum class PaymentField { Recipient, Iban, Amount, Purpose }

internal sealed class ValidationMessage(val field: PaymentField) {
    data class Empty(val paymentField: PaymentField): ValidationMessage(paymentField)
    object InvalidIban: ValidationMessage(PaymentField.Iban)
    object AmountFormat: ValidationMessage(PaymentField.Amount)
}

internal fun PaymentDetails.validate(): List<ValidationMessage> = mutableListOf<ValidationMessage>().apply {
    addAll(validateRecipient(this@validate.recipient))
    addAll(validateIban(this@validate.iban))
    addAll(validateAmount(this@validate.amount))
    addAll(validatePurpose(this@validate.purpose))
}

internal fun validateRecipient(recipient: String): List<ValidationMessage> = mutableListOf<ValidationMessage>().apply {
    if (recipient.trim().isEmpty()) add(ValidationMessage.Empty(PaymentField.Recipient))
}

internal fun validateIban(iban: String): List<ValidationMessage> = mutableListOf<ValidationMessage>().apply {
    if (iban.trim().isEmpty()) add(ValidationMessage.Empty(PaymentField.Iban))
    if (!isValidIban(iban.trim())) add(ValidationMessage.InvalidIban)
}

internal fun validateAmount(amount: String): List<ValidationMessage> = mutableListOf<ValidationMessage>().apply {
    if (amount.trim().isEmpty()) add(ValidationMessage.Empty(PaymentField.Amount))
    if (!amount.trim().isNumber()) add(ValidationMessage.AmountFormat)
}

internal fun validatePurpose(purpose: String): List<ValidationMessage> = mutableListOf<ValidationMessage>().apply {
    if (purpose.trim().isEmpty()) add(ValidationMessage.Empty(PaymentField.Purpose))
}

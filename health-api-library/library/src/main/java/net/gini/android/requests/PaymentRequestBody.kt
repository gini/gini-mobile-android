package net.gini.android.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.gini.android.models.PaymentRequestInput

@JsonClass(generateAdapter = true)
internal data class PaymentRequestBody(
    @Json(name = "sourceDocumentLocation") val sourceDocumentLocation: String? = null,
    @Json(name = "paymentProvider") val paymentProvider: String,
    @Json(name = "recipient") val recipient: String,
    @Json(name = "iban") val iban: String,
    @Json(name = "amount") val amount: String,
    @Json(name = "purpose") val purpose: String,
    @Json(name = "bic") val bic: String? = null,
)

internal fun PaymentRequestInput.toPaymentRequestBody() = PaymentRequestBody(
    sourceDocumentLocation, paymentProvider, recipient, iban, amount, purpose, bic
)

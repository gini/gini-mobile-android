package net.gini.android.bank.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.gini.android.bank.api.models.Configuration

@JsonClass(generateAdapter = true)
data class ConfigurationResponse(
    @Json(name = "clientID") val clientID: String?,
    @Json(name = "userJourneyAnalyticsEnabled") val userJourneyAnalyticsEnabled: Boolean?,
    @Json(name = "skontoEnabled") val skontoEnabled: Boolean?,
    @Json(name = "returnAssistantEnabled") val returnAssistantEnabled: Boolean?,
    @Json(name = "amplitudeApiKey") val amplitudeApiKey: String?,
    @Json(name = "transactionDocsEnabled") val transactionDocsEnabled: Boolean?,
    @Json(name = "qrCodeEducationEnabled") val qrCodeEducationEnabled: Boolean?,
    @Json(name = "instantPaymentEnabled") val instantPaymentEnabled: Boolean?,
    @Json(name = "eInvoiceEnabled") val eInvoiceEnabled: Boolean?,
    @Json(name = "paymentHintsEnabled") val paymentHintsEnabled: Boolean?,
)

internal fun ConfigurationResponse.toConfiguration() = Configuration(
    clientID = clientID ?: "",
    isUserJourneyAnalyticsEnabled = userJourneyAnalyticsEnabled ?: false,
    isSkontoEnabled = skontoEnabled ?: false,
    isReturnAssistantEnabled = returnAssistantEnabled ?: false,
    amplitudeApiKey = amplitudeApiKey,
    transactionDocsEnabled = transactionDocsEnabled ?: false,
    qrCodeEducationEnabled = qrCodeEducationEnabled ?: false,
    instantPaymentEnabled = instantPaymentEnabled ?: false,
    isEInvoiceEnabled = eInvoiceEnabled ?: false,
    paymentHintsEnabled = paymentHintsEnabled ?: false
)


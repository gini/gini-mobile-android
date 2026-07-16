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
    @Json(name = "alreadyPaidHintEnabled") val alreadyPaidHintEnabled: Boolean?,
    @Json(name = "paymentDueHintEnabled") val paymentDueHintEnabled: Boolean?,
    @Json(name = "savePhotosLocallyEnabled") val savePhotosLocallyEnabled: Boolean?,
    @Json(name = "unsupportedQRCodeWarningEnabled") val unsupportedQRCodeWarningEnabled: Boolean?,
)

/**
 * Maps the configuration response to the SDK's [Configuration] model, applying the same
 * defaults for missing fields that the SDK uses.
 */
fun ConfigurationResponse.toConfiguration() = Configuration(
    clientID = clientID ?: "",
    isUserJourneyAnalyticsEnabled = userJourneyAnalyticsEnabled ?: false,
    isSkontoEnabled = skontoEnabled ?: false,
    isReturnAssistantEnabled = returnAssistantEnabled ?: false,
    amplitudeApiKey = amplitudeApiKey,
    isTransactionDocsEnabled = transactionDocsEnabled ?: false,
    isQrCodeEducationEnabled = qrCodeEducationEnabled ?: false,
    isInstantPaymentEnabled = instantPaymentEnabled ?: false,
    isEInvoiceEnabled = eInvoiceEnabled ?: false,
    isAlreadyPaidHintEnabled = alreadyPaidHintEnabled ?: false,
    isPaymentDueHintEnabled = paymentDueHintEnabled ?: false,
    isSavePhotosLocallyEnabled = savePhotosLocallyEnabled ?: false,
    isUnsupportedQRCodeWarningEnabled = unsupportedQRCodeWarningEnabled ?: false,
)


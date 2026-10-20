package net.gini.android.capture.internal.qrcode

import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Pure-function support for the Wero QR code flow. Shared between capture-sdk and bank-sdk —
 * both callers run the same mapper and fallback logic at their respective /analysis call sites,
 * so platform behaviour stays identical.
 */
internal object WeroExtractionsMapper {

    const val WERO_PAYMENT_QR_CODE_URL_KEY = "weroPaymentQRCodeUrl"
    const val PAYMENT_QR_CODE_TYPE_KEY = "paymentQRCodeType"
    const val WERO_PAYMENT_QR_CODE_TYPE_VALUE = "wero"

    private val LOG: Logger = LoggerFactory.getLogger(WeroExtractionsMapper::class.java)

    /**
     * Collapses the extraction map to a single `weroPaymentQRCodeUrl` entry when the backend
     * returned a `paymentQRCodeType=wero` marker and we have the raw scanned payload.
     * Pass-through when the flag is off, the payload is missing, or no marker is present.
     * Case-insensitive on name, entity, and value — the backend has flipped case before.
     */
    fun mapWeroExtractions(
        specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        scannedPayload: String?,
        flagEnabled: Boolean,
    ): Map<String, GiniCaptureSpecificExtraction> {
        val applies = flagEnabled &&
            !scannedPayload.isNullOrEmpty() &&
            hasWeroMarker(specificExtractions)
        return if (applies) {
            LOG.debug(
                "Wero marker detected; collapsing extractions to single {} entry",
                WERO_PAYMENT_QR_CODE_URL_KEY,
            )
            mapOf(
                WERO_PAYMENT_QR_CODE_URL_KEY to GiniCaptureSpecificExtraction(
                    WERO_PAYMENT_QR_CODE_URL_KEY,
                    scannedPayload!!,
                    WERO_PAYMENT_QR_CODE_URL_KEY,
                    null,
                    emptyList(),
                )
            )
        } else {
            LOG.debug(
                "Wero mapper pass-through (flag={}, payloadPresent={}, markerPresent={})",
                flagEnabled,
                !scannedPayload.isNullOrEmpty(),
                hasWeroMarker(specificExtractions),
            )
            specificExtractions
        }
    }

    /**
     * True when the SDK should pop back to the camera and show the unsupported-QR popup:
     * flag is on, we uploaded a Wero candidate (raw payload present) and the backend neither
     * classified it as Wero nor returned a Wero URL extraction directly.
     */
    fun shouldRouteBackToCameraForWeroFallback(
        originalSpecificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        scannedPayload: String?,
        flagEnabled: Boolean,
    ): Boolean = flagEnabled &&
        !scannedPayload.isNullOrEmpty() &&
        !hasWeroMarker(originalSpecificExtractions) &&
        !originalSpecificExtractions.containsKey(WERO_PAYMENT_QR_CODE_URL_KEY)

    /**
     * Invokes [onFallback] and returns true when the gate fires; returns false otherwise and
     * leaves the caller to proceed with the normal post-/analysis routing.
     */
    fun performWeroFallbackIfNeeded(
        originalSpecificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        scannedPayload: String?,
        flagEnabled: Boolean,
        onFallback: () -> Unit,
    ): Boolean {
        if (!shouldRouteBackToCameraForWeroFallback(
                originalSpecificExtractions,
                scannedPayload,
                flagEnabled,
            )
        ) {
            return false
        }
        LOG.debug("Wero fallback triggered; routing back to camera for unsupported-QR warning")
        onFallback()
        return true
    }

    private fun hasWeroMarker(
        specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
    ): Boolean = specificExtractions.values.any { extraction ->
        val nameMatches = extraction.name.equals(PAYMENT_QR_CODE_TYPE_KEY, ignoreCase = true)
        val entityMatches = extraction.entity.equals(PAYMENT_QR_CODE_TYPE_KEY, ignoreCase = true)
        val valueIsWero =
            extraction.value.equals(WERO_PAYMENT_QR_CODE_TYPE_VALUE, ignoreCase = true)
        (nameMatches || entityMatches) && valueIsWero
    }
}

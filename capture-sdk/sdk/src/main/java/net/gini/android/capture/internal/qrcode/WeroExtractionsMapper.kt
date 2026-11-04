package net.gini.android.capture.internal.qrcode

import net.gini.android.capture.network.model.GiniCaptureExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * Pure-function support for the Wero QR code flow. Called from capture-sdk's `/analysis`-success
 * callback in `CameraFragmentImpl` to collapse Wero extractions to a single `weroPaymentQRCodeUrl`
 * entry or to drive the fallback-to-camera decision.
 */
internal object WeroExtractionsMapper {

    const val WERO_PAYMENT_QR_CODE_URL_KEY = "weroPaymentQRCodeUrl"
    const val PAYMENT_QR_CODE_TYPE_KEY = "paymentQRCodeType"
    const val WERO_PAYMENT_QR_CODE_TYPE_VALUE = "wero"

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
        return if (flagEnabled &&
            !scannedPayload.isNullOrEmpty() &&
            hasWeroMarker(specificExtractions)
        ) {
            mapOf(
                WERO_PAYMENT_QR_CODE_URL_KEY to GiniCaptureSpecificExtraction(
                    WERO_PAYMENT_QR_CODE_URL_KEY,
                    scannedPayload,
                    WERO_PAYMENT_QR_CODE_URL_KEY,
                    null,
                    // Matches handleEPSPaymentQRCode: candidates carries the raw payload as a
                    // single GiniCaptureExtraction so Wero and EPS deliver the same shape.
                    listOf(GiniCaptureExtraction(scannedPayload, WERO_PAYMENT_QR_CODE_URL_KEY, null)),
                )
            )
        } else {
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

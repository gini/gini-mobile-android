package net.gini.android.capture

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gini.android.capture.camera.CameraActivity
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * Result returned by capture flow.
 */
sealed class CaptureSDKResult : Parcelable {
    /**
     * Extractions were found.
     */
    @Parcelize
    class Success(
        val specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        val compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
        val returnReasons: List<GiniCaptureReturnReason>,
    ) : CaptureSDKResult() {

        /**
         * The cross-border payment compound extractions returned by the backend when
         * [ProductTag.CxExtractions] is active.
         *
         * Each entry in the outer list is a row of CX fields; each inner map contains the
         * extraction name mapped to its [GiniCaptureSpecificExtraction].
         *
         * Pass the confirmed field values from this property directly to
         * [GiniCapture.sendTransferSummary] for CX payments:
         * ```kotlin
         * val fields = result.crossBorderPayment
         *     ?.firstOrNull()
         *     ?.mapValues { it.value.value }
         *     ?: emptyMap()
         * GiniCapture.sendTransferSummary(fields)
         * ```
         */
        val crossBorderPayment: List<Map<String, GiniCaptureSpecificExtraction>>?
            get() = compoundExtractions["crossBorderPayment"]?.specificExtractionMaps
    }

    /**
     * No extraction.
     */
    @Parcelize
    object Empty : CaptureSDKResult()

    /**
     * User navigated back.
     */
    @Parcelize
    object Cancel : CaptureSDKResult()

    /**
     * Capture flow returned an error.
     */
    @Parcelize
    class Error(val value: GiniCaptureError) : CaptureSDKResult()

    /**
     * User decided to enter data manually after the scanning resulted in no results or an error.
     */
    @Parcelize
    object EnterManually: CaptureSDKResult()
}

internal fun CaptureSDKResult.Success.toIntent(): Intent {
    return Intent().apply {
        this.putExtra(CameraActivity.EXTRA_OUT_EXTRACTIONS, Bundle().apply {
            specificExtractions.forEach { putParcelable(it.key, it.value) }
        })
        this.putExtra(CameraActivity.EXTRA_OUT_COMPOUND_EXTRACTIONS, Bundle().apply {
            compoundExtractions.forEach { putParcelable(it.key, it.value) }
        })
    }
}
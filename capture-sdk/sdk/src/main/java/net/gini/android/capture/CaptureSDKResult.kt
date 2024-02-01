package net.gini.android.capture

import android.content.Intent
import android.os.Bundle
import net.gini.android.capture.camera.CameraActivity
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * Result returned by capture flow.
 */
sealed class CaptureSDKResult {
    /**
     * Extractions were found.
     */
    class Success(
        val specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        val compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
        val returnReasons: List<GiniCaptureReturnReason>,
    ) : CaptureSDKResult()

    /**
     * No extraction.
     */
    object Empty : CaptureSDKResult()

    /**
     * User navigated back.
     */
    object Cancel : CaptureSDKResult()

    /**
     * Capture flow returned an error.
     */
    class Error(val value: GiniCaptureError) : CaptureSDKResult()

    /**
     * User decided to enter data manually after the scanning resulted in no results or an error.
     */
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
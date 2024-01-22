package net.gini.android.bank.sdk.capture

import android.content.Intent
import android.os.Bundle
import net.gini.android.capture.CaptureSDKResult
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.camera.CameraActivity
import net.gini.android.capture.internal.util.FileImportValidator
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * Result returned by capture flow.
 */
sealed class CaptureResult {
    /**
     * Extractions were found.
     */
    class Success(
        val specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        val compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
        val returnReasons: List<GiniCaptureReturnReason>,
    ) : CaptureResult()

    /**
     * No extraction.
     */
    object Empty : CaptureResult()

    /**
     * User navigated back.
     */
    object Cancel : CaptureResult()

    /**
     * Capture flow returned an error.
     */
    class Error(val value: ResultError) : CaptureResult()

    /**
     * User decided to enter data manually after the scanning resulted in no results or an error.
     */
    object EnterManually: CaptureResult()
}

fun CaptureSDKResult.toCaptureResult(): CaptureResult {
    return when (this) {
        is CaptureSDKResult.Success -> {
            CaptureResult.Success(
                this.specificExtractions,
                this.compoundExtractions,
                this.returnReasons
            )
        }
        is CaptureSDKResult.Empty -> {
            CaptureResult.Empty
        }
        is CaptureSDKResult.Cancel -> {
            CaptureResult.Cancel
        }
        is CaptureSDKResult.Error -> {
            CaptureResult.Error(ResultError.Capture(this.value))
        }
        is CaptureSDKResult.EnterManually -> {
            CaptureResult.EnterManually
        }
    }
}

sealed class ResultError {
    /**
     * An error which occurred during the capture flow.
     */
    data class Capture(val giniCaptureError: GiniCaptureError) : ResultError()

    /**
     * An error which occurred during importing a file shared from another app.
     */
    data class FileImport(val code: FileImportValidator.Error? = null, val message: String? = null) : ResultError()
}

internal fun CaptureResult.Success.toIntent(): Intent {
    return Intent().apply {
        this.putExtra(CameraActivity.EXTRA_OUT_EXTRACTIONS, Bundle().apply {
            specificExtractions.forEach { putParcelable(it.key, it.value) }
        })
        this.putExtra(CameraActivity.EXTRA_OUT_COMPOUND_EXTRACTIONS, Bundle().apply {
            compoundExtractions.forEach { putParcelable(it.key, it.value) }
        })
    }
}
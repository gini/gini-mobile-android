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
     *
     * [returnReasons] is deprecated: return reasons are no longer supported and the SDK never
     * populates this list, so results produced by the SDK always carry an empty list.
     */
    @Parcelize
    class Success @JvmOverloads constructor(
        val specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        val compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
        @Suppress("kotlin:S1133") // Intentional deprecation, removal is scheduled for the next major version
        @Deprecated(
            "Return reasons are no longer supported. The SDK never populates this list and it " +
                "will be removed in the next major version."
        )
        val returnReasons: List<GiniCaptureReturnReason> = emptyList(),
    ) : CaptureSDKResult()

    /**
     * The user chose to schedule the payment for the invoice's due date instead of paying now.
     *
     * Carries the same extractions as [Success] — the hosting app is expected to open its own
     * scheduled transfer flow with them.
     *
     * [returnReasons] is deprecated: return reasons are no longer supported and the SDK never
     * populates this list, so results produced by the SDK always carry an empty list.
     */
    @Parcelize
    class SchedulePayment @JvmOverloads constructor(
        val specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        val compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
        @Suppress("kotlin:S1133") // Intentional deprecation, removal is scheduled for the next major version
        @Deprecated(
            "Return reasons are no longer supported. The SDK never populates this list and it " +
                "will be removed in the next major version."
        )
        val returnReasons: List<GiniCaptureReturnReason> = emptyList(),
    ) : CaptureSDKResult()

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
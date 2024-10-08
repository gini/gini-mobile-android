package net.gini.android.bank.sdk.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.IntentCompat

/**
 * Activity Result Api custom contract for starting the capture flow.
 *
 * It doesn't take any input.
 * It returns a [CaptureResult]
 */
class CaptureFlowContract : ActivityResultContract<Unit, CaptureResult>() {
    override fun createIntent(context: Context, input: Unit) = Intent(
        context, CaptureFlowActivity::class.java
    )

    override fun parseResult(resultCode: Int, intent: Intent?): CaptureResult {
        return internalParseResult(resultCode, intent)
    }
}

/**
 * Activity Result Api custom contract for starting the capture flow for the case in
 * which the a document was shared from another app.
 *
 * The input is generated by Gini Pay Bank SDK internally when calling [GiniPayBank.startCaptureFlowForIntent]
 *
 * It returns a [CaptureResult] same as [CaptureFlowContract]
 */
class CaptureFlowImportContract : ActivityResultContract<CaptureImportInput, CaptureResult>() {
    override fun createIntent(context: Context, input: CaptureImportInput) =
        Intent(context, CaptureFlowActivity::class.java).apply {
            putExtra(CaptureFlowActivity.EXTRA_IN_CAPTURE_IMPORT_INPUT, input)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): CaptureResult {
        return internalParseResult(resultCode, intent)
    }
}

internal fun internalParseResult(resultCode: Int, result: Intent?): CaptureResult {
    return when(resultCode) {
        Activity.RESULT_CANCELED -> CaptureResult.Cancel
        else -> result?.let {
            IntentCompat.getParcelableExtra(it, CaptureFlowActivity.EXTRA_OUT_RESULT, CaptureResult::class.java)
        } ?: CaptureResult.Empty
    }
}

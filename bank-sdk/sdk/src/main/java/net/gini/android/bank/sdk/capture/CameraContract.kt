package net.gini.android.bank.sdk.capture

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import net.gini.android.capture.camera.CameraActivity

internal class CameraContract : ActivityResultContract<Unit, CaptureResult>() {
    override fun createIntent(context: Context, input: Unit) = Intent(
        context, CameraActivity::class.java
    )

    override fun parseResult(resultCode: Int, result: Intent?): CaptureResult {
        return internalParseResult(resultCode, result)
    }
}
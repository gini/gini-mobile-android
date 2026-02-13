package net.gini.android.bank.sdk.exampleapp.ui.util

import android.app.Activity
import android.widget.Toast
import net.gini.android.bank.sdk.capture.ResultError
import net.gini.android.bank.sdk.exampleapp.ui.ExtractionsActivity
import net.gini.android.capture.CaptureSDKResult
import net.gini.android.capture.GiniCaptureFragmentListener

class CaptureResultListener(val context: Activity) : GiniCaptureFragmentListener {
    override fun onFinishedWithResult(result: CaptureSDKResult) {
        when (result) {
            CaptureSDKResult.Cancel -> {
                context.finish()
            }

            CaptureSDKResult.Empty -> {
                Toast.makeText(
                    context,
                    "Empty result, no documents scanned",
                    Toast.LENGTH_SHORT
                ).show()
                context.finish()
            }

            CaptureSDKResult.EnterManually -> {
                Toast.makeText(
                    context,
                    "Scan exited for manual enter mode",
                    Toast.LENGTH_SHORT
                ).show()
                context.finish()
            }

            is CaptureSDKResult.Error -> {
                Toast.makeText(
                    context,
                    "Error: ${(result.value as ResultError.FileImport).code} ${(result.value as ResultError.FileImport).message}",
                    Toast.LENGTH_LONG
                ).show()

                context.finish()
            }

            is CaptureSDKResult.Success -> {
                context.startActivity(
                    ExtractionsActivity.getStartIntent(
                        context,
                        result.specificExtractions,
                        true
                    )
                )
                context.finish()
            }
        }
    }
}
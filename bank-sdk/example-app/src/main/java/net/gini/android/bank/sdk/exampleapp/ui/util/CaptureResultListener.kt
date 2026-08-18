package net.gini.android.bank.sdk.exampleapp.ui.util

import android.app.Activity
import android.widget.Toast
import net.gini.android.bank.sdk.exampleapp.ui.ExtractionsActivity
import net.gini.android.capture.CaptureSDKResult
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureFragmentListener
import net.gini.android.capture.ProductTag

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
                    "Error: ${result.value.errorCode} ${result.value.message}",
                    Toast.LENGTH_LONG
                ).show()

                context.finish()
            }

            is CaptureSDKResult.Success -> {
                context.startActivity(
                    ExtractionsActivity.getStartIntent(
                        context,
                        result.specificExtractions,
                        result.compoundExtractions,
                        GiniCapture.getInstance().productTag == ProductTag.CxExtractions,
                        true
                    )
                )
                context.finish()
            }

            is CaptureSDKResult.SchedulePayment -> handleSchedulePayment(result)
        }
    }

    // A real bank app would open its scheduled transfer flow here instead of paying now.
    // The example app surfaces the request via a toast and the scheduled-payment
    // indicator on ExtractionsActivity, keeping it distinguishable from a Success result.
    private fun handleSchedulePayment(result: CaptureSDKResult.SchedulePayment) {
        Toast.makeText(
            context,
            "Schedule payment requested",
            Toast.LENGTH_SHORT
        ).show()
        context.startActivity(
            ExtractionsActivity.getStartIntent(
                context,
                result.specificExtractions,
                result.compoundExtractions,
                GiniCapture.getInstance().productTag == ProductTag.CxExtractions,
                true,
                isSchedulePayment = true
            )
        )
        context.finish()
    }
}

package net.gini.android.capture

/**
 * Internal use only.
 *
 * Interface used by [CaptureFlowFragment] to dispatch Bank SDK properties to Capture SDK.
 */
interface BankSDKBridge {

    /**
     * This method is implemented in `CaptureFlowFragment` to send the Bank SDK properties to the Capture SDK.
     *
     * @param captureResult a success result from the Capture SDK
     * @return the Bank SDK properties to be used in the Capture SDK
     */
    fun getBankSDKProperties(captureResult: CaptureSDKResult.Success): BankSDKProperties
}

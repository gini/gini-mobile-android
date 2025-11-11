package net.gini.android.capture

/**
 * Internal use only.
 * <p>
 * Interface used by {@link CaptureFlowFragment} to dispatch Bank SDK properties to Capture SDK
 * </p>
 */
interface BankSDKBridge {

    /**
     * <p>
     *     This method is implemented in CaptureFlowFragment to send the Bank SDK properties to the Capture SDK.
     * </p>
     * @param captureResult a success result from the Capture SDK
     * @return BankSDKProperties returns the Bank SDK properties to be used in the Capture SDK
     */
    fun getBankSDKProperties(captureResult: CaptureSDKResult.Success): BankSDKProperties
}

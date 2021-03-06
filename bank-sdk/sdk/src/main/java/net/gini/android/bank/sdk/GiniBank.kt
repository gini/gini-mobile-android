package net.gini.android.bank.sdk

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import net.gini.android.bank.api.GiniBankAPI
import net.gini.android.capture.AsyncCallback
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.ImportedFileValidationException
import net.gini.android.capture.requirements.GiniCaptureRequirements
import net.gini.android.capture.requirements.RequirementsReport
import net.gini.android.capture.util.CancellationToken
import net.gini.android.core.api.models.PaymentRequest
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.bank.sdk.GiniBank.releaseCapture
import net.gini.android.bank.sdk.GiniBank.setCaptureConfiguration
import net.gini.android.bank.sdk.GiniBank.startCaptureFlow
import net.gini.android.bank.sdk.GiniBank.startCaptureFlowForIntent
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.bank.sdk.capture.CaptureImportInput
import net.gini.android.bank.sdk.capture.applyConfiguration
import net.gini.android.bank.sdk.capture.util.getImportFileCallback
import net.gini.android.bank.sdk.error.AmountParsingException
import net.gini.android.bank.sdk.pay.getBusinessIntent
import net.gini.android.bank.sdk.pay.getRequestId
import net.gini.android.bank.sdk.util.parseAmountToBackendFormat

/**
 * Api for interacting with Capture and Payment features.
 *
 * The Capture feature is a layer of abstraction above Gini Capture SDK and the Return Assistant feature.
 * Capture feature can be used with:
 *  - the Screen API by calling [startCaptureFlow] or [startCaptureFlowForIntent].
 *  - the Component API by building everything around the provided fragments.
 * See example apps for more details about usage of Screen and Component APIs.
 *
 * To use capture features, they need to be configured with [setCaptureConfiguration].
 * Note that configuration is immutable. [releaseCapture] needs to be called before passing a new configuration.
 *
 * To use the pay feature, first [setGiniApi] needs to be called. The flow for this feature would be:
 *  - [getRequestId] to extract the id from the [Intent]
 *  - [getPaymentRequest] to get payment details set by the business app.
 *  - [resolvePaymentRequest] to mark the [PaymentRequest] as paid.
 *  - [returnToPaymentInitiatorApp] to return to the app that started the flow.
 */
object GiniBank {

    private var giniCapture: GiniCapture? = null
    private var captureConfiguration: CaptureConfiguration? = null
    private var giniApi: GiniBankAPI? = null

    internal fun getCaptureConfiguration() = captureConfiguration

    /**
     * Shows the return reasons dialog in the return assistant, if enabled.
     */
    var enableReturnReasons = true

    /**
     * Sets configuration for Capture feature.
     * Note that configuration is immutable. [releaseCapture] needs to be called before passing a new configuration.
     *
     * @throws IllegalStateException if capture is already configured.
     */
    fun setCaptureConfiguration(captureConfiguration: CaptureConfiguration) {
        check(giniCapture == null) { "Gini Capture already configured. Call releaseCapture() before setting a new configuration." }
        GiniBank.captureConfiguration = captureConfiguration
        GiniCapture.newInstance()
            .applyConfiguration(captureConfiguration)
            .build()
        giniCapture = GiniCapture.getInstance()
    }

    /**
     *  Frees up resources used by Capture.
     */
    fun releaseCapture(context: Context) {
        GiniCapture.cleanup(context)
        captureConfiguration = null
        giniCapture = null
    }

    /**
     *  Checks hardware requirements for Capture feature.
     *  Requirements are not enforced, but are recommended to be checked before using.
     */
    fun checkCaptureRequirements(context: Context): RequirementsReport = GiniCaptureRequirements.checkRequirements(context)

    /**
     * Screen API for starting the capture flow.
     *
     * @param resultLauncher
     * @throws IllegalStateException if the capture feature was not configured.
     */
    fun startCaptureFlow(resultLauncher: ActivityResultLauncher<Unit>) {
        check(giniCapture != null) { "Capture feature is not configured. Call setCaptureConfiguration before starting the flow." }
        resultLauncher.launch(Unit)
    }

    /**
     * Screen API for starting the capture flow when a pdf or image document was shared from another app.
     *
     * @param
     *
     * @throws IllegalStateException if the capture feature was not configured.
     */
    fun startCaptureFlowForIntent(resultLauncher: ActivityResultLauncher<CaptureImportInput>, context: Context, intent: Intent): CancellationToken {
        giniCapture.let { capture ->
            check(capture != null) { "Capture feature is not configured. Call setCaptureConfiguration before starting the flow." }
            return capture.createIntentForImportedFiles(intent, context, getImportFileCallback(resultLauncher))
        }
    }

    /**
     * Component API
     *
     * Creates an [Document] for a pdf or image that was shared from another app.
     *
     * Importing the files is executed on a secondary thread as it can take several seconds for
     * the process to complete. The callback methods are invoked on the main thread.
     *
     * @param intent the Intent your app received
     * @param context Android context
     * @param callback A [AsyncCallback} implementation
     *
     * @return a {@link CancellationToken} for cancelling the import process
     */
    fun createDocumentForImportedFiles(intent: Intent, context: Context, callback: AsyncCallback<Document, ImportedFileValidationException>) {
        giniCapture.let { capture ->
            check(capture != null) { "Capture feature is not configured. Call setCaptureConfiguration before creating the document." }
            capture.createDocumentForImportedFiles(intent, context, callback)
        }
    }

    /**
     * Set the [GiniBankAPI] instance to be used for the Pay feature.
     */
    fun setGiniApi(giniApi: GiniBankAPI) {
        GiniBank.giniApi = giniApi
    }

    /**
     * Clears the reference to giniApi set by [setGiniApi].
     */
    fun releaseGiniApi() {
        giniApi = null
    }

    /**
     *  Get the payment details for the request created by a business.
     *  The id is sent in an [Intent]. Use [getRequestId] for extracting the id from the [Intent].
     *
     *  @param id The id sent by the business.
     *  @return [PaymentRequest] created by the business.
     *  @throws Throwable This method makes a network call which may fail, the resulting throwable is not caught and a type is not guaranteed.
     */
    suspend fun getPaymentRequest(id: String): PaymentRequest {
        val api = giniApi
        check(api != null) { "Gini Api is not set" }
        return api.documentManager.getPaymentRequest(id)
    }

    /**
     * Marks the a [PaymentRequest] as paid.
     *
     * **Important**: The amount string in the [ResolvePaymentInput] must be convertible to a [Double].
     * For ex. "12.39" is valid, but "12.39 ???" or "12,39" are not valid.
     *
     * @param requestId id of [PaymentRequest] to be resolved.
     * @param resolvePaymentInput the details used for the actual payment.
     * @return [ResolvedPayment] containing the payment details and the Uri used for returning to the Business app.
     * @throws Throwable This method makes a network call which may fail, the resulting throwable is not caught and a type is not guaranteed.
     * @throws AmountParsingException If the amount string could not be parsed
     */
    suspend fun resolvePaymentRequest(requestId: String, resolvePaymentInput: ResolvePaymentInput): ResolvedPayment {
        val api = giniApi
        check(api != null) { "Gini Api is not set" }
        return api.documentManager.resolvePaymentRequest(requestId, resolvePaymentInput.copy(amount = resolvePaymentInput.parseAmountToBackendFormat()))
    }

    /**
     * Starts the app that started the payment flow.
     *
     * @param context used to call startActivity.
     * @param resolvedPayment the object returned by [resolvePaymentRequest]
     */
    fun returnToPaymentInitiatorApp(context: Context, resolvedPayment: ResolvedPayment) {
        context.startActivity(resolvedPayment.getBusinessIntent())
    }
}
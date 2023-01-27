package net.gini.android.bank.sdk

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import net.gini.android.bank.api.GiniBankAPI
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.bank.sdk.GiniBank.getPaymentRequest
import net.gini.android.bank.sdk.GiniBank.releaseCapture
import net.gini.android.bank.sdk.GiniBank.resolvePaymentRequest
import net.gini.android.bank.sdk.GiniBank.returnToPaymentInitiatorApp
import net.gini.android.bank.sdk.GiniBank.setCaptureConfiguration
import net.gini.android.bank.sdk.GiniBank.setGiniApi
import net.gini.android.bank.sdk.GiniBank.startCaptureFlow
import net.gini.android.bank.sdk.GiniBank.startCaptureFlowForIntent
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.bank.sdk.capture.CaptureImportInput
import net.gini.android.bank.sdk.capture.applyConfiguration
import net.gini.android.bank.sdk.capture.digitalinvoice.view.DefaultDigitalInvoiceOnboardingNavigationBarBottomAdapter
import net.gini.android.bank.sdk.capture.digitalinvoice.view.DigitalInvoiceOnboardingIllustrationAdapter
import net.gini.android.bank.sdk.capture.digitalinvoice.view.DigitalInvoiceOnboardingNavigationBarBottomAdapter
import net.gini.android.bank.sdk.capture.digitalinvoice.view.ImageDigitalInvoiceOnboardingIllustrationAdapter
import net.gini.android.bank.sdk.capture.util.getImportFileCallback
import net.gini.android.bank.sdk.error.AmountParsingException
import net.gini.android.bank.sdk.pay.getBusinessIntent
import net.gini.android.bank.sdk.pay.getRequestId
import net.gini.android.bank.sdk.util.parseAmountToBackendFormat
import net.gini.android.capture.*
import net.gini.android.capture.requirements.GiniCaptureRequirements
import net.gini.android.capture.requirements.RequirementsReport
import net.gini.android.capture.util.CancellationToken
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.PaymentRequest

/**
 * Api for interacting with Capture and Payment features.
 *
 * The Capture feature is a layer of abstraction above Gini Capture SDK and the Return Assistant feature.
 * Capture feature can be used with:
 *  - the Screen API by calling [startCaptureFlow] or [startCaptureFlowForIntent].
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

    var digitalInvoiceOnboardingNavigationBarBottomAdapter: DigitalInvoiceOnboardingNavigationBarBottomAdapter = DefaultDigitalInvoiceOnboardingNavigationBarBottomAdapter()
    var digitalInvoiceOnboardingIllustrationAdapter: DigitalInvoiceOnboardingIllustrationAdapter = ImageDigitalInvoiceOnboardingIllustrationAdapter()
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
    fun releaseCapture(
        context: Context,
        paymentRecipient: String,
        paymentReference: String,
        paymentPurpose: String,
        iban: String,
        bic: String,
        amount: Amount
    ) {
        GiniCapture.cleanup(context, paymentRecipient, paymentReference, paymentPurpose, iban, bic, amount)
        captureConfiguration = null
        giniCapture = null
        digitalInvoiceOnboardingNavigationBarBottomAdapter = DefaultDigitalInvoiceOnboardingNavigationBarBottomAdapter()
        digitalInvoiceOnboardingIllustrationAdapter = ImageDigitalInvoiceOnboardingIllustrationAdapter()
    }

    /**
     *  Checks hardware requirements for Capture feature.
     *  Requirements are not enforced, but are recommended to be checked before using.
     */
    fun checkCaptureRequirements(context: Context): RequirementsReport =
        GiniCaptureRequirements.checkRequirements(context)

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
    fun startCaptureFlowForIntent(
        resultLauncher: ActivityResultLauncher<CaptureImportInput>,
        context: Context,
        intent: Intent
    ): CancellationToken {
        giniCapture.let { capture ->
            check(capture != null) { "Capture feature is not configured. Call setCaptureConfiguration before starting the flow." }
            if (capture.isMultiPageEnabled) {
                return capture.createIntentForImportedFiles(
                    intent,
                    context,
                    getImportFileCallback(resultLauncher)
                )
            } else {
                try {
                    val captureIntent =
                        GiniCapture.createIntentForImportedFile(intent, context, null, null)
                    resultLauncher.launch(CaptureImportInput.Forward(captureIntent))
                } catch (exception: ImportedFileValidationException) {
                    resultLauncher.launch(
                        CaptureImportInput.Error(
                            exception.validationError,
                            exception.message
                        )
                    )
                }
                return CancellationToken {}
            }
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
        return when (val paymentRequestResource = api.documentManager.getPaymentRequest(id)) {
            is Resource.Cancelled -> throw Exception("Cancelled")
            is Resource.Error -> throw Exception(
                paymentRequestResource.message,
                paymentRequestResource.exception
            )
            is Resource.Success -> paymentRequestResource.data
        }
    }

    /**
     * Marks the a [PaymentRequest] as paid.
     *
     * **Important**: The amount string in the [ResolvePaymentInput] must be convertible to a [Double].
     * For ex. "12.39" is valid, but "12.39 €" or "12,39" are not valid.
     *
     * @param requestId id of [PaymentRequest] to be resolved.
     * @param resolvePaymentInput the details used for the actual payment.
     * @return [ResolvedPayment] containing the payment details and the Uri used for returning to the Business app.
     * @throws Throwable This method makes a network call which may fail, the resulting throwable is not caught and a type is not guaranteed.
     * @throws AmountParsingException If the amount string could not be parsed
     */
    suspend fun resolvePaymentRequest(
        requestId: String,
        resolvePaymentInput: ResolvePaymentInput
    ): ResolvedPayment {
        val api = giniApi
        check(api != null) { "Gini Api is not set" }
        return when (val resolvedPaymentResource = api.documentManager.resolvePaymentRequest(
            requestId,
            resolvePaymentInput.copy(amount = resolvePaymentInput.parseAmountToBackendFormat())
        )) {
            is Resource.Cancelled -> throw Exception("Cancelled")
            is Resource.Error -> throw Exception(
                resolvedPaymentResource.message,
                resolvedPaymentResource.exception
            )
            is Resource.Success -> resolvedPaymentResource.data
        }
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
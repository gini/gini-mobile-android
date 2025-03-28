package net.gini.android.bank.sdk

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import net.gini.android.bank.api.GiniBankAPI
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.bank.sdk.GiniBank.getPaymentRequest
import net.gini.android.bank.sdk.GiniBank.resolvePaymentRequest
import net.gini.android.bank.sdk.GiniBank.returnToPaymentInitiatorApp
import net.gini.android.bank.sdk.GiniBank.setCaptureConfiguration
import net.gini.android.bank.sdk.GiniBank.setGiniApi
import net.gini.android.bank.sdk.GiniBank.startCaptureFlow
import net.gini.android.bank.sdk.GiniBank.startCaptureFlowForIntent
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.bank.sdk.capture.CaptureFlowFragment
import net.gini.android.bank.sdk.capture.CaptureImportInput
import net.gini.android.bank.sdk.capture.applyConfiguration
import net.gini.android.bank.sdk.capture.di.skonto.captureSdkDiBridge
import net.gini.android.bank.sdk.capture.digitalinvoice.help.view.DefaultDigitalInvoiceHelpNavigationBarBottomAdapter
import net.gini.android.bank.sdk.capture.digitalinvoice.help.view.DigitalInvoiceHelpNavigationBarBottomAdapter
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.DigitalInvoiceSkontoNavigationBarBottomAdapter
import net.gini.android.bank.sdk.capture.digitalinvoice.view.DefaultDigitalInvoiceNavigationBarBottomAdapter
import net.gini.android.bank.sdk.capture.digitalinvoice.view.DefaultDigitalInvoiceOnboardingNavigationBarBottomAdapter
import net.gini.android.bank.sdk.capture.digitalinvoice.view.DigitalInvoiceNavigationBarBottomAdapter
import net.gini.android.bank.sdk.capture.digitalinvoice.view.DigitalInvoiceOnboardingNavigationBarBottomAdapter
import net.gini.android.bank.sdk.capture.skonto.SkontoNavigationBarBottomAdapter
import net.gini.android.bank.sdk.capture.skonto.help.SkontoHelpNavigationBarBottomAdapter
import net.gini.android.bank.sdk.di.BankSdkIsolatedKoinContext
import net.gini.android.bank.sdk.error.AmountParsingException
import net.gini.android.bank.sdk.invoice.InvoicePreviewFragment
import net.gini.android.bank.sdk.invoice.InvoicePreviewFragmentArgs
import net.gini.android.bank.sdk.pay.getBusinessIntent
import net.gini.android.bank.sdk.pay.getRequestId
import net.gini.android.bank.sdk.transactiondocs.TransactionDocs
import net.gini.android.bank.sdk.transactiondocs.internal.GiniBankTransactionDocs
import net.gini.android.bank.sdk.transactiondocs.ui.invoice.TransactionDocInvoicePreviewFragmentArgs
import net.gini.android.bank.sdk.util.parseAmountToBackendFormat
import net.gini.android.capture.Amount
import net.gini.android.capture.AsyncCallback
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.ImportedFileValidationException
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.onboarding.view.ImageOnboardingIllustrationAdapter
import net.gini.android.capture.onboarding.view.OnboardingIllustrationAdapter
import net.gini.android.capture.requirements.GiniCaptureRequirements
import net.gini.android.capture.requirements.RequirementsReport
import net.gini.android.capture.util.CancellationToken
import net.gini.android.capture.view.InjectedViewAdapterInstance
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
    private var giniBankTerminateCallback: (() -> Unit)? = null

    internal const val USER_COMMENT_GINI_BANK_VERSION = "GiniBankVer"

    internal var giniBankTransactionDocs: GiniBankTransactionDocs? = null
        private set

    val transactionDocs: TransactionDocs
        get() = giniBankTransactionDocs
            ?: error("Transaction list not initialized. Call `initializeTransactionListFeature(...)` first.")

    /**
     * Bottom navigation bar adapters. Could be changed to custom ones.
     */
    internal var digitalInvoiceOnboardingNavigationBarBottomAdapterInstance: InjectedViewAdapterInstance<DigitalInvoiceOnboardingNavigationBarBottomAdapter> =
        InjectedViewAdapterInstance(DefaultDigitalInvoiceOnboardingNavigationBarBottomAdapter())
    var digitalInvoiceOnboardingNavigationBarBottomAdapter: DigitalInvoiceOnboardingNavigationBarBottomAdapter
        set(value) {
            digitalInvoiceOnboardingNavigationBarBottomAdapterInstance =
                InjectedViewAdapterInstance(value)
        }
        get() = digitalInvoiceOnboardingNavigationBarBottomAdapterInstance.viewAdapter

    internal var digitalInvoiceHelpNavigationBarBottomAdapterInstance: InjectedViewAdapterInstance<DigitalInvoiceHelpNavigationBarBottomAdapter> =
        InjectedViewAdapterInstance(DefaultDigitalInvoiceHelpNavigationBarBottomAdapter())
    var digitalInvoiceHelpNavigationBarBottomAdapter: DigitalInvoiceHelpNavigationBarBottomAdapter
        set(value) {
            digitalInvoiceHelpNavigationBarBottomAdapterInstance =
                InjectedViewAdapterInstance(value)
        }
        get() = digitalInvoiceHelpNavigationBarBottomAdapterInstance.viewAdapter

    internal var digitalInvoiceOnboardingIllustrationAdapterInstance: InjectedViewAdapterInstance<OnboardingIllustrationAdapter> =
        InjectedViewAdapterInstance(
            ImageOnboardingIllustrationAdapter(
                R.drawable.gbs_digital_invoice_list_image,
                R.string.gbs_digital_invoice_onboarding_text_1
            )
        )
    var digitalInvoiceOnboardingIllustrationAdapter: OnboardingIllustrationAdapter
        set(value) {
            digitalInvoiceOnboardingIllustrationAdapterInstance = InjectedViewAdapterInstance(value)
        }
        get() = digitalInvoiceOnboardingIllustrationAdapterInstance.viewAdapter

    internal var digitalInvoiceNavigationBarBottomAdapterInstance: InjectedViewAdapterInstance<DigitalInvoiceNavigationBarBottomAdapter> =
        InjectedViewAdapterInstance(DefaultDigitalInvoiceNavigationBarBottomAdapter())
    var digitalInvoiceNavigationBarBottomAdapter: DigitalInvoiceNavigationBarBottomAdapter
        set(value) {
            digitalInvoiceNavigationBarBottomAdapterInstance = InjectedViewAdapterInstance(value)
        }
        get() = digitalInvoiceNavigationBarBottomAdapterInstance.viewAdapter


    internal var skontoNavigationBarBottomAdapterInstance: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>? =
        null

    var skontoNavigationBarBottomAdapter: SkontoNavigationBarBottomAdapter?
        set(value) {
            skontoNavigationBarBottomAdapterInstance =
                value?.let { InjectedViewAdapterInstance(it) }
        }
        get() = skontoNavigationBarBottomAdapterInstance?.viewAdapter

    internal var digitalInvocieSkontoNavigationBarBottomAdapterInstance: InjectedViewAdapterInstance<DigitalInvoiceSkontoNavigationBarBottomAdapter>? =
        null

    var digitalInvoiceSkontoNavigationBarBottomAdapter: DigitalInvoiceSkontoNavigationBarBottomAdapter?
        set(value) {
            digitalInvocieSkontoNavigationBarBottomAdapterInstance =
                value?.let { InjectedViewAdapterInstance(it) }
        }
        get() = digitalInvocieSkontoNavigationBarBottomAdapterInstance?.viewAdapter

    internal var skontoHelpNavigationBarBottomAdapterInstance: InjectedViewAdapterInstance<SkontoHelpNavigationBarBottomAdapter>? =
        null

    var skontoHelpNavigationBarBottomAdapter: SkontoHelpNavigationBarBottomAdapter?
        set(value) {
            skontoHelpNavigationBarBottomAdapterInstance =
                value?.let { InjectedViewAdapterInstance(it) }
        }
        get() = skontoHelpNavigationBarBottomAdapterInstance?.viewAdapter


    internal fun getCaptureConfiguration() = captureConfiguration

    internal fun setGiniBankTerminationCallback(callback: () -> Unit) {
        giniBankTerminateCallback = callback
    }

    internal fun cleanupGiniBankTerminationCallback() {
        giniBankTerminateCallback = null
    }

    /**
     * Shows the return reasons dialog in the return assistant, if enabled.
     * Note that it is disabled by default.
     */
    var enableReturnReasons = false

    /**
     * Sets configuration for Capture feature.
     * Note that configuration is immutable. [cleanupCapture] needs to be called before passing a new configuration.
     *
     * @throws IllegalStateException if capture is already configured.
     */
    @Deprecated(
        "Please use setCaptureConfiguration(context, captureConfiguration) which allows instance recreation without having to call releaseCapture()",
        ReplaceWith("setCaptureConfiguration(context, captureConfiguration)")
    )
    fun setCaptureConfiguration(captureConfiguration: CaptureConfiguration) {
        check(giniCapture == null) { "Gini Capture already configured. Call releaseCapture() before setting a new configuration." }
        GiniBank.captureConfiguration = captureConfiguration
        GiniCapture.newInstance().applyConfiguration(captureConfiguration).build()
        giniCapture = GiniCapture.getInstance()
    }

    /**
     * Sets configuration for Capture feature.
     */
    fun setCaptureConfiguration(context: Context, captureConfiguration: CaptureConfiguration) {
        GiniBank.captureConfiguration = captureConfiguration
        GiniCapture.newInstance(context).applyConfiguration(captureConfiguration).build()
        giniCapture = GiniCapture.getInstance()

        releaseTransactionDocsFeature(context)
        BankSdkIsolatedKoinContext.init(context)
        getGiniCaptureKoin().loadModules(listOf(captureSdkDiBridge))
        this.giniBankTransactionDocs = GiniBankTransactionDocs()
    }


    /**
     * Provides transfer summary to Gini.
     *
     * Please provide the required transfer summary to improve the future extraction accuracy.
     *
     * Follow the recommendations below:
     * - Make sure to call this method before calling [cleanupCapture] if the user has completed TAN verification.
     * - Provide values for all necessary fields, including those that were not extracted.
     * - Provide the final data approved by the user (and not the initially extracted only).
     * - Send the transfer summary after TAN verification and provide the extraction values the user has used.
     *
     * @param paymentRecipient payment receiver
     * @param paymentReference ID based on Client ID (Kundennummer) and invoice ID (Rechnungsnummer)
     * @param paymentPurpose statement what this payment is for
     * @param iban international bank account
     * @param bic bank identification code
     * @param amount accepts extracted amount and currency
     */
    fun sendTransferSummary(
        paymentRecipient: String,
        paymentReference: String,
        paymentPurpose: String,
        iban: String,
        bic: String,
        amount: Amount
    ) {
        GiniCapture.sendTransferSummary(
            paymentRecipient, paymentReference, paymentPurpose, iban, bic, amount
        )
    }

    internal fun sendTransferSummaryForSkonto(
        amount: Amount,
        skontoAmountToPayCalculated: String,
        skontoPercentageDiscountedCalculated: String,
        skontoDueDateCalculated: String
    ) {
        GiniCapture.sendTransferSummaryForSkonto(
            amount,
            skontoAmountToPayCalculated,
            skontoPercentageDiscountedCalculated,
            skontoDueDateCalculated,

        )

    }

    /**
     * Frees up resources used by the capture flow.
     *
     * Please provide the required transfer summary to improve the future extraction accuracy.
     * Follow the recommendations below:
     *
     * - Provide values for all necessary fields, including those that were not extracted.</li>
     * - Provide the final data approved by the user (and not the initially extracted only).</li>
     * - Do cleanup after TAN verification.to clean up and provide the extraction values the user has used.</li>
     *
     * @param context Android context
     * @param paymentRecipient payment receiver
     * @param paymentReference ID based on Client ID (Kundennummer) and invoice ID (Rechnungsnummer)
     * @param paymentPurpose statement what this payment is for
     * @param iban international bank account
     * @param bic bank identification code
     * @param amount accepts extracted amount and currency
     *
     * @deprecated Use [sendTransferSummary] to provide the required transfer summary first (if the user has completed TAN verification) and then [cleanupCapture] to let the SDK free up used resources.
     */
    @Deprecated(
        "Please use sendTransferSummary() to provide the required transfer summary first (if the user has completed TAN verification) and then releaseCapture() to let the SDK free up used resources.",
        ReplaceWith("releaseCapture(context)")
    )
    fun releaseCapture(
        context: Context,
        paymentRecipient: String,
        paymentReference: String,
        paymentPurpose: String,
        iban: String,
        bic: String,
        amount: Amount
    ) {
        sendTransferSummary(
            paymentRecipient, paymentReference, paymentPurpose, iban, bic, amount
        )
        cleanupCapture(context)
        releaseTransactionDocsFeature(context)
        BankSdkIsolatedKoinContext.clean()
    }


    /**
     * Frees up resources used by the capture flow.
     *
     * @param context Android context
     *
     */
    @Deprecated(
        "Please use cleanupCapture(context). This method will be removed in a future release.",
        ReplaceWith("cleanupCapture(context)")
    )
    fun releaseCapture(
        context: Context
    ) {
        cleanupCapture(context)
    }

    /**
     * Frees up resources used by the capture flow.
     *
     * @param context Android context
     *
     */
    fun cleanupCapture(
        context: Context
    ) {
        GiniCapture.cleanup(
            context
        )
        captureConfiguration = null
        giniCapture = null

        digitalInvoiceOnboardingNavigationBarBottomAdapter =
            DefaultDigitalInvoiceOnboardingNavigationBarBottomAdapter()
        digitalInvoiceHelpNavigationBarBottomAdapter =
            DefaultDigitalInvoiceHelpNavigationBarBottomAdapter()

        digitalInvoiceOnboardingIllustrationAdapter = ImageOnboardingIllustrationAdapter(
            R.drawable.gbs_digital_invoice_list_image,
            R.string.gbs_digital_invoice_onboarding_text_1
        )

        digitalInvoiceNavigationBarBottomAdapter = DefaultDigitalInvoiceNavigationBarBottomAdapter()
        releaseTransactionDocsFeature(context)
        BankSdkIsolatedKoinContext.clean()
    }

    /**
     *  Checks hardware requirements for Capture feature.
     *  Requirements are not enforced, but are recommended to be checked before using.
     *
     * @deprecated Checking the requirements is no longer necessary and this method will be removed in a future release.
     *             The majority of Android devices already meet the SDK's requirements.
     */
    @Deprecated(
        "Checking the requirements is no longer necessary and this method will be removed in a future release."
    )
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

    fun createCaptureFlowFragment(): CaptureFlowFragment {
        check(giniCapture != null) { "Capture feature is not configured. Call setCaptureConfiguration before creating the CaptureFlowFragment." }
        return CaptureFlowFragment.createInstance()
    }

    fun createCaptureFlowFragmentForIntent(
        context: Context,
        intent: Intent,
        callback: (CreateCaptureFlowFragmentForIntentResult) -> Unit
    ): CancellationToken {
        check(giniCapture != null) { "Capture feature is not configured. Call setCaptureConfiguration before creating the CaptureFlowFragment." }
        BankSdkIsolatedKoinContext.init(context)
        return giniCapture!!.createDocumentForImportedFiles(
            intent,
            context,
            object : AsyncCallback<Document, ImportedFileValidationException> {
                override fun onSuccess(document: Document) {
                    callback(
                        CreateCaptureFlowFragmentForIntentResult.Success(
                            createCaptureFlowFragmentForDocument(document)
                        )
                    )
                }

                override fun onError(exception: ImportedFileValidationException) {
                    callback(CreateCaptureFlowFragmentForIntentResult.Error(exception))
                }

                override fun onCancelled() {
                    callback(CreateCaptureFlowFragmentForIntentResult.Cancelled)
                }

            })
    }


    sealed class CreateCaptureFlowFragmentForIntentResult {
        data class Success(val fragment: CaptureFlowFragment) :
            CreateCaptureFlowFragmentForIntentResult()

        data class Error(val exception: ImportedFileValidationException) :
            CreateCaptureFlowFragmentForIntentResult()

        object Cancelled : CreateCaptureFlowFragmentForIntentResult()
    }

    /**
     * Screen API for starting the capture flow when a pdf or image document was shared from another app.
     *
     * @param
     *
     * @throws IllegalStateException if the capture feature was not configured.
     */
    fun startCaptureFlowForIntent(
        resultLauncher: ActivityResultLauncher<CaptureImportInput>, context: Context, intent: Intent
    ): CancellationToken {
        giniCapture.let { capture ->
            check(capture != null) { "Capture feature is not configured. Call setCaptureConfiguration before starting the flow." }
            return capture.createDocumentForImportedFiles(
                intent,
                context,
                object : AsyncCallback<Document, ImportedFileValidationException> {
                    override fun onSuccess(result: Document) {
                        resultLauncher.launch(CaptureImportInput.Forward(result))
                    }

                    override fun onError(exception: ImportedFileValidationException?) {
                        resultLauncher.launch(
                            CaptureImportInput.Error(
                                exception?.validationError,
                                exception?.message
                            )
                        )
                    }

                    override fun onCancelled() {
                    }
                })
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
                paymentRequestResource.message, paymentRequestResource.exception
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
        requestId: String, resolvePaymentInput: ResolvePaymentInput
    ): ResolvedPayment {
        val api = giniApi
        check(api != null) { "Gini Api is not set" }
        return when (val resolvedPaymentResource = api.documentManager.resolvePaymentRequest(
            requestId,
            resolvePaymentInput.copy(amount = resolvePaymentInput.parseAmountToBackendFormat())
        )) {
            is Resource.Cancelled -> throw Exception("Cancelled")
            is Resource.Error -> throw Exception(
                resolvedPaymentResource.message, resolvedPaymentResource.exception
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

    /**
     * Results of document creation from an imported pdf or image(s).
     */
    sealed class CreateDocumentFromImportedFileResult {
        /**
         * The document was processed successfully.
         */
        data class Success(val document: Document?) : CreateDocumentFromImportedFileResult()

        /**
         * Document processing returned an error.
         */
        data class Error(val error: ImportedFileValidationException?) :
            CreateDocumentFromImportedFileResult()

        /**
         * Document processing was cancelled.
         */
        object Cancelled : CreateDocumentFromImportedFileResult()
    }

    /**
     *
     *  Create a document based on a pdf or image(s) imported from another app.
     *
     *  @param intent - intent from which to get files
     *  @param context - Android context
     *  @param callback - returns the wrapped result of the file processing in the form of [CreateDocumentFromImportedFileResult]
     */
    fun createDocumentForImportedFiles(
        intent: Intent,
        context: Context,
        callback: (CreateDocumentFromImportedFileResult) -> Unit
    ): CancellationToken? {
        return giniCapture?.createDocumentForImportedFiles(
            intent,
            context,
            object : AsyncCallback<Document, ImportedFileValidationException> {
                override fun onSuccess(result: Document?) {
                    callback(CreateDocumentFromImportedFileResult.Success(result))
                }

                override fun onError(exception: ImportedFileValidationException?) {
                    callback(CreateDocumentFromImportedFileResult.Error(exception))
                }

                override fun onCancelled() {
                    callback(CreateDocumentFromImportedFileResult.Cancelled)
                }
            }
        )
    }

    /**
     * Starts capture flow for a document. This method should be used with documents created by [GiniBank.createDocumentForImportedFiles] when a pdf or image was shared from another app.
     *
     * @param resultLauncher
     * @param document The document to be forwarded by the result launcher.
     */
    fun startCaptureFlowForDocument(
        resultLauncher: ActivityResultLauncher<CaptureImportInput>, document: Document
    ) {
        resultLauncher.launch(CaptureImportInput.Forward(document))
    }

    /**
     *  Creates a [CaptureFlowFragment] with a document. This method should be used with documents created by [GiniBank.createDocumentForImportedFiles] when a pdf or image was shared from another app.
     *
     *  @param document The document with which the fragment will be created.
     */
    fun createCaptureFlowFragmentForDocument(document: Document): CaptureFlowFragment {
        check(giniCapture != null) { "Capture feature is not configured. Call setCaptureConfiguration before starting the flow." }
        return CaptureFlowFragment.createInstance(document)
    }

    fun createInvoicePreviewFragment(
        screenTitle: String,
        giniApiDocumentId: String,
        infoTextLines: List<String> = emptyList()
    ): InvoicePreviewFragment {
        return InvoicePreviewFragment.createInstance(
            createInvoicePreviewFragmentArgs(screenTitle, giniApiDocumentId, infoTextLines)
        )
    }

    fun createInvoicePreviewFragmentArgs(
        screenTitle: String,
        giniApiDocumentId: String,
        infoTextLines: List<String> = emptyList()
    ): InvoicePreviewFragmentArgs {
        check(giniApiDocumentId.isNotBlank() && giniApiDocumentId.isNotEmpty()) {
            "Gini Api Document Id should not be empty or blank"
        }
        return InvoicePreviewFragmentArgs(
            screenTitle,
            giniApiDocumentId,
            infoTextLines.toTypedArray(),
            arrayOf()
        )
    }

    fun createTransactionDocInvoicePreviewFragmentArgs(
        screenTitle: String,
        giniApiDocumentId: String,
        infoTextLines: List<String> = emptyList()
    ): TransactionDocInvoicePreviewFragmentArgs {
        check(giniApiDocumentId.isNotBlank() && giniApiDocumentId.isNotEmpty()) {
            "Gini Api Document Id should not be empty or blank"
        }
        return TransactionDocInvoicePreviewFragmentArgs(
            screenTitle,
            giniApiDocumentId,
            infoTextLines.toTypedArray(),
            arrayOf()
        )
    }

    @Suppress("UnusedParameter")
    private fun releaseTransactionDocsFeature(context: Context) {
        giniBankTransactionDocs = null
    }

    /**
     *
     *  Kills Bank sdk and stops Gini payment flow
     *  Use this with cautions
     *
     */
    fun terminateSDK() {
        giniBankTerminateCallback?.invoke()
    }
}

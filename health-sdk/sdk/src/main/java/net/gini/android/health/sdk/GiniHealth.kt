package net.gini.android.health.sdk

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.response.DeleteDocumentErrorResponse
import net.gini.android.health.sdk.GiniHealth.TrustMarkerResponse
import net.gini.android.health.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.health.sdk.integratedFlow.PaymentFragment
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.PaymentRequest
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.model.toCommonPaymentDetails
import net.gini.android.health.sdk.review.model.toPaymentDetails
import net.gini.android.health.sdk.review.model.wrapToResult
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.paymentComponent.PaymentProviderAppsState
import net.gini.android.internal.payment.utils.DisplayedScreen
import net.gini.android.internal.payment.utils.GiniLocalization
import net.gini.android.internal.payment.utils.isValidIban
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference


/**
 * [GiniHealth] is one of the main classes for interacting with the Gini Health SDK. It manages interaction with the Gini Health API.
 *
 *  [documentFlow], [paymentFlow], [openBankState] are used by the [PaymentFragment] to observe their state, but they are public
 *  so that they can be observed anywhere, the main purpose for this is to observe errors.
 *
 *  [displayedScreen] is a shared flow which forwards the [DisplayedScreen] that is currently visible in the [PaymentFragment].
 *  It can be observed to update the activity title if needed.
 *
 *  [trustMarkersFlow] emits [TrustMarkerResponse], containing the icons of two payment providers and how many other payment providers
 *  are integrating Gini. It can be observed to integrate the trust markers into custom UI.
 */
class GiniHealth(
    giniHealthAPI: GiniHealthAPI,
    context: Context
) {

    val giniInternalPaymentModule: GiniInternalPaymentModule = GiniInternalPaymentModule(
        context = context,
        giniHealthAPI = giniHealthAPI
    ).also { internalPaymentModule ->
        CoroutineScope(Job()).launch(Dispatchers.IO) {
            internalPaymentModule.loadPaymentProviderApps()
        }

        CoroutineScope(Job()).launch(Dispatchers.IO) {
            internalPaymentModule.getConfigurations()
        }
    }

    val documentManager = giniInternalPaymentModule.giniHealthAPI.documentManager

    private var registryOwner = WeakReference<SavedStateRegistryOwner?>(null)
    private var savedStateObserver: LifecycleEventObserver? = null

    private var capturedArguments: CapturedArguments? = null

    private val _documentFlow = MutableStateFlow<ResultWrapper<Document>>(ResultWrapper.Loading())

    private lateinit var moshi: Moshi
    /**
     * A flow for getting the [Document] set for review [setDocumentForReview].
     *
     * It always starts with [ResultWrapper.Loading] when setting a document.
     * [Document] will be wrapped in [ResultWrapper.Success], otherwise the throwable will
     * be in a [ResultWrapper.Error].
     *
     * It never completes.
     */
    val documentFlow: StateFlow<ResultWrapper<Document>> = _documentFlow

    private val _paymentFlow = MutableStateFlow<ResultWrapper<PaymentDetails>>(ResultWrapper.Loading())

    /**
     * A flow for getting extracted [PaymentDetails] for the document set for review (see [setDocumentForReview]).
     *
     * It always starts with [ResultWrapper.Loading] when setting a document.
     * [PaymentDetails] will be wrapped in [ResultWrapper.Success], otherwise the throwable will
     * be in a [ResultWrapper.Error].
     *
     * It never completes.
     */
    val paymentFlow: StateFlow<ResultWrapper<PaymentDetails>> = _paymentFlow

    private val _openBankState = MutableStateFlow<PaymentState>(PaymentState.NoAction)

    /**
     * A flow that exposes the state of opening the bank. You can collect this flow to get information about the errors of this action.
     */
    val openBankState: StateFlow<PaymentState> = _openBankState

    private val _displayedScreen: MutableSharedFlow<DisplayedScreen> = MutableSharedFlow(extraBufferCapacity = 1)

    /**
     * A flow for exposing the [DisplayedScreen] currently visible. It always starts with [DisplayedScreen.Nothing].
     * It can be observed to update the UI, such as the toolbar title.
     */
    val displayedScreen: SharedFlow<DisplayedScreen> = _displayedScreen

    private val _trustMarkersFlow = giniInternalPaymentModule.paymentComponent.paymentProviderAppsFlow.map { result ->
        when (result) {
            is PaymentProviderAppsState.Error -> ResultWrapper.Error(result.throwable)
            PaymentProviderAppsState.Loading -> ResultWrapper.Loading()
            PaymentProviderAppsState.Nothing -> ResultWrapper.Loading()
            is PaymentProviderAppsState.Success -> ResultWrapper.Success<TrustMarkerResponse> (
                TrustMarkerResponse(
                    paymentProviderIcon = result.paymentProviderApps.firstOrNull()?.icon,
                    secondPaymentProviderIcon = if (result.paymentProviderApps.size >= 2) result.paymentProviderApps[1].icon else null,
                    extraPaymentProvidersCount = maxOf(result.paymentProviderApps.size - 2, 0)
                )
            )
        }
    }

    /**
     * A flow for getting information about trust markers.
     *
     * It always starts with [ResultWrapper.Loading] while payment providers are being loaded.
     * [TrustMarkerResponse] will be wrapped in [ResultWrapper.Success], otherwise the throwable will
     * be in a [ResultWrapper.Error].
     *
     * [TrustMarkerResponse] will contain the icons of two payment providers and the total count of the
     * other loaded payment providers.
     *
     * It never completes.
     */
    val trustMarkersFlow: Flow<ResultWrapper<TrustMarkerResponse>> = _trustMarkersFlow

    /**
     * Sets the app language to the desired one from the languages the SDK is supporting. If not set then defaults to the system's language locale.
     *
     * @param language enum value for the desired language or null for default system language
     * @param context Context object to save the configuration.
     */
    fun setSDKLanguage(localization: GiniLocalization, context: Context) {
        giniInternalPaymentModule.setSDKLanguage(localization, context)
    }

    /**
     * Returns the localization set for the app.
     *
     * @param context Context object to retrieve the value from.
     */
    fun getSDKLanguage(context: Context): GiniLocalization? {
        return GiniInternalPaymentModule.getSDKLanguage(context)
    }

    /**
     * Sets a [Document] for review. Results can be collected from [documentFlow] and [paymentFlow].
     *
     * @param document document received from Gini API.
     */
    suspend fun setDocumentForReview(document: Document) {
        giniInternalPaymentModule.setPaymentDetails(null)
        capturedArguments = CapturedArguments.DocumentInstance(document)
        _documentFlow.value = ResultWrapper.Success(document)
        _paymentFlow.value = ResultWrapper.Loading()

        _paymentFlow.value = wrapToResult {
            documentManager.getAllExtractionsWithPolling(document).mapSuccess {
                Resource.Success(it.data.toPaymentDetails())
            }
        }
    }

    /**
     * This function deletes a payment request by its unique `paymentRequestId`.
     * If the deletion is successful, it returns `null`. Otherwise, if an error occurs or the request is cancelled,
     * it returns the corresponding error message or `"Request cancelled"`.
     *
     * @param paymentRequestId The unique identifier of the payment request to be deleted.
     * @return `null` if the deletion is successful, otherwise the error message or `"Request cancelled"`.
     */
    suspend fun deletePaymentRequest(paymentRequestId: String): String? {
        val response =
            giniInternalPaymentModule.giniHealthAPI.documentManager.deletePaymentRequest(
                paymentRequestId
            )
        return when (response) {
            is Resource.Success -> null
            is Resource.Error -> response.message ?: response.responseStatusCode?.let {
                "Failed to delete payment request with status code: $it"
                } ?: "Failed to delete payment request"

            is Resource.Cancelled -> "Request cancelled"
        }
    }

    /**
     * Sets a [Document] for review. Results can be collected from [documentFlow] and [paymentFlow].
     *
     * @param documentId id of the document returned by Gini API.
     * @param paymentDetails optional [PaymentDetails] for the document corresponding to [documentId]
     */
    suspend fun setDocumentForReview(documentId: String, paymentDetails: PaymentDetails? = null) {
        LOG.debug("Setting document for review with id: $documentId")
        giniInternalPaymentModule.setPaymentDetails(null)
        capturedArguments = CapturedArguments.DocumentId(documentId, paymentDetails)
        _paymentFlow.value = ResultWrapper.Loading()
        _documentFlow.value = ResultWrapper.Loading()
        _documentFlow.value = wrapToResult {
            documentManager.getDocument(documentId)
        }
        if (paymentDetails != null) {
            _paymentFlow.value = ResultWrapper.Success(paymentDetails)
        } else {
            when (val documentResult = documentFlow.value) {
                is ResultWrapper.Success -> {
                    _paymentFlow.value =
                        wrapToResult { documentManager.getAllExtractionsWithPolling(documentResult.value).mapSuccess {
                            Resource.Success(it.data.toPaymentDetails()) }
                        }
                }
                is ResultWrapper.Error -> {
                    _paymentFlow.value = ResultWrapper.Error(Throwable("Failed to get document"))
                }
                is ResultWrapper.Loading -> {}
            }
        }
    }

    /**
     * Checks whether the document is payable by fetching the document and its extractions from the
     * Gini Pay API and verifying that the extraction's payment state is "Payable".
     *
     * @return `true` if the document is payable and `false` otherwise
     * @throws Exception if there was an error while retrieving the document or the extractions
     */
    suspend fun checkIfDocumentIsPayable(documentId: String): Boolean {
        return when (val extractions = getExtractionsForDocument(documentId)) {
            null -> false
            else -> (extractions.specificExtractions["payment_state"]?.value ?: "") == PAYABLE
        }
    }

    /**
     * Checks whether the invoice contains multiple documents by fetching the invoice and its extractions from the
     * Gini Pay API and verifying whether it has multiple documents or not.
     *
     * @return `true` if the document contains multiple documents and `false` otherwise
     * @throws Exception if there was an error while retrieving the document or the extractions
     */
    suspend fun checkIfDocumentContainsMultipleDocuments(documentId: String): Boolean {
        return when (val extractions = getExtractionsForDocument(documentId)) {
            null -> false
            else -> (extractions.specificExtractions["contains_multiple_docs"]?.value ?: "" ) == HAS_MULTIPLE_DOCUMENTS
        }
    }

    /**
     * Deletes multiple documents in one go.
     * If request was successful, it returns null.
     * If request failed, it returns a [DeleteDocumentErrorResponse], with more information about why the request failed.
     *
     * @param documentIds the list of documentIds to be deleted
     * @return [DeleteDocumentErrorResponse] with more information about why the request failed
     */
    suspend fun deleteDocuments(documentIds: List<String>): DeleteDocumentErrorResponse? {
        val response = giniInternalPaymentModule.giniHealthAPI.documentManager.deleteDocuments(documentIds)
        return when (response) {
            is Resource.Success -> {
                null
            }

            is Resource.Error -> {
                LoggerFactory.getLogger(GiniInternalPaymentModule::class.java)
                    .error("Failed to delete documents with ids: ${response.exception}")
                response.message?.let { failureMessage ->
                    if (!this::moshi.isInitialized) {
                        moshi = Moshi.Builder()
                            .build()
                    }
                    val deleteDocumentsError = moshi.adapter(DeleteDocumentErrorResponse::class.java).lenient().fromJson(failureMessage)
                    return deleteDocumentsError
                }
                return DeleteDocumentErrorResponse()
            }

            is Resource.Cancelled -> {
                LoggerFactory.getLogger(GiniInternalPaymentModule::class.java)
                    .error("Deleting documents was cancelled")
                DeleteDocumentErrorResponse(message = "Delete documents request was cancelled")
            }
        }
    }

    private suspend fun getExtractionsForDocument(documentId: String): ExtractionsContainer? {
        val extractionsResource = documentManager.getDocument(documentId)
            .mapSuccess { documentResource ->
                documentManager.getAllExtractionsWithPolling(documentResource.data)
            }
        return when (extractionsResource) {
            is Resource.Cancelled -> null
            is Resource.Error -> throw Exception(extractionsResource.exception)
            is Resource.Success -> extractionsResource.data
        }
    }

    internal fun setOpenBankState(state: PaymentState, scope: CoroutineScope) {
        _openBankState.value = state
        scope.launch {
            withContext(NonCancellable) {
                delay(50)
                _openBankState.value = PaymentState.NoAction
            }
        }
    }

    internal suspend fun retryDocumentReview() {
        when (val arguments = capturedArguments) {
            is CapturedArguments.DocumentId -> setDocumentForReview(arguments.id, arguments.paymentDetails)
            is CapturedArguments.DocumentInstance -> setDocumentForReview(arguments.value)
            null -> { // Nothing
            }
        }
    }

    /**
     * Sets a lifecycle observer to handle state restoration after the system kills the app.
     *
     * @param registryOwner The SavedStateRegistryOwner to which the observer is attached.
     * @param retryScope Should be a scope [setDocumentForReview] would be called in (ex: viewModelScope).
     */
    fun setSavedStateRegistryOwner(registryOwner: SavedStateRegistryOwner, retryScope: CoroutineScope) {
        this.registryOwner.get()?.let { registry ->
            savedStateObserver?.let { observer ->
                registry.lifecycle.removeObserver(observer)
            }
        }
        this.registryOwner = WeakReference(registryOwner)
        savedStateObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                val registry = this.registryOwner.get()?.savedStateRegistry
                registry?.registerSavedStateProvider(PROVIDER, savedStateProvider)

                val state = registry?.consumeRestoredStateForKey(PROVIDER)
                if (capturedArguments == null) {
                    capturedArguments = when (state?.getString(CAPTURED_ARGUMENTS_TYPE)) {
                        CAPTURED_ARGUMENTS_ID -> state.getParcelable<CapturedArguments.DocumentId>(CAPTURED_ARGUMENTS)
                        CAPTURED_ARGUMENTS_DOCUMENT -> state.getParcelable<CapturedArguments.DocumentInstance>(
                            CAPTURED_ARGUMENTS
                        )
                        else -> null
                    }
                    retryScope.launch {
                        retryDocumentReview()
                    }
                }
            }
        }.also { observer ->
            registryOwner.lifecycle.addObserver(observer)
        }
    }

    /**
     * Creates an instance of the [PaymentFragment] with the given documentId and
     * configuration.
     *
     * @param documentId The document id for which the extractions should be loaded
     * @param configuration The configuration for the [PaymentFragment]
     */
    fun getPaymentFragmentWithDocument(documentId: String, configuration: PaymentFlowConfiguration?): PaymentFragment {
        LOG.debug("Getting payment review fragment for id: {}", documentId)
        giniInternalPaymentModule.setPaymentDetails(null)
        _paymentFlow.value = ResultWrapper.Loading()
        return PaymentFragment.newInstance(
            giniHealth = this,
            documentId = documentId,
            paymentFlowConfiguration = configuration ?: PaymentFlowConfiguration()
        )
    }

    /**
     * Creates an instance of [PaymentFragment] with the given payment details and
     * configuration.
     *
     * @param paymentDetails The payment details
     * @param configuration The configuration for the [PaymentFragment]
     * @throws IllegalStateException if any of the payment details ([recipient], [IBAN], [amount], [purpose]] are empty
     * or the [IBAN] is invalid.
     */
    fun getPaymentFragmentWithoutDocument(paymentDetails: PaymentDetails, configuration: PaymentFlowConfiguration?): PaymentFragment {
        LOG.debug("Getting payment fragment for payment details: {}", paymentDetails.toString())
        if (paymentDetails.iban.isEmpty() || paymentDetails.amount.isEmpty() || paymentDetails.purpose.isEmpty() || paymentDetails.recipient.isEmpty()) {
            throw IllegalStateException("Payment details are incomplete")
        }
        if (!isValidIban(paymentDetails.iban)) {
            throw IllegalStateException("Iban is invalid")
        }
        giniInternalPaymentModule.setPaymentDetails(paymentDetails.toCommonPaymentDetails())
        _paymentFlow.value = ResultWrapper.Loading()
        val paymentFragment = PaymentFragment.newInstance(
            giniHealth = this,
            paymentDetails = paymentDetails,
            paymentFlowConfiguration = configuration ?: PaymentFlowConfiguration()
        )
        return paymentFragment
    }

    /**
     * Manually load payment provider apps, in case there was an error when trying to load them automatically.
     */
    suspend fun loadPaymentProviders() = giniInternalPaymentModule.paymentComponent.loadPaymentProviderApps()

    private val savedStateProvider = SavedStateRegistry.SavedStateProvider {
        Bundle().apply {
            when (capturedArguments) {
                is CapturedArguments.DocumentId -> {
                    this.putString(CAPTURED_ARGUMENTS_TYPE, CAPTURED_ARGUMENTS_ID)
                    this.putParcelable(CAPTURED_ARGUMENTS, capturedArguments)
                }
                is CapturedArguments.DocumentInstance -> {
                    this.putString(CAPTURED_ARGUMENTS_TYPE, CAPTURED_ARGUMENTS_DOCUMENT)
                    this.putParcelable(CAPTURED_ARGUMENTS, capturedArguments)
                }
                null -> this.putString(CAPTURED_ARGUMENTS_TYPE, CAPTURED_ARGUMENTS_NULL)
            }
        }
    }

    internal fun setDisplayedScreen(screen: DisplayedScreen) {
        _displayedScreen.tryEmit(screen)
    }

    private sealed class CapturedArguments : Parcelable {
        @Parcelize
        class DocumentInstance(val value: Document) : CapturedArguments()

        @Parcelize
        class DocumentId(val id: String, val paymentDetails: PaymentDetails? = null) : CapturedArguments()
    }

    sealed class PaymentState {
        object NoAction : PaymentState()
        object Loading : PaymentState()
        object Cancel : PaymentState()
        class Success(val paymentRequest: PaymentRequest) : PaymentState()
        class Error(val throwable: Throwable) : PaymentState()
    }

    data class TrustMarkerResponse(
        val paymentProviderIcon: BitmapDrawable?,
        val secondPaymentProviderIcon: BitmapDrawable?,
        val extraPaymentProvidersCount: Int
    )

    companion object {
        private val LOG = LoggerFactory.getLogger(GiniHealth::class.java)

        private const val CAPTURED_ARGUMENTS_NULL = "CAPTURED_ARGUMENTS_NULL"
        private const val CAPTURED_ARGUMENTS_ID = "CAPTURED_ARGUMENTS_ID"
        private const val CAPTURED_ARGUMENTS_DOCUMENT = "CAPTURED_ARGUMENTS_DOCUMENT"
        private const val CAPTURED_ARGUMENTS_TYPE = "CAPTURED_ARGUMENTS_TYPE"
        private const val PROVIDER = "net.gini.android.health.sdk.GiniHealth"
        private const val CAPTURED_ARGUMENTS = "CAPTURED_ARGUMENTS"
        private const val PAYABLE = "Payable"
        private const val HAS_MULTIPLE_DOCUMENTS = "true"
    }
}

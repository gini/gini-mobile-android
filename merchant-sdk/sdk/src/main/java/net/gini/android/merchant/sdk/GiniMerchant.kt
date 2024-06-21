package net.gini.android.merchant.sdk

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.GiniHealthAPIBuilder
import net.gini.android.merchant.sdk.api.ResultWrapper
import net.gini.android.merchant.sdk.api.authorization.HealthApiSessionManagerAdapter
import net.gini.android.merchant.sdk.api.authorization.SessionManager
import net.gini.android.merchant.sdk.api.payment.model.Payment
import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.api.payment.model.PaymentRequest
import net.gini.android.merchant.sdk.api.payment.model.getPaymentExtraction
import net.gini.android.merchant.sdk.api.payment.model.toPayment
import net.gini.android.merchant.sdk.api.payment.model.toPaymentDetails
import net.gini.android.merchant.sdk.api.payment.model.toPaymentRequest
import net.gini.android.merchant.sdk.api.wrapToResult
import net.gini.android.merchant.sdk.util.DisplayedScreen
import net.gini.android.merchant.sdk.util.extensions.toException
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference

/**
 * [GiniMerchant] is one of the main classes for interacting with the Gini Merchant SDK.
 *
 *  [documentFlow], [paymentFlow], [openBankState] are used by the [ReviewFragment] to observe their state, but they are public
 *  so that they can be observed anywhere, the main purpose for this is to observe errors.
 */
class GiniMerchant(
    private val context: Context,
    private val clientId: String = "",
    private val clientSecret: String = "",
    private val emailDomain: String = "",
    private val sessionManager: SessionManager? = null,
    private val merchantApiBaseUrl: String? = null,
    private val userCenterApiBaseUrl: String? = null,
    private val debuggingEnabled: Boolean = false,
) {

    // Lazily initialized backing field for giniHealthAPI to allow mocking it for tests.
    private var _giniHealthAPI: GiniHealthAPI? = null
    internal val giniHealthAPI: GiniHealthAPI
        get() {
            _giniHealthAPI?.let { return it }
                ?: run {
                    val healthAPI = if (sessionManager == null) {
                        GiniHealthAPIBuilder(
                            context,
                            clientId,
                            clientSecret,
                            emailDomain
                        )
                    } else {
                        GiniHealthAPIBuilder(context, sessionManager = HealthApiSessionManagerAdapter(sessionManager))
                    }.apply {
                        if (merchantApiBaseUrl != null) {
                            setApiBaseUrl(merchantApiBaseUrl)
                        }
                        if (userCenterApiBaseUrl != null) {
                            setUserCenterApiBaseUrl(userCenterApiBaseUrl)
                        }
                        setDebuggingEnabled(debuggingEnabled)
                    }.build()
                    _giniHealthAPI = healthAPI
                    return healthAPI
                }
        }

    private var registryOwner = WeakReference<SavedStateRegistryOwner?>(null)
    private var savedStateObserver: LifecycleEventObserver? = null

    private var capturedArguments: CapturedArguments? = null

    private val _documentFlow = MutableStateFlow<ResultWrapper<Document>>(ResultWrapper.Loading())

    /**
     * A flow for getting the [Document] set for review [setDocumentForReview].
     *
     * It always starts with [ResultWrapper.Loading] when setting a document.
     * [Document] will be wrapped in [ResultWrapper.Success], otherwise the throwable will
     * be in a [ResultWrapper.Error].
     *
     * It never completes.
     */
    internal val documentFlow: StateFlow<ResultWrapper<Document>> = _documentFlow

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
    internal val paymentFlow: StateFlow<ResultWrapper<PaymentDetails>> = _paymentFlow

    /**
     * A flow that exposes events from the Merchant SDK. You can collect this flow to be informed about events such as errors,
     * successful payment requests or which screen is being displayed.
     */

    private val _eventsFlow: MutableSharedFlow<MerchantSDKEvents> = MutableSharedFlow(extraBufferCapacity = 1)

    val eventsFlow: SharedFlow<MerchantSDKEvents> = _eventsFlow

    @VisibleForTesting
    internal fun replaceHealthApiInstance(giniHealthAPI: GiniHealthAPI) {
        _giniHealthAPI = giniHealthAPI
    }

    // TODO EC-62: Made private because Document is from the Health API Library. This is still needed internally for restoring the saved state.
    /**
     * Sets a [Document] for review. Results can be collected from [documentFlow] and [paymentFlow].
     *
     * @param document document received from Gini API.
     */
    private suspend fun setDocumentForReview(document: Document) {
        capturedArguments = CapturedArguments.DocumentInstance(document)
        _documentFlow.value = ResultWrapper.Success(document)
        _paymentFlow.value = ResultWrapper.Loading()

        _paymentFlow.value = wrapToResult {
            giniHealthAPI.documentManager.getAllExtractionsWithPolling(document).mapSuccess {
                Resource.Success(it.data.toPaymentDetails())
            }
        }
    }

    // TODO EC-62: Add method and tests for setting image/PDF instead of document or document id
    /**
     * Sets a Document for review. Results can be collected from [documentFlow] and [paymentFlow].
     *
     * @param documentId id of the document returned by Gini API.
     * @param paymentDetails optional [PaymentDetails] for the document corresponding to [documentId]
     */
    suspend fun setDocumentForReview(documentId: String, paymentDetails: PaymentDetails? = null) {
        LOG.debug("Setting document for review with id: $documentId")

        capturedArguments = CapturedArguments.DocumentId(documentId, paymentDetails)
        _eventsFlow.tryEmit(MerchantSDKEvents.OnLoading)
        _paymentFlow.value = ResultWrapper.Loading()

        _documentFlow.value = wrapToResult {
            giniHealthAPI.documentManager.getDocument(documentId)
        }
        if (paymentDetails != null) {
            _paymentFlow.value = ResultWrapper.Success(paymentDetails)
        } else {
            when (val documentResult = _documentFlow.value) {
                is ResultWrapper.Success -> {
                    _paymentFlow.value =
                        wrapToResult { giniHealthAPI.documentManager.getAllExtractionsWithPolling(documentResult.value).mapSuccess {
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
     * Gini Pay API and verifying that the extractions contain an IBAN.
     *
     * @return `true` if the document is payable and `false` otherwise
     * @throws Exception if there was an error while retrieving the document or the extractions
     */
    suspend fun checkIfDocumentIsPayable(documentId: String): Boolean {
        val extractionsResource = giniHealthAPI.documentManager.getDocument(documentId)
            .mapSuccess { documentResource ->
                giniHealthAPI.documentManager.getAllExtractionsWithPolling(documentResource.data)
            }
        return when (extractionsResource) {
            is Resource.Cancelled -> false
            is Resource.Error -> throw Exception(extractionsResource.exception)
            is Resource.Success -> extractionsResource.data.compoundExtractions
                .getPaymentExtraction("iban")?.value?.isNotEmpty() ?: false
        }
    }

    // TODO EC-62: Use this method in the example app
    suspend fun getPaymentRequest(paymentRequestId: String): Result<PaymentRequest> {
        return when (val paymentRequestResource = giniHealthAPI.documentManager.getPaymentRequest(paymentRequestId)) {
            is Resource.Cancelled -> Result.failure(Exception("Cancelled"))
            is Resource.Error -> Result.failure(paymentRequestResource.toException())
            is Resource.Success -> {
                Result.success(paymentRequestResource.data.toPaymentRequest(paymentRequestId))
            }
        }
    }

    // TODO EC-62: Use this method in the example app
    suspend fun getPayment(paymentRequestId: String): Result<Payment> {
        return when (val paymentRequestResource = giniHealthAPI.documentManager.getPayment(paymentRequestId)) {
            is Resource.Cancelled -> Result.failure(Exception("Cancelled"))
            is Resource.Error -> Result.failure(paymentRequestResource.toException())
            is Resource.Success -> {
                Result.success(paymentRequestResource.data.toPayment())
            }
        }
    }

    //TODO set payment provider app name after making payment provider app internal
    internal fun emitSDKEvent(state: PaymentState) {
        when (state) {
            is PaymentState.Success -> _eventsFlow.tryEmit(
                MerchantSDKEvents.OnFinishedWithPaymentRequestCreated(
                    state.paymentRequest.id,
                    ""
//                    state.paymentRequest.paymentProviderApp?.name ?: ""
                )
            )

            is PaymentState.Error -> _eventsFlow.tryEmit(MerchantSDKEvents.OnErrorOccurred(state.throwable))
            PaymentState.Loading -> _eventsFlow.tryEmit(MerchantSDKEvents.OnLoading)
            else -> {}
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

    internal fun setDisplayedScreen(displayedScreen: DisplayedScreen) {
        _eventsFlow.tryEmit(MerchantSDKEvents.OnScreenDisplayed(displayedScreen))
    }

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

    private sealed class CapturedArguments : Parcelable {
        @Parcelize
        class DocumentInstance(val value: Document) : CapturedArguments()

        @Parcelize
        class DocumentId(val id: String, val paymentDetails: PaymentDetails? = null) : CapturedArguments()
    }

    sealed class PaymentState {
        object NoAction : PaymentState()
        object Loading : PaymentState()
        class Success(val paymentRequest: PaymentRequest) : PaymentState()
        class Error(val throwable: Throwable) : PaymentState()
    }

    sealed class MerchantSDKEvents {
        object OnLoading: MerchantSDKEvents()
        class OnScreenDisplayed(val displayedScreen: DisplayedScreen): MerchantSDKEvents()
        class OnFinishedWithPaymentRequestCreated(val paymentRequestId: String, val paymentProviderName: String): MerchantSDKEvents()
        class OnFinishedWithCancellation(): MerchantSDKEvents()
        class OnErrorOccurred(val throwable: Throwable): MerchantSDKEvents()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(GiniMerchant::class.java)

        private const val CAPTURED_ARGUMENTS_NULL = "CAPTURED_ARGUMENTS_NULL"
        private const val CAPTURED_ARGUMENTS_ID = "CAPTURED_ARGUMENTS_ID"
        private const val CAPTURED_ARGUMENTS_DOCUMENT = "CAPTURED_ARGUMENTS_DOCUMENT"
        private const val CAPTURED_ARGUMENTS_TYPE = "CAPTURED_ARGUMENTS_TYPE"
        private const val PROVIDER = "net.gini.android.merchant.sdk.GiniMerchant"
        private const val CAPTURED_ARGUMENTS = "CAPTURED_ARGUMENTS"
    }
}
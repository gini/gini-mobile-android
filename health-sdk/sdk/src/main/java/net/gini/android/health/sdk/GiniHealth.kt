package net.gini.android.health.sdk

import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.sdk.review.ReviewFragment
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.PaymentRequest
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.model.getPaymentExtraction
import net.gini.android.health.sdk.review.model.toPaymentDetails
import net.gini.android.health.sdk.review.model.wrapToResult
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference

/**
 * [GiniHealth] is the main class for interacting with the Gini Health SDK.
 * It provides a way to submit a document for reviewing its extracted payment details and
 * lets the user make the payment with one of the payment providers.
 *
 * The recommended flow is:
 *  1. Call one of the overloads of [setDocumentForReview], to submit a document.
 *  2. Display [ReviewFragment].
 *
 * [setDocumentForReview] can be called with:
 *  1. [Document] instance in the case the upload was performed with Gini Pay Api lib ([GiniHealthAPI]).
 *  2. Document id, this will probably be the case when there's backend integration between the Health SDK and the Gini Health API.
 *      This method will make a network call to obtain a [Document] instance so the other one is preferred if you have the [Document] instance.
 *
 *  [documentFlow], [paymentFlow], [openBankState] are used by the ReviewFragment to observe their state, but they are public
 *  so that they can be observed anywhere, the main purpose for this is to observe errors.
 */
class GiniHealth(
    val giniHealthAPI: GiniHealthAPI
) {
    private val documentManager = giniHealthAPI.documentManager

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

    /**
     * Sets a [Document] for review. Results can be collected from [documentFlow] and [paymentFlow].
     *
     * @param document document received from Gini API.
     */
    suspend fun setDocumentForReview(document: Document) {
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
     * Sets a [Document] for review. Results can be collected from [documentFlow] and [paymentFlow].
     *
     * @param documentId id of the document returned by Gini API.
     * @param paymentDetails optional [PaymentDetails] for the document corresponding to [documentId]
     */
    suspend fun setDocumentForReview(documentId: String, paymentDetails: PaymentDetails? = null) {
        LOG.debug("Setting document for review with id: $documentId")

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
     * Gini Pay API and verifying that the extractions contain an IBAN.
     *
     * @return `true` if the document is payable and `false` otherwise
     * @throws Exception if there was an error while retrieving the document or the extractions
     */
    suspend fun checkIfDocumentIsPayable(documentId: String): Boolean {
        val extractionsResource = documentManager.getDocument(documentId)
            .mapSuccess { documentResource ->
                documentManager.getAllExtractionsWithPolling(documentResource.data)
            }
        return when (extractionsResource) {
            is Resource.Cancelled -> false
            is Resource.Error -> throw Exception(extractionsResource.exception)
            is Resource.Success -> extractionsResource.data.compoundExtractions
                .getPaymentExtraction("iban")?.value?.isNotEmpty() ?: false
        }
    }

    internal fun setOpenBankState(state: PaymentState) {
        _openBankState.value = state
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

    companion object {
        private val LOG = LoggerFactory.getLogger(GiniHealth::class.java)

        private const val CAPTURED_ARGUMENTS_NULL = "CAPTURED_ARGUMENTS_NULL"
        private const val CAPTURED_ARGUMENTS_ID = "CAPTURED_ARGUMENTS_ID"
        private const val CAPTURED_ARGUMENTS_DOCUMENT = "CAPTURED_ARGUMENTS_DOCUMENT"
        private const val CAPTURED_ARGUMENTS_TYPE = "CAPTURED_ARGUMENTS_TYPE"
        private const val PROVIDER = "net.gini.android.health.sdk.GiniHealth"
        private const val CAPTURED_ARGUMENTS = "CAPTURED_ARGUMENTS"
    }
}
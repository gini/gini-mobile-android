package net.gini.android.health.sdk

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.sdk.review.ReviewFragment
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.PaymentRequest
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.model.toPaymentDetails
import net.gini.android.health.sdk.review.model.wrapToResult
import net.gini.android.health.sdk.util.GiniLocalization
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference

/**
 * [GiniHealth] is one of the main classes for interacting with the Gini Health SDK. It manages interaction with the Gini Health API.
 *
 *  [documentFlow], [paymentFlow], [openBankState] are used by the [ReviewFragment] to observe their state, but they are public
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
     * Sets the app language to the desired one from the languages the SDK is supporting. If not set then defaults to the system's language locale.
     *
     * @param language enum value for the desired language or null for default system language
     * @param context Context object to save the configuration.
     */
    fun setSDKLanguage(language: GiniLocalization?, context: Context) {
        localizedContext = null
        GiniHealthPreferences(context).saveSDKLanguage(language)
    }

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

    internal class GiniHealthPreferences(context: Context) {
        private val sharedPreferences = context.getSharedPreferences("GiniHealthPreferences", Context.MODE_PRIVATE)

        fun saveSDKLanguage(value: GiniLocalization?) {
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putString(SDK_LANGUAGE_PREFS_KEY, value?.readableName?.uppercase())
            editor.apply()
        }

        fun getSDKLanguage(): GiniLocalization? {
            val enumValue = sharedPreferences.getString(SDK_LANGUAGE_PREFS_KEY, null)
            return if (enumValue.isNullOrEmpty()) null else GiniLocalization.valueOf(enumValue)
        }
    }

    internal var localizedContext: Context? = null

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
        private const val SDK_LANGUAGE_PREFS_KEY = "SDK_LANGUAGE_PREFS_KEY"

        fun getSDKLanguage(context: Context): GiniLocalization? {
            return GiniHealthPreferences(context).getSDKLanguage()
        }
    }
}

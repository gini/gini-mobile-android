package net.gini.android.merchant.sdk

import android.content.Context
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parcelize
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.GiniHealthAPIBuilder
import net.gini.android.merchant.sdk.api.ResultWrapper
import net.gini.android.merchant.sdk.api.authorization.HealthApiSessionManagerAdapter
import net.gini.android.merchant.sdk.api.authorization.SessionManager
import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.api.payment.model.PaymentRequest
import net.gini.android.merchant.sdk.api.payment.model.getPaymentExtraction
import net.gini.android.merchant.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.merchant.sdk.integratedFlow.PaymentFragment
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.util.DisplayedScreen
import org.slf4j.LoggerFactory

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
    private val merchantApiBaseUrl: String = MERCHANT_BASE_URL,
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
                            emailDomain,
                            apiVersion = API_VERSION
                        )
                    } else {
                        GiniHealthAPIBuilder(context, sessionManager = HealthApiSessionManagerAdapter(sessionManager), apiVersion = API_VERSION)
                    }.apply {
                        setApiBaseUrl(merchantApiBaseUrl)
                        if (userCenterApiBaseUrl != null) {
                            setUserCenterApiBaseUrl(userCenterApiBaseUrl)
                        }
                        setDebuggingEnabled(debuggingEnabled)
                    }.build()
                    _giniHealthAPI = healthAPI
                    return healthAPI
                }
        }

    internal var paymentComponent = PaymentComponent(context, healthAPI = giniHealthAPI)

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
    
    fun getFragment(iban: String, recipient: String, amount: String, purpose: String, flowConfiguration: PaymentFlowConfiguration? = null): PaymentFragment {
        val paymentDetails = PaymentDetails(
            recipient = recipient,
            iban = iban,
            purpose = purpose,
            amount = amount
        )

        val paymentFragment = PaymentFragment.newInstance(
            giniMerchant = this,
            paymentDetails = paymentDetails,
            paymentFlowConfiguration = flowConfiguration ?: PaymentFlowConfiguration()
        )
        _paymentFlow.tryEmit(ResultWrapper.Success(paymentDetails))
        return paymentFragment
    }

    internal fun emitSDKEvent(state: PaymentState) {
        when (state) {
            is PaymentState.Success -> _eventsFlow.tryEmit(
                MerchantSDKEvents.OnFinishedWithPaymentRequestCreated(
                    state.paymentRequest.id,
                    state.paymentProviderName
                )
            )

            is PaymentState.Error -> _eventsFlow.tryEmit(MerchantSDKEvents.OnErrorOccurred(state.throwable))
            PaymentState.Loading -> _eventsFlow.tryEmit(MerchantSDKEvents.OnLoading)
            PaymentState.NoAction -> _eventsFlow.tryEmit(MerchantSDKEvents.NoAction)
        }
    }

    internal fun setDisplayedScreen(displayedScreen: DisplayedScreen) {
        _eventsFlow.tryEmit(MerchantSDKEvents.OnScreenDisplayed(displayedScreen))
    }

    internal fun setFlowCancelled() {
        _eventsFlow.tryEmit(MerchantSDKEvents.OnFinishedWithCancellation())
    }

    /**
     * Loads payment provider apps - can be used before starting the payment flow for faster loading
     */
    suspend fun loadPaymentProviderApps() = paymentComponent.loadPaymentProviderApps()

    private sealed class CapturedArguments : Parcelable {
        @Parcelize
        class DocumentInstance(val value: Document) : CapturedArguments()

        @Parcelize
        class DocumentId(val id: String, val paymentDetails: PaymentDetails? = null) : CapturedArguments()
    }

    sealed class PaymentState {
        object NoAction : PaymentState()
        object Loading : PaymentState()
        class Success(val paymentRequest: PaymentRequest, val paymentProviderName: String) : PaymentState()
        class Error(val throwable: Throwable) : PaymentState()
    }

    sealed class MerchantSDKEvents {
        object NoAction: MerchantSDKEvents()
        object OnLoading: MerchantSDKEvents()
        class OnScreenDisplayed(val displayedScreen: DisplayedScreen): MerchantSDKEvents()
        class OnFinishedWithPaymentRequestCreated(val paymentRequestId: String, val paymentProviderName: String): MerchantSDKEvents()
        class OnFinishedWithCancellation(): MerchantSDKEvents()
        class OnErrorOccurred(val throwable: Throwable): MerchantSDKEvents()
    }

    companion object {
        const val API_VERSION = 1

        private val LOG = LoggerFactory.getLogger(GiniMerchant::class.java)

        internal const val SHARE_WITH_INTENT_FILTER = "share_intent_filter"
        internal const val MERCHANT_BASE_URL = "https://merchant-api.gini.net/"
    }
}

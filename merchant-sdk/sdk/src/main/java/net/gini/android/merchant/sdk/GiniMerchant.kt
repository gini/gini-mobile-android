package net.gini.android.merchant.sdk

import android.content.Context
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parcelize
import net.gini.android.core.api.models.Document
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.GiniHealthAPIBuilder
import net.gini.android.merchant.sdk.api.ResultWrapper
import net.gini.android.merchant.sdk.api.authorization.HealthApiSessionManagerAdapter
import net.gini.android.merchant.sdk.api.authorization.SessionManager
import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.api.payment.model.PaymentRequest
import net.gini.android.merchant.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.merchant.sdk.integratedFlow.PaymentFragment
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.util.DisplayedScreen
import net.gini.android.merchant.sdk.util.toBackendFormat
import org.slf4j.LoggerFactory

/**
 *  [GiniMerchant] is one of the main classes for interacting with the Gini Merchant SDK. It provides a way to create the [PaymentFragment],
 *  which is the entrypoint to the Merchant SDK.
 *
 *  [eventsFlow] is used by the [PaymentFragment] to observe its state, but it is public
 *  so that it can be observed anywhere, the main purpose for this is to observe screen changes and finish events.
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
     * Creates and returns the [PaymentFragment]. Checks if [iban], [recipient], [amount] and [purpose] are empty and throws [IllegalStateException] if any of them are.
     *
     * @param iban - the iban of the recipient
     * @param recipient
     * @param amount - the amount to be paid
     * @param purpose - the purpose of the payment
     * @param flowConfiguration - optional parameter with the [PaymentFlowConfiguration]
     */
    fun createFragment(iban: String, recipient: String, amount: String, purpose: String, flowConfiguration: PaymentFlowConfiguration? = null): PaymentFragment {
        if (iban.isEmpty() || recipient.isEmpty() || amount.isEmpty() || purpose.isEmpty()) throw IllegalStateException("Payment details are incomplete.")

        try {
            amount.toBackendFormat()
        } catch (e: Exception) {
            throw IllegalStateException("Amount format is incorrect.")
        }

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

    /**
     * State of the payment request
     */
    sealed class PaymentState {
        /**
         * Not performing any operation.
         */
        object NoAction : PaymentState()

        /**
         * Request is in progress.
         */
        object Loading : PaymentState()

        /**
         * Payment request was completed successfully
         *
         * @param paymentRequest - the payment request
         * @param paymentProviderName - the name of the payment provider with which the request was created
         */
        class Success(val paymentRequest: PaymentRequest, val paymentProviderName: String) : PaymentState()

        /**
         * Payment request returned an error.
         */
        class Error(val throwable: Throwable) : PaymentState()
    }

    /**
     * Different events that can be emitted by the MerchantSDK.
     */
    sealed class MerchantSDKEvents {
        object NoAction: MerchantSDKEvents()

        /**
         * Signal loading started.
         */
        object OnLoading: MerchantSDKEvents()

        /**
         * A change of screens within the [PaymentFragment].
         *
         * @param [displayedScreen] - the newly displayed screen. Can be observed to update the activity title.
         */
        class OnScreenDisplayed(val displayedScreen: DisplayedScreen): MerchantSDKEvents()

        /**
         * Payment request finished with success.
         *
         * @param [paymentRequestId] - the id of the payment request
         * @param [paymentProviderName] - the selected payment provider name
         */
        class OnFinishedWithPaymentRequestCreated(val paymentRequestId: String,
                                                  val paymentProviderName: String): MerchantSDKEvents()

        /**
         * Payment request was cancelled. Can be either from the server, or by cancelling the payment flow.
         */
        class OnFinishedWithCancellation(): MerchantSDKEvents()

        /**
         * An error occurred during the payment request.
         */
        class OnErrorOccurred(val throwable: Throwable): MerchantSDKEvents()
    }

    companion object {
        const val API_VERSION = 1

        private val LOG = LoggerFactory.getLogger(GiniMerchant::class.java)

        internal const val SHARE_WITH_INTENT_FILTER = "share_intent_filter"
        internal const val MERCHANT_BASE_URL = "https://merchant-api.gini.net/"
    }
}

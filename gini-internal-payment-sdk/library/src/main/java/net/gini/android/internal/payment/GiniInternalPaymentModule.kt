package net.gini.android.internal.payment

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.GiniHealthAPIBuilder
import net.gini.android.health.api.GiniHealthAPIBuilder.Companion.API_VERSION
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.api.model.PaymentRequest
import net.gini.android.internal.payment.api.model.ResultWrapper
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentprovider.PaymentProviderApp
import net.gini.android.internal.payment.utils.DisplayedScreen
import net.gini.android.internal.payment.utils.GiniPaymentManager
import net.gini.android.internal.payment.utils.PaymentEventListener


class GiniInternalPaymentModule(private val context: Context,
                                private val clientId: String = "",
                                private val clientSecret: String = "",
                                private val emailDomain: String = "",
                                private var sessionManager: SessionManager? = null,
                                private val baseUrl: String = "",
                                private val userCenterApiBaseUrl: String? = null,
                                private val debuggingEnabled: Boolean = false,) {

    constructor(
        context: Context,
        giniHealthAPI: GiniHealthAPI,
    ) : this(context) {
        this._giniHealthAPI = giniHealthAPI
    }

    // Lazily initialized backing field for giniHealthAPI to allow mocking it for tests.
    private var _giniHealthAPI: GiniHealthAPI? = null
    val giniHealthAPI: GiniHealthAPI
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
                        GiniHealthAPIBuilder(context, sessionManager = sessionManager, apiVersion = API_VERSION)
                    }.apply {
                        setApiBaseUrl(baseUrl)
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
    internal val giniPaymentManager = GiniPaymentManager(giniHealthAPI, object: PaymentEventListener {
        override fun onError(e: Exception) {
            TODO("Not yet implemented")
        }

        override fun onLoading() {
            TODO("Not yet implemented")
        }

        override fun onPaymentRequestCreated(
            paymentRequest: PaymentRequest,
            paymentProviderName: String
        ) {
            TODO("Not yet implemented")
        }

    })

    suspend fun getPaymentRequest(paymentProviderApp: PaymentProviderApp?, paymentDetails: PaymentDetails) = giniPaymentManager.getPaymentRequest(paymentProviderApp, paymentDetails)
    suspend fun onPayment(paymentProviderApp: PaymentProviderApp?, paymentDetails: PaymentDetails) = giniPaymentManager.onPayment(paymentProviderApp, paymentDetails)

    private val _paymentFlow =
        MutableStateFlow<ResultWrapper<PaymentDetails>>(ResultWrapper.Loading())

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

    private val _eventsFlow: MutableSharedFlow<InternalPaymentEvents> =
        MutableSharedFlow(extraBufferCapacity = 1)

    val eventsFlow: SharedFlow<InternalPaymentEvents> = _eventsFlow

    fun emitSDKEvent(state: PaymentState) {
        when (state) {
            is PaymentState.Success -> _eventsFlow.tryEmit(
                InternalPaymentEvents.OnFinishedWithPaymentRequestCreated(
                    state.paymentRequest.id,
                    state.paymentProviderName
                )
            )

            is PaymentState.Error -> _eventsFlow.tryEmit(InternalPaymentEvents.OnErrorOccurred(state.throwable))
            PaymentState.Loading -> _eventsFlow.tryEmit(InternalPaymentEvents.OnLoading)
            PaymentState.NoAction -> _eventsFlow.tryEmit(InternalPaymentEvents.NoAction)
        }
    }

    suspend fun loadPaymentProviderApps() = paymentComponent.loadPaymentProviderApps()

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
    sealed class InternalPaymentEvents {
        object NoAction: InternalPaymentEvents()

        /**
         * Signal loading started.
         */
        object OnLoading: InternalPaymentEvents()

        /**
         * A change of screens within the [PaymentFragment].
         *
         * @param [displayedScreen] - the newly displayed screen. Can be observed to update the activity title.
         */
        class OnScreenDisplayed(val displayedScreen: DisplayedScreen): InternalPaymentEvents()

        /**
         * Payment request finished with success.
         *
         * @param [paymentRequestId] - the id of the payment request
         * @param [paymentProviderName] - the selected payment provider name
         */
        class OnFinishedWithPaymentRequestCreated(val paymentRequestId: String,
                                                  val paymentProviderName: String): InternalPaymentEvents()

        /**
         * Payment request was cancelled. Can be either from the server, or by cancelling the payment flow.
         */
        class OnFinishedWithCancellation(): InternalPaymentEvents()

        /**
         * An error occurred during the payment request.
         */
        class OnErrorOccurred(val throwable: Throwable): InternalPaymentEvents()
    }

}
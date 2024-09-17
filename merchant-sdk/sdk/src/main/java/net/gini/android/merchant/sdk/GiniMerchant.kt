package net.gini.android.merchant.sdk

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.api.model.PaymentRequest
import net.gini.android.merchant.sdk.api.authorization.HealthApiSessionManagerAdapter
import net.gini.android.merchant.sdk.api.authorization.SessionManager
import net.gini.android.merchant.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.merchant.sdk.integratedFlow.PaymentFragment
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
    private val debuggingEnabled: Boolean = true,
) {

    internal var giniInternalPaymentModule: GiniInternalPaymentModule = GiniInternalPaymentModule(
        context = context,
        clientId = clientId,
        clientSecret = clientSecret,
        emailDomain = emailDomain,
        sessionManager = sessionManager?.let { HealthApiSessionManagerAdapter(it) },
        baseUrl = MERCHANT_BASE_URL,
        userCenterApiBaseUrl = userCenterApiBaseUrl,
        debuggingEnabled = debuggingEnabled,
        apiVersion = API_VERSION
    )

    /**
     * A flow that exposes events from the Merchant SDK. You can collect this flow to be informed about events such as errors,
     * successful payment requests or which screen is being displayed.
     */

    private val _eventsFlow: MutableSharedFlow<MerchantSDKEvents> = MutableSharedFlow(extraBufferCapacity = 1)

    val eventsFlow: Flow<MerchantSDKEvents> = merge(_eventsFlow, giniInternalPaymentModule.eventsFlow.map { event -> mapInternalEvent(event) })

    private fun mapInternalEvent(event: GiniInternalPaymentModule.InternalPaymentEvents): MerchantSDKEvents = when (event) {
        GiniInternalPaymentModule.InternalPaymentEvents.NoAction -> MerchantSDKEvents.NoAction
        GiniInternalPaymentModule.InternalPaymentEvents.OnLoading -> MerchantSDKEvents.OnLoading
        GiniInternalPaymentModule.InternalPaymentEvents.OnCancelled -> MerchantSDKEvents.OnFinishedWithCancellation
        is GiniInternalPaymentModule.InternalPaymentEvents.OnScreenDisplayed -> MerchantSDKEvents.OnScreenDisplayed(DisplayedScreen.toDisplayedScreen(event.displayedScreen))
        is GiniInternalPaymentModule.InternalPaymentEvents.OnErrorOccurred -> MerchantSDKEvents.OnErrorOccurred(event.throwable)
        is GiniInternalPaymentModule.InternalPaymentEvents.OnFinishedWithPaymentRequestCreated -> MerchantSDKEvents.OnFinishedWithPaymentRequestCreated(event.paymentRequestId, event.paymentProviderName)
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
        if (iban.isEmpty() || recipient.isEmpty() || amount.isEmpty() || purpose.isEmpty()) {
            error("Payment details are incomplete.")
        }

        try {
            amount.toBackendFormat()
        } catch (e: NumberFormatException) {
            error("Amount format is incorrect.")
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
        giniInternalPaymentModule.setPaymentDetails(paymentDetails)
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
        _eventsFlow.tryEmit(MerchantSDKEvents.OnFinishedWithCancellation)
    }

    /**
     * Loads payment provider apps - can be used before starting the payment flow for faster loading
     */
    suspend fun loadPaymentProviderApps() = giniInternalPaymentModule.loadPaymentProviderApps()

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
        object OnFinishedWithCancellation: MerchantSDKEvents()

        /**
         * An error occurred during the payment request.
         */
        class OnErrorOccurred(val throwable: Throwable): MerchantSDKEvents()
    }

    companion object {
        const val API_VERSION = 1

        private val LOG = LoggerFactory.getLogger(GiniMerchant::class.java)

        internal const val MERCHANT_BASE_URL = "https://merchant-api.gini.net/"
    }
}

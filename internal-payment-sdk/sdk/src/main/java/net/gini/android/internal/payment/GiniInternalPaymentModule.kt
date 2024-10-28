package net.gini.android.internal.payment

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.GiniHealthAPIBuilder
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.api.model.PaymentRequest
import net.gini.android.internal.payment.api.model.ResultWrapper
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.review.openWith.OpenWithPreferences
import net.gini.android.internal.payment.utils.DisplayedScreen
import net.gini.android.internal.payment.utils.GiniLocalization
import net.gini.android.internal.payment.utils.GiniPaymentManager
import net.gini.android.internal.payment.utils.PaymentEventListener

class GiniInternalPaymentModule(private val context: Context,
                                private val clientId: String = "",
                                private val clientSecret: String = "",
                                private val emailDomain: String = "",
                                private val sessionManager: SessionManager? = null,
                                private val baseUrl: String = "",
                                private val userCenterApiBaseUrl: String? = null,
                                private val debuggingEnabled: Boolean = false,
                                private val apiVersion: Int = DEFAULT_API_VERSION,
                                ) {

    constructor(
        context: Context,
        giniHealthAPI: GiniHealthAPI,
    ) : this(context) {
        _giniHealthAPI = giniHealthAPI
    }

    internal val giniPaymentManager: GiniPaymentManager
        get() {
            _giniPaymentManager?.let { return it }
                ?: return GiniPaymentManager(this.giniHealthAPI, object: PaymentEventListener {
                override fun onError(e: Exception) {
                    _eventsFlow.tryEmit(InternalPaymentEvents.OnErrorOccurred(e))
                }

                override fun onLoading() {
                    _eventsFlow.tryEmit(InternalPaymentEvents.OnLoading)
                }

                override fun onPaymentRequestCreated(
                    paymentRequest: PaymentRequest,
                    paymentProviderName: String
                ) {
                    _eventsFlow.tryEmit(InternalPaymentEvents.OnFinishedWithPaymentRequestCreated(paymentRequest.id, paymentProviderName))
                }
            }).also {
                _giniPaymentManager = it
            }
        }

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
                            apiVersion = apiVersion
                        )
                    } else {
                        GiniHealthAPIBuilder(context, sessionManager = sessionManager, apiVersion = apiVersion)
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

    var paymentComponent = PaymentComponent(context, this)
    private val openWithPreferences = OpenWithPreferences(context)

    private val _paymentFlow =
        MutableStateFlow<ResultWrapper<PaymentDetails>>(ResultWrapper.Loading())
    private var _giniHealthAPI: GiniHealthAPI? = null
    private var _giniPaymentManager: GiniPaymentManager? = null

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

    /**
     * A flow that exposes events from the Merchant SDK. You can collect this flow to be informed about events such as errors,
     * successful payment requests or which screen is being displayed.
     */

    private val _eventsFlow: MutableSharedFlow<InternalPaymentEvents> =
        MutableSharedFlow(extraBufferCapacity = 1)

    val eventsFlow: SharedFlow<InternalPaymentEvents> = _eventsFlow

    suspend fun getPaymentRequest(paymentProviderApp: PaymentProviderApp?, paymentDetails: PaymentDetails?) = giniPaymentManager.getPaymentRequest(paymentProviderApp, paymentDetails)
    suspend fun onPayment(paymentProviderApp: PaymentProviderApp?, paymentDetails: PaymentDetails) = giniPaymentManager.onPayment(paymentProviderApp, paymentDetails)
    suspend fun loadPaymentProviderApps() = paymentComponent.loadPaymentProviderApps()

    fun setPaymentDetails(paymentDetails: PaymentDetails?) {
        _paymentFlow.value = if (paymentDetails != null) {
            ResultWrapper.Success(paymentDetails)
        } else {
            ResultWrapper.Loading()
        }
    }

    suspend fun incrementCountForPaymentProviderId(paymentProviderAppId: String) {
        openWithPreferences.incrementCountForPaymentProviderId(paymentProviderAppId)
    }

    fun getLiveCountForPaymentProviderId(paymentProviderAppId: String) = openWithPreferences.getLiveCountForPaymentProviderId(paymentProviderAppId)

    fun emitSdkEvent(event: InternalPaymentEvents) {
        _eventsFlow.tryEmit(event)
    }

    fun setSDKLanguage(language: GiniLocalization?, context: Context) {
        localizedContext = null
        GiniPaymentPreferences(context).saveSDKLanguage(language)
    }

    fun saveReturningUser() {
        GiniPaymentPreferences(context).saveReturningUser()
    }

    fun getReturningUser() = GiniPaymentPreferences(context).getReturningUser()

    internal class GiniPaymentPreferences(context: Context) {
        private val sharedPreferences = context.getSharedPreferences("GiniPaymentPreferences", Context.MODE_PRIVATE)

        fun saveSDKLanguage(value: GiniLocalization?) {
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putString(SDK_LANGUAGE_PREFS_KEY, value?.readableName?.uppercase())
            editor.apply()
        }

        fun getSDKLanguage(): GiniLocalization? {
            val enumValue = sharedPreferences.getString(SDK_LANGUAGE_PREFS_KEY, null)
            return if (enumValue.isNullOrEmpty()) null else GiniLocalization.valueOf(enumValue)
        }

        fun saveReturningUser() {
            val editor = sharedPreferences.edit()
            editor.putBoolean(RETURNING_USER_PREFS_KEY, true)
            editor.apply()
        }

        fun getReturningUser(): Boolean = sharedPreferences.getBoolean(RETURNING_USER_PREFS_KEY, false)
    }

    var localizedContext: Context? = null

    companion object {
        private const val SDK_LANGUAGE_PREFS_KEY = "SDK_LANGUAGE_PREFS_KEY"
        private const val RETURNING_USER_PREFS_KEY = "RETURNING_USER_PREFS_KEY"
        private const val DEFAULT_API_VERSION = 1
        const val SHARE_WITH_INTENT_FILTER = "share_intent_filter"

        fun getSDKLanguage(context: Context): GiniLocalization? {
            return GiniPaymentPreferences(context).getSDKLanguage()
        }
    }

    /**
     * Different events that can be emitted by the GiniInternalPaymentModule.
     */
    sealed class InternalPaymentEvents {
        object NoAction : InternalPaymentEvents()

        /**
         * Signal loading started.
         */
        object OnLoading : InternalPaymentEvents()

        /**
         * Payment flow was cancelled.
         */
        object OnCancelled : InternalPaymentEvents()

        /**
         * A change of screens within the [PaymentFragment].
         *
         * @param [displayedScreen] - the newly displayed screen. Can be observed to update the activity title.
         */
        class OnScreenDisplayed(val displayedScreen: DisplayedScreen) : InternalPaymentEvents()

        /**
         * Payment request finished with success.
         *
         * @param [paymentRequestId] - the id of the payment request
         * @param [paymentProviderName] - the selected payment provider name
         */
        class OnFinishedWithPaymentRequestCreated(
            val paymentRequestId: String,
            val paymentProviderName: String
        ) : InternalPaymentEvents()

        /**
         * An error occurred during the payment request.
         */
        class OnErrorOccurred(val throwable: Throwable) : InternalPaymentEvents()
    }
}

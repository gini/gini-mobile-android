package net.gini.android.merchant.sdk.integratedFlow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import net.gini.android.core.api.Resource
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.api.ResultWrapper
import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.api.payment.model.PaymentRequest
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.review.openWith.OpenWithPreferences
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.DisplayedScreen
import net.gini.android.merchant.sdk.util.FlowBottomSheetsManager
import net.gini.android.merchant.sdk.util.GiniPaymentManager
import net.gini.android.merchant.sdk.util.PaymentNextStep
import java.io.File
import java.util.Stack

internal class PaymentFlowViewModel(val paymentComponent: PaymentComponent, val documentId: String, val paymentFlowConfiguration: PaymentFlowConfiguration?, val giniPaymentManager: GiniPaymentManager, val giniMerchant: GiniMerchant) : ViewModel(), FlowBottomSheetsManager, BackListener {

    private val backstack: Stack<DisplayedScreen> = Stack<DisplayedScreen>().also { it.add(DisplayedScreen.Nothing) }
    private var initialSelectedPaymentProvider: PaymentProviderApp? = null

    private val _paymentDetails = MutableStateFlow(PaymentDetails("", "", "", ""))

    override var openWithPreferences: OpenWithPreferences? = null
    override var openWithCounter: Int = 0
    override val paymentNextStepFlow = MutableSharedFlow<PaymentNextStep>(
        extraBufferCapacity = 1,
    )
    override val paymentRequestFlow: MutableStateFlow<PaymentRequest?> = MutableStateFlow(null)

    val paymentNextStep: SharedFlow<PaymentNextStep> = paymentNextStepFlow

    private val _backButtonEvent = MutableSharedFlow<Void?>(extraBufferCapacity = 1)
    val backButtonEvent: SharedFlow<Void?> = _backButtonEvent

    init {
        viewModelScope.launch {
            giniMerchant.paymentFlow.collect { paymentResult ->
                if (paymentResult is ResultWrapper.Success) {
                    _paymentDetails.tryEmit(paymentResult.value)
                }
            }
        }

        viewModelScope.launch {
            _paymentDetails.collect {paymentDetails ->
                giniMerchant.setDocumentForReview(documentId, paymentDetails)
            }
        }

        viewModelScope.launch {
            paymentComponent.paymentProviderAppsFlow.collect {
                if (it is PaymentProviderAppsState.Error) {
                    giniMerchant.emitSDKEvent(GiniMerchant.PaymentState.Error(it.throwable))
                    delay(50)
                    giniMerchant.setFlowCancelled()
                }
            }
        }
    }

    fun loadPaymentProviderApps() = viewModelScope.launch {
        if (paymentComponent.paymentProviderAppsFlow.value is PaymentProviderAppsState.Loading || paymentComponent.paymentProviderAppsFlow.value is PaymentProviderAppsState.Success) return@launch
        paymentComponent.loadPaymentProviderApps()
    }

    fun addToBackStack(destination: DisplayedScreen) {
        backstack.add(destination)
        setDisplayedScreen()
    }

    fun popBackStack() {
        backstack.pop()
        setDisplayedScreen()
    }

    fun getLastBackstackEntry() = if (backstack.isNotEmpty()) backstack.peek() else DisplayedScreen.Nothing

    private fun setDisplayedScreen() = viewModelScope.launch {
        giniMerchant.setDisplayedScreen(getLastBackstackEntry())
        if (getLastBackstackEntry() is DisplayedScreen.Nothing) {
            giniMerchant.setDisplayedScreen(null)
        }
    }

    fun paymentProviderAppChanged(paymentProviderApp: PaymentProviderApp): Boolean {
        if (initialSelectedPaymentProvider == null) {
            observeOpenWithCount(paymentProviderApp.paymentProvider.id)
        }
        if (initialSelectedPaymentProvider?.paymentProvider?.id != paymentProviderApp.paymentProvider.id) {
            initialSelectedPaymentProvider = paymentProviderApp
            observeOpenWithCount(paymentProviderApp.paymentProvider.id)
            return true
        }
        return false
    }

    fun checkBankAppInstallState(paymentProviderApp: PaymentProviderApp) {
        if (paymentProviderApp.isInstalled() != initialSelectedPaymentProvider?.isInstalled()) {
            initialSelectedPaymentProvider = paymentProviderApp
        }
    }

    fun onPayment() = viewModelScope.launch {
        giniPaymentManager.onPayment(initialSelectedPaymentProvider, _paymentDetails.value)
    }

    fun loadPaymentDetails() = viewModelScope.launch {
        giniMerchant.setDocumentForReview(documentId)
    }

    fun onBankOpened() {
        // Schedule on the main dispatcher to allow all collectors to receive the current state before
        // the state is overridden
        viewModelScope.launch(Dispatchers.Main) {
            giniMerchant.setDisplayedScreen(null)
            giniMerchant.emitSDKEvent(GiniMerchant.PaymentState.NoAction)
        }
    }

    fun getPaymentProviderApp() = initialSelectedPaymentProvider

    private fun observeOpenWithCount(paymentProviderAppId: String) {
        startObservingOpenWithCount(viewModelScope, paymentProviderAppId)
    }

    fun emitFinishEvent() {
        paymentRequestFlow.value?.let {
            giniMerchant.emitSDKEvent(GiniMerchant.PaymentState.Success(it, initialSelectedPaymentProvider?.name ?: ""))
        }
    }

    fun onPaymentButtonTapped(externalCacheDir: File?) {
        checkNextStep(initialSelectedPaymentProvider, externalCacheDir, viewModelScope)
    }

    fun onForwardToSharePdfTapped(externalCacheDir: File?) {
        sharePdf(initialSelectedPaymentProvider, externalCacheDir, viewModelScope)
    }

    override fun emitSDKEvent(sdkEvent: GiniMerchant.PaymentState) {
        giniMerchant.emitSDKEvent(sdkEvent)
    }

    override fun sendFeedback() {
        viewModelScope.launch {
            giniPaymentManager.sendFeedbackAndStartLoading(_paymentDetails.value)
        }
    }

    override suspend fun getPaymentRequest(): PaymentRequest = giniPaymentManager.getPaymentRequest(initialSelectedPaymentProvider, _paymentDetails.value)

    override suspend fun getPaymentRequestDocument(paymentRequest: PaymentRequest): Resource<ByteArray> = giniMerchant.giniHealthAPI.documentManager.getPaymentRequestDocument(paymentRequest.id)

    class Factory(val paymentComponent: PaymentComponent, val documentId: String, private val paymentFlowConfiguration: PaymentFlowConfiguration?, val giniMerchant: GiniMerchant): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaymentFlowViewModel(paymentComponent = paymentComponent, documentId = documentId, paymentFlowConfiguration = paymentFlowConfiguration, giniPaymentManager = GiniPaymentManager(giniMerchant), giniMerchant = giniMerchant) as T
        }
    }

    override fun backCalled() {
        _backButtonEvent.tryEmit(null)
    }
}
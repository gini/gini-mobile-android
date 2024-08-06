package net.gini.android.merchant.sdk.integratedFlow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import net.gini.android.core.api.Resource
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.api.payment.model.Payment
import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.api.payment.model.PaymentRequest
import net.gini.android.merchant.sdk.paymentcomponent.BankPickerRows
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

internal class PaymentFlowViewModel(
    val paymentComponent: PaymentComponent,
    internal var paymentDetails: PaymentDetails,
    val paymentFlowConfiguration: PaymentFlowConfiguration?,
    val giniPaymentManager: GiniPaymentManager,
    val giniMerchant: GiniMerchant
) : ViewModel(), FlowBottomSheetsManager, BackListener {

    private val backstack: Stack<DisplayedScreen> = Stack<DisplayedScreen>().also { it.add(DisplayedScreen.Nothing) }
    private var initialSelectedPaymentProvider: PaymentProviderApp? = null

    override var openWithPreferences: OpenWithPreferences? = null
    override var openWithCounter: Int = 0
    override val paymentNextStepFlow = MutableSharedFlow<PaymentNextStep>(
        extraBufferCapacity = 1,
    )
    override val paymentRequestFlow: MutableStateFlow<PaymentRequest?> = MutableStateFlow(null)

    val paymentNextStep: SharedFlow<PaymentNextStep> = paymentNextStepFlow

    private val _backButtonEvent = MutableSharedFlow<Void?>(extraBufferCapacity = 1)
    val backButtonEvent: SharedFlow<Void?> = _backButtonEvent

    override val shareWithFlowStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            paymentComponent.paymentProviderAppsFlow.collect {
                if (it is PaymentProviderAppsState.Error) {
                    giniMerchant.emitSDKEvent(GiniMerchant.PaymentState.Error(it.throwable))
                    delay(50)
                    giniMerchant.setFlowCancelled()
                }
            }
        }
        paymentFlowConfiguration?.let {
            paymentComponent.shouldCheckReturningUser = it.checkForReturningUser
            paymentComponent.bankPickerRows = if (it.paymentComponentOnTwoRows) BankPickerRows.TWO else BankPickerRows.SINGLE
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

    fun updatePaymentDetails(paymentDetails: PaymentDetails) {
        this.paymentDetails = paymentDetails
    }

    fun onPayment() = viewModelScope.launch {
        giniPaymentManager.onPayment(initialSelectedPaymentProvider, paymentDetails)
    }

    fun onBankOpened() {
        // Schedule on the main dispatcher to allow all collectors to receive the current state before
        // the state is overridden
        giniMerchant.emitSDKEvent(GiniMerchant.PaymentState.NoAction)
    }

    fun getPaymentProviderApp() = initialSelectedPaymentProvider

    private fun observeOpenWithCount(paymentProviderAppId: String) {
        startObservingOpenWithCount(viewModelScope, paymentProviderAppId)
    }

    fun emitShareWithStartedEvent() {
        paymentRequestFlow.value?.let {
            giniMerchant.emitSDKEvent(GiniMerchant.PaymentState.Success(it, initialSelectedPaymentProvider?.name ?: ""))
        }
        shareWithFlowStarted.tryEmit(true)
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

    override suspend fun getPaymentRequest(): PaymentRequest = giniPaymentManager.getPaymentRequest(initialSelectedPaymentProvider, paymentDetails)

    override suspend fun getPaymentRequestDocument(paymentRequest: PaymentRequest): Resource<ByteArray> = giniMerchant.giniHealthAPI.documentManager.getPaymentRequestDocument(paymentRequest.id)

    class Factory(val paymentComponent: PaymentComponent, val paymentDetails: PaymentDetails, private val paymentFlowConfiguration: PaymentFlowConfiguration?, val giniMerchant: GiniMerchant): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaymentFlowViewModel(paymentComponent = paymentComponent, paymentDetails = paymentDetails,paymentFlowConfiguration = paymentFlowConfiguration, giniPaymentManager = GiniPaymentManager(giniMerchant), giniMerchant = giniMerchant) as T
        }
    }

    override fun backCalled() {
        _backButtonEvent.tryEmit(null)
    }
}
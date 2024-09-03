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
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.api.model.PaymentRequest
import net.gini.android.internal.payment.paymentComponent.BankPickerRows
import net.gini.android.internal.payment.paymentComponent.PaymentProviderAppsState
import net.gini.android.internal.payment.paymentprovider.PaymentProviderApp
import net.gini.android.internal.payment.utils.BackListener
import net.gini.android.internal.payment.utils.FlowBottomSheetsManager
import net.gini.android.internal.payment.utils.PaymentNextStep
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.util.DisplayedScreen
import java.io.File
import java.util.Stack

internal class PaymentFlowViewModel(
    internal var paymentDetails: PaymentDetails,
    val paymentFlowConfiguration: PaymentFlowConfiguration?,
    val giniMerchant: GiniMerchant,
) : ViewModel(), FlowBottomSheetsManager, BackListener {

    private val backstack: Stack<DisplayedScreen> = Stack<DisplayedScreen>().also { it.add(DisplayedScreen.Nothing) }
    private var initialSelectedPaymentProvider: PaymentProviderApp? = null

    override var openWithCounter: Int = 0
    override val paymentNextStepFlow = MutableSharedFlow<PaymentNextStep>(
        extraBufferCapacity = 1,
    )
    override val paymentRequestFlow: MutableStateFlow<PaymentRequest?> = MutableStateFlow(null)
    override val giniInternalPaymentModule = giniMerchant.giniInternalPaymentModule

    val paymentComponent = giniInternalPaymentModule.paymentComponent

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
        giniMerchant.giniInternalPaymentModule.onPayment(initialSelectedPaymentProvider, paymentDetails)
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

    override suspend fun getPaymentRequest(): PaymentRequest = giniMerchant.giniInternalPaymentModule.getPaymentRequest(initialSelectedPaymentProvider, paymentDetails)

    override suspend fun getPaymentRequestDocument(paymentRequest: PaymentRequest): Resource<ByteArray> = giniMerchant.giniInternalPaymentModule.giniHealthAPI.documentManager.getPaymentRequestDocument(paymentRequest.id)

    class Factory(val paymentDetails: PaymentDetails, private val paymentFlowConfiguration: PaymentFlowConfiguration?, val giniMerchant: GiniMerchant): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaymentFlowViewModel(paymentDetails = paymentDetails,paymentFlowConfiguration = paymentFlowConfiguration, giniMerchant = giniMerchant) as T
        }
    }

    override fun backCalled() {
        _backButtonEvent.tryEmit(null)
    }
}
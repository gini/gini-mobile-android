package net.gini.android.merchant.sdk.integratedFlow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.review.model.PaymentDetails
import net.gini.android.merchant.sdk.review.model.ResultWrapper
import net.gini.android.merchant.sdk.util.DisplayedScreen
import net.gini.android.merchant.sdk.util.GiniPaymentManager
import java.util.Stack

internal class IntegratedPaymentContainerViewModel(val paymentComponent: PaymentComponent?, val documentId: String, val integratedFlowConfiguration: IntegratedFlowConfiguration?, val giniPaymentManager: GiniPaymentManager, val giniMerchant: GiniMerchant?) : ViewModel() {

    private val backstack: Stack<DisplayedScreen> = Stack<DisplayedScreen>().also { it.add(DisplayedScreen.Nothing) }
    private var initialSelectedPaymentProvider: PaymentProviderApp? = null

    private val _paymentDetails = MutableStateFlow(PaymentDetails("", "", "", ""))

    init {
        when (paymentComponent?.selectedPaymentProviderAppFlow?.value) {
            is SelectedPaymentProviderAppState.AppSelected -> {
                initialSelectedPaymentProvider = (paymentComponent.selectedPaymentProviderAppFlow.value as SelectedPaymentProviderAppState.AppSelected) .paymentProviderApp
            }
            else -> {}
        }

        viewModelScope.launch {
            giniMerchant?.paymentFlow?.collect { paymentResult ->
                if (paymentResult is ResultWrapper.Success) {
                    _paymentDetails.tryEmit(paymentResult.value)
                }
            }
        }

        viewModelScope.launch {
            _paymentDetails.collect {paymentDetails ->
                giniMerchant?.setDocumentForReview(documentId, paymentDetails)
            }
        }
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

    fun setDisplayedScreen() {
        giniMerchant?.setDisplayedScreen(getLastBackstackEntry())
    }

    fun paymentProviderAppChanged(paymentProviderApp: PaymentProviderApp): Boolean {
        if (initialSelectedPaymentProvider?.paymentProvider?.id != paymentProviderApp.paymentProvider.id) {
            initialSelectedPaymentProvider = paymentProviderApp
            return true
        }
        return false
    }

    fun onPayment() = viewModelScope.launch {
        giniPaymentManager.onPayment(initialSelectedPaymentProvider, _paymentDetails.value)
    }

    fun loadPaymentDetails() = viewModelScope.launch {
        giniMerchant?.setDocumentForReview(documentId)
    }

    fun onBankOpened() {
        // Schedule on the main dispatcher to allow all collectors to receive the current state before
        // the state is overridden
        viewModelScope.launch(Dispatchers.Main) {
            giniMerchant?.emitSDKEvent(GiniMerchant.PaymentState.NoAction)
        }
    }

    fun getPaymentProviderApp() = initialSelectedPaymentProvider

    class Factory(val paymentComponent: PaymentComponent?, val documentId: String, val integratedFlowConfiguration: IntegratedFlowConfiguration?, val giniMerchant: GiniMerchant?): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IntegratedPaymentContainerViewModel(paymentComponent = paymentComponent, documentId = documentId, integratedFlowConfiguration = integratedFlowConfiguration, giniPaymentManager = GiniPaymentManager(giniMerchant), giniMerchant = giniMerchant) as T
        }
    }
}
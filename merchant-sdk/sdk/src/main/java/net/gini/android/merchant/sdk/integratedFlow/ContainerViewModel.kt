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
import net.gini.android.merchant.sdk.util.GiniPayment
import java.util.Stack


internal class ContainerViewModel(val paymentComponent: PaymentComponent?, val documentId: String, val flowConfiguration: FlowConfiguration?, val giniPayment: GiniPayment, val giniMerchant: GiniMerchant?) : ViewModel() {

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
    }

    fun popBackStack() {
        backstack.pop()
    }

    fun getLastBackstackEntry() = backstack.peek()

    fun paymentProviderAppChanged(paymentProviderApp: PaymentProviderApp): Boolean {
        if (initialSelectedPaymentProvider?.paymentProvider?.id != paymentProviderApp.paymentProvider.id) {
            initialSelectedPaymentProvider = paymentProviderApp
            return true
        }
        return false
    }

    fun onPayment() = viewModelScope.launch {
        giniPayment.onPayment(initialSelectedPaymentProvider, _paymentDetails.value)
    }

    fun loadPaymentDetails() = viewModelScope.launch {
        giniMerchant?.setDocumentForReview(documentId)
    }

    fun onBankOpened() {
        // Schedule on the main dispatcher to allow all collectors to receive the current state before
        // the state is overridden
        viewModelScope.launch(Dispatchers.Main) {
            giniMerchant?.setOpenBankState(GiniMerchant.PaymentState.NoAction)
        }
    }

    sealed class DisplayedScreen {
        object Nothing: DisplayedScreen()
        object PaymentComponentBottomSheet : DisplayedScreen()
        object BankSelectionBottomSheet: DisplayedScreen()
        object MoreInformationFragment: DisplayedScreen()
        object ReviewFragment: DisplayedScreen()
    }

    class Factory(val paymentComponent: PaymentComponent?, val documentId: String, val flowConfiguration: FlowConfiguration?, val giniMerchant: GiniMerchant?): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContainerViewModel(paymentComponent = paymentComponent, documentId = documentId, flowConfiguration = flowConfiguration, giniPayment = GiniPayment(giniMerchant), giniMerchant = giniMerchant) as T
        }
    }
}
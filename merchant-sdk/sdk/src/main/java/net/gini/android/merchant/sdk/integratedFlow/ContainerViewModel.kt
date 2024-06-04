package net.gini.android.merchant.sdk.integratedFlow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import java.util.Stack


internal class ContainerViewModel(private val paymentComponent: PaymentComponent?) : ViewModel() {
    private val _paymentComponent = MutableStateFlow<PaymentComponent?>(null)

    private val _backstack: Stack<DisplayedScreen> = Stack<DisplayedScreen>().also { it.add(DisplayedScreen.Nothing) }
    private var _initialSelectedPaymentProvider: PaymentProviderApp? = null

    init {
        _paymentComponent.value = paymentComponent
        when (paymentComponent?.selectedPaymentProviderAppFlow?.value) {
            is SelectedPaymentProviderAppState.AppSelected -> {
                _initialSelectedPaymentProvider = (paymentComponent.selectedPaymentProviderAppFlow.value as SelectedPaymentProviderAppState.AppSelected) .paymentProviderApp
            }
            else -> {}
        }
    }

    fun addToBackStack(destination: DisplayedScreen) {
        _backstack.add(destination)
    }

    fun popBackStack() {
        _backstack.pop()
    }

    fun getLastBackstackEntry() = _backstack.peek()

    fun paymentProviderAppChanged(paymentProviderApp: PaymentProviderApp): Boolean {
        if (_initialSelectedPaymentProvider?.paymentProvider?.id != paymentProviderApp.paymentProvider.id) {
            _initialSelectedPaymentProvider = paymentProviderApp
            return true
        }
        return false
    }

    sealed class DisplayedScreen {
        object Nothing: DisplayedScreen()
        object PaymentComponentBottomSheet : DisplayedScreen()
        object BankSelectionBottomSheet: DisplayedScreen()
        object MoreInformationFragment: DisplayedScreen()
        object ReviewFragment: DisplayedScreen()
    }

    class Factory(val paymentComponent: PaymentComponent?): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContainerViewModel(paymentComponent = paymentComponent) as T
        }
    }
}
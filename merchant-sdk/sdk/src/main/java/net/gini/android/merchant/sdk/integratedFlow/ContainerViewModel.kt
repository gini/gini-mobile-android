package net.gini.android.merchant.sdk.integratedFlow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import java.util.Stack


internal class ContainerViewModel(val paymentComponent: PaymentComponent?) : ViewModel() {

    private val backstack: Stack<DisplayedScreen> = Stack<DisplayedScreen>().also { it.add(DisplayedScreen.Nothing) }
    private var initialSelectedPaymentProvider: PaymentProviderApp? = null

    init {
        when (paymentComponent?.selectedPaymentProviderAppFlow?.value) {
            is SelectedPaymentProviderAppState.AppSelected -> {
                initialSelectedPaymentProvider = (paymentComponent.selectedPaymentProviderAppFlow.value as SelectedPaymentProviderAppState.AppSelected) .paymentProviderApp
            }
            else -> {}
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
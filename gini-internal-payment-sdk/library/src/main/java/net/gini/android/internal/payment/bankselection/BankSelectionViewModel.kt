package net.gini.android.internal.payment.bankselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.PaymentProviderAppsState
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentprovider.PaymentProviderApp
import net.gini.android.internal.payment.utils.BackListener
import org.slf4j.LoggerFactory

internal class BankSelectionViewModel(val paymentComponent: PaymentComponent?, val backListener: BackListener?) : ViewModel() {

    private val _paymentProviderAppsListFlow =
        MutableStateFlow<PaymentProviderAppsListState>(PaymentProviderAppsListState.Loading)
    val paymentProviderAppsListFlow: StateFlow<PaymentProviderAppsListState> =
        _paymentProviderAppsListFlow.asStateFlow()

    fun start() {
        viewModelScope.launch {
            if (paymentComponent == null) {
                LOG.warn("Cannot show payment provider apps: PaymentComponent must be set before showing the BankSelectionBottomSheet")
                return@launch
            }
            LOG.debug("Collecting payment provider apps state and selected payment provider app from PaymentComponent")

            paymentComponent.selectedPaymentProviderAppFlow.combine(paymentComponent.paymentProviderAppsFlow) { selectedPaymentProviderAppState, paymentProviderAppsState ->
                selectedPaymentProviderAppState to paymentProviderAppsState
            }.collect { (selectedPaymentProviderAppState, paymentProviderAppsState) ->
                LOG.debug(
                    "Received selected payment provider app state: {}",
                    selectedPaymentProviderAppState
                )
                LOG.debug("Received payment provider apps state: {}", paymentProviderAppsState)

                if (paymentProviderAppsState is PaymentProviderAppsState.Success) {
                    val paymentProviderAppsList = mutableListOf<PaymentProviderAppListItem>()

                    if (paymentProviderAppsState.paymentProviderApps.isNotEmpty()) {
                        LOG.debug(
                            "Received {} payment provider apps",
                            paymentProviderAppsState.paymentProviderApps.size
                        )
                        paymentProviderAppsList += paymentProviderAppsState.paymentProviderApps.map {
                            PaymentProviderAppListItem(it, false)
                        }
                    } else {
                        LOG.debug("No payment provider apps received")
                    }

                    when (selectedPaymentProviderAppState) {
                        is SelectedPaymentProviderAppState.AppSelected -> {
                            LOG.debug(
                                "Selected payment provider app: {}",
                                selectedPaymentProviderAppState.paymentProviderApp.name
                            )
                            paymentProviderAppsList.firstOrNull { paymentProviderApp ->
                                hasSamePaymentProviderId(paymentProviderApp.paymentProviderApp, selectedPaymentProviderAppState.paymentProviderApp)
                            }?.isSelected = true
                        }

                        SelectedPaymentProviderAppState.NothingSelected -> {
                            LOG.debug("No payment provider app selected")
                            paymentProviderAppsList.forEach { it.isSelected = false }
                        }
                    }

                    _paymentProviderAppsListFlow.value =
                        PaymentProviderAppsListState.Success(paymentProviderAppsList)
                } else if (paymentProviderAppsState is PaymentProviderAppsState.Error) {
                    LOG.error("Error loading payment provider apps", paymentProviderAppsState.throwable)
                    _paymentProviderAppsListFlow.value =
                        PaymentProviderAppsListState.Error(paymentProviderAppsState.throwable)
                }
            }
        }
    }

    private fun hasSamePaymentProviderId(
        paymentProviderApp: PaymentProviderApp,
        selectedPaymentProviderApp: PaymentProviderApp
    ): Boolean =
        paymentProviderApp.hasSamePaymentProviderId(selectedPaymentProviderApp)

    fun recheckWhichPaymentProviderAppsAreInstalled() {
        viewModelScope.launch {
            paymentComponent?.recheckWhichPaymentProviderAppsAreInstalled()
        }
    }

    fun setSelectedPaymentProviderApp(paymentProviderApp: PaymentProviderApp) {
        viewModelScope.launch {
            paymentComponent?.setSelectedPaymentProviderApp(paymentProviderApp)
        }
    }

    class Factory(private val paymentComponent: PaymentComponent?, private val backListener: BackListener? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BankSelectionViewModel(paymentComponent, backListener) as T
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BankSelectionViewModel::class.java)
    }
}

internal sealed class PaymentProviderAppsListState {
    object Loading : PaymentProviderAppsListState()
    class Success(val paymentProviderAppsList: List<PaymentProviderAppListItem>) : PaymentProviderAppsListState()
    class Error(val throwable: Throwable) : PaymentProviderAppsListState()
}

internal data class PaymentProviderAppListItem(val paymentProviderApp: PaymentProviderApp, var isSelected: Boolean)

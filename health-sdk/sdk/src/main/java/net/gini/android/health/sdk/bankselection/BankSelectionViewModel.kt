package net.gini.android.health.sdk.bankselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.health.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import org.slf4j.LoggerFactory

internal class BankSelectionViewModel(val paymentComponent: PaymentComponent?) : ViewModel() {

    private val _paymentProviderAppsListFlow =
        MutableStateFlow<PaymentProviderAppsListState>(PaymentProviderAppsListState.Loading)
    val paymentProviderAppsListFlow = _paymentProviderAppsListFlow

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
                            paymentProviderAppsList.firstOrNull {
                                it.paymentProviderApp == selectedPaymentProviderAppState.paymentProviderApp
                            }?.isSelected = true
                        }

                        SelectedPaymentProviderAppState.NothingSelected -> {
                            LOG.debug("No payment provider app selected")
                            paymentProviderAppsList.forEach { it.isSelected = false }
                        }
                    }

                    _paymentProviderAppsListFlow.value = PaymentProviderAppsListState.Success(paymentProviderAppsList)
                } else if (paymentProviderAppsState is PaymentProviderAppsState.Error) {
                    LOG.error("Error loading payment provider apps", paymentProviderAppsState.throwable)
                    _paymentProviderAppsListFlow.value =
                        PaymentProviderAppsListState.Error(paymentProviderAppsState.throwable)
                }
            }
        }
    }

    class Factory(private val paymentComponent: PaymentComponent?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BankSelectionViewModel(paymentComponent) as T
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

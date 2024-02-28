package net.gini.android.health.sdk.moreinformation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import org.slf4j.LoggerFactory


internal class MoreInformationViewModel(private val paymentComponent: PaymentComponent?) : ViewModel() {

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
            paymentComponent.paymentProviderAppsFlow.collect { paymentProviderAppsState ->
                when (paymentProviderAppsState) {
                    is PaymentProviderAppsState.Error -> { processError(paymentProviderAppsState.throwable) }
                    PaymentProviderAppsState.Loading -> {}
                    is PaymentProviderAppsState.Success -> {
                        if (paymentProviderAppsState.paymentProviderApps.isEmpty()) {
                            LOG.debug("No payment provider apps received")
                        } else {
                            LOG.debug("Received {} payment provider apps", paymentProviderAppsState.paymentProviderApps.size)
                        }
                        _paymentProviderAppsListFlow.value = PaymentProviderAppsListState.Success(paymentProviderAppsState.paymentProviderApps)
                    }
                }
            }
        }
    }

    private fun processError(throwable: Throwable) {
        LOG.error("Error loading payment provider apps", throwable)
        _paymentProviderAppsListFlow.value = PaymentProviderAppsListState.Error(throwable)
    }

    class Factory(private val paymentComponent: PaymentComponent?): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MoreInformationViewModel(paymentComponent) as T
        }
    }

    internal sealed class PaymentProviderAppsListState {
        object Loading : PaymentProviderAppsListState()
        class Success(val paymentProviderAppsList: List<PaymentProviderApp>) : PaymentProviderAppsListState()
        class Error(val throwable: Throwable) : PaymentProviderAppsListState()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MoreInformationViewModel::class.java)
    }

}
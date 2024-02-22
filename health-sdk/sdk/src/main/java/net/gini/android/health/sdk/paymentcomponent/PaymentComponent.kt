package net.gini.android.health.sdk.paymentcomponent

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.core.api.Resource
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.paymentprovider.getPaymentProviderApps
import org.slf4j.LoggerFactory

class PaymentComponent(private val context: Context, private val giniHealth: GiniHealth) {

    private val _paymentProviderAppsFlow = MutableStateFlow<PaymentProviderAppsState>(PaymentProviderAppsState.Loading)
    val paymentProviderAppsFlow: StateFlow<PaymentProviderAppsState> = _paymentProviderAppsFlow

    private val _selectedPaymentProviderAppFlow =
        MutableStateFlow<SelectedPaymentProviderAppState>(SelectedPaymentProviderAppState.NothingSelected)
    val selectedPaymentProviderAppFlow: StateFlow<SelectedPaymentProviderAppState> = _selectedPaymentProviderAppFlow

    var listener: Listener? = null

    suspend fun loadPaymentProviderApps() {
        LOG.debug("Loading payment providers")
        _paymentProviderAppsFlow.value = PaymentProviderAppsState.Loading
        _paymentProviderAppsFlow.value = try {
            when (val paymentProvidersResource = giniHealth.giniHealthAPI.documentManager.getPaymentProviders()) {
                is Resource.Cancelled -> {
                    LOG.debug("Loading payment providers cancelled")
                    PaymentProviderAppsState.Error(Exception("Cancelled"))
                }
                is Resource.Error -> {
                    LOG.error("Error loading payment providers", paymentProvidersResource.exception)
                    PaymentProviderAppsState.Error(
                        paymentProvidersResource.exception ?: Exception(
                            paymentProvidersResource.message
                        )
                    )
                }
                is Resource.Success -> {
                    LOG.debug("Loaded payment providers")
                    LOG.debug("Loading installed payment provider apps")
                    val paymentProviderApps = getPaymentProviderApps(paymentProvidersResource.data)

                    selectPaymentProviderApp(paymentProviderApps)

                    PaymentProviderAppsState.Success(paymentProviderApps)
                }
            }
        } catch (e: Exception) {
            LOG.error("Error loading payment providers", e)
            PaymentProviderAppsState.Error(e)
        }
    }

    internal fun setSelectedPaymentProviderApp(paymentProviderApp: PaymentProviderApp) {
        _selectedPaymentProviderAppFlow.value = SelectedPaymentProviderAppState.AppSelected(paymentProviderApp)
    }

    fun recheckWhichPaymentProviderAppsAreInstalled() {
        LOG.debug("Rechecking which payment provider apps are installed")
        when (val paymentProviderAppsState = _paymentProviderAppsFlow.value) {
            is PaymentProviderAppsState.Success -> {
                LOG.debug("Rechecking {} payment provider apps", paymentProviderAppsState.paymentProviderApps.size)

                val paymentProviders = paymentProviderAppsState.paymentProviderApps.map { it.paymentProvider }
                val paymentProviderApps = getPaymentProviderApps(paymentProviders)

                _paymentProviderAppsFlow.value = PaymentProviderAppsState.Success(paymentProviderApps)
            }

            else -> {
                LOG.debug("No payment provider apps to recheck")
            }
        }
    }

    private fun getPaymentProviderApps(paymentProviders: List<PaymentProvider>): List<PaymentProviderApp> {
        val paymentProviderApps = context.packageManager.getPaymentProviderApps(
            paymentProviders,
            context
        ).filter { it.isInstalled() || it.hasPlayStoreUrl() }

        return paymentProviderApps
    }

    private fun selectPaymentProviderApp(paymentProviderApps: List<PaymentProviderApp>) {
        if (paymentProviderApps.isNotEmpty()) {
            LOG.debug("Received {} payment provider apps", paymentProviderApps.size)
            if (_selectedPaymentProviderAppFlow.value !is SelectedPaymentProviderAppState.AppSelected) {
                val firstInstalledPaymentProviderApp =
                    paymentProviderApps.find { it.isInstalled() }
                if (firstInstalledPaymentProviderApp != null) {
                    LOG.debug(
                        "First payment provider app is installed: {}",
                        firstInstalledPaymentProviderApp.name
                    )
                    _selectedPaymentProviderAppFlow.value =
                        SelectedPaymentProviderAppState.AppSelected(firstInstalledPaymentProviderApp)
                } else {
                    LOG.debug("No installed payment provider app found")
                    _selectedPaymentProviderAppFlow.value = SelectedPaymentProviderAppState.NothingSelected
                }
            }
        } else {
            LOG.debug("No payment provider apps received")
            _selectedPaymentProviderAppFlow.value = SelectedPaymentProviderAppState.NothingSelected
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PaymentComponent::class.java)
    }

    interface Listener {
        fun onMoreInformationClicked()
        fun onBankPickerClicked()
        fun onPayInvoiceClicked()
    }

}

sealed class PaymentProviderAppsState {
    object Loading : PaymentProviderAppsState()
    class Success(val paymentProviderApps: List<PaymentProviderApp>) : PaymentProviderAppsState()
    class Error(val throwable: Throwable) : PaymentProviderAppsState()
}

sealed class SelectedPaymentProviderAppState {
    object NothingSelected : SelectedPaymentProviderAppState()
    class AppSelected(val paymentProviderApp: PaymentProviderApp) : SelectedPaymentProviderAppState()
}
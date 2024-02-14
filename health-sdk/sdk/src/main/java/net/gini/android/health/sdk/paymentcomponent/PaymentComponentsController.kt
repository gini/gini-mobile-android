package net.gini.android.health.sdk.paymentcomponent

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.core.api.Resource
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.paymentprovider.getPaymentProviderApps
import org.slf4j.LoggerFactory

class PaymentComponentsController(private val context: Context, private val giniHealth: GiniHealth) {

    private val _paymentProviderAppsFlow = MutableStateFlow<PaymentProviderAppsState>(PaymentProviderAppsState.Loading)
    val paymentProviderAppsFlow: StateFlow<PaymentProviderAppsState> = _paymentProviderAppsFlow

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
                    PaymentProviderAppsState.Success(
                        context.packageManager.getPaymentProviderApps(
                            paymentProvidersResource.data,
                            context
                        )
                    )
                }
            }
        } catch (e: Exception) {
            LOG.error("Error loading payment providers", e)
            PaymentProviderAppsState.Error(e)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PaymentComponentsController::class.java)
    }

}

sealed class PaymentProviderAppsState {
    object Loading : PaymentProviderAppsState()
    class Success(val paymentProviderApps: List<PaymentProviderApp>) : PaymentProviderAppsState()
    class Error(val throwable: Throwable) : PaymentProviderAppsState()
}
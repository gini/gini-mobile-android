package net.gini.android.health.sdk.paymentcomponent

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import net.gini.android.core.api.Resource
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.paymentprovider.getPaymentProviderApps
import org.slf4j.LoggerFactory

class PaymentComponent(private val context: Context, private val giniHealth: GiniHealth) {

    private val _paymentProviderAppsFlow = MutableStateFlow<PaymentProviderAppsState>(PaymentProviderAppsState.Loading)
    val paymentProviderAppsFlow: StateFlow<PaymentProviderAppsState> = _paymentProviderAppsFlow.asStateFlow()

    private val _selectedPaymentProviderAppFlow =
        MutableStateFlow<SelectedPaymentProviderAppState>(SelectedPaymentProviderAppState.NothingSelected)
    val selectedPaymentProviderAppFlow: StateFlow<SelectedPaymentProviderAppState> = _selectedPaymentProviderAppFlow.asStateFlow()

    private val paymentComponentPreferences = PaymentComponentPreferences(context)

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
                    val paymentProviderApps =
                        getPaymentProviderApps(paymentProvidersResource.data)

                    selectPaymentProviderApp(paymentProviderApps)

                    PaymentProviderAppsState.Success(paymentProviderApps)
                }
            }
        } catch (e: Exception) {
            LOG.error("Error loading payment providers", e)
            PaymentProviderAppsState.Error(e)
        }
    }

    internal suspend fun setSelectedPaymentProviderApp(paymentProviderApp: PaymentProviderApp) {
        _selectedPaymentProviderAppFlow.value = SelectedPaymentProviderAppState.AppSelected(paymentProviderApp)

        paymentComponentPreferences.saveSelectedPaymentProviderId(paymentProviderApp.paymentProvider.id)
    }

    suspend fun recheckWhichPaymentProviderAppsAreInstalled() {
        LOG.debug("Rechecking which payment provider apps are installed")
        when (val paymentProviderAppsState = _paymentProviderAppsFlow.value) {
            is PaymentProviderAppsState.Success -> {
                LOG.debug("Rechecking {} payment provider apps", paymentProviderAppsState.paymentProviderApps.size)

                val paymentProviders = paymentProviderAppsState.paymentProviderApps.map { it.paymentProvider }
                val paymentProviderApps = getPaymentProviderApps(paymentProviders)

                _paymentProviderAppsFlow.value = PaymentProviderAppsState.Success(paymentProviderApps)

                when (val selectedPaymentProviderAppState = _selectedPaymentProviderAppFlow.value) {
                    is SelectedPaymentProviderAppState.AppSelected -> {
                        if (!isSelectedPaymentProviderAppInstalled(
                                paymentProviderApps,
                                selectedPaymentProviderAppState.paymentProviderApp
                            )
                        ) {
                            LOG.debug("Selected payment provider app is not installed anymore. Updating selection state.")
                            selectFirstInstalledPaymentProviderAppOrNothing(paymentProviderApps)
                        } else {
                            LOG.debug("Selected payment provider app is still installed")
                        }
                    }

                    SelectedPaymentProviderAppState.NothingSelected -> {
                        LOG.debug("No payment provider app was selected")
                    }
                }
            }

            else -> {
                LOG.debug("No payment provider apps to recheck")
            }
        }
    }

    private fun isSelectedPaymentProviderAppInstalled(
        paymentProviderApps: List<PaymentProviderApp>,
        selectedPaymentProviderApp: PaymentProviderApp
    ): Boolean {
        val selectedApp =
            paymentProviderApps.find { it.hasSamePaymentProviderId(selectedPaymentProviderApp) }
        return selectedApp != null && selectedApp.isInstalled()
    }

    private fun getPaymentProviderApps(paymentProviders: List<PaymentProvider>): List<PaymentProviderApp> {
        val paymentProviderApps = context.packageManager.getPaymentProviderApps(
            paymentProviders,
            context
        ).filter { it.isInstalled() || it.hasPlayStoreUrl() }

        return paymentProviderApps
    }

    private suspend fun selectPaymentProviderApp(paymentProviderApps: List<PaymentProviderApp>) {
        if (paymentProviderApps.isNotEmpty()) {
            LOG.debug("Received {} payment provider apps", paymentProviderApps.size)

            if (_selectedPaymentProviderAppFlow.value !is SelectedPaymentProviderAppState.AppSelected) {
                val previouslySelectedPaymentProviderApp =
                    getPreviouslySelectedPaymentProviderApp(paymentProviderApps)

                if (previouslySelectedPaymentProviderApp != null && previouslySelectedPaymentProviderApp.isInstalled()) {
                    LOG.debug("Using previously selected payment provider app: {}", previouslySelectedPaymentProviderApp.name)

                    _selectedPaymentProviderAppFlow.value =
                        SelectedPaymentProviderAppState.AppSelected(previouslySelectedPaymentProviderApp)
                } else {
                    LOG.debug("Previously selected payment provider app is not installed")

                    selectFirstInstalledPaymentProviderAppOrNothing(paymentProviderApps)
                }
            }
        } else {
            LOG.debug("No payment provider apps received")
            _selectedPaymentProviderAppFlow.value = SelectedPaymentProviderAppState.NothingSelected

            paymentComponentPreferences.deleteSelectedPaymentProviderId()
        }
    }

    private suspend fun getPreviouslySelectedPaymentProviderApp(paymentProviderApps: List<PaymentProviderApp>): PaymentProviderApp? {
        return paymentComponentPreferences.getSelectedPaymentProviderId()?.let { previouslySelectedPaymentProviderId ->
            paymentProviderApps.find { it.hasSamePaymentProviderId(previouslySelectedPaymentProviderId) }
        }
    }

    private suspend fun selectFirstInstalledPaymentProviderAppOrNothing(paymentProviderApps: List<PaymentProviderApp>) {
        LOG.debug("Selecting first installed payment provider app or nothing")

        val firstInstalledPaymentProviderApp =
            paymentProviderApps.find { it.isInstalled() }
        
        if (firstInstalledPaymentProviderApp != null) {
            LOG.debug(
                "First payment provider app is installed: {}",
                firstInstalledPaymentProviderApp.name
            )
            _selectedPaymentProviderAppFlow.value =
                SelectedPaymentProviderAppState.AppSelected(firstInstalledPaymentProviderApp)

            paymentComponentPreferences.saveSelectedPaymentProviderId(firstInstalledPaymentProviderApp.paymentProvider.id)
        } else {
            LOG.debug("No installed payment provider app found")
            _selectedPaymentProviderAppFlow.value = SelectedPaymentProviderAppState.NothingSelected

            paymentComponentPreferences.deleteSelectedPaymentProviderId()
        }
    }

    private companion object {
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
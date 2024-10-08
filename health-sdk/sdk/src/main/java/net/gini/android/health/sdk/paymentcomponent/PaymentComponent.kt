package net.gini.android.health.sdk.paymentcomponent

import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.gini.android.core.api.Resource
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.review.ReviewConfiguration
import net.gini.android.health.sdk.review.ReviewFragment
import org.slf4j.LoggerFactory
import net.gini.android.health.sdk.bankselection.BankSelectionBottomSheet
import net.gini.android.health.sdk.moreinformation.MoreInformationFragment
import net.gini.android.health.sdk.paymentprovider.getPaymentProviderApps

/**
 * The [PaymentComponent] manages the data and state used by every [PaymentComponentView], the [MoreInformationFragment],
 * and the [BankSelectionBottomSheet].
 *
 * It requires a [GiniHealth] instance and a [Context] (application or activity) to be created.
 */
class PaymentComponent(private val context: Context, internal val giniHealth: GiniHealth, private var configuration: PaymentComponentConfiguration = PaymentComponentConfiguration()) {

    // Holds the state of the Payment Provider apps as received from the server - no processing is done on this list, to serve as a point of truth
    private val _initialStatePaymentProviderAppsFlow = MutableStateFlow<PaymentProviderAppsState>(PaymentProviderAppsState.Loading)

    private val _paymentProviderAppsFlow = MutableStateFlow<PaymentProviderAppsState>(PaymentProviderAppsState.Loading)

    /**
     * A [StateFlow] which emits the state of the payment provider apps. See [PaymentProviderAppsState] for the possible states.
     */
    val paymentProviderAppsFlow: StateFlow<PaymentProviderAppsState> = _paymentProviderAppsFlow.asStateFlow()

    private val _selectedPaymentProviderAppFlow =
        MutableStateFlow<SelectedPaymentProviderAppState>(SelectedPaymentProviderAppState.NothingSelected)

    /**
     * A [StateFlow] which emits the state of the selected payment provider app. See [SelectedPaymentProviderAppState] for the possible states.
     */
    val selectedPaymentProviderAppFlow: StateFlow<SelectedPaymentProviderAppState> = _selectedPaymentProviderAppFlow.asStateFlow()

    private val _returningUserFlow = MutableStateFlow(false)

    /**
     * A [StateFlow] which emits whether the user is a returning one or not.
     */
    val returningUserFlow: StateFlow<Boolean> = _returningUserFlow

    @VisibleForTesting
    internal val paymentComponentPreferences = PaymentComponentPreferences(context)

    internal val giniHealthLanguage = GiniHealth.getSDKLanguage(context)?.languageLocale()

    /**
     * A listener for the payment component. It exposes the user interactions with all of the [PaymentComponentView]s.
     * See [Listener] for the methods you need to implement.
     */
    var listener: Listener? = null

    var paymentComponentConfiguration: PaymentComponentConfiguration
        get() = configuration
        set(value) { configuration = value}

    /**
     * Loads the payment provider apps and selects the first installed payment provider app or nothing if no payment provider
     * app is installed. The selection (or lack of selection) will be visible once a [PaymentComponentView] is shown.
     *
     * It should be sufficient to call [loadPaymentProviderApps] only once when your app starts.
     *
     * By collecting the [paymentProviderAppsFlow] and [selectedPaymentProviderAppFlow] you can observe the state of the
     * loading process.
     */
    suspend fun loadPaymentProviderApps() {
        LOG.debug("Loading payment providers")
        _paymentProviderAppsFlow.value = PaymentProviderAppsState.Loading
        _paymentProviderAppsFlow.value = try {
            when (val paymentProvidersResource = giniHealth.giniHealthAPI.documentManager.getPaymentProviders()) {
                is Resource.Cancelled -> {
                    LOG.debug("Loading payment providers cancelled")
                    _initialStatePaymentProviderAppsFlow.value = PaymentProviderAppsState.Error(Exception("Cancelled"))
                    PaymentProviderAppsState.Error(Exception("Cancelled"))
                }

                is Resource.Error -> {
                    LOG.error("Error loading payment providers", paymentProvidersResource.exception)
                    _initialStatePaymentProviderAppsFlow.value = PaymentProviderAppsState.Error((
                            paymentProvidersResource.exception ?: Exception(
                                paymentProvidersResource.message
                            )))
                    PaymentProviderAppsState.Error(
                        paymentProvidersResource.exception ?: Exception(
                            paymentProvidersResource.message
                        )
                    )
                }

                is Resource.Success -> {
                    LOG.debug("Loaded payment providers")
                    LOG.debug("Loading installed payment provider apps")

                    val paymentProviderApps = getPaymentProviderAppsSorted(paymentProvidersResource.data)

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

    internal fun recheckWhichPaymentProviderAppsAreInstalled() {
        LOG.debug("Rechecking which payment provider apps are installed")
        when (val paymentProviderAppsState = _initialStatePaymentProviderAppsFlow.value) {
            is PaymentProviderAppsState.Success -> {
                LOG.debug("Rechecking {} payment provider apps", paymentProviderAppsState.paymentProviderApps.size)

                val paymentProviders = paymentProviderAppsState.paymentProviderApps.map { it.paymentProvider }
                val paymentProviderApps = getPaymentProviderAppsSorted(paymentProviders)
                updateSelectedPaymentProviderApp(paymentProviderApps)
                _paymentProviderAppsFlow.value = PaymentProviderAppsState.Success(paymentProviderApps)
            }

            else -> {
                LOG.debug("No payment provider apps to recheck")
            }
        }
    }

    @VisibleForTesting
    internal fun sortPaymentProviderApps(paymentProviderList: List<PaymentProviderApp>): List<PaymentProviderApp> = paymentProviderList.sortedBy { it.installedPaymentProviderApp == null }

    private fun getPaymentProviderAppsSorted(paymentProviders: List<PaymentProvider>): List<PaymentProviderApp> {
        val paymentProviderApps = context.packageManager.getPaymentProviderApps(
            paymentProviders,
            context
        )
        _initialStatePaymentProviderAppsFlow.tryEmit(PaymentProviderAppsState.Success(paymentProviderApps))
        val sortedPaymentProviderAppsList = sortPaymentProviderApps(paymentProviderApps)

        return sortedPaymentProviderAppsList
    }

    private suspend fun selectPaymentProviderApp(paymentProviderApps: List<PaymentProviderApp>) {
        if (paymentProviderApps.isNotEmpty()) {
            LOG.debug("Received {} payment provider apps", paymentProviderApps.size)

            if (_selectedPaymentProviderAppFlow.value !is SelectedPaymentProviderAppState.AppSelected) {
                val previouslySelectedPaymentProviderApp =
                    getPreviouslySelectedPaymentProviderApp(paymentProviderApps)

                if (previouslySelectedPaymentProviderApp != null) {
                    LOG.debug("Using previously selected payment provider app: {}", previouslySelectedPaymentProviderApp.name)

                    _selectedPaymentProviderAppFlow.value =
                        SelectedPaymentProviderAppState.AppSelected(previouslySelectedPaymentProviderApp)
                }
            }
        } else {
            LOG.debug("No payment provider apps received")
            _selectedPaymentProviderAppFlow.value = SelectedPaymentProviderAppState.NothingSelected

            paymentComponentPreferences.deleteSelectedPaymentProviderId()
        }
    }

    private fun updateSelectedPaymentProviderApp(paymentProviderApps: List<PaymentProviderApp>) {
        if (_selectedPaymentProviderAppFlow.value is SelectedPaymentProviderAppState.AppSelected) {
            val selectedPaymentProviderApp =
                (_selectedPaymentProviderAppFlow.value as SelectedPaymentProviderAppState.AppSelected).paymentProviderApp
            paymentProviderApps.firstOrNull { it.hasSamePaymentProviderId(selectedPaymentProviderApp) }
                ?.let {
                    _selectedPaymentProviderAppFlow.value = SelectedPaymentProviderAppState.AppSelected(it)
                }
        }
    }

    private suspend fun getPreviouslySelectedPaymentProviderApp(paymentProviderApps: List<PaymentProviderApp>): PaymentProviderApp? {
        return paymentComponentPreferences.getSelectedPaymentProviderId()?.let { previouslySelectedPaymentProviderId ->
            paymentProviderApps.find { it.hasSamePaymentProviderId(previouslySelectedPaymentProviderId) }
        }
    }

    /**
     * Loads the extractions for the given document id and creates an instance of the [ReviewFragment] with the given
     * configuration.
     *
     * You should create and show the [ReviewFragment] in the [Listener.onPayInvoiceClicked] method.
     *
     * @param documentId The document id for which the extractions should be loaded
     * @param configuration The configuration for the [ReviewFragment]
     * @throws IllegalStateException If no payment provider app has been selected
     */
    fun getPaymentReviewFragment(documentId: String, configuration: ReviewConfiguration): ReviewFragment {
        LOG.debug("Getting payment review fragment for id: {}", documentId)

        when (val selectedPaymentProviderAppState = _selectedPaymentProviderAppFlow.value) {
            is SelectedPaymentProviderAppState.AppSelected -> {
                LOG.debug("Creating ReviewFragment for selected payment provider app: {}", selectedPaymentProviderAppState.paymentProviderApp.name)

                return ReviewFragment.newInstance(
                    giniHealth,
                    configuration = configuration,
                    paymentComponent = this@PaymentComponent,
                    documentId = documentId
                )
            }

            SelectedPaymentProviderAppState.NothingSelected -> {
                LOG.error("Cannot create ReviewFragment: No selected payment provider app")

                val exception =
                    IllegalStateException("Cannot create ReviewFragment: No selected payment provider app")
                throw exception
            }
        }
    }

    internal suspend fun onPayInvoiceClicked(documentId: String) {
        paymentComponentPreferences.saveReturningUser()
        listener?.onPayInvoiceClicked(documentId)
        delay(500)
        checkReturningUser()
    }

    internal suspend fun checkReturningUser() {
        _returningUserFlow.value = paymentComponentPreferences.getReturningUser()
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger(PaymentComponent::class.java)
    }

    /**
     * A listener for the [PaymentComponent]. It exposes the user interactions with all of the [PaymentComponentView]s.
     */
    interface Listener {
        /**
         * Called when the user taps the "more information" link or the info icon in the [PaymentComponentView].
         *
         * You should show the [MoreInformationFragment] in this method.
         */
        fun onMoreInformationClicked()

        /**
         * Called when the user taps the bank picker button in the [PaymentComponentView].
         *
         * You should show the [BankSelectionBottomSheet] in this method.
         */
        fun onBankPickerClicked()

        /**
         * Called when the user taps the "pay invoice" button in the [PaymentComponentView]. The document id will be taken
         * from the clicked PaymentComponentView's [PaymentComponentView.documentId] property.
         *
         * @param documentId The value in the clicked PaymentComponentView's [PaymentComponentView.documentId] property
         */
        fun onPayInvoiceClicked(documentId: String)
    }

}

/**
 * The states of the payment provider apps loading process.
 */
sealed class PaymentProviderAppsState {
    /**
     * The payment provider apps are being loaded.
     */
    object Loading : PaymentProviderAppsState()

    /**
     * The payment provider apps were successfully loaded.
     */
    class Success(val paymentProviderApps: List<PaymentProviderApp>) : PaymentProviderAppsState()

    /**
     * An error occurred while loading the payment provider apps.
     */
    class Error(val throwable: Throwable) : PaymentProviderAppsState()
}

/**
 * The states of the selected payment provider app.
 */
sealed class SelectedPaymentProviderAppState {
    /**
     * No payment provider app is selected.
     */
    object NothingSelected : SelectedPaymentProviderAppState()

    /**
     * A payment provider app is selected.
     */
    class AppSelected(val paymentProviderApp: PaymentProviderApp) : SelectedPaymentProviderAppState()
}
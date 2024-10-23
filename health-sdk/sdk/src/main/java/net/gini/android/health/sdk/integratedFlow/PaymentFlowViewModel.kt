package net.gini.android.health.sdk.integratedFlow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import net.gini.android.core.api.Resource
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.model.toCommonPaymentDetails
import net.gini.android.health.sdk.review.model.withFeedback
import net.gini.android.health.sdk.util.DisplayedScreen
import net.gini.android.health.sdk.util.DisplayedScreen.Companion.toInternalDisplayedScreen
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.api.model.PaymentRequest
import net.gini.android.internal.payment.paymentComponent.BankPickerRows
import net.gini.android.internal.payment.paymentComponent.PaymentProviderAppsState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.utils.BackListener
import net.gini.android.internal.payment.utils.FlowBottomSheetsManager
import net.gini.android.internal.payment.utils.PaymentNextStep
import java.io.File
import java.util.Stack

internal class PaymentFlowViewModel(
    internal var paymentDetails: PaymentDetails?,
    internal var documentId: String?,
    val paymentFlowConfiguration: PaymentFlowConfiguration?,
    val giniHealth: GiniHealth
) : ViewModel(), FlowBottomSheetsManager, BackListener {

    private val backstack: Stack<DisplayedScreen> = Stack<DisplayedScreen>().also { it.add(DisplayedScreen.Nothing) }
    private var initialSelectedPaymentProvider: PaymentProviderApp? = null

    override var openWithCounter: Int = 0
    override val paymentNextStepFlow = MutableSharedFlow<PaymentNextStep>(
        extraBufferCapacity = 1,
    )
    override val paymentRequestFlow: MutableStateFlow<PaymentRequest?> = MutableStateFlow(null)
    override val giniInternalPaymentModule: GiniInternalPaymentModule = giniHealth.giniInternalPaymentModule

    val paymentComponent = giniInternalPaymentModule.paymentComponent

    val paymentNextStep: SharedFlow<PaymentNextStep> = paymentNextStepFlow

    private val _backButtonEvent = MutableSharedFlow<Void?>(extraBufferCapacity = 1)
    val backButtonEvent: SharedFlow<Void?> = _backButtonEvent

    override val shareWithFlowStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var externalCacheDir: File? = null

    init {
        viewModelScope.launch {
            paymentComponent.paymentProviderAppsFlow.collect {
                if (it is PaymentProviderAppsState.Error) {
                    giniInternalPaymentModule.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnErrorOccurred(it.throwable))
                    delay(50)
                    giniInternalPaymentModule.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnCancelled)
                }
            }
        }
        paymentFlowConfiguration?.let {
            paymentComponent.shouldCheckReturningUser = true
            paymentComponent.bankPickerRows = BankPickerRows.TWO
        }
        documentId?.let {
            viewModelScope.launch { giniHealth.setDocumentForReview(it) }
        }
    }

    fun loadPaymentProviderApps() = viewModelScope.launch {
        if (paymentComponent.paymentProviderAppsFlow.value is PaymentProviderAppsState.Loading || paymentComponent.paymentProviderAppsFlow.value is PaymentProviderAppsState.Success) return@launch
        paymentComponent.loadPaymentProviderApps()
    }

    fun addToBackStack(destination: DisplayedScreen) {
        backstack.add(destination)
        setDisplayedScreen()
    }

    fun popBackStack() {
        backstack.pop()
        setDisplayedScreen()
    }

    fun getLastBackstackEntry() = if (backstack.isNotEmpty()) backstack.peek() else DisplayedScreen.Nothing

    private fun setDisplayedScreen() = viewModelScope.launch {
        giniInternalPaymentModule.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnScreenDisplayed(getLastBackstackEntry().toInternalDisplayedScreen()))
    }

    fun paymentProviderAppChanged(paymentProviderApp: PaymentProviderApp): Boolean {
        if (initialSelectedPaymentProvider == null) {
            observeOpenWithCount(paymentProviderApp.paymentProvider.id)
        }
        if (initialSelectedPaymentProvider?.paymentProvider?.id != paymentProviderApp.paymentProvider.id) {
            initialSelectedPaymentProvider = paymentProviderApp
            observeOpenWithCount(paymentProviderApp.paymentProvider.id)
            return true
        }
        return false
    }

    fun checkBankAppInstallState(paymentProviderApp: PaymentProviderApp) {
        if (paymentProviderApp.isInstalled() != initialSelectedPaymentProvider?.isInstalled()) {
            initialSelectedPaymentProvider = paymentProviderApp
        }
    }

    fun updatePaymentDetails(paymentDetails: PaymentDetails) {
        this.paymentDetails = paymentDetails
    }

    fun onPayment() = viewModelScope.launch {
        documentId?.let {
            sendFeedback()
        }
        paymentDetails?.let {
            giniInternalPaymentModule.onPayment(initialSelectedPaymentProvider, it.toCommonPaymentDetails())
        }
    }

    fun getPaymentProviderApp() = initialSelectedPaymentProvider

    private fun observeOpenWithCount(paymentProviderAppId: String) {
        startObservingOpenWithCount(viewModelScope, paymentProviderAppId)
    }

    fun emitShareWithStartedEvent() {
        paymentRequestFlow.value?.let {
            giniInternalPaymentModule.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnFinishedWithPaymentRequestCreated(it.id, initialSelectedPaymentProvider?.name ?: ""))
        }
        shareWithFlowStarted.tryEmit(true)
    }

    fun onPaymentButtonTapped() {
        checkNextStep(initialSelectedPaymentProvider, externalCacheDir, viewModelScope)
    }

    fun onForwardToSharePdfTapped() {
        documentId?.let {
            sendFeedback()
        }
        sharePdf(initialSelectedPaymentProvider, externalCacheDir, viewModelScope)
    }

    override suspend fun getPaymentRequest(): PaymentRequest = giniInternalPaymentModule.getPaymentRequest(initialSelectedPaymentProvider, paymentDetails?.toCommonPaymentDetails())

    override suspend fun getPaymentRequestDocument(paymentRequest: PaymentRequest): Resource<ByteArray> = giniInternalPaymentModule.giniHealthAPI.documentManager.getPaymentRequestDocument(paymentRequest.id)

    fun setFlowCancelled() {
        giniInternalPaymentModule.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnCancelled)
    }

    private fun sendFeedback() {
        viewModelScope.launch {
            try {
                when (val documentResult = giniHealth.documentFlow.value) {
                    is ResultWrapper.Success -> paymentDetails?.extractions?.let { extractionsContainer ->
                        giniHealth.documentManager.sendFeedbackForExtractions(
                            documentResult.value,
                            extractionsContainer.specificExtractions,
                            extractionsContainer.compoundExtractions.withFeedback(paymentDetails!!)
                        )
                    }

                    is ResultWrapper.Error -> {}
                    is ResultWrapper.Loading -> {}
                }
            } catch (ignored: Throwable) {
                // Ignored since we don't want to interrupt the flow because of feedback failure
            }
        }
    }

    fun setExternalCacheDir(directory: File?) {
        externalCacheDir = directory
    }

    class Factory(private val paymentDetails: PaymentDetails?, private val documentId: String?, private val paymentFlowConfiguration: PaymentFlowConfiguration?, private val giniHealth: GiniHealth): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaymentFlowViewModel(
                paymentDetails = paymentDetails,
                documentId = documentId,
                paymentFlowConfiguration = paymentFlowConfiguration,
                giniHealth = giniHealth) as T
        }
    }

    override fun backCalled() {
        _backButtonEvent.tryEmit(null)
    }
}